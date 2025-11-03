package com.zeebra.domain.product.dto;

public record SalesItemOptions(String name, String value) {
	public static SalesItemOptions of(String name, String value){
		return new SalesItemOptions(name, value);
	}
}