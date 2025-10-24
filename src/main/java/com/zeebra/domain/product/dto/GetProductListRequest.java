package com.zeebra.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record GetProductListRequest(
        String keyWord,
        List<Long> brandIds,
        List<Long> categoryIds,
        int page,
        int size
) {
}
