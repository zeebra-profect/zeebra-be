package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatRoom;
import com.zeebra.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
    List<ChatRoomMember> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<ChatRoomMember> findByChatRoomIdAndDeletedAtIsNull(Long chatRoomId);
}
