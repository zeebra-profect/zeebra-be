package com.zeebra.domain.product.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class OptionCombination extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productOptionId;

    private Long optionNameId;

    public OptionCombination(Long productOptionId, Long optionNameId) {
        this.productOptionId = productOptionId;
        this.optionNameId = optionNameId;
    }
}
