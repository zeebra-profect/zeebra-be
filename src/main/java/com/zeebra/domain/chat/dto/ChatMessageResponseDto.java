package com.zeebra.domain.chat.dto;

import com.zeebra.domain.chat.entity.ChatMessage;
import com.zeebra.domain.chat.entity.ChatRoomMember;
import com.zeebra.domain.chat.entity.MessageType;
import com.zeebra.domain.member.entity.Member;


import java.time.LocalDateTime;

public record ChatMessageResponseDto (

    Long messageId,
    Long roomId,
    Long senderMemberId,
    String senderName,
    String profileImageUrl,
    MessageType messageType,
    String content,
    String imageUrl,
    LocalDateTime createdAt
){
    public static ChatMessageResponseDto from(ChatMessage chatMessage, Member member) {
        ChatRoomMember sender = chatMessage.getChatRoomMember();

        String senderName = (member != null) ? member.getNickname() : sender.getMemberName();
        String profileImageUrl = (member != null) ? member.getMemberImage() : null;

        return new ChatMessageResponseDto(
                chatMessage.getId(),
                sender.getChatRoom().getId(),
                sender.getMemberId(),
                senderName,
                profileImageUrl,
                chatMessage.getMessageType(),
                chatMessage.getMessageContent(),
                chatMessage.getImageUrl(),
                chatMessage.getCreatedAt()
        );
    }
}
