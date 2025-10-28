package com.zeebra.domain.chat.controller;

import com.zeebra.domain.chat.dto.*;
import com.zeebra.domain.chat.service.ChatService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Chat API", description = "채팅방 생성 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 생성 또는 입장", description = "productId 또는 saleId 받아 채팅방 조회 및 생성")
    @PostMapping("/rooms")
    public ApiResponse<ChatRoomResponseDto> createOrGetChatRoom(
            @RequestBody ChatRoomRequestDto chatRoomRequestDto,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ) {
        Long currentUserId = principal.getMemberId();

        ChatRoomResponseDto chatRoomResponse = chatService.createOrGetChatRoom(chatRoomRequestDto, currentUserId);

        return ApiResponse.success(chatRoomResponse);
    }

    @Operation(summary = "채팅방 메세지 내역 조회")
    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<Page<ChatMessageResponseDto>> getChatHistory(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal JwtProvider .JwtUserPrincipal principal, // 내 정보
            @PageableDefault(size = 30) Pageable pageable
    ){
        Long currentUserId = principal.getMemberId();
        Page<ChatMessageResponseDto> chatHistory = chatService.getChatHistory(roomId, currentUserId, pageable);
        return ApiResponse.success(chatHistory);
    }


    @Operation(summary = "1:1 채팅방 나가기")
    @DeleteMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ){
        Long currentUserId = principal.getMemberId();
        chatService.leaveChatRoom(roomId, currentUserId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "1:1 채팅방 내 거래")
    @PostMapping("/rooms/{roomId}/trade")
    public ApiResponse<TradeResponseDto> proposeTrade(
            @PathVariable Long roomId,
            @RequestBody TradeRequestDto tradeRequestDto,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ){
        Long currentUserId = principal.getMemberId();
        TradeResponseDto tradeResponseDto = chatService.proposeTrade(roomId, tradeRequestDto, currentUserId);
        return ApiResponse.success(tradeResponseDto);
    }
}
