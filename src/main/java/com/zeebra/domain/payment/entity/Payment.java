package com.zeebra.domain.payment.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "toss_order_id", length = 64)
    private String tossOrderId;

    @Column(name = "payment_amount", nullable = false, scale = 2, precision = 19)
    private BigDecimal paymentAmount;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "toss_response", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tossResponse =  new HashMap<>();

    public Payment(Long orderId, String paymentKey, String tossOrderId, BigDecimal paymentAmount,
                   PaymentMethod paymentMethod, PaymentStatus paymentStatus, LocalDateTime approvedAt, String failureReason, Map<String, Object> tossResponse) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.tossOrderId = tossOrderId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.approvedAt = approvedAt;
        this.failureReason = failureReason;
        this.tossResponse = tossResponse;
    }
}