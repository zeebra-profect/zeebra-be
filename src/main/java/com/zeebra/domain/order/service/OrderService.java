package com.zeebra.domain.order.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.dto.OrderInfo;
import com.zeebra.domain.order.dto.ReadOrderListResponse;
import com.zeebra.domain.order.entity.OrderItemStatus;
import com.zeebra.domain.order.entity.OrderStatus;

public interface OrderService {
	CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request);

	OrderInfo getOrder(Long memberId, Long orderId);

	void updateOrderStatus(Long orderId, OrderStatus orderStatus, String idempotencyKey);

	void updateAllOrderItemsStatus(Long orderId, OrderItemStatus orderItemStatus);

	ReadOrderListResponse getOrderList(Long memberId, LocalDate startDate, LocalDate endDate, OrderStatus orderStatus, Pageable pageable);
}