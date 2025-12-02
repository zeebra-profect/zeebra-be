package com.zeebra.domain.product.service;

import java.math.BigDecimal;
import java.util.List;

import com.zeebra.domain.product.dto.*;
import com.zeebra.domain.product.entity.ProductSort;
import org.springframework.data.domain.Pageable;

import com.zeebra.global.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;

public interface ProductService {

    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId, Long colorOptionNameId);

    public ApiResponse<SizeOptionResponseList> getProductOptionSize(Long productId, Long colorOptionNameId);

    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<Void> deleteFavoriteProduct(Long memberId, Long productId);

    public ApiResponse<ProductResponse> createProduct(Long memberId, ProductRequest request);

    public ApiResponse<SearchProductResponse> searchProduct(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            String productSort);

    public ApiResponse<FavoriteProductList> getFavoriteProduct(Long memberId, Pageable pageable);

    void validateProductOptionId(Long productOptionId);

    public ApiResponse<SuggestionListResponse> getSuggestions(String searchWord);

    public ApiResponse<SearchBrandListResponse> searchedBrands(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    );

    public ApiResponse<SearchCategoryListResponse> searchedCategories(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    );
}