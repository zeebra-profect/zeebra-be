package com.zeebra.domain.product.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsAggregate;
import co.elastic.clients.json.JsonData;
import com.zeebra.domain.brand.dto.BrandResponse;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.product.dto.CategoryResponseDto;
import com.zeebra.domain.product.dto.ProductSearchItem;
import com.zeebra.domain.product.entity.ProductDocument;
import com.zeebra.domain.product.entity.ProductSort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductSearchHelper {

    public HighlightQuery buildHighlightQuery() {
        Highlight highlight = new Highlight(
                HighlightParameters.builder()
                        .withPreTags("<strong>")
                        .withPostTags("</strong>")
                        .withRequireFieldMatch(false)
                        .build(),
                List.of(
                        new HighlightField("product_name",
                                HighlightFieldParameters.builder()
                                        .withNumberOfFragments(0)
                                        .build()),
                        new HighlightField("brand_name",
                                HighlightFieldParameters.builder()
                                        .withNumberOfFragments(0)
                                        .build()),
                        new HighlightField("category_name",
                                HighlightFieldParameters.builder()
                                        .withNumberOfFragments(0)
                                        .build()),
                        new HighlightField("description",
                                HighlightFieldParameters.builder()
                                        .withFragmentSize(150)
                                        .withNumberOfFragments(3)
                                        .build())
                )
        );

        return new HighlightQuery(highlight, ProductDocument.class);
    }

    public Sort buildSort(ProductSort productSort) {
        return switch (productSort) {
            case PRICE_LOW -> Sort.by(Sort.Order.asc("min_price"));
            case PRICE_HIGH -> Sort.by(Sort.Order.desc("min_price"));
            case REVIEW_COUNT_MOST -> Sort.by(Sort.Order.desc("review_count"));
            case REVIEW_COUNT_LEAST -> Sort.by(Sort.Order.asc("review_count"));
            case FAVORITE_COUNT_MOST -> Sort.by(Sort.Order.desc("favorite_product_count"));
            case FAVORITE_COUNT_LEAST -> Sort.by(Sort.Order.asc("favorite_product_count"));
            case NEWEST -> Sort.by(Sort.Order.desc("created_at"));
            case OLDEST -> Sort.by(Sort.Order.asc("created_at"));
            case RELEVANCE -> Sort.by(Sort.Order.desc("_score"));
        };
    }

    public List<ProductSearchItem> convertToProductSearchItems(
            List<SearchHit<ProductDocument>> hits
    ) {
        return hits.stream()
                .map(hit -> {
                    ProductDocument product = hit.getContent();
                    Map<String, List<String>> highlights = hit.getHighlightFields();

                    return new ProductSearchItem(
                            product.getProductId(),
                            // camelCase로 변경! ⬇️⬇️⬇️
                            getHighlight(highlights, "productName", product.getProductName()),
                            product.getModelNumber(),
                            getHighlight(highlights, "description", product.getDescription()),
                            product.getBrandId(),
                            getHighlight(highlights, "brandName", product.getBrandName()),
                            product.getCategoryId(),
                            getHighlight(highlights, "categoryName", product.getCategoryName()),
                            product.getProductThumbnail(),
                            product.getImages(),
                            product.getMinPrice(),
                            product.getReviewCount(),
                            product.getFavoriteProductCount(),
                            product.getCreatedAt(),
                            hit.getScore()
                    );
                })
                .toList();
    }

    public String getHighlight(
            Map<String, List<String>> highlights,
            String field,
            String original
    ) {
        if (highlights != null && highlights.containsKey(field) && !highlights.get(field).isEmpty()) {
            String highlighted = highlights.get(field).get(0);
            log.info("✅ Using highlight for {}: {}", field, highlighted);
            return highlighted;
        }
        log.info("❌ Using original for {}: {}", field, original);
        return original != null ? original : "";
    }

    public List<BrandResponse> extractBrandInfoFromComposite(SearchHits<ProductDocument> searchHits) {
        ElasticsearchAggregations aggregations =
                (ElasticsearchAggregations) searchHits.getAggregations();

        if (aggregations == null) {
            return Collections.emptyList();
        }

        return aggregations.aggregations().stream()
                .filter(agg -> "brand_agg".equals(agg.aggregation().getName()))
                .findFirst()
                .map(agg -> {
                    CompositeAggregate compositeAgg = agg.aggregation().getAggregate().composite();

                    return compositeAgg.buckets().array().stream()
                            .map(bucket -> {
                                Map<String, FieldValue> key = bucket.key();

                                // FieldValue에서 직접 추출
                                Long brandId = key.get("brand_id").longValue();
                                String brandName = key.get("brand_name").stringValue();

                                return new BrandResponse(brandId, brandName);
                            })
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());
    }

    public List<CategoryResponseDto> extractCategoryInfoFromComposite(SearchHits<ProductDocument> searchHits) {
        ElasticsearchAggregations aggregations =
                (ElasticsearchAggregations) searchHits.getAggregations();

        if (aggregations == null) {
            return Collections.emptyList();
        }

        return aggregations.aggregations().stream()
                .filter(agg -> "category_agg".equals(agg.aggregation().getName()))
                .findFirst()
                .map(agg -> {
                    CompositeAggregate compositeAgg = agg.aggregation().getAggregate().composite();

                    return compositeAgg.buckets().array().stream()
                            .map(bucket -> {
                                Map<String, FieldValue> key = bucket.key();

                                Long categoryId = key.get("category_id").longValue();
                                String categoryName = key.get("category_name").stringValue();

                                return new CategoryResponseDto(categoryId, categoryName);
                            })
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());
    }
}