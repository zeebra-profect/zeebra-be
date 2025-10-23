package com.zeebra.domain.order.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "order_item_price", nullable = false, scale = 2, precision = 19)
    private BigDecimal orderItemPrice;

    @Column(name = "order_item_quantity", nullable = false)
    private int orderItemQuantity = 1;

    @Column(name = "order_item_amount", nullable = false,  scale = 2, precision = 19)
    private BigDecimal orderItemAmount;

    @Column(name = "order_item_staus", nullable = false, scale = 2, precision = 19)
    @Enumerated(EnumType.STRING)
    private OrderItemStatus orderItemStatus =  OrderItemStatus.ORDERED;

    @Column(name = "refundable_quantity", nullable = false)
    private int refundableQuantity;

    @Column(name = "refundable_amount", nullable = false, scale = 2, precision = 19)
    private BigDecimal refundableAmount;

    @Column(name = "returned_quantity", nullable = false)
    private int returnedQuantity = 0;

    @Column(name = "return_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReturnStatus returnStatus = ReturnStatus.NONE;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "last_return_requested_time")
    private LocalDateTime lastReturnRequestedTime;

    @Column(name = "last_return_completed_time")
    private LocalDateTime lastReturnCompletedTime;

    public OrderItem(Long orderId, Long saleId, String orderItemName, BigDecimal orderItemPrice,
                     int orderItemQuantity, BigDecimal orderItemAmount, OrderItemStatus orderItemStatus,
                     int refundableQuantity, BigDecimal refundableAmount, int returnedQuantity, ReturnStatus returnStatus, String returnReason) {
        this.orderId = orderId;
        this.saleId = saleId;
        this.orderItemName = orderItemName;
        this.orderItemPrice = orderItemPrice;
        this.orderItemQuantity = orderItemQuantity;
        this.orderItemAmount = orderItemAmount;
        this.orderItemStatus = orderItemStatus;
        this.refundableQuantity = refundableQuantity;
        this.refundableAmount = refundableAmount;
        this.returnedQuantity = returnedQuantity;
        this.returnStatus = returnStatus;
        this.returnReason = returnReason;
    }
}