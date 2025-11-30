package com.zeebra.domain.chat.controller;

import com.zeebra.domain.chat.dto.ChatMessageRequestDto;
import com.zeebra.domain.chat.dto.ChatMessageResponseDto;
import com.zeebra.domain.chat.service.ChatService;
import com.zeebra.global.security.jwt.JwtProvider;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;


import java.security.Principal;
import java.time.Duration;


@Slf4j
@Controller
public class ChatSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    private final MeterRegistry meterRegistry;

    private final Counter messageCounter;
    private final Timer messageTimer;
    private final Counter errorCounter;

    // MeterRegistry를 주입받아 생성자에서 메트릭을 한 번만 등록/초기화
    public ChatSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate, MeterRegistry meterRegistry) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.meterRegistry = meterRegistry;

        // Counter 한 번만 생성 및 등록
        this.messageCounter = Counter.builder("ws_chat_message")
                .tag("endpoint", "/chat/message")
                .description("Total number of chat messages processed")
                .register(meterRegistry);

        // Timer 한 번만 생성 및 등록
        this.messageTimer = Timer.builder("ws_chat_message_seconds")
                .tag("endpoint", "/chat/message")
                .description("Processing time for chat messages")
                .publishPercentileHistogram()
                .sla(Duration.ofMillis(100))
                .register(meterRegistry);


        this.errorCounter = Counter.builder("ws_chat_message_error")
                .tag("endpoint", "/chat/message")
                .register(meterRegistry);
    }

    @MessageMapping("/chat/message")
    public void sendMessage(
            ChatMessageRequestDto requestDto,
            Principal principal

    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        boolean isSuccess = false;

        try { //️ 2. try-catch 블록 추가
            if (principal == null) {
                log.warn(" WebSocket - 인증되지 않은 사용자 메세지 전송 시도: (Room : {})", requestDto.getChatRoomId());
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    (UsernamePasswordAuthenticationToken) principal;

            JwtProvider.JwtUserPrincipal userPrincipal =
                    (JwtProvider.JwtUserPrincipal) auth.getPrincipal();

            Long currentMemberId = userPrincipal.getMemberId();
            System.out.println("currentMemId : " + currentMemberId);
            log.info(" [WebSocket] 메시지 수신: (Room: {}, User: {})",
                    requestDto.getChatRoomId(), currentMemberId);
            ChatMessageResponseDto savedMessage = chatService.saveMessage(requestDto, currentMemberId);

            messagingTemplate.convertAndSend(
                    "/sub/chat/room/" + savedMessage.roomId(),
                    savedMessage);

            isSuccess = true;
            log.info("[WebSocket] 메시지 전송 성공: (Room: {})", savedMessage.roomId());
        } catch (Exception e) {
            //  3. 에러 발생 시 서버 로그(터미널)에 에러 메시지 출력
            log.error("Failed to send WebSocket message: {}", e.getMessage());
            e.printStackTrace(); // (더 자세한 스택 트레이스)
            errorCounter.increment();

        } finally {
            if (isSuccess) {messageCounter.increment();}
            sample.stop(messageTimer); // 처리 시간 기록
        }
    }
}
