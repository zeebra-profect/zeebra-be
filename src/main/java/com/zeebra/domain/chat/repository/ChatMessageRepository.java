package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomMember_ChatRoomId(Long chatRoomId, Pageable pageable);

    Optional<ChatMessage> findById(Long lastMessageId);

    long countByChatRoomMember_ChatRoomIdAndIdGreaterThan(Long chatRoomId, Long lastReadMessageId);

    Optional<ChatMessage> findFirstByChatRoomMember_ChatRoomIdOrderByIdDesc(Long chatRoomId);

    // 여러 채팅방 안 읽은 메세지 개수 한 번에 그룹화, [chatRoomId, count]
    @Query("SELECT crm.chatRoom.id, COUNT(cm) " +
            "FROM ChatMessage cm " +
            "JOIN ChatRoomMember crm ON cm.chatRoomMember.chatRoom.id = crm.chatRoom.id " +
            "WHERE crm.memberId = :myMemberId " +
            "AND crm.chatRoom.id IN :roomIds " +
            "AND cm.id > crm.lastReadMessageId " +
            "GROUP BY crm.chatRoom.id")
    List<Object[]> countUnreadMessagesByRoomIds(@Param("roomIds") List<Long> roomIds, @Param("myMemberId") Long myMemberId);
}
