package com.zeebra.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "product_search_mv")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSearchMv {

    @Id
    private Long id;

    private String name;

    private String description;

    @Column(name = "model_number")
    private String modelNumber;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "product_name_norm")
    private String productNameNorm;

    @Column(name = "brand_name_norm")
    private String brandNameNorm;

    @Column(name = "description_norm")
    private String descriptionNorm;

    @Column(name = "review_count")
    private Integer reviewCount;
}