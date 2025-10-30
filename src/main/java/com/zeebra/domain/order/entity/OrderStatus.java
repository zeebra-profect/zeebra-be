package com.zeebra.domain.order.entity;

public enum OrderStatus {
    CREATED,				// 주문 생성됨
	FAILED,					// 주문 생성 실패 - 재고 부족, 가격 불일치, 쿠폰 만료
	PAYMENT_PENDING,		// 결제 진행 중
	PAID,					// 결제 완료
	PAYMENT_FAILED,			// 결제 실패 - 잔액 부족, 카드 오류, 토스 API 실패
	CONFIRMED,				// 주문 승인(배송 준비 개념)
	CANCELED,				// 주문 취소
	REFUNDED,				// 전액 환불
	PARTIALLY_REFUNDED,		// 부분 환불
	COMPLETED,				// 구매 확정
}