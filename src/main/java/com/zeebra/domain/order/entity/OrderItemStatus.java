package com.zeebra.domain.order.entity;

import java.util.List;
import java.util.Map;

import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

public enum OrderItemStatus {
	CREATED,			// 주문 생성
	PAID,				// 결제 완료
	CANCEL_REQUESTED,	// 취소 요청 (필요시)
	CANCELED,			// 취소 완료
	REFUND_REQUESTED,	// 환불 요청 (승인 필요한 경우)
	REFUNDED,			// 환불 완료
	SHIPPING,			// 배송 중
	DELIVERED,			// 배송 완료
	COMPLETED; 			// 구매 확정
	private final static Map<OrderItemStatus, List<OrderItemStatus>> allowedTransitions = Map.of(
		OrderItemStatus.CREATED, List.of(
			OrderItemStatus.PAID,
			OrderItemStatus.CANCELED
		),
		OrderItemStatus.PAID, List.of(
			OrderItemStatus.CANCELED,
			OrderItemStatus.REFUND_REQUESTED,
			OrderItemStatus.REFUNDED,
			OrderItemStatus.SHIPPING,
			OrderItemStatus.COMPLETED
		),
		OrderItemStatus.REFUND_REQUESTED, List.of(
			OrderItemStatus.REFUNDED,
			OrderItemStatus.PAID
		),
		OrderItemStatus.SHIPPING, List.of(
			OrderItemStatus.DELIVERED
		),
		OrderItemStatus.DELIVERED, List.of(
			OrderItemStatus.COMPLETED
		),
		OrderItemStatus.COMPLETED, List.of(),
		OrderItemStatus.CANCELED, List.of(),
		OrderItemStatus.REFUNDED, List.of()
	);

	public void validateStatusTransition(OrderItemStatus newStatus) {
		List<OrderItemStatus> allowedTransitions = OrderItemStatus.allowedTransitions.getOrDefault(this, List.of());
		if(!allowedTransitions.contains(newStatus)) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
	}

	public boolean isCancelable() {
		return this == OrderItemStatus.CREATED || this == OrderItemStatus.PAID;
	}

	public boolean isRefundable() {
		return this == OrderItemStatus.PAID || this == OrderItemStatus.SHIPPING || this == OrderItemStatus.DELIVERED;
	}

	public boolean isRefundRequested() {
		return this == OrderItemStatus.REFUND_REQUESTED;
	}

	public boolean isShippable() {
		return this == OrderItemStatus.PAID;
	}

	public boolean isReturnable() {
		return this == OrderItemStatus.DELIVERED || this == OrderItemStatus.SHIPPING;
	}

	public boolean isCompletable() {
		return this == OrderItemStatus.PAID || this == OrderItemStatus.DELIVERED;
	}

	public boolean isCompleted() {
		return this == OrderItemStatus.COMPLETED;
	}
}