package com.zeebra.domain.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

	@Column(name = "toss_order_id", nullable = false, length = 64)
	private String tossOrderId;

	@Column(name = "order_name_snapshot", nullable = false, length = 200)
	private String orderNameSnapshot;

    @Column(name = "payment_amount", nullable = false, scale = 2, precision = 19)
    private BigDecimal paymentAmount;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "toss_response", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tossResponse;

	@Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
	private String idempotencyKey;

	@Builder
    public Payment(
		Long orderId,
		String paymentKey,
		String tossOrderId,
		String orderNameSnapshot,
		BigDecimal paymentAmount,
		PaymentMethod paymentMethod,
		PaymentStatus paymentStatus,
		LocalDateTime approvedAt,
		String failureReason,
		Map<String, Object> tossResponse,
		String idempotencyKey
	) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.tossOrderId = tossOrderId;
		this.orderNameSnapshot = orderNameSnapshot;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PENDING;
        this.approvedAt = approvedAt;
        this.failureReason = failureReason;
        this.tossResponse = tossResponse != null ? tossResponse : new HashMap<>();
		this.idempotencyKey = idempotencyKey;
    }


	public static Payment createPayment(Long orderId, String tossOrderId, String orderNameSnapshot, BigDecimal paymentAmount, String idempotencyKey) {
		return Payment.builder()
			.orderId(orderId)
			.tossOrderId(tossOrderId)
			.orderNameSnapshot(orderNameSnapshot)
			.paymentAmount(paymentAmount)
			.paymentStatus(PaymentStatus.PENDING)
			.tossResponse(new HashMap<>())
			.idempotencyKey(idempotencyKey)
			.build();
	}

	public void updateApprovedAt(LocalDateTime approvedAt) {
		this.approvedAt = approvedAt;
	}
	public void updatePaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public void updateTossResponse(Map<String, Object> tossResponse) {
		this.tossResponse = tossResponse;
	}

	public void updateFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}
}