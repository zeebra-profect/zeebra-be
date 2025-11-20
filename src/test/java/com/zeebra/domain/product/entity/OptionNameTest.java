package com.zeebra.domain.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class OptionNameTest {


    @DisplayName("옵션 이름이 color라면 오류를 던지지 않는다")
    @Test
    void validateOptionNameIsColor_noThrowWhenColor() {
        // given
        OptionName optionName = new OptionName("color", "빨강");

        // when & then
        assertThatCode(optionName::validateOptionNameIsColor).doesNotThrowAnyException();


    }

    @DisplayName("옵션 이름이 color가 아니라면 오류를 던진다 ")
    @Test
    void validateOptionNameIsColor_throwWhenNotColor() {
        // given
        OptionName optionName = new OptionName("size", "L");

        // when & then
        assertThatThrownBy(optionName::validateOptionNameIsColor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("옵션이 색상값이 아닙니다.");
    }

}