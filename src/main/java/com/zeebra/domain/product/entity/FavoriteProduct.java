package com.zeebra.domain.product.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class FavoriteProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long productId;

    private LocalDateTime deletedAt;

    public FavoriteProduct(Long id, Long memberId, Long productId, LocalDateTime deletedAt) {
        this.id = id;
        this.memberId = memberId;
        this.productId = productId;
        this.deletedAt = deletedAt;
    }
}
