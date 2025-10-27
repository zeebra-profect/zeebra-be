package com.zeebra.domain.product.controller;

import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.domain.product.service.ProductServiceImpl;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class ProductRestController {

    private final ProductService productService;

//    @GetMapping
//    public ProductListResponse getProductList(@RequestBody GetProductListRequest request) {
//        return productService.getProductList(request);
//    }

    @GetMapping("/api/products/{productId}")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

    @PostMapping("/api/favorite-products/{productId}")
    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable Long productId) {
        Long memberId = principal.getMemberId();
        return productService.addFavoriteProduct(memberId, productId);
    }

    @DeleteMapping("/api/favorite-products/{productId}")
    public ApiResponse<Void> deleteFavoriteProduct(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, Long productId) {
        Long memberId = principal.getMemberId();
        return productService.deleteFavoriteProduct(memberId, productId);
    }
}
