package com.zeebra.domain.product.repository;

import java.math.BigDecimal;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QSales;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

	public Sales findCheapestAndOldestSales(Long productOptionId){

		Sales result = queryFactory
			.select(sales)
			.from(sales)
			.join(productOption).on(sales.productOptionId.eq(productOption.id))
			.where(
				productOption.id.eq(productOptionId),
				sales.salesStatus.eq(SalesStatus.ON_SALE)
			)
			.orderBy(
				sales.price.asc(),
				sales.createdTime.asc()
			)
			.fetchFirst();
		
		if (result == null) {
			log.warn("[최저가 판매 조회 실패] ON_SALE 상태의 Sales가 없습니다. productOptionId: {}", productOptionId);
		}
		
		return result;
	}
}