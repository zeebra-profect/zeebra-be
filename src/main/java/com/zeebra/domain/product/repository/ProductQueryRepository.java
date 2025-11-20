package com.zeebra.domain.product.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.product.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class ProductQueryRepository {

    private final QProduct product = QProduct.product;
    private final QProductOption productOption = QProductOption.productOption;
    private final QSales sales = QSales.sales;
    private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
    private final QOptionName optionName = QOptionName.optionName;
    private final QFavoriteProduct favoriteProduct = QFavoriteProduct.favoriteProduct;

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    /**
     * 검색어 전처리
     * "갤럭시 버즈" -> "갤럭시:* & 버즈:*"
     */
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

    /**
     * 상품 검색 (Full-Text Search 포함)
     */
    public List<Product> searchProduct(String keyword,
                                       List<Long> categoryIds,
                                       List<Long> brandIds,
                                       Pageable pageable,
                                       ProductSort productSort) {
        // 키워드가 없으면 일반 쿼리 (QueryDSL 사용)
        if (keyword == null || keyword.isBlank()) {
            return searchWithoutKeyword(categoryIds, brandIds, pageable, productSort);
        }

        // 키워드가 있으면 FTS 쿼리 (Native Query 사용)
        return searchWithKeyword(keyword, categoryIds, brandIds, pageable, productSort);
    }

    /**
     * 키워드 없이 검색 (QueryDSL)
     */
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

    /**
     * 키워드로 검색 (Native Query)
     */
    private List<Product> searchWithKeyword(String keyword,
                                            List<Long> categoryIds,
                                            List<Long> brandIds,
                                            Pageable pageable,
                                            ProductSort productSort) {
        String processedQuery = preprocessKeyword(keyword);

        // 동적 SQL 생성
        StringBuilder sql = new StringBuilder("""
            SELECT p.*
            FROM product p
            INNER JOIN product_search_document psd ON p.id = psd.product_id
            WHERE psd.search_vector @@ to_tsquery('simple', :query)
            AND ts_rank(psd.search_vector, to_tsquery('simple', :query)) >= 0.3
            """);

        // 동적 WHERE 조건 추가
        if (categoryIds != null && !categoryIds.isEmpty()) {
            sql.append(" AND p.category_id IN :categoryIds");
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            sql.append(" AND p.brand_id IN :brandIds");
        }

        // 동적 ORDER BY
        sql.append(" ORDER BY ");
        sql.append(buildNativeOrderClause(productSort));

        // 쿼리 실행
        Query query = em.createNativeQuery(sql.toString(), Product.class);
        query.setParameter("query", processedQuery);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }

        return query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    /**
     * Native Query용 ORDER BY 절 생성
     */
    private String buildNativeOrderClause(ProductSort productSort) {
        StringBuilder order = new StringBuilder();

        // FTS rank를 최우선 정렬 기준으로
        order.append("ts_rank(psd.search_vector, to_tsquery('simple', :query)) DESC, ");

        if (productSort != null) {
            switch (productSort) {
                case REVIEW_COUNT_LEAST:
                    order.append("p.review_count ASC, ");
                    break;
                case REVIEW_COUNT_MOST:
                    order.append("p.review_count DESC, ");
                    break;
            }
        }

        order.append("p.id DESC");
        return order.toString();
    }

    /**
     * 필터링된 브랜드 조회 (Native Query)
     */
    public List<Brand> filteredBrand(String keyword,
                                     List<Long> categoryIds,
                                     List<Long> brandIds) {
        String processedQuery = preprocessKeyword(keyword);

        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT b.*
            FROM brand b
            INNER JOIN product_search_document psd ON b.id = psd.brand_id
            WHERE psd.search_vector @@ to_tsquery('simple', :query)
            """);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            sql.append(" AND psd.category_id IN :categoryIds");
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            sql.append(" AND b.id IN :brandIds");
        }

        sql.append(" ORDER BY b.id ASC");

        Query query = em.createNativeQuery(sql.toString(), Brand.class);
        query.setParameter("query", processedQuery);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }

        return query.getResultList();
    }

    /**
     * 필터링된 카테고리 조회 (Native Query)
     */
    public List<Category> filteredCategory(String keyword,
                                           List<Long> categoryIds,
                                           List<Long> brandIds) {
        String processedQuery = preprocessKeyword(keyword);

        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT c.*
            FROM category c
            INNER JOIN product_search_document psd ON c.id = psd.category_id
            WHERE psd.search_vector @@ to_tsquery('simple', :query)
            """);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            sql.append(" AND c.id IN :categoryIds");
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            sql.append(" AND psd.brand_id IN :brandIds");
        }

        sql.append(" ORDER BY c.id ASC");

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

    /**
     * 검색 결과 카운트
     */
    public long countFiltered(String keyword,
                              List<Long> categoryIds,
                              List<Long> brandIds) {
        // 키워드가 없으면 일반 카운트 (QueryDSL)
        if (keyword == null || keyword.isBlank()) {
            Long count = queryFactory
                    .select(product.count())
                    .from(product)
                    .where(
                            findByBrandId(brandIds),
                            findByCategoryId(categoryIds)
                    )
                    .fetchOne();
            return count != null ? count : 0L;
        }

        // 키워드가 있으면 FTS 카운트 (Native Query)
        String processedQuery = preprocessKeyword(keyword);

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM product p
            INNER JOIN product_search_document psd ON p.id = psd.product_id
            WHERE psd.search_vector @@ to_tsquery('simple', :query)
            AND ts_rank(psd.search_vector, to_tsquery('simple', :query)) >= 0.3
            """);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            sql.append(" AND p.category_id IN :categoryIds");
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            sql.append(" AND p.brand_id IN :brandIds");
        }

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("query", processedQuery);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    // ==================== QueryDSL 메서드들 (변경 없음) ====================

    /**
     * 여러 상품의 최저가 조회
     */
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

    /**
     * 단일 상품의 최저가 조회
     */
    public BigDecimal lowPriceOfProduct(Long productId) {
        return queryFactory
                .select(sales.price.min())
                .from(productOption)
                .join(sales).on(sales.productOptionId.eq(productOption.id))
                .where(productOption.productId.eq(productId))
                .fetchOne();
    }

    /**
     * 특정 색상의 최저가 조회
     */
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

    /**
     * 찜한 상품 목록 조회
     */
    public List<Product> getFavoriteProducts(Long memberId) {
        return queryFactory
                .selectFrom(product)
                .join(favoriteProduct).on(favoriteProduct.productId.eq(product.id))
                .where(favoriteProduct.memberId.eq(memberId))
                .orderBy(favoriteProduct.createdTime.desc())
                .fetch();
    }

    /**
     * 찜한 상품 개수 조회
     */
    public long countFavoriteProducts(Long memberId) {
        Long count = queryFactory
                .select(product.count())
                .from(product)
                .join(favoriteProduct).on(favoriteProduct.productId.eq(product.id))
                .where(favoriteProduct.memberId.eq(memberId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    // ==================== Private Helper 메서드들 ====================

    private BooleanExpression findByCategoryId(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return product.categoryId.in(categoryIds);
    }

    private BooleanExpression findByBrandId(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) return null;
        return product.brandId.in(brandIds);
    }

    /**
     * QueryDSL용 ORDER BY 생성 (키워드 없을 때)
     */
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