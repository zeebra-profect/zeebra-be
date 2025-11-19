package com.zeebra.domain.product.service;

import com.zeebra.domain.brand.dto.BrandResponse;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.category.dto.CategorySearchResponse;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.member.service.MemberService;
import com.zeebra.domain.product.dto.*;
import com.zeebra.domain.product.entity.*;
import com.zeebra.domain.product.repository.*;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.web.KeywordSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final MemberRepository memberRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final ProductOptionQueryRepository productOptionQueryRepository;
    private final OptionNameRepository optionNameRepository;
    private final MemberService memberService;

    @Override
    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId, Long colorOptionNameId) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

        List<OptionName> colorOptionNames = productOptionQueryRepository.findAllOptionNames(product.getId());

        List<ColorOptionResponse> colorOptionResponses = colorOptionNames.stream()
                .map(colorOptionName -> new ColorOptionResponse(colorOptionName.getId(), colorOptionName.getValue()))
                .toList();

        if (colorOptionNameId == null) {

            BigDecimal lowPriceOfColor = productQueryRepository.lowPriceOfColor(product.getId(), colorOptionResponses.getFirst().colorOptionNameId());

            return ApiResponse.success(ProductDetailResponse.from(product, lowPriceOfColor, colorOptionResponses, colorOptionResponses.getFirst().colorValue()));
        } else {

            BigDecimal lowPriceOfColor = productQueryRepository.lowPriceOfColor(product.getId(), colorOptionNameId);

            OptionName optionName = optionNameRepository.findById(colorOptionNameId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 옵션이름이 없습니다."));

            return ApiResponse.success(ProductDetailResponse.from(product, lowPriceOfColor, colorOptionResponses, optionName.getValue()));
        }
    }

    @Override
    public ApiResponse<SizeOptionResponseList> getProductOptionSize(Long productId, Long colorOptionNameId) {
        OptionName optionName = optionNameRepository.findById(colorOptionNameId).orElseThrow(
                () -> new NoSuchElementException("해당하는 옵션값이 없습니다."));
        optionName.validateOptionNameIsColor();
        List<SizeOptionResponse> sizeOptionResponses = productOptionQueryRepository.findByColorOptionName(colorOptionNameId, productId);
        return ApiResponse.success(new SizeOptionResponseList(sizeOptionResponses));
    }

    @Transactional
    @Override
    public ApiResponse<FavoriteProductResponse> addFavoriteProduct(Long memberId, Long productId) {
        Member member = memberService.findByMemberId(memberId);

        Product product = findByProductId(productId);

        if (favoriteProductRepository.findByMemberIdAndProductId(memberId, productId).isPresent()) {
            throw new IllegalStateException("이미 관심 상품에 등록된 상품입니다.");
        } else {

            FavoriteProduct favoriteProduct = favoriteProductRepository.save(new FavoriteProduct(member.getId(), product.getId()));

            product.increaseFavoriteProductCount();

            productRepository.save(product);

            return ApiResponse.success(FavoriteProductResponse.toFavoriteProductResponse(favoriteProduct));
        }
    }

    public Product findByProductId(Long productId) {
        return productRepository.findById(productId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));
    }

    @Transactional
    @Override
    public ApiResponse<Void> deleteFavoriteProduct(Long memberId, Long productId) {
        Member member = memberService.findByMemberId(memberId);

        Product product = findByProductId(productId);

        FavoriteProduct favoriteProduct = favoriteProductRepository.findByMemberIdAndProductId(member.getId(), product.getId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 관심 상품이 없습니다."));

        favoriteProductRepository.delete(favoriteProduct);

        product.decreaseFavoriteProductCount();

        productRepository.save(product);

        return ApiResponse.successMessage("관심 상품 삭제에 성공했습니다.");
    }

    @Override
    @Transactional
    public ApiResponse<ProductResponse> createProduct(Long memberId, ProductRequest request) {
        Member member = memberService.findByMemberId(memberId);
        member.validateAdminPermission();
        Product product = productRepository.save(Product.from(request));
        return ApiResponse.success(ProductResponse.from(product));
    }

    @Override
    public ApiResponse<SearchProductResponse> searchProduct(String keyWord,
                                                            List<Long> categoryIds,
                                                            List<Long> brandIds,
                                                            Pageable pageable,
                                                            String productSort) {

        String cleanKeyword = KeywordSanitizer.sanitize(keyWord);

        ProductSort parseProductSort = ProductSort.from(productSort);

        List<Product> products = productQueryRepository.searchProduct(cleanKeyword, categoryIds, brandIds, pageable, parseProductSort);

        List<Brand> brands = productQueryRepository.filteredBrand(cleanKeyword, categoryIds, brandIds);

        List<Category> categories = productQueryRepository.filteredCategory(cleanKeyword, categoryIds, brandIds);

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();
        Map<Long, BigDecimal> priceMap = productQueryRepository.lowPriceOfProductList(productIds);
        List<GetProductDetailResponse> productDetailResponseList = products.stream()
                .map(product -> GetProductDetailResponse.of(product, priceMap.get(product.getId()) ))
                .toList();

        List<BrandResponse> brandListResponse = BrandResponse.toListBrandResponse(brands);
        List<CategorySearchResponse> categorySearchResponseList = CategorySearchResponse.toCategorySearchResponseList(categories);

        long totalCount = productQueryRepository.countFiltered(cleanKeyword, categoryIds, brandIds);

        int totalPage = (int) Math.ceil((double) totalCount / pageable.getPageSize());
        Pagination pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize(), totalCount, totalPage);
        SearchProductResponse searchProductResponse = SearchProductResponse.from(
                productDetailResponseList,
                brandListResponse,
                categorySearchResponseList,
                pagination);

        return ApiResponse.success(searchProductResponse);
    }

    @Override
    public ApiResponse<FavoriteProductList> getFavoriteProduct(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다."));
        List<Product> favoriteProducts = productQueryRepository.getFavoriteProducts(memberId);
        List<GetFavoriteProductResponse> getFavoriteProductResponses = favoriteProducts.stream()
                .map(GetFavoriteProductResponse::from)
                .toList();
        long totalCount = productQueryRepository.countFavoriteProducts(memberId);
        int totalPage = (int) Math.ceil((double) totalCount / pageable.getPageSize());
        Pagination pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize(), totalCount, totalPage);
        return ApiResponse.success(new FavoriteProductList(pagination, getFavoriteProductResponses));
    }
}
