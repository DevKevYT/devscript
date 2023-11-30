package com.devkev.devscript.raw;


import java.util.ArrayList;
import static com.devkev.devscript.raw.ProcessUtils.panic;

import com.devkev.devscript.raw.ProcessUtils.Type;

public abstract class Command {
    
	public String name;
    public String description;
    public String argumentsAsString;
    public DataType[] arguments;  
    public String[] argumentDescription;
    public int argumentCount;
    public boolean no_arguments;//, no_return_value;
    public static final int MIN_NAME_LENGTH = 1;
    public static final int MAX_NAME_LENGTH = 20;
    public int commandNameOffset = 0;
    
    private Property[] properties = new Property[] {};
    private boolean repeated = false; //If the ... argument type was used
    
    public static final String ARRAY_INDICATOR = "@";
    
    class Argument {
    	DataType type;
    	String description;
    }
    
    /**@param name - The name of the command.
     * @param Arguments as DataTypes read the manual for syntax"
     * @param usage - A short description of the command and the argument use.*/
    public Command(String name, String arguments, String usage) {
    	init(name, arguments, usage, 0);
    }
    
    /**@param name - The name of the command.
     * @param Arguments as DataTypes read the manual for syntax"
     * @param usage - A short description of the command and the argument use.
     * @param args - Allows for additional custom properties for the command. For example limit execution rights.*/
    public Command(String name, String arguments, String usage, Property ... settingsArgs) {
    	init(name, arguments, usage, 0, settingsArgs);
    }
    
    /**Arguments: the data types separated with a space character. use @ at start or end of argument to only accept arrays of the specified type.
     * "string @string" or "? @?" You can look up all DataTypes and their meaning at {@link ProcessUtils.Type}
     * @param args - Allows for additional custom arguments for the command. For example limit execution rights.*/
    public Command(String name, String arguments, String usage, int commandNameOffset, Property ... settingsArgs) {
    	init(name, arguments, usage, commandNameOffset, settingsArgs);
    }
    
    private void init(String name, String arguments, String usage, int nameOffset, Property ... settingsArgs) {
    	if(name.length() < MIN_NAME_LENGTH) panic("Command names need to have at least " + MIN_NAME_LENGTH + " character(s)!");
        if(name.length() >= MAX_NAME_LENGTH) panic("Command names need to have less than " + MIN_NAME_LENGTH + " characters!");
        if(Alphabet.partOf(name) && (!name.contains("<") && !name.contains(">")) || name.contains(" ")) panic("Command '" + name + "' contains illegal/reserved characters!");
        
        this.name = name;
        this.description = usage;
        this.argumentsAsString = arguments;
        arguments = prepare(arguments.toLowerCase());
        ArrayList<DataType> args = new ArrayList<DataType>();
        if(!arguments.isEmpty() && !arguments.contains(Type.NULL.typeName)) {
        	String[] splitted = arguments.split(" ");
        	for(int i = 0; i < splitted.length; i++) {
        		Type t = Type.getType(splitted[i].replaceAll(ARRAY_INDICATOR, "")); //Remove all array indicators
            	if(t == null) panic("Invalid type name for argument " + i + "(" + splitted[i] + ") on command: " + name);
            	if(t == Type.INTEGER || t == Type.FLOAT || t == Type.NUMBER) t = Type.STRING;
            	if(t == Type.NULL) continue;
            	if(t == Type.CONTINUE && args.size() > 0) {
            		repeated = true;
            		if(nameOffset > args.size()) panic("Cannot insert command name after infinite arguments");
            		break;
            	} else if(t == Type.CONTINUE && args.size() == 0) panic("Need to know wich argument type to repeated at command: " + name);
            	if(t != Type.CONTINUE) args.add(new DataType(t, splitted[i].contains(ARRAY_INDICATOR)));
        	}
        	no_arguments = false;
        } else no_arguments = true;
        	
        argumentCount = args.size();
        this.arguments = args.toArray(new DataType[args.size()]);
        if(nameOffset > this.arguments.length) {
        	System.out.println("Correcting");
        	commandNameOffset = this.arguments.length;
        } else this.commandNameOffset = nameOffset;
        
        if(settingsArgs.length > 0)
        	this.properties = settingsArgs;
    }
    
    public Object getOrProperty(String name, Object fallback) {
    	for(Property p : properties) {
    		if(p.getName().equals(name)) return p.getData();
    	}
    	return fallback;
    }
    
    public Property[] getProperties() {
    	return properties;
    }
    
    public boolean repeated() {
    	return repeated;
    }
    
    /**True, if the argument is a integer and not a float*/
    public boolean isInteger(String argument) {
    	return ProcessUtils.testForWholeNumber(argument);
    }
    
    /**True, if the argument is a float or integer*/
    public boolean isFloat(String argument) {
    	return ProcessUtils.testForFloat(argument);
    }
    
    public static String prepare(String arguments) {
    	StringBuilder prepArgs = new StringBuilder();
    	for(int i = 0; i < arguments.length(); i++) {
    		if(arguments.charAt(i) != ' ') prepArgs.append(arguments.charAt(i));
    		else if(i > 0) {
    			if(arguments.charAt(i) == ' ' && arguments.charAt(i - 1) != ' ') prepArgs.append(arguments.charAt(i));
    		}
    	}
    	
    	if(prepArgs.length() == 0) return prepArgs.toString();
    	if(prepArgs.charAt(prepArgs.length() - 1) == ' ') prepArgs.deleteCharAt(prepArgs.length() - 1);
    	return prepArgs.toString();
    }
    
    /**@param Block - The block, the command is executed in. No block = null*/
    public abstract Object execute(final Object[] args, final Process application, Block block) throws Exception;
}
