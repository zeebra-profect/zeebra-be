package com.zeebra.domain.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.zeebra.domain.cart.entity.CartItem;
import com.zeebra.domain.cart.repository.CartItemRepository;
import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
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
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private static final int NO_POINTS_USED = 0;
	private static final int RANDOM_NUMBER_BOUND = 100000;
	private static final String ORDER_NUMBER_FORMAT = "%05d";

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderItemQueryRepository orderItemQueryRepository;
	private final OrderHistoryRepository orderHistoryRepository;
	private final CartItemRepository cartItemRepository;

	@Transactional
	public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request) {
		String clientRequestId = request.ClientRequestId();

		Optional<CreateOrderResponse> existingResponse = checkIdempotency(clientRequestId, memberId);
		if (existingResponse.isPresent()) {
			return existingResponse.get();
		}

		validateOrderRequest(request.cartId(), request.salesItem(), memberId, clientRequestId);

		if (request.cartId() != null) {
			OrderResponse order = createOrderByCart(request.cartId(), memberId, clientRequestId);
			return CreateOrderResponse.of(order);
		}

		OrderResponse order = createOrderBySaleItem(request.salesItem(), memberId, clientRequestId);
		return CreateOrderResponse.of(order);
	}

	private Optional<CreateOrderResponse> checkIdempotency(String clientRequestId, Long memberId) {
		if (clientRequestId == null || clientRequestId.isBlank()) {
			log.error("[주문 생성 실패] clientRequestId가 비어있습니다. memberId: {}", memberId);
			throw new BusinessException(CommonErrorCode.INVALID_CLIENT_REQUEST_ID);
		}

		if (clientRequestId.length() > 255) {
			log.error("[주문 생성 실패] clientRequestId가 너무 깁니다. memberId: {}, length: {}",
				memberId, clientRequestId.length());
			throw new BusinessException(CommonErrorCode.INVALID_CLIENT_REQUEST_ID);
		}

		Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(clientRequestId);

		if (existingOrder.isEmpty()) {
			return Optional.empty();
		}

		Order order = existingOrder.get();
		OrderStatus orderStatus = order.getOrderStatus();

		if (orderStatus == OrderStatus.CREATED) {
			log.warn("[주문 생성 스킵] 이미 생성된 주문입니다. clientRequestId: {}, orderId: {}, status: {}",
				clientRequestId, order.getId(), orderStatus);

			List<OrderItemResponse> orderItems = orderItemQueryRepository
				.findOrderItemsByOrderId(order.getId());

			OrderResponse orderResponse = OrderResponse.of(order, orderItems);
			return Optional.of(CreateOrderResponse.of(orderResponse));
		} else {
			log.error("[주문 생성 실패] 이미 처리 중이거나 완료된 주문입니다. clientRequestId: {}, orderId: {}, status: {}",
				clientRequestId, order.getId(), orderStatus);
			throw new BusinessException(OrderErrorCode.ORDER_ALREADY_PROCESSED);
		}

	}

	private void validateOrderRequest(Long cartId, SalesItem salesItem, Long memberId, String clientRequestId) {
		if (cartId == null && salesItem == null) {
			log.error("[주문 생성 실패] 잘못된 주문 요청 - cartId와 salesItem이 모두 null입니다. memberId: {}, clientRequestId: {}",
				memberId, clientRequestId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		if (cartId != null && salesItem != null) {
			log.error(
				"[주문 생성 실패] 잘못된 주문 요청 - cartId와 salesItem을 동시에 사용할 수 없습니다. memberId: {}, cartId: {}, clientRequestId: {}",
				memberId, cartId, clientRequestId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}
	}

	private OrderResponse createOrderByCart(Long cartId, Long memberId, String idempotencyKey) {
		List<CartItem> cartItems = findCartItems(cartId, memberId);
		List<Sales> cheapestSales = findCheapestSales(cartItems);

		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);

		int totalQuantity = calculateTotalQuantity(cartItems);
		BigDecimal totalAmount = calculateTotalAmount(cheapestSales);

		Order savedOrder = createAndSaveOrder(memberId, orderNumber, now, totalQuantity, totalAmount, idempotencyKey);
		List<OrderItemResponse> orderItems = createOrderItemsFromCart(savedOrder.getId(), cartItems, cheapestSales);

		return OrderResponse.of(savedOrder, orderItems);
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

		int totalRequestedQuantity = productOptionQuantityMap.values().stream()
			.mapToInt(Integer::intValue)
			.sum();

		if (cheapestSales.size() < totalRequestedQuantity) {
			log.error("[장바구니 주문 실패] 일부 상품의 재고가 부족합니다. 요청: {}개, 조회: {}개",
				totalRequestedQuantity, cheapestSales.size());
			throw new BusinessException(OrderErrorCode.PRODUCT_OUT_OF_STOCK);
		}

		return cheapestSales;
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

		OrderHistory orderHistory = OrderHistory.createOrderHistory(
			savedOrder.getId(),
			savedOrder.getOrderStatus(),
			savedOrder.getIdempotencyKey()
		);

		orderHistoryRepository.save(orderHistory);
		return  savedOrder;
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

	private OrderResponse createOrderBySaleItem(SalesItem salesItem, Long memberId, String idempotencyKey) {
		validateSalesItem(salesItem, memberId);

		LocalDateTime now = LocalDateTime.now();
		String orderNumber = generateUniqueOrderNumber(now);

		BigDecimal totalAmount = calculateTotalAmount(salesItem);

		Order savedOrder = createAndSaveOrder(
			memberId,
			orderNumber,
			now,
			salesItem.quantity(),
			totalAmount,
			idempotencyKey
		);

		OrderItemResponse orderItemResponse = createAndSaveOrderItem(
			savedOrder.getId(),
			salesItem.salesId(),
			salesItem.price(),
			salesItem.quantity()
		);

		return OrderResponse.of(savedOrder, List.of(orderItemResponse));
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

	private BigDecimal calculateTotalAmount(SalesItem salesItem) {
		return salesItem.price().multiply(BigDecimal.valueOf(salesItem.quantity()));
	}

	private OrderItemResponse createAndSaveOrderItem(Long orderId, Long saleId, BigDecimal price, int quantity) {
		ProductInfo productInfo = orderItemQueryRepository.findProductInfoBySaleId(saleId);

		if (productInfo == null) {
			log.error("[주문 생성 실패] 상품 정보를 찾을 수 없습니다. saleId: {}", saleId);
			throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
		}

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

		return OrderItemResponse.of(
			savedOrderItem,
			productInfo.productOptionId(),
			productInfo.orderItemOptions()
		);
	}

	private String generateOrderNumber(LocalDateTime now) {
		String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String timePart = now.format(DateTimeFormatter.ofPattern("HHmm"));

		int randomNumber = (int)(Math.random() * RANDOM_NUMBER_BOUND);
		String randomPart = String.format(ORDER_NUMBER_FORMAT, randomNumber);
		return datePart + "-" + timePart + randomPart;
	}

	private String generateUniqueOrderNumber(LocalDateTime now) {
		String orderNumber;
		do {
			orderNumber = generateOrderNumber(now);
		} while (orderRepository.existsByOrderNumber(orderNumber));

		return orderNumber;
	}
}