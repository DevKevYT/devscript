package com.devkev.devscript.raw;

public class Variable {	
	
	public final String name;
	Object value;
	//If false, the value of this variable can't be modified or removed
	public final boolean canBeChanged;
	boolean permanent = true;
	Block block; //Stack access never null If block == null, the variable might have been initialized when the process is not running -> block will be main on startup
	
	/**@param block Holds the stack access and important, for garbage collection. Can be null
	 * @param permanent - If the variable should get cleared, after the process finished. Useful for command lines*/
	public Variable(String name, Object value, boolean canBeChanged, boolean permanent, Block block) {
		this.name = name;
		this.value = value;
		this.canBeChanged = canBeChanged;
		this.block = block;
		this.permanent = permanent;
		if(block == null) throw new ScriptHostException("Can't declare a non-permanent variable with a null block while process is running");
	}
	
	/**@return false, if the variable assertion failed*/
	boolean setValue(Object value, boolean ignoreCheck) {
		if(!canBeChanged && !permanent) return false;
		if(value.getClass().getTypeName().equals(value.getClass().getTypeName()) || ignoreCheck) {
			this.value = value;
			return true;
		} else return false;
	}
	
	public Block getBlock() {
		return block;
	}
}