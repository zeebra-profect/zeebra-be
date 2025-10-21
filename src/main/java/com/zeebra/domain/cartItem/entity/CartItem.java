package com.zeebra.domain.cartItem.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@Getter
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cartId;

    private Long productOptionId;

    private BigDecimal snapShotPrice;

    private int quantity;

    public CartItem(Long id, Long cartId, Long productOptionId, BigDecimal snapShotPrice, int quantity) {
        this.id = id;
        this.cartId = cartId;
        this.productOptionId = productOptionId;
        this.snapShotPrice = snapShotPrice;
        this.quantity = quantity;
    }
}
