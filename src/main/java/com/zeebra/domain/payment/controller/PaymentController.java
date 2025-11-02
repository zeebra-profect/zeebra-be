package com.zeebra.domain.payment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.payment.dto.ApprovePaymentRequest;
import com.zeebra.domain.payment.dto.ApprovePaymentResponse;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.service.PaymentService;
import com.zeebra.domain.payment.service.TossPaymentService;
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
	private final TossPaymentService tossPaymentService;

	@Operation(summary = "결제 정보 생성 API", description = "orderId를 통해서 결제 정보를 생성합니다. 토스로 결제 요청을 보내기 전에 DB에 결제 정보를 저장하는 역할을 합니다.")
	@PostMapping()
	public ApiResponse<CreatePaymentResponse> createPayment(@RequestBody @Valid CreatePaymentRequest request, @AuthenticationPrincipal
		JwtProvider.JwtUserPrincipal principal) {

		Long memberId = principal.getMemberId();

		CreatePaymentResponse response = paymentService.createPayment(request, memberId);

		return ApiResponse.success(response);
	}

	@Operation(summary = "결제 성공 처리 API", description = "결제창을 통한 결제가 성공하면 그 결과를 처리하고, 토스에 최종 결제 승인을 요청합니다.")
	@PostMapping("/approve")
	public ApiResponse<ApprovePaymentResponse> approvePayment(@RequestBody @Valid ApprovePaymentRequest request, @AuthenticationPrincipal
		JwtProvider.JwtUserPrincipal principal) {
		Long memberId = principal.getMemberId();
		ApprovePaymentResponse response = paymentService.approvePayment(request, memberId);
		return ApiResponse.success(response);
	}
}