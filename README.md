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

To get a list of all the commands, you can execute the command "help", but i will include a buch of them here:
