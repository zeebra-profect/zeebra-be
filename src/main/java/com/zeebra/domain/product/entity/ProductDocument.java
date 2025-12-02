package com.zeebra.domain.product.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "product")
@Setting(settingPath = "/elasticsearch/product-settings.json")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDocument {

    @Id
    @Field(type = FieldType.Long, name = "product_id")
    private Long productId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "product_name"),
            otherFields = {
                    @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    private String productName;

    @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer", name = "model_number")
    private String modelNumber;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String description;

    @Field(type = FieldType.Long, name = "brand_id")
    private Long brandId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "brand_name"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    private String brandName;

    @Field(type = FieldType.Keyword, name = "product_thumbnail", index = false, docValues = false)
    private String productThumbnail;

    @Field(type = FieldType.Keyword, name = "images", index = false, docValues = false)
    private List<String> images;

    @Field(type = FieldType.Scaled_Float, name = "min_price", scalingFactor = 100.0)
    private BigDecimal minPrice;

    @Field(type = FieldType.Long, name = "category_id")
    private Long categoryId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "category_name"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    private String categoryName;

    @Field(type = FieldType.Integer, name = "review_count")
    private Integer reviewCount;

    @Field(type = FieldType.Integer, name = "favorite_product_count")
    private Integer favoriteProductCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, name = "updated_at")
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, name = "created_at")
    private LocalDateTime createdAt;
}