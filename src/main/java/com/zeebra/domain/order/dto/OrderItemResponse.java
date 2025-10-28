package com.zeebra.domain.order.dto;

import java.math.BigDecimal;
import java.util.List;

import com.zeebra.domain.order.entity.OrderItem;
import com.zeebra.domain.order.entity.OrderItemStatus;

public record OrderItemResponse(
	Long orderItemId,
	Long saleId,
	Long productOptionId,
	String orderItemName,
	String orderItemThumbnail,
	BigDecimal orderItemPrice,
	int orderItemQuantity,
	BigDecimal orderItemAmount,
	OrderItemStatus orderItemStatus,
	List<OrderOption> orderItemOptions
) {
	public static OrderItemResponse of(
		OrderItem orderItem,
		Long productOptionId,
		List<OrderOption> orderItemOptions) {
		return new OrderItemResponse(orderItem.getId(), orderItem.getSaleId(), productOptionId, orderItem.getOrderItemName(),
			orderItem.getOrderItemThumbnail(), orderItem.getOrderItemPrice(), orderItem.getOrderItemQuantity(), orderItem.getOrderItemAmount(), orderItem.getOrderItemStatus(), orderItemOptions) ;
	}
}