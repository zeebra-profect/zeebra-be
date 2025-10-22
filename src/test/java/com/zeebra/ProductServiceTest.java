package com.zeebra;

import com.zeebra.domain.product.dto.ProductDetailResponse;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.repository.ProductQueryRepository;
import com.zeebra.domain.product.repository.ProductRepository;
import com.zeebra.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @InjectMocks
    private ProductService productService;

    private static final Long PRODUCT_ID = 1L;
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
}