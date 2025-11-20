package com.zeebra.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemLine;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.service.OrderService;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.entity.Payment;
import com.zeebra.domain.payment.repository.PaymentHistoryRepository;
import com.zeebra.domain.payment.repository.PaymentQueryRepository;
import com.zeebra.domain.payment.repository.PaymentRepository;
import com.zeebra.domain.payment.repository.PaymentTransactionRepository;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUnitTest {
	@Mock
	private OrderService orderService;
	@Mock
	private SalesService salesService;
	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private PaymentHistoryRepository paymentHistoryRepository;
	@Mock
	private PaymentTransactionRepository paymentTransactionRepository;
	@Mock
	private PaymentQueryRepository paymentQueryRepository;
	@Mock
	private TossPaymentService tossPaymentService;
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private PaymentServiceImpl paymentService;

	@DisplayName("주문과 판매를 검증한 결제 정보 생성 결과륾 반환합니다.")
	@Test
	void createPayment(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(50000);
		BigDecimal amount = BigDecimal.valueOf(50000);
		String orderName = "테스트 주문";

		CreatePaymentRequest request = new CreatePaymentRequest(
			orderId,
			orderName,
			price,
			BigDecimal.ZERO,
			amount,
			clientRequestId
		);

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);
		given(orderInfo.orderId()).willReturn(orderId);
		willDoNothing().given(orderInfo).validatePaymentCreatable();
		willDoNothing().given(orderInfo).validatePaymentAmount(amount);

		when(orderService.getOrder(memberId, orderId)).thenReturn(orderInfo);

		List<OrderItemLine> itemLines = List.of(
			new OrderItemLine(1L, 2)
		);
		given(orderService.getOrderItemLine(orderId)).willReturn(itemLines);

		Payment savedPayment = Payment.createPayment(
			orderId,
			"TOSS_ORDER_ID",
			orderName,
			amount,
			clientRequestId
		);
		given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

		// when
		CreatePaymentResponse response = paymentService.createPayment(request, memberId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.paymentId()).isEqualTo(savedPayment.getId());
		assertThat(response.amount().value()).isEqualByComparingTo(amount);
		assertThat(response.orderName()).isEqualTo(savedPayment.getOrderNameSnapshot());

		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should().validatePaymentAmount(amount);

		then(orderService).should().getOrderItemLine(orderId);
		then(salesService).should().validatePurchasable(itemLines);

		then(paymentRepository).should().save(any(Payment.class));
		then(orderService).should().updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, clientRequestId);
	}

	@DisplayName("주문이 결제 생성 불가능 상태라면 결제 정보 생성에 실패하고 예외가 발생합니다.")
	@Test
	void createPaymentWithInvalidOrderStatus(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(50000);
		BigDecimal amount = BigDecimal.valueOf(50000);

		CreatePaymentRequest request = new CreatePaymentRequest(
			orderId,
			"테스트 주문",
			price,
			BigDecimal.ZERO,
			amount,
			clientRequestId
		);

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);

		willThrow(new BusinessException(PaymentErrorCode.INVALID_PAYMENT_REQUEST))
			.given(orderInfo).validatePaymentCreatable();

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.INVALID_PAYMENT_REQUEST.getMessage());

		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should(never()).validatePaymentAmount(any());
		then(paymentRepository).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
	}
}