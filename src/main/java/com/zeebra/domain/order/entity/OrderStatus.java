package com.zeebra.domain.order.entity;

import java.util.List;
import java.util.Map;

import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum OrderStatus {
    CREATED,				// 주문 생성됨
	FAILED,					// 주문 생성 실패 - 재고 부족, 가격 불일치, 쿠폰 만료
	PAYMENT_PENDING,		// 결제 진행 중
	PAID,					// 결제 완료
	PAYMENT_FAILED,			// 결제 실패 - 잔액 부족, 카드 오류, 토스 API 실패
	CONFIRMED,				// 주문 승인(배송 준비 개념)
	CANCELED,				// 주문 취소(결제 전,후로 가능 단, 배송이 시작되기 전에 한함)
	REFUNDED,				// 전액 환불
	PARTIALLY_REFUNDED,		// 부분 환불
	COMPLETED;                // 구매 확정

	private final static Map<OrderStatus, List<OrderStatus>> allowedTransitions = Map.of(
			OrderStatus.CREATED, List.of(
				OrderStatus.PAYMENT_PENDING,
				OrderStatus.CANCELED,
				OrderStatus.FAILED
			),
			OrderStatus.PAYMENT_PENDING, List.of(
				OrderStatus.PAID,
				OrderStatus.PAYMENT_FAILED,
				OrderStatus.CANCELED
			),
			OrderStatus.PAID, List.of(
				OrderStatus.CONFIRMED,
				OrderStatus.CANCELED
			),
			OrderStatus.PAYMENT_FAILED, List.of(
				OrderStatus.CANCELED,
				OrderStatus.PAYMENT_PENDING,
				OrderStatus.FAILED,
				OrderStatus.CREATED,
				OrderStatus.PAID
			),
			OrderStatus.CONFIRMED, List.of(
				OrderStatus.COMPLETED,
				OrderStatus.REFUNDED,
				OrderStatus.PARTIALLY_REFUNDED
			),
			OrderStatus.PARTIALLY_REFUNDED, List.of(
				OrderStatus.COMPLETED,
				OrderStatus.REFUNDED
			)
	);

	public void validateStatusTransition(OrderStatus newStatus) {
		List<OrderStatus> allowedTransitions = OrderStatus.allowedTransitions.getOrDefault(this, List.of());
		if (!allowedTransitions.contains(newStatus)) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
	}

	public boolean isProcessing() {
		return this == CREATED;
	}

	public boolean isCancelable() {
		return this == CREATED || this == PAYMENT_PENDING || this == PAID || this == CONFIRMED;
	}

	public boolean isRefundable() {
		return this == PAID || this == CONFIRMED;
	}
}