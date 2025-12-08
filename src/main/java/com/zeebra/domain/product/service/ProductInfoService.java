package com.zeebra.domain.product.service;

import com.zeebra.domain.order.dto.ProductInfo;

public interface ProductInfoService {
	ProductInfo getProductInfoBySalesId(Long salesId);
}