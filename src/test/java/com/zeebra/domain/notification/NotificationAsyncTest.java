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
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
public class NotificationAsyncTest {

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

    @AfterAll
    public static void afterAllTruncate() throws Exception {
        try (Connection conn = staticDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE notification, order_history, orders, members RESTART IDENTITY CASCADE");
        }
    }

    @Autowired
    public void setDataSource(DataSource ds) {
        staticDataSource = ds;  // static에 수동 할당
    }

    @BeforeEach
    public void truncate() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE notification, order_history, orders, members RESTART IDENTITY CASCADE");
        }
        orderHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        memberRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-UT-NOTI-ASYNC-001-[정상] 여러 알림 비동기 동시 생성")
    public void createNotification_async_multipleNotifications_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        System.out.println("생성된 멤버 개수: " + members.size());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        System.out.println("생성된 요청 개수: " + reqs.size());  // 디버깅용

        // when
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);

        // then
        // 1. 모든 비동기 작업이 완료될 때까지 대기
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> responses.stream().allMatch(CompletableFuture::isDone));

        // 2. DB에 저장 완료 확인
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> notificationRepository.count() == testnum.intValue());

        assertThat(notificationRepository.count()).isEqualTo(testnum.intValue());
        long duration = System.currentTimeMillis() - startTime;
        assertThat(duration).isLessThan(5000);  // 진짜 5초 미만인지 체크
        System.out.println("duration: " + duration);
    }

    @Test
    @DisplayName("TC-UT-NOTI-ASYNC-002-[예외] 비동기 내부 예외가 메인 흐름에 영향 없음")
    public void createNotification_async_mainFlowNotAffected_fail() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<Order> orders = createOrders(members);
        List<NotificationRequest> reqs = createNotificationRequests(testnum + 1, testnum * 2, null, orders);

        // when
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);

        // then
        // 주문은 정상적으로 DB에 남아 있어야 함
        await().atMost(10, TimeUnit.SECONDS).until(() -> orderRepository.count() == testnum.intValue());
        assertThat(orders.size()).isEqualTo(testnum.intValue());

        // 알림 생성에서 발생한 예외 검증
        assertThatThrownBy(() ->
                CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join()
        )
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("해당하는 사용자가 없습니다.");
    }

    @Test
    @DisplayName("TC-UT-NOTI-READ-001-[정상] 알림 읽음처리 시 isRead가 true로 변경")
    public void readNotification_async_multipleNotifications_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);

        // when
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> notifications = readNotifications(members, responses);
        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> notifications.stream().allMatch(CompletableFuture::isDone));

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("비동기 읽음 처리 시간: " + duration + "ms");

        // then
        long startTime2 = System.currentTimeMillis();
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> notificationRepository.findAll().stream()
                        .allMatch(Notification::isRead)
                );
        assertThat(notificationRepository.findAll())
                .extracting(notifications1 -> notifications1.stream().allMatch(Notification::isRead)).isEqualTo(true);

        assertThat(duration).isLessThan(20000);  // 10초 이내 완료
        long duration2 = System.currentTimeMillis() - startTime2;
        System.out.println("비동기 읽음 처리 시간2: " + duration2 + "ms");
    }

    @Test
    @DisplayName("TC-UT-NOTI-READ-002-[예외] 존재하지 않는 알림 읽음처리 요청")
    public void readNotification_async_nonExistNotification_fail() {
        // given
        Member member = createTestMember("user1", "user1@a.b");

        // when & then
        CompletableFuture<Void> future =
                notificationService.readNotification(member.getId(), 999999L);

        assertThatThrownBy(() -> future.join()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NoSuchElementException.class).cause().hasMessage("해당하는 알림이 없습니다.");
    }

    @Test
    @DisplayName("TC-UT-NOTI-READ-003-[정상] 이미 읽음처리된 알림을 또 읽음처리 요청")
    public void readNotification_async_ReadAgain_success() {
        // given
        Long testnum = 1000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);
        List<CompletableFuture<Void>> firstReadFutures = readNotifications(members, responses);

        // when
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> secondReadFutures = readNotifications(members, responses);

        // then
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> secondReadFutures.stream().allMatch(CompletableFuture::isDone));

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("재처리 시간: " + duration + "ms");
        assertThat(notificationRepository.findAll().stream().allMatch(Notification::isRead)).isTrue();
        assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("TC-UT-NOTI-READ-004-[예외] 접근권한 없는 알림 읽음처리 요청")
    public void readNotification_async_unauthorized_fail() {
        // given
        Long testnum = 1000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<Member> unauthorizedMembers = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);

        // when
        List<CompletableFuture<Void>> futures = readNotifications(unauthorizedMembers, responses);

        // then
        //        assertThatThrownBy(() -> futures.get(0).join()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(AccessDeniedException.class).cause().hasMessage("권한이 없습니다.");
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> futures.get(0).join())
                            .isInstanceOf(CompletionException.class)
                            .hasCauseInstanceOf(AccessDeniedException.class)
                            .cause()
                            .hasMessage("권한이 없습니다.");
                });
    }

    @Test
    @DisplayName("TC-UT-NOTI-READ-005-[예외] null notificationId로 읽음처리 요청")
    public void readNotification_async_null_notificationId_fail() {
        // given
        Long testnum = 1000L;
        List<Member> members = createTestMembers(testnum.intValue());

        // when
        List<CompletableFuture<Void>> futures = readNotifications(members, null);

        // then
//        assertThatThrownBy(() -> futures.get(0).join()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class).cause().hasMessage("알림 id가 null입니다.");
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> futures.get(0).join())
                            .isInstanceOf(CompletionException.class)
                            .hasCauseInstanceOf(NullPointerException.class)
                            .cause()
                            .hasMessage("알림 id가 null입니다.");
                });
    }

    @Test
    @DisplayName("TC-UT-NOTI-DELETE-001-[정상] 알림 삭제처리 시 삭제됨")
    public void deleteNotification_async_delete_success() {
        // given
        Long testnum = 10000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);

        // when
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> futures = deleteNotifications(members, responses);

        // then
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> futures.stream().allMatch(CompletableFuture::isDone));

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("재처리 시간: " + duration + "ms");
        assertThat(notificationRepository.findAll().size()).isEqualTo(0);
        assertThat(duration).isLessThan(10000);

    }

    @Test
    @DisplayName("TC-UT-NOTI-DELETE-002-[정상] 존재하지 않는 알림 삭제 요청")
    public void deleteNotification_async_deleteNonExistNotification_fail() {
        // given
        Member member = createTestMember("user1", "user1@a.b");

        // when & then
        CompletableFuture<Void> future =
                notificationService.deleteNotification(member.getId(), 999999L);

        assertThatThrownBy(() -> future.join()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NoSuchElementException.class).cause().hasMessage("해당하는 알림이 없습니다.");
    }

    @Test
    @DisplayName("TC-UT-NOTI-DELETE-003-[예외] 접근권한 없는 알림 삭제 요청")
    public void deleteNotification_async_unauthorized_fail() {
        // given
        Long testnum = 1000L;
        List<Member> members = createTestMembers(testnum.intValue());
        List<Member> unauthorizedMembers = createTestMembers(testnum.intValue());
        List<NotificationRequest> reqs = createNotificationRequests(1L, testnum, members, null);
        List<CompletableFuture<NotificationResponse>> responses = createNotificationResponses(reqs);

        // when
        List<CompletableFuture<Void>> futures = deleteNotifications(unauthorizedMembers, responses);

        // then
        //        assertThatThrownBy(() -> futures.get(0).join()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(AccessDeniedException.class).cause().hasMessage("권한이 없습니다.");
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> futures.get(0).join())
                            .isInstanceOf(CompletionException.class)
                            .hasCauseInstanceOf(AccessDeniedException.class)
                            .cause()
                            .hasMessage("권한이 없습니다.");
                });
    }

    @Test
    @DisplayName("TC-UT-NOTI-DELETE-004-[예외] null notificationId로 삭제 요청")
    public void deleteNotification_async_null_notificationId_fail() {
        // given
        Long testnum = 1000L;
        List<Member> members = createTestMembers(testnum.intValue());

        // when
        List<CompletableFuture<Void>> futures = deleteNotifications(members, null);

        // then
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> futures.get(0).join())
                            .isInstanceOf(CompletionException.class)
                            .hasCauseInstanceOf(NullPointerException.class)
                            .cause()
                            .hasMessage("알림 id가 null입니다.");
                });
    }

    // 헬퍼 메서드
    private Member createTestMember(String loginId, String email) {
        return memberRepository.save(Member.builder().userLoginId(loginId).memberName(loginId).memberEmail(email).nickname("testMember").birth(LocalDate.now()).gender(Gender.WOMAN).passwordHash("hashedPassword").role(Role.USER).build());
    }

    private List<Member> createTestMembers(int num) {
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= num; i++) {
            Member member = memberRepository.save(Member.builder().userLoginId("user" + i).memberName("user" + i).memberEmail("user" + i + "@a.b").nickname("testMember" + i).birth(LocalDate.now()).gender(Gender.WOMAN).passwordHash("hashedPassword").role(Role.USER).build());
            members.add(member);
        }
        return members;
    }

    private List<CompletableFuture<NotificationResponse>> createNotificationResponses(List<NotificationRequest> notificationRequests) {
        List<CompletableFuture<NotificationResponse>> notis = new ArrayList<>();

        for (NotificationRequest notificationRequest : notificationRequests) {
            CompletableFuture<NotificationResponse> noti = notificationService.createNotificationAsync(notificationRequest);
            notis.add(noti);
        }
        return notis;
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

            NotificationRequest req = new NotificationRequest(memberId, type, order);
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
//            System.out.println("orderSaved!: " + savedOrder.getId());
        }
        return orders;
    }

    private List<CompletableFuture<Void>> readNotifications(List<Member> members, List<CompletableFuture<NotificationResponse>> notifications) {
        // notifications가 있는 경우
        if (notifications != null && !notifications.isEmpty()) {
            CompletableFuture.allOf(notifications.toArray(new CompletableFuture[0])).join();

            return IntStream.range(0, notifications.size())
                    .mapToObj(i -> {
                        NotificationResponse response = notifications.get(i).join();
                        Long memberId = (members != null && i < members.size())
                                ? members.get(i).getId()
                                : response.memberId();
                        return notificationService.readNotification(memberId, response.notificationId());
                    })
                    .collect(Collectors.toList());
        }

        // notifications가 null이고 members만 있는 경우
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(member -> notificationService.readNotification(member.getId(), null))
                    .collect(Collectors.toList());
        }

        // 둘 다 없으면 빈 리스트 반환
        return Collections.emptyList();
    }

    public List<CompletableFuture<Void>> deleteNotifications(List<Member> members, List<CompletableFuture<NotificationResponse>> notifications
    ) {
        // notifications가 있는 경우
        if (notifications != null && !notifications.isEmpty()) {
            CompletableFuture.allOf(notifications.toArray(new CompletableFuture[0])).join();

            return IntStream.range(0, notifications.size())
                    .mapToObj(i -> {
                        NotificationResponse response = notifications.get(i).join();
                        Long memberId = (members != null && i < members.size())
                                ? members.get(i).getId()
                                : response.memberId();
                        return notificationService.deleteNotification(memberId, response.notificationId());
                    })
                    .collect(Collectors.toList());
        }

        // notifications가 null이고 members만 있는 경우
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(member -> notificationService.deleteNotification(member.getId(), null))
                    .collect(Collectors.toList());
        }

        // 둘 다 없으면 빈 리스트 반환
        return Collections.emptyList();
    }
}
