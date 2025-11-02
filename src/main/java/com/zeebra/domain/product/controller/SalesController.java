package com.zeebra.domain.product.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.product.dto.SalesDetailResponse;
import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
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
	@GetMapping("api/sales/{salesId}")
	public ApiResponse<SalesDetailResponse> getSalesDetail(@PathVariable Long salesId, @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
		Long memberId = principal.getMemberId();
		return ApiResponse.success(salesService.getSalesDetail(memberId, salesId));
	}
}