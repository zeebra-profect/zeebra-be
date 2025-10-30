package com.zeebra.domain.notification.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public enum NotificationType {
    SIGN_UP("회원가입을 축하합니다!"),
    PAYMENT_FAILED("결제에 실패했습니다. 다시 결제를 시도해 주세요."), // 결제 실패
    ORDER_CONFIRMED("주문이 완료되었습니다. 주문번호:"), // 주문 확인
    ORDER_SHIPPED("주문하신 상품 배송이 시작되었습니다."), // 배송 시작
    ORDER_DELIVERED("주문하신 상품 배송이 완료되었습니다."), // 배송 완료. 리뷰 작성 관련 알림
    WISHLIST_RESTOCK("상품이 재입고 되었어요!"), // 관심상품 입고
    WISHLIST_LOW_STOCK("상품의 재고가 얼마 남지 않았어요!"), // 관심상품 품절 임박
    REVIEW_REQUEST("님, 상품 후기를 남겨주세요!"), // 리뷰 요청
    NEW_CHAT("새로운 채팅방이 열렸습니다!"), // 새로운 채팅
    NEW_MESSAGE("아직 읽지 않은 메시지가 있습니다." + LocalDateTime.now()),
    TEST_LOGIN("로그인 알림"); // 기존 채팅의 새 메시지

    private final String noticeText;

    NotificationType(String noticeText) {
        this.noticeText = noticeText;
    }
}
