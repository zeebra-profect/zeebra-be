package com.zeebra.domain.product.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "product")
@Setting(settingPath = "/elasticsearch/product-settings.json")
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

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer",name = "brand_name"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    private String brandName;

    @Field(type = FieldType.Long, name = "category_id")
    private Long categoryId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "category_name"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    private String categoryName;

    @Field(type = FieldType.Integer, name = "review_count")
    private Integer reviewCount;

    @Field(type = FieldType.Integer, name = "favorite_count")
    private Integer favoriteCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, name = "updated_at")
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, name = "created_at")
    private LocalDateTime createdAt;
}
