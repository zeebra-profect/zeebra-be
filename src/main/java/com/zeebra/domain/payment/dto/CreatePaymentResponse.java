package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;

import com.zeebra.domain.payment.entity.Payment;

public record CreatePaymentResponse(
	Long paymentId,
	Amount amount,
 	String orderName,
 	String tossOrderId,
	String successUrl,
	String failUrl
) {
	private static final String SUCCESS_URL = "/payments/success";
	private static final String FAIL_URL = "/payments/fail";

	public static CreatePaymentResponse of(Payment payment) {
		return new CreatePaymentResponse(payment.getId(), Amount.of(payment.getPaymentAmount()), payment.getOrderNameSnapshot(), payment.getTossOrderId(), SUCCESS_URL, FAIL_URL);
	}

	public record Amount(BigDecimal value, String currency) {
		private static final String DEFAULT_CURRENCY = "KRW";
		
		public static Amount of(BigDecimal value) {
			return new Amount(value, DEFAULT_CURRENCY);
		}
		
		public static Amount of(BigDecimal value, String currency) {
			return new Amount(value, currency);
		}
	}
}