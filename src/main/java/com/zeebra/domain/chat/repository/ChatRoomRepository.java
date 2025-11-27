package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findTopByProductIdOrderByIdAsc(Long productId);

    Optional<ChatRoom> findByDmPairKey(String dmPairKey);

    Optional<ChatRoom> findByProductId(Long productId);

    List<ChatRoom> findBySaleId(Long saleId);

}
