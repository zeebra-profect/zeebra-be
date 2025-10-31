package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.*;
import com.zeebra.global.ApiResponse;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId, Long colorOptionNameId);

    public ApiResponse<SizeOptionResponseList> getProductOptionSize(Long productId, Long colorOptionNameId);

    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<Void> deleteFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<ProductResponse> createProduct(Long memberId, ProductRequest request);

    public ApiResponse<SearchProductResponse> searchProduct(String keyWord, List<Long> categoryIds, List<Long> brandIds, Pageable pageable, String productSort);

    public ApiResponse<FavoriteProductList> getFavoriteProduct(Long memberId, Pageable pageable);
}
