package com.zeebra.domain.product.dto;

import com.querydsl.core.annotations.QueryProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductSearchResult(
        Long productId,
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelNumber,
        String ProductThumbnail,
        List<String> images,
        int reviewCount,
        int favoriteProductCount,
        LocalDateTime createdAt
) {

    @QueryProjection
    public ProductSearchResult {
    }
}
