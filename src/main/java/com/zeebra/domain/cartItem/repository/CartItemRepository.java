package com.zeebra.domain.cartItem.repository;

import com.zeebra.domain.cartItem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
