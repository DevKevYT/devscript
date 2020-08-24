package com.devkev.devscript.raw;

import java.util.HashMap;
import java.util.Set;

/**Aka "object"*/
public class Dictionary {
	
	private HashMap<String, Object> entries = new HashMap<String, Object>(1);
	
	public void addEntry(String key, Object value) {
		entries.put(key, value);
	}
	
	public boolean removeEntry(String key) {
		return entries.remove(key) != null;
	}
	
	public void clear() {
		entries.clear();
	}
	
	/**May return null String*/
	public Object getEntry(String key) {
		return entries.get(key);
	}
	
	public Set<String> getKeys() {
		return entries.keySet();
	}
	
	public String toString() {
		String dict = "DICT:{";
		for(String s : entries.keySet()) dict += "<" + s.toString() + ":" + entries.get(s) + ">";
		dict += "}";
		return dict;
	}
}
