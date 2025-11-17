package com.zeebra.domain.product.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.brand.entity.QBrand;
import com.zeebra.domain.brand.repository.BrandRepository;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.category.entity.QCategory;
import com.zeebra.domain.product.dto.SizeOptionResponse;
import com.zeebra.domain.product.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.zeebra.domain.product.entity.QProductSearchMv.productSearchMv;

@RequiredArgsConstructor
@Repository
public class ProductQueryRepository {

    private final QProduct product = QProduct.product;
    private final QBrand brand = QBrand.brand;
    private final QCategory category = QCategory.category;
    private final QProductOption productOption = QProductOption.productOption;
    private final QSales sales = QSales.sales;
    private final JPAQueryFactory queryFactory;
    private final QProductSearchMv productSearchMv = QProductSearchMv.productSearchMv;
    private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
    private final QOptionName optionName = QOptionName.optionName;
    private final QFavoriteProduct favoriteProduct = QFavoriteProduct.favoriteProduct;

    public List<Product> searchProduct(String keyword,
                                       List<Long> categoryIds,
                                       List<Long> brandIds,
                                       Pageable pageable,
                                       ProductSort productSort) {
        if (keyword == null) {
            return queryFactory
                    .selectFrom(product)
                    .where(
                            findByBrandId(brandIds),
                            findByCategoryId(categoryIds)
                    )
                    .orderBy(buildOrderSpecifier(productSort, null)) // 기본 정렬
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();
        }

        StringTemplate normKeyword = Expressions.stringTemplate(
                "hangul_normalize({0})",
                Expressions.constant(keyword == null ? "" : keyword.trim())
        );

        NumberExpression<Double> nameScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.productNameNorm,
                normKeyword
        );

        NumberExpression<Double> brandScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.brandNameNorm,
                normKeyword
        );

        NumberExpression<Double> descScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.descriptionNorm,
                normKeyword
        );

        NumberExpression<Double> totalScore = nameScore.multiply(3.0)
                .add(brandScore.multiply(2.0))
                .add(descScore.multiply(1.0));

        BooleanExpression scoreFilter = totalScore.goe(0.1);

        return queryFactory
                .select(product)
                .from(productSearchMv)
                .join(product).on(product.id.eq(productSearchMv.id))
                .where(
                        scoreFilter,
                        findByCategoryId(categoryIds),
                        findByBrandId(brandIds)
                )
                .orderBy(buildOrderSpecifier(productSort, totalScore))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }


    public List<Brand> filteredBrand(String keyword, List<Long> categoryIds,
                                     List<Long> brandIds) {
        String safeWord = (keyword == null) ? "" : keyword.trim();
        boolean isShort = safeWord.length() < 3;

        StringTemplate normKeyword = Expressions.stringTemplate(
                "hangul_normalize({0})",
                Expressions.constant(keyword == null ? "" : keyword.trim())
        );

        NumberExpression<Double> nameScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.productNameNorm,
                normKeyword
        );

        NumberExpression<Double> brandScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.brandNameNorm,
                normKeyword
        );

        NumberExpression<Double> descScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.descriptionNorm,
                normKeyword
        );

        NumberExpression<Double> totalScore = nameScore.multiply(3.0)
                .add(brandScore.multiply(2.0))
                .add(descScore.multiply(1.0));

        BooleanExpression scoreFilter = totalScore.goe(0.1);


        return queryFactory
                .selectDistinct(brand)
                .from(productSearchMv)
                .join(product).on(product.id.eq(productSearchMv.id))
                .join(brand).on(brand.id.eq(product.brandId))
                .where(
                        scoreFilter,
                        findByCategoryId(categoryIds),
                        findByBrandId(brandIds)
                )
                .orderBy(brand.id.asc())
                .fetch();
    }


    public List<Category> filteredCategory(String keyword, List<Long> categoryIds, List<Long> brandIds) {

        String safeWord = (keyword == null) ? "" : keyword.trim();
        boolean isShort = safeWord.length() < 3;

        StringTemplate normKeyword = Expressions.stringTemplate(
                "hangul_normalize({0})",
                Expressions.constant(keyword == null ? "" : keyword.trim())
        );

        NumberExpression<Double> nameScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.productNameNorm,
                normKeyword
        );

        NumberExpression<Double> brandScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.brandNameNorm,
                normKeyword
        );

        NumberExpression<Double> descScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.descriptionNorm,
                normKeyword
        );

        NumberExpression<Double> totalScore = nameScore.multiply(3.0)
                .add(brandScore.multiply(2.0))
                .add(descScore.multiply(1.0));

        BooleanExpression scoreFilter = totalScore.goe(0.1);


        return queryFactory
                .selectDistinct(category)
                .from(productSearchMv)
                .join(product).on(product.id.eq(productSearchMv.id))
                .join(category).on(category.id.eq(product.categoryId))
                .where(
                        scoreFilter,
                        findByCategoryId(categoryIds),
                        findByBrandId(brandIds)
                )
                .orderBy(category.id.asc())
                .fetch();
    }


    public BigDecimal lowPriceOfProduct(Long productId) {
        return queryFactory
                .select(sales.price.min())
                .from(productOption)
                .join(sales).on(sales.productOptionId.eq(productOption.id))
                .where(productOption.productId.eq(productId))
                .fetchOne();
    }

    public BigDecimal lowPriceOfColor(Long productId, Long colorOptionId) {
        return queryFactory
                .select(sales.price.min())
                .from(productOption)
                .join(sales).on(sales.productOptionId.eq(productOption.id))
                .join(optionCombination).on(optionCombination.productOptionId.eq(productOption.id))
                .join(optionName).on(optionName.id.eq(optionCombination.optionNameId))
                .where(productOption.productId.eq(productId),
                        optionName.id.eq(colorOptionId))
                .fetchOne();
    }

    private BooleanExpression findByCategoryId(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return product.categoryId.in(categoryIds);
    }

    private BooleanExpression findByBrandId(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) return null;
        return product.brandId.in(brandIds);
    }


    public long countFiltered(String cleaned, List<Long> categoryIds, List<Long> brandIds) {
        if (cleaned == null) {
            Long c = queryFactory
                    .select(product.count())
                    .from(product)
                    .where(
                            findByBrandId(brandIds),
                            findByCategoryId(categoryIds)
                    ).fetchOne();
            return c != null ? c : 0L;
        }
        String cleanedKeyword = (cleaned == null || cleaned.trim().isEmpty()) ? "" : cleaned.trim();

        StringTemplate normKeyword = Expressions.stringTemplate(
                "hangul_normalize({0})", Expressions.constant(cleanedKeyword == null ? "" : cleanedKeyword)
        );

        NumberExpression<Double> nameScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.productNameNorm,
                normKeyword
        );

        NumberExpression<Double> brandScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.brandNameNorm,
                normKeyword
        );

        NumberExpression<Double> descScore = Expressions.numberTemplate(
                Double.class,
                "CAST(similarity({0}, {1}) AS double)",
                productSearchMv.descriptionNorm,
                normKeyword
        );

        NumberExpression<Double> totalScore = nameScore.multiply(3.0)
                .add(brandScore.multiply(2.0))
                .add(descScore.multiply(1.0));

        BooleanExpression scoreFilter = totalScore.goe(0.1);
        Long c = queryFactory
                .select(product.count())
                .from(productSearchMv)
                .join(product).on(product.id.eq(productSearchMv.id))
                .where(
                        scoreFilter,
                        findByBrandId(brandIds),
                        findByCategoryId(categoryIds)
                ).fetchOne();
        return c != null ? c : 0L;
    }


    private OrderSpecifier<?>[] buildOrderSpecifier(ProductSort productSort,
                                                    NumberExpression<Double> score) {
        QProduct product = QProduct.product;
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (productSort != null) {
            switch (productSort) {
                case REVIEW_COUNT_LEAST:
                    orderSpecifiers.add(product.reviewCount.asc());
                    break;
                case REVIEW_COUNT_MOST:
                    orderSpecifiers.add(product.reviewCount.desc());
                    break;
            }
        }

        if (score != null) {
            orderSpecifiers.add(score.desc());
        }

        orderSpecifiers.add(product.id.desc());

        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }

    public List<Product> getFavoriteProducts(Long memberId) {
        return queryFactory
                .selectFrom(product)
                .join(favoriteProduct).on(favoriteProduct.productId.eq(product.id))
                .where(favoriteProduct.memberId.eq(memberId))
                .orderBy(favoriteProduct.createdTime.desc())
                .fetch();
    }

    public long countFavoriteProducts(Long memberId) {
        Long count = queryFactory
                .select(product.count())
                .from(product)
                .join(favoriteProduct).on(favoriteProduct.productId.eq(product.id))
                .where(favoriteProduct.memberId.eq(memberId))
                .fetchOne();
        return count != null ? count : 0L;
    }
}
