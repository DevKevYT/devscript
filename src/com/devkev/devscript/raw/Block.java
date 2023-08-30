package com.devkev.devscript.raw;

import java.util.ArrayList;

import com.devkev.devscript.raw.ProcessUtils.ExitCodes;

public class Block { //So stupid...
	
	private Process host;
	private ArrayList<Variable> variables = new ArrayList<>();
	
	public final StringBuilder blockCode;
	public int executeIndex = 0;	//The current char index, that is executed or compiled. 0 < ExecuteIndex < blockCode.length() 
	String currentCommand = "";
	public Thread thread;  	//A block can also be attached to a thread. This variable would store the thread, the blockCode is executed in.
						//Be careful. May be null
	public volatile boolean alive = false; //Commands are only executed, if the block is alive
	public volatile boolean interrupted = false; //If true, it stays true, until next block execution
	public volatile boolean continued = false;
	
	public Block parent;
	private byte stack = 0;
	
	ArrayList<Command> cached = new ArrayList<>(1);
	int exitCode = ExitCodes.DONE;
	
	//Block flags
	//This flag gets set to true, if a variable containing a block (= function) is made in the command = (function = {#some code#}) (Flag: F)
	private volatile boolean isFunction = false;
	public Object functionReturn = null;
	
	//If the block should isolate variables (A bit more complex than that) (Flag: O)
	private volatile boolean isConstrucor = false;
	
	//If true, errors dont cause the script to exit. (=> the block acts like a try/catch block.) (Flag: T)
	private volatile boolean isTryCatch = false;
	
	//True if the block belongs to a loop. This will affect return, continue and break commands. (Flag: L)
	private volatile boolean isLoop = false;
	
	class CompiledCommand {
		Command command;
		ArrayList<Object> args;
	}
	
	public Block(StringBuilder blockCode, Block parent, Process host) {
		this.blockCode = blockCode;
		this.host = host;
		
		if(parent != null) {
			this.parent = parent;
			if(parent.stack + 1 > Byte.MAX_VALUE) throw new IllegalStateException("Block stack exceeded max stack count: " + Byte.MAX_VALUE);
			this.stack = (byte) (parent.stack + 1);
		} else this.stack = 0;
	}
	
	public boolean inheritTryCatch() {
		Block current = this;
		while(current.parent != null) {
			if(current.parent == null) return current.isTryCatch;
			else if(current.parent == host.main) return current.isTryCatch;
			else {
				current = current.parent;
				if(current.isTryCatch()) return true;
			}
		}
		return current.isTryCatch;
	}
	
	public boolean isTryCatch() {
		return isTryCatch;
	}
	
	public void setAsTryCatch() {
		isTryCatch = true;
	}
	
	public boolean isObject() {
		return isConstrucor;
	}
	
	public void setAsObject() {
		isConstrucor = true;
	}
	
	public boolean isFunction() {
		return isFunction;
	}
	
	public void setAsFunction() {
		isFunction = true;
	}
	
	public synchronized void interrupt() {
		interrupted = true;
		isLoop = false;
	}
	
	public synchronized boolean interrupted() {
		return interrupted;
	}
	
	public byte getStack() {
		return stack;
	}
	
	public boolean isLoop() {
		return isLoop;
	}
	
	public void setAsLoop() {
		isLoop = true;
	}
	
	public synchronized boolean isAlive() {
		return alive;
	}
	
	void addToCache(Command c) {
		for(int i = 0; i < cached.size(); i++) {
			if(c.equals(cached.get(i))) return;
		}
		cached.add(c);
	}
	
	void clearCache() {
		cached.clear();
	}
	
	private static void checkForBlockInheritage(Object obj, Block parent) {
		if(obj instanceof Block)
			((Block) obj).parent = parent;
	}
	
	/**@return true, if handled, that means a variable with the name was found or a variable was created*/
	public boolean setVariable(String name, Object value, boolean FINAL, boolean permanent, boolean allowCreation) {
		if(name.equals("true")) return true;
		else if(name.equals("false")) return true;
		else if(name.equals("null")) return true;
		
		if(name.contains(" ") || Alphabet.partOf(name)) {
			host.kill(this, "Variable name contains illegal/reserved characters");
			return false;
		}
		
		for(Variable v : variables) {
			if(v.name.equals(name)) {
				if(!v.FINAL) {// || v.permanent) {
					if(!v.setValue(value, true)) {
						host.kill(this, "Failed to assign type " + value + " to existing variable " + v.value);
						checkForBlockInheritage(value, this);
					}
				}
				return true;
			}
		}
		//If not found, check the parents!
		if(parent != null && !isConstrucor) {
			if(parent.setVariable(name, value, FINAL, permanent, false)) {
				checkForBlockInheritage(value, this);
				return true;
			}
		}
		
		if(allowCreation) {
			this.variables.add(new Variable(name, value, FINAL, permanent, this));
			checkForBlockInheritage(value, this);
			return true;
		}
		return false;
	}
	
	public void clearVariables() {
		variables.clear();
	}
	
	public void removeVariable(String name) {
		for(Variable v : variables) {
			if(v.name.equals(name) && !v.FINAL) {
				variables.remove(v);
				return;
			}
		}
		//Otherwise look for the next stack if the block is not a constructor (aka "isolated")
		if(!isConstrucor && parent != null)
			parent.removeVariable(name);
	}
	
	public ArrayList<Variable> getAccessibleVariables() {
		ArrayList<Variable> var = new ArrayList<>();
		var.addAll(variables);
		
		if(parent != null)
			var.addAll(parent.getAccessibleVariables());
		
		return var;
	}
	
	public ArrayList<Variable> getLocalVariables() {
		return variables;
	}
	
	public Object getVariable(String name) {
		//constants
		if(name.equals("true")) return Process.TRUE;
		else if(name.equals("false")) return Process.FALSE;
		else if(name.equals("null")) return Process.UNDEFINED;
		else if(name.equals("n")) return "\n";
		else if(name.equals("t")) return "\t";
		else if(name.equals("PI")) return "3.1415";
		
		for(Variable var : variables) {
			if(var.name.equals(name))
				return var.value;
		}
		//Otherwise search the parent
		if(parent != null)
			return parent.getVariable(name);
		else return null;
	}
	
	public Process getHost() {
		return host;
	}
	
	public String toString() {
		return "BLOCK:" + (isFunction ? "F" : "") + (isConstrucor ? "O" : "") + (isTryCatch ? "T" : "") + (isLoop ? "L" : "") + " " + stack
				+ (thread != null ? " Thread: " + thread.getName() : "");
					//+ " CODE [" + blockCode + "]";
	}
}
