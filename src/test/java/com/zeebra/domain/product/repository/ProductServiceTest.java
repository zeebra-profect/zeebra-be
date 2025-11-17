package com.zeebra.domain.product.repository;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductRequest;
import com.zeebra.domain.product.dto.ProductResponse;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.MemberErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

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
        Long notExistId = 100L;
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
