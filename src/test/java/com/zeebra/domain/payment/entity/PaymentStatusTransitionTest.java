package com.zeebra.domain.payment.entity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ActiveProfiles;

import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

@ActiveProfiles("test")
public class PaymentStatusTransitionTest {
	static Stream<Arguments> paymentStatusTransitions(){
		return Stream.of(
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.DONE)
		);
	}

	static Stream<Arguments> paymentStatusTransitionsInvalid(){
		return Stream.of(
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.PENDING, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.APPROVING, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.WAITING_FOR_DEPOSIT, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.APPROVED, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.FAILED, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.VOIDED, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.EXPIRED, PaymentStatus.DONE),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.PENDING),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.APPROVING),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.WAITING_FOR_DEPOSIT),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.APPROVED),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.FAILED),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.VOIDED),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.REFUNDED),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.PARTIALLY_REFUNDED),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.EXPIRED),
			Arguments.of(PaymentStatus.DONE, PaymentStatus.DONE)
		);
	}

	@ParameterizedTest(name = "올바른 상태 전이는 허용한다: {0} -> {1}")
	@MethodSource("paymentStatusTransitions")
	void paymentStatusTransition(PaymentStatus from, PaymentStatus to){
		from.validateStatusTransition(to);
	}

	@ParameterizedTest(name = "잘못된 상태 전이는 실패한다: {0} -> {1}")
	@MethodSource("paymentStatusTransitionsInvalid")
	void paymentStatusTransirionInvalid(PaymentStatus from, PaymentStatus to){
		assertThatThrownBy(() -> from.validateStatusTransition(to)).isInstanceOf(BusinessException.class).hasMessage(PaymentErrorCode.INVALID_STATUS_TRANSITION.getMessage());
	}
}