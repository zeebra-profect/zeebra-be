package com.zeebra.domain.product.dto;

public record Pagination(
        int currentPage,
        int pageSize,
        Long totalCount,
        int totalPages
) {
}
