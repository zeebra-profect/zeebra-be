package com.zeebra.domain.product.entity;

public enum ProductSort {
    REVIEW_COUNT_MOST("most_reviewed"),
    REVIEW_COUNT_LEAST("least_reviewed");

    private final String value;

    ProductSort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProductSort from(String value) {
        if (value == null || value.isBlank()) {
            return REVIEW_COUNT_MOST;
        }

        String normalized = value.trim().toLowerCase();

        for (ProductSort sort : values()) {
            if (sort.value.equals(normalized)) {
                return sort;
            }
        }

        return REVIEW_COUNT_MOST;
    }
}
