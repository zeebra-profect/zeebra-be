package com.zeebra.domain.notification;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.repository.NotificationRepository;
import com.zeebra.domain.notification.service.NotificationService;
import com.zeebra.domain.order.dto.CreateOrderRequest;
import com.zeebra.domain.order.dto.CreateOrderResponse;
import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.repository.OrderHistoryRepository;
import com.zeebra.domain.order.repository.OrderRepository;
import com.zeebra.domain.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
public class NotificationAsyncTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;


    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @BeforeEach
    public void truncate() {
        memberRepository.deleteAll();
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        orderHistoryRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-UT-NOTI-ASYNC-001-[정상] 여러 알림 비동기 동시 생성")
    public void createNotification_async_multipleNotifications_success() {
        // given
        Member member1 = createTestMember("user1", "user1@abc.a");
        Member member2 = createTestMember("user2", "user2@abc.a");
        Member member3 = createTestMember("user3", "user3@abc.a");
        NotificationRequest req1 = new NotificationRequest(member1.getId(), NotificationType.TEST, null);
        NotificationRequest req2 = new NotificationRequest(member2.getId(), NotificationType.TEST, null);
        NotificationRequest req3 = new NotificationRequest(member3.getId(), NotificationType.TEST, null);

        // when
        CompletableFuture<NotificationResponse> f1 = notificationService.createNotificationAsync(req1);
        CompletableFuture<NotificationResponse> f2 = notificationService.createNotificationAsync(req2);
        CompletableFuture<NotificationResponse> f3 = notificationService.createNotificationAsync(req3);

        CompletableFuture.allOf(f1, f2, f3).join();
        // theㄴ
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> notificationRepository.count() == 3);
        List<Notification> list = notificationRepository.findAll();
        assertThat(list.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-UT-NOTI-ASYNC-002-[예외] 비동기 내부 예외가 메인 흐름에 영향 없음")
    public void createNotification_async_mainFlowNotAffected_fail() {
        // given
        Member member1 = createTestMember("user1", "user1@abc.a");
        CreateOrderRequest orderRequest = new CreateOrderRequest("test", null, 1L, null);
        CreateOrderResponse order1 = orderService.createOrder(member1.getId(), orderRequest);
        Order savedOrder = orderRepository.findById(order1.order().orderId()).get();

        // 단 여기서 이상한 req를 하나 준비. 실패 유도
        NotificationRequest req1 = new NotificationRequest(99999L, NotificationType.ORDER_CONFIRMED, savedOrder);

        // when
        CompletableFuture<NotificationResponse> f1 = notificationService.createNotificationAsync(req1);

        // then
        // 주문은 정상적으로 DB에 남아 있어야 함
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> orderRepository.findById(order1.order().orderId()).isPresent());
        assertThat(savedOrder.getMemberId()).isEqualTo(member1.getId());

        // 알림 생성에서 발생한 예외는 CompletableFuture에서 잡을 수 있음
        assertThatThrownBy(() -> f1.join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("해당하는 사용자가 없습니다.");
    }

    // 헬퍼 메서드
    private Member createTestMember(String loginId, String email) {
        return memberRepository.save(Member.builder().userLoginId(loginId).memberName(loginId).memberEmail(email).nickname("testMember").birth(LocalDate.now()).gender(Gender.WOMAN).passwordHash("hashedPassword").role(Role.USER).build());
    }
}
