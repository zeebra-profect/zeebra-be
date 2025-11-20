package com.zeebra.domain.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_search_document")
@Immutable  // 읽기 전용 (DB 트리거가 관리)
@Getter
public class ProductSearchDocument {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "model_number")
    private String modelNumber;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "favorite_count")
    private Integer favoriteCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
