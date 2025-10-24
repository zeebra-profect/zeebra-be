package com.zeebra.domain.chat.dto;

import com.zeebra.domain.chat.entity.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {

    private Long chatRoomId;
    private MessageType messageType;
    private String content;
    private String imageUrl;
}
