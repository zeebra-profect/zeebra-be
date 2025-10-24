package com.zeebra;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.FavoriteProductResponse;
import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.entity.FavoriteProduct;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.repository.FavoriteProductRepository;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.product.repository.ProductQueryRepository;
import com.zeebra.domain.product.repository.ProductRepository;
import com.zeebra.domain.product.repository.SalesRepository;
import com.zeebra.domain.product.service.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductQueryRepository productQueryRepository;
    @Mock private SalesRepository salesRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private static final Long PRODUCT_ID = 1L;
    private static final Long MEMBER_ID  = 7L;
    private static final BigDecimal LOW_PRICE = new BigDecimal("29900");

    private Product createMockProduct() {
        return new Product(
                PRODUCT_ID,
                1L,
                1L,
                "에어포스 1",
                "클래식 스니커즈",
                "CW2288-111",
                "https://example.com/thumbnail.jpg",
                List.of("img1.jpg", "img2.jpg"),
                150,
                3200,
                LocalDateTime.now()
        );
    }

    // -------------------------
    // getProductDetail 기존 테스트
    // -------------------------
    @Test
    @DisplayName("상품 상세 조회 성공 - 최저가 포함")
    void getProductDetail_success() {
        // given
        Product mockProduct = createMockProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(mockProduct));
        when(productQueryRepository.lowPriceOfProduct(PRODUCT_ID)).thenReturn(LOW_PRICE);

        // when
        ProductDetailResponse response = productService.getProductDetail(PRODUCT_ID);

        // then
        assertNotNull(response);
        assertEquals(PRODUCT_ID, response.productId());
        assertEquals(1L, response.brandId());
        assertEquals(1L, response.categoryId());
        assertEquals("에어포스 1", response.productName());
        assertEquals(LOW_PRICE, response.lowPrice());
        assertEquals(150, response.reviewCount());
        assertEquals(3200, response.favoriteProductCount());

        verify(productRepository).findById(PRODUCT_ID);
        verify(productQueryRepository).lowPriceOfProduct(PRODUCT_ID);
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 상품 없음")
    void getProductDetail_fail_productNotFound() {
        // given
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // when & then
        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> productService.getProductDetail(PRODUCT_ID)
        );

        assertEquals("해당하는 상품이 존재하지 않습니다.", ex.getMessage());
        verify(productQueryRepository, never()).lowPriceOfProduct(anyLong());
    }

    // -------------------------
    // addFavoriteProduct 추가 테스트
    // -------------------------
    @Test
    @DisplayName("addFavoriteProduct 성공: 회원/상품 존재 시 즐겨찾기 저장 및 DTO 반환")
    void addFavoriteProduct_success() {
        // given
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(MEMBER_ID);

        Product mockProduct = createMockProduct();

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(mockMember));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(mockProduct));

        // save에 전달되는 엔티티를 캡쳐하고, 그대로 반환하도록 구성
        ArgumentCaptor<FavoriteProduct> captor = ArgumentCaptor.forClass(FavoriteProduct.class);
        when(favoriteProductRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        FavoriteProductResponse resp = productService.addFavoriteProduct(MEMBER_ID, PRODUCT_ID);

        // then - 저장 인수 검증
        FavoriteProduct saved = captor.getValue();
        assertEquals(MEMBER_ID, saved.getMemberId());
        assertEquals(PRODUCT_ID, saved.getProductId());

        // then - 반환 DTO(현 구현 기준: productId가 두 번 들어감)에 대한 최소 검증
        assertNotNull(resp);
        assertEquals(PRODUCT_ID, resp.productId());
        assertEquals(MEMBER_ID, resp.memberId());

        // 상호작용 검증
        InOrder inOrder = inOrder(memberRepository, productRepository, favoriteProductRepository);
        inOrder.verify(memberRepository).findById(MEMBER_ID);
        inOrder.verify(productRepository).findById(PRODUCT_ID);
        inOrder.verify(favoriteProductRepository).save(any(FavoriteProduct.class));
    }

    @Test
    @DisplayName("addFavoriteProduct 실패: 회원 없음")
    void addFavoriteProduct_fail_memberNotFound() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // when & then
        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> productService.addFavoriteProduct(MEMBER_ID, PRODUCT_ID)
        );
        assertEquals("해당하는 사용자가 없습니다", ex.getMessage());

        verify(memberRepository).findById(MEMBER_ID);
        verifyNoInteractions(productRepository, favoriteProductRepository);
    }

    @Test
    @DisplayName("addFavoriteProduct 실패: 상품 없음")
    void addFavoriteProduct_fail_productNotFound() {
        // given
        Member mockMember = mock(Member.class);
        // when(mockMember.getId()).thenReturn(MEMBER_ID); // 호출되지 않으므로 제거
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(mockMember));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // when & then
        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> productService.addFavoriteProduct(MEMBER_ID, PRODUCT_ID)
        );
        assertEquals("해당하는 상품이 존재하지 않습니다.", ex.getMessage());

        InOrder inOrder = inOrder(memberRepository, productRepository);
        inOrder.verify(memberRepository).findById(MEMBER_ID);
        inOrder.verify(productRepository).findById(PRODUCT_ID);

        verifyNoInteractions(favoriteProductRepository);
    }
}
