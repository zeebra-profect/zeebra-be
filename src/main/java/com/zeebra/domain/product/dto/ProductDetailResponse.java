package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
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
        LocalDateTime createdAt,
        List<ColorOptionResponse> colorOptionResponses,
        String colorValue
) {
    public static ProductDetailResponse from(Product product,
                                             BigDecimal lowPriceOfProduct,
                                             List<ColorOptionResponse> colorOptionResponses,
                                             String colorValue) {
        return new ProductDetailResponse(
                product.getId(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getModelNumber(),
                product.getThumbnail(),
                product.getImages(),
                lowPriceOfProduct,
                product.getReviewCount(),
                product.getFavoriteProductCount(),
                product.getCreatedTime(),
                colorOptionResponses,
                colorValue);
    }
}
