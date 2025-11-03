//package com.zeebra.global.config; // (패키지 경로는 맞게 수정하세요)
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.SimpMessageType;
//import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
//import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
//import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity; // ⭐️ (필요할 수 있음)
//
//@Configuration
//@EnableWebSocketSecurity // ⭐️ (보안 활성화)
//public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
//
//    @Override
//    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
//        //    1. CONNECT (연결) : 모든 사용자 조회 가능
//            messages.simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
//
//                //  2. SUBSCRIBE (구독/읽기) : 모든 사용자 그룹 채팅을 구독
//                .simpTypeMatchers(SimpMessageType.SUBSCRIBE).permitAll()
//
//                // 3. SEND (발행/쓰기) : 오직 인증된 사용자만 메시지를 보낼 수 있음
//                .simpDestMatchers("/pub/**").authenticated()
//
//                // ⭐️ 4. 그 외 모든 메시지는 일단 인증 요구
//                .anyMessage().authenticated();
//
//    }
//
//    /**
//     * CSRF 보호 비활성화 (STOMP는 Cookie가 아닌 다른 방식으로 인증하므로)
//     */
//    @Override
//    protected boolean sameOriginDisabled() {
//        return true;
//    }
//}

package com.zeebra.global.config;

import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

//    @Bean
//    public AuthorizationManager<Message<?>> messageAuthorizationManager(
//            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
//
//        messages
//                // 1. CONNECT (연결) - 모든 사용자 허용
//                .nullDestMatcher().permitAll()
//                .simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
//                .simpTypeMatchers(SimpMessageType.DISCONNECT).permitAll()
//                .simpTypeMatchers(SimpMessageType.HEARTBEAT).permitAll()
//
//                // 2. SUBSCRIBE (구독/읽기) - 모든 사용자 허용
//                .simpTypeMatchers(SimpMessageType.SUBSCRIBE).permitAll()
//
//                // 3. SEND (발행/쓰기) - 인증된 사용자만
//                .simpDestMatchers("/pub/**").authenticated()
//
//                // 4. 그 외 모든 메시지 - 인증 요구
//                .anyMessage().authenticated();
//
//        return messages.build();
//    }
}