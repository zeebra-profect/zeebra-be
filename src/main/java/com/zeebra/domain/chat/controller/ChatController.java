package com.zeebra.domain.chat.controller;

import com.zeebra.domain.chat.dto.ChatRoomRequestDto;
import com.zeebra.domain.chat.dto.ChatRoomResponseDto;
import com.zeebra.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@Tag(name = "Chat API", description = "채팅방 생성 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 생성 또는 입장", description = "productId 또는 saleId 받아 채팅방 조회 및 생성")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponseDto> createOrGetChatRoom(
            @RequestBody ChatRoomRequestDto chatRoomRequestDto
            //@AuthenticationPrincipal user 가져오기
    ) {
        Long currentUserId = 1L;

        ChatRoomResponseDto chatRoomResponseDto = chatService.createOrGetChatRoom(chatRoomRequestDto, currentUserId);

        return ResponseEntity.ok(chatRoomResponseDto);
    }
}
