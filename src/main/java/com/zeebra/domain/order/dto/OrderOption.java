package com.zeebra.domain.order.dto;

public record OrderOption(String name, String value) {
	public static OrderOption of(String name, String value){
		return new OrderOption(name, value);
	}
}