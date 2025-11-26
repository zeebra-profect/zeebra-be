//package com.zeebra.domain.webpush;
//
//import com.zeebra.ZeebraApplication;
//import com.zeebra.domain.member.entity.Gender;
//import com.zeebra.domain.member.entity.Member;
//import com.zeebra.domain.member.entity.Role;
//import com.zeebra.domain.member.repository.MemberRepository;
//import com.zeebra.domain.notification.repository.NotificationRepository;
//import com.zeebra.domain.order.repository.OrderHistoryRepository;
//import com.zeebra.domain.order.repository.OrderRepository;
//import com.zeebra.domain.webpush.dto.WebPushRequest;
//import com.zeebra.domain.webpush.repository.WebPushRepository;
//import com.zeebra.domain.webpush.service.WebPushService;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.scheduling.annotation.EnableAsync;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.Statement;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//import static java.util.concurrent.TimeUnit.SECONDS;
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.awaitility.Awaitility.await;
//
//@SpringBootTest(classes = ZeebraApplication.class, properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration")
//@EnableAsync
//public class WebPushTest {
//
//    @Autowired
//    private static DataSource staticDataSource;
//    @Autowired
//    private DataSource dataSource;
//    @Autowired
//    private MemberRepository memberRepository;
//    @Autowired
//    private NotificationRepository notificationRepository;
//    @Autowired
//    private OrderRepository orderRepository;
//    @Autowired
//    private OrderHistoryRepository orderHistoryRepository;
//    @Autowired
//    private WebPushService webPushService;
//    @Autowired
//    private WebPushRepository webPushRepository;
//
//    @AfterAll
//    public static void afterAllTruncate() throws Exception {
//        try (Connection conn = staticDataSource.getConnection();
//             Statement stmt = conn.createStatement()) {
//            stmt.execute("TRUNCATE TABLE notification, order_history, orders, members RESTART IDENTITY CASCADE");
//        }
//    }
//
//    @Autowired
//    public void setDataSource(DataSource ds) {
//        staticDataSource = ds;  // static에 수동 할당
//    }
//
//
//    @BeforeEach
//    public void truncate() throws Exception {
//        try (Connection conn = dataSource.getConnection();
//             Statement stmt = conn.createStatement()) {
//            stmt.execute("TRUNCATE TABLE web_push, notification, order_history, orders, members RESTART IDENTITY CASCADE");
//        }
//        orderHistoryRepository.deleteAll();
//        orderRepository.deleteAll();
//        memberRepository.deleteAll();
//        notificationRepository.deleteAll();
//        webPushRepository.deleteAll();
//    }
//
//    @Test
//    @DisplayName("TC-IT-WP-ASYNC-001-[정상] 여러 유저에게 비동기 푸시 발송")
//    public void sendPush_async_multipleNotifications_success() {
//        // given
//        Long testnum = 10L;
//        List<Member> members = createTestMembers(testnum.intValue());
//        subscriptions(members);
//
//        // when
//        List<Boolean> result = send(members);
//
//        // then
//        await().atMost(5, SECONDS)
//                .until(() -> result.stream().allMatch(Boolean::booleanValue));
//
//        assertThat(result.stream().allMatch(Boolean::booleanValue)).isEqualTo(true);
//    }
//
//    // 헬퍼 메서드
//    private List<Member> createTestMembers(int num) {
//        List<Member> members = new ArrayList<>();
//        for (int i = 1; i <= num; i++) {
//            Member member = memberRepository.save(Member.builder().userLoginId("user" + i).memberName("user" + i).memberEmail("user" + i + "@a.b").nickname("testMember" + i).birth(LocalDate.now()).gender(Gender.WOMAN).passwordHash("hashedPassword").role(Role.USER).build());
//            members.add(member);
//        }
//        return members;
//    }
//
//    private List<String> subscriptions(List<Member> members) {
//        List<String> subscriptions = new ArrayList<>();
//        for (Member member : members) {
//            WebPushRequest request = new WebPushRequest();
//            request.setAuth("fsMy2rhJlKtGpuFih7iR/g==");
//            request.setDeviceInfo("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
//            request.setEndpoint("https://fcm.googleapis.com/fcm/send/f8lWS7F9uM8:APA91bF9_AodpZkh6Hg7bcB_Bue1OHXJpZ_PdddO4bmvNPnT41Ad99GWypp4NH3f_KgJfda9t1P8uJJAaf_2kjNTI4I5Hm2DkMEmMr6x9pMqEPviqwL-NmIsUJ95wwBsnURIh3X5cXFp");
//            request.setP256dh("BHYhVqFHTSThexb2FLVVErQlzW7/6/9w8+5JloXPVzOzhD4ME3/dfX/aruNb8HdtNBPkq+WltWH7Jr/29nF6gL8=");
//            //            request.setAuth("test");
//            //            request.setDeviceInfo("test");
//            //            request.setEndpoint("test");
//            //            request.setP256dh("test");
//            String subscription = webPushService.saveSubscription(member.getId(), request);
//
//            subscriptions.add(subscription);
//            System.out.println("subscription 추가!: " + subscription);
//        }
//        return subscriptions;
//    }
//
//    private List<String> unsubscriptions(List<Member> members, List<String> subscriptions) {
//        List<String> unsubscriptions = new ArrayList<>();
//        for (Member member : members) {
//            String unsubscription = webPushService.deleteSubscription(member.getId());
//            unsubscriptions.add(unsubscription);
//            System.out.println("unsubscription 추가!: " + unsubscription);
//        }
//        return unsubscriptions;
//    }
//
//    private List<Boolean> send(List<Member> members) {
//        List<Boolean> results = new ArrayList<>();
//        for (Member member : members) {
//            CompletableFuture<Boolean> result = webPushService.sendPush(member.getId(), "TEST", "WebPushTest");
//            System.out.println("send 보냄!: " + result);
//            results.add(result.join());
//        }
//
//        return results;
//    }
//
//
//}
