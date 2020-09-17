package com.devkev.devscript.raw;

public interface ApplicationListener {
	/**Exit codes are: <br>{@link Block#DONE}<br> {@link Block#ERROR}*/
	public void done(int exitCode);
}
