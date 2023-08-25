# Devscript
A script inside Java for developing and debugging

# Implementation

Just copy the Code into your project or import the downloadable .jar (under "releases") file to your build path and you are done!<br>
DevScript does not use any unnessecary 3rd party libraries.

Basic syntax to execute a "Hello World" script as a string inside your program:
Both input and outputs are the default System.in and System.out, but you can define your own.

```java
Process p = new Process(true);
p.addSystemOutput();
p.setInput(System.in);
p.execute("println \"Hello World\"", false);
p.execute(new File("path_to_file"), true);
```
# Running the Editor
Download the newest DevScript .jar File unter [releases](https://github.com/DevKevYT/devscript/releases) and just run the file.<br>
Java 1.8 or higher needs to be installed.

You can also execute the script from the command line with the raw jar file and the argument --nogui.<br>
This example will execute the command "version" and print the current version.
> java -jar devscript_1.9.0.jar --nogui -e "version"

Command line arguments are:<br>
- -e or --execute <script> Executes a script right from the command line
- -f or --file <pathToFile> Executes the contents of a text file
- --nogui rejects the program to open the GUI editor
- If only --nogui is given, the jar opens the default editor, stored in Editor.txt

# Syntax
Examples and tutorial on how the syntax work can be found [here](Examples)<br>
Or just download the .jar from the releases and start tinkering with the inbuild editor!
