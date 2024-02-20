package com.devkev.devscript.raw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.devkev.devscript.nativecommands.NativeLibrary;
import com.devkev.devscript.raw.ExecutionState.ExitCodes;

public class Process {

	private ArrayList<HookedLibrary> libraries = new ArrayList<HookedLibrary>();
	private ArrayList<Output> output = new ArrayList<Output>(1); 
	//Store all the threads of blocks in this "public" pool, so they can be controlled when the script terminates
	private final List<Block> threadedBlockRuntimes = Collections.synchronizedList(new ArrayList<Block>());
	private ApplicationListener listener;
	
	public static final boolean FALSE = false;
	public static final boolean TRUE = true;
	public static final Undefined UNDEFINED = new Undefined();
	
	private BufferedReader inputReader;
	private InputStream inputStream;
	
	volatile Block runtime;
	//Main block is not never in the list, since the process gets terminated, if main is killed.
	boolean breakRequested = false;
	
	public long maxRuntime = 0; //Runtime in ms. If < 0, allowed runtime is infinite
	private long currentChar = 0;
	public final String version = "1.9.14"; 
	
	private boolean caseSensitive = false;
	
	/**The file, the script is executed from. May be null. Just useful for some Native commands*/
	public File file = null;
	private Random random;
	
	public class HookedLibrary {
		
		public final Command[] commands;
		public final String name;
		public final Library lib;
		
		public HookedLibrary(Library library) {
			this.lib = library;
			this.commands = library.createLib();
			this.name = library.getName();
		}
	}
	
	//The id of the thread the script is runned e.g. the "execute" function is called. Blocks are only allowed to be executed in this thread context as well
	private long threadID;
	
	/**@param useCache - If the process should temprary save common used commands in a separate list.
	 * May improve performance on large scripts, but also increase the memory usage.
	 * <br>In worst case, the data usage of the command library could be doubled.
	 * <br>Use {@link Process#setCacheLimit(int limit)} to limit cache usage
	 * It also imports the NativeLibrary (basic commands like if,println ...) at default.
	 * If you don't want these native commands, call {@link Process#clearLibraries()} before adding your custom ones
	 * @throws Exception */
	public Process(boolean useCache) throws Exception {
		includeLibrary(new NativeLibrary());
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				//Cant execute onexit function. Just execute the library listener
				if(isRunning())
					kill(runtime, "JVM killed");
			}
		}));
	}
	
	public ExecutionState execute(String script) {
		
		random = new Random();
		
		if(isRunning()) {
			warning("Java Environment was trying to start a new process, while its already running");
			return null;
		}
		
		script = script.replaceAll("\t", "");
		script = script.replaceAll("\r", "");
		script = script.replaceAll("\n", "");
		
		//TODO persistent variables
		runtime = new Block(new StringBuilder(script), null, this);
		
		threadID = Thread.currentThread().getId();
		
		ExecutionState state = executeBlock(runtime, true);
		
		//Free resources and interrupt all threads, in case the process was terminated unexpected
		finalizeExit(runtime.currentExecutionState.exitCode, runtime.currentExecutionState.stateMessage);
		
		synchronized (this) {
			for(Block b : threadedBlockRuntimes) {
				if(b.threadMeta.isAlive()) {
					System.out.println("Found thread that is still alive! (" + b.threadMeta.getName() + ")");
					b.threadMeta.interrupt();
				}
			}
		}
		
		if(listener != null) 
			listener.done(runtime.currentExecutionState); 
		
		return state;
	}
	
	public ExecutionState execute(File file) throws IOException {	
		String code  = "";
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while(line != null) {
			code += line;
			line = reader.readLine();
		}
		reader.close();
		
		this.file = file;
		return execute(code);
	}
	
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	public boolean isCaseSensitive() {
		return caseSensitive;
	}
	
	/**Executes a block in a separate thread
	 * Regular blocks are only allowed to be run in the same thread as the script host*/
	public Thread executeThreadedBlock(final Block block, final boolean garbageCollector, final Object... args) {
		
		if(Thread.currentThread().getId() != threadID)
			throw new ScriptHostException("You are not allowed to execute blocks in a different thread context than the ScriptHost");
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				executeUnsave(block, garbageCollector, args);
				
				synchronized (Process.this) {
					threadedBlockRuntimes.remove(block);
				}
			}
		}, "Threaded Block " + block.toString());
		synchronized (this) {
			threadedBlockRuntimes.add(block);
		}
		block.threadMeta = t;
		t.start();
		return t;
	}
	
	/**@param garbageCollector - If the process should remove variables after the block was executed.
	 * Usually true, because the variables would not be accessible anymore anyway.
	 * not garbegage collected and are isolated from other blocks. 
	 * @param caller - The block that initiates this execution. Important for stack traces and error handling
	 * @return The execution state wether this block executed successful or with an exception. If no errors ocurred, the returned object equals to {@link ExecutionState#STATE_SUCCESS}*/
	public ExecutionState executeBlock(Block block, boolean garbageCollector, Object... args) {
		
		if(Thread.currentThread().getId() != threadID)
			throw new ScriptHostException("You are not allowed to execute blocks in a different thread context than the ScriptHost");
		
		return executeUnsave(block, garbageCollector, args);
	}
	
	//Just for internal use
	private ExecutionState executeUnsave(Block block, boolean garbageCollector, Object... args) {
		if(block == null) block = getRuntime();
		
		/*The arguments are just variables declared in the block - local scope and removed afterwards, if requested*/
		if(block != null && args.length > 0) {
			Array arr = new Array();
			//1.9.14: Also add the arguments as an array with the name $args
			for(int i = 0; i < args.length; i++) {
				block.setVariable(String.valueOf(i), args[i], false, false, true);
				arr.push(args[i]);
			}
			
			block.setVariable("args", arr, false, false, true);
		}
		
		if(block == null) return ExecutionState.STATE_BLOCK_NULL;
		StringBuilder command = block.blockCode;
		
		block.currentExecutionState = ExecutionState.STATE_SUCCESS;
		block.alive = true;
		block.executeIndex = 0;
		block.interrupted = false;
		if(!checkForInterrupt(block)) return ExecutionState.STATE_BLOCK_INTERRUPTED;
		
		while(block.executeIndex < block.blockCode.length()-1 && block.alive) {
			executeCommand(command, block.executeIndex, true, block);
		}
		
		block.alive = false;
		
		if(garbageCollector) garbageCollection(block);
		
		return block.currentExecutionState;
	}
	
	/**@return false, if the block: <br>
	 * <li>Is interrupted</li>
	 * <li>Is not alive for any reason</li>
	 * <li>Parent block is not alive and not null</li>
	 * <li>Parent block is interrupted and not null</li>
	 * <li>The parent block is not a constructor</li>*/
	private boolean checkForInterrupt(Block block) {
		if(block.getParent() == null) return block.alive && !block.interrupted;
		else return block.alive && !block.interrupted
				&& (block.getParent().alive && !block.getParent().interrupted
				|| block.getParent().isObject());
	}
	
	/**Interprets a command and its arguments*/
	Object executeCommand(StringBuilder command, int startIndex, boolean updateExecuteIndex, Block block) {
		
		if(!block.alive) return null;
		
		if(block.getStack() > 10) throw new ScriptHostException("Max block stack exceeded (10)");
		
		block.currentCommand = command.toString();
		ArrayList<Object> args = interpretArguments(command, startIndex, updateExecuteIndex, block);
		if(args == null) return null;
		if(!block.alive || args.isEmpty()) return null;
		boolean allNull = true;
		for(Object a : args) {
			if(a != null) {
				allNull = false;
				break;
			}
		}
		if(allNull) return null;
		if(maxRuntime > 0) {
			if(System.currentTimeMillis() - currentChar > maxRuntime) {
				kill(block, "Process passed max runtime. Aborting");
				return null;
			}
		}
		//Worst case is O(2n), but thats very unlikely
		boolean addToBlockCache = false;
		
		if(block.isLoop() && block.cached.isEmpty()) addToBlockCache = true;
		else if(!block.cached.isEmpty()) {
			for(int j = 0; j < block.cached.size(); j++) {
				Command c = block.cached.get(j);
				if(c.commandNameOffset >= args.size()) continue;
				
				String argument = args.get(c.commandNameOffset).toString();
				if(argument.getClass().getTypeName().equals("java.lang.String") && !argument.getClass().isArray()) {
					
					String name = !isCaseSensitive() ? c.name.toLowerCase() : c.name;
					if(name.equals(!isCaseSensitive() ? argument.toLowerCase() : argument) && (c.argumentCount == (args.size()-1) || c.repeated())) {
						boolean accepted = true;
						for(int i = 0, arg = 0; i < args.size()-1; i++, arg++) {
							if(i == c.commandNameOffset) arg++;
							if(arg > c.argumentCount) break;
							if(!c.repeated()) {
								if(!ProcessUtils.typeMatch(c.arguments[i], ProcessUtils.toDataType(args.get(arg)))) accepted = false;//continue main; 
							} else {
								if(arg < c.argumentCount) {
									if(!ProcessUtils.typeMatch(c.arguments[i], ProcessUtils.toDataType(args.get(arg)))) accepted = false;//continue main;
								} else if(!ProcessUtils.typeMatch(c.arguments[c.argumentCount-1], ProcessUtils.toDataType(args.get(arg)))) accepted = false;
							}
							if(!accepted) break;
						}
					
						if(accepted) {
							try {
								args.remove(c.commandNameOffset);
								//commands_executed++;
								return c.execute(args.toArray(new Object[args.size()]), this, block);
							} catch (Exception e) {
								kill(block, "Failed to execute command " + c.name + " (" + e.toString() + ") more info can be found in the stacktrace");
								e.printStackTrace();
							}
						}
					}
				}
			}
			if(addToBlockCache) warning("Failed to execute command " + args + "from cached state");
			addToBlockCache = true;
		}
		
		for(HookedLibrary lib : libraries) {
			for(Command c : lib.commands) {
					if(c.commandNameOffset >= args.size()) continue;
					
					String argument = args.get(c.commandNameOffset).toString();
					if(argument.getClass().getTypeName().equals("java.lang.String") && !argument.getClass().isArray()) {
					

						String name = !isCaseSensitive() ? c.name.toLowerCase() : c.name;
						if(name.equals(!isCaseSensitive() ? argument.toLowerCase() : argument) && (c.argumentCount == (args.size()-1) || c.repeated())) {
							boolean accepted = true;
							for(int i = 0, arg = 0; i < args.size()-1; i++, arg++) {
								if(i == c.commandNameOffset) arg++;
								if(arg > c.argumentCount) break;
								if(!c.repeated()) {
									if(!ProcessUtils.typeMatch(c.arguments[i], ProcessUtils.toDataType(args.get(arg)))) accepted = false;//continue main; 
								} else {
									if(arg < c.argumentCount) {
										if(!ProcessUtils.typeMatch(c.arguments[i], ProcessUtils.toDataType(args.get(arg)))) accepted = false;//continue main;
									} else if(!ProcessUtils.typeMatch(c.arguments[c.argumentCount-1], ProcessUtils.toDataType(args.get(arg)))) accepted = false;
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
									kill(block, "Failed to execute command " + c.name + " (" + e.toString() + ") more info can be found in the stacktrace");
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
		return null;
	}
	
	/**Breaks the arguments, when a ; is encountered*/
	public ArrayList<Object> interpretArguments(StringBuilder command, int start, boolean updateIndex, Block block) {
		if(!checkForInterrupt(block)) {
			//warning("Aborting argument interptretation for block " + block + ": interrupted.");
			block.interrupt();
			block.alive = false;
			return null;
		}
		
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
				args.add(subsequence.replace(Alphabet.ESCAPE+"<", "<").replace(Alphabet.ESCAPE+">", ">").replace("\\\\", "\\").replace("\\n", "\n").replace("\\t", "\t"));
				i += subsequence.length() + 1;
				
			} else if(current == Alphabet.STR_0) {
				String subsequence = findMatching(command.toString(), i, 0, Alphabet.STR_0, Alphabet.STR_0, Alphabet.ESCAPE, false);
				if(subsequence != null) {
					Object string = subsequence.replace("\\n", "\n").replace("\\t", "\t").replace("\\", "");
					args.add(string);
					i += subsequence.length() + 1;
				} else kill(block, "Syntax error: Missing Semicolon");
					
			} else if(current == Alphabet.BRK_0) {
				String subCommand = findMatching(command.toString(), i, 0, Alphabet.BRK_0, Alphabet.BRK_1, Alphabet.ESCAPE, true);
				if (subCommand == null) {
					kill(block, "Missing a closing bracket");
					return null;
				}
				Object returned = executeCommand(new StringBuilder(subCommand), 0, false, block);
				if(!checkForInterrupt(block)) return null;
				if(returned != null) args.add(returned);
				i += subCommand.length() + 1;
						
			} else if(current == Alphabet.VAR_0) {  //Search in <variables> for the specified datacontainer
				ArrayList<String> objDots = new ArrayList<String>();
				//At first, check for dots and chain them
				String dotVarname = "";
				for(int j = i+1; j < command.length(); j++, i++) {
					char c = command.charAt(j);
					
					if(c == ' ' || c == Alphabet.BLCK_0 || 
							c == Alphabet.BREAK || c == Alphabet.BRK_0 || c == Alphabet.STR_0 || c == Alphabet.BRK_1) {
						objDots.add(dotVarname);
						dotVarname = "";
						break;
					}
					
					if(c == Alphabet.OBJECT_DOT) {
						objDots.add(dotVarname);
						dotVarname = "";
						continue;
					}
					dotVarname += command.charAt(j);
				}
				
				if(!dotVarname.isEmpty()) objDots.add(dotVarname.trim());
				
				Object lastVar = null;
				Block dotScope = block; //The scope can change if you want to access variables from within an object!
				for(int k = 0; k < objDots.size(); k++) {
					String dotvars = objDots.get(k);
					
					ArrayList<String> indexChain = new ArrayList<String>();
					boolean atIndex = false;
					StringBuilder realVarName = new StringBuilder();
					for(int j = 0; j < dotvars.length(); j++) {
						char c = dotvars.charAt(j);
						
						if(c == Alphabet.ARR_0) {
							String index = findMatching(dotvars, j, 0, Alphabet.ARR_0, Alphabet.ARR_1, Alphabet.ESCAPE, true);
							indexChain.add(index);
							j += index.length();
							
							atIndex = true;
						}
						if(!atIndex) realVarName.append(c);
						
					}
					
					if(realVarName.length() == 0) realVarName.append(dotvars);
					
					Object variableValue = dotScope.getVariable(realVarName.toString());
					
					if(variableValue == null && !realVarName.toString().equals("null")) {
						kill(block, "Unknown variable name or property value: " + realVarName.toString());
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
								kill(block, "Index must be an integer or ?");
								return null;
							}
							
							int value = 0;
							if(index.toString().equals("?")) 
								value = random.nextInt(((Array) variableValue).getIndexes().size());
							else {
								boolean canBeInteger = ProcessUtils.testForWholeNumber(index.toString());
								if(!canBeInteger) {
									kill(block, "Index must be an integer or ?");
									return null;
								}
								value = Integer.valueOf(index.toString());
								if(value < 0 || value >= ((Array) variableValue).getIndexes().size()) {
									kill(block, "Array index out of bounds (index: " + value + " length: " + ((Array) variableValue).getIndexes().size() + ")");
									return null;
								}
							}
							variableValue = ((Array) variableValue).getIndexes().get(value);
						}
						
					}
					lastVar = variableValue;
					
					if(objDots.size() > 1 && k < objDots.size()-1) {
						if(!(variableValue instanceof Block)) {
							kill(block, "Cannot access properties of non-object variables");
							return null;
						} else {
							if(!((Block) variableValue).isObject()) {
								kill(block, "Cannot access properties of non-object variables");
								return null;
							} else dotScope = (Block) variableValue;
						}
					}
				}	
				args.add(lastVar);
				
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
				
				args.add(new Block(new StringBuilder(blockCode), block, this));
				i += blockCode.length() + 1;
			
			} else if(current != ' ') {
				
				StringBuilder sequence = new StringBuilder();
				b: for(int j = i; j < command.length(); j++) { //Break this string by any alphabet char -> Otherwise surround string with "
					char  c = command.charAt(j);
					//if(!Alphabet.partOf(c) && c != ' ') {
					if(c != ' ' && (!Alphabet.partOf(c) || Alphabet.partOfVariableDeclarator(c))) {
						sequence.append(c);
						if(j+1 < command.length()) {
							char next = command.charAt(j+1);
							if(next == ' ' || Alphabet.notPartOfVariableDeclarator(next)) {
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
	
	/**Clears all variables associated with the block*/
	public void garbageCollection(Block block) {
		if(block == null) return;
		block.clearVariables();
	}
	
	/**Adding a library with the same name as an already included one will result in an exception.
	 * Same goes with commands.
	 * Library names are case-sensitive<br>
	 * Command names are checked for case sensitivity, if {@link Process#setCaseSensitive(boolean)} is true
	 * @param lib The library to add
	 * @throws Exception 
	 * */
	public void includeLibrary(Library lib) {
		if(lib.bound != null) {
			if(lib.bound != this) 
				throw new ScriptHostException("This Library instance is already part of a different process. Try creating a new one: includeLibrary(new Library())");
		}
		
		HookedLibrary hadd = new HookedLibrary(lib);
		
		//Check for command or library name duplicates
		for(HookedLibrary hlib : getLibraries()) {
			if(hlib.name.equals(hadd.name))
				throw new ScriptHostException("A library with the name: \"" + lib.getName() + "\" was already added to the Process " + this.toString());
			for(Command c : hlib.commands) {
				for(Command c2 : hadd.commands) {
					if(this.isCaseSensitive() ? c.name.equals(c2.name) : c.name.toLowerCase().equals(c2.name.toLowerCase()) 
							&& c.argumentsAsString.equals(c2.argumentsAsString)
							&& c.commandNameOffset == c2.commandNameOffset) 
						throw new ScriptHostException("Trying to add a library to the process that contains one or more conflicting commands: " + c.name + " " + c.argumentsAsString);
				}
			}
		}
		//Check passed
		libraries.add(hadd);
	}
	
	/**Includes the ~70 default commands for scripts.
	 * This function is just a wrapper for <br>
	 * <code>includeLibrary(new NativeLibrary())</code>
	 * @throws Exception 
	 */
	public void includeNativeLibrary() throws Exception {
		includeLibrary(new NativeLibrary());
	}
	
	public void clearLibraries() {
		libraries.clear();
	}
	
	public void log(String message, boolean newline) {
		for(Output output : this.output) output.log(message, newline);
		System.out.print("[+]" + message + (newline ? " \n" : ""));
	}
	
	public void error(String message) {
		for(Output output : this.output) output.error(message);
		System.out.println("[!] " + message);
	}
	
	public void warning(String message) {
		for(Output output : this.output) output.warning(message);
		System.out.println("[-] " + message);
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
	
	public ArrayList<HookedLibrary> getLibraries() {
		return libraries;
	}
	
	public HookedLibrary getLibrary(String name) {
		for(HookedLibrary lib : libraries) {
			if(lib.name.equals(name)) return lib;
		}
		return null;
	}
	
	public synchronized void kill(Block block, String errorMessage) {
		
		if(block == null) {
			error("A java error happened outside of the devscript environment: " + errorMessage);
			return;
		} else {
			
			//If its a try/catch, just report any execution state without exiting the script.
			//Sadly, this makes it not possible to create a stacktrace
			Block inherit = block.inheritTryCatch();
			
			if(inherit != null) {
				
				inherit.interrupt();
				inherit.currentExecutionState = new ExecutionState(ExitCodes.ERROR, errorMessage);
				return;
			}
			
			if (block.currentCommand.length() > 30) {
				block.currentCommand = block.currentCommand.subSequence(0, 10) + " ... " + block.currentCommand.substring(block.currentCommand.length() - 10, block.currentCommand.length());
			}
			if(!errorMessage.isEmpty()) error("Unhandled error at [" + block.currentCommand + "]> " + errorMessage);
			block.interrupt();
		}
		
		try {
			//Notify the stream, in case an input is still awaited and fire the 
			synchronized (inputStream) {
				inputStream.notify();
			}
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//TODO
		block.currentExecutionState = new ExecutionState(ExitCodes.ERROR, errorMessage);
		
		garbageCollection(runtime);
	}
	
	public boolean isRunning() {
		if(runtime != null) return runtime.alive;
		else return false;
	}
	
	/**Pauses the process thread (if not specified, the Runnable.getRunnable() thread)
	 * and waits, until an input from the given input stream ({@link Process#setInput(InputStream)})
	 * <br>The input is passed
	 * @param escapeChar The character that terminates the input. Usually \n or \r
	 * @throws IOException */
	public String waitForInput() throws IOException {
		if(inputReader == null) return "";
		return inputReader.readLine();
	}
	
	/**You can use {@link ApplicationInput}*/
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
	
	/**Returns the main block and its context
	 * @return The main block, if this script host is running or null*/
	public Block getRuntime() {
		return runtime;
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
	
	/**
	 * Free up resources and call the "onExit" script function and the {@link Library#scriptExit(Process, int, String)} function on all libraries, in case open resouces, 
	 * for example sockets or readers are still used
	 * */
	public void finalizeExit(int exitCode, String errorMessage) {
		if(getRuntime() != null) { //Main should not be null
			Object exitFunction = getRuntime().getVariable("onExit");
			
			if(exitFunction != null) 
				executeBlock(((Block) exitFunction), true, exitCode, errorMessage);
			
		} else System.out.println("Failed to call onExit function");
		
		for(HookedLibrary lib : libraries) 
			lib.lib.scriptExit(this, exitCode, errorMessage);
	}
	
	@Override
	protected void finalize() throws Throwable {
		finalizeExit(1, "JVM Killed");
		
		if(listener != null) {
			listener.done(runtime.currentExecutionState); 
		}
	}
}
