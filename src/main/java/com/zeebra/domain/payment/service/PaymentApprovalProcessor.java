package com.zeebra.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.order.dto.OrderItemResponse;
import com.zeebra.domain.order.entity.OrderItemStatus;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.service.OrderService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApprovalProcessor {

	private final PaymentRepository paymentRepository;
	private final PaymentQueryRepository paymentQueryRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final OrderService orderService;
	private final SalesService salesService;
	private final ObjectMapper objectMapper;

	/**
	 * 1단계: Payment 승인 준비
	 */
	@Transactional
	public Payment preparePaymentForApproval(
		String tossOrderId,
		Long memberId,
		BigDecimal amount,
		String paymentKey,
		String idempotencyKey
	) {
		Payment payment = findPaymentByTossOrderId(tossOrderId);

		if (!paymentQueryRepository.isPaymentOwnedByMember(payment.getId(), memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		payment.validateApprovable();
		payment.validateAmount(amount);

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

		List<OrderItemResponse> orderItems = orderService.getOrderDetail(memberId, payment.getOrderId()).orderItems();
		salesService.reserveSales(orderItems);

		log.info("[결제 승인 준비 완료] paymentId: {}, tossOrderId: {}, status: APPROVING",
			payment.getId(), tossOrderId);

		return payment;
	}

	/**
	 * 3-1단계: 승인 성공 시 Payment 업데이트 + 이력 저장
	 */
	@Transactional
	public void updatePaymentOnSuccess(
		Payment payment,
		TossApprovalResponse tossResponse,
		String idempotencyKey,
		Long memberId
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
		orderService.updateOrderStatus(
			reloadedPayment.getOrderId(),
			OrderStatus.PAID,
			idempotencyKey + "-order-paid"
		);
		orderService.updateAllOrderItemsStatus(reloadedPayment.getOrderId(), OrderItemStatus.PAID);

		List<OrderItemResponse> orderItems = orderService.getOrderDetail(memberId, payment.getOrderId()).orderItems();
		salesService.confirmSales(orderItems);

		log.info("[Payment 승인 성공 + 이력 저장 완료] paymentId: {}, status: APPROVED", reloadedPayment.getId());
	}

	/**
	 * 3-2단계: 승인 실패 시 Payment 업데이트 + 이력 저장
	 */
	@Transactional
	public void updatePaymentOnFailure(
		Payment payment,
		TossApprovalResponse tossResponse,
		String idempotencyKey,
		Long memberId
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

		orderService.updateOrderStatus(
			reloadedPayment.getOrderId(),
			OrderStatus.PAYMENT_FAILED,
			idempotencyKey + "-order-failed"
		);

		List<OrderItemResponse> orderItems = orderService.getOrderDetail(memberId, payment.getOrderId()).orderItems();
		salesService.cancelSales(orderItems);

		log.warn("[Payment 승인 실패 + 이력 저장 완료] paymentId: {}, status: FAILED, reason: {}",
			reloadedPayment.getId(), reloadedPayment.getFailureReason());
	}

	/**
	 * API 호출 자체가 실패한 경우 처리
	 */
	@Transactional
	public void handleApiCallException(
		Payment payment,
		Exception exception,
		String idempotencyKey,
		Long memberId
	) {
		Payment reloadedPayment = reloadPayment(payment.getId());

		reloadedPayment.updatePaymentStatus(PaymentStatus.FAILED);
		reloadedPayment.updateFailureReason("API 호출 실패: " + exception.getMessage());

		savePaymentHistory(reloadedPayment, PaymentStatus.FAILED, idempotencyKey + "-api-exception");

		updatePaymentTransactionStatus(
			reloadedPayment,
			PaymentTransactionStatus.FAILED,
			createApiErrorResponse(exception)
		);

		orderService.updateOrderStatus(
			reloadedPayment.getOrderId(),
			OrderStatus.PAYMENT_FAILED,
			idempotencyKey + "-order-api-failed"
		);

		List<OrderItemResponse> orderItems = orderService.getOrderDetail(memberId, payment.getOrderId()).orderItems();
		salesService.cancelSales(orderItems);

		log.error("[API 호출 예외 처리 완료] paymentId: {}, status: FAILED", reloadedPayment.getId());
	}

	/**
	 * 토스 응답 성공 여부 확인
	 */
	public boolean isApprovalSuccessful(TossApprovalResponse tossResponse) {
		return tossResponse != null
			&& tossResponse.tossPayment() != null
			&& "DONE".equals(tossResponse.tossPayment().status());
	}

	/**
	 * Payment 재조회 (트랜잭션 분리 후 최신 상태 조회용)
	 */
	public Payment reloadPayment(Long paymentId) {
		return paymentRepository.findById(paymentId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	// ========== Private Helper Methods ==========

	private Payment findPaymentByTossOrderId(String tossOrderId) {
		return paymentRepository.findByTossOrderId(tossOrderId)
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

	private Map<String, Object> createApiErrorResponse(Exception exception) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("error", "API_CALL_FAILED");
		errorResponse.put("message", exception.getMessage());
		errorResponse.put("exceptionType", exception.getClass().getSimpleName());
		return errorResponse;
	}

	// ========== Toss Response Helper Methods ==========

	private void updatePaymentMethodFromTossResponse(Payment payment, TossApprovalResponse tossResponse) {
		String method = tossResponse.tossPayment().method();

		if (!PaymentMethod.isSupported(method)) {
			log.warn("[결제 수단 변환 실패] 지원하지 않는 결제 수단입니다. method: {}, paymentId: {}",
				method, payment.getId());
			return;
		}

		PaymentMethod paymentMethod = PaymentMethod.of(method);
		payment.updatePaymentMethod(paymentMethod);
		log.debug("[결제 수단 저장] paymentId: {}, method: {}", payment.getId(), paymentMethod);
	}

	private void updateApprovedAtFromTossResponse(Payment payment, TossApprovalResponse tossResponse) {
		String approvedAtStr = tossResponse.tossPayment().approvedAt();
		if (approvedAtStr == null) {
			return;
		}

		LocalDateTime approvedAt = parseApprovedAt(approvedAtStr);

		if (approvedAt != null) {
			log.debug("[승인 날짜 저장] paymentId: {}, approvedAt: {}", payment.getId(), approvedAt);
			payment.updateApprovedAt(approvedAt);
			return;
		}

		log.warn("[승인 날짜 파싱 실패] approvedAt: {}", approvedAtStr);
	}

	private LocalDateTime parseApprovedAt(String approvedAtStr) {
		try {
			if (approvedAtStr.contains("+") || approvedAtStr.endsWith("Z")) {
				return OffsetDateTime.parse(approvedAtStr).toLocalDateTime();
			}
			return LocalDateTime.parse(approvedAtStr);
		} catch (Exception e) {
			return null;
		}
	}

	private String extractFailureReason(TossApprovalResponse tossResponse) {
		if (tossResponse == null) {
			return "응답 없음";
		}

		if (tossResponse.tossError() == null) {
			return "결제 정보 없음";
		}

		return tossResponse.tossError().code() + " - " + tossResponse.tossError().message();
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
}