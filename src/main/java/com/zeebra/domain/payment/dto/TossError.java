package com.zeebra.domain.payment.dto;

public record TossError(
	String code,
	String message
) {
}