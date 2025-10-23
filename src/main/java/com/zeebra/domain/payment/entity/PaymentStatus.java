package com.zeebra.domain.payment.entity;

public enum PaymentStatus {
    PENDING,
    PENDING_AWAITING_DEPOSIT,
    APPROVED,
    VOIDED,
    FAILED,
    EXPIRED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    DONE,
}