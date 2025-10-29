package com.zeebra.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomList {
    Long chatRoomId;
    String roomName;
    String roomProfileImageUrl;

    String lastMessageContent;
    LocalDateTime lastMessageTime;

    long unreadCount;
}
