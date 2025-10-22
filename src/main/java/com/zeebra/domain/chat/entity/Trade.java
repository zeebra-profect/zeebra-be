package com.zeebra.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "Trade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false, unique = true)
    private ChatRoom chatRoom;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Builder
    public Trade(ChatRoom chatRoom, BigDecimal price) {
        this.chatRoom = chatRoom;
        this.price = price;
    }
}
