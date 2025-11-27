package com.zeebra.domain.product.dto;

public record SearchProductPagination(
        int currentPage,
        int pageSize,
        boolean hasNext
) {
}
