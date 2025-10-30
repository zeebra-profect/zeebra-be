package com.zeebra.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.cart.entity.CartItem;
import com.zeebra.domain.cart.repository.CartItemRepository;
import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemResponse;
import com.zeebra.domain.order.dto.OrderResponse;
import com.zeebra.domain.order.dto.ProductInfo;
import com.zeebra.domain.order.dto.SalesItem;
import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderHistory;
import com.zeebra.domain.order.entity.OrderItem;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.repository.OrderHistoryRepository;
import com.zeebra.domain.order.repository.OrderItemQueryRepository;
import com.zeebra.domain.order.repository.OrderItemRepository;
import com.zeebra.domain.order.repository.OrderRepository;
import com.zeebra.domain.order.service.OrderService;
import com.zeebra.domain.product.entity.Sales;
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
	private final OrderItemRepository orderItemRepository;
	private final OrderItemQueryRepository orderItemQueryRepository;
	private final OrderHistoryRepository orderHistoryRepository;
	private final CartItemRepository cartItemRepository;

	@Transactional
	public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request) {
		String clientRequestId = request.ClientRequestId();

		Optional<CreateOrderResponse> existingResponse = findExistingOrder(clientRequestId, memberId);
		if (existingResponse.isPresent()) {
			return existingResponse.get();
		}

		validateOrderRequest(request.cartId(), request.salesItem(), memberId, clientRequestId);

		return request.cartId() != null
			? createOrderFromCart(request.cartId(), memberId, clientRequestId)
			: createOrderFromSalesItem(request.salesItem(), memberId, clientRequestId);
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
			} else {
				throw new BusinessException(CommonErrorCode.IDEMPOTENCY_CONFLICT);
			}
		}

		order.updateOrderStatus(orderStatus);
		saveOrderHistory(orderId, orderStatus, idempotencyKey);
	}

	private Optional<CreateOrderResponse> findExistingOrder(String clientRequestId, Long memberId) {
		validateIdempotencyKey(clientRequestId, memberId);

		return orderRepository.findByIdempotencyKey(clientRequestId)
			.map(order -> handleExistingOrder(order, clientRequestId));
	}

	private void validateIdempotencyKey(String clientRequestId, Long memberId) {
		if (clientRequestId == null || clientRequestId.isBlank()) {
			log.error("[주문 생성 실패] clientRequestId가 비어있습니다. memberId: {}", memberId);
			throw new BusinessException(CommonErrorCode.INVALID_CLIENT_REQUEST_ID);
		}

		if (clientRequestId.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
			log.error("[주문 생성 실패] clientRequestId가 너무 깁니다. memberId: {}, length: {}",
				memberId, clientRequestId.length());
			throw new BusinessException(CommonErrorCode.INVALID_CLIENT_REQUEST_ID);
		}
	}

	private CreateOrderResponse handleExistingOrder(Order order, String clientRequestId) {
		OrderStatus orderStatus = order.getOrderStatus();

		if (orderStatus != OrderStatus.CREATED) {
			log.error("[주문 생성 실패] 이미 처리 중이거나 완료된 주문입니다. clientRequestId: {}, orderId: {}, status: {}",
				clientRequestId, order.getId(), orderStatus);
			throw new BusinessException(OrderErrorCode.ORDER_ALREADY_PROCESSED);
		}

		log.warn("[주문 생성 스킵] 이미 생성된 주문입니다. clientRequestId: {}, orderId: {}, status: {}",
			clientRequestId, order.getId(), orderStatus);

		return createOrderResponse(order);
	}

	private CreateOrderResponse createOrderResponse(Order order) {
		List<OrderItemResponse> orderItems = orderItemQueryRepository.findOrderItemsByOrderId(order.getId());
		return CreateOrderResponse.of(OrderResponse.of(order, orderItems));
	}

	private void validateOrderRequest(Long cartId, SalesItem salesItem, Long memberId, String clientRequestId) {
		if (cartId == null && salesItem == null) {
			log.error("[주문 생성 실패] cartId와 salesItem이 모두 null입니다. memberId: {}, clientRequestId: {}",
				memberId, clientRequestId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		if (cartId != null && salesItem != null) {
			log.error("[주문 생성 실패] cartId와 salesItem을 동시에 사용할 수 없습니다. memberId: {}, cartId: {}, clientRequestId: {}",
				memberId, cartId, clientRequestId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}
	}

	private CreateOrderResponse createOrderFromCart(Long cartId, Long memberId, String idempotencyKey) {
		List<CartItem> cartItems = findCartItems(cartId, memberId);
		List<Sales> cheapestSales = findCheapestSales(cartItems);

		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);

		int totalQuantity = calculateTotalQuantity(cartItems);
		BigDecimal totalAmount = calculateTotalAmount(cheapestSales);

		Order savedOrder = createAndSaveOrder(memberId, orderNumber, now, totalQuantity, totalAmount, idempotencyKey);
		List<OrderItemResponse> orderItems = createOrderItemsFromCart(savedOrder.getId(), cartItems, cheapestSales);

		return CreateOrderResponse.of(OrderResponse.of(savedOrder, orderItems));
	}

	private CreateOrderResponse createOrderFromSalesItem(SalesItem salesItem, Long memberId, String idempotencyKey) {
		validateSalesItem(salesItem, memberId);

		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);
		BigDecimal totalAmount = calculateTotalAmount(salesItem);

		Order savedOrder = createAndSaveOrder(memberId, orderNumber, now, salesItem.quantity(), totalAmount,
			idempotencyKey);
		OrderItemResponse orderItem = createAndSaveOrderItem(savedOrder.getId(), salesItem.salesId(), salesItem.price(),
			salesItem.quantity());

		return CreateOrderResponse.of(OrderResponse.of(savedOrder, List.of(orderItem)));
	}

	private List<CartItem> findCartItems(Long cartId, Long memberId) {
		List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

		if (cartItems.isEmpty()) {
			log.error("[장바구니 주문 실패] 장바구니가 비어있습니다. cartId: {}, memberId: {}", cartId, memberId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		return cartItems;
	}

	private List<Sales> findCheapestSales(List<CartItem> cartItems) {
		Map<Long, Integer> productOptionQuantityMap = cartItems.stream()
			.collect(Collectors.toMap(
				CartItem::getProductOptionId,
				CartItem::getQuantity,
				Integer::sum
			));

		List<Sales> cheapestSales = orderItemQueryRepository.findCheapestAndOldestSales(productOptionQuantityMap);
		validateSalesAvailability(productOptionQuantityMap, cheapestSales);

		return cheapestSales;
	}

	private void validateSalesAvailability(Map<Long, Integer> productOptionQuantityMap, List<Sales> cheapestSales) {
		int totalRequestedQuantity = productOptionQuantityMap.values().stream()
			.mapToInt(Integer::intValue)
			.sum();

		if (cheapestSales.size() < totalRequestedQuantity) {
			log.error("[장바구니 주문 실패] 일부 상품의 재고가 부족합니다. 요청: {}개, 조회: {}개",
				totalRequestedQuantity, cheapestSales.size());
			throw new BusinessException(OrderErrorCode.PRODUCT_OUT_OF_STOCK);
		}
	}

	private void validateSalesItem(SalesItem salesItem, Long memberId) {
		if (salesItem.quantity() <= 0) {
			log.error("[주문 생성 실패] 주문 수량이 0 이하입니다. saleId: {}, quantity: {}, memberId: {}",
				salesItem.salesId(), salesItem.quantity(), memberId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		if (salesItem.price().compareTo(BigDecimal.ZERO) <= 0) {
			log.error("[주문 생성 실패] 상품 가격이 0 이하입니다. saleId: {}, price: {}, memberId: {}",
				salesItem.salesId(), salesItem.price(), memberId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}
	}

	private int calculateTotalQuantity(List<CartItem> cartItems) {
		return cartItems.stream()
			.mapToInt(CartItem::getQuantity)
			.sum();
	}

	private BigDecimal calculateTotalAmount(List<Sales> cheapestSales) {
		return cheapestSales.stream()
			.map(Sales::getPrice)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal calculateTotalAmount(SalesItem salesItem) {
		return salesItem.price().multiply(BigDecimal.valueOf(salesItem.quantity()));
	}

	private Order createAndSaveOrder(Long memberId, String orderNumber, LocalDateTime orderTime,
		int totalQuantity, BigDecimal totalAmount, String idempotencyKey) {
		Order order = Order.createOrder(
			memberId,
			orderNumber,
			orderTime,
			totalQuantity,
			totalAmount,
			totalAmount,
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

	private List<OrderItemResponse> createOrderItemsFromCart(Long orderId, List<CartItem> cartItems,
		List<Sales> cheapestSales) {
		return IntStream.range(0, cartItems.size())
			.mapToObj(i -> {
				CartItem cartItem = cartItems.get(i);
				Sales sale = cheapestSales.get(i);
				return createAndSaveOrderItem(orderId, sale.getId(), sale.getPrice(), cartItem.getQuantity());
			})
			.collect(Collectors.toList());
	}

	private OrderItemResponse createAndSaveOrderItem(Long orderId, Long saleId, BigDecimal price, int quantity) {
		ProductInfo productInfo = findProductInfo(saleId);

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
			log.error("[주문 생성 실패] 상품 정보를 찾을 수 없습니다. saleId: {}", saleId);
			throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
		}

		return productInfo;
	}

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
		int randomNumber = (int)(Math.random() * RANDOM_NUMBER_BOUND);
		return String.format(ORDER_NUMBER_FORMAT, randomNumber);
	}
}