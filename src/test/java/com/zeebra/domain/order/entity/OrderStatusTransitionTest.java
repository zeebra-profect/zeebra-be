package com.zeebra.domain.order.entity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ActiveProfiles;

import com.zeebra.global.exception.BusinessException;

@ActiveProfiles("test")
public class OrderStatusTransitionTest {

	static Stream<Arguments> orderStatusTransitions(){
		return Stream.of(
			Arguments.of(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.CREATED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.PAID),
			Arguments.of(OrderStatus.PAID, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.PAID, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.REFUNDED)
		);
	}

	static Stream<Arguments> orderStatusTransitionsInvalid(){
		return Stream.of(
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.PAID),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.COMPLETED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.PAID),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.PARTIALLY_REFUNDED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.PAID),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.REFUNDED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.PAID),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.CANCELED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PAID),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.PAYMENT_FAILED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.PAID, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.PAID, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.PAID, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.PAID, OrderStatus.PAID),
			Arguments.of(OrderStatus.PAID, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.PAID, OrderStatus.FAILED),
			Arguments.of(OrderStatus.PAID, OrderStatus.CREATED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.FAILED),
			Arguments.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CREATED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.CANCELED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.PAID),
			Arguments.of(OrderStatus.FAILED, OrderStatus.PAYMENT_PENDING),
			Arguments.of(OrderStatus.FAILED, OrderStatus.FAILED),
			Arguments.of(OrderStatus.FAILED, OrderStatus.CREATED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.COMPLETED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.PARTIALLY_REFUNDED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.REFUNDED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.CONFIRMED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.PAYMENT_FAILED),
			Arguments.of(OrderStatus.CREATED, OrderStatus.PAID),
			Arguments.of(OrderStatus.CREATED, OrderStatus.CREATED)
		);
	}

	@ParameterizedTest(name = "올바른 상태 전이는 허용한다: {0} -> {1}")
	@MethodSource("orderStatusTransitions")
	void orderStatusTransition(OrderStatus from, OrderStatus to){
		from.validateStatusTransition(to);
	}

	@ParameterizedTest(name = "잘못된 상태 전이는 실패한다: {0} -> {1}")
	@MethodSource("orderStatusTransitionsInvalid")
	void orderStatusTransitionInvalid(OrderStatus from, OrderStatus to){
		assertThatThrownBy(() -> from.validateStatusTransition(to)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 상태 전이입니다.");
	}
}