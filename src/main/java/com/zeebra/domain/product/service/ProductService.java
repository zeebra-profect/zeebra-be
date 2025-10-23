package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.ProductDetailResponse;

public interface ProductService {

    public ProductDetailResponse getProductDetail(Long productId);
}
