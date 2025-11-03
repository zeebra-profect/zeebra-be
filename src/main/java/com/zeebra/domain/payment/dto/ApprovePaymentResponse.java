package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.payment.entity.Payment;
import com.zeebra.domain.payment.entity.PaymentMethod;
import com.zeebra.domain.payment.entity.PaymentStatus;

public record ApprovePaymentResponse(
	Long paymentId,
	String orderNameSnapshot,
	BigDecimal paymentAmount,
	PaymentMethod paymentMethod,
	PaymentStatus paymentStatus,
	LocalDateTime approvedAt,
	boolean isApproved,
 	OrderInfo orderInfo) {
	
	public static ApprovePaymentResponse of(Payment payment, OrderInfo orderInfo) {
		if (payment == null) {
			throw new IllegalArgumentException("Payment cannot be null");
		}
		if (orderInfo == null) {
			throw new IllegalArgumentException("OrderInfo cannot be null");
		}

		PaymentStatus status = payment.getPaymentStatus();
		boolean isApproved = status == PaymentStatus.APPROVED || status == PaymentStatus.DONE;
		
		return new ApprovePaymentResponse(
			payment.getId(),
			payment.getOrderNameSnapshot(),
			payment.getPaymentAmount(),
			payment.getPaymentMethod(),
			status,
			payment.getApprovedAt(),
			isApproved,
			orderInfo
		);
	}
}