package com.devkev.devscript.nativecommands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Random;

import com.devkev.devscript.raw.Alphabet;
import com.devkev.devscript.raw.ProcessUtils;
import com.devkev.devscript.raw.Array;
import com.devkev.devscript.raw.Block;
import com.devkev.devscript.raw.Command;
import com.devkev.devscript.raw.ConsoleMain;
import com.devkev.devscript.raw.DataType;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;
import com.devkev.devscript.raw.Process.HookedLibrary;
import com.devkev.devscript.raw.Variable;
import com.devkev.devscript.raw.ProcessUtils.Type;

public class NativeLibrary extends Library {
	
	public BufferedReader execErrorReader;
	public BufferedReader execReader;
	public java.lang.Process process;
	
	public NativeLibrary() {
		super("Native");
	}

	@Override
	public Command[] createLib() {
		return new Command[] {
				new Command("println", "??? ...", "Prints any object's toString() method in a new line") { 
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						for(Object d : args) application.log(d == null ? "null" : d.toString(), false);
						application.log("", true);
						return null;
					}
				},
				
				new Command("print", "??? ...", "Prints any objecs's toString() method") { 
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						for(Object d : args) application.log(d == null ? "null" : d.toString(), false);
						return null;
					}
				},
				
				new Command("=", "string ???", "Defines a variable. Access it with $variableName", 1) {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Block dotScope = block;
						
						ArrayList<String> objDots = new ArrayList<String>();
						//At first, check for dots and chain them
						String dotVarname = "";
						for(int j = 0; j < args[0].toString().length(); j++) {
							if(args[0].toString().charAt(j) == Alphabet.OBJECT_DOT) {
								objDots.add(dotVarname);
								dotVarname = "";
								continue;
							}
							dotVarname += args[0].toString().charAt(j);
						}
						if(!dotVarname.isEmpty()) objDots.add(dotVarname.trim());
						
						for(int i = 0; i < objDots.size(); i++) {
							String currentDot = objDots.get(i);
							ArrayList<String> indexChain = new ArrayList<String>();
							boolean atIndex = false;
							StringBuilder realVarName = new StringBuilder();
							for(int j = 0; j < currentDot.length(); j++) {
								char c = currentDot.charAt(j);
								
								if(c == Alphabet.ARR_0) {
									String index = Process.findMatching(currentDot, j, 0, Alphabet.ARR_0, Alphabet.ARR_1, Alphabet.ESCAPE, true);
									indexChain.add(index);
									j += index.length();
									atIndex = true;
								}
								if(!atIndex) realVarName.append(c);
							}
							if(realVarName.length() == 0) realVarName.append(currentDot);
							
							Object variableValue = dotScope.getVariable(realVarName.toString());
							
							if(!indexChain.isEmpty()) {
								Array lastArrayPointer = null;
								int lastIndexPointer = 0;
								for(String s : indexChain) {
									ArrayList<Object> indexArgument = application.interpretArguments(new StringBuilder(s), 0, false, block);
									if(indexArgument == null) {
										application.kill(block, "Error trying to fetch array index for variable: " + realVarName + " (Failed to interpret index argument: [" + s + "] )");
										return null;
									}
									if(indexArgument.isEmpty()) {
										application.kill(block, "Index for variable: \"" + realVarName + "\" needs to be a positive integer (empty)");
										return null;
									}
									Object index = indexArgument.get(0);
									if(index == null) {
										application.kill(block, "Index must be an integer");
										return null;
									}
									boolean canBeInteger = ProcessUtils.testForWholeNumber(index.toString());
									if(!canBeInteger) {
										application.kill(block, "Index for variable: \"" + realVarName + "\" needs to be a positive integer (" + index + ")");
										return null;
									}
									int value = Integer.valueOf(index.toString());
									if(value < 0) {
										application.kill(block, "Index for variable: \"" + realVarName + "\" needs to be a positive integer (" + index + ")");
										return null;
									}
									if(variableValue == null) {
										Array array = new Array();
										for(int k = 0; k < value; k++) array.push(Process.UNDEFINED);
										dotScope.setVariable(realVarName.toString(), array, false, false, true);
										variableValue = dotScope.getVariable(realVarName.toString());
									} else if(!(variableValue instanceof Array)) {
										application.kill(block, "Trying to get index from variable \"" + realVarName + "\" with type: " + ProcessUtils.toDataType(variableValue).type);
										return null;
									}
									if(value >= ((Array) variableValue).getIndexes().size()) {
										for(int k = ((Array) variableValue).getIndexes().size(); k < value+1; k++) 
											((Array) variableValue).push(Process.UNDEFINED);
									}
									lastIndexPointer = value;
									lastArrayPointer = ((Array) variableValue);
									variableValue = ((Array) variableValue).getIndexes().get(value);
								}
								if(lastArrayPointer != null) {
									if(i == objDots.size()-1) {
										if(args[1] instanceof Block) 
											((Block) args[1]).setAsFunction();
										lastArrayPointer.getIndexes().set(lastIndexPointer, args[1]);
										return null;
									} else variableValue = lastArrayPointer.getIndexes().get(lastIndexPointer);
								}
							}
							
							if(objDots.size() > 1) {
								if(i < objDots.size()-1) {
									if(variableValue == null) {
										application.kill(block, "Cannot access properties of non existing variable \"" + realVarName.toString() + "\"");
										return null;
									} else if(!(variableValue instanceof Block)) {
										application.kill(block, "Cannot access properties of non-object variable \"" + realVarName.toString() + "\"");
										return null;
									} else {
										if(!((Block) variableValue).isObject()) {
											application.kill(block, "Cannot access properties of non-object variable \"" + realVarName.toString() + "\"");
											return null;
										} else dotScope = (Block) variableValue;
									}
								} else {
									if(i == objDots.size()-1) {
										dotScope.setVariable(realVarName.toString(), args[1], false, false, true);
										return null;
									}
								}
							}
						}
						
						if(args[1] instanceof Block) 
							((Block) args[1]).setAsFunction();
						application.setVariable(args[0].toString(), args[1], false, false, block);
						return null;
					}
				},
				
				new Command("cos", "string", "Cosine function (Argument in degrees from 0 to 360)") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(ProcessUtils.testForFloat(args[0].toString())) {
							double num = Double.valueOf(args[0].toString().replace(",", "."));
							double value = Math.cos(num * (Math.PI / 180d));
							return String.valueOf((float) Math.round(value * 100000d) / 100000d);
						} else application.kill(block, "Command only accepts numbers");
						return null;
					}
				},
				
				new Command("sin", "string", "Sin function (Argument in degrees from 0 to 360)") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(ProcessUtils.testForFloat(args[0].toString())) {
							double num = Double.valueOf(args[0].toString().replace(",", "."));
							double value = Math.sin(num * (Math.PI / 180d));
							return String.valueOf((float) Math.round(value * 100000d) / 100000d);
						} else application.kill(block, "Command only accepts numbers");
						return null;
					}
				},
				
				new Command("tan", "string", "Tan function (Argument in degrees from 0 to 360)") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(ProcessUtils.testForFloat(args[0].toString())) {
							double num = Double.valueOf(args[0].toString().replace(",", "."));
							double value = Math.tan(num * (Math.PI / 180d));
							return String.valueOf((float) Math.round(value * 100000d) / 100000d);
						} else application.kill(block, "Command only accepts numbers");
						return null;
					}
				},
				
				new Command("sqrt", "string", "Calculates the square root") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(ProcessUtils.testForFloat(args[0].toString())) {
							double num = Double.valueOf(args[0].toString().replace(",", "."));
							double value = Math.sqrt(num);
							return String.valueOf((float) Math.round(value * 100000d) / 100000d);
						} else application.kill(block, "Command only accepts numbers");
						return null;
					}
				},
				
				new Command("try", "block", "Tries to execute the block and ignores errors. Blocks that inherid this block are also affected.") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						((Block) args[0]).setAsTryCatch();
						application.executeBlock((Block) args[0], false);
						return null;
					}
				},
				
				new Command("+", "??? ???", "Adds two numbers and returns the result, if either of the arguments is not a number, two strings are added. If both arguments are array, the two arrays are concatenated", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args[0] instanceof Array && args[1] instanceof Array) {
							Array concatenated = (Array) args[0];
							concatenated.getIndexes().addAll(((Array) args[1]).getIndexes());
							concatenated.updateArraytype();
							return concatenated;
						} else if(args[0] instanceof Array && !(args[1] instanceof Array)) {
							//Add an element to an array
							Array arr = (Array) args[0];
							arr.push(args[1]);
							return arr;
						}
						
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							return args[0].toString() + args[1].toString();
						}					
						
						if(ProcessUtils.testForWholeNumber(args[0].toString()) && ProcessUtils.testForWholeNumber(args[1].toString())) {
							if(args[0].toString().length() <= 19 && args[1].toString().length() <= 19) {
								return String.valueOf(Long.valueOf(args[0].toString()) + Long.valueOf(args[1].toString()));
							} else return args[0].toString() + args[1].toString();
						} else { //Only valid float values remain
							float f1 = Float.valueOf(args[0].toString());
							float f2 = Float.valueOf(args[1].toString());
							return String.format(java.util.Locale.US, "%.4f", (f1 + f2));
						}
					}
				},
				
				new Command("-", "string string", "Subtracts two numbers", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							application.kill(block, "Can only subtract numbers! (" + args[0].toString() + " - " + args[1].toString() + ")");
							return null;
						}
						
						if(ProcessUtils.testForWholeNumber(args[0].toString()) && ProcessUtils.testForWholeNumber(args[1].toString())) {
							if(args[0].toString().length() <= 19 && args[1].toString().length() <= 19) {
								return String.valueOf(Long.valueOf(args[0].toString()) - Long.valueOf(args[1].toString()));
							} else {
								application.kill(block, "Can only subtract numbers! (" + args[0].toString() + " - " + args[1].toString() + ")");
								return null;
							}
						} else { //Only valid float values remain
							float f1 = Float.valueOf(args[0].toString());
							float f2 = Float.valueOf(args[1].toString());
							return String.format(java.util.Locale.US, "%.4f", (f1 - f2));
						}
					}
				},
				
				new Command("*", "string string", "Multiplies two numbers", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							application.kill(block, "Can only multiply numbers! (" + args[0].toString() + " * " + args[1].toString() + ")");
							return null;
						}
						if(ProcessUtils.testForWholeNumber(args[0].toString()) && ProcessUtils.testForWholeNumber(args[1].toString())) {
							if(args[0].toString().length() <= 19 && args[1].toString().length() <= 19) {
								return String.valueOf(Long.valueOf(args[0].toString()) * Long.valueOf(args[1].toString()));
							} else {
								application.kill(block, "Can only multiply numbers! (" + args[0].toString() + " * " + args[1].toString() + ")");
								return null;
							}
						} else { //Only valid float values remain
							float f1 = Float.valueOf(args[0].toString());
							float f2 = Float.valueOf(args[1].toString());
							return String.format(java.util.Locale.US, "%.4f", (f1 * f2));
						}
					}
				},
				
				new Command("/", "string string", "Divides two numbers", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							application.kill(block, "Can only divide numbers! (" + args[0].toString() + " / " + args[1].toString() + ")");
							return null;
						}
						float f1 = Float.valueOf(args[0].toString());
						float f2 = Float.valueOf(args[1].toString());
						if(f2 == 0) {
							application.kill(block, "Division by zero (" + f1 + " / " + f2 + ")");
							return null;
						}
						return String.format(java.util.Locale.US, "%f", (f1 / f2));
					}
				},
				
				new Command("exec", "string", "Executes a shell command. Output only and limited to one command.") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
					String OS = System.getProperty("os.name", "generic").toLowerCase();
						
					if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
				    	//mac
						ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", args[0].toString());
						process = builder.start();
						
						execReader =  new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line = null;
						while ( (line = execReader.readLine()) != null) {
							application.log(line, true);
						}
						execReader.close();

						execErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
						line = null;
						String complete = "";
						while ( (line = execErrorReader.readLine()) != null) {
							application.log(line, true);
							complete += line;
						}
						execErrorReader.close();
						process.destroy();
						return complete;
						
					} else if (OS.indexOf("win") >= 0) {
						//windows
						ProcessBuilder builder = new ProcessBuilder("cmd", "/c", args[0].toString());
						process = builder.start();

						execReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line = null;
						while ( (line = execReader.readLine()) != null) {
							application.log(line, true);
						}
						execReader.close();

						execErrorReader =  new BufferedReader(new InputStreamReader(process.getErrorStream()));
						line = null;
						while ( (line = execErrorReader.readLine()) != null) {
							application.log(line, true);
						}
						execErrorReader.close();
						process.destroy();
						
					} else if (OS.indexOf("nux") >= 0) {
						//linux
						ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", args[0].toString());
						process = builder.start();
						
						execReader =  new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line = null;
						while ( (line = execReader.readLine()) != null) {
							application.log(line, true);
						}
						execReader.close();

						execErrorReader =  new BufferedReader(new InputStreamReader(process.getErrorStream()));
						line = null;
						while ( (line = execErrorReader.readLine()) != null) {
							application.log(line, true);
						}
						execErrorReader.close();
						process.destroy();
						
					} else application.error("Failed to determine operating system");
						return null;
					}
					
				},
				
				new Command("script", "string", "Executes a new script sub-process with its parent in- and outputs") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Process p = new Process(true);
						p.setInput(application.getInput());
						for (Output o : application.getOutput()) p.addOutput(o);
						p.execute(args[0].toString(), false);
						return null;
					}
				},
				
				new Command("length", "@?", "Returns the size of the array") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return String.valueOf(((Array) args[0]).getIndexes().size());
					}
				},
				
				new Command("pop", "@? string", "Removes the specified index of the array") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Array array = (Array) args[0];
						if(!ProcessUtils.testForWholeNumber(args[1].toString())) {
							application.kill(block, "Array index needs to be an integer");
							return null;
						}
						
						int indexToRemove = Integer.valueOf(args[1].toString());
						if(indexToRemove >= array.getIndexes().size() || indexToRemove < 0) {
							application.kill(block, "Array out of bounds ("+indexToRemove+" of "+array.getIndexes().size() + ")");
							return null;
						}
						
						array.getIndexes().remove(indexToRemove);
						return null;
					}
				},
				
				new Command("new-object", "block", "new-object <constructor>") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Block b = (Block) args[0];
						b.setAsObject();
						application.executeBlock(b, false);
						return b;
					}
				},
				
				new Command("object", "string block", "object <name> <constructor>") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Block b = (Block) args[1];
						b.setAsObject();
						application.executeBlock(b, false);
						application.setVariable(args[0].toString(), b, false, false, block);
						return null;
					}
				},
				
				new Command("push", "??? @? string", "Pushes a new value into the array at a specified index. push [value] [array] [index]") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Array arr = (Array) args[1];
						
						if(ProcessUtils.testForWholeNumber(args[2].toString())) {
							
							int value = Integer.valueOf(args[2].toString());
							
							if(value >= arr.getIndexes().size()) {
								for(int k = arr.getIndexes().size(); k < value; k++) {
									arr.push(Process.UNDEFINED);
								}
							}
							arr.push(args[0], value);
							
						} else application.kill(block, "Argument 2 needs to be an integer instead of " + args[2].toString());
						
						//May be legacy code, but this dosen't make any sense wtf
//						if(!ProcessUtils.typeMatch(typeArg1, new DataType(typeArg0.type, true)) && typeArg1.type != Type.NULL) {
//							application.kill(block, "Unable to push value type " + typeArg0.type + " into array with type " + typeArg1.type);
//							return null;
//						} else arr.push(args[0]);
						return null;
					}
				},
				
				new Command("push", "??? @?", "Pushes a new value into the array. push [value] [array]") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Array arr = (Array) args[1];
						DataType typeArg1 = ProcessUtils.toDataType(args[1]);
						DataType typeArg0 = ProcessUtils.toDataType(args[0]);
						
						if(!ProcessUtils.typeMatch(typeArg1, new DataType(typeArg0.type, true)) && typeArg1.type != Type.NULL) {
							application.kill(block, "Unable to push value type " + typeArg0.type + " into array with type " + typeArg1.type);
							return null;
						} else arr.push(args[0]);
						return null;
					}
				},
				
				new Command("lt", "string string", "Returns true, if argument 1 is less than 2 (If the arguments are not numbers, it checks the length of the string)", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Float arg0;
						Float arg1;
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							arg0 = (float) args[0].toString().length();
							arg1 = (float) args[1].toString().length();
						} else {
							arg0 = Float.valueOf(args[0].toString());
							arg1 = Float.valueOf(args[1].toString());
						}
						return arg0 < arg1;
					}
				},
				
				new Command("lteq", "string string", "Returns true, if argument 1 is less or equal than 2 (If the arguments are not numbers, it checks the length of the string)", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Float arg0;
						Float arg1;
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							arg0 = (float) args[0].toString().length();
							arg1 = (float) args[1].toString().length();
						} else {
							arg0 = Float.valueOf(args[0].toString());
							arg1 = Float.valueOf(args[1].toString());
						}
						return arg0 <= arg1;
					}
				},
				
				new Command("gteq", "string string", "Returns true, if argument 1 is grater or equal than 2 (If the arguments are not numbers, it checks the length of the string)", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Float arg0;
						Float arg1;
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							arg0 = (float) args[0].toString().length();
							arg1 = (float) args[1].toString().length();
						} else {
							arg0 = Float.valueOf(args[0].toString());
							arg1 = Float.valueOf(args[1].toString());
						}
						return arg0 >= arg1;
					}
				},
				
				new Command("gt", "string string", "Returns true, if argument 1 is grater than 2 (If the arguments are not numbers, it checks the length of the string)", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Float arg0;
						Float arg1;
						if(!ProcessUtils.testForFloat(args[0].toString()) || !ProcessUtils.testForFloat(args[1].toString())) {
							arg0 = (float) args[0].toString().length();
							arg1 = (float) args[1].toString().length();
						} else {
							arg0 = Float.valueOf(args[0].toString());
							arg1 = Float.valueOf(args[1].toString());
						}
						return arg0 > arg1;
					}
				},
				
				new Command("==", "? ?", "Returns true, if argument 1 and 2 are equal", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args[0] == null) {
							return args[1] == null; //If argument 1 is null, it is only equal if argument 2 is also null
						}
						if(args[1] == null) {
							return args[0] == null; //Same thing here
						}
						//Nothing can be null from here
						
						//Check if the values are numbers, otherwise 0.000 = 0 would return false
						if(ProcessUtils.testForFloat(args[0].toString()) && ProcessUtils.testForFloat(args[1].toString())) {
							float f1 = Float.valueOf(args[0].toString());
							float f2 = Float.valueOf(args[1].toString());
							return (f1 == f2);
						}
						
						return args[0].toString().equals(args[1].toString());
					}
				},
				
				new Command("!=", "? ?", "Returns true, if argument 1 and 2 are not equal", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return !args[0].toString().equals(args[1].toString());
					}
				},
				
				new Command("not", "boolean", "Inverts a boolean") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return !(Boolean) args[0];
					}
				},
				
				new Command("and", "boolean boolean ? ...", "Chain boolean conditions: if ($true and $true or $false) {...", 1) {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args.length == 2) return (Boolean) args[0] && (Boolean) args[1];
						else {
							boolean chain = (Boolean) args[0] && (Boolean) args[1];
							
							for(int i = 2; i < args.length; i+=2) {
								if(args[i].getClass().getTypeName().equals("java.lang.String") && i+1 <= args.length-1) {
									if(args[i+1].getClass().getTypeName().equals("java.lang.Boolean") && (args[i].toString().equals("and") || args[i].toString().equals("or"))) {
										chain = args[i].toString().equals("and") ? chain && (Boolean) args[i+1] : chain || (Boolean) args[i+1];
									} else application.kill(block, "Command and expects arguments the following pattern: <boolean> and <boolean> and <boolean> ...");
								} else application.kill(block, "Command and expects arguments the following pattern: <boolean> and <boolean> and <boolean> ...");
							}
							return chain;
						}
					}
				},
				
				new Command("random", "", "Returns a random number between 0 and 1") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Random r = new Random();
						return String.format(java.util.Locale.US,"%.4f", r.nextFloat());
					}
				},
				
				new Command("or", "boolean boolean ? ...", "Chain boolean conditions: if ($true or $true and $false) {...", 1) {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args.length == 2) return (Boolean) args[0] || (Boolean) args[1];
						else {
							boolean chain = (Boolean) args[0] || (Boolean) args[1];
							
							for(int i = 2; i < args.length; i+=2) {
								if(args[i].getClass().getTypeName().equals("java.lang.String") && i+1 <= args.length-1) {
									if(args[i+1].getClass().getTypeName().equals("java.lang.Boolean") && (args[i].toString().equals("and") || args[i].toString().equals("or"))) {
										chain = args[i].toString().equals("and") ? chain && (Boolean) args[i+1] : chain || (Boolean) args[i+1];
									} else application.kill(block, "Command and expects arguments the following pattern: <boolean> and <boolean> and <boolean> ...");
								} else application.kill(block, "Command and expects arguments the following pattern: <boolean> and <boolean> and <boolean> ...");
							}
							return chain;
						}
					}
				},
				
				new Command("int", "?", "Casts the given value into java.lang.Integer") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForFloat(args[0].toString())) {
							application.kill(block, "Unable to convert type " + args[0].getClass().getTypeName() + " into java.lang.Integer");
							return null;
						}
						float f = Float.valueOf(args[0].toString());
						return (int) f;
					}
				},
				
				new Command("long", "?", "Casts the given value into java.lang.Long") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForFloat(args[0].toString())) {
							application.kill(block, "Unable to convert type " + args[0].getClass().getTypeName() + " into java.lang.Integer");
							return null;
						}
						float f = Float.valueOf(args[0].toString());
						return (long) f;
					}
				},
				
				new Command("float", "?", "Casts the given object into java.lang.Float") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForFloat(args[0].toString())) {
							application.kill(block, "Unable to convert type " + args[0].getClass().getTypeName() + " into java.lang.Integer");
							return  null;
						}
						return Float.valueOf(args[0].toString());
					}
				},
				
				new Command("string", "?", "Casts the given value into java.lang.String") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return args[0].toString();
					}
				},
				
				new Command("call", "block ??? ...", "Executes a function (Variable that is a block: x = { function code... }. You can also pass arguments. Access them inside the block with $0 $1 etc...Returns the returned value of the function") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Block b = (Block) args[0];
						for(int i = 1; i < args.length; i++) application.setVariable(String.valueOf(i-1), args[i], false, false, b);
						application.executeBlock(b, true);
						return b.functionReturn;
					}
				},
				
				new Command("debug", "", "Just for breakpoints") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return null;
					}
				},
				
				new Command("listvars", "block ...", "Lists all variables from the block the command is executed in (If no block is specified)") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args.length == 0) {
							application.log("----------\nACCESSIBLE VARIABLES FOR BLOCK " + 
									(block == application.getMain() ? "[MAIN]" : "[" + block + "]") + " :", true);
							for(Variable var : block.getAccessibleVariables()) {
								application.log("Name: " + var.name + "\t\tBlock: " 
										+ (var.getBlock() == application.getMain() ? "[MAIN]" : "[" + var.getBlock() + "]")
										+ "\t\tValue: " + application.getVariable(var.name, var.getBlock()), true);
							}
							application.log("----------", true);
						} else if(args.length == 1) {
							application.log("----------\nACCESSIBLE VARIABLES FOR BLOCK " + args[0] + " :", true);
							for(Variable var : ((Block) args[0]).getAccessibleVariables()) {
								application.log("Name: " +  var.name + "\t\tBlock: " 
										+ (var.getBlock() == application.getMain() ? "[MAIN]" : "[" + var.getBlock() + "]")
										+ "\t\tValue: " + application.getVariable(var.name, var.getBlock()) + "\n----------", true);
							}	
							application.log("----------", true);
						}
						return null;
					}
				},
				
				new Command("?", "boolean ??? ???", "", 1) {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if((Boolean) args[0]) {
							return args[1];
						} else return args[0];
					}
				},
				
				new Command("if", "boolean block ? ...", "if $condition { } else {} | if $condition1 { } elseif $condition2 {} else {} ...") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if((Boolean) args[0]) {
							application.executeBlock((Block) args[1], true);
							return null;
						}
						for(int i = 2; i < args.length; i++) {
							if(args[i].equals("else")) {
								if(i + 1 <= args.length) {
									if(args[i+1] instanceof Block) {
										application.executeBlock((Block) args[i+1], true);
										i++;
									} else application.kill(block, "Expecting a block after boolean expression at argument " + (i + 1));
								}
								break;
							} else if(args[i].equals("elseif")) {
								if(i + 2 <= args.length) {
									if(args[i+1] instanceof Boolean) {
										if((Boolean) args[i+1]) {
											if(args[i+2] instanceof Block) {
												application.executeBlock((Block) args[i+2], true);
												return null;
											} else {
												application.kill(block, "Expecting a block after boolean expression at argument " + (i + 1));
												return null;
											}
										}
										i = i+2;
									} else {
										application.kill(block, "Expecting a boolean expression at argument " + (i + 1) + " after " + args[i].toString());
										return null;
									}
								}
							} else if(args[i] instanceof Block && i == 2) { //Compatibility reasons
								application.executeBlock((Block) args[i], true);
								break;
							} else {
								application.kill(block, "Unknown if- condition: '" + args[i].toString() + "'. Expecting 'else' or 'elseif' at argument " + (i + 1));
								return null;
							}
						}
						return null;
					}
				},
				
				new Command("ifnot", "boolean block", "Inverted If-statement") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!(Boolean) args[0]) application.executeBlock((Block) args[1], true);
						return null;
					}
				},
				
				new Command("for", "string string block", "For loop: for i 10 {...}") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForWholeNumber(args[1].toString())) {
							application.kill(block, "Argument 2 needs to be an integer!");
							return null;
						}
						
						String iterator = "0";
						Block b = (Block) args[2];
						b.setAsLoop();
						b.setVariable(args[0].toString(), iterator, false, false, true);
						
						int length = Integer.valueOf(args[1].toString());
						a: for(int i = 0; i < length; i++) {
							b.setVariable(args[0].toString(), String.valueOf(i), false, false, false);
							if(!b.interrupted()) {
								iterator = String.valueOf(i);
								application.executeBlock(b, i == length-1); //Only clear variables, when for loop finished
								if(i < length-1) i = Integer.valueOf(application.getVariable(args[0].toString(), b).toString());
							} else break a;
						}
						return null;
					}
				},
				
				new Command("break", "", "Breaks out of the next found loop in the stack. If no loop was found, this command interrupts the block") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						
						Block current = block;
						while(current.parent != null) {
							if(current.isLoop()) {
								current.interrupt();
								return null; //current is a loop block
							}
							current = current.parent;
						}
						
						block.interrupted = true; //Interrupt this block
						return null;
					}
				},
				
				new Command("return", "??? ...", "Searches the first occurrence of a block that is a function and returns its given value or null (Block execution gets terminated)") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Block current = block;
						boolean found = false;
						while(current != null) {
							//Also, interrupt loops
							if(current.isLoop()) 
								current.interrupt();
							if(current.isFunction()) {
								current.alive = false;
								if(args.length > 0)
									current.functionReturn = args[0];
								found = true;
								break;
							}
							current = current.parent;
						}
						if(!found) application.warning("The return command has no effect here.");
						return null;
					}
				},
				
				new Command("loop", "block", "Infinite loop. Use the break command inside it.") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Block b = (Block) args[0];
						b.setAsLoop();
						//b.beginLoop();
						while(true) {
							if(b.isAlive() || b.interrupted()) {
								application.garbageCollection(b);
								break;
							} 
							application.executeBlock(b, false);
						}
						b.clearVariables();
						return null;
					}
				},

				new Command("kill", "string ...", "Stops the application and throws an error message") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args.length > 0) application.kill(block, args[0].toString());
						else application.kill(block, "");
						return null;
					}
				},
				
				new Command("input", "", "Reads input from the set input stream") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						String input = application.waitForInput();
						if(input == null) return "";
						return input;
					}
				},
				
				new Command("typeof", "??? string", "Argument 2 is a string representation of the type like the command arguments. There are two adittional useful types: int and float", 1) {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						DataType checkType = DataType.toDataType(args[1].toString());
						if(checkType == null) {
							application.kill(block, "Unknown type: " + args[1].toString());
							return null;
						}
						
						if(checkType.type == Type.INTEGER) return ProcessUtils.testForWholeNumber(args[0].toString());
						else if(checkType.type == Type.FLOAT) return ProcessUtils.testForFloat(args[0].toString());
						else if(checkType.type == Type.NUMBER) return ProcessUtils.testForWholeNumber(args[0].toString()) || ProcessUtils.testForFloat(args[0].toString());
						else return ProcessUtils.typeMatch(ProcessUtils.toDataType(args[0]), checkType);
					}
				},
				
				new Command("wait", "string", "Pauses the script for x milliseconds") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForWholeNumber(args[0].toString())) application.kill(block, "Argument 1 needs to be an integer at command: wait <milliseconds>");
						Thread.sleep(Integer.valueOf(args[0].toString()));
						return null;
					}
					
				},
				
				new Command("thread", "string block", "Runs a separate thread along the process") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Thread t = new Thread(new Runnable() {
							public void run() {
								application.executeBlock((Block) args[1], true);
							}
						}, args[0].toString());
						application.setVariable(args[0].toString(), (Block) args[1], true, false, block);
						((Block) args[1]).thread = t;
						((Block) args[1]).setAsFunction();
						t.start();
						return null;
					}
				},
				
				new Command("kill", "block", "") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						((Block) args[0]).alive = false;
						return null;
					}
				},
				
				new Command("pause", "", "Pauses the block, the command is executed in") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						application.pause(block);
						return null;
					}
				},
				
				new Command("pause", "block", "Pauses the specified block, if it is running in a separate thread.") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(((Block) args[0]).thread == null) application.warning("Block has not thread attached. Pausing wont have any effect");
						if(!((Block) args[0]).alive) application.warning("Block is not active");
						application.pause(((Block) args[0]));
						return null;
					}
				},
				
				new Command("waitfor", "block", "Waits for a block to finish") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(((Block) args[0]).thread == null) {
							application.warning("Block has no thread attached.");
							return null;
						}
						((Block) args[0]).thread.join();
						return null;
					}
				},
				
				new Command("wake", "block", "Wakes the specified thread") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						application.wake((Block) args[0]);
						return null;
					}
				},
				
				new Command("alive", "block", "") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return ((Block) args[0]).alive;
					}
				},
				
				new Command("return", "block", "") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						((Block) args[0]).alive = false;
						return null;
					}
				},
				
				new Command("charAt", "string string", "[index] [string]") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForWholeNumber(args[0].toString())) {
							application.kill(block, "Argument 1 needs to be an integer!");
							return null;
						}
						int index = Integer.valueOf(args[0].toString());
						if(index < 0 || index > args[1].toString().length()) {
							application.kill(block, "String index out of Range (" + index + ") of " + args[1].toString().length());
							return null;
						}
						return String.valueOf(args[1].toString().charAt(index));
					}
				},
				
				new Command("stringLength", "string", "") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return String.valueOf(args[0].toString().length());
					}
				},
				
				new Command("toArray", "string", "") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						Array arr = new Array();
						for(char c : args[0].toString().toCharArray()) arr.push(String.valueOf(c));
						return arr;
					}
				},
				
				new Command("substring", "string string string", "[string] [begin] [end]") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForWholeNumber(args[1].toString()) || !ProcessUtils.testForWholeNumber(args[2].toString())) {
							application.kill(block, "Argument 1 and 2 need to be integer");
							return null;
						}
						return args[0].toString().substring(Integer.valueOf(args[1].toString()), Integer.valueOf(args[2].toString()));
					}
				},
				
				new Command("version", "", "Prints the version of the script") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						application.log(application.version, true);
						return null;
					}
				},
				
				new Command("help", "", "Prints all the available commands with a brief explanation and arguments") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(args.length == 0) {
							application.log(String.format("%-40s%s", "SYNTAX EXAMPLE", "DESCRIPTION"), true);
							application.log("", true);
							application.log("Listeners", true);
							application.log("onexit = {};  Fires when the application is finished. Useful for closing sockets etc.", true);
							application.log("", true);
							int maxLength = 0;
							for(HookedLibrary lib : application.getLibraries()) {
								application.log("\nLIBRARY: '" + lib.name + "' (" + lib.commands.length + (lib.commands.length > 1 ? " commands)\n" : " command)\n"), true);
								for(Command c : lib.commands) {
									String example = "";
									if(!c.no_arguments) {
										for(int i = 0; i < c.argumentCount; i++) {
											if(i == c.commandNameOffset) {
												example += c.name + " ";
								  			} 
											example += "[" + c.arguments[i].type.toString() + "] ";
										}
									} else example += c.name;
									if(example.length() > maxLength) maxLength = example.length();
									application.log(String.format("\t%-40s%s", example, c.description), true);
								}
							}
						}
						return null;
					}
				},
				
				new Command("import", "string", "Imports a library from a compiled .jar file. The class should extend com.mygdx.devkev.devscript.raw.Library and be named CustomLibrary\n"
						+ "You can use a * to reference the current path the process is executed in (*/library.jar") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!args[0].toString().endsWith(".jar")) args[0] += ".jar"; 
						File fileToImport;
						if(!args[0].toString().startsWith("*")) fileToImport = new File(args[0].toString());
						else {
							File jarFile = new File(URLDecoder.decode(ConsoleMain.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));
							fileToImport = new File(jarFile.getParent() + args[0].toString().substring(1));
						}
						
						if(!fileToImport.exists()) {
							application.kill(block, "File to import not found: " + fileToImport.getPath());
							return null;
						}
						
						java.net.URI uri = fileToImport.toURI();
						java.net.URL url = uri.toURL();
							
						Library lib;
						try {
							@SuppressWarnings("resource")
							ClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {url});
							Class<?> pluginClass = loader.loadClass("Library");
							
							lib = (Library) pluginClass.getConstructor().newInstance();
							lib.bound = application;
							lib.scriptImport(application);
							
						} catch(Exception e) {
							application.kill(block, "Java error occurred, while importing library: " + e.toString());
							return null;
						}
						
						if(!lib.COMPATIBLE_VERSION.equals(application.version)) {
							application.warning("Incopatible versions detected: " + lib.COMPATIBLE_VERSION + " Application Version: " + application.version + " Library or Application may be obsolete!");
						}
						
						application.includeLibrary(lib);
						return null;
					}
				},
				
				new Command("use", "string", "use <string> Inserts the given code. (Tip: Usefil in combination with 'readFile'!)") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						block.blockCode.insert(block.executeIndex, args[0].toString());
						return null;
					}
				},
				
				new Command("readFile", "obj", "readFile <file> Returns the file as string") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						File file = (File) args[0];
						if(file.isDirectory()) {
							application.kill(block, "Unable to read directory!");
							return null;
						}
						StringBuilder content = new StringBuilder();
						BufferedReader reader = new BufferedReader(new FileReader(file));
						String line = reader.readLine();
						while(line != null) {
							content.append(line);
							line = reader.readLine();
						}
						reader.close();
						return content.toString();
					}
					
				},
				
				new Command("readFileLines", "obj", "readFile <file> Returns the file lines as string array") {
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						File file = (File) args[0];
						if(file.isDirectory()) {
							application.kill(block, "Unable to read directory!");
							return null;
						}
					
						Array lines = new Array();
						BufferedReader reader = new BufferedReader(new FileReader(file));
						String line = reader.readLine();
						while(line != null) {
							lines.push(line == null ? "" : line);
							line = reader.readLine();
						}
						reader.close();
						return lines;
					}
					
				},
				
				new Command("getFile", "string", "Returns a java.io.File object. Use */ to reference the current directory.") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						File file;
						if(args[0].toString().startsWith("*")) {
							if(application.file == null) {
								File dir = new File(URLDecoder.decode(ConsoleMain.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));
								file = new File(dir.getParent() + args[0].toString().substring(1));
							} else file = new File(application.file.getParent() + args[0].toString().substring(1));
						} else file = new File(args[0].toString());
						return file;
					}
				},
				
				new Command("fileExists", "obj", "Checks if a file exists") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return ((File) (args[0])).exists();
					}
				},
				
				new Command("deleteFile", "obj", "Deletes a file. You can use this command, if you want to clear a files content and append lines with the writeFileLine command") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						((File) (args[0])).delete();
						return null;
					}
				},
				
				new Command("writeFileLine", "obj string", "[file] [content] Appends a new line to the file") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						File file = ((File) (args[0]));
						file.createNewFile();
						BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						writer.append(args[1].toString());
						writer.close();
						return null;
					}
				},
				
				new Command("listDirectory", "obj", "Returns an array containing all files inside this directory") {
					@Override
					@SuppressWarnings("all")
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return new Array(((File) (args[0])).listFiles());
					}
				},
				
				new Command("isDirectory", "obj", "") {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						return ((File) (args[0])).isDirectory();
					}
				},
				
				new Command("%", "string string", "[STRING] % [STRING]", 1) {
					@Override
					public Object execute(Object[] args, Process application, Block block) throws Exception {
						if(!ProcessUtils.testForWholeNumber(args[0].toString()) || !ProcessUtils.testForWholeNumber(args[1].toString())) {
							application.kill(block, "This command can only handle integers!");
							return null;
						}
						if(args[0].toString().length() <= 19 && args[1].toString().length() <= 19) {
							return String.valueOf(Long.valueOf(args[0].toString()) % Long.valueOf(args[1].toString()));
						} else {
							application.kill(block, "Can only % numbers!");
							return null;
						}
					}
				}
		};
	}

	@Override
	public void scriptImport(Process process) {}

	@Override
	public void scriptExit(Process process, int exitCode, String errorMessage) {
			if(execErrorReader != null) {
				try {
					execErrorReader.close();
					execErrorReader = null;
					System.out.println("Reader closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(execReader != null) {
				try {
					execReader.close();
					execReader = null;
					System.out.println("Reader closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(this.process != null) {
				this.process.destroy();
			}
	}
}
