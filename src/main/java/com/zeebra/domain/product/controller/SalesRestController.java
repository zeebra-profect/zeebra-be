package com.zeebra.domain.product.controller;

import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.service.SalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SalesRestController {

    private final SalesService salesService;

    @PostMapping("/api/sales")
    public SalesResponse createSales(Long memberId, @RequestBody SalesRequest request) {
        return salesService.createSales(memberId, request);
    }
}
