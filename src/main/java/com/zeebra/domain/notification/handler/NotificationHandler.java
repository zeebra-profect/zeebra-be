package com.zeebra.domain.notification.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler extends TextWebSocketHandler {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🟢 웹소켓 연결! sessionId: {}", session.getId());

        // 👇 여기에 추가
        String token = null;
        if (session.getHandshakeHeaders().containsKey("Cookie")) {
            String cookies = session.getHandshakeHeaders().getFirst("Cookie");
            if (cookies != null) {
                for (String cookie : cookies.split("; ")) {
                    if (cookie.startsWith("__Host-AT=")) {
                        token = cookie.substring("__Host-AT=".length());
                        break;
                    }
                }
            }
        }

        log.info("추출한 토큰: {}", token);

        if (token != null) {
            Long memberId = jwtProvider.getSubjectAsLong(token);
            log.info("추출한 memberId: {}", memberId);

            if (memberId != null) {
                userSessions.put(memberId, session);
                log.info("✅ 유저 {} 등록 완료. 현재 접속자: {}명", memberId, userSessions.size());
            }
        } else {
            log.error("❌ 쿠키에 토큰 없음");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            JsonNode jsonNode = objectMapper.readTree(payload);
            String token = jsonNode.get("token").asText();

            Long memberId = jwtProvider.getSubjectAsLong(token);

            if (memberId == null) {
                log.error("❌ 유효하지 않은 토큰");
                session.close();
                return;
            }

            userSessions.put(memberId, session);
            log.info("✅ 유저 {} 인증 완료. 현재 접속자: {}명", memberId, userSessions.size());

        } catch (Exception e) {
            log.error("❌ 인증 실패: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.values().remove(session);
        log.info("🔴 웹소켓 끊김. 현재 접속자: {}명", userSessions.size());
    }

    public void sendNotifications(Long memberId, List<NotificationResponse> response) {
//        System.out.println("sendNotifications 실행");
        WebSocketSession session = userSessions.get(memberId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
                log.info("📤 유저 {}에게 알림 전송 완료", memberId);
            } catch (Exception e) {
                log.error("❌ 알림 전송 실패: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ 유저 {} 세션 없음 또는 닫힘", memberId);
        }
    }

    public void sendNotification(Long memberId, NotificationResponse response) {
        WebSocketSession session = userSessions.get(memberId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
                log.info("📤 유저 {}에게 알림 전송 완료", memberId);
            } catch (Exception e) {
                log.error("❌ 알림 전송 실패: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ 유저 {} 세션 없음 또는 닫힘", memberId);
        }
    }
}