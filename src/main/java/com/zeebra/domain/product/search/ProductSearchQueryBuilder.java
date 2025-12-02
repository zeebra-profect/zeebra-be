package com.zeebra.domain.product.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductSearchQueryBuilder {

    /**
     * 검색어 매칭 쿼리 생성
     */
    public List<Query> buildSearchQueries(String keyWord) {
        return List.of(
                // 모델 넘버 부분 일치 (최고 가중치)
                PrefixQuery.of(p -> p
                        .field("model_number")
                        .value(keyWord.toLowerCase())
                        .boost(100.0f)
                )._toQuery(),

                // 멀티 필드 검색 (오타 허용)
                MultiMatchQuery.of(m -> m
                        .query(keyWord)
                        .fields("product_name^4", "brand_name^3", "category_name^2", "description^1")
                        .fuzziness("AUTO")
                        .prefixLength(2)
                        .type(TextQueryType.BestFields)
                )._toQuery(),

                // NGram 부분 일치
                MatchQuery.of(m -> m
                        .field("product_name.ngram")
                        .query(keyWord)
                        .boost(2.0f)
                )._toQuery(),

                // 상품명 정확 일치
                TermQuery.of(t -> t
                        .field("product_name.keyword")
                        .value(keyWord)
                        .boost(8.0f)
                )._toQuery(),

                // 브랜드명 정확 일치
                TermQuery.of(t -> t
                        .field("brand_name.keyword")
                        .value(keyWord)
                        .boost(6.0f)
                )._toQuery(),

                // 카테고리명 정확 일치
                TermQuery.of(t -> t
                        .field("category_name.keyword")
                        .value(keyWord)
                        .boost(4.0f)
                )._toQuery()
        );
    }

    /**
     * 인기도 부스팅 쿼리 생성
     */
    public List<Query> buildPopularityBoostQueries() {
        return List.of(
                // 리뷰 100개 이상
                NumberRangeQuery.of(r -> r
                        .field("review_count")
                        .gte(100.0)
                )._toRangeQuery()._toQuery(),

                // 관심상품 100개 이상
                NumberRangeQuery.of(r -> r
                        .field("favorite_product_count")
                        .gte(100.0)
                )._toRangeQuery()._toQuery()
        );
    }

    /**
     * 필터 쿼리 생성
     */
    public List<Query> buildFilterQueries(
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        List<Query> filters = new ArrayList<>();

        // 카테고리 필터
        if (categoryIds != null && !categoryIds.isEmpty()) {
            filters.add(TermsQuery.of(t -> t
                    .field("category_id")
                    .terms(termsQuery -> termsQuery.value(
                            categoryIds.stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())
                    ))
            )._toQuery());
        }

        // 브랜드 필터
        if (brandIds != null && !brandIds.isEmpty()) {
            filters.add(TermsQuery.of(t -> t
                    .field("brand_id")
                    .terms(termsQuery -> termsQuery.value(
                            brandIds.stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())
                    ))
            )._toQuery());
        }

        // 가격 범위 필터
        if (minPrice != null || maxPrice != null) {
            filters.add(buildPriceRangeQuery(minPrice, maxPrice));
        }

        return filters;
    }

    /**
     * 가격 범위 쿼리 생성
     */
    private Query buildPriceRangeQuery(BigDecimal minPrice, BigDecimal maxPrice) {
        return NumberRangeQuery.of(r -> {
            r.field("min_price");

            if (minPrice != null) {
                r.gte(minPrice.doubleValue());
            }

            if (maxPrice != null) {
                r.lte(maxPrice.doubleValue());
            }

            return r;
        })._toRangeQuery()._toQuery();
    }

    /**
     * Bool Query 생성 (전체 조합)
     */
    public Query buildBoolQuery(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return BoolQuery.of(b -> {
            // Should: 검색어 매칭
            buildSearchQueries(keyWord).forEach(b::should);

            // Should: 인기도 부스팅
            buildPopularityBoostQueries().forEach(b::should);

            // Filter: 필터링
            buildFilterQueries(categoryIds, brandIds, minPrice, maxPrice).forEach(b::filter);

            return b.minimumShouldMatch("1");
        })._toQuery();
    }
}
