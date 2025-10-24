package com.zeebra.domain.product.repository;

import com.querydsl.core.QueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QSales;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@Slf4j
@RequiredArgsConstructor
public class SalesQueryRepository {

    private final QProductOption productOption = QProductOption.productOption;
    private final QSales sales = QSales.sales;
    private final JPAQueryFactory queryFactory;

    public BigDecimal cheapestSalesPrice(Long productOptionId) {
        return queryFactory
                .select(sales.price.min())
                .from(sales)
                .join(productOption).on(sales.productOptionId.eq(productOption.id))
                .where(productOption.id.eq(productOptionId))
                .fetchOne();
    }
}
