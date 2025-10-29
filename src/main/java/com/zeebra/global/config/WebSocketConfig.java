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
//    private final TestWebSocketHandler testWebSocketHandler;

//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        // 핸들러 등록 및 모든 도메인에서 웹소켓 허용
//        System.out.println("===== 웹소켓 핸들러 등록 중... =====");

    /// /        registry.addHandler(notificationHandler, "/api/notification")
    /// /                .setAllowedOrigins("*");
//        registry.addHandler(testWebSocketHandler, "/test")
//                .setAllowedOrigins("*");
//
//
//    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/api/notification")
                .setAllowedOrigins("*");
    }

}
