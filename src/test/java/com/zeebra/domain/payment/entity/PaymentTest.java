package com.zeebra.domain.payment.entity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

@ActiveProfiles("test")
public class PaymentTest {
	@DisplayName("새로운 결제는 PENDING 상태로 생성된다.")
	@Test
	void createPayment(){
		//given	//when
		Payment payment = Payment.createPayment(1L, "TOSS_ORDER_ID", "테스트 주문", BigDecimal.valueOf(50000), "idem-key-001");
		//then
		assertThat(payment.getOrderId()).isEqualTo(1L);
		assertThat(payment.getTossOrderId()).isEqualTo("TOSS_ORDER_ID");
		assertThat(payment.getOrderNameSnapshot()).isEqualTo("테스트 주문");
		assertThat(payment.getPaymentAmount()).isEqualTo(BigDecimal.valueOf(50000));
		assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(payment.getTossResponse()).isEqualTo(new HashMap<>());
		assertThat(payment.getIdempotencyKey()).isEqualTo("idem-key-001");
	}

	@DisplayName("올바른 상태 전이는 규칙을 호출하고 상태가 실제로 바뀐다.")
	@Test
	void updatePaymentStatus(){
		//given
		Payment payment = Payment.createPayment(1L, "TOSS_ORDER_ID", "테스트 주문", BigDecimal.valueOf(50000), "idem-key-001");
		//when
		payment.updatePaymentStatus(PaymentStatus.APPROVING);
		//then
		assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVING);
	}

	@DisplayName("잘못된 상태 전이는 규칙 호출 중에 예외를 던진다.")
	@Test
	void updatePaymentStatusThrowsException(){
		//given
		Payment payment = Payment.createPayment(1L, "TOSS_ORDER_ID", "테스트 주문", BigDecimal.valueOf(50000), "idem-key-001");
		//when //then
		assertThatThrownBy(() -> payment.updatePaymentStatus(PaymentStatus.APPROVED)).isInstanceOf(BusinessException.class).hasMessage(
			PaymentErrorCode.INVALID_STATUS_TRANSITION.getMessage());
	}
}