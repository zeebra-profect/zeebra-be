package com.zeebra.domain.product.entity;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {


    @DisplayName("관심상품 갯수를 증가시킨다.")
    @Test
    void increaseFavoriteProductCount() {
        // given
        Product product = createProduct("test1");

        // when
        product.increaseFavoriteProductCount();

        // then
        assertThat(product.getFavoriteProductCount()).isEqualTo(1);
    }


    @DisplayName("관심상품 갯수를 감소시킨다.")
    @Test
    void decreaseFavoriteProductCount() {
        // given
        Product product = createProduct("test1");
        product.increaseFavoriteProductCount();

        // when
        product.decreaseFavoriteProductCount();

        // then
        assertThat(product.getFavoriteProductCount()).isEqualTo(0);

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