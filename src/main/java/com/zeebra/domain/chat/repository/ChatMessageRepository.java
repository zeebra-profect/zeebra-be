package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomMemberChatRoomId(Long chatRoomId, Pageable pageable);

    Optional<ChatMessage> findById(Long lastMessageId);

    long countByChatRoomMember_ChatRoomIdAndIdGreaterThan(Long chatRoomId, Long lastReadMessageId);

    Optional<ChatMessage> findFirstByChatRoomMember_ChatRoomIdOrderByIdDesc(Long chatRoomId);
}
