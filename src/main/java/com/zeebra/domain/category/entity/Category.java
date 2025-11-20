package com.zeebra.domain.category.entity;

import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;

    private String name;

	private String thumbnail;

	@Builder
    public Category(Long parentId, String name, String thumbnail) {
        this.parentId = parentId;
        this.name = name;
		this.thumbnail = thumbnail;
    }
}