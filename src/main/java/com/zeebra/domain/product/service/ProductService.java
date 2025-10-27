package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.global.ApiResponse;

public interface ProductService {

    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId);

    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<Void> deleteFavoriteProduct(Long memberId, Long productId);
}
