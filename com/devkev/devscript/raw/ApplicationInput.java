package com.devkev.devscript.raw;

import java.io.IOException;
import java.io.InputStream;

public abstract class ApplicationInput extends InputStream {
	
	private volatile boolean inputReqested = false;
	
	private String data = "";
	private int index = 0;
	
	@Override
	public int read() throws IOException {
		if(!inputReqested) {
			inputReqested = true;
			awaitInput();
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		index++;
		if(index < data.length() && !data.isEmpty()) {
			return data.getBytes()[index-1];
		} else {
			inputReqested = false;
			data = "";
			index = 0;
			return -1;
		}
	}
	
	/**This function is called, before */
	public abstract void awaitInput();
	
	public synchronized boolean inputRequested() {
		return inputReqested;
	}
	
	public void flush(String data) {
		if(inputReqested) {
			this.data = data == null ? "" : data;
			synchronized (this) {
				notify();
			}
		}
	}
}
