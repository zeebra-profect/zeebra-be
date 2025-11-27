package com.zeebra.domain.chat.controller;

import com.zeebra.domain.chat.dto.ChatMessageRequestDto;
import com.zeebra.domain.chat.dto.ChatMessageResponseDto;
import com.zeebra.domain.chat.service.ChatService;
import com.zeebra.global.security.jwt.JwtProvider;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;


import java.security.Principal;


@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    private final MeterRegistry meterRegistry;


    @MessageMapping("/chat/message")
    public void sendMessage(
            ChatMessageRequestDto requestDto,
            Principal principal

    ) {
        Timer.Sample sample = Timer.start(meterRegistry);

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
                    savedMessage
            );
            log.info("[WebSocket] 메시지 전송 성공: (Room: {})", savedMessage.roomId());

            Counter counter = meterRegistry.counter("chat-message-total", "endpoint", "/chat/message");

            counter.increment(); // 메트릭 증가

        } catch (Exception e) {
            //  3. 에러 발생 시 서버 로그(터미널)에 에러 메시지 출력
            log.error("Failed to send WebSocket message: {}", e.getMessage());
            e.printStackTrace(); // (더 자세한 스택 트레이스)
        } finally {
            Timer timer = meterRegistry.timer("ws_chat_message_seconds", "endpoint", "/chat/message");

            sample.stop(timer); // 처리 시간 기록
        }
    }
}
