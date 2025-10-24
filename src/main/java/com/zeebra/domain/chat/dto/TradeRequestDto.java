package com.zeebra.domain.chat.dto;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TradeRequestDto {
    private BigDecimal price;
}
