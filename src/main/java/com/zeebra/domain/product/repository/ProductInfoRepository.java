package com.zeebra.domain.product.repository;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.order.dto.OrderOption;
import com.zeebra.domain.order.dto.ProductInfo;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QProductOptionMetaView;
import com.zeebra.domain.product.entity.QSales;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductInfoRepository {
	private final JPAQueryFactory queryFactory;
	private final ObjectMapper objectMapper;
	private final QProductOption productOption = QProductOption.productOption;
	private final QSales sales = QSales.sales;
	private final QProductOptionMetaView productOptionMetaView = QProductOptionMetaView.productOptionMetaView;

	public ProductInfo findProductInfoBySaleId(Long saleId) {
		if (saleId == null) {
			return null;
		}

		Tuple meta = queryFactory
			.select(
				sales.productOptionId,
				productOptionMetaView.productName,
				productOptionMetaView.productThumbnail,
				productOptionMetaView.optionListJson
			)
			.from(sales)
			.join(productOptionMetaView).on(productOptionMetaView.productOptionId.eq(sales.productOptionId))
			.where(sales.id.eq(saleId))
			.fetchOne();

		if (meta == null) {
			return null;
		}

		Long productOptionId = meta.get(sales.productOptionId);
		String productName = meta.get(productOptionMetaView.productName);
		String productThumbnail = meta.get(productOptionMetaView.productThumbnail);
		String optionJson = meta.get(productOptionMetaView.optionListJson);

		List<OrderOption> options = parseOptions(optionJson);

		return new ProductInfo(
			saleId,
			productOptionId,
			productName,
			productThumbnail,
			options
		);
	}

	private List<OrderOption> parseOptions(String optionJson) {
		if (optionJson == null) {
			return List.of();
		}

		try {
			List<Map<String, String>> raw = objectMapper.readValue(
				optionJson,
				new TypeReference<List<Map<String, String>>>() {}
			);

			return raw.stream()
				.map(m -> OrderOption.of(m.get("name"), m.get("value")))
				.distinct()
				.toList();
		} catch (Exception e) {
			return List.of();
		}
	}
}