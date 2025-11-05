package com.zeebra.domain.product.dto;

import com.zeebra.domain.product.entity.Sales;

import java.math.BigDecimal;

public record UserSalesItem(
        Long salesId,
        Long productOptionId,
        BigDecimal price
) {
    public static UserSalesItem from(Sales sales) {
        return new UserSalesItem(sales.getId(),
                sales.getProductOptionId(),
                sales.getPrice()
        );
    }
}
