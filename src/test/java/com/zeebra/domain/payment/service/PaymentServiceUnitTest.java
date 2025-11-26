package com.zeebra.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

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
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.ErrorCode.SalesErrorCode;
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

	@DisplayName("주문과 판매를 검증한 결제 정보 생성 결과를 반환합니다.")
	@Test
	void createPayment(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		String idempotencyKey = OrderStatus.PAYMENT_PENDING + ":" + "idem-key-001";
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

		given(paymentRepository.findByIdempotencyKey(clientRequestId))
			.willReturn(Optional.empty());

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);
		given(orderInfo.orderId()).willReturn(orderId);

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

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should().validatePaymentAmount(amount);

		then(orderService).should().getOrderItemLine(orderId);
		then(salesService).should().validatePurchasable(itemLines);

		then(paymentRepository).should().save(any(Payment.class));
		then(orderService).should().updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, idempotencyKey);
	}

	@DisplayName("같은 멱등성 키로 재요청 시 기존 결제 정보를 반환합니다.")
	@Test
	void createPaymentIdempotentReturnsExisting(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(10000);
		BigDecimal amount = BigDecimal.valueOf(10000);
		String orderName = "테스트 주문";

		CreatePaymentRequest request = new CreatePaymentRequest(orderId, orderName, price, BigDecimal.ZERO, amount, clientRequestId);

		Payment existingPayment = Payment.createPayment(
			orderId,
			"TOSS_ORDER_ID",
			orderName,
			amount,
			clientRequestId
		);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.of(existingPayment));

		//when
		CreatePaymentResponse response = paymentService.createPayment(request, memberId);

		//then
		assertThat(response).isNotNull();
		assertThat(response.paymentId()).isEqualTo(existingPayment.getId());
		assertThat(response.amount().value()).isEqualByComparingTo(amount);
		assertThat(response.orderName()).isEqualTo(existingPayment.getOrderNameSnapshot());

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
		then(paymentRepository).should(never()).save(any(Payment.class));
	}

	@DisplayName("주문 정보의 가격 - 할인 정보가 결제금액과 일치하지 않으면 결제 정보를 생성할 수 없습니다.")
	@Test
	void createPaymentWithInvalidAmount(){
		//given
		String orderName= "테스트 주문";
		String clientRequestId = "idem-key-001";

		CreatePaymentRequest request = new CreatePaymentRequest(1L, orderName, BigDecimal.valueOf(10000), BigDecimal.ONE, BigDecimal.valueOf(10000), clientRequestId);

		given(paymentRepository.findByIdempotencyKey(clientRequestId))
			.willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, 1L)).isInstanceOf(BusinessException.class).hasMessage(
			PaymentErrorCode.INVALID_AMOUNT.getMessage());

		then(orderService).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
		then(paymentRepository).should(never()).save(any(Payment.class));
	}

	@DisplayName("주문이 존재하지 않으면 결제 정보 생성에 실패합니다.")
	@Test
	void createPaymentWithNotExistingOrder(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";

		CreatePaymentRequest request = new CreatePaymentRequest(orderId, "테스트 주문", BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(10000), clientRequestId);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		given(orderService.getOrder(memberId, orderId)).willThrow(new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(BusinessException.class).hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage());

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);

		then(orderService).should(never()).getOrderItemLine(anyLong());
		then(salesService).shouldHaveNoInteractions();
		then(paymentRepository).should(never()).save(any(Payment.class));
		then(orderService).should(never()).updateOrderStatus(anyLong(), any(), anyString());
	}

	@DisplayName("주문이 결제 생성 불가능 상태라면 결제 정보 생성에 실패하고 예외가 발생합니다.")
	@Test
	void createPaymentWithInvalidOrderStatus(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(10000);
		BigDecimal amount = BigDecimal.valueOf(10000);

		CreatePaymentRequest request = new CreatePaymentRequest(
			orderId,
			"테스트 주문",
			price,
			BigDecimal.ZERO,
			amount,
			clientRequestId
		);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);

		willThrow(new BusinessException(PaymentErrorCode.INVALID_PAYMENT_REQUEST))
			.given(orderInfo).validatePaymentCreatable();

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.INVALID_PAYMENT_REQUEST.getMessage());

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should(never()).validatePaymentAmount(any());
		then(orderService).should(never()).getOrderItemLine(anyLong());
		then(paymentRepository).should(never()).save(any(Payment.class));
		then(salesService).shouldHaveNoInteractions();
		then(orderService).should(never()).updateOrderStatus(anyLong(), any(), anyString());
	}

	@DisplayName("주문의 결제 금액과 요청의 결제 금액이 일치하지 않으면 결제 정보 생성에 실패합니다.")
	@Test
	void createPaymentWithOrderAmountMismatch(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(10000);
		BigDecimal amount = BigDecimal.valueOf(10000);

		CreatePaymentRequest request = new CreatePaymentRequest(orderId, "테스트 주문", price, BigDecimal.ZERO, amount, clientRequestId);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);
		willThrow(new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH)).given(orderInfo).validatePaymentAmount(any());

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should().validatePaymentAmount(amount);

		then(orderService).should(never()).getOrderItemLine(anyLong());
		then(salesService).shouldHaveNoInteractions();
		then(paymentRepository).should(never()).save(any(Payment.class));
		then(orderService).should(never()).updateOrderStatus(anyLong(), any(), anyString());
	}

	@DisplayName("주문 상품 정보가 없으면 결제 정보 생성에 실패합니다.")
	@Test
	void createPaymentWithNotExistOrderItem(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(10000);
		BigDecimal amount = BigDecimal.valueOf(10000);
		CreatePaymentRequest request = new CreatePaymentRequest(orderId, "테스트 상품", price, BigDecimal.ZERO, amount, clientRequestId);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);
		given(orderInfo.orderId()).willReturn(orderId);

		willThrow(new BusinessException(OrderErrorCode.ORDER_HAS_NO_ITEMS)).given(orderService).getOrderItemLine(orderId);

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(BusinessException.class).hasMessage(OrderErrorCode.ORDER_HAS_NO_ITEMS.getMessage());

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should().validatePaymentAmount(amount);
		then(orderService).should().getOrderItemLine(anyLong());
		then(salesService).shouldHaveNoInteractions();
		then(paymentRepository).should(never()).save(any(Payment.class));
		then(orderService).should(never()).updateOrderStatus(anyLong(), any(), anyString());
	}

	@DisplayName("판매 불가 상품이 포함된 경우에 결제 정보 생성에 실패합니다.")
	@Test
	void createPaymentWithNotPurchasableSales(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(10000);
		BigDecimal amount = BigDecimal.valueOf(10000);
		CreatePaymentRequest request = new CreatePaymentRequest(orderId, "테스트 상품", price, BigDecimal.ZERO, amount, clientRequestId);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);
		given(orderInfo.orderId()).willReturn(orderId);

		List<OrderItemLine> itemLines = List.of(new OrderItemLine(1L, 2));
		given(orderService.getOrderItemLine(orderId)).willReturn(itemLines);

		willThrow(new BusinessException(SalesErrorCode.SALES_NOT_AVAILABLE)).given(salesService).validatePurchasable(itemLines);

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(BusinessException.class).hasMessage(SalesErrorCode.SALES_NOT_AVAILABLE.getMessage());

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should().validatePaymentAmount(amount);
		then(orderService).should().getOrderItemLine(anyLong());
		then(salesService).should().validatePurchasable(itemLines);
		then(paymentRepository).should(never()).save(any(Payment.class));
		then(orderService).should(never()).updateOrderStatus(anyLong(), any(), anyString());
	}

	@DisplayName("결제 저장 중 예외가 발생하면 결제 생성이 중단되고 주문 상태는 변경되지 않습니다.")
	@Test
	void createPaymentSaveFailed(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		BigDecimal price = BigDecimal.valueOf(10000);
		BigDecimal amount = BigDecimal.valueOf(10000);
		CreatePaymentRequest request = new CreatePaymentRequest(orderId, "테스트 상품", price, BigDecimal.ZERO, amount, clientRequestId);

		given(paymentRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		OrderInfo orderInfo = mock(OrderInfo.class);
		given(orderService.getOrder(memberId, orderId)).willReturn(orderInfo);
		given(orderInfo.orderId()).willReturn(orderId);

		List<OrderItemLine> itemLines = List.of(new OrderItemLine(1L, 2));
		given(orderService.getOrderItemLine(orderId)).willReturn(itemLines);

		willThrow(new DataAccessException("DB error") {})
			.given(paymentRepository).save(any(Payment.class));

		//when //then
		assertThatThrownBy(() -> paymentService.createPayment(request, memberId)).isInstanceOf(DataAccessException.class).hasMessage("DB error");

		then(paymentRepository).should().findByIdempotencyKey(clientRequestId);
		then(orderService).should().getOrder(memberId, orderId);
		then(orderInfo).should().validatePaymentCreatable();
		then(orderInfo).should().validatePaymentAmount(amount);
		then(orderService).should().getOrderItemLine(anyLong());
		then(salesService).should().validatePurchasable(itemLines);
		then(paymentRepository).should().save(any(Payment.class));
		then(orderService).should(never()).updateOrderStatus(anyLong(), any(), anyString());
	}
}