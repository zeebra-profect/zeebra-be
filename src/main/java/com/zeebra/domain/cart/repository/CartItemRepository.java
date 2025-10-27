package com.zeebra.domain.cart.repository;

import com.zeebra.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductOptionId(Long cartId, Long productOptionId);
}
