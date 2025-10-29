package com.zeebra.domain.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        this.orderItemStatus = orderItemStatus != null ? orderItemStatus : OrderItemStatus.ORDERED;
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
			.orderItemStatus(OrderItemStatus.ORDERED)
			.refundableQuantity(orderItemQuantity)
			.refundableAmount(orderItemAmount)
			.returnedQuantity(0)
			.returnStatus(ReturnStatus.NONE)
			.returnReason(null)
			.build();
	}

	public OrderItem updateOrderItemStatus(OrderItemStatus orderItemStatus) {
		this.orderItemStatus = orderItemStatus;
		return this;
	}

	public OrderItem updateRefundableQuantity(int refundableQuantity) {
		this.refundableQuantity = refundableQuantity;
		return this;
	}
}