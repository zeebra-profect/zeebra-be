package com.zeebra.domain.cart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	List<CartItem> findByCartId(Long cartId);
}