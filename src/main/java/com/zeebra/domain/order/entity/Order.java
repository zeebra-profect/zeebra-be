package com.zeebra.domain.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Column(name = "idempotency_key", nullable = false,  length = 128, unique = true)
    private String idempotencyKey;

	@Builder
    public Order(Long memberId, String orderNumber, OrderStatus orderStatus, LocalDateTime orderTime, int totalQuantity, BigDecimal totalPrice, BigDecimal totalAmount, int usePoint, String idempotencyKey) {
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

	public static Order createOrder(Long memberId, String orderNumber, LocalDateTime orderTime, int totalQuantity, BigDecimal totalPrice, BigDecimal totalAmount, int usePoint, String idempotencyKey) {
		return Order.builder()
			.memberId(memberId)
			.orderNumber(orderNumber)
			.orderStatus(OrderStatus.CREATED)
			.orderTime(orderTime)
			.totalQuantity(totalQuantity)
			.totalPrice(totalPrice)
			.totalAmount(totalAmount)
			.usePoint(usePoint)
			.idempotencyKey(idempotencyKey)
			.build();
	}

	public void updateOrderStatus(OrderStatus newStatus) {
		validateStatusTransition(newStatus);
		this.orderStatus = newStatus;
	}

	public void transitionToPaymentPending() {
		if (this.orderStatus != OrderStatus.CREATED) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		this.orderStatus = OrderStatus.PAYMENT_PENDING;
	}

	public void transitionToPaid() {
		if (this.orderStatus != OrderStatus.PAYMENT_PENDING) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		this.orderStatus = OrderStatus.PAID;
	}

	public void cancel() {
		if (!canCancel()) {
			throw new BusinessException(OrderErrorCode.CANNOT_BE_CANCELLED);
		}
		this.orderStatus = OrderStatus.CANCELED;
	}

	public boolean canCancel() {
		return this.orderStatus == OrderStatus.CREATED
			|| this.orderStatus == OrderStatus.PAYMENT_PENDING
			|| this.orderStatus == OrderStatus.PAID;
	}

	public boolean canRefund() {
		return this.orderStatus == OrderStatus.PAID
			|| this.orderStatus == OrderStatus.CONFIRMED;
	}
	private void validateStatusTransition(OrderStatus newStatus) {
		Map<OrderStatus, List<OrderStatus>> allowedTransitions = Map.of(
			OrderStatus.CREATED, List.of(
				OrderStatus.PAYMENT_PENDING,
				OrderStatus.CANCELED,
				OrderStatus.FAILED
			),
			OrderStatus.PAYMENT_PENDING, List.of(
				OrderStatus.PAID,
				OrderStatus.PAYMENT_FAILED,
				OrderStatus.CANCELED
			),
			OrderStatus.PAID, List.of(
				OrderStatus.CONFIRMED,
				OrderStatus.CANCELED
			),
			OrderStatus.CONFIRMED, List.of(
				OrderStatus.COMPLETED,
				OrderStatus.REFUNDED,
				OrderStatus.PARTIALLY_REFUNDED
			)
		);

		List<OrderStatus> allowed = allowedTransitions.getOrDefault(this.orderStatus, List.of());

		if (!allowed.contains(newStatus)) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
	}
}