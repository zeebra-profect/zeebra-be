package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TossApprovalRequest(
	@NotBlank String paymentKey,
	@NotBlank String orderId,
	@NotNull BigDecimal amount
) {
}