package com.zeebra.domain.category.dto;

import com.zeebra.domain.category.entity.Category;

import java.util.List;

public record CategorySearchResponse(
        Long categoryId,
        String categoryName
) {
    public static List<CategorySearchResponse> toCategorySearchResponseList(List<Category> categories) {
        return categories.stream()
                .map(category -> new CategorySearchResponse(category.getId(), category.getName()))
                .toList();
    }
}
