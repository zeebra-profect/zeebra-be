package com.zeebra.domain.product.entity;

public enum ProductSort {
    RELEVANCE("relevance"),                    // 관련도순 (기본)
    PRICE_LOW("price_low"),                    // 가격 낮은순
    PRICE_HIGH("price_high"),                  // 가격 높은순
    REVIEW_COUNT_MOST("most_reviewed"),        // 리뷰 많은순
    REVIEW_COUNT_LEAST("least_reviewed"),      // 리뷰 적은순
    FAVORITE_COUNT_MOST("most_favorite"),      // 좋아요 많은순
    FAVORITE_COUNT_LEAST("least_favorite"),    // 좋아요 적은순
    NEWEST("newest"),                          // 최신순
    OLDEST("oldest");                          // 오래된순

    private final String value;

    ProductSort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProductSort from(String value) {
        if (value == null || value.isBlank()) {
            return RELEVANCE;  // 기본값: 관련도순
        }

        String normalized = value.trim().toLowerCase();

        for (ProductSort sort : values()) {
            if (sort.value.equals(normalized)) {
                return sort;
            }
        }

        return RELEVANCE;  // 잘못된 값이면 기본값
    }
}