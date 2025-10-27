package com.zeebra.domain.chat.service;


import com.zeebra.domain.chat.dto.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    /**
     * @param chatRoomRequestDto (productId or SaleId)
     * @param currentMemberId
     * @return 생성되거나 조회된 채팅방 정보
     */
    ChatRoomResponseDto createOrGetChatRoom(ChatRoomRequestDto chatRoomRequestDto, Long currentMemberId);

    /**
     * 실시간 채팅 메세지 저장
     * @param chatMessageRequestDto
     * @param currentMemberId
     * @return 저장된 메세지
     */
    @Transactional
    ChatMessageResponseDto saveMessage(ChatMessageRequestDto chatMessageRequestDto, Long currentMemberId);

    /**
     * 채팅방 메세지 조회
     */
    Page<ChatMessageResponseDto> getChatHistory(Long chatRoomId, Pageable pageable);

    /**
     * 채팅방 나가기
     * @param chatRoomId 나갈 채팅방의 ID
     * @param currentMemberId 나가는 사용자의 ID
     */
    void leaveChatRoom(Long chatRoomId, Long currentMemberId);


    /**
     * 거래 제안
     */
    @Transactional
    TradeResponseDto proposeTrade(Long chatRoomId, TradeRequestDto tradeRequestDto, Long currentMemberId);
}
