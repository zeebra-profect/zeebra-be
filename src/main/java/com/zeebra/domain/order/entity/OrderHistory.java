package com.zeebra.domain.order.entity;

import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_history_seq")
	@SequenceGenerator(name = "order_history_seq", sequenceName = "order_history_order_history_id_seq", allocationSize = 1000)
    @Column(name = "order_history_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

	@Builder
    public OrderHistory(Long orderId, OrderStatus orderStatus, String idempotencyKey) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
        this.idempotencyKey = idempotencyKey;
    }

	public static OrderHistory createOrderHistory(Long orderId, OrderStatus orderStatus, String idempotencyKey){
		return OrderHistory.builder()
			.orderId(orderId)
			.orderStatus(orderStatus)
			.idempotencyKey(idempotencyKey)
			.build();
	}
}