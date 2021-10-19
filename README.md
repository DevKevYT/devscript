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
You can also execute the script from the command line with the raw jar file and the argument --nogui.<br>
This example will execute the command "version" and print the current version.
> java -jar devscript_1.9.0.jar --nogui -e "version"

Command line arguments are:<br>
- -e or --execute <script> Executes a script right from the command line
- -f or --file <pathToFile> Executes the contents of a text file
- --nogui rejects the program to open the GUI editor
- If only --nogui is given, the jar opens the default editor, stored in Editor.txt

# Syntax
In this big section, I will try to bring you near the usage and capabillities of the DevScript, so you can use them for your own projects

## Commands

### Data Types
There are 6 main data types: _STRING, BOOLEAN, BLOCK, DICTIONARY, ARRAY, OBJECT_ (OBJECT is any java object, except the complex and primitive data types)<br>
There are also 4 sub- data types: _ANY, ARRAY_ANY, INTEGER, FLOAT, CONTINUE_<br>
Sub- data types are not considered as 'real' data types. They are only important in specific situations<br>

This would be a typical command, that expects certain argument types:
> exampleCommand [BLOCK] [ANY] [STRING] [CONTINUE];

The example command expects a block (Discussed in section _Block_) as the first argument, then any other type and Strings.
The [CONTINUE] type means, there is no limitation for the last argument in number,<br>
so you could pass an infinite amount of STRINGs (Because the last type before CONTINUE was a string).

### Command Syntax
The whole syntax is based on commands. These act like functions, that means, they can return values and accept arguments and are separated with ';'.<br>
Example for the command println:
  - This command expects strings without limitation in number: _println [STRING] [CONTINUE]_ and returns $null, that means nothing.
  > println "Hello World" "And another line";

Command names can also be shifted, like the _[string] + [string]_ command to make the code more readable.
So, instead of writing + 1 1 ('+' or 'add' being the command here) we can shift the command one section to the right:
  >  1 + 1; <- Better to read and write!

This command returns the sum of its two arguments.
But this command alone does not do very much. How do you use the new, returned value?
Look at this example:
  >  println (1 + 1); 

This is the same as:
  >  println 2; 
  
Commands can be combined with others with parantheses.
 >  println (1 + (4 / 2));
 
 Note that if you want to use $null as a returned value, the program will throw an error, because $null is not interpreted as argument,<br>
 so the interpreter thinks this command has not arguments: use [STRING] != use
 >  use (println "null?!")<br>Error at [println "null?!"] No such command use.

## Variables

Variables can get declared with the _[STRING] = [ANY]_ command.
And you can access them with a $ sign, followed by the variable name:
> foo = 10;<br>println (1 + $foo);

  There are also a few inbuild variables:
    ´$true, $false and $null´

## Blocks
Blocks are code, wrapped inside two curly braces { }.
In theDevScript Language, blocks are treated as a data type [BLOCK]. Basically, a block only consists of a String of code and
their main goal is to provide functions.
To define a function, you create a variable as usual (With the "=" command), since -as already mentioned- blocks are just data types and can get passed as command argument.
> function = {<br>println "I am a function :)";<br>println "But how do you execute me?"<br>};

To execute a function, use the _"call [BLOCK] [CONTINUE] ..."_ command:
> call $function;

Functions can also accept arguments (This is why the call command has some more optional arguments).<br>
Inside the function, the passed argument values are disguised as $0, $1, $2 and so on, depending on how much arguments you pass.
> function = {<br>  println "Argument 0 is:" $0;<br>  println "Argument 1 is:" $1;<br>};<br>call $function "Argument0" "Argument1";

Last but not least, functions can also return value with the command _return [ANY]_. If you use this command, the "call" command has now a new return value.
> add = {<br>  #Adds argument0 and argument1 and return new new value#<br>  return ($0 + $1);<br>}<br>println (call $add 1 1);

Please take your time to understand this section, as it can get quite complex.
## Arrays
  Arrays can get declared as empty, or filled.
  All arrays are dynamic. They can get altered with ´push [ANY] [ARRAY]´ and ´pop [STRING]´.

> emptyArray = [];<br>
filledArray = ["string1" "string2" $true (20 + 12) {println "Arrays can hold blocks. They are data types :)"}];<br>
newObject = "This is a String";<br>
push $newObject $emptyArray;
    
As you can see, you can set multiple data types to one array.
However if an array -for example- only has Strings, the type of the array will be STRING.
- $filledArray would have the type ANY, since it contains multiple, different data types, such as STRING, BOOLEAN, BLOCK
- $emptyArray's type would be STRING: It has only strings in it.<br>
_Tip: To check a variable for a type, you can use the [ANY] typeof [STRING] command.<br>
The [STRING] argument would be the type written out. Different types are listed in the Section: Data Types. Minor and Major work.<br>
$filledArray typeof "any" would return true._

Also arrays with multiple dimensions are possible. You access an array index like in any other programming language:
>array = [["inner" "inner2"] "string"];<br>println $array[1];<br>println $array[0][1];

And if we are discussing Arrays and you already know about blocks, you may want to learn how to use for loops etc.:
This would be a typical for- loop:
>for i 10 {<br>println "This text will be printed 10 times!";<br>println "Iteration: " $i;<br>};

The first argument of the _for [STRING] [STRING] [BLOCK]_ command is the variable name starting at zero.
>array = ["John" "Peter" "Chris"];<br>for i (length $array) {<br>println $array[$i];<br>};

## If
Here is an example of an if- statement:
>if (10 == 11) {<br>println "Condition is true";<br>} {<br>println "Condition is false";<br>};

But keep in mind, that the parantheses after the if are just wrapping another command ([ANY] == [ANY]) and are not there like in Java or other languages.
>if $true {<br>println "true"<br>};
## Thats it!
Now, if you understand the basic syntax and command usage, you are ready to start!
It may also be handy to notice, that this script also supports threading.
You can use the _help_ command for a list of commands, but I will print them below, so you can have a look.

# All default commands

LIBRARY 'Native' (65 Commands)<br>
println [ARRAY_ANY]											Prints any object's toString() method in a new line<br>
print [ARRAY_ANY]                       Prints any objecs's toString() method<br>
[STRING] = [ARRAY_ANY]                  Defines a variable. Access it with $variableName<br>
[ARRAY_ANY] === [ANY] [STRING]          [object] === [array] [index] [index] ... Like '=' for arrays<br>
[STRING] + [STRING]                     Adds two numbers and returns the result, if either of the arguments is not a number, two strings are added<br>
[STRING] - [STRING]                     Subtracts two numbers<br>
[STRING] * [STRING]                     Multiplies two numbers<br>
[STRING] / [STRING]                     Divides two numbers<br>
exec [STRING]                           Executes a shell command<br>
script [STRING]                         Executes a new script sub-process with its parent in- and outputs<br>
length [ANY]                            Returns the size of the array<br>
pop [ANY] [STRING]                      Removes the specified index of the array<br>
createdict                              Returns an empty dictionary<br>
get [DICTIONARY] [STRING]               get [dictionary] [key] Returns the corresponding value of the key inside the dictionary<br>
set [DICTIONARY] [STRING] [ARRAY_ANY]   set [dictionary] [key] [object]<br>
remove [DICTIONARY] [STRING]            remove [dictionary] [key] Returns false, if there was no associated key with the name.<br>
clear [DICTIONARY]                      clear [dict] Removes all values from the dictionary<br>
isEmpty [DICTIONARY]                    Checks, if a dictionary is empty<br>
push [ARRAY_ANY] [ANY]                  Pushes a new value into the array<br>
[STRING] lt [STRING]                    Returns true, if argument 1 is less than 2 (If the arguments are not numbers, it checks the length of the string)<br>
[STRING] lteq [STRING]                  Returns true, if argument 1 is less or equal than 2 (If the arguments are not numbers, it checks the length of the string)<br>
[STRING] gteq [STRING]                  Returns true, if argument 1 is grater or equal than 2 (If the arguments are not numbers, it checks the length of the string)<br>
[STRING] gt [STRING]                    Returns true, if argument 1 is grater than 2 (If the arguments are not numbers, it checks the length of the string)<br>
[ANY] == [ANY]                          Returns true, if argument 1 and 2 are equal<br>
[ANY] != [ANY]                          Returns true, if argument 1 and 2 are not equal<br>
not [BOOLEAN]                           Inverts a boolean<br>
[BOOLEAN] and [BOOLEAN] [ANY]           Chain boolean conditions: if ($true and $true or $false) {...<br>
random                                  Returns a random number between 0 and 1<br>
[BOOLEAN] or [BOOLEAN] [ANY]            Chain boolean conditions: if ($true or $true and $false) {...<br>
int [ANY]                               Casts the given value into java.lang.Integer<br>
float [ANY]                             Casts the given object into java.lang.Float<br>
string [ANY]                            Casts the given value into java.lang.String<br>
call [BLOCK] [ARRAY_ANY]                Executes a function (Variable that is a block: x = { function code... }. You can also pass arguments. Access them inside the block with $0 $1 etc...Returns the returned value of the function<br>
if [BOOLEAN] [BLOCK] [BLOCK]            If statement<br>
if [BOOLEAN] [BLOCK]                    If statement<br>
ifnot [BOOLEAN] [BLOCK]                 Inverted If-statement<br>
for [STRING] [STRING] [BLOCK]           For loop: for i 10 {...}<br>
loop [BLOCK]                            Infinite loop. Use the break command inside it.<br>
break                                   Breaks out of the next found loop in the stack. If no loop was found, this command interrupts the block<br>
kill [STRING]                           Stops the application and throws an error message<br>
input                                   Reads input from the set input stream<br>
[ARRAY_ANY] typeof [STRING]             Argument 2 is a string representation of the type like the command arguments. There are two adittional useful types: int and float<br>
wait [STRING]<br>
thread [STRING] [BLOCK]                 Runs a separate thread along the process<br>
kill [BLOCK]<br>
pause                                   Pauses the block, the command is executed in<br>
pause [BLOCK]                           Pauses the specified block, if it is running in a separate thread.<br>
waitfor [BLOCK]                         Waits for a block to finish<br>
wake [BLOCK]                            Wakes the specified thread<br>
alive [BLOCK]<br>
return [ARRAY_ANY]                      Searches the first occurrence of a block that is a function and returns its given value or null (Block execution gets terminated)<br>
return [BLOCK]<br>
charAt [STRING] [STRING]                [index] [string]<br>
stringLength [STRING]<br>
toArray [STRING]<br>
substring [STRING] [STRING] [STRING]    [string] [begin] [end]<br>
version                                 Prints the version of the script<br>
help                                    Prints all the available commands with a brief explanation and arguments<br>
import [STRING]                         Imports a library from a compiled .jar file. The class should extend com.mygdx.devkev.devscript.raw.Library and be named CustomLibrary<br>
You can use a * to reference the current path the process is executed in (*/library.jar<br>
getFile [STRING]                        Returns a java.io.File object<br>
fileExists [OBJECT]                     Checks if a file exists<br>
deleteFile [OBJECT]                     Deletes a file. You can use this command, if you want to clear a files content and append lines with the writeFileLine command<br>
writeFileLine [OBJECT] [STRING]         [file] [content] Appends a new line to the file<br>
listDirectory [OBJECT]                  Returns an array containing all files inside this directory<br>
isDirectory [OBJECT]<br>
