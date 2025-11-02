package com.zeebra.domain.product.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.product.dto.SalesDetailResponse;
import com.zeebra.domain.product.dto.SalesItemOptions;
import com.zeebra.domain.product.entity.QOptionName;
import com.zeebra.domain.product.entity.QProduct;
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
	private final QProduct product = QProduct.product;
	private final QProductOption optionCombination = QProductOption.productOption;
	private final QOptionName optionName = QOptionName.optionName;
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

	public SalesDetailResponse findSalesDetailById(Long salesId, Long memberId) {
		Tuple baseInfo = queryFactory
			.select(
				sales.id,
				sales.productOptionId,
				sales.price,
				sales.soldPrice,
				sales.salesStatus,
				sales.stock,
				sales.createdTime,
				sales.soldAt,
				product.thumbnail,
				product.name
			)
			.from(sales)
			.join(productOption).on(sales.productOptionId.eq(productOption.id))
			.join(product).on(productOption.productId.eq(product.id))
			.where(
				sales.id.eq(salesId),
				sales.memberId.eq(memberId)
			)
			.fetchOne();

		if (baseInfo == null) {
			return null;
		}

		List<SalesItemOptions> options = queryFactory
			.select(optionName.name, optionName.value)
			.from(sales)
			.join(productOption).on(sales.productOptionId.eq(productOption.id))
			.join(optionName).on(productOption.id.eq(optionName.id))
			.where(sales.id.eq(salesId))
			.fetch()
			.stream()
			.filter(tuple -> tuple.get(optionName.name) != null)
			.map(tuple -> SalesItemOptions.of(
				tuple.get(optionName.name),
				tuple.get(optionName.value)
			))
			.distinct()
			.collect(Collectors.toList());


		return new SalesDetailResponse(
			baseInfo.get(sales.id),
			baseInfo.get(sales.productOptionId),
			baseInfo.get(sales.price),
			baseInfo.get(sales.soldPrice),
			baseInfo.get(sales.salesStatus),
			baseInfo.get(sales.stock),
			baseInfo.get(sales.createdTime),
			baseInfo.get(sales.soldAt),
			baseInfo.get(product.thumbnail),
			baseInfo.get(product.name),
			options
		);
	}
}