package com.zeebra.global.config;

import com.zeebra.domain.notification.handler.NotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationHandler notificationHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        System.out.println("========== WebSocket 핸들러 등록 중 ==========");

        registry.addHandler(notificationHandler, "/ws/notification")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");


        System.out.println("========== WebSocket 핸들러 등록 완료 ==========");
    }
}
