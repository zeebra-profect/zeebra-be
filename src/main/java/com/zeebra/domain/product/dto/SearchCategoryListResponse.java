package com.zeebra.domain.product.dto;

import com.zeebra.domain.category.dto.CategoryResponse;

import java.util.List;

public record SearchCategoryListResponse(
        List<CategoryResponseDto> categoryResponses) {
}
