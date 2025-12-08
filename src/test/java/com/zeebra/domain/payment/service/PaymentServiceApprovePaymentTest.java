// package com.zeebra.domain.payment.service;
//
// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.BDDMockito.*;
//
// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.time.OffsetDateTime;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
//
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.zeebra.domain.order.dto.OrderInfo;
// import com.zeebra.domain.order.dto.OrderItemResponse;
// import com.zeebra.domain.order.dto.OrderResponse;
// import com.zeebra.domain.order.entity.OrderItemStatus;
// import com.zeebra.domain.order.entity.OrderStatus;
// import com.zeebra.domain.order.service.OrderService;
// import com.zeebra.domain.payment.dto.ApprovePaymentRequest;
// import com.zeebra.domain.payment.dto.ApprovePaymentResponse;
// import com.zeebra.domain.payment.dto.TossApprovalResponse;
// import com.zeebra.domain.payment.dto.TossError;
// import com.zeebra.domain.payment.dto.TossPayment;
// import com.zeebra.domain.payment.entity.Payment;
// import com.zeebra.domain.payment.entity.PaymentMethod;
// import com.zeebra.domain.payment.entity.PaymentStatus;
// import com.zeebra.domain.payment.entity.PaymentTransaction;
// import com.zeebra.domain.payment.entity.PaymentTransactionStatus;
// import com.zeebra.domain.payment.entity.PaymentTransactionType;
// import com.zeebra.domain.payment.repository.PaymentHistoryRepository;
// import com.zeebra.domain.payment.repository.PaymentQueryRepository;
// import com.zeebra.domain.payment.repository.PaymentRepository;
// import com.zeebra.domain.payment.repository.PaymentTransactionRepository;
// import com.zeebra.domain.product.service.SalesService;
// import com.zeebra.global.ErrorCode.PaymentErrorCode;
// import com.zeebra.global.exception.BusinessException;
//
// @ExtendWith(MockitoExtension.class)
// public class PaymentServiceApprovePaymentTest{
//
// 	private static final Long MEMBER_ID = 1L;
// 	private static final Long ORDER_ID = 1L;
// 	private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
// 	private static final String CLIENT_REQUEST_ID = "idem-key-001";
// 	private static final String PAYMENT_KEY = "payment-key-001";
// 	private static final String TOSS_ORDER_ID = "toss-order-id";
//
// 	@Mock
// 	private OrderService orderService;
// 	@Mock
// 	private SalesService salesService;
// 	@Mock
// 	private PaymentRepository paymentRepository;
// 	@Mock
// 	private PaymentHistoryRepository paymentHistoryRepository;
// 	@Mock
// 	private PaymentTransactionRepository paymentTransactionRepository;
// 	@Mock
// 	private PaymentQueryRepository paymentQueryRepository;
// 	@Mock
// 	private TossPaymentService tossPaymentService;
// 	@Mock
// 	private ObjectMapper objectMapper;
//
// 	@InjectMocks
// 	private PaymentServiceImpl paymentService;
//
// 	@DisplayName("결제 승인 성공  Payment/Order 상태와 이력이 정상적으로 갱신됩니다.")
// 	@Test
// 	void approvePayment(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
// 		String approvedAtStr = "2025-11-27T10:00:01+09:00";
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse(PaymentMethod.CARD.name(), approvedAtStr);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(1L, MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)). willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
//
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should().isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentHistoryRepository).should(times(2)).save(any());
// 		then(paymentTransactionRepository).should().save(any());
// 		then(transaction).should().update(
// 			eq(PaymentTransactionStatus.SUCCEEDED),
// 			anyMap(),
// 			eq(200),
// 			eq(0),
// 			isNull()
// 		);
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
//
// 		then(paymentRepository).should(atLeastOnce()).findById(pendingPayment.getId());
// 		then(paymentHistoryRepository).should(atLeast(2)).save(any());
// 		then(paymentTransactionRepository).should(atLeastOnce()).save(any());
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAID,
// 			CLIENT_REQUEST_ID + "-order-paid"
// 		);
// 		then(orderService).should().updateAllOrderItemsStatus(
// 			pendingPayment.getOrderId(),
// 			OrderItemStatus.PAID
// 		);
//
// 		then(orderService).should().getOrder(MEMBER_ID, pendingPayment.getOrderId());
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().confirmSales(orderItems);
// 	}
//
// 	@DisplayName("토스 승인 API에서 TossError 객체가 오면 Payment/Order 상태를 실패로 갱신합니다.")
// 	@Test
// 	void approvePaymentFail(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
// 		TossApprovalResponse tossApprovalResponse = createTossFailureResponse();
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(1L, MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)). willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should().isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentHistoryRepository).should(times(2)).save(any());
// 		then(paymentTransactionRepository).should().save(any());
// 		then(transaction).should().update(
// 			eq(PaymentTransactionStatus.FAILED),
// 			anyMap(),
// 			eq(200),
// 			eq(0),
// 			isNull()
// 		);
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
//
// 		then(paymentRepository).should(atLeastOnce()).findById(pendingPayment.getId());
// 		then(paymentHistoryRepository).should(atLeast(2)).save(any());
// 		then(paymentTransactionRepository).should(atLeastOnce()).save(any());
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAYMENT_FAILED,
// 			CLIENT_REQUEST_ID + "-order-failed"
// 		);
//
// 		then(orderService).should().getOrder(MEMBER_ID, pendingPayment.getOrderId());
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().cancelSales(orderItems);
// 	}
//
// 	@DisplayName("결제 정보가 없는 결제 승인 요청은 예외가 발생합니다.")
// 	@Test
// 	void approvePaymentNotFoundPayment(){
// 		//given
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		//when //then
// 		assertThatThrownBy(() -> paymentService.approvePayment(request, MEMBER_ID)).isInstanceOf(BusinessException.class).hasMessage(
// 			PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
// 	}
//
// 	@DisplayName("결제 승인 요청을 한 사용자와 결제 정보 소유자가 다르면 예외가 발생합니다.")
// 	@Test
// 	void approvePaymentIsNotOwnedByMember(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(1L, MEMBER_ID)).willReturn(false);
//
// 		//when //then
// 		assertThatThrownBy(() -> paymentService.approvePayment(request, MEMBER_ID)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS.getMessage());
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 	}
//
// 	@DisplayName("결제 승인 요청 중 결제 정보를 찾지 못하면 예외가 발생합니다.")
// 	@Test
// 	void approvePaymentNotFoundPaymentById(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse(null, null);
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.empty());
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(tossApprovalResponse);
//
// 		//when
// 		assertThatThrownBy(() -> paymentService.approvePayment(request, MEMBER_ID)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
//
// 		//then
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should().isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
//
// 		then(paymentRepository).should().findById(pendingPayment.getId());
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
// 	}
//
// 	@DisplayName("updatePaymentTransactionStatus에서 트랜잭션이 없으면 PAYMENT_NOT_FOUND 예외를 던집니다.")
// 	@Test
// 	void approvePayment_transactionNotFound_throwsException() {
// 		// given
// 		Payment pendingPayment = pendingPayment();
//
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.empty());
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		String approvedAtStr = "2025-11-27T10:00:01+09:00";
// 		TossApprovalResponse tossResponse = createTossSuccessResponse(PaymentMethod.CARD.name(), approvedAtStr);
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(tossResponse);
//
// 		//when
// 		assertThatThrownBy(() -> paymentService.approvePayment(request, MEMBER_ID)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
//
// 		// then
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should().isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentRepository).should().findById(pendingPayment.getId());
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
// 	}
//
// 	@DisplayName("status가 DONE이 아니면 승인되지 않은 것으로 처리됩니다.")
// 	@Test
// 	void approvePayment_statusNotDone() {
//
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest req = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId())).willReturn(Optional.of(transaction));
//
// 		TossPayment tossPayment = mock(TossPayment.class);
// 		given(tossPayment.status()).willReturn("FAILED");
//
// 		TossApprovalResponse tossResponse = TossApprovalResponse.ofSuccess(tossPayment);
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(tossResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(createOrderItemResponse());
//
// 		// when
// 		ApprovePaymentResponse response = paymentService.approvePayment(req, MEMBER_ID);
//
// 		// then
// 		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
// 	}
//
// 	@DisplayName("토스 응답의 method 값이 null이면 PaymentMethod는 기존 값 그대로 유지됩니다.")
// 	@Test
// 	void approvePaymentWithNullMethod(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		String approvedAtStr = "2025-11-27T10:00:01+09:00";
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse(null, approvedAtStr);
//
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
//
// 		assertThat(pendingPayment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
//
// 		assertThat(pendingPayment.getPaymentMethod()).isEqualTo(null);
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should()
// 			.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentRepository).should(atLeastOnce()).findById(pendingPayment.getId());
// 		then(paymentHistoryRepository).should(atLeast(2)).save(any());
// 		then(paymentTransactionRepository).should(atLeastOnce()).save(any());
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAID,
// 			CLIENT_REQUEST_ID + "-order-paid"
// 		);
// 		then(orderService).should().updateAllOrderItemsStatus(
// 			pendingPayment.getOrderId(),
// 			OrderItemStatus.PAID
// 		);
//
// 		then(orderService).should().getOrder(MEMBER_ID, pendingPayment.getOrderId());
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().confirmSales(orderItems);
// 	}
//
// 	@DisplayName("토스 응답의 method 값이 잘못된 문자열이면 PaymentMethod는 기존 값 그대로 유지됩니다.")
// 	@Test
// 	void approvePaymentWithInvalidMethod() {
// 		// given
// 		Payment pendingPayment = pendingPayment();
// 		String approvedAtStr = "2025-11-27T10:00:01+09:00";
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse("INVALID_METHOD", approvedAtStr);
//
// 		ApprovePaymentRequest request =
// 			new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID))
// 			.willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID))
// 			.willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT))
// 			.willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		// when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		// then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
//
// 		assertThat(pendingPayment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
//
// 		assertThat(pendingPayment.getPaymentMethod()).isEqualTo(null);
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should()
// 			.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentRepository).should(atLeastOnce()).findById(pendingPayment.getId());
// 		then(paymentHistoryRepository).should(atLeast(2)).save(any());
// 		then(paymentTransactionRepository).should(atLeastOnce()).save(any());
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAID,
// 			CLIENT_REQUEST_ID + "-order-paid"
// 		);
// 		then(orderService).should().updateAllOrderItemsStatus(
// 			pendingPayment.getOrderId(),
// 			OrderItemStatus.PAID
// 		);
//
// 		then(orderService).should().getOrder(MEMBER_ID, pendingPayment.getOrderId());
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().confirmSales(orderItems);
// 	}
//
// 	@DisplayName("토스 응답의 approvedAt 값이 null이면 승인 시각은 저장되지 않습니다.")
// 	@Test
// 	void approvePaymentWithNullApprovedAt_doesNotUpdateApprovedAt() {
// 		// given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId())).willReturn(Optional.of(transaction));
//
// 		// TossPayment: status = DONE, approvedAt = null
// 		TossPayment tossPayment = mock(TossPayment.class);
// 		given(tossPayment.status()).willReturn("DONE");
// 		given(tossPayment.method()).willReturn("CARD");
// 		given(tossPayment.approvedAt()).willReturn(null);
//
// 		TossApprovalResponse tossResponse = TossApprovalResponse.ofSuccess(tossPayment);
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(tossResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		// when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		// then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
//
// 		assertThat(pendingPayment.getApprovedAt()).isNull();
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAID,
// 			CLIENT_REQUEST_ID + "-order-paid"
// 		);
// 		then(orderService).should().updateAllOrderItemsStatus(
// 			pendingPayment.getOrderId(),
// 			OrderItemStatus.PAID
// 		);
// 	}
//
// 	@DisplayName("토스 응답의 approvedAt 값에 Offset이 포함된 경우 LocalDateTime이 올바르게 파싱됩니다.")
// 	@Test
// 	void approvePaymentWithOffsetApprovedAt() {
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		String approvedAtStr = "2025-11-27T10:00:01+09:00";
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse("CARD", approvedAtStr);
//
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId())).willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT))
// 			.willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		// when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
//
// 		assertThat(pendingPayment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
// 		assertThat(pendingPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
//
// 		LocalDateTime expected = OffsetDateTime.parse(approvedAtStr).toLocalDateTime();
//
// 		assertThat(pendingPayment.getApprovedAt()).isEqualTo(expected);
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should()
// 			.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentRepository).should(atLeastOnce()).findById(pendingPayment.getId());
// 		then(paymentHistoryRepository).should(atLeast(2)).save(any());
// 		then(paymentTransactionRepository).should(atLeastOnce()).save(any());
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAID,
// 			CLIENT_REQUEST_ID + "-order-paid"
// 		);
// 		then(orderService).should().updateAllOrderItemsStatus(
// 			pendingPayment.getOrderId(),
// 			OrderItemStatus.PAID
// 		);
//
// 		then(orderService).should().getOrder(MEMBER_ID, pendingPayment.getOrderId());
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().confirmSales(orderItems);
// 	}
//
// 	@DisplayName("토스 응답의 approvedAt 값에 Offset 정보가 없으면 LocalDateTime 그대로 저장됩니다.")
// 	@Test
// 	void approvePaymentWithLocalDateTimeApprovedAt() {
// 		// given
// 		Payment pendingPayment = pendingPayment();
// 		String approvedAtStr = "2025-11-27T10:00:01";
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse("CARD", approvedAtStr);
//
// 		ApprovePaymentRequest request =
// 			new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID))
// 			.willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID))
// 			.willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT))
// 			.willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		// when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		// then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
// 		assertThat(pendingPayment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
//
// 		LocalDateTime expected = LocalDateTime.parse(approvedAtStr);
// 		assertThat(pendingPayment.getApprovedAt()).isEqualTo(expected);
//
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAID,
// 			CLIENT_REQUEST_ID + "-order-paid"
// 		);
// 		then(orderService).should().updateAllOrderItemsStatus(
// 			pendingPayment.getOrderId(),
// 			OrderItemStatus.PAID
// 		);
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().confirmSales(orderItems);
// 	}
//
// 	@DisplayName("토스 응답의 approvedAt 값이 잘못된 포맷이면 승인 시간은 저장되지 않습니다.")
// 	@Test
// 	void approvePaymentWithInvalidApprovedAtFormat(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		String InvalidApprovedAtStr = "2025";
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse("CARD", InvalidApprovedAtStr);
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID))
// 			.willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID))
// 			.willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT))
// 			.willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(createOrderItemResponse());
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
// 		assertThat(pendingPayment.getApprovedAt()).isNull();
//
// 		then(orderService).should().updateOrderStatus(pendingPayment.getOrderId(), OrderStatus.PAID, CLIENT_REQUEST_ID + "-order-paid");
// 		then(orderService).should().updateAllOrderItemsStatus(pendingPayment.getOrderId(), OrderItemStatus.PAID);
// 		then(salesService).should().reserveSales(anyList());
// 		then(salesService).should().confirmSales(anyList());
// 	}
//
// 	@DisplayName("Toss 응답에 TossError가 없으면 '결제 정보 없음'을 실패 사유로 사용합니다.")
// 	@Test
// 	void approvePaymentTossErrorIsNull(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		TossApprovalResponse tossApprovalResponse = TossApprovalResponse.ofFail(null);
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID))
// 			.willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID))
// 			.willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId())).willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT))
// 			.willReturn(tossApprovalResponse);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
//
// 		assertThat(pendingPayment.getFailureReason()).isEqualTo("결제 정보 없음");
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should().isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentHistoryRepository).should(times(2)).save(any());
// 		then(paymentTransactionRepository).should().findByPaymentId(pendingPayment.getId());
// 		then(paymentTransactionRepository).should().save(any());
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAYMENT_FAILED,
// 			CLIENT_REQUEST_ID + "-order-failed"
// 		);
//
// 		then(orderService).should(never())
// 			.updateAllOrderItemsStatus(pendingPayment.getOrderId(), OrderItemStatus.PAID);
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().cancelSales(orderItems);
// 	}
//
// 	@DisplayName("토스 승인 API 호출에 실패하면 Payment/Order를 실패로 갱신하고 TOSS_API_ERROR 예외를 던집니다.")
// 	@Test
// 	void approvePaymentTossApiError(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(1L, MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId()))
// 			.willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = createTransaction(pendingPayment.getId());
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		RuntimeException tossException = new RuntimeException("Toss API down");
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willThrow(tossException);
//
// 		//when
// 		assertThatThrownBy(() -> paymentService.approvePayment(request, MEMBER_ID))
// 			.isInstanceOf(BusinessException.class)
// 			.hasMessage(PaymentErrorCode.TOSS_API_ERROR.getMessage());
//
// 		//then
// 		then(tossPaymentService).should().approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT);
//
// 		assertThat(pendingPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
// 		assertThat(pendingPayment.getFailureReason())
// 			.contains("API 호출 실패")
// 			.contains("Toss API down");
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should().isPaymentOwnedByMember(1L, MEMBER_ID);
// 		then(paymentHistoryRepository).should(times(2)).save(any());
// 		then(paymentTransactionRepository).should().findByPaymentId(pendingPayment.getId());
// 		assertThat(transaction.getPaymentTransactionStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAYMENT_FAILED,
// 			CLIENT_REQUEST_ID + "-order-api-failed"
// 		);
//
// 		then(orderService).should(never())
// 			.updateAllOrderItemsStatus(pendingPayment.getOrderId(), OrderItemStatus.PAID);
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().cancelSales(orderItems);
// 	}
//
// 	@DisplayName("토스 승인 API가 null을 반환하면 Payment/Order를 실패로 갱신하고 빈 응답 맵으로 처리합니다.")
// 	@Test
// 	void approvePaymentTossApiNullResponse(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
//
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
//
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId())).willReturn(Optional.of(transaction));
//
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(null);
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentId()).isEqualTo(pendingPayment.getId());
// 		assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
//
// 		assertThat(pendingPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
// 		assertThat(pendingPayment.getFailureReason()).isEqualTo("응답 없음");
//
// 		ArgumentCaptor<Map<String, Object>> responseMapCaptor =
// 			ArgumentCaptor.forClass(Map.class);
//
// 		then(paymentRepository).should().findByTossOrderId(TOSS_ORDER_ID);
// 		then(paymentQueryRepository).should()
// 			.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID);
// 		then(paymentHistoryRepository).should(times(2)).save(any());
// 		then(paymentTransactionRepository).should().save(any());
//
// 		then(transaction).should().update(
// 			eq(PaymentTransactionStatus.FAILED),
// 			responseMapCaptor.capture(),
// 			anyInt(),
// 			anyInt(),
// 			isNull()
// 		);
//
// 		assertThat(responseMapCaptor.getValue()).isEmpty();
//
// 		then(orderService).should().updateOrderStatus(
// 			pendingPayment.getOrderId(),
// 			OrderStatus.PAYMENT_FAILED,
// 			CLIENT_REQUEST_ID + "-order-failed"
// 		);
//
// 		then(orderService).should().getOrder(MEMBER_ID, pendingPayment.getOrderId());
// 		then(salesService).should().reserveSales(orderItems);
// 		then(salesService).should().cancelSales(orderItems);
// 	}
//
// 	@DisplayName("TossPayment 변환 중 예외가 발생해도 승인 플로우는 예외 없이 진행됩니다.")
// 	@Test
// 	void approvePaymentTossPaymentConversionError(){
// 		//given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request = new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId())).willReturn(Optional.of(transaction));
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		TossApprovalResponse tossApprovalResponse = createTossSuccessResponse("CARD", "2025-11-27T10:00:01");
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT)).willReturn(tossApprovalResponse);
//
// 		given(objectMapper.convertValue(any(TossPayment.class), eq(Map.class))).willThrow(new IllegalArgumentException("convert payment fail"));
//
// 		//when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		//then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
// 		then(objectMapper).should().convertValue(tossApprovalResponse.tossPayment(), Map.class);
// 		then(transaction).should().update(
// 			eq(PaymentTransactionStatus.SUCCEEDED),
// 			anyMap(),
// 			eq(200),
// 			eq(0),
// 			isNull()
// 		);
// 	}
//
// 	@DisplayName("TossError 변환 중 예외가 발생하면 fallback 에러 정보가 저장됩니다.")
// 	@Test
// 	void approvePaymentTossErrorConversionError() {
// 		// given
// 		Payment pendingPayment = pendingPayment();
// 		ApprovePaymentRequest request =
// 			new ApprovePaymentRequest(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT, CLIENT_REQUEST_ID);
//
// 		given(paymentRepository.findByTossOrderId(TOSS_ORDER_ID)).willReturn(Optional.of(pendingPayment));
// 		given(paymentQueryRepository.isPaymentOwnedByMember(pendingPayment.getId(), MEMBER_ID)).willReturn(true);
// 		given(paymentRepository.findById(pendingPayment.getId())).willReturn(Optional.of(pendingPayment));
//
// 		PaymentTransaction transaction = mock(PaymentTransaction.class);
// 		given(paymentTransactionRepository.findByPaymentId(pendingPayment.getId()))
// 			.willReturn(Optional.of(transaction));
//
// 		TossError tossError = new TossError("ALREADY_PROCESSED", "이미 처리된 결제입니다");
// 		TossApprovalResponse tossResponse = TossApprovalResponse.ofFail(tossError);
// 		given(tossPaymentService.approve(PAYMENT_KEY, TOSS_ORDER_ID, AMOUNT))
// 			.willReturn(tossResponse);
//
// 		given(objectMapper.convertValue(tossResponse.tossError(), Map.class)).willThrow(new IllegalArgumentException("convert error fail"));
//
// 		OrderInfo orderInfo = mock(OrderInfo.class);
// 		given(orderService.getOrder(MEMBER_ID, ORDER_ID)).willReturn(orderInfo);
//
// 		OrderResponse orderDetail = mock(OrderResponse.class);
// 		List<OrderItemResponse> orderItems = createOrderItemResponse();
// 		given(orderService.getOrderDetail(ORDER_ID, MEMBER_ID)).willReturn(orderDetail);
// 		given(orderDetail.orderItems()).willReturn(orderItems);
//
// 		// when
// 		ApprovePaymentResponse response = paymentService.approvePayment(request, MEMBER_ID);
//
// 		// then
// 		assertThat(response).isNotNull();
// 		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
//
// 		then(objectMapper).should().convertValue(tossResponse.tossError(), Map.class);
//
// 		@SuppressWarnings("unchecked")
// 		ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
//
// 		then(transaction).should().update(
// 			eq(PaymentTransactionStatus.FAILED),
// 			mapCaptor.capture(),
// 			eq(200),
// 			eq(0),
// 			isNull()
// 		);
//
// 		Map<String, Object> captured = mapCaptor.getValue();
// 		assertThat(captured.get("success")).isEqualTo(false);
// 		@SuppressWarnings("unchecked")
// 		Map<String, Object> errorMap = (Map<String, Object>) captured.get("error");
// 		assertThat(errorMap.get("code")).isEqualTo("ALREADY_PROCESSED");
// 		assertThat(errorMap.get("message")).isEqualTo("이미 처리된 결제입니다");
// 	}
//
// 	private Payment pendingPayment() {
// 		return Payment.builder()
// 			.id(1L)
// 			.orderId(ORDER_ID)
// 			.paymentKey(PAYMENT_KEY)
// 			.tossOrderId(TOSS_ORDER_ID)
// 			.orderNameSnapshot("테스트 주문")
// 			.paymentAmount(AMOUNT)
// 			.paymentMethod(null)
// 			.paymentStatus(PaymentStatus.PENDING)
// 			.approvedAt(null)
// 			.failureReason(null)
// 			.tossResponse(new HashMap<>())
// 			.idempotencyKey("idem-key-002")
// 			.build();
// 	}
//
// 	private Payment approvingPayment(String paymentKey, String tossOrderId) {
// 		return Payment.builder()
// 			.id(1L)
// 			.orderId(ORDER_ID)
// 			.paymentKey(paymentKey)
// 			.tossOrderId(tossOrderId)
// 			.orderNameSnapshot("테스트 주문")
// 			.paymentAmount(AMOUNT)
// 			.paymentMethod(PaymentMethod.CARD)
// 			.paymentStatus(PaymentStatus.APPROVING)
// 			.approvedAt(null)
// 			.failureReason(null)
// 			.tossResponse(new HashMap<>())
// 			.idempotencyKey("idem-key-002")
// 			.build();
// 	}
//
// 	private Payment approvedPayment(String paymentKey, String tossOrderId) {
// 		return Payment.builder()
// 			.id(1L)
// 			.orderId(ORDER_ID)
// 			.paymentKey(paymentKey)
// 			.tossOrderId(tossOrderId)
// 			.orderNameSnapshot("테스트 주문")
// 			.paymentAmount(AMOUNT)
// 			.paymentMethod(PaymentMethod.CARD)
// 			.paymentStatus(PaymentStatus.APPROVED)
// 			.approvedAt(null)
// 			.failureReason(null)
// 			.tossResponse(new HashMap<>())
// 			.idempotencyKey("idem-key-002")
// 			.build();
// 	}
//
// 	private TossApprovalResponse createTossSuccessResponse(
// 		String paymentMethod, String approvedAtStr
// 	) {
// 		TossPayment tossPayment = new TossPayment(
// 			"2022-11-16",         // version
// 			PAYMENT_KEY,           // paymentKey
// 			"NORMAL",             // type
// 			TOSS_ORDER_ID,          // tossOrderId
// 			"테스트 주문",          // orderName
// 			"test-mId",           // mId
// 			"KRW",                // currency
// 			paymentMethod,               // method
// 			AMOUNT,               // totalAmount
// 			AMOUNT,               // balanceAmount
// 			"DONE",               // status
// 			"2025-11-27T10:00:00Z", // requestedAt
// 			approvedAtStr,			// approvedAt
// 			false,                // useEscrow
// 			AMOUNT,               // suppliedAmount
// 			BigDecimal.ZERO,      // vat
// 			false,                // cultureExpense
// 			BigDecimal.ZERO,      // taxFreeAmount
// 			BigDecimal.ZERO,      // taxExemptionAmount
// 			null,                 // cancel
// 			true,                 // isPartialCancelable
// 			new TossPayment.Card( // Card
// 				AMOUNT,
// 				"3K",             // issuerCode
// 				"3K",             // acquirerCode
// 				"1111-2222-3333-4444",
// 				0,
// 				"12345678",
// 				false,
// 				"신용",            // cardType
// 				"개인",            // ownerType
// 				"READY",
// 				false,
// 				null
// 			),
// 			null,                 // secret
// 			null,                 // virtualAccount
// 			null,                 // mobilePhone
// 			null,                 // giftCertificate
// 			null,                 // transfer
// 			Map.of("test", "value"), // metadata
// 			new TossPayment.Receipt("https://test-receipt"), // receipt
// 			null,                 // checkout
// 			null,                 // easyPay
// 			"KR",                 // country
// 			null,                 // failure
// 			null,                 // cashReceipt
// 			List.of(),            // cashReceipts
// 			null                  // discount
// 		);
//
// 		return TossApprovalResponse.ofSuccess(tossPayment);
// 	}
//
// 	private TossApprovalResponse createTossFailureResponse() {
//
// 		TossError tossError =  new TossError("ALREADY_PROCESSED", "이미 처리된 결제입니다");
//
// 		return TossApprovalResponse.ofFail(tossError);
// 	}
//
// 	private PaymentTransaction createTransaction(Long id) {
// 		return PaymentTransaction.builder()
// 			.paymentId(id)
// 			.paymentTransactionType(PaymentTransactionType.PAYMENT)
// 			.retryCount(0)
// 			.parentTransactionId(null)
// 			.build();
// 	}
//
// 	private List<OrderItemResponse> createOrderItemResponse() {
// 		return List.of(
// 			new OrderItemResponse(
// 				1L,
// 				1L,
// 				1L,
// 				"테스트 상품",
// 				null,
// 				AMOUNT,
// 				2,
// 				AMOUNT,
// 				OrderItemStatus.CREATED,
// 				List.of()
// 			)
// 		);
// 	}
//
// }