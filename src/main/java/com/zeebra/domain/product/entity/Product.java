package com.zeebra.domain.product.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
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

    public Product(Long brandId, Long categoryId, String name, String description, String modelNumber,
                   String thumbnail, List<String> images) {
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.modelNumber = modelNumber;
        this.thumbnail = thumbnail;
        this.images = images;
    }
}
