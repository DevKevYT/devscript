# devscript
A Java inbuild script for developing and debugging

# Implementation

Basic syntax to execute a "Hello World" script as a string inside your program:
Both input and outputs are the default System.in and System.out.

```java
Process p = new Process(true);
p.addSystemOutput();
p.addInput(System.in);
p.execute("println \"Hello World\"");
```

# Syntax
In this big section, I will try to bring you near the usage and capabillities of the DevScript, so you can use them for your own projects

## Commands

### Data Types
*This section is only important, if you want to implement your own library if not, you can skip this section*<br>
There are 6 main data types: _STRING, BOOLEAN, BLOCK, DICTIONARY, ARRAY, OBJECT_ (OBJECT is any Java object)<br>
There are also 4 minor data types: _ANY, ARRAY_ANY, INTEGER, FLOAT, CONTINUE_<br>
They are called, minor types, because they are not considered as 'real' data types. They are only important in specific situations<br>

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

Command names can also be shifted, like the _[string] + [string]_ command to make the code more readable:
  >  1 + 1;
  
But this command alone does not do very much. How do you use the new, returned value?
Look at this example:
  >  println (1 + 1); 

This is the same as:
  >  println 2; 
  
Commands can be combined with others with parantheses.
 >  println (1 + (4 / 2));

## Variables

Variables can get declared with the _[STRING] = [ANY]_ command.
And you can access them with a $ sign, followed by the variable name:
> foo = 10;<br>println (1 + $foo);

  There are also a few inbuild variables:
    ´$true, $false and $null´

## Blocks
Blocks are code, wrapped inside two curly braces { }.
In theDevScript Language, blocks are treated as a data type [BLOCK]. Basically, a block only consists of a code String and
their main goal is to provide functions.
To define a function, you create a variable as usual (With the "+" command), since -as already mentioned- blocks are just data types and can get passed as command argument.
> function = {<br>println "I am a function :)";<br>println "But how do you execute me?"<br>};

To execute a function, use the _"call [BLOCK] [ANY] ..."_ command:
> call $function;

Functions can also accept arguments (This is why the call command has some more optional arguments).<br>
Inside the function, the passed argument values are disguised as $0, $1, $2 and so on, depending on how much arguments you pass.
> function = {<br>  println "Argument 0 is:" $0;<br>  println "Argument 1 is:" $1;<br>};<br>call $function "Argument0" "Argument1";

Last but not least, functions can also return value with the command _return [ANY]_. If you use this command, the "call" command has now a new return value.
> add = {<br>  #Adds argument0 and argument1 and return new new value#<br>  return ($0 + $1);<br>}<br>println (call $add 1 1);

Please take your time to understand this section, as it can get quite complex.
## Arrays
  Arrays can get declared as empty, or filled.
  All arrays are dynamic. They can get altered with ´push [obj] [index]´ and ´pop [index]´.

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

The first argument of the _for [STRING] [STRING] [BLOCK]_ command is the variable name.
>array = ["John" "Peter" "Chris"];<br>for i (length $array) {<br>println $array[$i];<br>};

## Thats it!
Now, if you understand the basic syntax and command usage, you are ready to start!
It may also be handy to notice, that this script also supports threading.
You can use the _help_ command for a list of commands, but I will print them below, so you can have a look.
