package com.zeebra.domain.payment.entity;

public enum PaymentTransactionType {
    PAYMENT,
    PAYMENT_CANCEL,
    REFUND,
    PARTIAL_REFUND,
    CAPTURE,
    VERIFY
}