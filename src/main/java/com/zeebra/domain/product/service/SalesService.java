package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.global.ApiResponse;

public interface SalesService {

    public ApiResponse<SalesResponse> createSales(Long memberId, SalesRequest request);

    public ApiResponse<Void> deleteSales(Long memberId, Long salesId);
}
