package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.GetProductListRequest;
import com.zeebra.domain.product.dto.ProductListResponse;
import com.zeebra.domain.product.repository.ProductQueryRepository;
import com.zeebra.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;

//    public ProductListResponse getProductList(GetProductListRequest request) {
//
//    }

}
