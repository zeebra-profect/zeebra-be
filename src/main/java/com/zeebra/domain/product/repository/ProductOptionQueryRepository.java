package com.zeebra.domain.product.repository;

import com.querydsl.core.QueryFactory;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.product.dto.ColorOptionResponse;
import com.zeebra.domain.product.dto.SizeOptionResponse;
import com.zeebra.domain.product.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductOptionQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QProductOption productOption = QProductOption.productOption;
    private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
    private final QProduct product = QProduct.product;
    private final QOptionName optionName = QOptionName.optionName;
    private final QSales sales = QSales.sales;
    private final QOptionCombination colorOptionCombination = new QOptionCombination("colorOptionCombination");
    private final QOptionName colorOptionName = new QOptionName("colorOptionName");
    private final QOptionCombination sizeOptionCombination = new QOptionCombination("sizeOptionCombination");
    private final QOptionName sizeOptionName = new QOptionName("sizeOptionName");

    public List<OptionName> findAllOptionNames(Long productId) {
        return queryFactory
                .selectDistinct(optionName)
                .from(optionName)
                .join(optionCombination).on(optionCombination.optionNameId.eq(optionName.id))
                .join(productOption).on(productOption.id.eq(optionCombination.productOptionId))
                .where(productOption.productId.eq(productId),
                        optionName.name.eq("color"))
                .orderBy(optionName.id.asc())
                .fetch();
    }

    public List<SizeOptionResponse> findByColorOptionName(Long colorOptionNameId, Long productId) {
        return queryFactory
                .select(Projections.constructor(
                        SizeOptionResponse.class,
                        sizeOptionCombination.id,
                        sizeOptionName.value,
                        productOption.id,
                        sales.price.min()
                ))
                .from(productOption)
                .join(colorOptionCombination).on(colorOptionCombination.productOptionId.eq(productOption.id))
                .join(colorOptionName).on(colorOptionName.id.eq(colorOptionCombination.optionNameId))
                .join(sizeOptionCombination).on(sizeOptionCombination.productOptionId.eq(productOption.id))
                .join(sizeOptionName).on(sizeOptionName.id.eq(sizeOptionCombination.optionNameId))
                .join(sales).on(sales.productOptionId.eq(productOption.id))
                .where(
                        productOption.productId.eq(productId),
                        colorOptionName.id.eq(colorOptionNameId),
                        sizeOptionName.name.eq("size")
                )
                .groupBy(
                        sizeOptionCombination.id,
                        sizeOptionName.value,
                        productOption.id
                )
                .orderBy(sizeOptionName.value.asc(), sizeOptionCombination.id.asc())
                .fetch();
    }


    public List<ProductOption> getProductOptions(Long productId, Long optionNameId) {
        return queryFactory
                .selectFrom(productOption)
                .join(optionCombination).on(optionCombination.productOptionId.eq(productOption.id))
                .join(optionName).on(optionName.id.eq(optionCombination.optionNameId))
                .where(productOption.productId.eq(productId),
                        optionName.id.eq(optionNameId))
                .fetch();
    }
}
