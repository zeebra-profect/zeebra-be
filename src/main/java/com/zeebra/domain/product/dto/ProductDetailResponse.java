package com.zeebra.domain.product.dto;

import java.time.LocalDateTime;

public record ProductDetailResponse(
        Long productId,
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelNumber,
        String ProductThumbnail,
        int reviewCount,
        int favoriteProductCount,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
}
