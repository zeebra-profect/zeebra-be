package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.SalesStatus;

import java.math.BigDecimal;

public record SalesRequest(
        Long productOptionId,
        BigDecimal price,
        SalesStatus salesStatus,
        int stock
) {
}
