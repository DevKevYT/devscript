package com.devkev.devscript.raw;

/**This class holds various constants etc for the script.*/
public final class ProcessUtils {
	
	public enum Type {
		//Base data types
		NULL("null"), 
		STRING("string"), 
		BOOLEAN("boolean"),
		
		//Complex data types
		BLOCK("block"), 
		ANY("?"), 
		
		NATIVE_OBJECT("obj"), //For objects outside of the devscript structure
		CONTINUE("..."), //only used in command arguments
		ARRAY_ANY("???"), //Difference between any and all: any: any object is accepted/returned. arrayany: Any object, no matter if its an array or not
	
		INTEGER("int"), 
		FLOAT("float"), 
		NUMBER("num");
		//Examples on command arguments"
		// ? = Any data type accepted, no array
		// @? = Array of any data type. -> array = [ $true "String" (add 1 1)]
		// @string = String array accepted (Array only containing strings) -> ["string" "string" "string"]
		// string = string accepted
		// ??? = Any data type accepted, no matter if array or not
		// @??? = Also possible, but wont make a difference to ???
		
		public final String typeName;
		
		Type(String typeName) {
			this.typeName = typeName.toLowerCase();
		}
		
		public static Type getType(String typeName) {
			typeName = typeName.toLowerCase();
			for(Type t : Type.values()) {
				if(t.typeName.equals(typeName)) return t;
			}
			return null;
		}
	}
	
	public static DataType toDataType(Object object) { //TODO arrays
		if(object == null) return new DataType(Type.NULL, false);
    	if(object.getClass().isArray()) panic("Single objects can't hold arrays. Use the Process.Array class to use arrays");
    	
    	if(object instanceof String) return new DataType(Type.STRING, false);
    	else if(object instanceof Integer) return new DataType(Type.STRING, false);
    	else if(object instanceof Float) return new DataType(Type.STRING, false);
    	else if(object instanceof Long) return new DataType(Type.STRING, false);
    	else if(object instanceof Boolean) return new DataType(Type.BOOLEAN, false);
    	else if(object instanceof Block) return new DataType(Type.BLOCK, false);
    	else if(object instanceof Array) return ((Array) (object)).arrayType;
    	else return new DataType(Type.NATIVE_OBJECT, false);
	}
	
	/**Returns true, if type1.isArray == type2.isArray and type1.type == Type.ANY or type1 == type2<br>
	 * If type1.type == Type.ARRAY_ANY, true is returned, without checking if type1.isArray == type2.isArray*/
	public static boolean typeMatch(DataType type1, DataType type2) {
		if(type1.type == Type.ARRAY_ANY) return true;
		if(type1.isArray != type2.isArray) return false;
		if(type1.type == Type.ANY) return true;
		if(type1.type == Type.NULL) return true;
		//if(type2.type == Type.ANY) return true;
		return type1.type == type2.type;
	}
	
	/**Throws an error if it can't get fixed/ignored. Panics should not happen.*/
	public static void panic(String message) {
		throw new IllegalAccessError("An error happened outside of the devscript environment: " + message);
	}
	
	/**Test, if the given string is convert-able to integer. (Contains only numbers)*/
	public static boolean testForWholeNumber(String string) {
		if(string == null) return false;
		if(string.isEmpty()) return false;
		if(string.startsWith("-")) string = string.substring(1);
		
		for(int i = 0; i < string.length(); i++) {
			if(!Character.isDigit(string.charAt(i))) return false;
		}
		return true;
	}
	
	/**Valid, if the string is separated either with a dot, or a comma:<br>
	 * <code>0.01<br>0,2<br>12,000.00</code> Is not allowed, because of a separator.<br><code>12000.00 or 12000,00</code>would be accepted.*/
	public static boolean testForFloat(String string) {
		if(string == null) return false;
		if(string.isEmpty()) return false;
		if(string.startsWith("-")) string = string.substring(1);
		
		string = string.replaceAll(",", ".");
		boolean point = false;
		for(int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if(!Character.isDigit(c) && c != '.') return false;
			else if(c == '.' && point) return false;
			if(c == '.') point = true;
		}
		return true;
	}
}
