package com.zeebra.domain.order.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.dto.ReadOrderListResponse;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.service.OrderService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Order API", description = "주문 관련 API")
public class OrderController {

	private final OrderService orderService;

	@Operation(summary = "주문 생성 API", description = "장바구니 혹은 즉시 구매의 주문서를 만듭니다. 결제 직전의 상태입니다.")
	@PostMapping()
	public ApiResponse<CreateOrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request, @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {

		Long memberId = principal.getMemberId();

		return ApiResponse.success(orderService.createOrder(memberId, request));
	}

	@Operation(summary = "주문 목록 조회 API", description = "주문한 내역을 조건별로 조회합니다.")
	@GetMapping()
	public ApiResponse<ReadOrderListResponse> getOrders(
		@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
		@Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = false)
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = false)
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
		@Parameter(description = "주문 상태", required = false)
		@RequestParam(required = false) OrderStatus orderStatus,
		Pageable pageable
	) {
		Long memberId = principal.getMemberId();

		ReadOrderListResponse response = orderService.getOrderList(memberId, startDate, endDate, orderStatus, pageable);

		return ApiResponse.success(response);
	}
}