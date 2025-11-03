package com.zeebra.domain.product.dto;

public record GetFavoriteProductResponse(
        Long productId,
        Long brandId,
        Long categoryId,
        String productName,
        String productDescription,
        String modelNumber,
        String ProductThumbnail
) {
}
