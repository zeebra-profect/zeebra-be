package com.zeebra.domain.product.entity;

import com.zeebra.domain.product.dto.ProductRequest;
import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long brandId;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    private String description;

    private String modelNumber;

    private BigDecimal minPrice;

    private String thumbnail;

    private List<String> images;

    private int reviewCount;

    private int favoriteProductCount;

    private LocalDateTime deletedAt;

    @Builder
    public Product(Long brandId, Long categoryId, String name, String description, String modelNumber,
                   String thumbnail, List<String> images) {
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.modelNumber = modelNumber;
        this.thumbnail = thumbnail;
        this.images = images;
        this.reviewCount = 0;
        this.favoriteProductCount = 0;
    }

    public static Product from(ProductRequest request) {
        return Product.builder()
                .brandId(request.brandId())
                .categoryId(request.categoryId())
                .name(request.productName())
                .description(request.productDescription())
                .modelNumber(request.modelName())
                .images(request.productImages())
                .thumbnail(request.productThumbnail())
                .build();
    }

    public Boolean hasFavoriteProducts() {
        return this.favoriteProductCount > 0;
    }

    public void increaseFavoriteProductCount() {
        this.favoriteProductCount++;
    }

    public void decreaseFavoriteProductCount() {
        if (favoriteProductCount <= 0) {
            throw new IllegalStateException(
                    "관심상품 개수가 0 이하인데 감소를 시도했습니다. 현재 값: " + favoriteProductCount
            );
        }
        favoriteProductCount--;
    }

    public void updateMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }
}
