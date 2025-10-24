package com.zeebra.domain.chat.dto;


import com.zeebra.domain.chat.entity.ChatRoom;
import com.zeebra.domain.chat.entity.ChatRoomType;

import lombok.Getter;

@Getter

public class ChatRoomResponseDto {
    private Long chatRoomId;
    private Long productId;
    private Long saleId;
    private Long lastMessageId;
    private ChatRoomType chatRoomType;

    public static ChatRoomResponseDto from(ChatRoom chatRoom) {
        return new ChatRoomResponseDto(
                chatRoom.getId(),
                chatRoom.getChatRoomType(),
                chatRoom.getProductId(),
                chatRoom.getSaleId(),
                chatRoom.getLastMessageId()
        );
    }
    private ChatRoomResponseDto(Long chatRoomId, ChatRoomType chatRoomType, Long productId, Long saleId, Long lastMessageId) {
        this.chatRoomId = chatRoomId;
        this.chatRoomType = chatRoomType;
        this.productId = productId;
        this.saleId = saleId;
        this.lastMessageId = lastMessageId;
    }
}

