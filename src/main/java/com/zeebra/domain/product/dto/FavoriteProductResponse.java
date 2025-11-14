package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.FavoriteProduct;

import java.time.LocalDateTime;

public record FavoriteProductResponse(
        Long favoriteProductId,
        Long productId,
        Long memberId,
        LocalDateTime createdAt
) {
    public static FavoriteProductResponse toFavoriteProductResponse(FavoriteProduct favoriteProduct) {
        return new FavoriteProductResponse(
                favoriteProduct.getProductId(),
                favoriteProduct.getProductId(),
                favoriteProduct.getMemberId(),
                favoriteProduct.getCreatedTime());
    }
}
