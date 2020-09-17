package com.devkev.devscript.raw;

import java.util.ArrayList;

import com.devkev.devscript.raw.ApplicationBuilder.Type;

public class Array { //For dataContainers
	private final ArrayList<Object> indexes = new ArrayList<Object>(2);
	public DataType arrayType;
	
	public Array(Object... indexes) {
		for(Object d : indexes) this.indexes.add(d);
		updateArraytype();
	}
	
	public Array() {
		updateArraytype();
	}
	
	public void push(Object container) {
		indexes.add(container);
		updateArraytype();
	}
	
	public void push(Object container, int index) {
		indexes.add(index, container);
		updateArraytype();
	}
	
	public void pop(int index) {
		indexes.remove(index);
		updateArraytype();
	}
	
	public void pop(Object container) {
		indexes.remove(container);
		updateArraytype();
	}
	
	public ArrayList<Object> getIndexes() {
		return indexes;
	}
	
	public void updateArraytype() {
    	Type type = Type.NULL;
		for(int j = 0; j < indexes.size(); j++) {
			DataType containerType = ApplicationBuilder.toDataType(indexes.get(j));
			if(j > 0) {
				DataType indexType = ApplicationBuilder.toDataType(indexes.get(j-1));
				if(indexType.type != containerType.type && type != Type.ANY) {
					type = Type.ANY;
					break;
				}
			} else if(j == 0) {
				type = containerType.type;
			}
		}
		arrayType = new DataType(type, true);
	}
	
	public String toString() {
		String s = "ARRAY:{";
		for(int i = 0; i < indexes.size()-1; i++) s += indexes.get(i) + ",";
		if(indexes.size() > 0) s += indexes.get(indexes.size()-1);
		return s + "}";
	}
}
