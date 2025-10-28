package com.zeebra.domain.order.service;

import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;

public interface OrderService {
	CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request);
}