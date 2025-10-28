package com.zeebra.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderStatus;

public record OrderResponse(
	Long orderId,
	String orderName,
	OrderStatus orderStatus,
	LocalDateTime orderTime,
	int totalQuantity,
	BigDecimal totalPrice,
	BigDecimal totalAmount,
	int usePoint,
	String idempotencyKey,
	List<OrderItemResponse> orderItems
) {
	public static OrderResponse of(
		Order order, List<OrderItemResponse> orderItems) {
		return new OrderResponse(
			order.getId(),
			order.getOrderNumber(),
			order.getOrderStatus(),
			order.getOrderTime(),
			order.getTotalQuantity(),
			order.getTotalPrice(),
			order.getTotalAmount(),
			order.getUsePoint(),
			order.getIdempotencyKey(),
			orderItems);
	}
}