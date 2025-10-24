package com.zeebra.domain.product.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.entity.FavoriteProduct;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.repository.*;
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
    private final MemberRepository memberRepository;
    private final FavoriteProductRepository favoriteProductRepository;

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

    private FavoriteProductResponse toFavoriteProductResponse(FavoriteProduct favoriteProduct) {
        return new FavoriteProductResponse(
                favoriteProduct.getProductId(),
                favoriteProduct.getProductId(),
                favoriteProduct.getMemberId(),
                favoriteProduct.getCreatedTime());
    }

    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

        BigDecimal lowPriceOfProduct = productQueryRepository.lowPriceOfProduct(productId);

        return toProductDetailResponse(product, lowPriceOfProduct);
    }

    @Override
    public FavoriteProductResponse addFavoriteProduct(Long memberId, Long productId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다"));

        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

        FavoriteProduct favoriteProduct = favoriteProductRepository.save(new FavoriteProduct(member.getId(), product.getId()));

        return toFavoriteProductResponse(favoriteProduct);
    }
}
