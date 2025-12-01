package com.zeebra.domain.product.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.zeebra.domain.order.dto.OrderItemLine;
import com.zeebra.domain.order.dto.OrderItemResponse;
import com.zeebra.domain.product.dto.OrderSalesItem;
import com.zeebra.domain.product.dto.SalesDetailResponse;
import com.zeebra.domain.product.dto.SalesListResponse;
import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.dto.UserSalesItem;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.global.ApiResponse;

public interface SalesService {

    public SalesResponse createSales(Long memberId, SalesRequest request);

    public ApiResponse<Void> deleteSales(Long memberId, Long salesId);

	OrderSalesItem findCheapestSalesByProductOptionId(Long productOptionId);

	SalesDetailResponse getSalesDetail(Long memberId, Long salesId);

	SalesListResponse getSalesList(Long memberId, LocalDate startDate, LocalDate endDate, SalesStatus salesStatus, Pageable pageable);

    List<UserSalesItem> findSalesByMemberId(Long memberId);

	List<OrderSalesItem> selectCheapestValidSales(Map<Long, Integer> productOptionQuantityMap);

	void validateSales(Long salesId, int quantity	);

	void validatePurchasable(List<OrderItemLine> itemLines);

	void reserveSales(List<OrderItemResponse> orderItems);

	void cancelSales(List<OrderItemResponse> orderItems);

	void confirmSales(List<OrderItemResponse> orderItems);
}