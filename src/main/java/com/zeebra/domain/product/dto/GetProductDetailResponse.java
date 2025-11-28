package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetProductDetailResponse(
        Long productId,
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelNumber,
        String ProductThumbnail,
        List<String> images,
        BigDecimal lowPrice,
        int reviewCount,
        int favoriteProductCount,
        LocalDateTime createdAt
) {
    public static GetProductDetailResponse of(ProductSearchResult productSearchResult, BigDecimal lowPrice) {
        return new GetProductDetailResponse(
                productSearchResult.productId(),
                productSearchResult.brandId(),
                productSearchResult.categoryId(),
                productSearchResult.productName(),
                productSearchResult.productDescription(),
                productSearchResult.modelNumber(),
                productSearchResult.ProductThumbnail(),
                productSearchResult.images(),
                lowPrice,
                productSearchResult.reviewCount(),
                productSearchResult.favoriteProductCount(),
                productSearchResult.createdAt());
    }
}
