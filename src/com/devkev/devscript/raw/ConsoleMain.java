package com.devkev.devscript.raw;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**@author Philipp Gersch
 * @version 1.8.2 (stable)
 * Valid arguments are: <br>-e or --execute [some code] - Executes the String<br>
 * -f or --file [path] - Reads the file and executes the code from it.<br>
 * If no argument was passed, the default Editor GUI will open.*/
public class ConsoleMain {
	public static void main(String[] args) throws IOException {
		Process p = new Process(true);
		p.addSystemOutput();
		p.setInput(System.in);
		
		if(args.length == 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(ConsoleMain.class.getResourceAsStream("/Editor.txt")));
			String code  = "";
			String line = reader.readLine();
			while(line != null) {
				code += line;
				line = reader.readLine();
			}
			reader.close();
			p.execute(code, false);
			return;
		}
		
		if(args[0].equals("-e") || args[0].equals("--execute")) {
			if(args.length < 2) {
				System.err.println("Argument " + args[0] + " expects an executable script, e.g.: -e \"println \"Hello World;\"\"");
				System.exit(-1);
			}
			p.execute(args[1], true);
		} else if(args[0].equals("-f") || args[0].equals("--file")) {
			if(args.length < 2) {
				System.err.println("Argument " + args[0] + " expects a path to a text file containing the script");
				System.exit(-1);
			}
			p.execute(new File(args[1]), true);
		} 
	}
}
