package com.zeebra.domain.chat.config;

import com.zeebra.global.security.jwt.JwtProvider;
import com.zeebra.global.web.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    private final String ACCESS_TOKEN_COOKIE_NAME = "__Host-AT";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub"); //구독자가 메세지 받을 경로

        registry.setApplicationDestinationPrefixes("/pub"); //발행자가 메세지 보낼 경로
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            @Order(Ordered.HIGHEST_PRECEDENCE)
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // 4. STOMP "CONNECT" 프레임일 때만 실행
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // 5. HTTP 핸드셰이크 때 저장된 "nativeHeaders" (Cookie 포함) 가져오기
                    Map<String, List<String>> nativeHeaders =
                            (Map<String, List<String>>) message.getHeaders().get("nativeHeaders");

                    if (nativeHeaders != null && nativeHeaders.containsKey("cookie")) {
                        String cookieHeader = nativeHeaders.get("cookie").get(0);

                        // 6. Cookie에서 Access Token 파싱
                        String accessToken = cookieUtil.getCookieHeader(cookieHeader, ACCESS_TOKEN_COOKIE_NAME);

                        if (accessToken != null && jwtProvider.isValid(accessToken)) {
                            // 7. 토큰이 유효하면, 인증 정보(UserPrincipal) 생성
                            Authentication authentication = jwtProvider.toAuthentication(accessToken);

                            // 8. (핵심) 인증 정보를 STOMP 세션의 유저로 등록
                            accessor.setUser(authentication);
                        }
                    }
                }
                return message;
            }

        });

    }
}
