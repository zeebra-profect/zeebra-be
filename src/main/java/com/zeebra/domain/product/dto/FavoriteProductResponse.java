package com.zeebra.domain.product.dto;

import java.time.LocalDateTime;

public record FavoriteProductResponse(
        Long favoriteProductId,
        Long productId,
        Long memberId,
        LocalDateTime createdAt
) {
}
