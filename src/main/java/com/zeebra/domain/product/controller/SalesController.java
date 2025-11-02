package com.zeebra.domain.product.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.product.dto.SalesDetailResponse;
import com.zeebra.domain.product.dto.SalesListResponse;
import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sales API", description = "판매 관련 API")
public class SalesController {

    private final SalesService salesService;

    @PostMapping("/api/sales")
    public ApiResponse<SalesResponse> createSales(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @RequestBody SalesRequest request) {
        Long memberId = principal.getMemberId();
        return salesService.createSales(memberId, request);
    }

    @DeleteMapping("/api/salses/{salesId}")
    public ApiResponse<Void> deleteSales(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable Long salesId) {
        Long memberId = principal.getMemberId();
        return salesService.deleteSales(memberId, salesId);
    }

	@Operation(summary = "판매 내역 상세 조회 API", description = "salesId를 통해서 판매 상세를 조회합니다.")
	@GetMapping("/api/sales/{salesId}")
	public ApiResponse<SalesDetailResponse> getSalesDetail(@PathVariable Long salesId, @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
		Long memberId = principal.getMemberId();
		return ApiResponse.success(salesService.getSalesDetail(memberId, salesId));
	}

	@Operation(summary = "판매 내역 목록 조회 API", description = "현재 사용자의 모든 판매 목록을 조회합니다.")
	@GetMapping("/api/sales")
	public ApiResponse<SalesListResponse> getSalesList(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
		@Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = false)
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = false)
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
		@Parameter(description = "판매 상태", required = false)
		@RequestParam(required = false) SalesStatus salesStatus,
		Pageable pageable) {
		Long memberId = principal.getMemberId();

		SalesListResponse response = salesService.getSalesList(memberId, startDate, endDate, salesStatus, pageable);
		return ApiResponse.success(response);
	}
}