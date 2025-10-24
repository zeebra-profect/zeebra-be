package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.SalesStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalesResponse(
        Long salesId,
        Long productOptionId,
        BigDecimal price,
        SalesStatus salesStatus,
        int stock,
        LocalDateTime createdAt
) {
}
