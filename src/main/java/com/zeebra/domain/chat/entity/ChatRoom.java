package com.zeebra.domain.chat.entity;


import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChatRoom")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    // 1:1 채팅 (Sales)
    @Column(name = "sale_id")
    private Long saleId;

    // 그룹 채팅 (Product)
    @Column(name = "product_id")
    private Long productId;

    //enum으로 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "chat_room_type", nullable = false)
    private ChatRoomType ChatRoomType;

    @Column(name = "dm_pair_key", unique = true)
    private String dmPairKey;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "room_deleted_date")
    private LocalDateTime roomDeletedDate;

    @Builder
    public ChatRoom(long saleId, long productId, ChatRoomType chatRoomType,
                    String dmPairKey, long lastMessageId, LocalDateTime roomDeletedDate) {
        this.saleId = saleId;
        this.productId = productId;
        this.ChatRoomType = chatRoomType;
        this.dmPairKey = dmPairKey;
        this.lastMessageId = lastMessageId;
        this.roomDeletedDate = roomDeletedDate;
    }
}
