package com.zeebra.domain.product.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductDocument {

    @JsonProperty("id")
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