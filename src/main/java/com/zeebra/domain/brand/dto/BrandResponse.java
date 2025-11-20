package com.zeebra.domain.brand.dto;

import com.zeebra.domain.brand.entity.Brand;

import java.util.List;

public record BrandResponse(
        Long brandId,
        String brandName
) {
    public static List<BrandResponse> toListBrandResponse(List<Brand> brands) {
        return brands.stream()
                .map(brand -> new BrandResponse(brand.getId(), brand.getName()))
                .toList();
    }
}

