package com.zeebra.domain.product.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductRequest(
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelName,
        String productThumbnail,
        List<String> productImages
) {
}
