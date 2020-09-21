package com.devkev.devscript.raw;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import com.devkev.gui.Window;

/**@author Philipp Gersch
 * @version 1.8.2 (stable)
 * Valid arguments are: <br>-e or --execute [some code] - Executes the String<br>
 * -f or --file [path] - Reads the file and executes the code from it.<br>
 * If no argument was passed, the default Editor GUI will open.*/
public class ConsoleMain {
	
	/*Argument schema: -f <string> -e <string> --nogui </>
	 * When nogui is not given, the gui program will start with the given script from the command line
	 * */
	
	public static void main(String[] args) throws IOException {
		boolean initGUI = true;
		String scriptToExecute = null;
		String filePath = null;
		//ALL arguments need to be checked
		
		for(int i = 0; i < args.length; i++) {
			if((args[i].equals("-e") || args[i].equals("--execute")) && scriptToExecute == null) {
				if(i + 1 < args.length) {
					if(!isArgument(args[i+1])) {
						scriptToExecute = args[i+1];
						i++; //Skip the next iteration
						continue;
					}
				}
				throw new IllegalArgumentException("Expecting string after " + args[i] + " [script_to_execute]");
			} else if((args[i].equals("-f") || args[i].equals("--file")) && filePath == null) {
				if(i + 1 < args.length) {
					if(!isArgument(args[i+1])) {
						filePath = args[i+1];
						i++;
						continue;
					} 
				}
				throw new IllegalArgumentException("Expecting string after " + args[i] + " [path_to_file]");
			} else if(args[i].equals("--nogui")) {
				initGUI = false;
				continue;
			}
			System.out.println("Valid arguments are:\n-f | --file\tOpens the file in the editor (If --nogui is set, the file is executed)\n-e | --execute\tOpens the script with the editor (If --nogui is set, the script is executed)\n   | --nogui\tOpens the command line editor");
			throw new IllegalArgumentException("Unknown argument " + args[i] );
		}
		
		if(initGUI) {
			Window w = new Window();
			if(filePath != null) {
				w.openDocument(new File(filePath));
			} else if(scriptToExecute != null) {
				w.setScript(scriptToExecute);
			}
		} else {
			Process p = new Process(true);
			p.addSystemOutput();
			p.setInput(System.in);
			
			if(filePath != null) {
				p.execute(new File(args[1]), true);
			} else if(scriptToExecute != null) {
				p.execute(args[1], true);
			} else {
				if(ConsoleMain.class.getResourceAsStream("/Editor.txt") == null) {
					System.err.println("Editor file is missing at: " + URLDecoder.decode(ConsoleMain.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));
					return;
				}
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
		}
	}
	
	private static boolean isArgument(String arg) {
		return arg.equals("-f") || arg.equals("--file") || arg.equals("-e") || arg.equals("--execute") || arg.equals("--nogui");
	}
}
