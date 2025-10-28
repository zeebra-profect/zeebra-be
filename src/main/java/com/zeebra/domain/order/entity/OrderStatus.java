package com.zeebra.domain.order.entity;

public enum OrderStatus {
    CREATED,
	FAILED,
    PAYMENT_PENDING,
    PAID,
    COMPLETED,
    PAID_FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CONFIRMED,
    CANCELLED,
}