package com.devkev.devscript.raw;

import java.util.ArrayList;

public abstract class Library {

	public final String COMPATIBLE_VERSION = "1.9.1";
    private final ArrayList<Command> collection = new ArrayList<Command>();
    private String name = "";
    
    public abstract Command[] createLib();
    
    public Library(String name) {
    	this.name = name;
    	Command[] lib = createLib();
    	if(lib != null) addCommand(lib);
    }


    public static final Library merge(String mergedName, Library... lib) {
        Library merge = new Library(mergedName) {
            @Override
            public Command[] createLib() {
                return null;
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
