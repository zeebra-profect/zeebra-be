package com.zeebra.domain.product.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode; // 추가
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode; // 추가
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductSearchQueryBuilder {

    /**
     * 검색어 매칭 쿼리 생성
     *
     * 🔄 변경: NGram 쿼리를 조건부로 추가 (성능 최적화)
     */
    public List<Query> buildSearchQueries(String keyWord) {
        List<Query> queries = new ArrayList<>();

        // 모델 넘버 부분 일치 (최고 가중치)
        queries.add(PrefixQuery.of(p -> p
                .field("model_number")
                .value(keyWord.toLowerCase())
                .boost(100.0f)
        )._toQuery());

        // 멀티 필드 검색 (오타 허용)
        queries.add(MultiMatchQuery.of(m -> m
                .query(keyWord)
                .fields("product_name^4", "brand_name^3", "category_name^2", "description^1")
                .fuzziness("AUTO")
                .prefixLength(2)
                .type(TextQueryType.BestFields)
        )._toQuery());

        // 🔄 변경: NGram은 1-2글자일 때만 사용 (성능 최적화)
        if (keyWord.length() <= 2) {
            queries.add(MatchQuery.of(m -> m
                    .field("product_name.ngram")
                    .query(keyWord)
                    .boost(2.0f)
            )._toQuery());
        }

        // 상품명 정확 일치
        queries.add(TermQuery.of(t -> t
                .field("product_name.keyword")
                .value(keyWord)
                .boost(8.0f)
        )._toQuery());

        // 브랜드명 정확 일치
        queries.add(TermQuery.of(t -> t
                .field("brand_name.keyword")
                .value(keyWord)
                .boost(6.0f)
        )._toQuery());

        // 카테고리명 정확 일치
        queries.add(TermQuery.of(t -> t
                .field("category_name.keyword")
                .value(keyWord)
                .boost(4.0f)
        )._toQuery());

        return queries;
    }

    /**
     * ❌ 삭제: 인기도 부스팅을 range 쿼리로 처리하던 기존 방식
     * 이유: 800만개 문서를 모두 스캔하여 718ms 병목 발생
     *
     * 이 메서드는 더 이상 사용하지 않고 buildPopularityFunctions()로 대체
     */
    @Deprecated
    public List<Query> buildPopularityBoostQueries() {
        // 더 이상 사용하지 않음
        // buildPopularityFunctions()를 대신 사용하세요
        throw new UnsupportedOperationException(
                "Use buildPopularityFunctions() instead for better performance"
        );
    }

    /**
     * ✅ 신규: 인기도 부스팅 함수 생성 (function_score 방식)
     *
     * 장점:
     * 1. 성능: 매칭된 문서에만 적용되어 훨씬 빠름 (718ms → ~50ms 예상)
     * 2. 정확도: 연속적인 점수 부여로 더 정교한 랭킹
     *    - 기존: 리뷰 100개 = 1점, 1000개 = 1점 (동일)
     *    - 개선: 리뷰 100개 = 0.3점, 1000개 = 0.45점 (차등)
     */
    public List<FunctionScore> buildPopularityFunctions() {
        return List.of(
                // 리뷰 수 기반 부스팅
                FunctionScore.of(f -> f
                        .fieldValueFactor(fvf -> fvf
                                .field("review_count")
                                .factor(0.1)  // 조정 가능한 계수
                                .modifier(FieldValueFactorModifier.Log1p)  // log(1 + value)
                                .missing(0.0)  // 값이 없으면 0
                        )
                        .weight(1.5)  // 리뷰가 중요하므로 높은 가중치
                ),

                // 관심상품 수 기반 부스팅
                FunctionScore.of(f -> f
                        .fieldValueFactor(fvf -> fvf
                                .field("favorite_product_count")
                                .factor(0.1)
                                .modifier(FieldValueFactorModifier.Log1p)
                                .missing(0.0)
                        )
                        .weight(1.2)  // 리뷰보다는 약간 낮은 가중치
                )
        );
    }

    /**
     * 필터 쿼리 생성 (변경 없음)
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
     * 가격 범위 쿼리 생성 (변경 없음)
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
     * ❌ 기존 방식 (더 이상 사용하지 않음)
     * Bool Query 생성 - should 절에 range 쿼리 포함
     */
    @Deprecated
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

            // ❌ 문제: 인기도 range 쿼리가 should에 포함되어 병목 발생
            // buildPopularityBoostQueries().forEach(b::should);

            // Filter: 필터링
            buildFilterQueries(categoryIds, brandIds, minPrice, maxPrice).forEach(b::filter);

            return b.minimumShouldMatch("1");
        })._toQuery();
    }

    /**
     * ✅ 신규: Function Score Query 생성 (권장)
     *
     * 구조 변경:
     * 기존: bool(should[검색쿼리, range쿼리])
     * 개선: function_score(query=검색쿼리, functions=인기도함수)
     *
     * 성능 개선:
     * - 1,434ms → 200~400ms 예상 (약 70% 개선)
     * - range 쿼리 병목 718ms 제거
     */
    public Query buildFunctionScoreQuery(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        // 메인 검색 쿼리 (인기도 제외)
        Query mainQuery = BoolQuery.of(b -> {
            // Should: 검색어 매칭만
            buildSearchQueries(keyWord).forEach(b::should);

            // Filter: 필터링
            buildFilterQueries(categoryIds, brandIds, minPrice, maxPrice).forEach(b::filter);

            return b.minimumShouldMatch("2");
        })._toQuery();

        // Function Score로 감싸서 인기도 반영
        return FunctionScoreQuery.of(fs -> fs
                .query(mainQuery)  // 메인 검색 쿼리
                .functions(buildPopularityFunctions())  // 인기도 함수들
                .scoreMode(FunctionScoreMode.Sum)  // 함수들의 점수를 합산
                .boostMode(FunctionBoostMode.Multiply)  // 검색 점수와 곱하기
        )._toQuery();
    }
}