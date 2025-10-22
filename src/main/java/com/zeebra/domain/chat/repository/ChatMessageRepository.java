package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomMember_ChatRoomId(Long chatRoomId, Pageable pageable);
}
