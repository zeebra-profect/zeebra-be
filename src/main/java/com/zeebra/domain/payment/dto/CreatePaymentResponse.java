package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;

import com.zeebra.domain.payment.entity.Payment;

public record CreatePaymentResponse(
	Long paymentId,
	Amount amount,
 	String orderName,
 	String orderId,
	String successUrl,
	String failUrl
) {
	public static CreatePaymentResponse of(
			Payment payment,
			String successUrl, 
			String failUrl) {
		return new CreatePaymentResponse(payment.getId(), Amount.of(payment.getPaymentAmount()), payment.getOrderNameSnapshot(), payment.getTossOrderId(), successUrl, failUrl);
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