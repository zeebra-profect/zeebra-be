package com.zeebra.domain.order.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.zeebra.domain.order.entity.OrderItem;

public record OrderItemLine(
	Long salesId,
	int quantity
) {
	public static OrderItemLine of(OrderItem orderitem) {
		return new OrderItemLine(orderitem.getSaleId(), orderitem.getOrderItemQuantity());
	}

	public static List<OrderItemLine> of(List<OrderItem> items) {
		return items.stream()
			.map(item -> OrderItemLine.of(item))
			.collect(Collectors.toList());
	}
}