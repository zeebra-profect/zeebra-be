package com.zeebra.domain.category.dto;

public record CategoryRequest(
        Long parentCategoryId,
        String categoryName
) {
}
