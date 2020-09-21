package com.devkev.devscript.raw;

import java.util.ArrayList;

public class Block { //So stupid...
	
	public static final byte DONE = 0;
	public static final byte ERROR = 1;
	
	public final StringBuilder blockCode;
	public int executeIndex = 0;	//The current char index, that is executed or compiled. 0 < ExecuteIndex < blockCode.length() 
	String currentCommand = "";
	public Thread thread;  	//A block can also be attached to a thread. This variable would store the thread, the blockCode is executed in.
						//Be careful. May be null
	public volatile boolean alive = false; //Commands are only executed, if the block is alive
	public volatile boolean interrupted = false; //If true, it stays true, until next block execution
	public volatile boolean continued = false;
	
	public Block parent;
	public volatile boolean isFunction = false; //This flag gets set to true, if a variable containing a block (= function) is made in the command = (function = {#some code#})
	public Object functionReturn = null;
	private byte stack = 0;
	
	public boolean loop = false;
	ArrayList<Command> cached = new ArrayList<Command>(0);
	int exitCode = DONE;
	
	Block(StringBuilder blockCode, Block parent) {
		this.blockCode = blockCode;
		
		if(parent != null) {
			this.parent = parent;
			if(parent.stack + 1 > Byte.MAX_VALUE) throw new IllegalStateException("Block stack exceeded max stack count: " + Byte.MAX_VALUE);
			this.stack = (byte) (parent.stack + 1);
		} else this.stack = 0;
	}
	
	public synchronized void interrupt() {
		interrupted = true;
	}
	
	public synchronized boolean interrupted() {
		return interrupted;
	}
	
	public byte getStack() {
		return stack;
	}
	
	public boolean isLoop() {
		return loop;
	}
	
	public void beginLoop() {
		loop = true;
	}
	
	public void endLoop(boolean clearcache) {
		loop = false;
		clearCache();
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
	
	public String toString() {
		return "BLOCK:" + stack + "" + (thread != null ? " Thread: " + thread.getName() : " Thread: none");
	}
}
