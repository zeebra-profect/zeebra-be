package com.zeebra.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemLine;
import com.zeebra.domain.order.entity.OrderItemStatus;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.service.OrderService;
import com.zeebra.domain.payment.dto.ApprovePaymentRequest;
import com.zeebra.domain.payment.dto.ApprovePaymentResponse;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.dto.FailPaymentRequest;
import com.zeebra.domain.payment.dto.FailPaymentResponse;
import com.zeebra.domain.payment.dto.TossApprovalResponse;
import com.zeebra.domain.payment.entity.Payment;
import com.zeebra.domain.payment.entity.PaymentHistory;
import com.zeebra.domain.payment.entity.PaymentMethod;
import com.zeebra.domain.payment.entity.PaymentStatus;
import com.zeebra.domain.payment.entity.PaymentTransaction;
import com.zeebra.domain.payment.entity.PaymentTransactionStatus;
import com.zeebra.domain.payment.entity.PaymentTransactionType;
import com.zeebra.domain.payment.repository.PaymentHistoryRepository;
import com.zeebra.domain.payment.repository.PaymentQueryRepository;
import com.zeebra.domain.payment.repository.PaymentRepository;
import com.zeebra.domain.payment.repository.PaymentTransactionRepository;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	private static final String TOSS_ORDER_ID_PREFIX = "ORD_";
	private static final int MAX_RETRY_COUNT = 3;

	private final PaymentRepository paymentRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;
	private final PaymentQueryRepository paymentQueryRepository;
	private final OrderService orderService;
	private final TossPaymentService tossPaymentService;
	private final ObjectMapper objectMapper;
	private final SalesService salesService;

	@Transactional
	public CreatePaymentResponse createPayment(CreatePaymentRequest request, Long memberId) {

		request.validateInternalAmount();
		OrderInfo order = orderService.getOrder(memberId, request.orderId());

		order.validatePaymentCreatable();
		order.validatePaymentAmount(request.amount());

		List<OrderItemLine>  itemLines = orderService.getOrderItemLine(order.orderId());

		salesService.validatePurchasable(itemLines);

		Payment payment = savePaymentWithHistory(request, order);
		orderService.updateOrderStatus(order.orderId(), OrderStatus.PAYMENT_PENDING, request.clientRequestId());

		return CreatePaymentResponse.of(payment);
	}

	public ApprovePaymentResponse approvePayment(ApprovePaymentRequest request, Long memberId) {
		// 1단계: Payment 조회 및 검증, 상태 변경
		Payment payment = preparePaymentForApproval(
			request.tossOrderId(),
			memberId,
			request.amount(),
			request.paymentKey(),
			request.clientRequestId()
		);

		// 2단계: 토스 API 호출 (트랜잭션 외부!)
		TossApprovalResponse tossResponse = callTossApiSafely(payment, request);

		// 3단계: 토스 응답에 따라 처리
		if (isApprovalSuccessful(tossResponse)) {
			updatePaymentOnSuccess(payment, tossResponse, request.clientRequestId());
		} else {
			updatePaymentOnFailure(payment, tossResponse, request.clientRequestId());
		}

		Payment reloadedPayment = reloadPayment(payment.getId());

		return ApprovePaymentResponse.of(reloadedPayment, orderService.getOrder(memberId, reloadedPayment.getOrderId()));
	}

	@Transactional
	public FailPaymentResponse failPayment(Long memberId, FailPaymentRequest request) {
		Payment payment = findPaymentByTossOrderId(request.tossOrderId());

		if(payment.getPaymentStatus() == PaymentStatus.FAILED ) {
			throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
		}

		orderService.updateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_FAILED, request.clientRequestId());
		payment.updatePaymentStatus(PaymentStatus.FAILED);

		PaymentHistory paymentHistory = PaymentHistory.createPaymentHistory(payment.getId(), PaymentStatus.FAILED, request.clientRequestId());

		OrderInfo order = orderService.getOrder(memberId, payment.getOrderId());
		return FailPaymentResponse.of(payment, order);
	}

	/**
	 * 1단계: Payment 승인 준비
	 */
	@Transactional
	protected Payment preparePaymentForApproval(
		String tossOrderId,
		Long memberId,
		BigDecimal amount,
		String paymentKey,
		String idempotencyKey
	) {
		Payment payment = findPaymentByTossOrderId(tossOrderId);
		validatePaymentForApproval(payment, memberId, amount);

		payment.updatePaymentStatus(PaymentStatus.APPROVING);
		payment.updatePaymentKey(paymentKey);

		savePaymentHistory(payment, PaymentStatus.APPROVING, idempotencyKey);
		savePaymentTransaction(
			payment,
			PaymentTransactionType.PAYMENT,
			PaymentTransactionStatus.REQUESTED,
			null,
			0
		);

		log.info("[결제 승인 준비 완료] paymentId: {}, tossOrderId: {}, status: APPROVING",
			payment.getId(), tossOrderId);

		return payment;
	}

	/**
	 * 2단계: 토스 API 호출 (트랜잭션 외부)
	 */
	private TossApprovalResponse callTossApiSafely(Payment payment, ApprovePaymentRequest request) {
		try {
			return callTossApprovalApi(payment, request.paymentKey(), request.tossOrderId(), request.amount());
		} catch (Exception e) {
			log.error("[토스 API 호출 실패] paymentId: {}, error: {}", payment.getId(), e.getMessage(), e);
			handleApiCallException(payment, e, request.clientRequestId());
			throw new BusinessException(PaymentErrorCode.TOSS_API_ERROR);
		}
	}

	protected TossApprovalResponse callTossApprovalApi(
		Payment payment,
		String paymentKey,
		String tossOrderId,
		BigDecimal amount
	) {
		log.info("[토스 승인 API 호출 시작] paymentId: {}, tossOrderId: {}", payment.getId(), tossOrderId);

		TossApprovalResponse response = tossPaymentService.approve(paymentKey, tossOrderId, amount);

		log.info("[토스 승인 API 호출 완료] paymentId: {}, status: {}",
			payment.getId(), response.tossPayment() != null ? response.tossPayment().status() : "null");

		return response;
	}

	/**
	 * API 호출 자체가 실패한 경우 처리
	 */
	@Transactional
	protected void handleApiCallException(Payment payment, Exception exception, String idempotencyKey) {
		Payment reloadedPayment = reloadPayment(payment.getId());

		reloadedPayment.updatePaymentStatus(PaymentStatus.FAILED);
		reloadedPayment.updateFailureReason("API 호출 실패: " + exception.getMessage());

		savePaymentHistory(reloadedPayment, PaymentStatus.FAILED, idempotencyKey + "-api-exception");

		updatePaymentTransactionStatus(
			reloadedPayment,
			PaymentTransactionStatus.FAILED,
			createApiErrorResponse(exception)
		);

		updateOrderStatusSafely(
			reloadedPayment.getOrderId(),
			OrderStatus.PAYMENT_FAILED,
			idempotencyKey + "-order-api-failed"
		);

		log.error("[API 호출 예외 처리 완료] paymentId: {}, status: FAILED", reloadedPayment.getId());
	}

	/**
	 * 3-1단계: 승인 성공 시 Payment 업데이트 + 이력 저장
	 */
	@Transactional
	protected void updatePaymentOnSuccess(
		Payment payment,
		TossApprovalResponse tossResponse,
		String idempotencyKey
	) {
		Payment reloadedPayment = reloadPayment(payment.getId());

		reloadedPayment.updatePaymentStatus(PaymentStatus.APPROVED);
		updatePaymentMethodFromTossResponse(reloadedPayment, tossResponse);
		updateApprovedAtFromTossResponse(reloadedPayment, tossResponse);

		Map<String, Object> tossResponseMap = convertTossPaymentToMap(tossResponse);
		reloadedPayment.updateTossResponse(tossResponseMap);

		savePaymentHistory(reloadedPayment, PaymentStatus.APPROVED, idempotencyKey + "-approved");

		updatePaymentTransactionStatus(
			reloadedPayment,
			PaymentTransactionStatus.SUCCEEDED,
			tossResponseMap
		);

		updateOrderStatus(
			reloadedPayment.getOrderId(),
			OrderStatus.PAID,
			idempotencyKey + "-order-paid"
		);

		// sales 재고랑 상태 변경해줘야 함..

		log.info("[Payment 승인 성공 + 이력 저장 완료] paymentId: {}, status: APPROVED", reloadedPayment.getId());
	}

	/**
	 * 3-2단계: 승인 실패 시 Payment 업데이트 + 이력 저장
	 */
	@Transactional
	protected void updatePaymentOnFailure(
		Payment payment,
		TossApprovalResponse tossResponse,
		String idempotencyKey
	) {
		Payment reloadedPayment = reloadPayment(payment.getId());

		reloadedPayment.updatePaymentStatus(PaymentStatus.FAILED);
		reloadedPayment.updateFailureReason(extractFailureReason(tossResponse));

		Map<String, Object> tossResponseMap = convertTossPaymentToMap(tossResponse);
		reloadedPayment.updateTossResponse(tossResponseMap);

		savePaymentHistory(reloadedPayment, PaymentStatus.FAILED, idempotencyKey + "-failed");

		updatePaymentTransactionStatus(
			reloadedPayment,
			PaymentTransactionStatus.FAILED,
			tossResponseMap
		);

		updateOrderStatusSafely(
			reloadedPayment.getOrderId(),
			OrderStatus.PAYMENT_FAILED,
			idempotencyKey + "-order-failed"
		);

		log.warn("[Payment 승인 실패 + 이력 저장 완료] paymentId: {}, status: FAILED, reason: {}",
			reloadedPayment.getId(), reloadedPayment.getFailureReason());
	}

	// ========== Validation Methods ==========

	private Payment findPaymentByTossOrderId(String tossOrderId) {
		return paymentRepository.findByTossOrderId(tossOrderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	private void validatePaymentForApproval(Payment payment, Long memberId, BigDecimal amount) {
		if (!paymentQueryRepository.isPaymentOwnedByMember(payment.getId(), memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		validatePaymentStatus(payment);

		if (payment.getPaymentAmount().compareTo(amount) != 0) {
			throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
		}
	}

	private void validatePaymentStatus(Payment payment) {
		if (payment.getPaymentStatus() == PaymentStatus.APPROVED) {
			throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_APPROVED);
		}

		if (payment.getPaymentStatus() == PaymentStatus.APPROVING) {
			throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
		}

		if (payment.getPaymentStatus() != PaymentStatus.PENDING && payment.getPaymentStatus() != PaymentStatus.FAILED) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS_TRANSITION);
		}
	}

	// ========== Payment Helper Methods ==========

	private Payment reloadPayment(Long paymentId) {
		return paymentRepository.findById(paymentId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	private void savePaymentHistory(Payment payment, PaymentStatus paymentStatus, String idempotencyKey) {
		PaymentHistory paymentHistory = PaymentHistory.createPaymentHistory(
			payment.getId(),
			paymentStatus,
			idempotencyKey
		);
		paymentHistoryRepository.save(paymentHistory);
	}

	private Payment savePaymentWithHistory(CreatePaymentRequest request, OrderInfo order) {
		String tossOrderId = generateTossOrderId(order.orderNumber());
		Payment payment =  Payment.createPayment(
			request.orderId(),
			tossOrderId,
			request.orderName(),
			request.amount(),
			request.clientRequestId()
		);

		Payment savedPayment = paymentRepository.save(payment);

		savePaymentHistory(savedPayment, savedPayment.getPaymentStatus(), request.clientRequestId());

		return savedPayment;
	}

	private String generateTossOrderId(String orderNumber) {
		String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String tossOrderId = TOSS_ORDER_ID_PREFIX + orderNumber + "_" + uniqueId;

		return tossOrderId;
	}

	// ========== PaymentTransaction Helper Methods ==========

	private void savePaymentTransaction(
		Payment payment,
		PaymentTransactionType type,
		PaymentTransactionStatus status,
		TossApprovalResponse tossResponse,
		int responseCode
	) {
		Map<String, Object> requestData = createTransactionRequestData(payment);
		Map<String, Object> responseData = convertTossPaymentToMap(tossResponse);

		PaymentTransaction transaction = new PaymentTransaction(
			payment.getId(),
			type,
			status,
			requestData,
			responseData,
			responseCode,
			0,
			null
		);

		paymentTransactionRepository.save(transaction);
	}

	private void updatePaymentTransactionStatus(
		Payment payment,
		PaymentTransactionStatus status,
		Map<String, Object> responseData
	) {
		PaymentTransaction transaction = paymentTransactionRepository.findByPaymentId(payment.getId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

		transaction.update(status, responseData, 200, 0, null);
	}

	private Map<String, Object> createTransactionRequestData(Payment payment) {
		Map<String, Object> requestData = new HashMap<>();
		requestData.put("paymentId", payment.getId());
		requestData.put("orderId", payment.getOrderId());
		requestData.put("tossOrderId", payment.getTossOrderId());
		requestData.put("paymentKey", payment.getPaymentKey());
		requestData.put("amount", payment.getPaymentAmount());
		return requestData;
	}

	// ========== Toss Response Helper Methods ==========

	private boolean isApprovalSuccessful(TossApprovalResponse tossResponse) {
		return tossResponse != null
			&& tossResponse.tossPayment() != null
			&& "DONE".equals(tossResponse.tossPayment().status());
	}

	private void updatePaymentMethodFromTossResponse(Payment payment, TossApprovalResponse tossResponse) {
		if (tossResponse.tossPayment() != null && tossResponse.tossPayment().method() != null) {
			try {
				String method = tossResponse.tossPayment().method();
				PaymentMethod paymentMethod = convertToPaymentMethod(method);
				payment.updatePaymentMethod(paymentMethod);
				log.debug("[결제 수단 저장] paymentId: {}, method: {}", payment.getId(), paymentMethod);
			} catch (IllegalArgumentException e) {
				log.warn("[결제 수단 변환 실패] method: {}, error: {}", 
					tossResponse.tossPayment().method(), e.getMessage());
			}
		}
	}

	private PaymentMethod convertToPaymentMethod(String tossMethod) {
		if (tossMethod == null) {
			throw new IllegalArgumentException("결제 수단이 null입니다.");
		}

		// 영문 -> Enum 직접 변환 시도
		try {
			return PaymentMethod.valueOf(tossMethod.toUpperCase());
		} catch (IllegalArgumentException e) {
			// 한글 -> 영문 매핑
			return switch (tossMethod) {
				case "카드" -> PaymentMethod.CARD;
				case "간편결제" -> PaymentMethod.EASY_PAY;
				case "가상계좌" -> PaymentMethod.VIRTUAL_ACCOUNT;
				case "휴대폰" -> PaymentMethod.MOBILE_PHONE;
				case "계좌이체" -> PaymentMethod.TRANSFER;
				case "문화상품권" -> PaymentMethod.CULTURE_GIFT_CERTIFICATE;
				case "도서문화상품권" -> PaymentMethod.BOOK_GIFT_CERTIFICATE;
				case "게임문화상품권" -> PaymentMethod.GAME_GIFT_CERTIFICATE;
				default -> throw new IllegalArgumentException("알 수 없는 결제 수단: " + tossMethod);
			};
		}
	}

	private void updateApprovedAtFromTossResponse(Payment payment, TossApprovalResponse tossResponse) {
		if (tossResponse.tossPayment() != null && tossResponse.tossPayment().approvedAt() != null) {
			try {
				String approvedAtStr = tossResponse.tossPayment().approvedAt();
				LocalDateTime approvedAt = parseApprovedAt(approvedAtStr);
				payment.updateApprovedAt(approvedAt);
				log.debug("[승인 날짜 저장] paymentId: {}, approvedAt: {}", payment.getId(), approvedAt);
			} catch (Exception e) {
				log.warn("[승인 날짜 파싱 실패] approvedAt: {}, error: {}", 
					tossResponse.tossPayment().approvedAt(), e.getMessage());
			}
		}
	}

	/**
	 * Toss API의 approvedAt 파싱
	 * 타임존 포함 여부에 관계없이 LocalDateTime으로 변환
	 */
	private LocalDateTime parseApprovedAt(String approvedAtStr) {
		try {
			if (approvedAtStr.contains("+") || approvedAtStr.endsWith("Z")) {
				OffsetDateTime offsetDateTime = OffsetDateTime.parse(approvedAtStr);
				return offsetDateTime.toLocalDateTime();
			}
			return LocalDateTime.parse(approvedAtStr);
		} catch (Exception e) {
			throw new IllegalArgumentException("날짜 파싱 실패: " + approvedAtStr, e);
		}
	}

	private String extractFailureReason(TossApprovalResponse tossResponse) {
		if (tossResponse == null) {
			return "응답 없음";
		}

		if (tossResponse.tossError() == null) {
			return "결제 정보 없음";
		}

		String reason = tossResponse.tossError().code() + " - " + tossResponse.tossError().message();
		return reason;
	}

	private Map<String, Object> convertTossPaymentToMap(TossApprovalResponse tossResponse) {
		if (tossResponse == null) {
			return new HashMap<>();
		}

		Map<String, Object> responseMap = new HashMap<>();

		if (tossResponse.tossPayment() != null) {
			try {
				Map<String, Object> paymentMap = objectMapper.convertValue(tossResponse.tossPayment(), Map.class);
				responseMap.put("payment", paymentMap);
				responseMap.put("success", true);
			} catch (IllegalArgumentException e) {
				log.warn("[토스 결제 정보 변환 실패] error: {}", e.getMessage());
			}
		}

		if (tossResponse.tossError() != null) {
			try {
				Map<String, Object> errorMap = objectMapper.convertValue(tossResponse.tossError(), Map.class);
				responseMap.put("error", errorMap);
				responseMap.put("success", false);
			} catch (IllegalArgumentException e) {
				log.warn("[토스 에러 정보 변환 실패] error: {}", e.getMessage());
				Map<String, Object> fallbackError = new HashMap<>();
				fallbackError.put("code", tossResponse.tossError().code());
				fallbackError.put("message", tossResponse.tossError().message());
				responseMap.put("error", fallbackError);
				responseMap.put("success", false);
			}
		}

		return responseMap;
	}

	private Map<String, Object> createApiErrorResponse(Exception exception) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("error", "API_CALL_FAILED");
		errorResponse.put("message", exception.getMessage());
		errorResponse.put("exceptionType", exception.getClass().getSimpleName());
		return errorResponse;
	}

	// ========== Order Helper Methods ==========

	private void updateOrderStatus(Long orderId, OrderStatus status, String idempotencyKey) {
		try {
			orderService.updateOrderStatus(orderId, status, idempotencyKey);
			orderService.updateAllOrderItemsStatus(orderId, OrderItemStatus.PAID);
			log.info("[Order 상태 업데이트 성공] orderId: {}, status: {}", orderId, status);
		} catch (Exception e) {
			log.error("[Order 상태 업데이트 실패] orderId: {}, error: {}", orderId, e.getMessage(), e);
			throw new BusinessException(PaymentErrorCode.ORDER_UPDATE_FAILED);
		}
	}

	private void updateOrderStatusSafely(Long orderId, OrderStatus status, String idempotencyKey) {
		try {
			orderService.updateOrderStatus(orderId, status, idempotencyKey);
			log.info("[Order 상태 업데이트 성공] orderId: {}, status: {}", orderId, status);
		} catch (Exception e) {
			log.error("[Order 상태 업데이트 실패] orderId: {}, error: {}", orderId, e.getMessage(), e);
		}
	}
}