package com.zeebra.domain.order.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    COMPLETED,
    PAID_FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CONFIRMED,
    CANCELLED,
}
