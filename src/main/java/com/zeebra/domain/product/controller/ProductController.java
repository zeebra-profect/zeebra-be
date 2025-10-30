package com.zeebra.domain.product.controller;

import com.zeebra.domain.product.dto.*;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Tag(name = "Product API", description = "상품 관련 API")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 조회")
    @GetMapping("/api/products")
    public ApiResponse<SearchProductResponse> getProductList(@RequestParam(required = false) String keyWord,
                                                             @RequestParam(required = false) List<Long> categoryIds,
                                                             @RequestParam(required = false) List<Long> brandIds,
                                                             @RequestParam(required = false) String productSort,
                                                             Pageable pageable) {
        return productService.searchProduct(keyWord, categoryIds, brandIds, pageable, productSort);
    }

    @GetMapping("/api/products/{productId}")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

    @PostMapping("/api/favorite-products/{productId}")
    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                                                   @PathVariable Long productId) {
        Long memberId = principal.getMemberId();
        return productService.addFavoriteProduct(memberId, productId);
    }

    @DeleteMapping("/api/favorite-products/{productId}")
    public ApiResponse<Void> deleteFavoriteProduct(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                                   @PathVariable Long productId) {
        Long memberId = principal.getMemberId();
        return productService.deleteFavoriteProduct(memberId, productId);
    }

    @PostMapping("/api/products")
    public ApiResponse<ProductResponse> createProduct(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
            , @RequestBody ProductRequest request) {
        Long memberId = principal.getMemberId();
        return productService.createProduct(memberId, request);
    }
}
