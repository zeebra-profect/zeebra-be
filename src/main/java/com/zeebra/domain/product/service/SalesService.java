package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;

public interface SalesService {

    public SalesResponse createSales(Long memberId, SalesRequest request);
}
