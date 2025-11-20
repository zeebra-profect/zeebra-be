package com.zeebra.domain.order.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.OrderItemLine;
import com.zeebra.domain.order.dto.OrderResponse;
import com.zeebra.domain.order.dto.ReadOrderListResponse;
import com.zeebra.domain.order.entity.OrderItemStatus;
import com.zeebra.domain.order.entity.OrderStatus;

public interface OrderService {
	CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request);

	OrderInfo getOrder(Long memberId, Long orderId);

	void updateOrderStatus(Long orderId, OrderStatus orderStatus, String idempotencyKey);

	void updateOrderItemStatus(Long orderId, Long orderItemId, OrderItemStatus orderItemStatus);

	void updateAllOrderItemsStatus(Long orderId, OrderItemStatus orderItemStatus);

	ReadOrderListResponse getOrderList(Long memberId, LocalDate startDate, LocalDate endDate, OrderStatus orderStatus, Pageable pageable);

	OrderResponse getOrderDetail(Long memberId, Long orderId);

	List<OrderItemLine> getOrderItemLine(Long aLong);
}