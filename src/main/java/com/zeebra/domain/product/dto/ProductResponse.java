package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.Product;
import lombok.Builder;

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
        String productThumbnail,
        List<String> images,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getModelNumber(),
                product.getThumbnail(),
                product.getImages(),
                product.getCreatedTime()
        );
    }
}
