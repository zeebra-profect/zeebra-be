package com.zeebra.domain.chat.dto;

import com.zeebra.domain.chat.entity.ChatMessage;
import com.zeebra.domain.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;


import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageResponseDto {

    private Long messageId;
    private Long roomId;
    private Long senderMemberId;
    private String senderName;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private LocalDateTime createTime;

    public static ChatMessageResponseDto from(ChatMessage chatMessage) {
        return new ChatMessageResponseDto(
                chatMessage.getId(),
                chatMessage.getChatRoomMember().getChatRoom().getId(),
                chatMessage.getChatRoomMember().getMemberId(),
                chatMessage.getChatRoomMember().getMemberName(),
                chatMessage.getMessageType(),
                chatMessage.getMessageContent(),
                chatMessage.getImageUrl(),
                chatMessage.getCreatedAt()
        );
    }
}
