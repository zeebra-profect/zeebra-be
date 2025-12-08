package com.zeebra.domain.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.zeebra.domain.cart.dto.CartItemInfo;
import com.zeebra.domain.cart.service.CartService;
import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemResponse;
import com.zeebra.domain.order.dto.OrderResponse;
import com.zeebra.domain.order.dto.ProductInfo;
import com.zeebra.domain.order.dto.ReadOrderListResponse;
import com.zeebra.domain.order.dto.SalesItem;
import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderHistory;
import com.zeebra.domain.order.entity.OrderItem;
import com.zeebra.domain.order.entity.OrderItemStatus;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.entity.OrderType;
import com.zeebra.domain.order.generator.OrderNumberGenerator;
import com.zeebra.domain.order.repository.OrderHistoryRepository;
import com.zeebra.domain.order.repository.OrderItemQueryRepository;
import com.zeebra.domain.order.repository.OrderItemRepository;
import com.zeebra.domain.order.repository.OrderQueryRepository;
import com.zeebra.domain.order.repository.OrderRepository;
import com.zeebra.domain.product.dto.OrderSalesItem;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.domain.product.service.ProductInfoService;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class OrderServiceUnitTest {
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderQueryRepository orderQueryRepository;
	@Mock
	private OrderItemRepository orderItemRepository;
	@Mock
	private OrderItemQueryRepository orderItemQueryRepository;
	@Mock
	private OrderHistoryRepository orderHistoryRepository;
	@Mock
	private ProductService productService;
	@Mock
	private SalesService salesService;
	@Mock
	private CartService cartService;
	@Mock
	private OrderNumberGenerator orderNumberGenerator;
	@Mock
	private ProductInfoService productInfoService;
	@InjectMocks
	private OrderServiceImpl orderService;

	@DisplayName("clientRequestId가 없으면 주문 생성에 실패햡니다.")
	@Test
	void createOrderWithNullClientRequestId(){
		//given
		CreateOrderRequest request = CreateOrderRequest.fromCart(null, 1L);

		// when // then
		assertThatThrownBy(() -> orderService.createOrder(1L, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("멱등성 키가 필요합니다");

		then(orderRepository).should(never()).findByIdempotencyKey(anyString());
		then(cartService).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
	}

	@DisplayName("같은 clientRequestId로 두 번 요청하면 두 번째는 새 주문을 생성하지 않습니다.")
	@Test
	void createOrderWithSameClientRequestId(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, 1L);

		Order existingOrder = org.mockito.Mockito.mock(Order.class);
		given(existingOrder.getId()).willReturn(orderId);
		given(existingOrder.getOrderStatus()).willReturn(OrderStatus.CREATED);
		given(existingOrder.getIdempotencyKey()).willReturn(clientRequestId);
		given(orderRepository.findByIdempotencyKey(clientRequestId))
			.willReturn(Optional.of(existingOrder));

		OrderItemResponse itemResponse = org.mockito.Mockito.mock(OrderItemResponse.class);
		given(orderItemQueryRepository.findOrderItemsByOrderId(orderId))
			.willReturn(List.of(itemResponse));
		//when
		CreateOrderResponse response = orderService.createOrder(memberId, request);
		//then
		OrderResponse orderResponse = response.order();
		assertThat(orderResponse.orderId()).isEqualTo(orderId);
		assertThat(orderResponse.orderStatus()).isEqualTo(OrderStatus.CREATED);

		then(orderRepository).should(never()).save(any(Order.class));
		then(cartService).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
	}

	@DisplayName("장바구니 주문 정상 케이스에서 CartService, SalesService, Repository 호출 흐름을 검증합니다.")
	@Test
	void createOrderByCartId(){
		//given
		Long memberId = 1L;
		Long cartId = 1L;
		String clientRequestId = "idem-key-001";
		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cartId);
		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		CartItemInfo cartItem = new CartItemInfo(1L, 1L, BigDecimal.valueOf(50000), 1);
		given(cartService.getCartItemsByCartId(cartId, memberId)).willReturn(List.of(cartItem));

		OrderSalesItem salesItem = OrderSalesItem.of(new Sales(1L, cartItem.productOptionId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE), 1);
		given(salesService.selectCheapestValidSales(anyMap())).willReturn(List.of(salesItem));

		given(orderRepository.save(any(Order.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		ProductInfo productInfo = ProductInfo.of(1L, salesItem.productOptionId(), "상품 이름", "thumbnail.jpeg", List.of());
		given(productInfoService.getProductInfoBySalesId(salesItem.salesId())).willReturn(productInfo);
		given(orderItemRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		//when
		CreateOrderResponse response = orderService.createOrder(memberId, request);
		// then
		assertThat(response.order().orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(response.order().totalQuantity()).isEqualTo(1);
		assertThat(response.order().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
		assertThat(response.order().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));

		then(cartService).should().getCartItemsByCartId(cartId, memberId);
		then(salesService).should().selectCheapestValidSales(anyMap());
		then(orderRepository).should().save(any(Order.class));
		then(orderHistoryRepository).should().save(any());
		then(orderItemRepository).should().save(any());
	}

	@DisplayName("즉시 구매 주문 정상 케이스에서 SalesService, Repository 호출 흐름을 검증합니다.")
	@Test
	void createOrderByProductOptionId(){
		//given
		Long memberId = 1L;
		Long productOptionId = 1L;
		String clientRequestId = "idem-key-001";
		CreateOrderRequest request = CreateOrderRequest.fromDirect(clientRequestId, productOptionId);
		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		OrderSalesItem salesItem = OrderSalesItem.of(new Sales(1L, productOptionId, BigDecimal.valueOf(50000),1, SalesStatus.ON_SALE), 1);
		given(salesService.findCheapestSalesByProductOptionId(productOptionId)).willReturn(salesItem);

		given(orderRepository.save(any(Order.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		ProductInfo productInfo = ProductInfo.of(1L, salesItem.productOptionId(), "상품 이름", "thumbnail.jpeg", List.of());
		given(productInfoService.getProductInfoBySalesId(salesItem.salesId())).willReturn(productInfo);
		given(orderItemRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		//when
		CreateOrderResponse response = orderService.createOrder(memberId, request);

		//then
		assertThat(response.order().orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(response.order().totalQuantity()).isEqualTo(1);
		assertThat(response.order().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
		assertThat(response.order().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));

		then(salesService).should().findCheapestSalesByProductOptionId(productOptionId);
		then(orderRepository).should().save(any(Order.class));
		then(orderHistoryRepository).should().save(any());
		then(orderItemRepository).should().save(any());
	}

	@DisplayName("채팅 거래 주문 정상 케이스에서 SalesService, Repository 호출 흐름을 검증합니다.")
	@Test
	void createOrderBySalesItem(){
		//given
		Long memberId = 1L;
		String clientRequestId = "idem-key-001";
		Long tradeId = 1L;
		Long salesId = 1L;
		String orderNumber = "20210901-000001";
		SalesItem salesItem = SalesItem.of(tradeId, salesId, 1, BigDecimal.valueOf(50000));
		CreateOrderRequest request = CreateOrderRequest.fromSalesItem(clientRequestId, salesItem);
		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		given(orderNumberGenerator.generate(any(LocalDateTime.class))).willReturn(orderNumber);

		given(orderRepository.save(any(Order.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		ProductInfo productInfo = ProductInfo.of(1L, 1L, "상품 이름", "thumbnail.jpeg", List.of());
		given(productInfoService.getProductInfoBySalesId(salesItem.salesId())).willReturn(productInfo);
		given(orderItemRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		//when
		CreateOrderResponse response = orderService.createOrder(memberId, request);

		//then
		assertThat(response.order().orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(response.order().totalQuantity()).isEqualTo(1);
		assertThat(response.order().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
		assertThat(response.order().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));

		then(salesService).should().validateSales(salesItem.salesId(), salesItem.quantity());
		then(orderRepository).should().save(any(Order.class));
		then(orderHistoryRepository).should().save(any());
		then(orderItemRepository).should().save(any());
	}

	@DisplayName("장바구니 주문인데 장바구니가 비어 있으면 예외가 발생합니다")
	@Test
	void createOrderByCartIdWithEmptyCart(){
		//given
		Long memberId = 1L;
		Long cartId = 1L;
		String clientRequestId = "idem-key-001";

		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cartId);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		given(cartService.getCartItemsByCartId(cartId, memberId)).willReturn(List.of());

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("유효하지 않은 주문 요청입니다");

		then(cartService).should().getCartItemsByCartId(cartId, memberId);
		then(salesService).shouldHaveNoInteractions();
		then(orderRepository).should(never()).save(any(Order.class));
	}

	@DisplayName("cartId, productOptionId, salesItem이 모두 null이면 예외가 발생합니다")
	@Test
	void createOrderWithNullCartIdAndProductOptionIdAndSalesItem(){
		//given
		String clientRequestId = "idem-key-001";
		CreateOrderRequest request = new CreateOrderRequest(clientRequestId, null, null, null);
		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");
	}
	@DisplayName("cartId와 productOptionId를 동시에 넘기면 예외가 발생합니다.")
	@Test
	void createOrderWithCartIdAndProductOptionId(){
		//given
		Long cartId = 1L;
		Long productOptionId = 1L;
		String clientRequestId = "idem-key-001";

		CreateOrderRequest request = new CreateOrderRequest(clientRequestId, cartId, productOptionId, null);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");
	}

	@DisplayName("cartId와 salesItem을 동시에 넘기면 예외가 발생합니다.")
	@Test
	void createOrderWithCartIdAndSalesItem(){
		//given
		Long cartId = 1L;
		String clientRequestId = "idem-key-001";
		SalesItem salesItem = SalesItem.of(1L, 1L, 1, BigDecimal.valueOf(50000));
		CreateOrderRequest request = new CreateOrderRequest(clientRequestId, 1L, null, salesItem);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(()-> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");
	}

	@DisplayName("productOptionId와 salesItem을 동시에 넘기면 예외가 발생합니다.")
	@Test
	void createOrderWithProductOptionIdAndSalesItems(){
		//given
		Long productOptionId = 1L;
		SalesItem salesItem = SalesItem.of(1L, 1L, 1, BigDecimal.valueOf(50000));
		String clientRequestId = "idem-key-001";
		CreateOrderRequest request = new CreateOrderRequest(clientRequestId, null, 1L, salesItem);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");
	}


	@DisplayName("장바구니 주문 시 재고가 부족하면 예외가 발생합니다.")
	@Test
	void createOrderByCartIdWithOutOfStockProductOption(){
		//given
		Long memberId = 1L;
		Long cartId = 1L;
		String clientRequestId = "idem-key-001";
		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cartId);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		CartItemInfo cartItem = new CartItemInfo(1L, 1L, BigDecimal.valueOf(50000), 1);
		given(cartService.getCartItemsByCartId(cartId, memberId)).willReturn(List.of(cartItem));
		given(salesService.selectCheapestValidSales(anyMap())).willThrow(new BusinessException(OrderErrorCode.PRODUCT_OUT_OF_STOCK, "상품 재고가 부족합니다."));

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(memberId, request)).isInstanceOf(BusinessException.class).hasMessage("상품 재고가 부족합니다.");

		then(cartService).should().getCartItemsByCartId(cartId, memberId);
		then(salesService).should().selectCheapestValidSales(anyMap());
		then(orderRepository).should(never()).save(any());
		then(orderHistoryRepository).shouldHaveNoInteractions();
		then(orderItemRepository).shouldHaveNoInteractions();
	}

	@DisplayName("즉시 구매 주문에서 수량이 0 이하면 예외가 발생합니다.")
	@Test
	void createOrderByProductOptionIdWithZeroQuantity(){
		//given
		Long memberId = 1L;
		Long productOptionId = 9999L;
		String clientRequestId = "idem-key-001";

		CreateOrderRequest request = CreateOrderRequest.fromDirect(clientRequestId, productOptionId);
		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		doNothing().when(productService).validateProductOptionId(productOptionId);
		given(salesService.findCheapestSalesByProductOptionId(productOptionId))
			.willThrow(new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND));

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(OrderErrorCode.PRODUCT_NOT_FOUND.getMessage());

		then(salesService).should().findCheapestSalesByProductOptionId(productOptionId);
		then(orderRepository).should(never()).save(any(Order.class));
		then(cartService).shouldHaveNoInteractions();
	}


	@DisplayName("채팅 거래 주문 시 수량이 0 이하면 주문 생성에 실패하고, 예외가 발생합니다.")
	@Test
	void createOrderBySalesItemWithZeroQuantity(){
		//given
		Long memberId = 1L;
		String clientRequestId = "idem-key-001";

		Long tradeId = 1L;
		Long salesId = 1L;
		SalesItem salesItem = SalesItem.of(tradeId, salesId, 0, BigDecimal.valueOf(50000));
		CreateOrderRequest request = CreateOrderRequest.fromSalesItem(clientRequestId, salesItem);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.createOrder(memberId, request)).isInstanceOf(BusinessException.class).hasMessage("주문 수량이 0 이하입니다.");

		then(cartService).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
	}

	@DisplayName("채팅 거래 주문 시 거래를 요청하는 가격이 0 미만이면 주문 생성에 실패하고, 예외가 발생합니다.")
	@Test
	void createOrderBySalesItemWithMinusPrice(){
		//given
		Long memberId = 1L;
		String clientRequestId = "idem-key-001";

		Long tradeId = 1L;
		Long salesId = 1L;
		SalesItem salesItem = SalesItem.of(tradeId, salesId, 1, BigDecimal.valueOf(-50000));
		CreateOrderRequest request = CreateOrderRequest.fromSalesItem(clientRequestId, salesItem);

		given(orderRepository.findByIdempotencyKey(clientRequestId)).willReturn(Optional.empty());
		//when //then
		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("상품 가격이 0 미만입니다.");

		then(cartService).shouldHaveNoInteractions();
		then(salesService).shouldHaveNoInteractions();
	}

	@DisplayName("기존 이력이 없으면 주문 상태를 변경하고 히스토리를 저장합니다.")
	@Test
	void updateOrderStatus(){
		//given
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		OrderStatus newStatus = OrderStatus.PAID;

		Order mockOrder = mock(Order.class);
		given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
		given(orderHistoryRepository.findByOrderIdAndIdempotencyKey(orderId, clientRequestId)).willReturn(Optional.empty());

		//when
		orderService.updateOrderStatus(orderId, newStatus, clientRequestId);

		//then
		then(orderRepository).should().findById(orderId);
		then(orderHistoryRepository).should().findByOrderIdAndIdempotencyKey(orderId, clientRequestId);
		then(mockOrder).should(times(1)).updateOrderStatus(newStatus);
		then(orderHistoryRepository).should(times(1)).save(any(OrderHistory.class));
	}

	@DisplayName("동일한 멱등키와 동일한 상태가 이미 처리되었으면 아무것도 하지 않습니다.")
	@Test
	void updateOrderStatusWithDuplicateIdempotencyKey(){
		//given
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		OrderStatus orderStatus = OrderStatus.PAID;

		Order mockOrder = mock(Order.class);
		OrderHistory mockOrderHistory = mock(OrderHistory.class);

		given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
		given(mockOrderHistory.getOrderStatus()).willReturn(orderStatus);
		given(orderHistoryRepository.findByOrderIdAndIdempotencyKey(orderId, clientRequestId)).willReturn(Optional.of(mockOrderHistory));

		//when
		orderService.updateOrderStatus(orderId, orderStatus, clientRequestId);

		//then
		then(orderRepository).should().findById(orderId);
		then(orderHistoryRepository).should().findByOrderIdAndIdempotencyKey(orderId, clientRequestId);
		then(mockOrderHistory).should().getOrderStatus();

		then(mockOrder).shouldHaveNoInteractions();
		then(orderHistoryRepository).should(never()).save(any());
	}

	@DisplayName("주문이 존재하지 않으면 예외가 발생합니다.")
	@Test
	void updateOrderStatusWithNotFoundOrder(){
		//given
		Long orderId = 9999L;
		String clientRequestId = "idem-key-001";
		OrderStatus newStatus = OrderStatus.PAID;

		given(orderRepository.findById(orderId)).willReturn(Optional.empty());
		//when //then
		assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, newStatus, clientRequestId)).isInstanceOf(
			BusinessException.class).hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage());

		then(orderRepository).should().findById(orderId);
		then(orderHistoryRepository).shouldHaveNoInteractions();
	}

	@DisplayName("동일한 멱등키로 다른 상태가 이미 처리되었으면 충돌 예외가 발생합니다.")
	@Test
	void updateOrderStatusWithDuplicateIdempotencyKeyAndDifferentStatus(){
		//given
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";
		OrderStatus storedOrderStatus = OrderStatus.CREATED;
		OrderStatus requestedOrderStatus = OrderStatus.PAID;

		Order mockOrder = mock(Order.class);
		OrderHistory mockOrderHistory = mock(OrderHistory.class);

		given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
		given(mockOrderHistory.getOrderStatus()).willReturn(storedOrderStatus);
		given(orderHistoryRepository.findByOrderIdAndIdempotencyKey(orderId, clientRequestId)).willReturn(Optional.of(mockOrderHistory));

		//when //then
		assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, requestedOrderStatus, clientRequestId)).isInstanceOf(BusinessException.class).hasMessage("동일한 멱등성 키의 요청이 이미 처리되었습니다");

		then(orderRepository).should().findById(orderId);
		then(orderHistoryRepository).should().findByOrderIdAndIdempotencyKey(orderId, clientRequestId);
		then(mockOrderHistory).should().getOrderStatus();

		then(mockOrder).shouldHaveNoInteractions();
		then(orderHistoryRepository).should(never()).save(any());
	}

	@DisplayName("memberId와 orderId가 유효하면 주문 정보를 반환합니다.")
	@Test
	void getOrder(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;
		String clientRequestId = "idem-key-001";

		LocalDateTime now = LocalDateTime.now();
		Order order = Order.createOrder(memberId, "20251119-123456789", OrderType.CART, now, 1, BigDecimal.valueOf(50000), BigDecimal.valueOf(5000), null, 0, clientRequestId);

		given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.of(order));

		//when
		OrderInfo orderInfo = orderService.getOrder(memberId, orderId);

		//then
		assertThat(orderInfo).isNotNull();
		assertThat(orderInfo.orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(orderInfo.orderNumber()).isEqualTo("20251119-123456789");
		assertThat(orderInfo.orderTime()).isEqualTo(now);
		assertThat(orderInfo.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
		assertThat(orderInfo.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
		assertThat(orderInfo.usePoint()).isEqualTo(0);

		then(orderRepository).should().findByIdAndMemberId(orderId, memberId);
	}

	@DisplayName("memberId나 orderId가 null이면 예외가 발생합니다.")
	@Test
	void getOrderWithNullMemberIdOrOrderId(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;

		//when //then
		assertThatThrownBy(() -> orderService.getOrder(null, orderId)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");

		assertThatThrownBy(() -> orderService.getOrder(memberId, null)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");

		then(orderRepository).shouldHaveNoInteractions();
	}

	@DisplayName("주어진 memeberId와 orderId에 해당하는 주문이 없으면 예외가 발생합니다.")
	@Test
	void getOrderWithNotFoundOrder(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;

		given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.getOrder(memberId, orderId)).isInstanceOf(BusinessException.class).hasMessage("주문을 찾을 수 없습니다");

		then(orderRepository).should().findByIdAndMemberId(orderId, memberId);
	}

	@DisplayName("주문과 주문상품이 정상 조회되면 OrderResponse를 반환합니다.")
	@Test
	void getOrderDetail(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;

		LocalDateTime now = LocalDateTime.now();
		Order order = Order.createOrder(memberId, "20251119-1234567890", OrderType.CART, now, 1, BigDecimal.valueOf(50000), BigDecimal.valueOf(5000), null, 0, "idem-key-001");

		given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.of(order));

		OrderItemResponse orderItemResponse = mock(OrderItemResponse.class);
		given(orderItemQueryRepository.findOrderItemsByOrderId(order.getId())).willReturn(List.of(orderItemResponse));

		//when
		OrderResponse response = orderService.getOrderDetail(memberId, orderId);

		//then
		assertThat(response).isNotNull();
		assertThat(response.orderNumber()).isEqualTo("20251119-1234567890");
		assertThat(response.orderStatus()).isEqualTo(order.getOrderStatus());
		assertThat(response.orderTime()).isEqualTo(now);
		assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
		assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
		assertThat(response.usePoint()).isEqualTo(0);
		assertThat(response.orderItems()).containsExactly(orderItemResponse);

		then(orderRepository).should().findByIdAndMemberId(orderId, memberId);
		then(orderItemQueryRepository).should().findOrderItemsByOrderId(order.getId());
	}

	@DisplayName("요청에 해당하는 주문이 없으면 예외가 발생합니다.")
	@Test
	void getOrderDetailWithNotFoundOrder(){
		//given
		Long memberId = 1L;
		Long orderId = 1L;

		given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.getOrderDetail(memberId, orderId)).isInstanceOf(BusinessException.class).hasMessage("주문을 찾을 수 없습니다");

		then(orderRepository).should().findByIdAndMemberId(orderId, memberId);
		then(orderItemQueryRepository).shouldHaveNoInteractions();
	}

	@DisplayName("주어진 memberId가 유효하면 주문 목록을 반환합니다.")
	@Test
	void getOrderList(){
		//given
		Long memberId = 1L;
		LocalDate startDate = LocalDate.of(2025, 1, 1);
		LocalDate endDate = LocalDate.of(2025, 12, 31);
		Pageable pageable = PageRequest.of(0, 10);
		LocalDate adjustedEndDate = endDate.plusDays(1);

		Order order1 = mock(Order.class);
		Order order2 = mock(Order.class);
		Order order3 = mock(Order.class);
		given(order1.getId()).willReturn(1L);
		given(order2.getId()).willReturn(2L);
		given(order3.getId()).willReturn(3L);

		Page<Order> orderPage = new PageImpl<>(List.of(order1, order2, order3), pageable, 3);

		given(orderQueryRepository.findOrdersByConditions(eq(memberId), eq(startDate), eq(adjustedEndDate), isNull(), eq(pageable))).willReturn(orderPage);

		OrderItemResponse item1 = mock(OrderItemResponse.class);
		OrderItemResponse item2 = mock(OrderItemResponse.class);
		OrderItemResponse item3 = mock(OrderItemResponse.class);
		OrderItemResponse item4 = mock(OrderItemResponse.class);
		OrderItemResponse item5 = mock(OrderItemResponse.class);
		given(orderItemQueryRepository.findOrderItemsByOrderId(order1.getId())).willReturn(List.of(item1, item4, item5));
		given(orderItemQueryRepository.findOrderItemsByOrderId(order2.getId())).willReturn(List.of(item2));
		given(orderItemQueryRepository.findOrderItemsByOrderId(order3.getId())).willReturn(List.of(item3));
		//when
		ReadOrderListResponse response = orderService.getOrderList(memberId, startDate, endDate, null, pageable);
		//then
		assertThat(response).isNotNull();
		assertThat(response.orders()).hasSize(3);
		then(orderQueryRepository).should().findOrdersByConditions(eq(memberId), eq(startDate), eq(adjustedEndDate), isNull(), eq(pageable));
		then(orderItemQueryRepository).should().findOrderItemsByOrderId(order1.getId());
		then(orderItemQueryRepository).should().findOrderItemsByOrderId(order2.getId());
		then(orderItemQueryRepository).should().findOrderItemsByOrderId(order3.getId());
	}

	@DisplayName("memberId가 비어있으면 예외가 발생합니다.")
	@Test
	void getOrderListWithEmptyMemberId(){
		//given
		Long memberId = null;
		LocalDate startDate = LocalDate.of(2025, 1, 1);
		LocalDate endDate = LocalDate.of(2025, 12, 31);
		Pageable pageable = PageRequest.of(0, 10);

		//when //then
		assertThatThrownBy(() -> orderService.getOrderList(memberId, startDate, endDate, null, pageable)).isInstanceOf(
			BusinessException.class).hasMessage("로그인이 필요합니다.");

		then(orderQueryRepository).shouldHaveNoInteractions();
		then(orderItemQueryRepository).shouldHaveNoInteractions();
	}

	@DisplayName("시작일이 종료일보다 늦으면 예외가 발생합니다.")
	@Test
	void getOrderListWithInvalidDate(){
		//given
		Long memberId = 1L;
		LocalDate startDate = LocalDate.of(2025, 1, 2);
		LocalDate endDate = LocalDate.of(2025, 1, 1);
		Pageable pageable = PageRequest.of(0, 10);

		//when //then
		assertThatThrownBy(() -> orderService.getOrderList(memberId, startDate, endDate, null, pageable)).isInstanceOf(BusinessException.class).hasMessage("시작일은 종료일보다 빠르거나 같아야 합니다.");

		then(orderQueryRepository).shouldHaveNoInteractions();
		then(orderItemQueryRepository).shouldHaveNoInteractions();
	}

	@DisplayName("주문에 속한 모든 주문 상품의 상태를 지정한 상태로 변경합니다.")
	@Test
	void updateAllOrderItemsStatus(){
		//given
		Long orderId = 1L;
		OrderItemStatus newStatus = OrderItemStatus.PAID;

		OrderItem orderItem1 = mock(OrderItem.class);
		OrderItem orderItem2 = mock(OrderItem.class);

		given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of(orderItem1, orderItem2));

		//when
		orderService.updateAllOrderItemsStatus(orderId, newStatus);

		//then
		then(orderItemRepository).should().findByOrderId(orderId);
		then(orderItem1).should().updateOrderItemStatus(newStatus);
		then(orderItem2).should().updateOrderItemStatus(newStatus);
	}

	@DisplayName("해당 주문에 주문 상품이 없으면 아무 일도 일어나지 않습니다.")
	@Test
	void updateAllOrderItemsStatusWithEmptyOrderItems(){
		//given
		Long orderId = 1L;
		OrderItemStatus newStatus = OrderItemStatus.PAID;

		given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of());

		//when
		orderService.updateAllOrderItemsStatus(orderId, newStatus);
		//then
		then(orderItemRepository).should().findByOrderId(orderId);
	}

	@DisplayName("주문 id와 주문 상품 id가 일치하면 해당 주문 상품 상태를 변경합니다.")
	@Test
	void updateOrderItemStatus(){
		//given
		Long orderId = 1L;
		Long orderItemId = 1L;
		OrderItemStatus newStatus = OrderItemStatus.PAID;

		Order order = mock(Order.class);
		OrderItem orderItem = mock(OrderItem.class);

		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
		given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
		given(orderItem.getOrderId()).willReturn(orderId);

		//when
		orderService.updateOrderItemStatus(orderId, orderItemId, newStatus);

		//then
		then(orderRepository).should().findById(orderId);
		then(orderItemRepository).should().findById(orderItemId);
		then(orderItem).should().updateOrderItemStatus(newStatus);
	}

	@DisplayName("존재하지 않는 주문에 대해 주문 상품 상태 변경을 요청하면 예외가 발생합니다.")
	@Test
	void updateOrderItemStatusWithNotFoundOrder(){
		//given
		Long orderId = 1L;
		Long orderItemId = 1L;
		OrderItemStatus newStatus = OrderItemStatus.PAID;

		given(orderRepository.findById(orderId)).willReturn(Optional.empty());
		//when //then
		assertThatThrownBy(() -> orderService.updateOrderItemStatus(orderId, orderItemId, newStatus)).isInstanceOf(BusinessException.class).hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage());

		then(orderRepository).should().findById(orderId);
		then(orderItemRepository).shouldHaveNoInteractions();
	}

	@DisplayName("주문은 존재하지만 주문 상품이 없으면 예외가 발생합니다.")
	@Test
	void updateOrderItemStatusWithNotFoundOrderItem(){
		//given
		Long orderId = 1L;
		Long orderItemId = 1L;
		OrderItemStatus newStatus = OrderItemStatus.PAID;

		Order order = mock(Order.class);
		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
		given(orderItemRepository.findById(orderItemId)).willReturn(Optional.empty());

		//when //then
		assertThatThrownBy(() -> orderService.updateOrderItemStatus(orderId, orderItemId, newStatus)).isInstanceOf(
			BusinessException.class).hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage());

		then(orderRepository).should().findById(orderId);
		then(orderItemRepository).should().findById(orderItemId);
	}

	@DisplayName("주문 상품이 다른 주문에 속해 있으면 예외가 발생합니다.")
	@Test
	void updateOrderItemStatusWithOrderItemBelongsToAnotherOrder(){
		//given
		Long orderId = 1L;
		Long orderItemId = 1L;
		OrderItemStatus newStatus = OrderItemStatus.PAID;

		Order order = mock(Order.class);
		OrderItem orderItem = mock(OrderItem.class);

		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
		given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
		given(orderItem.getOrderId()).willReturn(9999L);
		//when //then
		assertThatThrownBy(() -> orderService.updateOrderItemStatus(orderId, orderItemId, newStatus)).isInstanceOf(BusinessException.class).hasMessage(
			CommonErrorCode.INVALID_REQUEST.getMessage());

		then(orderRepository).should().findById(orderId);
		then(orderItemRepository).should().findById(orderItemId);
		then(orderItem).should(never()).updateOrderItemStatus(any());
	}
}