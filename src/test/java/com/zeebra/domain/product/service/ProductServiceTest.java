package com.zeebra.domain.product.service;

import com.zeebra.IntegrationTestSupport;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.brand.repository.BrandRepository;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.category.repository.CategoryRepository;
import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.*;
import com.zeebra.domain.product.entity.*;
import com.zeebra.domain.product.repository.*;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.MemberErrorCode;
import com.zeebra.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
public class ProductServiceTest extends IntegrationTestSupport {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FavoriteProductRepository favoriteProductRepository;

    @Autowired
    private OptionNameRepository optionNameRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OptionCombinationRepository optionCombinationRepository;

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
        favoriteProductRepository.deleteAllInBatch();
        optionNameRepository.deleteAllInBatch();
        productOptionRepository.deleteAllInBatch();
        salesRepository.deleteAllInBatch();
        brandRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        optionCombinationRepository.deleteAllInBatch();
        brandRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @DisplayName("상품 정보를 받아서 상품을 생성한다")
    @Test
    void createProductTest() {
        // given
        Member admin = createAdmin("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        Member saveAdmin = memberRepository.save(admin);

        ProductRequest productRequest = new ProductRequest(100L, 200L,
                "테스트 상품", "상품 설명", "MODEL-001", "thumbnail.jpg",
                List.of("image1.jpg", "image2.jpg"));
        // when
        ApiResponse<ProductResponse> product = productService.createProduct(saveAdmin.getId(), productRequest);

        // then

        assertThat(product.getData().productId()).isNotNull();
        assertThat(product.getData().brandId()).isEqualTo(100L);
        assertThat(product.getData().categoryId()).isEqualTo(200L);
    }

    @DisplayName("관리자가 아닌 사용자는 상품을 생성할 수 없다")
    @Test
    void createProduct_whenUserIsNotAdmin() {
        // given
        Member member = Member.createMember("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        Member saveMember = memberRepository.save(member);

        ProductRequest productRequest = new ProductRequest(100L, 200L,
                "테스트 상품", "상품 설명", "MODEL-001", "thumbnail.jpg",
                List.of("image1.jpg", "image2.jpg"));

        // when & then
        assertThatThrownBy(() -> productService.createProduct(saveMember.getId(), productRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("관리자가 아닙니다.");
    }


    @DisplayName("존재하지 않는 회원은 상품을 생성할 수 없다.")
    @Test
    void createProduct_MemberNotFound_ThrowsBusinessException() {
        // given
        Long notExistId = 92345098L;
        ProductRequest productRequest = new ProductRequest(100L, 200L,
                "테스트 상품", "상품 설명", "MODEL-001", "thumbnail.jpg",
                List.of("image1.jpg", "image2.jpg"));

        // when & then
        assertThatThrownBy(() -> productService.createProduct(notExistId, productRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());

    }


    @DisplayName("관심 상품 추가 성공")
    @Test
    void addFavoriteProduct_Success() {
        // given
        Member member = Member.createMember("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        Member saveMember = memberRepository.save(member);

        Product product = createProduct("test1");
        Product saveProduct = productRepository.save(product);

        // when
        ApiResponse<FavoriteProductResponse> favoriteProductResponse = productService.addFavoriteProduct(saveMember.getId(), saveProduct.getId());

        // then
        assertThat(favoriteProductResponse.getData().productId()).isNotNull();
        assertThat(favoriteProductResponse.getData().memberId()).isEqualTo(saveMember.getId());
        assertThat(favoriteProductResponse.getData().productId()).isEqualTo(saveProduct.getId());

    }


    @DisplayName("관심 상품 추가 시 상품의 좋아요 수가 증가한다")
    @Test
    void addFavoriteProduct_IncreaseFavoriteCount() {
        // given
        Member member = Member.createMember("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        Member saveMember = memberRepository.save(member);

        Product product = createProduct("test1");
        Product saveProduct = productRepository.save(product);

        // when
        ApiResponse<FavoriteProductResponse> favoriteProductResponse = productService.addFavoriteProduct(saveMember.getId(), saveProduct.getId());

        // then
        Product updatedProduct = productRepository.findById(saveProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getFavoriteProductCount()).isEqualTo(saveProduct.getFavoriteProductCount() + 1);
    }


    @DisplayName("관심 상품 삭제 성공")
    @Test
    void deleteFavoriteProduct_Success() {
        // given
        Member member = Member.createMember("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        Member saveMember = memberRepository.save(member);

        Product product = createProduct("test1");
        Product saveProduct = productRepository.save(product);

        productService.addFavoriteProduct(saveMember.getId(), saveProduct.getId());

        // when
        productService.deleteFavoriteProduct(saveMember.getId(), saveProduct.getId());

        // then
        assertThat(favoriteProductRepository.findById(saveProduct.getId()).isPresent()).isFalse();
    }


    @DisplayName("관심 상품 삭제 시 상품의 좋아요 수가 감소한다")
    @Test
    void deleteFavoriteProduct_DecreaseFavoriteCount() {
        // given
        Member member = Member.createMember("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        Member saveMember = memberRepository.save(member);

        Product product = createProduct("test1");
        Product saveProduct = productRepository.save(product);

        productService.addFavoriteProduct(saveMember.getId(), saveProduct.getId());
        Product saveUpdateProduct = productRepository.findById(saveProduct.getId()).orElseThrow();

        // when
        productService.deleteFavoriteProduct(saveMember.getId(), saveProduct.getId());

        // then
        Product deleteUpdateProduct = productRepository.findById(saveProduct.getId()).orElseThrow();

        assertThat(saveUpdateProduct.getFavoriteProductCount()).isEqualTo(deleteUpdateProduct.getFavoriteProductCount() + 1);
    }


    @DisplayName("회원의 관심 상품 목록 조회 성공")
    @Test
    void getFavoriteProduct_Success() {
        // given
        Product product1 = productRepository.save(createProduct("test1"));
        Product product2 = productRepository.save(createProduct("test2"));

        Member member = memberRepository.save(createMember("testUser123"));

        productService.addFavoriteProduct(member.getId(), product1.getId());
        productService.addFavoriteProduct(member.getId(), product2.getId());

        Pageable pageable = PageRequest.of(0, 10);
        // when
        ApiResponse<FavoriteProductList> favoriteProducts = productService.getFavoriteProduct(member.getId(), pageable);

        // then
        assertThat(favoriteProducts.getData().favoriteProductResponses().size()).isEqualTo(2);
        assertThat(favoriteProducts.getData().pagination().currentPage()).isEqualTo(0);
        assertThat(favoriteProducts.getData().pagination().totalPages()).isEqualTo(1);
        assertThat(favoriteProducts.getData().pagination().totalCount()).isEqualTo(2);

    }


    @DisplayName("colorOptionNameId가 null일 때 첫 번째 색상 옵션으로 상품 상세 조회 성공")
    @Test
    void getProductDetail_WithNullColorOptionNameId_ReturnsFirstColorOption() {
        // given
        Product product1 = productRepository.save(createProduct("test1"));
        OptionName optionName1 = optionNameRepository.save(new OptionName("color", "빨강"));
        OptionName optionName2 = optionNameRepository.save(new OptionName("size", "L"));

        ProductOption productOption1 = productOptionRepository.save(new ProductOption(product1.getId()));

        optionCombinationRepository.save(new OptionCombination(productOption1.getId(), optionName1.getId()));
        optionCombinationRepository.save(new OptionCombination(productOption1.getId(), optionName2.getId()));

        Sales sales1 = salesRepository.save(new Sales(productOption1.getId(), 1L, new BigDecimal("15000.00"), 1, SalesStatus.ON_SALE));
        Sales sales2 = salesRepository.save(new Sales(productOption1.getId(), 1L, new BigDecimal("16000.00"), 1, SalesStatus.ON_SALE));

        // when
        ApiResponse<ProductDetailResponse> productDetail = productService.getProductDetail(product1.getId(), null);

        // then
        assertThat(productDetail.getData().colorOptionResponses().size()).isEqualTo(1);
        assertThat(productDetail.getData().productId()).isEqualTo(product1.getId());
        assertThat(productDetail.getData().lowPrice()).isEqualTo(sales1.getPrice());

    }


    @DisplayName("colorOptionNameId를 지정하여 해당 색상 옵션의 상품 상세 조회 성공")
    @Test
    void getProductDetail_WithSpecificColorOptionNameId_ReturnsSpecifiedColorOption() {
        // given
        Product product1 = productRepository.save(createProduct("test1"));
        OptionName optionName1 = optionNameRepository.save(new OptionName("color", "빨강"));
        OptionName optionName2 = optionNameRepository.save(new OptionName("size", "L"));

        ProductOption productOption1 = productOptionRepository.save(new ProductOption(product1.getId()));

        optionCombinationRepository.save(new OptionCombination(productOption1.getId(), optionName1.getId()));
        optionCombinationRepository.save(new OptionCombination(productOption1.getId(), optionName2.getId()));

        Sales sales1 = salesRepository.save(new Sales(productOption1.getId(), 1L, new BigDecimal("15000.00"), 1, SalesStatus.ON_SALE));
        Sales sales2 = salesRepository.save(new Sales(productOption1.getId(), 1L, new BigDecimal("16000.00"), 1, SalesStatus.ON_SALE));

        // when
        ApiResponse<ProductDetailResponse> productDetail = productService.getProductDetail(product1.getId(), optionName1.getId());

        // then
        assertThat(productDetail.getData().colorOptionResponses().size()).isEqualTo(1);
        assertThat(productDetail.getData().productId()).isEqualTo(product1.getId());
        assertThat(productDetail.getData().lowPrice()).isEqualTo(sales1.getPrice());

    }

    @DisplayName("상품 상세 조회시 상품이 존재하지 않을 때 오류를 반환한다")
    @Test
    void getProductDetail_notFound_whenProductNotExist() {
        // given
        OptionName optionName1 = optionNameRepository.save(new OptionName("color", "빨강"));

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(985512367L, optionName1.getId()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("해당하는 상품이 존재하지 않습니다.");
    }

    @DisplayName("색상 옵션 ID로 사이즈 옵션 목록을 조회한다")
    @Test
    void getProductOptionSize_WithValidColorOptionId_ReturnsSizeOptionList() {
        // given
        Product product1 = productRepository.save(createProduct("test1"));
        OptionName optionName1 = optionNameRepository.save(new OptionName("color", "빨강"));
        OptionName optionName2 = optionNameRepository.save(new OptionName("size", "L"));

        ProductOption productOption1 = productOptionRepository.save(new ProductOption(product1.getId()));

        optionCombinationRepository.save(new OptionCombination(productOption1.getId(), optionName1.getId()));
        optionCombinationRepository.save(new OptionCombination(productOption1.getId(), optionName2.getId()));

        Sales sales1 = salesRepository.save(new Sales(productOption1.getId(), 1L, new BigDecimal("15000.00"), 1, SalesStatus.ON_SALE));
        Sales sales2 = salesRepository.save(new Sales(productOption1.getId(), 1L, new BigDecimal("16000.00"), 1, SalesStatus.ON_SALE));

        // when
        ApiResponse<SizeOptionResponseList> productOptionSize = productService.getProductOptionSize(product1.getId(), optionName1.getId());

        // then
        assertThat(productOptionSize.getData().sizeOptionResponses().size()).isEqualTo(1);
        assertThat(productOptionSize.getData().sizeOptionResponses().getFirst().lowPriceOfSize()).isEqualTo(sales1.getPrice());

    }

    private FavoriteProduct createFavoriteProduct(Long productId, Long memberId) {
        return new FavoriteProduct(memberId, productId);
    }

    private Product createProduct(String productName) {
        return Product.builder()
                .thumbnail("testImage")
                .images(List.of("testImage1", "testImage2"))
                .name(productName)
                .brandId(1L)
                .categoryId(1L)
                .modelNumber("testModelNumber")
                .build();
    }

    private Member createMember(String memberName) {
        return Member.createMember(
                "testLoginId",
                memberName,
                "testEmail@test.com",
                "testNickName",
                LocalDate.now(),
                Gender.MAN,
                "hashedPassword123"
        );
    }

    private Member createAdmin(String userLoginId,
                               String memberName,
                               String memberEmail,
                               String nickname,
                               LocalDate birth,
                               Gender gender,
                               String passwordHash) {
        return Member.createAdmin(userLoginId, memberName, memberEmail, nickname, birth, gender, passwordHash);
    }

    private ProductRequest createProductRequest(Long brandId,
                                                Long categoryId,
                                                String productName,
                                                String productDescription,
                                                String modelName,
                                                String productThumbnail,
                                                List<String> productImages) {
        return ProductRequest.builder()
                .productThumbnail(productThumbnail)
                .productImages(productImages)
                .brandId(brandId)
                .categoryId(categoryId)
                .productName(productName)
                .modelName(modelName)
                .productDescription(productDescription)
                .build();
    }
}
