package com.zeebra.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
	@NotBlank String ClientRequestId,
	Long cartId,
	@Valid SalesItem salesItem
) {
	public static CreateOrderRequest of(String ClientRequestId, Long cartId, SalesItem salesItem) {
		return new CreateOrderRequest(ClientRequestId, cartId, salesItem);
	}
}