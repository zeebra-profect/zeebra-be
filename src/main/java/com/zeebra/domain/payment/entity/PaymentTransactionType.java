package com.zeebra.domain.payment.entity;

public enum PaymentTransactionType {
    PAYMENT,		// 결제 승인
    PAYMENT_CANCEL,	// 결제 취소
    REFUND,			// 전액 환불
    PARTIAL_REFUND,	// 부분 환불
    CAPTURE,		// 지연 승인 결제 확정
    VERIFY			// 카드/결제 수단 등 인증 요청
}