package com.devkev.devscript.raw;

import java.util.ArrayList;

public abstract class Library {

	public final String COMPATIBLE_VERSION = "1.10.0";
    private final ArrayList<Command> collection = new ArrayList<Command>();
    private String name = "";
    
    public Process bound;
    
    public abstract Command[] createLib();
    
    public Library(String name) {
    	this.name = name;
    	Command[] lib = createLib();
    	if(lib != null) addCommand(lib);
    }
    
    /**Called once when the library is initialized and the "import" command is called at script runtime.*/
    public abstract void scriptImport(Process process);

    /**Called when the JVM exits or the script finished
     * Exit codes: 0 = ok, 1 = error*/
    public abstract void scriptExit(Process process, int exitCode, String errorMessage);
    
    
    public static final Library merge(String mergedName, Library... lib) {
        Library merge = new Library(mergedName) {
            @Override
            public Command[] createLib() {
                return null;
            }

			@Override
			public void scriptImport(Process process) {
			}

			@Override
			public void scriptExit(Process process, int exitCode, String errorMessage) {
			}
        };
        for(Library l : lib) merge.collection.addAll(l.collection);
        return merge;
    }

    public final Command[] getCollection() {
        return collection.toArray(new Command[collection.size()]);
    }

    public final void addCommand(Command... command) {
        for(Command c : command) collection.add(c);
    }

    public final void addLibrary(Library library) {
        for(Command c : library.collection) collection.add(c);
    }
    
    public final String getName() {
    	return name;
    }
    
    public String toString() {
    	return name;
    }
    
    /**Manually executes a function while the process is running<br>
     * The function needs to be in the main block.*/
    public void executeEventFunction(String functionName, Object ... variables) throws IllegalAccessError {
    	if(bound != null) {
    		if(bound.isRunning()) {
    			Object obj = bound.getVariable(functionName, null);
    			if(obj != null) {
    				if(obj instanceof Block) {
    					Block b = (Block) obj;
    					bound.executeBlock(b, true, variables);
    					
    				} else throw new IllegalAccessError("Variable " + functionName + " is not a function!");
    			} else throw new IllegalAccessError("Event funtion with the name " + functionName + " not registered!");
    		} else throw new IllegalAccessError("Process not running!");
    	} else throw new IllegalAccessError("Library was not added to any processes!");
    }
    
    public final boolean removeCommand(String commandName) {
        for(Command c : collection) {
            if(c.name.equals(commandName)) {
                collection.remove(c);
                return true;
            }
        }
        return false;
    }
}
