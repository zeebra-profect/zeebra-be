package com.zeebra.domain.product.controller;

import java.time.LocalDate;
import java.util.List;

import com.zeebra.domain.product.dto.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return ApiResponse.success(salesService.createSales(memberId, request));
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
		@ParameterObject @PageableDefault(size = 20, sort = "createdTime", direction = Sort.Direction.DESC) Pageable pageable) {
		Long memberId = principal.getMemberId();

		SalesListResponse response = salesService.getSalesList(memberId, startDate, endDate, salesStatus, pageable);
		return ApiResponse.success(response);
	}

    @Operation(summary = "특정 회원 판매 상품 목록 조회 (채팅 연동)", description = "그룹 채팅에서 유저 프로필 클릭 시 사용")
    @GetMapping("/api/sales/member/{memberId}")
    public ApiResponse<List<UserSalesItem>> getSalesByMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ){
        List<UserSalesItem> userSales = salesService.findSalesByMemberId(memberId);
        return ApiResponse.success(userSales);
    }
}