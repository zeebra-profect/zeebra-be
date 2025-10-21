package com.zeebra.domain.product.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long brandId;

    private Long categoryId;

    private String name;

    private String description;

    private String modelNumber;

    private String thumbnail;

    private List<String> images;

    private int reviewCount;

    private int favoriteProductCount;

    private LocalDateTime deletedAt;
}
