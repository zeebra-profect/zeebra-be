package com.zeebra.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TradeResponseDto {
    private Long tradeId;
    private Long chatRoomId;
    private BigDecimal price;
}
