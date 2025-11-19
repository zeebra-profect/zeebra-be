package com.zeebra.domain.order.entity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.zeebra.global.exception.BusinessException;

@ActiveProfiles("test")
public class OrderTest {
	@DisplayName("새로운 주문은 CREATED 상태로 생성된다.")
	@Test
	void createOrder(){
		//given //when
		Order order = Order.createOrder(
			1L, "ORD_20251114-1445238969", OrderType.CART, LocalDateTime.of(2025,11,14,14,45,30, 00), 1, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), null,0, "idem-key-001"
		);

		//then
		assertThat(order.getMemberId()).isEqualTo(1L);
		assertThat(order.getOrderNumber()).isEqualTo("ORD_20251114-1445238969");
		assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(order.getOrderType()).isEqualTo(OrderType.CART);
		assertThat(order.getOrderTime()).isEqualTo(LocalDateTime.of(2025,11,14,14,45,30, 00));
		assertThat(order.getTotalQuantity()).isEqualTo(1);
		assertThat(order.getTotalPrice()).isEqualTo(BigDecimal.valueOf(5000));
		assertThat(order.getTotalAmount()).isEqualTo(BigDecimal.valueOf(5000));
		assertThat(order.getUsePoint()).isEqualTo(0);
		assertThat(order.getIdempotencyKey()).isEqualTo("idem-key-001");
	}

	@DisplayName("올바른 상태 전이는 규칙을 호출하고 상태가 실제로 바뀐다.")
	@Test
	void updateOrderStatus(){
		//given
		Order order = Order.createOrder(
			1L, "ORD_20251114-1445238969", OrderType.CART, LocalDateTime.of(2025,11,14,14,45,30, 00), 1, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), null,0, "idem-key-001"
		);
		//when
		order.updateOrderStatus(OrderStatus.PAYMENT_PENDING);
		//then
		assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
	}

	@DisplayName("잘못된 상태 전이는 규칙 호출 중에 예외를 던진다.")
	@Test
	void updateOrderStatusThrowsException(){
		//given
		Order order = Order.createOrder(
			1L, "ORD_20251114-1445238969", OrderType.CART, LocalDateTime.of(2025,11,14,14,45,30, 00), 1, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), null,0, "idem-key-001"
		);
		//when //then
		assertThatThrownBy(() -> order.updateOrderStatus(OrderStatus.PAID)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 상태 전이입니다.");
	}
}