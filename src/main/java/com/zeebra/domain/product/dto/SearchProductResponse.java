package com.zeebra.domain.product.dto;

import com.zeebra.domain.brand.dto.BrandResponse;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.category.dto.CategorySearchResponse;

import java.util.List;

public record SearchProductResponse(
        List<GetProductDetailResponse> productDetailResponses,
        List<CategorySearchResponse> categoryResponses,
        List<BrandResponse> brandResponses,
        Pagination pagination
) {
    public static SearchProductResponse from(List<GetProductDetailResponse> productDetailResponseList,
                                             List<BrandResponse> brandListResponse,
                                             List<CategorySearchResponse> categorySearchResponseList,
                                             Pagination pagination) {
        return new SearchProductResponse(productDetailResponseList,
                categorySearchResponseList,
                brandListResponse,
                pagination);
    }
}
