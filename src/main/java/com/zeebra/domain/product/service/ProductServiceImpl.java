package com.zeebra.domain.product.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.zeebra.domain.product.search.ProductSearchHelper;
import com.zeebra.domain.product.search.ProductSearchQueryBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.stereotype.Service;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.member.service.MemberService;
import com.zeebra.domain.product.dto.*;
import com.zeebra.domain.product.entity.*;
import com.zeebra.domain.product.repository.*;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

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
    private final ProductOptionRepository productOptionRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchQueryBuilder queryBuilder;
    private final ProductSearchHelper searchHelper;

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

    public void validateProductOptionId(Long productOptionId) {
        productOptionRepository.findById(productOptionId).orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 상품입니다."));
    }

    @Override
    public ApiResponse<SuggestionListResponse> getSuggestions(String searchWord) {
        Query multiMatchQuery = MultiMatchQuery.of(m -> m.query(searchWord)
                        .type(TextQueryType.BoolPrefix)
                        .fields(
                                "product_name",
                                "product_name.nori",
                                "product_name.ngram",
                                "brand_name",
                                "category_name"
                        ))
                ._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(multiMatchQuery)
                .withPageable(PageRequest.of(0, 5))
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<String> suggestions = searchHits.getSearchHits().stream()
                .map(hit -> {
                    ProductDocument productDocument = hit.getContent();
                    return productDocument.getProductName();
                })
                .toList();

        SuggestionListResponse suggestionListResponse = new SuggestionListResponse(suggestions);

        return ApiResponse.success(suggestionListResponse);
    }

    @Override
    public ApiResponse<SearchProductResponse> searchProduct(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            String productSort
    ) {
        // 1. Bool Query 생성
        Query boolQuery = queryBuilder.buildBoolQuery(
                keyWord, categoryIds, brandIds, minPrice, maxPrice
        );

        // 정렬값 이넘으로 변경
        ProductSort sort = ProductSort.from(productSort);

        // 2. Highlight 쿼리 생성
        HighlightQuery highlightQuery = searchHelper.buildHighlightQuery();

        // 3. Native Query 생성 (size + 1로 조회)
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withHighlightQuery(highlightQuery)
                .withPageable(PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize() + 1,
                        searchHelper.buildSort(sort)
                ))
                .build();

        // 4. 검색 실행
        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<SearchHit<ProductDocument>> hits = searchHits.getSearchHits();

        // 5. hasNext 계산
        boolean hasNext = hits.size() > pageable.getPageSize();
        List<SearchHit<ProductDocument>> resultHits = hasNext ?
                hits.subList(0, pageable.getPageSize()) : hits;

        // 6. 결과 변환
        List<ProductSearchItem> productSearchItems =
                searchHelper.convertToProductSearchItems(resultHits);

        // 7. Pagination 생성
        SearchProductPagination pagination = new SearchProductPagination(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                hasNext
        );

        // 8. Response 생성
        SearchProductResponse response = SearchProductResponse.of(
                productSearchItems,
                pagination
        );

        return ApiResponse.success(response);
    }


}