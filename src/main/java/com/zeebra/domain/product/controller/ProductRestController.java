package com.zeebra.domain.product.controller;

import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.domain.product.service.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ProductRestController {

    private final ProductService productService;

//    @GetMapping
//    public ProductListResponse getProductList(@RequestBody GetProductListRequest request) {
//        return productService.getProductList(request);
//    }

    @GetMapping("/api/products/{productId}")
    public ProductDetailResponse getProductDetail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

    @PostMapping("/api/favorite-products/{productId}")
    public FavoriteProductResponse addFavoriteProduct(Long memberId, @PathVariable Long productId) {
        return productService.addFavoriteProduct(memberId, productId);
    }
}
