package com.zeebra.domain.chat.controller;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zeebra.ZeebraApplication;
import com.zeebra.domain.chat.dto.ChatMessageRequestDto;
import com.zeebra.domain.chat.dto.ChatMessageResponseDto;
import com.zeebra.domain.chat.entity.ChatRoom;
import com.zeebra.domain.chat.entity.ChatRoomMember;
import com.zeebra.domain.chat.entity.ChatRoomType;
import com.zeebra.domain.chat.entity.MessageType;
import com.zeebra.domain.chat.repository.ChatRoomMemberRepository;
import com.zeebra.domain.chat.repository.ChatRoomRepository;
import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.security.jwt.JwtProvider;
import com.zeebra.global.web.CookieUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@Slf4j
//@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ZeebraApplication.class)
public class ChatSocketControllerTest {

    @LocalServerPort
    private int port;
    private String URL;

    // Test Client
    private WebSocketStompClient stompClient;

    // 테스트 데이터 생성용
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    ChatRoomMemberRepository chatRoomMemberRepository;

    private Member testUser1;
    private Member testUser2;
    private String token1;
    private String token2;

    private ChatRoom testGroupRoom;
    @Autowired
    private CookieUtil cookieUtil;
    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;

    private static final String ACCESS_TOKEN_COOKIE_NAME = "__Host-AT";

    @BeforeEach
    void setUp() {
        // 테스트 유저 두명 생성 (DB에 실제 저장)
        testUser1 = Member.builder()
                .userLoginId("sender")
                .nickname("보내는 사람")
                .memberName("sender")
                .memberEmail("dd@dd.dd")
                .birth(LocalDate.now())
                .gender(Gender.MAN)
                .passwordHash("Testuser!234")
                .role(Role.USER).build();

        testUser2 = Member.builder()
                .userLoginId("receiver")
                .nickname("받는 사람")
                .memberName("receiver")
                .memberEmail("dd@dd.dd")
                .birth(LocalDate.now())
                .gender(Gender.MAN)
                .passwordHash("Testuser!234")
                .role(Role.USER).build();

        memberRepository.save(testUser1);
        memberRepository.save(testUser2);

        // 테스트용 그룹 채팅방 생성 (DB에 실제 저장)
        ChatRoom roomToSave = ChatRoom.builder()
                .productId(100L)
                .chatRoomType(ChatRoomType.GROUP)
                .build();
        testGroupRoom = chatRoomRepository.save(roomToSave);

        // 두 유저 채팅방 멤버로 저장 (DB에 실제 저장)
        ChatRoomMember member1 = ChatRoomMember.builder()
                .chatRoom(testGroupRoom).memberId(testUser1.getId()).memberName(testUser1.getNickname()).build();

        ChatRoomMember member2 = ChatRoomMember.builder()
                .chatRoom(testGroupRoom).memberId(testUser2.getId()).memberName(testUser2.getNickname()).build();

        chatRoomMemberRepository.saveAll(List.of(member1, member2));

        token1 = jwtProvider.createAccessToken(testUser1.getId(), testUser1.getUserLoginId(), "USER", 10);
        token2 = jwtProvider.createAccessToken(testUser2.getId(), testUser2.getUserLoginId(), "USER", 10);

        // SockJsClient 사용
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketClient sockJsClient = new SockJsClient(transports);
        this.stompClient = new WebSocketStompClient(sockJsClient);

        // JSON 변환기 설정
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        MappingJackson2MessageConverter conv = new MappingJackson2MessageConverter();
        conv.setObjectMapper(om);
        this.stompClient.setMessageConverter(conv);

        //엔드포인트 URL
        this.URL = "ws://localhost:" + port + "/ws/chat";

    }

    private StompHeaders createConnectHeaders(String token) {
        StompHeaders connectHeaders = new StompHeaders();
        String cookieHeader = ACCESS_TOKEN_COOKIE_NAME + "=" + token;
        connectHeaders.add("cookie", cookieHeader);
        return connectHeaders;
    }

    @Test
    @DisplayName("그룹 채팅방 메세지 전송 및 수신 (SockJS + Cookie)")
    void scenario_SendAndReceive_GroupChatMessage() throws Exception {
        //Given
        CompletableFuture<ChatMessageResponseDto> future = new CompletableFuture<>();
        CountDownLatch subscribeLatch = new CountDownLatch(1);

        // 연결 및 구독
        // 유저 2 인증 쿠키 설정, 웹소켓 연결(SockJS 사용)
        StompHeaders receiverConnectHeaders = createConnectHeaders(token2);

        StompSession receiverSession = stompClient.connectAsync(
                URL,
                new WebSocketHttpHeaders(),
                receiverConnectHeaders,
                new StompSessionHandlerAdapter() {

           @Override
           public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
               log.info("Receiver Connected Session: {}", session.getSessionId());
               //방 구독
               session.subscribe("/sub/chat/room/" + testGroupRoom.getId(), new StompFrameHandler() {
                   @Override
                   public Type getPayloadType(StompHeaders headers){
                       return ChatMessageResponseDto.class;
                   }
                   @Override
                   public void handleFrame(StompHeaders headers, Object payload) {
                       future.complete((ChatMessageResponseDto) payload); // 메세지 도착 시 future에 값 넣음
                   }
               });
               subscribeLatch.countDown();
           }
           @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
               log.error("Receiver STOMP ERROR: {}", exception.getMessage(), exception);
           }
        }).get(10, TimeUnit.SECONDS);

        // 유저 1/ 보내는 사람 - 연결 및 메세지 전송

        // 유저 1 인증 쿠키 설정
        StompHeaders senderConnectHeaders = createConnectHeaders(token1);

        // 유저 1 WebSocket 연결
        StompSession senderSession = stompClient.connectAsync(
                URL,
                new WebSocketHttpHeaders(),
                senderConnectHeaders,
                new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                log.info("Sender Connected Session: {}", session.getSessionId());
            }
            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                log.error("Sender Stomp Error: {}", exception.getMessage(), exception);
            }
        }).get(10, TimeUnit.SECONDS);

        // 유저 1이 보낼 메세지
        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setChatRoomId(testGroupRoom.getId());
        requestDto.setMessageType(MessageType.TEXT);
        requestDto.setContent("hi receiver");

        assertTrue(subscribeLatch.await(2, TimeUnit.SECONDS), "Receiver가 시간 내에 구독하지 못하였습니다.");

        //When
        senderSession.send("/pub/chat/message", requestDto);

        //Then
        ChatMessageResponseDto receivedMessage = future.get(1, TimeUnit.SECONDS);

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.content()).isEqualTo("hi receiver");
        assertThat(receivedMessage.senderName()).isEqualTo(testUser1.getNickname());
    }

    @ParameterizedTest(name = "실시간 그룹채팅 브로드캐스트 테스트")
    @ValueSource(ints = {100, 500})
    @DisplayName("N명 구독 중일 때 하나의 메세지 보내서 모두가 수신 SockJs + STOMP + Cookie")
    void broadcast_to_N_sub(int participantCount) throws Exception {
        //Given 참여자 N명 + 발신자 1명 생성
        List<Member> receivers = new ArrayList<>(participantCount);
        List<String> receiverTokens = new ArrayList<>(participantCount);
        for (int i = 0; i < participantCount; i++) {
            Member members = Member.builder()
                    .userLoginId("user" + i)
                    .nickname("유저" + i)
                    .memberName("user" + i)
                    .memberEmail("dd@dd.dd")
                    .birth(LocalDate.now())
                    .gender(Gender.MAN)
                    .passwordHash("User1234!")
                    .role(Role.USER).build();
            memberRepository.save(members);
            receivers.add(members);

            String token = jwtProvider.createAccessToken(members.getId(), members.getUserLoginId(), "USER", 10);
            receiverTokens.add(token);

            //방 멤버 등록
            ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                    .chatRoom((testGroupRoom))
                    .memberId(members.getId())
                    .memberName((members.getNickname()))
                    .build();
            chatRoomMemberRepository.save(chatRoomMember);
        }
        Member sender = Member.builder()
                .userLoginId("sender")
                .nickname("sender")
                .memberName("sender")
                .memberEmail("dd@dd.dd")
                .birth(LocalDate.now())
                .gender(Gender.MAN)
                .passwordHash("Sender1234!")
                .role(Role.USER).build();
        memberRepository.save(sender);
        String senderToken = jwtProvider.createAccessToken(sender.getId(), sender.getNickname(), "sender", 10);

        //방 멤버 등록
        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom((testGroupRoom))
                .memberId(sender.getId())
                .memberName((sender.getNickname()))
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        //모든 수신자 세션 연결
        CountDownLatch subscribeAll = new CountDownLatch(participantCount);

        // 수신 메세지 검증용
        Map<String, ChatMessageResponseDto> receivedMap = new ConcurrentHashMap<>();

        //세션 정리용 컨테이너
        List<StompSession> receiverSessions = new CopyOnWriteArrayList<>();


        //TODO: 동기/비동기/블로킹/논블로킹, 자바에서 스레드 관리 방법(커널과 연동지어서), HikariCP
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(16, participantCount));
        try{
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < participantCount; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    try {
                        StompHeaders connectHeaders = createConnectHeaders(receiverTokens.get(idx));
                        StompSession session = stompClient
                                .connectAsync(URL, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
                                    @Override
                                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                        String sessionId = session.getSessionId();
                                        session.subscribe("/sub/chat/room/" + testGroupRoom.getId(), new StompFrameHandler() {
                                            @Override public Type getPayloadType(StompHeaders headers){
                                                return ChatMessageResponseDto.class;
                                            }
                                            @Override
                                            public void handleFrame(StompHeaders headers, Object payload) {
                                                receivedMap.putIfAbsent(sessionId, (ChatMessageResponseDto) payload);
                                            }
                                        });
                                        subscribeAll.countDown();
                                    }
                                    @Override
                                    public void handleException(
                                            StompSession session, StompCommand stompCommand,
                                            StompHeaders headers, byte[] payload, Throwable exception){
                                        log.error("Receiver {}, STOMP ERROR {}", idx, exception.getMessage(), exception);
                                    }
                                }).get(10, TimeUnit.SECONDS);
                        receiverSessions.add(session);
                    }catch (Exception e){
                        log.error("Receiver {}, STOMP ERROR {}", idx, e.getMessage(), e);
                    }
                }));
            }
            for (Future<?> f: futures) f.get();
        } finally {
            pool.shutdown();
        }

        assertTrue(subscribeAll.await(5, TimeUnit.SECONDS), "구독 완료 대기 타임아웃(구독 실패)");

        //When
        StompSession senderSession = stompClient
                .connectAsync(URL, new WebSocketHttpHeaders(), createConnectHeaders(senderToken), new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        log.info("Sender Connected: {}", session.getSessionId());
                    }
                }).get(10, TimeUnit.SECONDS);

        ChatMessageRequestDto message = new ChatMessageRequestDto();
        message.setChatRoomId(testGroupRoom.getId());
        message.setMessageType(MessageType.TEXT);
        message.setContent("hi");

        CountDownLatch receivedAll = new CountDownLatch(participantCount);

        senderSession.send("/pub/chat/message", message);

        // polling 방식으로 성공조건 기다림

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            int got = receivedMap.size();
            while (receivedAll.getCount() > participantCount - got) {
                receivedAll.countDown();
            }
            if (got >= participantCount) break;
            Thread.sleep(5000);
        }

        //Then
        assertTrue(receivedAll.await(0, TimeUnit.SECONDS),
                () -> "모든 수신자가 메세지를 받지 못함" + receivedMap.size() + "/" + participantCount);

        int checks = Math.min(5, participantCount);
        List<ChatMessageResponseDto> samples = receivedMap.values().stream().limit(checks).toList();
        for (ChatMessageResponseDto dto : samples) {
            assertThat(dto).isNotNull();
            assertThat(dto.content()).isEqualTo("hi");
            assertThat(dto.roomId()).isEqualTo(testGroupRoom.getId());
        }

        try {senderSession.disconnect(); } catch (Exception ignore) {}
        for (StompSession stompSession : receiverSessions) {
            try {stompSession.disconnect();} catch (Exception ignore) {}
        }
    }
}
