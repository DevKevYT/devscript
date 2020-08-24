package com.devkev.devscript.raw;

public interface Output {
	/**Output of the application*/
	void log(String message, boolean newline);
	/**Error messages. Usually the application is terminated, after an exception is thrown*/
	void error(String message);
	/**Extra stream for warnings and tips*/
	void warning(String message);
}
