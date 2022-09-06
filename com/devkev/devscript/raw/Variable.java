package com.devkev.devscript.raw;

public class Variable {	
	
	public final String name;
	Object value;
	public final boolean FINAL;
	boolean permanent = true;
	Block block; //Stack access never null If block == null, the variable might have been initialized when the process is not running -> block will be main on startup
	
	/**@param block Holds the stack access and important, for garbage collection. Can be null
	 * @param permanent - If the variable should get cleared, after the process finished. Useful for command lines*/
	public Variable(String name, Object value, boolean FINAL, boolean permanent, Block block) {
		this.name = name;
		this.value = value;
		this.FINAL = FINAL;
		this.block = block;
		this.permanent = permanent;
		if(block == null) {
			ProcessUtils.panic("Can't declare a non-permanent variable with a null block while process is running");
		}
	}
	
	/**@return false, if the variable assertion failed*/
	boolean setValue(Object value, boolean ignoreCheck) {
		if(FINAL && !permanent) return false;
		if(value.getClass().getTypeName().equals(value.getClass().getTypeName()) || ignoreCheck) {
			this.value = value;
			return true;
		} else return false;
	}
	
	public Block getBlock() {
		return block;
	}
}