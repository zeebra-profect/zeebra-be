package com.zeebra.domain.product.entity;

public enum SalesStatus {
    ON_SALE,
    PENDING,
    CONFIRMED;

	public boolean isOnSale() {
		return this == ON_SALE;
	}
}