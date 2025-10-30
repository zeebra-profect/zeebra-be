package com.zeebra.domain.payment.entity;

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
public class PaymentHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_history_id")
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

	@Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
	private String idempotencyKey;

    public PaymentHistory(Long paymentId, PaymentStatus paymentStatus, String idempotencyKey) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
    	this.idempotencyKey = idempotencyKey;
	}

	public static PaymentHistory createPaymentHistory(Long paymentId, PaymentStatus paymentStatus, String idempotencyKey) {
		return new PaymentHistory(paymentId, paymentStatus, idempotencyKey);
	}
}