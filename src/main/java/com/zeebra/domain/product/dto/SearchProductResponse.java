package com.zeebra.domain.product.dto;

import java.util.List;

public record SearchProductResponse(
        List<ProductSearchItem> products,
        SearchProductPagination pagination
) {
    public static SearchProductResponse of(
            List<ProductSearchItem> products,
            SearchProductPagination pagination
    ) {
        return new SearchProductResponse(products, pagination);
    }
}