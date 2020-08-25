# devscript
A Java inbuild script for developing and debugging

Basic syntax to execute a "Hello World" script as a string inside your program:
Both input and outputs are the default System.in and System.out.

```java
Process p = new Process(true);
p.addSystemOutput();
p.addInput(System.in);
p.execute("println \"Hello World\"");
```

Syntax:

The whole syntax is based on commands. These act like functions, that means, they can return values and accept arguments.
These arguments can be consistent of: String, Object, Boolean, Null, Any, Array_Any, Dictionary.
Example for the command println:
  This command expects an unlimited amount of strings: println <string> ...;
  ´´´java
  println "Hello World" "And another line";
  ´´´
