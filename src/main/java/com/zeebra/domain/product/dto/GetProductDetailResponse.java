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
    public static GetProductDetailResponse of(Product product, BigDecimal lowPrice) {
        return new GetProductDetailResponse(
                product.getId(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getModelNumber(),
                product.getThumbnail(),
                product.getImages(),
                lowPrice,
                product.getReviewCount(),
                product.getFavoriteProductCount(),
                product.getCreatedTime());
    }
}
