package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreatePaymentRequest(
	@NotNull Long orderId,
	@NotBlank @Length(max = 200, min = 1) String orderName,
	@NotNull @Positive BigDecimal price,
	@NotNull @PositiveOrZero BigDecimal discount,
	@NotNull @Positive BigDecimal amount,
	// @NotNull PaymentMethod paymentMethod,
	@NotBlank String clientRequestId
) {
}