package com.zeebra.domain.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationType {
    SIGN_UP("회원가입 축하메시지", "{nickname}님, 회원가입을 축하합니다!"),
    LOGIN("로그인 알림", "{nickName}님, 어서오세요!"),
    PAYMENT_FAILED("결제 실패", "결제에 실패했습니다. 다시 결제를 시도해 주세요. 주문번호:{orderName}"), // 결제 실패
    ORDER_CONFIRMED("주문 완료", "주문이 완료되었습니다. 주문번호:{orderName}"), // 주문 확인
    ORDER_SHIPPED("배송 시작", "주문하신 상품 배송이 시작되었습니다. 주문번호:{orderName}"), // 배송 시작
    ORDER_DELIVERED("배송 완료", "주문하신 상품 배송이 완료되었습니다. 주문번호:{orderName}"), // 배송 완료. 리뷰 작성 관련 알림
    WISHLIST_RESTOCK("상품 재입고", "상품이 재입고 되었어요! 상품명:{productName}"), // 관심상품 입고
    WISHLIST_LOW_STOCK("상품 품절 임박", "상품의 재고가 얼마 남지 않았어요! 상품명:{productName}"), // 관심상품 품절 임박
    REVIEW_REQUEST("리뷰 요청", "상품 후기를 남겨주세요! 주문번호:{orderName}"), // 리뷰 요청
    NEW_CHAT("새로운 채팅방", "새로운 채팅방이 열렸습니다! 상품명:{productName}"), // 새로운 채팅
    NEW_MESSAGE("새로운 메시지", "아직 읽지 않은 메시지가 있습니다. 상품명:{productName}"),
    TEST("테스트1", "테스트용 타입. {nickname}"),
    TEST_OBJECT("테스트2", "테스트용 타입_오브젝트 있음");

    private String notificationTitle;
    private String noticeBasicText;


    NotificationType(String noticeText, String notificationTitle) {
        this.noticeBasicText = noticeText;
        this.notificationTitle = notificationTitle;
    }

    public void createMessage(String str) {

        String result = null;

        switch (this) {
            case SIGN_UP:
                result = this.noticeBasicText.replace("{nickname}", str);
                break;
            case WISHLIST_RESTOCK, WISHLIST_LOW_STOCK, NEW_MESSAGE, NEW_CHAT:
                result = this.noticeBasicText.replace("{productName}", str);
                break;
            case ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED, REVIEW_REQUEST:
                result = this.noticeBasicText.replace("{orderName}", str);
                break;
            case TEST:
                result = this.noticeBasicText.replace("{nickname}", str);
                break;
        }
        this.noticeBasicText = result;
        System.out.println("myText: " + this.noticeBasicText);
        System.out.println("result: " + result);
    }


}
