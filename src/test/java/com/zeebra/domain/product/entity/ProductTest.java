package com.zeebra.domain.product.entity;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.product.dto.ProductRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {


    @DisplayName("관심상품이 하나라도 있으면 true를 반환한다")
    @Test
    void returnsTrueWhenFavoriteProductsExist() {
        // given
        Product product = createProduct("test1");
        product.increaseFavoriteProductCount();
        // when
        Boolean hasFavoriteProducts = product.hasFavoriteProducts();

        // then
        assertThat(hasFavoriteProducts).isTrue();
    }


    @DisplayName("관심상품이 없으면 false를 반환한다")
    @Test
    void returnsFalseWhenNoFavoriteProducts() {
        // given
        Product product = createProduct("test1");

        // when
        Boolean hasFavoriteProducts = product.hasFavoriteProducts();

        // then
        assertThat(hasFavoriteProducts).isFalse();

    }

    @DisplayName("관심상품 갯수를 증가시킨다")
    @Test
    void increaseFavoriteProductCount_incrementsCount() {
        // given
        Product product = createProduct("test1");

        // when
        product.increaseFavoriteProductCount();

        // then
        assertThat(product.getFavoriteProductCount()).isEqualTo(1);
    }


    @DisplayName("관심상품이 있을 때만 감소시킬 수 있다")
    @Test
    void decreaseFavoriteProductCount_decrementsOnlyWhenCountGreaterThanZero() {
        // given
        Product product = createProduct("test1");
        product.increaseFavoriteProductCount();

        // when
        product.decreaseFavoriteProductCount();

        // then
        assertThat(product.getFavoriteProductCount()).isEqualTo(0);

    }


    @DisplayName("관심상품이 없을 때 감소시키면 예외가 발생한다")
    @Test
    void decreaseFavoriteProductCount_throwsException_whenNoFavorites() {
        // given
        Product product = createProduct("test1");

        // when & then
        assertThatThrownBy(product::decreaseFavoriteProductCount)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("관심상품 개수가 0 이하인데 감소를 시도했습니다")
                .hasMessageContaining("현재 값: 0");


    }


    @DisplayName("ProductRequest를 Product로 반환할수 있다")
    @Test
    void fromRequest_createsProductSuccessfully() {
        // given
        ProductRequest request = new ProductRequest(1L, 1L, "test",
                "test", "test", "test", List.of("test"));

        // when
        Product fromRequest = Product.from(request);

        // then
        assertThat(fromRequest.getBrandId()).isEqualTo(request.brandId());
        assertThat(fromRequest.getCategoryId()).isEqualTo(request.categoryId());
        assertThat(fromRequest.getName()).isEqualTo(request.productName());

    }


    private Product createProduct(String name) {
        return Product.builder()
                .name(name)
                .description("thisIsTest")
                .modelNumber("thisIsTest")
                .images(List.of("testImage"))
                .thumbnail("testThum")
                .brandId(2L)
                .categoryId(1L)
                .build();
    }
}