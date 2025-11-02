package com.zeebra.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record FailPaymentRequest(
	String code,
	String message,
	@NotBlank String tossOrderId,
	@NotBlank String clientRequestId
) {
}