package com.zeebra.domain.order.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record ReadOrderListResponse(
	List<OrderResponse> orders,
	int currentPage,
	int totalPages,
	long totalElements,
	boolean hasNext
) {
	public static ReadOrderListResponse of(Page<OrderResponse> orderPage) {
		return new ReadOrderListResponse(
			orderPage.getContent(),
			orderPage.getNumber(),
			orderPage.getTotalPages(),
			orderPage.getTotalElements(),
			orderPage.hasNext()
		);
	}
}