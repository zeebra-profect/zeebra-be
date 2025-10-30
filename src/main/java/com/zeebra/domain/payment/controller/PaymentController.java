package com.zeebra.domain.payment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.service.PaymentService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "결제 관련 API")
public class PaymentController {

	private final PaymentService paymentService;


	@Operation(summary = "결제 정보 생성 API", description = "orderId를 통해서 결제 정보를 생성합니다. 토스로 결제 요청을 보내기 전에 DB에 결제 정보를 저장하는 역할을 합니다.")
	@PostMapping()
	public ApiResponse<CreatePaymentResponse> createPayment(@RequestBody @Valid CreatePaymentRequest request, @AuthenticationPrincipal
		JwtProvider.JwtUserPrincipal principal) {

		Long memberId = principal.getMemberId();

		CreatePaymentResponse response = paymentService.createPayment(request, memberId);

		return ApiResponse.success(response);
	}
}