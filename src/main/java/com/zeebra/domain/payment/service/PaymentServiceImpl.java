package com.zeebra.domain.payment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemLine;
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
import com.zeebra.domain.payment.entity.PaymentStatus;
import com.zeebra.domain.payment.repository.PaymentHistoryRepository;
import com.zeebra.domain.payment.repository.PaymentRepository;
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

	private final PaymentRepository paymentRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;
	private final OrderService orderService;
	private final SalesService salesService;
	private final TossPaymentService tossPaymentService;
	private final PaymentApprovalProcessor paymentApprovalProcessor;

	@Override
	@Transactional
	public CreatePaymentResponse createPayment(CreatePaymentRequest request, Long memberId) {
		Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.clientRequestId());

		if (existing.isPresent()) {
			return CreatePaymentResponse.of(existing.get());
		}

		request.validateInternalAmount();
		OrderInfo order = orderService.getOrder(memberId, request.orderId());

		order.validatePaymentCreatable();
		order.validatePaymentAmount(request.amount());

		List<OrderItemLine> itemLines = orderService.getOrderItemLine(order.orderId());
		salesService.validatePurchasable(itemLines);

		String orderStatusIdempotencyKey = OrderStatus.PAYMENT_PENDING.name() + ":" + request.clientRequestId();

		Payment payment = savePaymentWithHistory(request, order);
		orderService.updateOrderStatus(order.orderId(), OrderStatus.PAYMENT_PENDING, orderStatusIdempotencyKey);

		return CreatePaymentResponse.of(payment);
	}

	@Override
	public ApprovePaymentResponse approvePayment(ApprovePaymentRequest request, Long memberId) {
		// 1단계: Payment 조회 및 검증, 상태 변경
		Payment payment = paymentApprovalProcessor.preparePaymentForApproval(
			request.tossOrderId(),
			memberId,
			request.amount(),
			request.paymentKey(),
			request.clientRequestId()
		);

		// 2단계: 토스 API 호출 (트랜잭션 외부)
		TossApprovalResponse tossResponse;
		try {
			tossResponse = callTossApprovalApi(request.paymentKey(), request.tossOrderId(), request.amount(),
				request.isTest());
		} catch (Exception e) {
			paymentApprovalProcessor.handleApiCallException(payment, e, request.clientRequestId(), memberId);
			throw new BusinessException(PaymentErrorCode.TOSS_API_ERROR);
		}

		// 3단계: 토스 응답에 따라 처리
		if (paymentApprovalProcessor.isApprovalSuccessful(tossResponse)) {
			paymentApprovalProcessor.updatePaymentOnSuccess(payment, tossResponse, request.clientRequestId(), memberId);
		} else {
			paymentApprovalProcessor.updatePaymentOnFailure(payment, tossResponse, request.clientRequestId(), memberId);
		}

		Payment reloadedPayment = paymentApprovalProcessor.reloadPayment(payment.getId());

		return ApprovePaymentResponse.of(reloadedPayment, orderService.getOrder(memberId, reloadedPayment.getOrderId()));
	}

	@Override
	@Transactional
	public FailPaymentResponse failPayment(Long memberId, FailPaymentRequest request) {
		Payment payment = findPaymentByTossOrderId(request.tossOrderId());

		if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
			throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
		}

		orderService.updateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_FAILED, request.clientRequestId());
		payment.updatePaymentStatus(PaymentStatus.FAILED);

		savePaymentHistory(payment, PaymentStatus.FAILED, request.clientRequestId());

		OrderInfo order = orderService.getOrder(memberId, payment.getOrderId());
		return FailPaymentResponse.of(payment, order);
	}

	// ========== Private Methods ==========

	private TossApprovalResponse callTossApprovalApi(String paymentKey, String tossOrderId, BigDecimal amount, boolean isTest) {
		return tossPaymentService.approve(paymentKey, tossOrderId, amount, isTest);
	}

	private Payment findPaymentByTossOrderId(String tossOrderId) {
		return paymentRepository.findByTossOrderId(tossOrderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	private Payment savePaymentWithHistory(CreatePaymentRequest request, OrderInfo order) {
		String tossOrderId = generateTossOrderId(order.orderNumber());

		Payment payment = Payment.createPayment(
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

	private void savePaymentHistory(Payment payment, PaymentStatus paymentStatus, String idempotencyKey) {
		PaymentHistory paymentHistory = PaymentHistory.createPaymentHistory(
			payment.getId(),
			paymentStatus,
			idempotencyKey
		);
		paymentHistoryRepository.save(paymentHistory);
	}

	private String generateTossOrderId(String orderNumber) {
		String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return TOSS_ORDER_ID_PREFIX + orderNumber + "_" + uniqueId;
	}
}