package com.zeebra.domain.product.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDocument {

    // ES에 product_id 로 저장되어 있지만, 가끔 "id"로 올 수도 있어서 둘 다 허용
    @Id
    @Field(type = FieldType.Long, name = "product_id")
    @JsonProperty("product_id")
    @JsonAlias({ "id" })
    private Long productId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "product_name"),
            otherFields = {
                    @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    @JsonProperty("product_name")
    private String productName;

    @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer", name = "model_number")
    @JsonProperty("model_number")
    private String modelNumber;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    @JsonProperty("description")
    private String description;

    @Field(type = FieldType.Long, name = "brand_id")
    @JsonProperty("brand_id")
    private Long brandId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "brand_name"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    @JsonProperty("brand_name")
    private String brandName;

    @Field(type = FieldType.Keyword, name = "product_thumbnail", index = false, docValues = false)
    @JsonProperty("product_thumbnail")
    private String productThumbnail;

    @Field(type = FieldType.Keyword, name = "images", index = false, docValues = false)
    @JsonProperty("images")
    private List<String> images;

    @Field(type = FieldType.Scaled_Float, name = "min_price", scalingFactor = 100.0)
    @JsonProperty("min_price")
    private BigDecimal minPrice;

    @Field(type = FieldType.Long, name = "category_id")
    @JsonProperty("category_id")
    private Long categoryId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer", name = "category_name"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            })
    @JsonProperty("category_name")
    private String categoryName;

    @Field(type = FieldType.Integer, name = "review_count")
    @JsonProperty("review_count")
    private Integer reviewCount;

    @Field(type = FieldType.Integer, name = "favorite_product_count")
    @JsonProperty("favorite_product_count")
    private Integer favoriteProductCount;

    // 날짜 포맷이 "yyyy-MM-dd'T'HH:mm:ss" 또는 epoch_millis를 쓰고 있다면 이 포맷으로 시도
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, name = "created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
