package com.zeebra.domain.product.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.web.KeywordSanitizer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final SalesRepository salesRepository;
    private final ProductOptionRepository productOptionRepository;
    private final MemberRepository memberRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final ProductOptionQueryRepository productOptionQueryRepository;
    private final OptionNameRepository optionNameRepository;
    private final MemberService memberService;

    private List<GetProductDetailResponse> toProductDetailResponse(List<Product> products) {
        return products.stream()
                .map(product -> new GetProductDetailResponse(
                        product.getId(),
                        product.getBrandId(),
                        product.getCategoryId(),
                        product.getName(),
                        product.getDescription(),
                        product.getModelNumber(),
                        product.getThumbnail(),
                        product.getImages(),
                        productQueryRepository.lowPriceOfProduct(product.getId()),
                        product.getReviewCount(),
                        product.getFavoriteProductCount(),
                        product.getCreatedTime()
                ))
                .toList();
    }

    private ProductDetailResponse toProductDetailResponse(Product product,
                                                          BigDecimal lowPriceOfProduct,
                                                          List<ColorOptionResponse> colorOptionResponses,
                                                          String colorValue) {
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
                product.getCreatedTime(),
                colorOptionResponses,
                colorValue);
    }

    private FavoriteProductResponse toFavoriteProductResponse(FavoriteProduct favoriteProduct) {
        return new FavoriteProductResponse(
                favoriteProduct.getProductId(),
                favoriteProduct.getProductId(),
                favoriteProduct.getMemberId(),
                favoriteProduct.getCreatedTime());
    }

    private List<BrandResponse> toBrandListResponse(List<Brand> brands) {
        return brands.stream()
                .map(brand -> new BrandResponse(brand.getId(), brand.getName()))
                .toList();
    }

    private List<CategorySearchResponse> toCategorySearchResponseList(List<Category> categories) {
        return categories.stream()
                .map(category -> new CategorySearchResponse(category.getId(), category.getName()))
                .toList();
    }

    private SearchProductResponse toSearchProductResponse(List<GetProductDetailResponse> productDetailResponseList,
                                                          List<BrandResponse> brandListResponse,
                                                          List<CategorySearchResponse> categorySearchResponseList,
                                                          Pagination pagination) {
        return new SearchProductResponse(productDetailResponseList,
                categorySearchResponseList,
                brandListResponse,
                pagination);
    }

    private ProductSort parseProductSort(String productSort) {
        if (productSort == null || productSort.isBlank()) {
            return ProductSort.REVIEW_COUNT_MOST;
        }

        return switch (productSort.trim().toLowerCase()) {
            case "most_reviewed" -> ProductSort.REVIEW_COUNT_MOST;
            case "least_reviewed" -> ProductSort.REVIEW_COUNT_LEAST;
            default -> ProductSort.REVIEW_COUNT_MOST;
        };
    }

    @Override
    public ApiResponse<ProductDetailResponse> getProductDetail(Long productId, Long colorOptionNameId) {
        try {
            Product product = productRepository.findById(productId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

            List<OptionName> colorOptionNames = productOptionQueryRepository.findAllOptionNames(product.getId());

            List<ColorOptionResponse> colorOptionResponses = colorOptionNames.stream()
                    .map(colorOptionName -> new ColorOptionResponse(colorOptionName.getId(), colorOptionName.getValue()))
                    .toList();

//            List<SizeOptionResponse> sizeOptionResponses = productOptionQueryRepository.findByColorOptionName(colorOptionResponses.get(0).optionNameId(), product.getId());

            if (colorOptionNameId == null) {

                BigDecimal lowPriceOfColor = productQueryRepository.lowPriceOfColor(product.getId(), colorOptionResponses.get(0).colorOptionNameId());

                return ApiResponse.success(toProductDetailResponse(product, lowPriceOfColor, colorOptionResponses, colorOptionResponses.get(0).colorValue()));
            } else {

                BigDecimal lowPriceOfColor = productQueryRepository.lowPriceOfColor(product.getId(), colorOptionNameId);

                OptionName optionName = optionNameRepository.findById(colorOptionNameId).orElseThrow(
                        () -> new NoSuchElementException("해당하는 옵션이름이 없습니다."));

                return ApiResponse.success(toProductDetailResponse(product, lowPriceOfColor, colorOptionResponses, optionName.getValue()));
            }
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "상품 상세 조회 과정에 오류가 발생했습니다.");
        }
    }

    @Override
    public ApiResponse<SizeOptionResponseList> getProductOptionSize(Long productId, Long colorOptionNameId) {
        OptionName optionName = optionNameRepository.findById(colorOptionNameId).orElseThrow(
                () -> new NoSuchElementException("해당하는 옵션값이 없습니다."));
        if (!"color".equals(optionName.getName())) {
            return ApiResponse.error(null, "색상값이 아닙니다.");
        }
        List<SizeOptionResponse> sizeOptionResponses = productOptionQueryRepository.findByColorOptionName(colorOptionNameId, productId);
        return ApiResponse.success(new SizeOptionResponseList(sizeOptionResponses));
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

            product.increaseFavoriteProductCount();

            productRepository.save(product);

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
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다"));

        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 존재하지 않습니다."));

        FavoriteProduct favoriteProduct = favoriteProductRepository.findByMemberIdAndProductId(member.getId(), product.getId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 관심 상품이 없습니다."));

        favoriteProductRepository.delete(favoriteProduct);

        product.decreaseFavoriteProductCount();

        productRepository.save(product);

        return ApiResponse.successMessage("상품 삭제에 성공했습니다.");
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

        ProductSort parseProductSort = parseProductSort(productSort);

        List<Product> products = productQueryRepository.searchProduct(cleanKeyword, categoryIds, brandIds, pageable, parseProductSort);

        List<Brand> brands = productQueryRepository.filteredBrand(cleanKeyword, categoryIds, brandIds);

        List<Category> categories = productQueryRepository.filteredCategory(cleanKeyword, categoryIds, brandIds);

        List<GetProductDetailResponse> productDetailResponseList = toProductDetailResponse(products);

        List<BrandResponse> brandListResponse = toBrandListResponse(brands);

        List<CategorySearchResponse> categorySearchResponseList = toCategorySearchResponseList(categories);

        long totalCount = productQueryRepository.countFiltered(cleanKeyword, categoryIds, brandIds);

        int totalPage = (int) Math.ceil((double) totalCount / pageable.getPageSize());

        Pagination pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize(), totalCount, totalPage);
        SearchProductResponse searchProductResponse = toSearchProductResponse(
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
                .map(product -> new GetFavoriteProductResponse(
                        product.getId(),
                        product.getBrandId(),
                        product.getCategoryId(),
                        product.getName(),
                        product.getDescription(),
                        product.getModelNumber(),
                        product.getThumbnail()
                ))
                .toList();
        long totalCount = productQueryRepository.countFavoriteProducts(memberId);
        int totalPage = (int) Math.ceil((double) totalCount / pageable.getPageSize());
        Pagination pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize(), totalCount, totalPage);
        return ApiResponse.success(new FavoriteProductList(pagination, getFavoriteProductResponses));
    }

	public void validateProductOptionId(Long productOptionId) {
		productOptionRepository.findById(productOptionId).orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 상품입니다."));
	}
}