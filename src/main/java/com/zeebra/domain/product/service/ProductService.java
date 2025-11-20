package com.zeebra.domain.product.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.zeebra.domain.product.dto.FavoriteProductList;
import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.dto.ProductRequest;
import com.zeebra.domain.product.dto.ProductResponse;
import com.zeebra.domain.product.dto.SearchProductResponse;
import com.zeebra.domain.product.dto.SizeOptionResponseList;
import com.zeebra.global.ApiResponse;

public interface ProductService {

    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId, Long colorOptionNameId);

    public ApiResponse<SizeOptionResponseList> getProductOptionSize(Long productId, Long colorOptionNameId);

    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<Void> deleteFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<ProductResponse> createProduct(Long memberId, ProductRequest request);

    public ApiResponse<SearchProductResponse> searchProduct(String keyWord, List<Long> categoryIds, List<Long> brandIds, Pageable pageable, String productSort);

    public ApiResponse<FavoriteProductList> getFavoriteProduct(Long memberId, Pageable pageable);

	void validateProductOptionId(Long productOptionId);
}