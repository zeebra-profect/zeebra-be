package com.zeebra.domain.chat.controller;

import com.zeebra.domain.chat.dto.ChatMessageRequestDto;
import com.zeebra.domain.chat.dto.ChatMessageResponseDto;
import com.zeebra.domain.chat.entity.ChatMessage;
import com.zeebra.domain.chat.service.ChatService;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping
    public void sendMessage(
            ChatMessageRequestDto requestDto,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ){
        Long currentMemberId = principal.getMemberId();

        ChatMessageResponseDto savedMessage = chatService.saveMessage(requestDto, currentMemberId);

        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + savedMessage.roomId(),
                savedMessage
        );
    }
}
