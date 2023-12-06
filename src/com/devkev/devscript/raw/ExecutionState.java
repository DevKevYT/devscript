package com.devkev.devscript.raw;

/**This class describes wether a script block execution was successful or not.
 * Instead of calling the {@link Process#kill(Block, String)} method from within a block,
 * a execution state is reported as a return value of the function that executed a specific block.
 * Using this method, an exception can be thrown or controlled without specifying the {@link Block#setAsTryCatch()} method.*/
public class ExecutionState {

	public interface ExitCodes {
		public byte DONE = 0;
		public byte ERROR = 1;
	}
	
	public final byte exitCode;
	public final String stateMessage;
	public final Block block;
	
	public ExecutionState(Block block, byte exitCode, String stateMessage) {
		this.exitCode = exitCode;
		this.block = block;
		this.stateMessage = stateMessage;
	}
	
	
	
}
