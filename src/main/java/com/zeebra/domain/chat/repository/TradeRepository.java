package com.zeebra.domain.chat.repository;

import com.zeebra.domain.chat.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Optional<Trade> findByRoomId(Long roomId);
}
