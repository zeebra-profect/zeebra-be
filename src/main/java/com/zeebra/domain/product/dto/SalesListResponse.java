package com.zeebra.domain.product.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record SalesListResponse(
	List<SalesDetailResponse> salesList,
	int currentPage,
	int totalPages,
	long totalElements,
	boolean hasNext
) {
	public static SalesListResponse of(Page<SalesDetailResponse> salesPage) {
		return new SalesListResponse(salesPage.getContent(), salesPage.getNumber(), salesPage.getTotalPages(), salesPage.getTotalElements(), salesPage.hasNext());
	}
}