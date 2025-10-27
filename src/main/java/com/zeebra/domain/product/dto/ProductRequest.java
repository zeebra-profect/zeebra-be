package com.zeebra.domain.product.dto;

import java.util.List;

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
