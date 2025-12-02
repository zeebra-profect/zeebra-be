package com.zeebra.domain.product.dto;

import com.zeebra.domain.brand.dto.BrandResponse;

import java.util.List;

public record SearchBrandListResponse(
        List<BrandResponse> searchedBrandList
) {
}
