package com.zeebra.domain.order.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private int memberId;

    @Column(name = "order_number", nullable = false, length = 20, unique = true)
    private String orderNumber;

    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(name = "order_time", nullable = false)
    private LocalDateTime orderTime;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "total_price", nullable = false,  scale = 2, precision = 19)
    private BigDecimal totalPrice;

    @Comment("할인/포인트/쿠폰/배송비 적용 후 최종 결제 예정 금액")
    @Column(name = "total_amount", nullable = false, scale = 2, precision = 19)
    private BigDecimal totalAmount;

    @Column(name = "use_point", nullable = false)
    private int usePoint;

    @Column(name = "idempotency_key", nullable = false,  length = 100, unique = true)
    private String idempotencyKey;

    public Order(int memberId, String orderNumber, OrderStatus orderStatus, LocalDateTime orderTime, int totalQuantity, BigDecimal totalPrice, BigDecimal totalAmount, int usePoint, String idempotencyKey) {
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus != null ? orderStatus : OrderStatus.CREATED;
        this.orderTime = orderTime;
        this.totalQuantity = totalQuantity;
        this.totalPrice = totalPrice;
        this.totalAmount = totalAmount;
        this.usePoint = usePoint;
        this.idempotencyKey = idempotencyKey;
    }
}