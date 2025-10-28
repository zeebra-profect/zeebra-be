package com.zeebra.domain.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SalesItem(
	@NotNull Long salesId,
	@Positive int quantity,
	@DecimalMin("0.0") BigDecimal price) {
	public static SalesItem of(Long salesId, int count, BigDecimal price) {
		return new SalesItem(salesId, count, price);
	}
}