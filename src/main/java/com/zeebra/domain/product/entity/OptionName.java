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
public class OptionName extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String value;

    private Boolean useWn;

    public OptionName(String name, String value, Boolean useWn) {
        this.name = name;
        this.value = value;
        this.useWn = useWn;
    }
}
