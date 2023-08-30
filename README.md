# Devscript
A script inside Java for developing and debugging

# Implementation

Just copy the Code into your project or import the downloadable .jar (under "releases") file to your build path and you are done!<br>
DevScript does not use any unnecessary 3rd party libraries.

Basic syntax to execute a "Hello World" script from a string inside your program:
Both input and outputs are the default System.in and System.out, but you can define your own.

```java
Process p = new Process(true);
p.addSystemOutput();
p.setInput(System.in);
p.execute("println \"Hello World\"", false);
p.execute(new File("path_to_file"), true);
```

DevScript is a command-based scripting language written in Java. The native library currently consists of ~70 default commands.<br>
If you want to extend this list for more complex approaches or use your own commands to create -for example- a console input, 
take a look at the "Custom Commands" section 

You can also execute the script from the command line with the raw jar file and the argument --nogui.<br>
This example will execute the command "version" and print the current version.
> java -jar devscript_1.9.0.jar --nogui -e "version"

Command line arguments are:<br>
- -e or --execute <script> Executes a script right from the command line
- -f or --file <pathToFile> Executes the contents of a text file
- --nogui rejects the program to open the GUI editor
- If only --nogui is given, the jar opens the default editor, stored in Editor.txt

# Examples of usage

Devscript has a wide variety of use cases. An example would be an command line argument interpreter,<br>
or for creating a quick CLI.
But you can also create whole scripts with it. It is even possible to create an ascii 3d game. (Trust me, I've done it)

# Syntax
Tutorials and examples can be found here in /Examples/... or in the editor under "File" > "Examples"

# Custom Commands

You create custom commands by creating a new class that extends the "com.devkev.devscript.raw.Library" class.
If all the methods are imported, it should look something like this. I added some comments and examples to help you get started:

```java
public class MyCommands extends Library {
	//Just pass the name of the library to the super class
	public MyCommands() {
		super("Name of my Library");
	}

	//This function should return all of your handy commands as an array.
	@Override
	public Command[] createLib() {
		return new Command[] {
			
			//This is an example of a very simple command that expects no arguments.
			//If you want to take a look at more complex commands, see the "com.devkev.devscript.nativecommands.NativeLibrary" class 
			new Command("ping", "", "Calling this command will print out pong") {
				@Override
				public Object execute(Object[] args, Process application, Block block) throws Exception {
					application.log("pong!", true);
					return null;
				}
			}
			
		};
	}

	//This function is executed, when a script process imports commands from this library
	//This is especially useful if you want to set up listeners for example key listeners for macros or something
	@Override
	public void scriptImport(Process process) {
		//This function can execute script functions. Again, this is really useful for listeners.
		//You could create a function in the script like keyPressed = {}; and call it from here.
		super.executeEventFunction("keyPressed", "A");
	}
	
	//This function is executed when a script finishes, errors out or is terminated otherwise.
	@Override
	public void scriptExit(Process process, int exitCode, String errorMessage) {
		
	}
}
```

## Use custom commands

You add the library to your process by using the "includeLibrary()" function:

```java
Process p = new Process(true);
p.addSystemOutput();
p.setInput(System.in);
p.clearlibraries(); //Use this function, if you don't want the 70 default commands to be available
p.includeLibrary(new MyCommands()); //Add the library to the current process
p.execute("ping"); //This will print "pong" into the console or wherever you directed the output to
```

## Libraries as .jar files

It is also possible to create Libraries as .jar files that you can import in other scripts using the "import [path-to-jar]" command.<br>
Just add Devscript to the build path of a new project, create any class that extends "Library" (Like above) and export the jar file.<br>
<b>Done!</b><br>
You can now import the file with the "import" command and use them in the GUI editor.

# Thank you for reading

If you have any Ideas or suggestions, feel free to create an issue.
