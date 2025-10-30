package com.zeebra.domain.order.entity;

public enum OrderItemStatus {
	CREATED,			// 주문 생성
	PAID,				// 결제 완료
	CANCEL_REQUESTED,	// 취소 요청 (필요시)
	CANCELED,			// 취소 완료
	REFUND_REQUESTED,	// 환불 요청 (승인 필요한 경우)
	REFUNDED,			// 환불 완료
	SHIPPING,			// 배송 중
	DELIVERED,			// 배송 완료
	COMPLETED 			// 구매 확정
}