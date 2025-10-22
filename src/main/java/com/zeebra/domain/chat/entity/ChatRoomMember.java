package com.zeebra.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ChatRoomMember")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {
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

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Builder
    public ChatRoomMember(ChatRoom chatRoom, Long memberId, String memberName) {
        this.chatRoom = chatRoom;
        this.memberId = memberId;
        this.memberName = memberName;
    }
}
