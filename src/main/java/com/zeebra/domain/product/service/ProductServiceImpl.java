package com.zeebra.domain.product.service;


import com.zeebra.domain.brand.dto.BrandResponse;
import com.zeebra.domain.product.search.ProductSearchHelper;
import com.zeebra.domain.product.search.ProductSearchQueryBuilder;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

import java.io.IOException;
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
    private final ProductSearchQueryBuilder queryBuilder;
    private final ProductSearchHelper searchHelper;
    private final OpenSearchClient openSearchClient;

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

        Query multiMatchQuery = Query.of(q -> q
                .multiMatch(m -> m
                        .query(searchWord)
                        .type(TextQueryType.BoolPrefix)
                        .fields(
                                "product_name",
                                "product_name.nori",
                                "product_name.ngram",
                                "brand_name",
                                "category_name"
                        )
                )
        );

        // 2. OpenSearch 검색 실행 (Native Client 사용)
        SearchResponse<ProductDocument> searchResponse = null;
        try {
            searchResponse = openSearchClient.search(s -> s
                            .index("products")  // 인덱스 이름
                            .query(multiMatchQuery)
                            .from(0)
                            .size(5)
                    , ProductDocument.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 3. 결과 변환
        List<String> suggestions = searchResponse.hits().hits().stream()
                .map(hit -> {
                    ProductDocument productDocument = hit.source();
                    return productDocument.getProductName();
                })
                .toList();

        // 4. Response 생성
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
        // 1. Function Score Query 생성
        Query functionScoreQuery = queryBuilder.buildFunctionScoreQuery(
                keyWord, categoryIds, brandIds, minPrice, maxPrice
        );

        log.info("OpenSearch Query: {}", functionScoreQuery);

        // 2. 정렬값 이넘으로 변경
        ProductSort sort = ProductSort.from(productSort);

        // 3. Highlight 생성
        Highlight highlight = searchHelper.buildHighlight();

        // 4. OpenSearch 검색 실행 (size + 1로 hasNext 계산)
        SearchResponse<ProductDocument> searchResponse = null;
        try {
            searchResponse = openSearchClient.search(s -> s
                            .index("product")
                            .query(functionScoreQuery)
                            .highlight(highlight)
                            .from(pageable.getPageNumber() * pageable.getPageSize())
                            .size(pageable.getPageSize() + 1)
                            .sort(searchHelper.buildSort(sort).stream()
                                    .map(order -> SortOptions.of(so -> so
                                            .field(f -> f
                                                    .field(order.getProperty())
                                                    .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)
                                            )
                                    ))
                                    .toList())
                    , ProductDocument.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 5. 결과 추출
        List<Hit<ProductDocument>> hits = searchResponse.hits().hits();

        // 6. hasNext 계산
        boolean hasNext = hits.size() > pageable.getPageSize();
        List<Hit<ProductDocument>> resultHits = hasNext ?
                hits.subList(0, pageable.getPageSize()) : hits;

        // 7. 결과 변환
        List<ProductSearchItem> productSearchItems =
                searchHelper.convertToProductSearchItems(resultHits);

        // 8. Pagination 생성
        SearchProductPagination pagination = new SearchProductPagination(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                hasNext
        );

        // 9. Response 생성
        SearchProductResponse response = SearchProductResponse.of(
                productSearchItems,
                pagination
        );

        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<SearchBrandListResponse> searchedBrands(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {

        Query functionScoreQuery = queryBuilder.buildFunctionScoreQuery(
                keyWord, categoryIds, brandIds, minPrice, maxPrice
        );

        // Composite Aggregation: brand_id + brand_name을 함께 그룹핑
        SearchResponse<ProductDocument> searchResponse = null;
        try {
            searchResponse = openSearchClient.search(s -> s
                            .index("products")
                            .query(functionScoreQuery)
                            .size(0)  // 문서는 안 가져옴 (aggregation만 필요)
                            .aggregations("brand_agg", a -> a
                                    .composite(c -> c
                                            .size(1000)
                                            .sources(List.of(
                                                    Map.of("brand_id", CompositeAggregationSource.of(cs -> cs
                                                            .terms(t -> t.field("brand_id"))
                                                    )),
                                                    Map.of("brand_name", CompositeAggregationSource.of(cs -> cs
                                                            .terms(t -> t.field("brand_name.keyword"))
                                                    ))
                                            ))
                                    )
                            )
                    , ProductDocument.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<BrandResponse> brandResponses = searchHelper.extractBrandInfoFromComposite(searchResponse);

        return ApiResponse.success(new SearchBrandListResponse(brandResponses));
    }

    @Override
    public ApiResponse<SearchCategoryListResponse> searchedCategories(
            String keyWord,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {

        Query functionScoreQuery = queryBuilder.buildFunctionScoreQuery(
                keyWord, categoryIds, brandIds, minPrice, maxPrice
        );

        // Composite Aggregation: category_id + category_name을 함께 그룹핑
        SearchResponse<ProductDocument> searchResponse = null;
        try {
            searchResponse = openSearchClient.search(s -> s
                            .index("products")
                            .query(functionScoreQuery)
                            .size(0)  // 문서는 안 가져옴 (aggregation만 필요)
                            .aggregations("category_agg", a -> a
                                    .composite(c -> c
                                            .size(1000)
                                            .sources(List.of(
                                                    Map.of("category_id", CompositeAggregationSource.of(cs -> cs
                                                            .terms(t -> t.field("category_id"))
                                                    )),
                                                    Map.of("category_name", CompositeAggregationSource.of(cs -> cs
                                                            .terms(t -> t.field("category_name.keyword"))
                                                    ))
                                            ))
                                    )
                            )
                    , ProductDocument.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<CategoryResponseDto> categoryResponses = searchHelper.extractCategoryInfoFromComposite(searchResponse);

        return ApiResponse.success(new SearchCategoryListResponse(categoryResponses));
    }
}