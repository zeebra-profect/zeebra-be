package com.zeebra.domain.category.dto;

public record CategoryResponse(
        Long categoryId,
        Long parentCategoryId,
        String categoryName
) {
}
