package com.devkev.devscript.raw;

public final class Alphabet {
    public static final char BRK_0 = '(';
    public static final char BRK_1 = ')';
    public static final char STR_0 = '"';
    public static final char VAR_0 = '$';
    public static final char BLCK_0 = '{';
    public static final char BLCK_1 = '}';
    public static final char BREAK = ';';
    public static final char ARR_0 = '[';
    public static final char ARR_1 = ']';
    public static final char ESCAPE = '\\';
    public static final char COMMENT = '#';
    public static final char LSTRING_0 = '<';
    public static final char LSTRING_1 = '>';
    
    private Alphabet() {}

    public static boolean partOf(String checkString) {
    	for(char c : checkString.toCharArray()) {
    		if(!partOf(c)) return false;
    	}
    	return true;
    }
    
    public static boolean partOf(char c) {
        for (java.lang.reflect.Field f : Alphabet.class.getFields()) {
            try {
                if (f.getChar(null) == c) return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
