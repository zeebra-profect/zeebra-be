package com.zeebra.domain.chat.listener;


import com.zeebra.domain.chat.config.ChatAmqpConfig;
import com.zeebra.domain.chat.dto.ChatMessageResponseDto;
import com.zeebra.domain.chat.service.ChatService;
import com.zeebra.domain.chat.service.RedisChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ChatService chatService;
    private final RedisChatService redisChatService;

    @RabbitListener(queues = ChatAmqpConfig.CHAT_DB_QUEUE)
    public void consumeChatMessage(ChatMessageResponseDto message) {
        try {
            chatService.saveFromResponse(message);

            // Redis 캐시
            redisChatService.appendMessage(message);
        } catch (Exception e) {
            log.error("MQ에서 메세지 처리 실패, room={}, error={}", message.roomId(), e.getMessage(), e);
        }
    }
}
