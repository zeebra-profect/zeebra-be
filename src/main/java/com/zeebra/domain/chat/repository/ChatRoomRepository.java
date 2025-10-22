package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProductIdAndSaleId(Long productId, Long saleId);

    Optional<ChatRoom> findByDmPairKey(String dmPairKey);


}
