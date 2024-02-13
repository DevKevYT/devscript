package com.devkev.devscript.raw;

import com.devkev.devscript.nativecommands.NativeLibrary;

/**Not to be confused with script errors.
 * This exception is reserved for java exceptions that occur while starting or executin a DevScript script.
 * Script errors may be catched using the "try [block]" command @see {@link NativeLibrary}
 */
public class ScriptHostException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public ScriptHostException(String message) {
		super("An unhandled Java exception occurred while trying to start/during the DevScript script execution:\n\t" + message + "\nYou can find more info in the stack trace");
	}
}