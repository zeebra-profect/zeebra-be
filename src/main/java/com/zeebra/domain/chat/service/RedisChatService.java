package com.zeebra.domain.chat.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.domain.chat.dto.ChatMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_KEY_PREFIX = "chat:room:";


    public void appendMessage(ChatMessageResponseDto message) {
        String key = CHAT_KEY_PREFIX + message.roomId();

        try{
            String json = objectMapper.writeValueAsString(message);

            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, Duration.ofDays(7));

        } catch (JsonProcessingException e) {
            log.error("메세지 직렬화 실패, room={}, err={}", message.roomId(), e.getMessage());
        }
    }
}
