package com.zeebra.domain.product.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.product.dto.SalesDetailResponse;
import com.zeebra.domain.product.dto.SalesItemOptions;
import com.zeebra.domain.product.entity.QOptionCombination;
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
	private final QOptionName optionName = QOptionName.optionName;
	private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
    private final JPAQueryFactory queryFactory;

    public BigDecimal cheapestSalesPrice(Long productOptionId) {
        return queryFactory
                .select(sales.price.min())
                .from(sales)
                .join(productOption).on(sales.productOptionId.eq(productOption.id))
                .where(productOption.id.eq(productOptionId))
                .fetchOne();
    }
	public List<Sales> findCheapestAndOldestSales(Map<Long, Integer> productOptionQuantityMap ){
		if (productOptionQuantityMap == null || productOptionQuantityMap.isEmpty()) {
			log.error("[최저가 Sale 목록 조회 실패] productOptionQuantityMap이 null이거나 비어있습니다.");
			return List.of();
		}

		try {
			List<Sales> allSales = productOptionQuantityMap.entrySet().stream()
				.filter(entry -> entry.getValue() != null && entry.getValue() > 0)
				.flatMap(entry -> {
					Long productOptionId = entry.getKey();
					Integer quantity = entry.getValue();

					List<Sales> sales = queryFactory
						.select(QSales.sales)
						.from(QSales.sales)
						.where(QSales.sales.productOptionId.eq(productOptionId))
						.where(QSales.sales.salesStatus.eq(SalesStatus.ON_SALE))
						.orderBy(
							QSales.sales.price.asc(),
							QSales.sales.createdTime.asc()
						)
						.limit(quantity)
						.fetch();

					if (sales.size() < quantity) {
						log.warn("[최저가 Sale 조회 경고] 요청 수량보다 적게 조회되었습니다. productOptionId: {}, 요청: {}개, 조회: {}개",
							productOptionId, quantity, sales.size());
					}

					return sales.stream();
				})
				.collect(Collectors.toList());

			return allSales;
		} catch (Exception e){
			log.error("[최저가 Sale 목록 조회 실패] error: {}", e.getMessage(), e);
			return List.of();
		}
	}

	public Sales findCheapestAndOldestSales(Long productOptionId){

		Sales result = queryFactory
			.selectFrom(sales)
			.where(
				sales.productOptionId.eq(productOptionId),
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
			.join(optionCombination).on(productOption.id.eq(optionCombination.productOptionId))
			.join(optionName).on(optionCombination.optionNameId.eq(optionName.id))
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

	public Page<SalesDetailResponse> findSalesDetailsByConditions(Long memberId, LocalDate startDate, LocalDate endDate, SalesStatus salesStatus, Pageable pageable) {
		List<Long> salesIds = queryFactory
			.select(sales.id)
			.from(sales)
			.where(
				memberIdEq(memberId),
				createdTimeBetween(startDate, endDate),
				salesStatusEq(salesStatus)
			)
			.orderBy(sales.createdTime.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		if (salesIds.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, 0L);
		}

		Long total = queryFactory
			.select(sales.count())
			.from(sales)
			.where(
				memberIdEq(memberId),
				createdTimeBetween(startDate, endDate),
				salesStatusEq(salesStatus)
			)
			.fetchOne();

		List<SalesDetailResponse> detailResponses = salesIds.stream()
			.map(salesId -> findSalesDetailById(salesId, memberId))
			.filter(detail -> detail != null)
			.collect(Collectors.toList());

		return new PageImpl<>(detailResponses, pageable, total != null ? total : 0L);
	}

	private BooleanExpression memberIdEq(Long memberId) {
		return memberId != null ? sales.memberId.eq(memberId) : null;
	}

	private BooleanExpression createdTimeBetween(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null) {
			LocalDateTime startDateTime = startDate.atStartOfDay();
			LocalDateTime endDateTime = endDate.atStartOfDay();
			return sales.createdTime.between(startDateTime, endDateTime);
		}
		if (startDate != null) {
			return sales.createdTime.goe(startDate.atStartOfDay());
		}
		if (endDate != null) {
			return sales.createdTime.lt(endDate.atStartOfDay());
		}
		return null;
	}

	private BooleanExpression salesStatusEq(SalesStatus salesStatus) {
		return salesStatus != null ? sales.salesStatus.eq(salesStatus) : null;
	}

}