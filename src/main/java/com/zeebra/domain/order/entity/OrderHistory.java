package com.zeebra.domain.order.entity;

import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_history_name")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    public OrderHistory(Long orderId, OrderStatus orderStatus, String idempotencyKey) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
        this.idempotencyKey = idempotencyKey;
    }

	public static OrderHistory createOrderHistory(Long orderId, OrderStatus orderStatus, String idempotencyKey){
		return new OrderHistory(orderId, orderStatus, idempotencyKey);
	}
}