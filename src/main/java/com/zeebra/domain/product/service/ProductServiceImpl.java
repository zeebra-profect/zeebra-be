package com.zeebra.domain.product.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.entity.FavoriteProduct;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.repository.*;
import com.zeebra.global.ApiResponse;
import jakarta.transaction.Transactional;
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
    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId) {
        try {
            Product product = productRepository.findById(productId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

            BigDecimal lowPriceOfProduct = productQueryRepository.lowPriceOfProduct(productId);

            return ApiResponse.success(toProductDetailResponse(product, lowPriceOfProduct));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "상품 상세 조회 과정에 오류가 발생했습니다.");
        }
    }

    @Transactional
    @Override
    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(Long memberId, Long productId) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다"));

            Product product = productRepository.findById(productId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

            FavoriteProduct favoriteProduct = favoriteProductRepository.save(new FavoriteProduct(member.getId(), product.getId()));

            return ApiResponse.success(toFavoriteProductResponse(favoriteProduct));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "관심 상품 추가 과정에 오류가 발생했습니다.");
        }
    }

    @Transactional
    @Override
    public ApiResponse<Void> deleteFavoriteProduct(Long memberId, Long productId) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다"));

            Product product = productRepository.findById(productId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

            FavoriteProduct favoriteProduct = favoriteProductRepository.findByMemberIdAndProductId(member.getId(), product.getId()).orElseThrow(
                    () -> new NoSuchElementException("해당하는 관심 상품이 없습니다."));

            favoriteProductRepository.delete(favoriteProduct);
            return ApiResponse.successMessage("상품 삭제에 성공했습니다.");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "상품 삭제 과정에서 오류가 발생했습니다.");
        }
    }
}
