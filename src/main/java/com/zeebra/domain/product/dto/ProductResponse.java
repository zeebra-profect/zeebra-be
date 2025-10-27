package com.zeebra.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long productId,
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelNumber,
        String ProductThumbnail,
        List<String> images,
        LocalDateTime createdAt
) {
}
