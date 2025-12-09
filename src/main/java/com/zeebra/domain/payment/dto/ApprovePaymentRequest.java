package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApprovePaymentRequest(
	@NotBlank String paymentKey,
	@NotBlank String tossOrderId,
	@NotNull BigDecimal amount,
	@NotBlank String clientRequestId,
	boolean isTest
) {
}