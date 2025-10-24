package com.zeebra.domain.product.dto;

import java.util.List;

public record ProductListResponse(
        List<ProductDetailResponse> productDetailResponses,
        Pagination pagination
) {
}
