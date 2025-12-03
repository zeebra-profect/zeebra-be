package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatRoom;
import com.zeebra.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.inject.Qualifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    List<ChatRoomMember> findByChatRoomIdAndDeletedAtIsNull(Long chatRoomId);

    @EntityGraph(attributePaths = {"chatRoom"})
    List<ChatRoomMember> findByMemberIdAndDeletedAtIsNull(Long memberId);

    // 여러 채팅방의 상대방 정보들 한 번에 조회 -> 내가 속한 방들 나 제외한 모든 유저 가져옴
    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.member " +
            "WHERE crm.chatRoom.id IN :roomIds AND crm.memberId <> :myMemberId " +
            "AND crm.deletedAt IS NULL")
    List<ChatRoomMember> findAllOpponents(@Param("roomIds") List<Long> roomIds, @Param("myMemberId") Long myMemberId);
}
