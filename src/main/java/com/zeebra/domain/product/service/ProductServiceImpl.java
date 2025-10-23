package com.zeebra.domain.product.service;

import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.product.repository.ProductQueryRepository;
import com.zeebra.domain.product.repository.ProductRepository;
import com.zeebra.domain.product.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final SalesRepository salesRepository;
    private final ProductOptionRepository productOptionRepository;

//    public ProductListResponse getProductList(GetProductListRequest request) {
//
//    }

    private ProductDetailResponse toProductDetailResponse(Product product, BigDecimal lowPriceOfProduct) {
        return new ProductDetailResponse(
                product.getId(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getModelNumber(),
                product.getThumbnail(),
                product.getImages(),
                lowPriceOfProduct,
                product.getReviewCount(),
                product.getFavoriteProductCount(),
                product.getCreatedTime());
    }

    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

        BigDecimal lowPriceOfProduct = productQueryRepository.lowPriceOfProduct(productId);

        return toProductDetailResponse(product, lowPriceOfProduct);
    }
}
