package com.zeebra.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
	@NotBlank String ClientRequestId,
	Long cartId,
	Long productOptionId,
	@Valid SalesItem salesItem
) {
}