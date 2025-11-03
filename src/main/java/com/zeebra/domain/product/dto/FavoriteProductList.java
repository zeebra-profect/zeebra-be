package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.FavoriteProduct;

import java.util.List;

public record FavoriteProductList(
        Pagination pagination,
        List<GetFavoriteProductResponse>  favoriteProductResponses
) {
}
