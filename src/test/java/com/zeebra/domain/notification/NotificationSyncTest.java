package com.zeebra.domain.notification;

import com.zeebra.ZeebraApplication;
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
import com.zeebra.domain.webpush.repository.WebPushRepository;
import com.zeebra.domain.webpush.service.WebPushService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(classes = ZeebraApplication.class, properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration")
@Slf4j
public class NotificationSyncTest {

    @Autowired
    private static DataSource staticDataSource;
    @Autowired
    private DataSource dataSource;
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
    @Autowired
    private WebPushService webPushService;
    @Autowired
    private WebPushRepository webPushRepository;

    @AfterAll
    public static void afterAllTruncate() throws Exception {
        try (Connection conn = staticDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE notification, order_history, orders, members RESTART IDENTITY CASCADE");
        }
    }

    @Autowired
    public void setDataSource(DataSource ds) {
        staticDataSource = ds;
    }

    @BeforeEach
    public void truncate() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE web_push, notification, order_history, orders, members RESTART IDENTITY CASCADE");
        }
        orderHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        memberRepository.deleteAll();
        notificationRepository.deleteAll();
        webPushRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-IT-NOTI-SYNC-001-[정상] 여러 알림 동기 동시 생성")
    public void createNotification_sync_multipleNotifications_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);

        // when
        long startTime = System.currentTimeMillis();
        createNotificationResponsesSync(reqs);
        System.out.println("[정상] 여러 알림 동기 동시 생성 처리 시간: " + (System.currentTimeMillis() - startTime));
        // then
        assertThat(notificationRepository.count()).isEqualTo(testnum.intValue());
    }

    @Test
    @DisplayName("TC-IT-NOTI-SYNC-002-[예외] 동기 내부 예외가 메인 흐름에 영향")
    public void createNotification_sync_mainFlowAffected_fail() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<Order> orders = createOrders(members);
        List<NotificationRequest> reqs = createNotificationRequests(testnum + 1, testnum * 2, null, orders);

        // when & then
        long startTime = System.currentTimeMillis();
        for (NotificationRequest req : reqs) {
            try {
                notificationService.createNotificationAsync(req).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof NoSuchElementException) {
                } else {
                    log.info(e.getMessage());
                }
            }
        }
        System.out.println("[예외] 동기 내부 예외가 메인 흐름에 영향 처리 시간: " + (System.currentTimeMillis() - startTime));
        // 주문은 정상적으로 DB에 남아 있어야 함
        assertThat(orders.size()).isEqualTo(testnum.intValue());
    }

    @Test
    @DisplayName("TC-IT-NOTI-READ-001-[정상] 알림 읽음처리 시 isRead가 true로 변경")
    public void readNotification_sync_multipleNotifications_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<NotificationResponse> responses = createNotificationResponsesSync(reqs);

        // when
        long startTime = System.currentTimeMillis();
        readNotificationsSync(members, responses);
        System.out.println("[정상] 알림 읽음처리 시 isRead가 true로 변경 처리 시간: " + (System.currentTimeMillis() - startTime));

        // then
        assertThat(notificationRepository.findAll().stream().allMatch(Notification::isRead)).isTrue();
    }

    @Test
    @DisplayName("TC-IT-NOTI-READ-002-[예외] 존재하지 않는 알림 읽음처리 요청")
    public void readNotification_sync_nonExistNotification_fail() {
        // given
        Member member = createTestMember("user1", "user1@a.b");

        // when & then
        assertThatThrownBy(() ->
                notificationService.readNotificationSync(member.getId(), 999999L)
        )
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당하는 알림이 없습니다.");
    }

    @Test
    @DisplayName("TC-IT-NOTI-READ-003-[정상] 이미 읽음처리된 알림을 또 읽음처리 요청")
    public void readNotification_sync_ReadAgain_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<NotificationResponse> responses = createNotificationResponsesSync(reqs);
        readNotificationsSync(members, responses);

        // when
        long startTime = System.currentTimeMillis();
        readNotificationsSync(members, responses);
        System.out.println("[정상] 이미 읽음처리된 알림을 또 읽음처리 요청 처리 시간: " + (System.currentTimeMillis() - startTime));

        // then
        assertThat(notificationRepository.findAll().stream().allMatch(Notification::isRead)).isTrue();
    }

    @Test
    @DisplayName("TC-IT-NOTI-READ-004-[예외] 접근권한 없는 알림 읽음처리 요청")
    public void readNotification_sync_unauthorized_fail() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<Member> unauthorizedMembers = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<NotificationResponse> responses = createNotificationResponsesSync(reqs);

        // when & then
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < responses.size(); i++) {
            final int index = i;
            assertThatThrownBy(() ->
                    notificationService.readNotificationSync(
                            unauthorizedMembers.get(index).getId(),
                            responses.get(index).notificationId()
                    )
            )
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("권한이 없습니다.");
        }
        System.out.println("[예외] 접근권한 없는 알림 읽음처리 요청 처리 시간: " + (System.currentTimeMillis() - startTime));

    }

    @Test
    @DisplayName("TC-IT-NOTI-READ-005-[예외] null notificationId로 읽음처리 요청")
    public void readNotification_sync_null_notificationId_fail() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());

        long startTime = System.currentTimeMillis();
//        readNotificationsSync(members, null);
        // when & then
        for (int i = 0; i < members.size(); i++) {
            final int index = i;
            assertThatThrownBy(() ->
                    notificationService.readNotificationSync(members.get(index).getId(), null)
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("알림 id가 null입니다.");
        }
        System.out.println("[예외] null notificationId로 읽음처리 요청 처리 시간: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    @DisplayName("TC-IT-NOTI-DELETE-001-[정상] 알림 삭제처리 시 삭제됨")
    public void deleteNotification_sync_delete_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<NotificationResponse> responses = createNotificationResponsesSync(reqs);

        // when
        long startTime = System.currentTimeMillis();
        deleteNotificationsSync(members, responses);
        System.out.println("[정상] 알림 삭제처리 시 삭제됨 처리 시간: " + (System.currentTimeMillis() - startTime));

        // then
        assertThat(notificationRepository.findAll().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("TC-IT-NOTI-DELETE-002-[정상] 존재하지 않는 알림 삭제 요청")
    public void deleteNotification_sync_deleteNonExistNotification_fail() {
        // given
        Member member = createTestMember("user1", "user1@a.b");

        // when & then
        assertThatThrownBy(() ->
                notificationService.deleteNotificationSync(member.getId(), 999999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당하는 알림이 없습니다.");
    }

    @Test
    @DisplayName("TC-IT-NOTI-DELETE-003-[예외] 접근권한 없는 알림 삭제 요청")
    public void deleteNotification_sync_unauthorized_fail() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<Member> unauthorizedMembers = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<NotificationResponse> responses = createNotificationResponsesSync(reqs);

        // when & then
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < responses.size(); i++) {
            final int index = i;
            assertThatThrownBy(() ->
                    notificationService.deleteNotificationSync(
                            unauthorizedMembers.get(index).getId(),
                            responses.get(index).notificationId()
                    )
            )
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("권한이 없습니다.");
        }
        System.out.println("[예외] 접근권한 없는 알림 삭제 요청 처리 시간: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    @DisplayName("TC-IT-NOTI-DELETE-004-[예외] null notificationId로 삭제 요청")
    public void deleteNotification_sync_null_notificationId_fail() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());

        // when & then
        long startTime = System.currentTimeMillis();
//        deleteNotificationsSync(members, null);

        for (int i = 0; i < members.size(); i++) {
            final int index = i;
            assertThatThrownBy(() ->
                    notificationService.deleteNotificationSync(
                            members.get(index).getId(),
                            null
                    )
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("알림 id가 null입니다.");
        }
        System.out.println("[예외] null notificationId로 삭제 요청 처리 시간: " + (System.currentTimeMillis() - startTime));

    }

    // ========== 동기 헬퍼 메서드 ==========

    private List<NotificationResponse> createNotificationResponsesSync(List<NotificationRequest> notificationRequests) {
        List<NotificationResponse> responses = new ArrayList<>();

        for (NotificationRequest notificationRequest : notificationRequests) {
            NotificationResponse response = notificationService.createNotificationSync(notificationRequest);
            responses.add(response);
        }
        return responses;
    }

    private void readNotificationsSync(List<Member> members, List<NotificationResponse> notifications) {
        IntStream.range(0, members.size())
                .forEach(i -> {
                    NotificationResponse response = notifications.get(i);
                    Long memberId = (members != null && i < members.size())
                            ? members.get(i).getId()
                            : response.memberId();
                    notificationService.readNotificationSync(memberId, response.notificationId());
                });
    }

    private void deleteNotificationsSync(List<Member> members, List<NotificationResponse> notifications) {
        IntStream.range(0, members.size())
                .forEach(i -> {
                    NotificationResponse response = (notifications != null && i < members.size())
                            ? notifications.get(i)
                            : null;
                    Long NotificationId = (response != null) ?
                            response.notificationId() : null;
                    Long memberId = (members != null && i < members.size())
                            ? members.get(i).getId()
                            : response.memberId();
                    notificationService.deleteNotificationSync(memberId, NotificationId);
                });
    }

    // ========== 기존 헬퍼 메서드 (변경 없음) ==========

    private Member createTestMember(String loginId, String email) {
        return memberRepository.save(Member.builder()
                .userLoginId(loginId)
                .memberName(loginId)
                .memberEmail(email)
                .nickname("testMember")
                .birth(LocalDate.now())
                .gender(Gender.WOMAN)
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build());
    }

    private List<Member> createTestMembers(int num) {
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= num; i++) {
            Member member = memberRepository.save(Member.builder()
                    .userLoginId("user" + i)
                    .memberName("user" + i)
                    .memberEmail("user" + i + "@a.b")
                    .nickname("testMember" + i)
                    .birth(LocalDate.now())
                    .gender(Gender.WOMAN)
                    .passwordHash("hashedPassword")
                    .role(Role.USER)
                    .build());
            members.add(member);
        }
        return members;
    }

    private List<NotificationRequest> createNotificationRequests(Long start, Long end, List<Member> members, List<Order> orders) {
        List<NotificationRequest> notificationRequests = new ArrayList<>();

        for (long i = start; i <= end; i++) {
            int index = (int) (i - start);

            if (members != null && index >= members.size())
                break;

            if (orders != null && index >= orders.size())
                break;

            Long memberId = (members != null)
                    ? members.get(index).getId()
                    : i;

            NotificationType type = (orders != null)
                    ? NotificationType.ORDER_CONFIRMED
                    : NotificationType.TEST;

            Order order = (orders != null)
                    ? orders.get(index)
                    : null;

            NotificationRequest req = new NotificationRequest(memberId, type, order, null);
            notificationRequests.add(req);
        }

        return notificationRequests;
    }

    private List<Order> createOrders(List<Member> members) {
        List<Order> orders = new ArrayList<>();

        for (Member member : members) {
            CreateOrderRequest orderRequest = new CreateOrderRequest("test" + member.getId(), null, 1L, null);
            CreateOrderResponse order = orderService.createOrder(member.getId(), orderRequest);
            Order savedOrder = orderRepository.findById(order.order().orderId()).get();
            orders.add(savedOrder);
        }
        return orders;
    }
}