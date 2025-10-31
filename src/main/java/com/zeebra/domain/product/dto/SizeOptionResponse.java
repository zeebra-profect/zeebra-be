package com.zeebra.domain.product.dto;

import java.math.BigDecimal;

public record SizeOptionResponse(
        Long optionCombinationId,
        String sizeValue,
        Long productOptionId,
        BigDecimal lowPriceOfSize) {
}
