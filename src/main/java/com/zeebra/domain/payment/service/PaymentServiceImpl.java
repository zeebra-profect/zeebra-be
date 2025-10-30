package com.zeebra.domain.payment.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.service.OrderService;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.entity.Payment;
import com.zeebra.domain.payment.entity.PaymentHistory;
import com.zeebra.domain.payment.repository.PaymentHistoryRepository;
import com.zeebra.domain.payment.repository.PaymentRepository;
import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	private static final String SUCCESS_URL = "/payments/success";
	private static final String FAIL_URL = "/payments/fail";
	private static final String TOSS_ORDER_ID_PREFIX = "ORD_";

	private final PaymentRepository paymentRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;
	private final OrderService orderService;

	@Transactional
	public CreatePaymentResponse createPayment(CreatePaymentRequest request, Long memberId) {
		OrderInfo order = orderService.getOrder(memberId, request.orderId());

		validateOrderCanBeProcessed(order);
		validatePaymentAmount(request, order);

		Payment payment = savePaymentWithHistory(request, order);
		updateOrderStatusToPending(order.orderId(), request.clientRequestId());

		return CreatePaymentResponse.of(payment, SUCCESS_URL, FAIL_URL);
	}


	private void validateOrderCanBeProcessed(OrderInfo order) {
		if (order.orderStatus() != OrderStatus.CREATED) {
			log.error("[결제 생성 실패] 이미 처리된 주문입니다. orderId: {}, status: {}",
				order.orderId(), order.orderStatus());
			throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
		}
	}

	private void validatePaymentAmount(CreatePaymentRequest request, OrderInfo order) {
		BigDecimal calculatedAmount = request.price().subtract(request.discount());

		if (calculatedAmount.compareTo(request.amount()) != 0) {
			log.error("[결제 생성 실패] 계산된 금액이 일치하지 않습니다. price: {}, discount: {}, amount: {}",
				request.price(), request.discount(), request.amount());
			throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
		}

		if (order.totalAmount().compareTo(request.amount()) != 0) {
			log.error("[결제 생성 실패] 주문 금액과 결제 금액이 일치하지 않습니다. orderId: {}, orderAmount: {}, paymentAmount: {}",
				order.orderId(), order.totalAmount(), request.amount());
			throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}
	}

	private Payment savePaymentWithHistory(CreatePaymentRequest request, OrderInfo order) {
		Payment payment = createPayment(request, order);
		Payment savedPayment = paymentRepository.save(payment);

		savePaymentHistory(savedPayment, request.clientRequestId());

		return savedPayment;
	}

	private Payment createPayment(CreatePaymentRequest request, OrderInfo order) {
		String tossOrderId = TOSS_ORDER_ID_PREFIX + order.orderNumber();

		return Payment.createPayment(
			request.orderId(),
			tossOrderId,
			request.orderName(),
			request.amount(),
			request.clientRequestId()
		);
	}

	private void savePaymentHistory(Payment payment, String clientRequestId) {
		PaymentHistory paymentHistory = PaymentHistory.createPaymentHistory(
			payment.getId(),
			payment.getPaymentStatus(),
			clientRequestId
		);
		paymentHistoryRepository.save(paymentHistory);
	}

	private void updateOrderStatusToPending(Long orderId, String clientRequestId) {
		orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, clientRequestId);
	}
}