package com.zeebra.domain.payment.entity;

public enum PaymentStatus {
    PENDING,				// 결제 생성됨
	APPROVING,				// 토스에 승인 요청 중
    WAITING_FOR_DEPOSIT,	// 가상계좌 발급 완료(입금 대기)
	APPROVED,				// 결제 승인 완료
	FAILED,					// 결제 실패
	VOIDED,					// 매입 전 취소(승인 취소)
    REFUNDED,				// 전액 환불
	PARTIALLY_REFUNDED,		// 부분 환불
	EXPIRED,				// 만료됨(가상계좌 입금 기한 초과 등)
	DONE,					// 결제 확정(주문 확정 시, 환불 불가)
}