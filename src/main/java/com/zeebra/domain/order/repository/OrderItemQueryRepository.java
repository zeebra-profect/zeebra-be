package com.zeebra.domain.order.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.order.dto.OrderItemResponse;
import com.zeebra.domain.order.dto.OrderOption;
import com.zeebra.domain.order.dto.ProductInfo;
import com.zeebra.domain.order.entity.OrderItem;
import com.zeebra.domain.order.entity.QOrderItem;
import com.zeebra.domain.product.entity.QOptionCombination;
import com.zeebra.domain.product.entity.QOptionName;
import com.zeebra.domain.product.entity.QProduct;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QSales;
import com.zeebra.domain.product.entity.Sales;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderItemQueryRepository {

	private final QOrderItem orderItem = QOrderItem.orderItem;
	private final QSales sales = QSales.sales;
	private final QProductOption productOption = QProductOption.productOption;
	private final QProduct product = QProduct.product;
	private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
	private final QOptionName optionName = QOptionName.optionName;

	private final JPAQueryFactory queryFactory;
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
	public ProductInfo findProductInfoBySaleId(Long saleId) {
		if (saleId == null) {
			log.error("[상품 정보 조회 실패] saleId가 null입니다.");
			return null;
		}

		List<Tuple> results;
		try {
			results = queryFactory
				.select(
					sales.id,
					productOption.id,
					product.name,
					product.thumbnail,
					optionName.name,
					optionName.value
				)
				.from(sales)
				.join(productOption).on(productOption.id.eq(sales.productOptionId))
				.join(product).on(product.id.eq(productOption.productId))
				.leftJoin(optionCombination).on(optionCombination.productOptionId.eq(productOption.id))
				.leftJoin(optionName).on(optionName.id.eq(optionCombination.optionNameId))
				.where(sales.id.eq(saleId))
				.fetch();
		} catch (Exception e) {
			log.error("[상품 정보 조회 실패] 데이터베이스 조회 중 오류가 발생했습니다. saleId: {}, error: {}",
				saleId, e.getMessage(), e);
			return null;
		}

		if (results.isEmpty()) {
			log.warn("[상품 정보 조회 실패] 해당 saleId로 상품을 찾을 수 없습니다. saleId: {}", saleId);
			return null;
		}

		Tuple first = results.get(0);

		List<OrderOption> options = results.stream()
				.filter(tuple -> tuple.get(optionName.name) != null)
				.map(tuple -> OrderOption.of(
					tuple.get(optionName.name),
					tuple.get(optionName.value)
				))
				.distinct()
				.collect(Collectors.toList());

		return new ProductInfo(
				first.get(sales.id),
				first.get(productOption.id),
				first.get(product.name),
				first.get(product.thumbnail),
				options
			);
	}

	public List<OrderItemResponse> findOrderItemsByOrderId(Long orderId) {
		log.debug("[주문 아이템 조회 시작] orderId: {}", orderId);

		try {
			List<OrderItem> orderItems = queryFactory
				.selectFrom(orderItem)
				.where(orderItem.orderId.eq(orderId))
				.fetch();

			if (orderItems.isEmpty()) {
				log.warn("[주문 아이템 조회] 주문 아이템이 없습니다. orderId: {}", orderId);
				return List.of();
			}

			List<OrderItemResponse> responses = orderItems.stream()
				.map(item -> {
					ProductInfo productInfo = findProductInfoBySaleId(item.getSaleId());
					return OrderItemResponse.of(
						item,
						productInfo != null ? productInfo.productOptionId() : null,
						productInfo != null ? productInfo.orderItemOptions() : List.of()
					);
				})
				.collect(Collectors.toList());

			log.info("[주문 아이템 조회 성공] orderId: {}, itemCount: {}", orderId, responses.size());
			return responses;

		} catch (Exception e) {
			log.error("[주문 아이템 조회 실패] orderId: {}, error: {}", orderId, e.getMessage(), e);
			return List.of();
		}

	}
}