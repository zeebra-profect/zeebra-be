package com.zeebra.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

public record OrderInfo (
	Long orderId,
	String orderNumber,
	OrderStatus orderStatus,
	LocalDateTime orderTime,
	BigDecimal totalPrice,
	BigDecimal totalAmount,
	int usePoint
) {
	public static OrderInfo of(Order order) {
		return new OrderInfo(
			order.getId(),
			order.getOrderNumber(),
			order.getOrderStatus(),
			order.getOrderTime(),
			order.getTotalPrice(),
			order.getTotalAmount(),
			order.getUsePoint()
		);
	}

	public void validatePaymentAmount(BigDecimal paymentAmount) {
		if (totalAmount().compareTo(paymentAmount) != 0) {
			throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}
	}

	public void validatePaymentCreatable() {
		if(orderStatus.isCreated()){
			throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_REQUEST);
		}
	}
}