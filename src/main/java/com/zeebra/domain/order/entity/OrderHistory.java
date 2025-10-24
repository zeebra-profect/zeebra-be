package com.zeebra.domain.order.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
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
}