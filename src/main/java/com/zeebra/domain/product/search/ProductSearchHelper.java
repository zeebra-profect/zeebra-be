package com.zeebra.domain.product.search;

import com.zeebra.domain.brand.dto.BrandResponse;
import com.zeebra.domain.product.dto.CategoryResponseDto;
import com.zeebra.domain.product.dto.ProductSearchItem;
import com.zeebra.domain.product.entity.ProductDocument;
import com.zeebra.domain.product.entity.ProductSort;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregate;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductSearchHelper {

    public Highlight buildHighlight() {
        return Highlight.of(h -> h
                .preTags("<strong>")
                .postTags("</strong>")
                .requireFieldMatch(false)
                .fields("product_name", HighlightField.of(f -> f
                        .numberOfFragments(0)
                ))
                .fields("brand_name", HighlightField.of(f -> f
                        .numberOfFragments(0)
                ))
                .fields("category_name", HighlightField.of(f -> f
                        .numberOfFragments(0)
                ))
                .fields("description", HighlightField.of(f -> f
                        .fragmentSize(150)
                        .numberOfFragments(3)
                ))
        );
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

    public List<ProductSearchItem> convertToProductSearchItems(List<Hit<ProductDocument>> hits) {
        return hits.stream()
                .map(hit -> {
                    ProductDocument product = hit.source();
                    Map<String, List<String>> highlights = hit.highlight();

                    return new ProductSearchItem(
                            product.getProductId(),
                            getHighlight(highlights, "product_name", product.getProductName()),
                            product.getModelNumber(),
                            getHighlight(highlights, "description", product.getDescription()),
                            product.getBrandId(),
                            getHighlight(highlights, "brand_name", product.getBrandName()),
                            product.getCategoryId(),
                            getHighlight(highlights, "category_name", product.getCategoryName()),
                            product.getProductThumbnail(),
                            product.getImages(),
                            product.getMinPrice(),
                            product.getReviewCount(),
                            product.getFavoriteProductCount(),
                            product.getCreatedAt(),
                            hit.score() != null ? hit.score().floatValue() : 0.0f
                    );
                })
                .toList();
    }

    public String getHighlight(Map<String, List<String>> highlights, String field, String original) {
        if (highlights != null && highlights.containsKey(field) && !highlights.get(field).isEmpty()) {
            return highlights.get(field).get(0);
        }
        return original != null ? original : "";
    }

    public List<BrandResponse> extractBrandInfoFromComposite(SearchResponse<ProductDocument> searchResponse) {
        Map<String, Aggregate> aggregations = searchResponse.aggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            return Collections.emptyList();
        }

        Aggregate brandAgg = aggregations.get("brand_agg");
        if (brandAgg == null) {
            return Collections.emptyList();
        }

        CompositeAggregate compositeAgg = brandAgg.composite();

        return compositeAgg.buckets().array().stream()
                .map(bucket -> {
                    Map<String, FieldValue> key = bucket.key();

                    Long brandId = key.get("brand_id").longValue();
                    String brandName = key.get("brand_name").stringValue();

                    return new BrandResponse(brandId, brandName);
                })
                .collect(Collectors.toList());
    }

    public List<CategoryResponseDto> extractCategoryInfoFromComposite(SearchResponse<ProductDocument> searchResponse) {
        Map<String, Aggregate> aggregations = searchResponse.aggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            return Collections.emptyList();
        }

        Aggregate categoryAgg = aggregations.get("category_agg");
        if (categoryAgg == null) {
            return Collections.emptyList();
        }

        CompositeAggregate compositeAgg = categoryAgg.composite();

        return compositeAgg.buckets().array().stream()
                .map(bucket -> {
                    Map<String, FieldValue> key = bucket.key();

                    Long categoryId = key.get("category_id").longValue();
                    String categoryName = key.get("category_name").stringValue();

                    return new CategoryResponseDto(categoryId, categoryName);
                })
                .collect(Collectors.toList());
    }
}