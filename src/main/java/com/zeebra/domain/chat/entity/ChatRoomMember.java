package com.zeebra.domain.chat.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChatRoomMember")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cr_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ChatRoomMember(ChatRoom chatRoom, Long memberId, String memberName) {
        this.chatRoom = chatRoom;
        this.memberId = memberId;
        this.memberName = memberName;
    }

    public void leave() {
        this.deletedAt = LocalDateTime.now();
    }
}
