# devscript
A Java inbuild script for developing and debugging

Example:
´´´
Process p = new Process(true);
p.addSystemOutput();
p.addInput(System.in);
p.execute("println \"Hello World\"", false);
´´´ 
