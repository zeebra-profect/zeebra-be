package com.zeebra.domain.sales.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class Sales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productOptionId;

    private Long memberId;

    private BigDecimal price;

    private BigDecimal soldPrice;

    private int stock;

    private SalesStatus salesStatus;

    private LocalDateTime soldAt;
}
