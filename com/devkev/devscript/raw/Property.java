package com.devkev.devscript.raw;

public class Property {
	
	private Object data;
	private String name;
	
	private Property(String name, Object data) {
		this.name = name;
		this.data = data;
	}
	
	public static Property of(String name, Object object) {
		return new Property(name, object);
	}
	
	public String getName() {
		return name;
	}
	
	public Object getData() {
		return data;
	}
}
