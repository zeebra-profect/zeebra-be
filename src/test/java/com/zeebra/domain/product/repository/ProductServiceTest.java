package com.zeebra.domain.product.repository;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.ProductRequest;
import com.zeebra.domain.product.dto.ProductResponse;
import com.zeebra.domain.product.service.ProductService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.MemberErrorCode;
import com.zeebra.global.exception.BusinessException;
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
