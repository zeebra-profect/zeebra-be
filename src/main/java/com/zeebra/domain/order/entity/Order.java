package com.zeebra.domain.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "order_number", nullable = false, length = 20, unique = true)
    private String orderNumber;

    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

	@Column(name = "order_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private OrderType orderType;

    @Column(name = "order_time", nullable = false)
    private LocalDateTime orderTime;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "total_price", nullable = false,  scale = 2, precision = 19)
    private BigDecimal totalPrice;

    @Comment("할인/포인트/쿠폰/배송비 적용 후 최종 결제 예정 금액")
    @Column(name = "total_amount", nullable = false, scale = 2, precision = 19)
    private BigDecimal totalAmount;

	@Column(name = "trade_id")
	private Long tradeId;

    @Column(name = "use_point", nullable = false)
    private int usePoint;

    @Column(name = "idempotency_key", nullable = false,  length = 128, unique = true)
    private String idempotencyKey;

	@Builder
    public Order(Long memberId, String orderNumber, OrderStatus orderStatus, OrderType orderType, LocalDateTime orderTime, int totalQuantity, BigDecimal totalPrice, BigDecimal totalAmount, Long tradeId, int usePoint, String idempotencyKey) {
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus != null ? orderStatus : OrderStatus.CREATED;
		this.orderType = orderType;
        this.orderTime = orderTime;
        this.totalQuantity = totalQuantity;
        this.totalPrice = totalPrice;
        this.totalAmount = totalAmount;
		this.tradeId = tradeId;
        this.usePoint = usePoint;
        this.idempotencyKey = idempotencyKey;
    }

	public static Order createOrder(Long memberId, String orderNumber, OrderType orderType, LocalDateTime orderTime, int totalQuantity, BigDecimal totalPrice, BigDecimal totalAmount, Long tradeId,int usePoint, String idempotencyKey) {
		return Order.builder()
			.memberId(memberId)
			.orderNumber(orderNumber)
			.orderStatus(OrderStatus.CREATED)
			.orderType(orderType)
			.orderTime(orderTime)
			.totalQuantity(totalQuantity)
			.totalPrice(totalPrice)
			.totalAmount(totalAmount)
			.tradeId(tradeId)
			.usePoint(usePoint)
			.idempotencyKey(idempotencyKey)
			.build();
	}

	public void updateOrderStatus(OrderStatus newStatus) {
		this.orderStatus.validateStatusTransition(newStatus);
		this.orderStatus = newStatus;
	}

	public void validateCreatable() {
		if (!this.orderStatus.isProcessing()) {
			throw new BusinessException(OrderErrorCode.ALREADY_IN_PROGRESS);
		}
	}

	public void transitionToPaymentPending() {
		this.orderStatus.validateStatusTransition(OrderStatus.PAYMENT_PENDING);
		this.orderStatus = OrderStatus.PAYMENT_PENDING;
	}

	public void transitionToPaid() {
		this.orderStatus.validateStatusTransition(OrderStatus.PAID);
		this.orderStatus = OrderStatus.PAID;
	}

	public void transitionToCancel() {
		this.orderStatus.validateStatusTransition(OrderStatus.CANCELED);
		this.orderStatus = OrderStatus.CANCELED;
	}

	public void transitionToRefund() {
		this.orderStatus.validateStatusTransition(OrderStatus.REFUNDED);
		this.orderStatus = OrderStatus.REFUNDED;
	}

	public boolean canCancel() {
		return this.orderStatus.isCancelable();
	}

	public boolean canRefund() {
		return this.orderStatus.isRefundable();
	}
}