package com.zeebra.domain.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Getter
@NoArgsConstructor
public class ProductSearchMv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String modelNumber;

    private String thumbnail;

    private String categoryName;

    private String brandName;

    private String searchTextNorm;
}
