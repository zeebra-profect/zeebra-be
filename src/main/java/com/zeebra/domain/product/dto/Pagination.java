package com.zeebra.domain.product.dto;

public record Pagination(
        int currentPage,
        int pageSize,
        int totalItems,
        int totalPages
) {
}
