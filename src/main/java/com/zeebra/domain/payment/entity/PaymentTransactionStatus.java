package com.zeebra.domain.payment.entity;

public enum PaymentTransactionStatus {
    REQUESTED,		// 트랙잭션 요청됨, API 호출 직전
    PENDING,		// 처리 중(토스 서버에서 처리 중)
    SUCCEEDED,		// 성공
    FAILED,			// 실패
    CANCELED,		// 취소됨
}