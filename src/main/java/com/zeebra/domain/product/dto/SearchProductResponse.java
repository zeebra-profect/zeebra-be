package com.zeebra.domain.product.dto;

import com.zeebra.domain.brand.dto.BrandResponse;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.category.dto.CategorySearchResponse;

import java.util.List;

public record SearchProductResponse(
        List<ProductDetailResponse> productDetailResponses,
        List<CategorySearchResponse> categoryResponses,
        List<BrandResponse> brandResponses,
        Pagination pagination
) {
}
