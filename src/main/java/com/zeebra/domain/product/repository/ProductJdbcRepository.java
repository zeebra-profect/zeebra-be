package com.zeebra.domain.product.repository;

import com.zeebra.domain.product.dto.ProductSearchResult;
import com.zeebra.domain.product.entity.ProductSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

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

    public List<ProductSearchResult> searchWithKeyword(
            String keyword,
            List<Long> categoryIds,
            List<Long> brandIds,
            Pageable pageable,
            ProductSort productSort) {

        String processedQuery = preprocessKeyword(keyword);
        String secondaryOrder = buildSecondaryOrder(productSort);

        StringBuilder sql = new StringBuilder("""
        SELECT
            p.id,
            p.brand_id,
            p.category_id,
            p.name,
            p.description,
            p.model_number,
            p.thumbnail,
            p.images,
            p.review_count,
            p.favorite_product_count,
            p.created_time
        FROM product_search_document psd
        JOIN product p ON p.id = psd.product_id
        WHERE psd.search_vector @@ to_tsquery('simple', :query)
        """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", processedQuery)
                .addValue("offset", pageable.getOffset())
                .addValue("limit", pageable.getPageSize());

        // 동적 조건 추가
        if (categoryIds != null && !categoryIds.isEmpty()) {
            sql.append("AND psd.category_id = ANY(:categoryIds) ");
            params.addValue("categoryIds", categoryIds.toArray(Long[]::new));
        }

        if (brandIds != null && !brandIds.isEmpty()) {
            sql.append("AND psd.brand_id = ANY(:brandIds) ");
            params.addValue("brandIds", brandIds.toArray(Long[]::new));
        }

        sql.append("ORDER BY ").append(secondaryOrder).append(" p.id DESC ");
        sql.append("OFFSET :offset LIMIT :limit");

        return namedJdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new ProductSearchResult(
                        rs.getLong("id"),
                        rs.getLong("brand_id"),
                        rs.getLong("category_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("model_number"),
                        rs.getString("thumbnail"),
                        convertToList(rs.getArray("images")),
                        rs.getInt("review_count"),
                        rs.getInt("favorite_product_count"),
                        getLocalDateTime(rs, "created_time")
                )
        );
    }

    private List<String> convertToList(Array sqlArray) throws SQLException {
        if (sqlArray == null) return List.of();
        String[] array = (String[]) sqlArray.getArray();
        return array != null ? Arrays.asList(array) : List.of();
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
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

}
