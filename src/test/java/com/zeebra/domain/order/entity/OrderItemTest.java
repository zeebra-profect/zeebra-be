package com.zeebra.domain.order.entity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.zeebra.global.exception.BusinessException;

@ActiveProfiles("test")
public class OrderItemTest {

	@DisplayName("새로운 주문 상품은 CREATED 상태로 생성된다.")
	@Test
	void createOrderItem(){
		//given //when
		OrderItem orderItem = OrderItem.createOrderItem(
			1L, 1L, "아디다스 맨투맨", BigDecimal.valueOf(50000), "product_1.jpeg", 1, BigDecimal.valueOf(50000)
		);
		//then
		assertThat(orderItem.getOrderId()).isEqualTo(1L);
		assertThat(orderItem.getSaleId()).isEqualTo(1L);
		assertThat(orderItem.getOrderItemName()).isEqualTo("아디다스 맨투맨");
		assertThat(orderItem.getOrderItemThumbnail()).isEqualTo("product_1.jpeg");
		assertThat(orderItem.getOrderItemPrice()).isEqualTo(BigDecimal.valueOf(50000));
		assertThat(orderItem.getOrderItemQuantity()).isEqualTo(1);
		assertThat(orderItem.getOrderItemAmount()).isEqualTo(BigDecimal.valueOf(50000));
		assertThat(orderItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.CREATED);
		assertThat(orderItem.getRefundableQuantity()).isEqualTo(orderItem.getOrderItemQuantity());
		assertThat(orderItem.getRefundableAmount()).isEqualTo(orderItem.getOrderItemAmount());
		assertThat(orderItem.getReturnedQuantity()).isEqualTo(0);
		assertThat(orderItem.getReturnStatus()).isEqualTo(ReturnStatus.NONE);
		assertThat(orderItem.getReturnReason()).isNull();
		assertThat(orderItem.getLastReturnRequestedTime()).isNull();
		assertThat(orderItem.getLastReturnRequestedTime()).isNull();
	}

	@DisplayName("올바른 상태 전이는 규칙을 호출하고 상태가 실제로 바뀐다.")
	@Test
	void updateOrderItemStatus(){
		//given
		OrderItem orderItem = OrderItem.createOrderItem(
			1L, 1L, "아디다스 맨투맨", BigDecimal.valueOf(50000), "product_1.jpeg", 1, BigDecimal.valueOf(50000)
		);
		//when
		orderItem.updateOrderItemStatus(OrderItemStatus.PAID);
		//then
		assertThat(orderItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.PAID);
	}

	@DisplayName("잘못된 상태 전이는 규칙 호출 중에 예외를 던진다.")
	@Test
	void updateOrderItemStatusThrowsException(){
		//given
		OrderItem orderItem = OrderItem.createOrderItem(
			1L, 1L, "아디다스 맨투맨", BigDecimal.valueOf(50000), "product_1.jpeg", 1, BigDecimal.valueOf(50000)
		);
		//when //then
		assertThatThrownBy(() -> orderItem.updateOrderItemStatus(OrderItemStatus.DELIVERED)).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 상태 전이입니다.");
	}
}