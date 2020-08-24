package com.devkev.devscript.raw;

import com.devkev.devscript.raw.ApplicationBuilder.Type;

public class DataType {
	public final Type type;
	public final boolean isArray;
	
	public DataType(Type type, boolean isArray) {
		this.type = type;
		this.isArray = isArray;
	}
	
	/**Converts a type string to a dataType.<br>
	 * Returns null, if failed*/
	public static DataType toDataType(String s) {
		Type t = Type.getType(s.replaceAll(Command.ARRAY_INDICATOR, ""));
		
		if(t == null) return null;
		else return new DataType(t, s.contains(Command.ARRAY_INDICATOR));
	}
	
	public String toString() {
		return "[" + type.name() + ":" + isArray + "]";
	}
}
