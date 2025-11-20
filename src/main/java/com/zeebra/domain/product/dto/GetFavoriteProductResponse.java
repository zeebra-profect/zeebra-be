package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.Product;

public record GetFavoriteProductResponse(
        Long productId,
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelNumber,
        String productThumbnail
) {

    public static GetFavoriteProductResponse from(Product product) {
        return new GetFavoriteProductResponse(
                product.getId(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getModelNumber(),
                product.getThumbnail());
    }
}