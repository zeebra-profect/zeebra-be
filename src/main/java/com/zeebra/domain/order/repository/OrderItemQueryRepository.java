package com.zeebra.domain.order.repository;

import java.util.List;
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

	public ProductInfo findProductInfoBySaleId(Long saleId) {
		if (saleId == null) {
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
			return null;
		}

		if (results.isEmpty()) {
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