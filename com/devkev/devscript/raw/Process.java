package com.devkev.devscript.raw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.devkev.devscript.nativecommands.NativeLibrary;

public class Process {

	private static long process_id = 0L;
	
	private ArrayList<GeneratedLibrary> libraries = new ArrayList<GeneratedLibrary>();
	private ArrayList<Output> output = new ArrayList<Output>(1); 
	private ArrayList<Variable> variables = new ArrayList<Variable>(5);
	private ApplicationListener listener;
	
	private static final boolean False = false;
	private static final boolean True = true;
	private static final Object Null = null;
	
	private BufferedReader inputReader;
	private InputStream inputStream;
	
	Block main;
	//Main block is not never in the list, since the process gets terminated, if main is killed.
	private final ArrayList<Block> aliveBlocks = new ArrayList<Block>(1); 
	boolean breakRequested = false;
	
	public long maxRuntime = 0; //Runtime in ms. If < 0, runtime is infinite
	private long currentChar = 0;
	public final String version = "1.9.1"; 
	
	/**The file, the script is executed from. May be null. Just useful for some Native commands*/
	public File file = null;
	
	/**Coming soon*/
//	public boolean debug = false;
//	public volatile int commands_executed = 0;
//	public volatile long process_start = 0;
//	public volatile long process_end = 0;
//	public volatile int execution_time = 0;
//	public volatile float average_commands_per_sec = 0;
	
	public class GeneratedLibrary {
		public final Command[] commands;
		public final String name;
		
		public GeneratedLibrary(Library library) {
			this.commands = library.createLib();
			this.name = library.getName();
		}
	}
	
	class Variable {	
		final String name;
		private Object value;
		final boolean FINAL;
		boolean permanent = true;
		Block block; //Stack access never null If block == null, the variable might have been initialized when the process is not running -> block will be main on startup
		
		/**@param block Holds the stack access and important, for garbage collection. Can be null
		 * @param permanent - If the variable should get cleared, after the process finished. Useful for command lines*/
		public Variable(String name, Object value, boolean FINAL, boolean permanent, Block block) {
			this.name = name;
			this.value = value;
			this.FINAL = FINAL;
			this.block = block;
			this.permanent = permanent;
			if(block == null && main != null) {
				if(main.alive) ApplicationBuilder.panic("Can't declare a non-permanent variable with a null block while process is running");
			}
		}
		
		/**@return false, if the variable assertion failed*/
		boolean setValue(Object value, boolean ignoreCheck) {
			if(FINAL && !permanent) return false;
			if(value.getClass().getTypeName().equals(value.getClass().getTypeName()) || ignoreCheck) {
				this.value = value;
				return true;
			} else return false;
		}
	}
	
	/**@param useCache - If the process should temprary save common used commands in a separate list.
	 * May improve performance on large scripts, but also increase the memory usage.
	 * <br>In worst case, the data usage of the command library could be doubled.
	 * <br>Use {@link Process#setCacheLimit(int limit)} to limit cache usage
	 * It also imports the NativeLibrary (basic commands like if,println ...) at default.
	 * If you don't want these native commands, call {@link Process#clearLibraries()} before adding your custom ones*/
	public Process(boolean useCache) {
		includeLibrary(new NativeLibrary());
	}
	
	public Thread execute(String script, boolean newThread) {
		if(main != null) {
			if(main.alive) {
				warning("Java Environment was trying to start a new process, while its already running");
				return null;
			}
		}
		
		script = script.replaceAll("\t", "");
		script = script.replaceAll("\r", "");
		script = script.replaceAll("\n", " ");
		
		main = new Block(new StringBuilder(script), null);
		main.thread = null;
		
		for(int i = 0; i < variables.size(); i++) {  //Remove non-permanent variables
			if(!variables.get(i).permanent) {
				variables.remove(i);
				i--;
			} else if(variables.get(i).block == null) variables.get(i).block = main;
		}
		currentChar = System.currentTimeMillis();
		
		process_id ++;
		if(newThread) {
			main.thread =  new Thread(new Runnable() {
				public void run() {
					start(main);
					main = null;
				}
			}, "process " + process_id);
			main.thread.start();
			return main.thread;
		} else {
			start(main);
			main = null;
		}
		return null;
	}
	
	public Thread execute(File file, boolean newThread) throws IOException {
		if(main != null) {
			if(main.alive) {
				warning("Java Environment was trying to start a new process, while its already running");
				return null;
			}
		}
		
		String code  = "";
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while(line != null) {
			code += line;
			line = reader.readLine();
		}
		reader.close();
		
		this.file = file;
		return execute(code, newThread);
	}
	
	private void start(Block block) {
		if(block == null) return;
		
//		process_start = System.currentTimeMillis();
//		commands_executed = 0;
//		average_commands_per_sec = 0;
		aliveBlocks.add(block);
		
		//char[] arr = block.blockCode.toCharArray();
		StringBuilder command = block.blockCode;
		
		block.exitCode = Block.DONE;
		block.alive = true;
		block.executeIndex = 0;
		block.interrupted = false;
		if(!checkForInterrupt(block)) return;
		
		while(block.executeIndex < block.blockCode.length()-1 && block.alive) {
			executeCommand(command, block.executeIndex, true, block);
		}
		
//		boolean inQuote = false;
//		boolean inComment = false;
//		short blockWrap = 0;
//		short arrayWrap = 0;
//		short paranthesisWrap = 0;
//		
//		short lStringWrap = 0;
//		
//		block.executeIndex = 0;
//		for(int i = 0; i < block.blockCode.length(); i++) {
//			block.executeIndex ++;
//			
//			char c = block.blockCode.charAt(i);
//			boolean ignore = false;
//			
//			if(c == Alphabet.LSTRING_0 && !inQuote) lStringWrap++;
//			else if(c == Alphabet.LSTRING_1 && !inQuote) lStringWrap--;
//			
//			if(lStringWrap == 0) {
//				if(c == Alphabet.STR_0 && !inQuote && (i > 0 ? block.blockCode.charAt(i-1) != Alphabet.ESCAPE : true)) inQuote = true;
//				else if(c == Alphabet.STR_0 && inQuote && (i > 0 ? block.blockCode.charAt(i-1) != Alphabet.ESCAPE : true)) inQuote = false;
//			
//				if(c == Alphabet.ARR_0 && !inQuote) arrayWrap++;
//				else if(c == Alphabet.ARR_1 && !inQuote) arrayWrap--;
//			
//				if(c == Alphabet.BRK_0 && !inQuote) paranthesisWrap++;
//				else if(c == Alphabet.BRK_1 && !inQuote) paranthesisWrap--;
//			
//				if(c == Alphabet.BLCK_0 && !inQuote) blockWrap++;
//				else if(c == Alphabet.BLCK_1 && !inQuote) blockWrap--;
//			
//				if(c == Alphabet.COMMENT && !inComment && !inQuote) inComment = true;
//				else if(c == Alphabet.COMMENT && inComment && !inQuote) {
//					inComment = false;
//					ignore = true;
//				}
//			}
//			
//			if(!inComment && !ignore && c != '\n') command.append(c);
//			
//			if(((c == Alphabet.BREAK || c == '\n') && !inQuote && blockWrap == 0 && lStringWrap == 0 && !inComment) || i >= block.blockCode.length()-1) {
//				if(command.toString().isEmpty() || command.length() == 1) continue;
//				block.currentCommand = command.toString();
//				if(inQuote) {
//					kill(block, "Syntax Error: Missing quote");
//					return;
//				} else if(blockWrap != 0) {
//					kill(block, "Syntax Error: Missing closing paranthesis");
//					return;
//				} else if(arrayWrap != 0) {
//					kill(block, "Syntax Error: Missing closing array paranthesis");
//					return;
//				} else if(inComment) {
//					kill(block, "Syntax Error: You may have forgot to close the comment quote (" + Alphabet.COMMENT + ")");
//					return;
//				} else if(paranthesisWrap != 0) {
//					kill(block, "Syntax Error: Missing or misplaced paranthesis");
//					return;
//				}
//				
//				if(command.charAt(command.length()-1) == Alphabet.BREAK) command.setLength(command.length() - 1);
//				if(command.length() == 0) continue;
//				
//				while(command.length() > 0) {
//					if(command.charAt(0) == ' ') {
//						command.deleteCharAt(0);
//						//block.blockCode.deleteCharAt(0);
//						//i++;
//					} else break;
//				}
//				
//				if(command.toString().isEmpty()) continue;
//				
//				executeCommand(command, block);
//				if(!checkForInterrupt(block)) break;
//				command.setLength(0);
//				//System.gc();
//			}
//		}
		
		//Searches for a variable called onexit with the type BLOCK
	
		
		
		if(block.equals(main) && listener != null) {
			Object exitFunction = getVariable("onexit", null);

			if(exitFunction != null) 
				executeBlock(((Block) exitFunction), true);
			
			listener.done(main.exitCode); 
		}
		block.alive = false;
		aliveBlocks.remove(block);
	}
	
	/**@return false, if the block: <br>
	 * <li>Is interrupted</li>
	 * <li>Is not alive for any reason</li>
	 * <li>Parent block is not alive and not null</li>
	 * <li>Parent block is interrupted and not null</li>*/
	private boolean checkForInterrupt(Block block) {
		if(block.parent == null) return block.alive && !block.interrupted;
		else return block.alive && !block.interrupted
				&& block.parent.alive && !block.parent.interrupted;
	}
	
	/**Interprets a command and its arguments*/
	Object executeCommand(StringBuilder command, int startIndex, boolean updateExecuteIndex, Block block) {
		if(!block.alive) return null;
		
		if(block.getStack() > 10) {
			ApplicationBuilder.panic("Max block stack exceeded (10)");
			return null;
		}
		
		block.currentCommand = command.toString();
		ArrayList<Object> args = interpretArguments(command, startIndex, updateExecuteIndex, block);
		if(args == null) return null;
		if(!block.alive || args.isEmpty()) return null;
		if(maxRuntime > 0) {
			if(System.currentTimeMillis() - currentChar > maxRuntime) {
				kill(block, "Process passed max runtime. Aborting");
				return null;
			}
		}
		//Worst case is O(2n), but thats very unlikely
		boolean addToBlockCache = false;
		
		if(block.loop && block.cached.isEmpty()) addToBlockCache = true;
		else if(!block.cached.isEmpty()) {
			for(int j = 0; j < block.cached.size(); j++) {
				Command c = block.cached.get(j);
				if(c.commandNameOffset >= args.size()) continue;
				
				String argument = args.get(c.commandNameOffset).toString();
				if(argument.getClass().getTypeName().equals("java.lang.String") && !argument.getClass().isArray()) {
				
					if(c.name.equals(argument) && (c.argumentCount == (args.size()-1) || c.repeated())) {
						boolean accepted = true;
						for(int i = 0, arg = 0; i < args.size()-1; i++, arg++) {
							if(i == c.commandNameOffset) arg++;
							if(arg > c.argumentCount) break;
							if(!c.repeated()) {
								if(!ApplicationBuilder.typeMatch(c.arguments[i], ApplicationBuilder.toDataType(args.get(arg)))) accepted = false;//continue main; 
							} else {
								if(arg < c.argumentCount) {
									if(!ApplicationBuilder.typeMatch(c.arguments[i], ApplicationBuilder.toDataType(args.get(arg)))) accepted = false;//continue main;
								} else if(!ApplicationBuilder.typeMatch(c.arguments[c.argumentCount-1], ApplicationBuilder.toDataType(args.get(arg)))) accepted = false;
							}
							if(!accepted) break;
						}
					
						if(accepted) {
							try {
								args.remove(c.commandNameOffset);
								//commands_executed++;
								return c.execute(args.toArray(new Object[args.size()]), this, block);
							} catch (Exception e) {
								kill(block, "Failed to execute command " + c.name + ": ");
								e.printStackTrace();
								return null;
							}
						}
					}
				}
			}
			if(addToBlockCache) warning("Failed to execute command " + args + "from cached state");
			addToBlockCache = true;
		}
		
		for(GeneratedLibrary lib : libraries) {
			for(Command c : lib.commands) {
					if(c.commandNameOffset >= args.size()) continue;
					
					String argument = args.get(c.commandNameOffset).toString();
					if(argument.getClass().getTypeName().equals("java.lang.String") && !argument.getClass().isArray()) {
					
						if(c.name.equals(argument) && (c.argumentCount == (args.size()-1) || c.repeated())) {
							boolean accepted = true;
							for(int i = 0, arg = 0; i < args.size()-1; i++, arg++) {
								if(i == c.commandNameOffset) arg++;
								if(arg > c.argumentCount) break;
								if(!c.repeated()) {
									if(!ApplicationBuilder.typeMatch(c.arguments[i], ApplicationBuilder.toDataType(args.get(arg)))) accepted = false;//continue main; 
								} else {
									if(arg < c.argumentCount) {
										if(!ApplicationBuilder.typeMatch(c.arguments[i], ApplicationBuilder.toDataType(args.get(arg)))) accepted = false;//continue main;
									} else if(!ApplicationBuilder.typeMatch(c.arguments[c.argumentCount-1], ApplicationBuilder.toDataType(args.get(arg)))) accepted = false;
								}
								if(!accepted) break;
							}
						
							if(accepted) {
								try {
									args.remove(c.commandNameOffset);
									if(addToBlockCache) block.addToCache(c);
									//commands_executed++;
									return c.execute(args.toArray(new Object[args.size()]), this, block);
								} catch (Exception e) {
									kill(block, "Failed to execute command " + c.name + " (" + e.toString() + ")");
									e.printStackTrace();
									return null;
								}
							}
						}
					}
				} 
		}
		 String argsString = "";
		 for(Object d : args) argsString +=  (d instanceof Array ? "@" : "") + d.toString() + " ";
		 kill(block,"No such command: " +  (argsString.isEmpty() ? "[null]" : argsString));
//		 process_end = System.currentTimeMillis();
//		 execution_time = (int) (process_end - process_start);
//		 if(debug) {
//			 log("Debug report:\n\tExecution time: " + execution_time + "(" + (float) execution_time/1000f + " sec)\n\t"
//			 		+ "Commands executed: " + commands_executed + "\n\t"
//			 		+ "Average commands per second:  " + average_commands_per_sec + " commands/sec", true);
//		 }
		 return null;
	}
	
	/**Breaks the arguments, when a ; is encountered*/
	ArrayList<Object> interpretArguments(StringBuilder command, int start, boolean updateIndex, Block block) {
		if(!checkForInterrupt(block)) return null;
		
		ArrayList<Object> args = new ArrayList<>(1);
		
		for(int i = start; i < command.length(); i++) {
			if(updateIndex) block.executeIndex = i;
			char current = command.charAt(i);

			if(current == ' ') {
				if(updateIndex) block.executeIndex = i+1;
				continue;
			}
			if(current == Alphabet.BREAK || current == Alphabet.BLCK_1) {
				if(updateIndex) block.executeIndex = i+1;
				return args;
			}
			
			if(current == Alphabet.COMMENT) {
				String subsequence = findMatching(command.toString(), i, 0, Alphabet.COMMENT, Alphabet.COMMENT, Alphabet.ESCAPE, false);
				if(subsequence == null) {
					kill(block, "");
					return null;
				}
				
				i += subsequence.length() + 1;
				
			} else if(current == Alphabet.LSTRING_0) {
				String subsequence = findMatching(command.toString(), i, 0, Alphabet.LSTRING_0, Alphabet.LSTRING_1, Alphabet.ESCAPE, false);
				//Now, only remove \ near < and >
				if(subsequence == null) {
					kill(block, "Unable to close long String (too many \"<\" without escape character)");
					return null;
				}
				args.add(subsequence.replace(Alphabet.ESCAPE+"<", "<").replace(Alphabet.ESCAPE+">", ">").replace("\\\\", "\\"));
				i += subsequence.length() + 1;
				
			} else if(current == Alphabet.STR_0) {
				String subsequence = findMatching(command.toString(), i, 0, Alphabet.STR_0, Alphabet.STR_0, Alphabet.ESCAPE, false);
				Object string = subsequence.replace(Alphabet.ESCAPE+"", "");
				args.add(string);
				i += subsequence.length() + 1;
					
			} else if(current == Alphabet.BRK_0) {
				String subCommand = findMatching(command.toString(), i, 0, Alphabet.BRK_0, Alphabet.BRK_1, Alphabet.ESCAPE, true);
				if (subCommand == null) {
					kill(block, "Wtf you doing?!");
					return null;
				}
				Object returned = executeCommand(new StringBuilder(subCommand), 0, false, block);
				if(!checkForInterrupt(block)) return null;
				if(returned != null) args.add(returned);
				i += subCommand.length() + 1;
						
			} else if(current == Alphabet.VAR_0) {  //Search in <variables> for the specified datacontainer
				StringBuilder varName = new StringBuilder();
				ArrayList<String> indexChain = new ArrayList<String>();
				boolean atIndex = false;
				
				for(int j = i+1; j < command.length(); j++, i++) {
					char c = command.charAt(j);
					
					if(c == ' ' || c == Alphabet.BLCK_0 || c == Alphabet.BREAK || c == Alphabet.BRK_0 || c == Alphabet.STR_0 || c == Alphabet.BRK_1) break;					
					
					if(c == Alphabet.ARR_0) {
						String index = findMatching(command.toString(), j, 0, Alphabet.ARR_0, Alphabet.ARR_1, Alphabet.ESCAPE, true);
						indexChain.add(index);
						j += index.length();
						i = j-1;
						atIndex = true;
					}
					
					if(!atIndex) varName.append(c);
				}
				Object variableValue = getVariable(varName.toString(), block);
				
				if(variableValue == null && !varName.toString().equals("null")) {
					kill(block, "Unknown variable name: " + varName);
					return null;
				}
				
				if(!indexChain.isEmpty()) {
					
					for(String s : indexChain) {
						ArrayList<Object> indexArgument = interpretArguments(new StringBuilder(s), 0, false, block);
						
						if(indexArgument == null) {
							kill(block, "Error trying to fetch array index (Caused by previous error)");
							return null;
						}
						
						if(indexArgument.isEmpty()) {
							kill(block, "Error trying to fetch array index (Empty statement)");
							return null;
						}
						
						Object index = indexArgument.get(0);
						
						if(!(variableValue instanceof Array)) {
							kill(block, "Trying to get index from a non-array value");
							return null;
						}
						
						if(index == null) {
							kill(block, "Index must be an integer");
							return null;
						}
						
						boolean canBeInteger = ApplicationBuilder.testForWholeNumber(index.toString());
						if(!canBeInteger) {
							kill(block, "Index must be an integer");
							return null;
						}
						int value = Integer.valueOf(index.toString());
						if(value < 0 || value >= ((Array) variableValue).getIndexes().size()) {
							kill(block, "Array index out of bounds (index: " + value + " length: " + ((Array) variableValue).getIndexes().size() + ")");
							return null;
						}
						
						variableValue = ((Array) variableValue).getIndexes().get(value);
					}
					args.add(variableValue);
				} else args.add(variableValue);
						
			} else if(current == Alphabet.ARR_0) {
				String array = findMatching(command.toString(), i, 0, Alphabet.ARR_0, Alphabet.ARR_1, Alphabet.ESCAPE, true);
				Array arrayData = new Array();
				ArrayList<Object> indexes = interpretArguments(new StringBuilder(array), 0, false, block);
				if(!block.alive || block.interrupted) return null;
				for(Object object : indexes) arrayData.push(object);
						
				args.add(arrayData);
				i += array.length() + 1;
						
			} else if(current == Alphabet.BLCK_0) {
				String blockCode = findMatching(command.toString(), i, 0, Alphabet.BLCK_0, Alphabet.BLCK_1, Alphabet.ESCAPE, true);
				
				args.add(new Block(new StringBuilder(blockCode), block));
				i += blockCode.length() + 1;
			
			} else if(current != ' ') {
				
				StringBuilder sequence = new StringBuilder();
				b: for(int j = i; j < command.length(); j++) { //Break this string by any alphabet char -> Otherwise surround string with "
					char  c = command.charAt(j);
					if(!Alphabet.partOf(c) && c != ' ') {
						sequence.append(c);
						if(j+1 < command.length()) {
							char next = command.charAt(j+1);
							if(Alphabet.partOf(next) || next == ' ') {
								i--;
								break b;
							}
						} else {
							i--;
							break b;
						}
					} else break b;
				}
				if(sequence.length() > 0) {
					args.add(sequence.toString());
					i += sequence.length();
				}				
			}
			if(updateIndex) block.executeIndex = i;
		}
		//System.out.println("End command with arguments: " + args);
		return args;
	}
	
	/**@param garbageCollector - If the process should remove variables after the block was executed.
	 * Usually true, because the variables would not be accessible anymore anyway.*/
	public void  executeBlock(Block block, boolean garbageCollector, Object... args) {
		if(block == null) block = getMain();
		//if(block.alive) kill(block, "Block already running");
		
		/*The arguments are just variables declared in the block - local scope and removed afterwards, if requested*/
		if(block != null) {
			for(int i = 0; i < args.length; i++) {
				setVariable(String.valueOf(i), args[i], true, false, block);
			}
		}
		
		start(block);
		if(garbageCollector) garbageCollection(block);
	}
	
	/**Clears all variables associated with the block*/
	public void garbageCollection(Block block) {
		if(block == null) return;
		
		for(int i = 0; i < variables.size(); i++) {
			if(variables.get(i).block != null) {
				if(variables.get(i).block.equals(block) && !variables.get(i).permanent) {
					variables.remove(i);
					i--;
				}
			}
		}
		
		System.gc();
	}
	
	public void removeVariable(String name, Block block) {
		for(Variable v : variables) {
			if(v.name.equals(name) && !v.FINAL && v.block.getStack() <= block.getStack()) {
				variables.remove(v);
				return;
			}
		}
	}
	
	/**Removes all variables with the specified name, no matter in wich block or stack they are*/
	public void removeVariable(String name) {
		for(int i = 0; i < variables.size(); i++) {
			if(variables.get(i).name.equals(name) && !variables.get(i).FINAL) {
				variables.remove(i);
				i--;
			}
		}
	}
	
	public ArrayList<Variable> getVariables() {
		return variables;
	}
	
	/**If a variable with the name already exist, the value is altered, if:<br>
	 * <li>The data type is the same,<br>
	 * <li>The variable is not declared as final
	 * <br>
	 * @param block - The block, the variable will be associated with. Important is the {@link Block#stack}
	 * Use null, if the variable is not declared inside a block.*/
	public void setVariable(String name, Object value, boolean FINAL, boolean permanent, Block block) {
		if(name.contains(" ") || Alphabet.partOf(name)) {
			kill(block, "Variable name contains illegal/reserved characters");
			return;
		}
		if(block == null) block = getMain();
		
		for(Variable v : variables) { //If any block is null, it is treated as stack = 0 and alive = false
			if(v.name.equals(name)) {
				int stack1 = v.block == null ? 0 : v.block.getStack();
				int stack2 = block == null ? 0 : block.getStack();
			
				if(stack1 <= stack2) {
					if(!v.FINAL) {// || v.permanent) {
						if(!v.setValue(value, getMain() == null)) kill(block, "Failed to assign type " + value + " to existing variable " + v.value);
					} else if(getMain() != null) {
						kill(block,"Tried to modyfy a constant: " + name);
					}
					return;
				}
			}
		}
		this.variables.add(new Variable(name, value, FINAL, permanent, block));
	}
	
	public void setVariable(String name, Object value, boolean FINAL, boolean permanent) {
		setVariable(name, value, FINAL, permanent, getMain());
	}
	
	/**Returns null, if the variable was not found
	 * Use null to use main block*/
	public Object getVariable(String name, Block block) {
		if(name.equals("true")) return True;
		else if(name.equals("false")) return False;
		else if(name.equals("null")) return Null;
		else {
			for(Variable v : variables) {
				byte stack1 = v.block == null ? 0 : v.block.getStack();
				byte stack2 = block == null ? 0 : block.getStack();
				if(v.name.equals(name) && stack1 <= stack2) return v.value;
			}
			return null;
		}
	}
	
	public void includeLibrary(Library lib) {
		libraries.add(new GeneratedLibrary(lib));
	}
	
	public void clearLibraries() {
		libraries.clear();
	}
	
	public void log(String message, boolean newline) {
		for(Output output : this.output) output.log(message, newline);
	}
	
	public void error(String message) {
		for(Output output : this.output) output.error(message);
	}
	
	public void warning(String message) {
		for(Output output : this.output) output.warning(message);
	}
	
	public void addOutput(Output output) {
		this.output.add(output);
	}
	
	public void addSystemOutput() {
		addOutput(new Output() {
				public void log(String message, boolean newline) {
					System.out.print(message);
					if(newline) System.out.println();
				}

				public void error(String message) {
					System.err.println(message);
				}
				
				public void warning(String message) {
					System.out.println("[WARN] " + message);
				}
			});
	}
	
	public ArrayList<GeneratedLibrary> getLibraries() {
		return libraries;
	}
	
	public GeneratedLibrary getLibrary(String name) {
		for(GeneratedLibrary lib : libraries) {
			if(lib.name.equals(name)) return lib;
		}
		return null;
	}
	
	public synchronized void kill(Block block, String errorMessage) {
		if(block == null) {
			error("Java Error> " + errorMessage);
			return;
		} else {
			if (block.currentCommand.length() > 30) {
				block.currentCommand = block.currentCommand.subSequence(0, 10) + " ... " + block.currentCommand.substring(block.currentCommand.length() - 10, block.currentCommand.length());
			}
			if(!errorMessage.isEmpty()) error("Error at [" + block.currentCommand + "]> " + errorMessage);
		
			for(Block b : aliveBlocks) {
				b.cached.clear();
				b.alive = false;
				b.interrupted = true;
			}
			block.alive = false;
			block.interrupted = true;
			block.currentCommand = "";
		}
		
		block.exitCode = Block.ERROR;
		aliveBlocks.clear();
		garbageCollection(main);
		System.gc();
	}
	
	public boolean isRunning() {
		if(main != null) return main.alive;
		else return false;
	}
	
	public void pause(Block block, int millisec) {
		if(block.thread == null) return;
		
		if(block.alive) {
			synchronized (block.thread) {
				try {
					block.thread.wait(millisec);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} 
	}
	
	public void pause(Block block) {
		if(block.thread == null) {
			warning("Block has no thread attached. Pausing wont hav any effect");
			return;
		}
		
		if(block.alive) {
			synchronized (block.thread) {
				try {
					block.thread.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} 
	}
	
	/**Notifies a block with a thread.*/
	public void wake(Block block) {
		if(block.thread == null) {
			warning("Block has no thread");
			return;
		}
		if(!block.alive) {
			warning("Block not running. Nothing to wake");
			return;
		}
		
		synchronized (block.thread ) {
			block.thread.notify();
		}
	}
	
	/**Pauses the process thread (if not specified, the Runnable.getRunnable() thread)
	 * and waits, until an input from the given input stream ({@link Process#setInput(InputStream)})
	 * <br>The input is passed, if the line is fed into the process, if it is terminated with either a \n or \r character
	 * @throws IOException */
	public String waitForInput() throws IOException {
		if(inputReader == null) return "";
		return inputReader.readLine();
	}
	
	public void setInput(InputStream input) {
		if(input == null) return;
		
		if(inputReader != null)
			try {
				inputReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		inputStream = input;
		inputReader = new BufferedReader(new InputStreamReader(input));
	}
	
	/**May be null*/
	public InputStream getInput() {
		return inputStream;
	}
	
	public ArrayList<Output> getOutput() {
		return output;
	}
	
	public Block getMain() {
		return main;
	}
	
	public void breakLoop() {
		breakRequested = true;
	}
	
	public void setApplicationListener(ApplicationListener listener) {
		this.listener = listener;
	}
	
	public ApplicationListener getApplicationListener() {
		return listener;
	}
	
	public static String findMatching(String string, int start, int skip, char openChar, char closeChar, char escapeChar, boolean removeEscape) {
		char[] arr = string.toCharArray();
		int wrap = 0;
		int subStart = 0;
		int subEnd = 0;
		char prev = 0x00;
		boolean sameOpen = false;
		
		for(int i = start; i < arr.length; i++) {
			char current = arr[i];
			
			if(current == openChar && prev != escapeChar && !sameOpen) {
				if(wrap == skip) subStart = i + 1;
				if(openChar == closeChar && wrap == skip) sameOpen = true;
				wrap++;
				continue;
			}
			
			if(current == closeChar && prev != escapeChar) {
				wrap--;
				if(wrap == skip || sameOpen) {
					subEnd = i;
					string = string.substring(subStart, subEnd);
					if(removeEscape) {
						string = string.replace(new String(new char[]{escapeChar, openChar}), new String(new char[] {openChar}));
						string = string.replace(new String(new char[]{escapeChar, closeChar}), new String(new char[] {closeChar}));
					}
					return string;
				}
			} 
			prev = current;
		}
		return null;
	}
}
