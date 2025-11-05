package com.zeebra.domain.product.service;

import java.time.LocalDate;
import java.util.List;

import com.zeebra.domain.product.dto.*;
import org.springframework.data.domain.Pageable;

import com.zeebra.domain.order.dto.SalesItem;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.global.ApiResponse;

public interface SalesService {

    public SalesResponse createSales(Long memberId, SalesRequest request);

    public ApiResponse<Void> deleteSales(Long memberId, Long salesId);

	SalesItem findCheapestSalesByProductOptionId(Long productOptionId);

	SalesDetailResponse getSalesDetail(Long memberId, Long salesId);

	SalesListResponse getSalesList(Long memberId, LocalDate startDate, LocalDate endDate, SalesStatus salesStatus, Pageable pageable);

    List<UserSalesItem> findSalesByMemberId(Long memberId);
}