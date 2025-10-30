package com.zeebra.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderStatus;

public record OrderInfo (
	Long orderId,
	String orderNumber,
	OrderStatus orderStatus,
	LocalDateTime orderTime,
	BigDecimal totalPrice,
	BigDecimal totalAmount,
	int usePoint
) {
	public static OrderInfo of(Order order) {
		return new OrderInfo(order.getId(), order.getOrderNumber(), order.getOrderStatus(), order.getOrderTime(), order.getTotalPrice(), order.getTotalAmount(), order.getUsePoint());
	}
}