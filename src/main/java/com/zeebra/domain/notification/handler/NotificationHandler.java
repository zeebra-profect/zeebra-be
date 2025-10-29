package com.zeebra.domain.notification.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class NotificationHandler extends TextWebSocketHandler {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    // 유저별 세션 저장소
    private Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 연결될 때 실행
        System.out.println("웹소켓 연결 확인 : " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(payload);
            String token = jsonNode.get("token").asText();

            // 토큰에서 memberId 추출
            Long memberId = jwtProvider.getSubjectAsLong(token);

            if (memberId == null) {
                System.err.println("유효하지 않은 토큰");
                return;
            }

            // 세션 저장
            userSessions.put(memberId, session);
            System.out.println("유저 " + memberId + " 인증 완료");

        } catch (Exception e) {
            System.err.println("인증 실패: " + e.getMessage());
        }
    }

    // 알림 보내는 메서드
    public void sendNotification(Long memberId, NotificationResponse response) {
        WebSocketSession session = userSessions.get(memberId);
        if (memberId != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                System.err.println("알림 전송 실패: " + e.getMessage());
            }
        }
        System.out.println("인증 실패");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 연결 끊길 때 실행
        userSessions.values().remove(session);
        System.out.println("웹소켓 연결 끊김: " + session.getId());
    }
}
