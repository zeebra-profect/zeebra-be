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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Chat API", description = "채팅방 생성 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 생성 또는 입장", description = "productId, GROUP을 받아 그룹채팅 생성")
    @PostMapping("/group/rooms")
    public ApiResponse<ChatRoomResponseDto> createOrGetGroupChatRoom(
            @RequestBody ChatRoomRequestDto chatRoomRequestDto, // (productId, type==Group)
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ) {
        Long currentUserId = (principal != null) ? principal.getMemberId() : null;
        ChatRoomResponseDto chatRoomResponse = chatService.createOrGetChatRoom(chatRoomRequestDto, currentUserId);

        return ApiResponse.success(chatRoomResponse);
    }

    @Operation(summary = "그룹 채팅방 메세지 내역 조회(로그인 불필요)")
    @GetMapping("/group/rooms/{roomId}/messages")
    public ApiResponse<Page<ChatMessageResponseDto>> getGroupChatHistory(
            @PathVariable("roomId") Long roomId,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        Page<ChatMessageResponseDto> chatHistory = chatService.getChatHistory(roomId, null, pageable);
        return ApiResponse.success(chatHistory);
    }

    @Operation(summary = "1:1 채팅방 생성 또는 입장", description = "SaleId, DM을 받아 그룹채팅 생성")
    @PostMapping("/dm/rooms")
    public ApiResponse<ChatRoomResponseDto> createOrGetDMChatRoom(
            @RequestBody ChatRoomRequestDto chatRoomRequestDto, // (productId, type==Group)
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ) {
        Long currentUserId = principal.getMemberId();
        ChatRoomResponseDto chatRoomResponse = chatService.createOrGetChatRoom(chatRoomRequestDto, currentUserId);

        return ApiResponse.success(chatRoomResponse);
    }

    @Operation(summary = "1:1 채팅방 메세지 내역 조회(로그인 필수)")
    @GetMapping("/dm/rooms/{roomId}/messages")
    public ApiResponse<Page<ChatMessageResponseDto>> getDMChatHistory(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        Long currentUserId = principal.getMemberId();
        Page<ChatMessageResponseDto> chatHistory = chatService.getChatHistory(roomId, currentUserId, pageable);
        return ApiResponse.success(chatHistory);
    }

    @Operation(summary = "1:1 채팅방 목록")
    @GetMapping("/rooms/dmlist")
    public ApiResponse<List<ChatRoomList>> getChatRooms(
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ) {
        Long currentUserId = principal.getMemberId();
        List<ChatRoomList> myDm = chatService.getMyChatRooms(currentUserId);
        return ApiResponse.success(myDm);
    }

    @Operation(summary = "1:1 채팅방 나가기")
    @DeleteMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal
    ) {
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
    ) {
        Long currentUserId = principal.getMemberId();
        TradeResponseDto tradeResponseDto = chatService.proposeTrade(roomId, tradeRequestDto, currentUserId);
        return ApiResponse.success(tradeResponseDto);
    }
}
