package com.zeebra.domain.product.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "product_option_meta_mv")
public class ProductOptionMetaView {
	@Id
	@Column(name = "product_option_id")
	private Long productOptionId;

	@Column(name = "product_name")
	private String productName;

	@Column(name = "product_thumbnail")
	private String productThumbnail;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "option_list", columnDefinition = "jsonb")
	private String optionListJson;
}