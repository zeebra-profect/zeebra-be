package com.zeebra.domain.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")
	@SequenceGenerator(name = "order_item_seq", sequenceName = "order_item_order_item_id_seq", allocationSize = 1000 )
	@Column(name = "order_item_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "order_item_name", nullable = false, length = 100)
    private String orderItemName;

	@Column(name = "order_item_thumbnail", length = 2048)
	private String orderItemThumbnail;

    @Column(name = "order_item_price", nullable = false, scale = 2, precision = 19)
    private BigDecimal orderItemPrice;

    @Column(name = "order_item_quantity", nullable = false)
    private int orderItemQuantity;

    @Column(name = "order_item_amount", nullable = false,  scale = 2, precision = 19)
    private BigDecimal orderItemAmount;

    @Column(name = "order_item_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderItemStatus orderItemStatus;

    @Column(name = "refundable_quantity", nullable = false)
    private int refundableQuantity;

    @Column(name = "refundable_amount", nullable = false, scale = 2, precision = 19)
    private BigDecimal refundableAmount;

    @Column(name = "returned_quantity", nullable = false)
    private int returnedQuantity;

    @Column(name = "return_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReturnStatus returnStatus;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "last_return_requested_time")
    private LocalDateTime lastReturnRequestedTime;

    @Column(name = "last_return_completed_time")
    private LocalDateTime lastReturnCompletedTime;

    @Builder
    public OrderItem(
		Long orderId,
		Long saleId,
		String orderItemName,
		BigDecimal orderItemPrice,
	 	String orderItemThumbnail,
		int orderItemQuantity,
		BigDecimal orderItemAmount,
		OrderItemStatus orderItemStatus,
	 	int refundableQuantity,
		BigDecimal refundableAmount,
		int returnedQuantity,
		ReturnStatus returnStatus,
		String returnReason) {
        this.orderId = orderId;
        this.saleId = saleId;
        this.orderItemName = orderItemName;
		this.orderItemThumbnail = orderItemThumbnail;
        this.orderItemPrice = orderItemPrice;
        this.orderItemQuantity = orderItemQuantity != 0 ? orderItemQuantity : 1;
        this.orderItemAmount = orderItemAmount;
        this.orderItemStatus = orderItemStatus != null ? orderItemStatus : OrderItemStatus.CREATED;
        this.refundableQuantity = refundableQuantity;
        this.refundableAmount = refundableAmount;
        this.returnedQuantity = returnedQuantity;
        this.returnStatus = returnStatus != null ? returnStatus : ReturnStatus.NONE;
        this.returnReason = returnReason;
    }

	public static OrderItem createOrderItem(Long orderId, Long saleId, String orderItemName, BigDecimal orderItemPrice, String orderItemThumbnail, int orderItemQuantity, BigDecimal orderItemAmount) {
		return OrderItem.builder()
			.orderId(orderId)
			.saleId(saleId)
			.orderItemName(orderItemName)
			.orderItemPrice(orderItemPrice)
			.orderItemThumbnail(orderItemThumbnail)
			.orderItemQuantity(orderItemQuantity)
			.orderItemAmount(orderItemAmount)
			.orderItemStatus(OrderItemStatus.CREATED)
			.refundableQuantity(orderItemQuantity)
			.refundableAmount(orderItemAmount)
			.returnedQuantity(0)
			.returnStatus(ReturnStatus.NONE)
			.returnReason(null)
			.build();
	}

	public OrderItem updateRefundableQuantity(int refundableQuantity) {
		this.refundableQuantity = refundableQuantity;
		return this;
	}

	public void updateOrderItemStatus(OrderItemStatus newStatus) {
		this.orderItemStatus.validateStatusTransition(newStatus);
		this.orderItemStatus = newStatus;
	}

	public void transitionToRefundRequested() {
		if (!this.orderItemStatus.isRefundable()) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		if (this.returnStatus != ReturnStatus.NONE) {
			throw new BusinessException(OrderErrorCode.ALREADY_IN_PROGRESS);
		}
		this.orderItemStatus = OrderItemStatus.REFUND_REQUESTED;
	}

	public void transitionToRefunded() {
		if (!this.canRefund()) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		this.orderItemStatus = OrderItemStatus.REFUNDED;
	}

	public void transitionToShipping() {
		if (this.orderItemStatus != OrderItemStatus.PAID) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		if (this.returnStatus != ReturnStatus.NONE) {
			throw new BusinessException(OrderErrorCode.ALREADY_IN_PROGRESS);
		}
		this.orderItemStatus = OrderItemStatus.SHIPPING;
	}

	public void transitionToDelivered() {
		if (this.orderItemStatus != OrderItemStatus.SHIPPING) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		this.orderItemStatus = OrderItemStatus.DELIVERED;
	}

	public void transitionToCompleted() {
		if (this.orderItemStatus != OrderItemStatus.PAID &&
			this.orderItemStatus != OrderItemStatus.DELIVERED) {
			throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
		}
		if (this.returnStatus != ReturnStatus.NONE) {
			throw new BusinessException(OrderErrorCode.ALREADY_IN_PROGRESS);
		}
		this.orderItemStatus = OrderItemStatus.COMPLETED;
	}

	public void cancel() {
		if (!canCancel()) {
			throw new BusinessException(OrderErrorCode.CANNOT_BE_CANCELLED);
		}
		this.orderItemStatus = OrderItemStatus.CANCELED;
	}

	public void refund() {
		if (!canRefund()) {
			throw new BusinessException(OrderErrorCode.CANNOT_BE_REFUNDED);
		}
		this.orderItemStatus = OrderItemStatus.REFUNDED;
	}

	public boolean canCancel() {
		return this.orderItemStatus.isCancelable() &&
			this.returnStatus == ReturnStatus.NONE;
	}

	public boolean canRequestRefund(){
		return this.orderItemStatus.isRefundable() &&
			this.returnStatus == ReturnStatus.NONE;
	}

	public boolean canRefund() {
		return this.orderItemStatus.isRefundRequested() &&
			(this.returnStatus == ReturnStatus.NONE || this.returnStatus == ReturnStatus.RETURNED);
	}

	public boolean canStartShipping() {
		return this.orderItemStatus.isShippable() &&
			this.returnStatus == ReturnStatus.NONE;
	}

	public boolean canRequestReturn() {
		return this.orderItemStatus.isReturnable() &&
			this.returnStatus == ReturnStatus.NONE;
	}

	public boolean canComplete() {
		return this.orderItemStatus.isCompletable() &&
			this.returnStatus == ReturnStatus.NONE;
	}

}