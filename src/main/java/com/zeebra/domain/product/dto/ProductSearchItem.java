package com.zeebra.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductSearchItem(
        Long productId,
        String productName,
        String modelNumber,
        String description,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        String productThumbnail,
        List<String> images,
        BigDecimal minPrice,
        Integer reviewCount,
        Integer favoriteCount,
        LocalDateTime createdAt,
        Float score  // 검색 점수
) {
}