package com.zeebra.domain.payment.dto;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

public class CreatePaymentRequestTest {
	@DisplayName("price - discount가 amount와 같으면 DTO 내부 검증을 통과합니다.")
	@Test
	void validateInternalAmount() {
		//given
		CreatePaymentRequest request = new CreatePaymentRequest(
			1L,
			"테스트 결제",
			BigDecimal.valueOf(10000),
			BigDecimal.ZERO,
			BigDecimal.valueOf(10000),
			"idem-key-001"
		);
		//when //then
		assertThatCode(request::validateInternalAmount).doesNotThrowAnyException();
	}

	@DisplayName("price - discount가 amount와 다르면 예외를 던집니다.")
	@Test
	void validateInternalAmountWithInvalidPriceAndDiscount() {
		//given
		CreatePaymentRequest request = new CreatePaymentRequest(
			1L,
			"테스트 결제",
			BigDecimal.valueOf(10000),
			BigDecimal.ONE,
			BigDecimal.valueOf(10000),
			"idem-key-001"
		);
		//when //then
		assertThatThrownBy(request::validateInternalAmount).isInstanceOf(BusinessException.class).hasMessage(
			PaymentErrorCode.INVALID_AMOUNT.getMessage());
	}
}