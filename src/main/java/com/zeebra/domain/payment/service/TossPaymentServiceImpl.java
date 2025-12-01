package com.zeebra.domain.payment.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.payment.dto.TossApprovalResponse;
import com.zeebra.domain.payment.dto.TossError;
import com.zeebra.domain.payment.dto.TossPayment;
import com.zeebra.global.ErrorCode.PaymentErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentServiceImpl implements TossPaymentService {

	private static final String TOSS_PAYMENTS_BASE_URL = "https://api.tosspayments.com/v1/payments";
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	@Value("${toss.payments.secret-key}")
	private String tossSecretKey;

	@Override
	public TossApprovalResponse approve(String paymentKey, String tossOrderId, BigDecimal amount) {
		String url = TOSS_PAYMENTS_BASE_URL + "/confirm";

		try {

			// 요청 헤더 생성
			HttpHeaders headers = createHeaders();

			// 요청 바디 생성
			Map<String, Object> requestBody = createRequestBody(paymentKey, tossOrderId, amount);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			// API 호출
			ResponseEntity<TossPayment> response = restTemplate.exchange(
				url,
				HttpMethod.POST,
				request,
				TossPayment.class
			);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.info("[토스 API 성공] paymentKey: {}, status: {}", paymentKey, response.getBody().status());
				return TossApprovalResponse.ofSuccess(response.getBody());
			}

			log.warn("[토스 API 예상치 못한 응답] statusCode: {}", response.getStatusCode());
			throw new BusinessException(PaymentErrorCode.TOSS_API_ERROR);

		} catch (HttpClientErrorException e) {
			log.error("[토스 API 실패] statusCode: {}, responseBody: {}", 
				e.getStatusCode(), e.getResponseBodyAsString(), e);
			return handleTossApiError(e);
		} catch (Exception e) {
			log.error("[토스 API 호출 중 예외 발생] error: {}", e.getMessage(), e);
			throw new BusinessException(PaymentErrorCode.TOSS_API_ERROR);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String encodedSecretKey = Base64.getEncoder()
			.encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
		headers.set("Authorization", "Basic " + encodedSecretKey);

		return headers;
	}

	private Map<String, Object> createRequestBody(String paymentKey, String tossOrderId, BigDecimal amount) {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", paymentKey);
		requestBody.put("orderId", tossOrderId);
		requestBody.put("amount", amount);
		return requestBody;
	}

	/**
	 * 토스 API 에러 응답 처리
	 */
	private TossApprovalResponse handleTossApiError(HttpClientErrorException e) {
		try {
			String responseBody = e.getResponseBodyAsString();
			TossError tossError = objectMapper.readValue(responseBody, TossError.class);

			log.error("[토스 API 에러 파싱 완료] code: {}, message: {}",
				tossError.code(), tossError.message());

			return TossApprovalResponse.ofFail(tossError);
		} catch (Exception parseException) {
			log.error("[토스 API 에러 파싱 실패] error: {}", parseException.getMessage(), parseException);
			
			// 파싱 실패 시 기본 에러 응답 생성
			TossError error = new TossError("PARSE_ERROR", e.getMessage());
			return TossApprovalResponse.ofFail(error);
		}
	}
}