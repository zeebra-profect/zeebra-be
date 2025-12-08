package com.zeebra.domain.product.search;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.springframework.stereotype.Component;
import org.opensearch.client.json.JsonData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductSearchQueryBuilder {

    /**
     * 검색어 매칭 쿼리 생성
     * OpenSearch 3.x는 Query.of() 방식 사용
     */
    public List<Query> buildSearchQueries(String keyWord) {
        List<Query> queries = new ArrayList<>();

        // 모델 넘버 부분 일치 (최고 가중치)
        queries.add(Query.of(q -> q
                .prefix(p -> p
                        .field("model_number")
                        .value(keyWord.toLowerCase())
                        .boost(100.0f)
                )
        ));

        // 멀티 필드 검색 (오타 허용)
        queries.add(Query.of(q -> q
                .multiMatch(m -> m
                        .query(keyWord)
                        .fields("product_name^4", "brand_name^3", "category_name^2", "description^1")
                        .fuzziness("AUTO")
                        .prefixLength(2)
                        .type(TextQueryType.BestFields)
                )
        ));

        // NGram은 1-2글자일 때만 사용 (성능 최적화)
        if (keyWord.length() <= 2) {
            queries.add(Query.of(q -> q
                    .match(m -> m
                            .field("product_name.ngram")
                            .query(FieldValue.of(keyWord))
                            .boost(2.0f)
                    )
            ));
        }

        // 상품명 정확 일치
        queries.add(Query.of(q -> q
                .term(t -> t
                        .field("product_name.keyword")
                        .value(FieldValue.of(keyWord))
                        .boost(8.0f)
                )
        ));

        // 브랜드명 정확 일치
        queries.add(Query.of(q -> q
                .term(t -> t
                        .field("brand_name.keyword")
                        .value(FieldValue.of(keyWord))
                        .boost(6.0f)
                )
        ));

        // 카테고리명 정확 일치
        queries.add(Query.of(q -> q
                .term(t -> t
                        .field("category_name.keyword")
                        .value(FieldValue.of(keyWord))
                        .boost(4.0f)
                )
        ));

        return queries;
    }

    /**
     * 인기도 부스팅 함수 생성 (function_score 방식)
     */
    public List<FunctionScore> buildPopularityFunctions() {
        return List.of(
                // 리뷰 수 기반 부스팅
                FunctionScore.of(f -> f
                        .fieldValueFactor(fvf -> fvf
                                .field("review_count")
                                .factor(0.1F)
                                .modifier(FieldValueFactorModifier.Log1p)
                                .missing(0.0)
                        )
                        .weight(1.5F)
                ),

                // 관심상품 수 기반 부스팅
                FunctionScore.of(f -> f
                        .fieldValueFactor(fvf -> fvf
                                .field("favorite_product_count")
                                .factor(0.1F)
                                .modifier(FieldValueFactorModifier.Log1p)
                                .missing(0.0)
                        )
                        .weight(1.2F)
                )
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
            filters.add(Query.of(q -> q
                    .terms(t -> t
                            .field("category_id")
                            .terms(termsQuery -> termsQuery.value(
                                    categoryIds.stream()
                                            .map(FieldValue::of)
                                            .collect(Collectors.toList())
                            ))
                    )
            ));
        }

        // 브랜드 필터
        if (brandIds != null && !brandIds.isEmpty()) {
            filters.add(Query.of(q -> q
                    .terms(t -> t
                            .field("brand_id")
                            .terms(termsQuery -> termsQuery.value(
                                    brandIds.stream()
                                            .map(FieldValue::of)
                                            .collect(Collectors.toList())
                            ))
                    )
            ));
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
        return Query.of(q -> q
                .range(r -> {
                    r.field("min_price");

                    if (minPrice != null) {
                        r.gte(JsonData.of(minPrice.doubleValue()));
                    }

                    if (maxPrice != null) {
                        r.lte(JsonData.of(maxPrice.doubleValue()));
                    }

                    return r;
                })
        );
    }

    /**
     * Function Score Query 생성 (메인 검색 메서드)
     */
    public Query buildFunctionScoreQuery(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        // 메인 검색 쿼리 (인기도 제외)
        Query mainQuery = Query.of(q -> q
                .bool(b -> {
                    // Should: 검색어 매칭만
                    buildSearchQueries(keyWord).forEach(b::should);

                    // Filter: 필터링
                    buildFilterQueries(categoryIds, brandIds, minPrice, maxPrice)
                            .forEach(b::filter);

                    return b.minimumShouldMatch("2");
                })
        );

        // Function Score로 감싸서 인기도 반영
        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(mainQuery)
                        .functions(buildPopularityFunctions())
                        .scoreMode(FunctionScoreMode.Sum)
                        .boostMode(FunctionBoostMode.Multiply)
                )
        );
    }
}