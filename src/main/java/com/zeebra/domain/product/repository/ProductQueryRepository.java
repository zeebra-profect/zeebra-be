package com.zeebra.domain.product.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.brand.entity.QBrand;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.category.entity.QCategory;
import com.zeebra.domain.product.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Slf4j
public class ProductQueryRepository {

    private final QProduct product = QProduct.product;
    private final QProductOption productOption = QProductOption.productOption;
    private final QSales sales = QSales.sales;
    private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
    private final QOptionName optionName = QOptionName.optionName;
    private final QFavoriteProduct favoriteProduct = QFavoriteProduct.favoriteProduct;
    private final QBrand brand = QBrand.brand;
    private final QCategory category = QCategory.category;

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;


    private String preprocessKeyword(String keyword) {
        String cleaned = keyword.trim()
                .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ");

        String[] words = cleaned.split(" ");
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                query.append(words[i]).append(":*");
                if (i < words.length - 1) {
                    query.append(" & ");
                }
            }
        }

        return query.toString();
    }

    public List<Product> searchProduct(String keyword,
                                       List<Long> categoryIds,
                                       List<Long> brandIds,
                                       Pageable pageable,
                                       ProductSort productSort) {
        if (keyword == null || keyword.isBlank()) {
            return searchWithoutKeyword(categoryIds, brandIds, pageable, productSort);
        }

        return searchWithKeyword(keyword, categoryIds, brandIds, pageable, productSort);
    }

    private List<Product> searchWithoutKeyword(List<Long> categoryIds,
                                               List<Long> brandIds,
                                               Pageable pageable,
                                               ProductSort productSort) {
        return queryFactory
                .selectFrom(product)
                .where(
                        findByBrandId(brandIds),
                        findByCategoryId(categoryIds)
                )
                .orderBy(buildOrderSpecifier(productSort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<Product> searchWithKeyword(String keyword, List<Long> categoryIds, List<Long> brandIds, Pageable pageable, ProductSort productSort) {
        String processedQuery = preprocessKeyword(keyword);
        String secondaryOrder = buildSecondaryOrder(productSort);
        String sql = """
                SELECT p.*
                FROM product_search_document psd
                JOIN product p ON p.id = psd.product_id
                WHERE psd.search_vector @@ to_tsquery('simple', :query)
                %s
                %s
                ORDER BY %s p.id DESC 
                OFFSET :offset 
                LIMIT :limit 
                """
                .formatted(categoryIds != null && !categoryIds.isEmpty() ? "AND psd.category_id = ANY(:categoryIds)" : "",
                        brandIds != null && !brandIds.isEmpty() ? "AND psd.brand_id = ANY(:brandIds)" : "", secondaryOrder);
        Query query = em.createNativeQuery(sql, Product.class);
        query.setParameter("query", processedQuery);
        query.setParameter("offset", pageable.getOffset());
        query.setParameter("limit", pageable.getPageSize());
        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }
        List resultList = query.getResultList();
        em.clear();
        return resultList;
    }


    private String buildSecondaryOrder(ProductSort productSort) {
        if (productSort == null) {
            return "p.review_count DESC, ";  // 기본값: 리뷰 많은 순
        }

        return switch (productSort) {
            case REVIEW_COUNT_MOST -> "p.review_count DESC, ";
            case REVIEW_COUNT_LEAST -> "p.review_count ASC, ";
        };
    }

    public List<Brand> filteredBrandWithoutKeyword(List<Long> categoryIds,
                                                   List<Long> brandIds) {

        if (categoryIds != null && !categoryIds.isEmpty()) {
            return queryFactory
                    .selectDistinct(brand)
                    .from(product)
                    .join(brand).on(brand.id.eq(product.brandId))
                    .where(
                            product.categoryId.in(categoryIds),
                            brandIds != null && !brandIds.isEmpty() ?
                                    brand.id.in(brandIds) : null
                    )
                    .fetch();
        }

        return queryFactory
                .selectDistinct(brand)
                .from(brand)
                .where(brandIds != null && !brandIds.isEmpty() ?
                        brand.id.in(brandIds) : null)
                .fetch();
    }

    public List<Brand> filteredBrand(String keyword,
                                     List<Long> categoryIds,
                                     List<Long> brandIds) {
        String processedQuery = preprocessKeyword(keyword);

        String sql = """
                WITH filtered AS (
                    SELECT DISTINCT psd.brand_id
                    FROM product_search_document psd
                    WHERE psd.search_vector @@ to_tsquery('simple', :query || ':*')
                      AND ts_rank(psd.search_vector, to_tsquery('simple', :query || ':*')) >= 0.3
                      %s
                      %s
                    ORDER BY 
                        ts_rank(psd.search_vector, to_tsquery('simple', :query || ':*')) DESC,
                        psd.product_id DESC
                    LIMIT 1000
                )
                SELECT b.*
                FROM filtered f
                INNER JOIN brand b ON b.id = f.brand_id
                ORDER BY b.id
                """.formatted(
                categoryIds != null && !categoryIds.isEmpty() ? "AND psd.category_id = ANY(:categoryIds)" : "",
                brandIds != null && !brandIds.isEmpty() ? "AND psd.brand_id = ANY(:brandIds)" : ""
        );

        Query query = em.createNativeQuery(sql, Brand.class);
        query.setParameter("query", processedQuery);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }

        return query.getResultList();
    }

    public List<Category> filteredCategoryWithoutKeyword(List<Long> categoryIds,
                                                         List<Long> brandIds) {
        if (brandIds != null && !brandIds.isEmpty()) {
            return queryFactory
                    .selectDistinct(category)
                    .from(product)
                    .join(category).on(category.id.eq(product.categoryId))
                    .where(
                            product.brandId.in(brandIds),
                            categoryIds != null && !categoryIds.isEmpty() ?
                                    category.id.in(categoryIds) : null
                    )
                    .fetch();
        }
        return queryFactory
                .selectDistinct(category)
                .from(category)
                .where(categoryIds != null && !categoryIds.isEmpty() ?
                        category.id.in(categoryIds) : null)
                .fetch();
    }

    public List<Category> filteredCategory(String keyword,
                                           List<Long> categoryIds,
                                           List<Long> brandIds) {
        String processedQuery = preprocessKeyword(keyword);

        String sql = """
                WITH filtered AS (
                    SELECT DISTINCT psd.category_id
                    FROM product_search_document psd
                    WHERE psd.search_vector @@ to_tsquery('simple', :query || ':*')
                      AND ts_rank(psd.search_vector, to_tsquery('simple', :query || ':*')) >= 0.3
                      %s
                      %s
                    ORDER BY 
                        ts_rank(psd.search_vector, to_tsquery('simple', :query || ':*')) DESC,
                        psd.product_id DESC
                    LIMIT 1000
                )
                SELECT c.*
                FROM filtered f
                INNER JOIN category c ON c.id = f.category_id
                ORDER BY c.id
                """.formatted(
                categoryIds != null && !categoryIds.isEmpty() ? "AND psd.category_id = ANY(:categoryIds)" : "",
                brandIds != null && !brandIds.isEmpty() ? "AND psd.brand_id = ANY(:brandIds)" : ""
        );

        Query query = em.createNativeQuery(sql.toString(), Category.class);
        query.setParameter("query", processedQuery);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }

        return query.getResultList();
    }

    public Map<Long, BigDecimal> lowPriceOfProductList(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tuple> fetch = queryFactory
                .select(sales.price.min(),
                        productOption.productId)
                .from(productOption)
                .join(sales).on(sales.productOptionId.eq(productOption.id))
                .where(productOption.productId.in(productIds))
                .groupBy(productOption.productId)
                .fetch();

        return fetch.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(productOption.productId),
                        tuple -> tuple.get(sales.price.min())
                ));
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


    private BooleanExpression findByCategoryId(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return product.categoryId.in(categoryIds);
    }

    private BooleanExpression findByBrandId(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) return null;
        return product.brandId.in(brandIds);
    }

    private OrderSpecifier<?>[] buildOrderSpecifier(ProductSort productSort) {
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

        orderSpecifiers.add(product.id.desc());

        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }
}