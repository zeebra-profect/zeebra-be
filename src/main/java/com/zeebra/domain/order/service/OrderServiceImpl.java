package com.zeebra.domain.order.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.cart.dto.CartItemInfo;
import com.zeebra.domain.cart.service.CartService;
import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemLine;
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
import com.zeebra.domain.order.repository.OrderHistoryRepository;
import com.zeebra.domain.order.repository.OrderItemQueryRepository;
import com.zeebra.domain.order.repository.OrderItemRepository;
import com.zeebra.domain.order.repository.OrderQueryRepository;
import com.zeebra.domain.order.repository.OrderRepository;
import com.zeebra.domain.product.dto.OrderSalesItem;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private static final int NO_POINTS_USED = 0;
	private static final int RANDOM_NUMBER_BOUND = 100000;
	private static final String ORDER_NUMBER_FORMAT = "%05d";
	private static final String ORDER_NUMBER_DATE_FORMAT = "yyyyMMdd";
	private static final String ORDER_NUMBER_TIME_FORMAT = "HHmm";
	private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

	private final OrderRepository orderRepository;
	private final OrderQueryRepository orderQueryRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderItemQueryRepository orderItemQueryRepository;
	private final OrderHistoryRepository orderHistoryRepository;
	private final CartService cartService;
	private final SalesService salesService;
	private final ProductService productService;

	@Transactional
	public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request) {
		String clientRequestId = request.clientRequestId();

		Optional<CreateOrderResponse> existingResponse = findExistingOrder(clientRequestId);
		if (existingResponse.isPresent()) {
			return existingResponse.get();
		}

		request.validate();

		return request.cartId() != null
			? createOrderFromCart(request.cartId(), memberId, OrderType.CART, clientRequestId)
			: request.productOptionId() != null
				? createOrderFromProductOptionId(request.productOptionId(), memberId, OrderType.DIRECT, clientRequestId)
				: createOrderFromSalesItem(request.salesItem(), memberId, OrderType.TRADE, clientRequestId);
	}

	@Transactional(readOnly = true)
	public OrderInfo getOrder(Long memberId, Long orderId) {
		if (orderId == null || memberId == null) {
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		return OrderInfo.of(order);
	}

	@Transactional
	public void updateOrderStatus(Long orderId, OrderStatus orderStatus, String idempotencyKey) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
		Optional<OrderHistory> existingHistory = orderHistoryRepository
			.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey);

		if (existingHistory.isPresent()) {
			OrderHistory history = existingHistory.get();
			if (history.getOrderStatus() == orderStatus) {
				return;
			}
			throw new BusinessException(CommonErrorCode.IDEMPOTENCY_CONFLICT);
		}

		order.updateOrderStatus(orderStatus);
		saveOrderHistory(orderId, orderStatus, idempotencyKey);
	}

	@Transactional
	public void updateOrderItemStatus(Long orderId, Long orderItemId, OrderItemStatus orderItemStatus) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
		OrderItem item = orderItemRepository.findById(orderItemId).orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		if(!item.getOrderId().equals(orderId)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		item.updateOrderItemStatus(orderItemStatus);
	}

	@Transactional
	public void updateAllOrderItemsStatus(Long orderId, OrderItemStatus orderItemStatus) {
		List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
		//TODO: 1억개면? 뭉쳐서 한번에 업데이트 치는게 맞을까?
		orderItems.forEach(item -> item.updateOrderItemStatus(orderItemStatus));
	}

	//TODO: 파라미터에 대해서 dto로 매핑하는거에 대한 고민
	@Transactional(readOnly = true)
	public ReadOrderListResponse getOrderList(Long memberId, LocalDate startDate, LocalDate endDate, OrderStatus orderStatus, Pageable pageable) {
		if (memberId == null) {
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST, "로그인이 필요합니다.");
		}

		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST, "시작일은 종료일보다 빠르거나 같아야 합니다.");
		}

		LocalDate adjustedEndDate = endDate != null ? endDate.plusDays(1) : null;

		Page<Order> orderPage = orderQueryRepository.findOrdersByConditions(
			memberId,
			startDate,
			adjustedEndDate,
			orderStatus,
			pageable
		);

		Page<OrderResponse> orderResponsePage = orderPage.map(order -> {
			List<OrderItemResponse> orderItems = orderItemQueryRepository.findOrderItemsByOrderId(order.getId());
			return OrderResponse.of(order, orderItems);
		});

		return ReadOrderListResponse.of(orderResponsePage);
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrderDetail(Long memberId, Long orderId) {
		Order order = orderRepository.findByIdAndMemberId(orderId, memberId).orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		List<OrderItemResponse> orderItems = orderItemQueryRepository.findOrderItemsByOrderId(order.getId());

		return OrderResponse.of(order, orderItems);
	}

	@Transactional(readOnly = true)
	public List<OrderItemLine> getOrderItemLine(Long orderId) {
		List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
		if (orderItems.isEmpty()) {
			throw new BusinessException(OrderErrorCode.ORDER_HAS_NO_ITEMS);
		}
		return OrderItemLine.of(orderItems);
	}

	private Optional<CreateOrderResponse> findExistingOrder(String clientRequestId) {
		validateIdempotencyKey(clientRequestId);

		return orderRepository.findByIdempotencyKey(clientRequestId)
			.filter(order -> !order.getOrderStatus().isFailed())
			.map(order -> handleExistingOrder(order));
	}

	private void validateIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_CLIENT_REQUEST_ID);
		}

		if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
			throw new BusinessException(CommonErrorCode.INVALID_CLIENT_REQUEST_ID);
		}
	}

	private CreateOrderResponse handleExistingOrder(Order order) {
		List<OrderItemResponse> orderItems = orderItemQueryRepository.findOrderItemsByOrderId(order.getId());
		return CreateOrderResponse.of(OrderResponse.of(order, orderItems));
	}

	private CreateOrderResponse createOrderFromCart(Long cartId, Long memberId, OrderType orderType, String idempotencyKey) {
		List<CartItemInfo> cartItems = cartService.getCartItemsByCartId(cartId, memberId);
		if (cartItems.isEmpty()) {
			log.error("[장바구니 주문 실패] 장바구니가 비어있습니다. cartId: {}, memberId: {}", cartId, memberId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		Map<Long, Integer> productOptionQuantityMap = cartItems.stream()
			.collect(Collectors.toMap(
				CartItemInfo::productOptionId,
				CartItemInfo::quantity,
				Integer::sum
			));

		List<OrderSalesItem> cheapestSales = salesService.selectCheapestValidSales(productOptionQuantityMap);

		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);

		int totalQuantity = calculateTotalQuantity(cartItems);
		BigDecimal totalAmount = calculateTotalAmount(cheapestSales);

		Order savedOrder = createAndSaveOrder(memberId, orderNumber,orderType, now, totalQuantity, totalAmount, null, idempotencyKey);
		List<OrderItemResponse> orderItems = cheapestSales.stream()
			.map(sales -> createAndSaveOrderItem(
				savedOrder.getId(),
				sales.salesId(),
				sales.price(),
				sales.quantity()
			))
			.collect(Collectors.toList());
		return CreateOrderResponse.of(OrderResponse.of(savedOrder, orderItems));
	}

	private CreateOrderResponse createOrderFromProductOptionId(Long productOptionId, Long memberId, OrderType orderType, String idempotencyKey) {
		productService.validateProductOptionId(productOptionId);

		OrderSalesItem salesItem = salesService.findCheapestSalesByProductOptionId(productOptionId);

		salesItem.validateSalesItem();
		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);
		BigDecimal totalAmount = salesItem.lineAmount();

		Order savedOrder = createAndSaveOrder(memberId, orderNumber, orderType, now, salesItem.quantity(), totalAmount, null,
			idempotencyKey);
		OrderItemResponse orderItem = createAndSaveOrderItem(savedOrder.getId(), salesItem.salesId(), salesItem.price(),
			salesItem.quantity());

		return CreateOrderResponse.of(OrderResponse.of(savedOrder, List.of(orderItem)));
	}

	private CreateOrderResponse createOrderFromSalesItem(SalesItem salesItem, Long memberId, OrderType orderType, String idempotencyKey) {
		salesItem.validateSalesItem();

		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);
		BigDecimal totalAmount = salesItem.price().multiply(BigDecimal.valueOf(salesItem.quantity()));

		//todo: trade status 검증해야 함
		salesService.validateSales(salesItem.salesId(), salesItem.quantity());

		Order savedOrder = createAndSaveOrder(memberId, orderNumber, orderType, now, salesItem.quantity(), totalAmount, salesItem.tradeId(), idempotencyKey);
		OrderItemResponse orderItem = createAndSaveOrderItem(savedOrder.getId(), salesItem.salesId(), salesItem.price(),
			salesItem.quantity());

		return CreateOrderResponse.of(OrderResponse.of(savedOrder, List.of(orderItem)));
	}

	private int calculateTotalQuantity(List<CartItemInfo> cartItems) {
		return cartItems.stream()
			.mapToInt(CartItemInfo::quantity)
			.sum();
	}

	private BigDecimal calculateTotalAmount(List<OrderSalesItem> cheapestSales) {
		return cheapestSales.stream()
			.map(OrderSalesItem::lineAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Order createAndSaveOrder(Long memberId, String orderNumber, OrderType orderType, LocalDateTime orderTime,  int totalQuantity, BigDecimal totalAmount, Long tradeId, String idempotencyKey) {
		Order order = Order.createOrder(
			memberId,
			orderNumber,
			orderType,
			orderTime,
			totalQuantity,
			totalAmount,
			totalAmount,
			tradeId,
			NO_POINTS_USED,
			idempotencyKey
		);

		Order savedOrder = orderRepository.save(order);
		saveOrderHistory(savedOrder.getId(), savedOrder.getOrderStatus(), savedOrder.getIdempotencyKey());

		return savedOrder;
	}

	private void saveOrderHistory(Long orderId, OrderStatus orderStatus, String idempotencyKey) {
		OrderHistory orderHistory = OrderHistory.createOrderHistory(orderId, orderStatus, idempotencyKey);
		orderHistoryRepository.save(orderHistory);
	}

	private OrderItemResponse createAndSaveOrderItem(Long orderId, Long saleId, BigDecimal price, int quantity) {
		ProductInfo productInfo = productInfoService.getProductInfoBySalesId(saleId);

		OrderItem orderItem = OrderItem.createOrderItem(
			orderId,
			saleId,
			productInfo.productName(),
			price,
			productInfo.productThumbnail(),
			quantity,
			price.multiply(BigDecimal.valueOf(quantity))
		);

		OrderItem savedOrderItem = orderItemRepository.save(orderItem);

		return OrderItemResponse.of(savedOrderItem, productInfo.productOptionId(), productInfo.orderItemOptions());
	}

	private ProductInfo findProductInfo(Long saleId) {
		ProductInfo productInfo = orderItemQueryRepository.findProductInfoBySaleId(saleId);

		if (productInfo == null) {
			throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
		}

		return productInfo;
	}


	/*
	주문 번호 생성
	 */
	private String generateUniqueOrderNumber(LocalDateTime now) {
		String orderNumber;
		do {
			orderNumber = generateOrderNumber(now);
		} while (orderRepository.existsByOrderNumber(orderNumber));

		return orderNumber;
	}

	private String generateOrderNumber(LocalDateTime now) {
		String datePart = now.format(DateTimeFormatter.ofPattern(ORDER_NUMBER_DATE_FORMAT));
		String timePart = now.format(DateTimeFormatter.ofPattern(ORDER_NUMBER_TIME_FORMAT));
		String randomPart = generateRandomPart();

		return String.format("%s-%s%s", datePart, timePart, randomPart);
	}

	private String generateRandomPart() {
		int randomNumber = (int) (Math.random() * RANDOM_NUMBER_BOUND);
		return String.format(ORDER_NUMBER_FORMAT, randomNumber);
	}
}