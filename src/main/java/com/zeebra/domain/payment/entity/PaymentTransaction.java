package com.zeebra.domain.payment.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_transaction_id")
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentTransactionType paymentTransactionType;

    @Column(name = "payment_transaction_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentTransactionStatus paymentTransactionStatus;

    @Column(name = "request_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> requestData;

    @Column(name = "response_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> responseData;

    @Column(name = "response_code")
    private int responseCode;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "parent_transaction_id")
    private Long parentTransactionId;

    public PaymentTransaction(Long paymentId, PaymentTransactionType paymentTransactionType,
                              PaymentTransactionStatus paymentTransactionStatus, Map<String, Object> requestData, Map<String, Object> responseData, int responseCode, int retryCount, Long parentTransactionId) {
        this.paymentId = paymentId;
        this.paymentTransactionType = paymentTransactionType;
        this.paymentTransactionStatus = paymentTransactionStatus;
        this.requestData = requestData != null ? requestData : new HashMap<>();
        this.responseData = responseData != null ? responseData : new HashMap<>();
        this.responseCode = responseCode;
        this.retryCount = retryCount;
        this.parentTransactionId = parentTransactionId;
    }
}