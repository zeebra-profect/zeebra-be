package com.zeebra.domain.order.dto;

public record CreateOrderResponse(
	OrderResponse order
) {
	public static CreateOrderResponse of(OrderResponse order) {
		return new CreateOrderResponse(order);
	}
}