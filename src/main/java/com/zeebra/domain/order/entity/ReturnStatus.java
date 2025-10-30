package com.zeebra.domain.order.entity;

public enum ReturnStatus {
    NONE,				// 기본 상태
    REQUESTED,			// 반품 요청
	APPROVED,			// 반품 승인
	RETURNED,			// 반품 완료
	REFUNDED,			// 환불 완료
    REJECTED,			// 반품 거절(판매자 측)
	CANCELED			// 반품 취소
}