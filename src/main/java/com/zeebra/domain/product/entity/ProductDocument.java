package com.zeebra.domain.product.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDocument {

    private Long productId;
    private String productName;
    private String modelNumber;
    private String description;
    private Long brandId;
    private String brandName;
    private String productThumbnail;
    private List<String> images;
    private BigDecimal minPrice;
    private Long categoryId;
    private String categoryName;
    private Integer reviewCount;
    private Integer favoriteProductCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}