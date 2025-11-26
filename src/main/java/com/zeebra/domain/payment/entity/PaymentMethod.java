package com.zeebra.domain.payment.entity;

import java.util.HashMap;
import java.util.Map;

public enum PaymentMethod {
    CARD("카드"),
    EASY_PAY("간편결제"),
    VIRTUAL_ACCOUNT("가상계좌"),
    MOBILE_PHONE("휴대폰"),
    TRANSFER("계좌이체"),
    CULTURE_GIFT_CERTIFICATE("문화상품권"),
    BOOK_GIFT_CERTIFICATE("도서문화상품권"),
    GAME_GIFT_CERTIFICATE("게임문화상품권");

	private static final Map<String, PaymentMethod> englishMap =
		new HashMap<>();
	private static final Map<String, PaymentMethod> koreanMap =
		new HashMap<>();

	static {
		for (PaymentMethod paymentMethod : values()) {
			englishMap.put(paymentMethod.name().toUpperCase(), paymentMethod);
			koreanMap.put(paymentMethod.korean, paymentMethod);
		}
	}

	private final String korean;

	PaymentMethod(String korean) {
		this.korean = korean;
	}

	public static PaymentMethod of(String value) {
		if(value == null){
			throw new IllegalArgumentException("결제 수단을 알 수 없습니다.");
		}

		PaymentMethod paymentMethod = englishMap.get(value.toUpperCase());
		if(paymentMethod != null){ return paymentMethod; }

		paymentMethod = koreanMap.get(value);
		if(paymentMethod != null){ return paymentMethod; }

		throw new IllegalArgumentException("알 수 없는 결제수단입니다: " + value);
	}
}