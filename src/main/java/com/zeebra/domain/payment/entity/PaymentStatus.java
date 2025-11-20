package com.zeebra.domain.payment.entity;

import java.util.List;
import java.util.Map;

import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

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
	DONE;					// 결제 확정(주문 확정 시, 환불 불가)
	private final static Map<PaymentStatus, List<PaymentStatus>> allowedTransitions = Map.of(
		PaymentStatus.PENDING, List.of(
			PaymentStatus.APPROVING,
			PaymentStatus.WAITING_FOR_DEPOSIT,
			PaymentStatus.FAILED,
			PaymentStatus.EXPIRED
		),
		PaymentStatus.APPROVING, List.of(
			PaymentStatus.APPROVED,
			PaymentStatus.FAILED
		),
		PaymentStatus.WAITING_FOR_DEPOSIT, List.of(
			PaymentStatus.APPROVED,
			PaymentStatus.EXPIRED
		),
		PaymentStatus.APPROVED, List.of(
			PaymentStatus.VOIDED,
			PaymentStatus.REFUNDED,
			PaymentStatus.PARTIALLY_REFUNDED,
			PaymentStatus.DONE
		),
		PaymentStatus.FAILED, List.of(
			PaymentStatus.APPROVING,
			PaymentStatus.WAITING_FOR_DEPOSIT,
			PaymentStatus.EXPIRED
		),
		PaymentStatus.VOIDED, List.of(),
		PaymentStatus.REFUNDED, List.of(),
		PaymentStatus.PARTIALLY_REFUNDED, List.of(
			PaymentStatus.REFUNDED,
			PaymentStatus.DONE
		),
		PaymentStatus.EXPIRED, List.of(),
		PaymentStatus.DONE, List.of()
	);

	public void validateStatusTransition(PaymentStatus newStatus) {
		List<PaymentStatus> allowedTransitions = PaymentStatus.allowedTransitions.getOrDefault(this, List.of());
		if (!allowedTransitions.contains(newStatus)) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS_TRANSITION);
		}
	}
}