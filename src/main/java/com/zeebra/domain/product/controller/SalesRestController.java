package com.zeebra.domain.product.controller;

import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.service.SalesService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SalesRestController {

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
}
