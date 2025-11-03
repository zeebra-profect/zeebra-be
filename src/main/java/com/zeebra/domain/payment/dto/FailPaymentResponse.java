package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;

import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.payment.entity.Payment;
import com.zeebra.domain.payment.entity.PaymentStatus;

public record FailPaymentResponse(
	Long paymentId,
	String orderNameSnapshot,
	BigDecimal paymentAmount,
	PaymentStatus paymentStatus,
	OrderInfo orderInfo
) {
	public static FailPaymentResponse of(Payment payment, OrderInfo orderInfo) {
		return new FailPaymentResponse(payment.getId(), payment.getOrderNameSnapshot(),payment.getPaymentAmount(),payment.getPaymentStatus(), orderInfo);
	}
}