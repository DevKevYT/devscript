package com.devkev.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import com.devkev.devscript.raw.ApplicationInput;
import com.devkev.devscript.raw.ApplicationListener;
import com.devkev.devscript.raw.Block;
import com.devkev.devscript.raw.Command;
import com.devkev.devscript.raw.ConsoleMain;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;

public class Window {
	//TODO add case sensitive option
	JFrame window;
	int fontSize = 12;
	JMenuBar bar;
	JTextPane textArea;
	JScrollPane pane;
	Process p;
	
	ApplicationInput input;
	volatile boolean waitForEnter = false;
	volatile int inputStart = 0;
	
	private static Font font;
	private File openedFile = null; //Null means, creating a new file when saving.
	private static String TITLE = "Devscript 1.9.12 Editor ";
	private ArrayList<String> history = new ArrayList<String>();
	private int historyIndex = 0;
	public int maxHistorySize = 50;
	
	JLayeredPane layerPane;
	JPanel previewContainer;
	JLabel commandPreview;
	File exampleRoot;
	FileSystem fileSystem;
	
	public Window() {
		
		try {
	        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
	    } catch (Exception evt) {}
		font = new Font("Consolas", Font.TYPE1_FONT, 13);
		
		window = new JFrame(TITLE + " - unsaved");
		window.setVisible(true);
		window.setResizable(true);
		window.setLayout(null);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				if(textArea == null) return;
				pane.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				layerPane.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				pane.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		window.setMinimumSize(new Dimension(180, 180));
		
		p = new Process(true);
		TITLE = "Devscript " + p.version + " Editor ";
		window.setTitle(TITLE);
		p.addOutput(new Output() {
			public void warning(String message) {
				console.append("[WARN] " + message + "\n");
				consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
			}
			public void log(String message, boolean newline) {
				console.append(message + (newline ? "\n" : ""));
				consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
			}
			public void error(String message) {
				console.append(message + "\n");
				consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
			}
		});
		input = new ApplicationInput() {
			@Override
			public void awaitInput() {
				waitForEnter = true;
				console.setEnabled(true);
				inputStart = console.getText().length();
				console.setCaretPosition(inputStart);
				console.setCaretColor(Color.black);
			}
		};
		p.setInput(input);
		p.includeLibrary(new Library("Custom DevScript Editor Commands") {
			public void scriptImport(Process process) {}
			public void scriptExit(Process process, int exitCode, String errorMessage) {}
			@Override
			public Command[] createLib() {
				return new Command[] {
					new Command("clearConsole", "", "Clears the console") {
						@Override
						public Object execute(Object[] args, Process application, Block block) throws Exception {
							console.setText("");
							return null;
						}
					},
				};
			}
		});
		p.setApplicationListener(new ApplicationListener() {
			public void done(int exitCode) {
				runWindow.setTitle("Finished with Exit Code " + exitCode);
				window.setEnabled(true);
				console.setEnabled(false);
			}
		});
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			
			
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER && input.inputRequested() && waitForEnter && e.getID() == KeyEvent.KEY_RELEASED) {
					input.flush(console.getText().substring(inputStart, console.getCaretPosition()) + "\n");
					waitForEnter = false;
					console.setCaretColor(Color.black);
				}
				return false;
			}
		});
		
		bar = new JMenuBar();
		
		JMenu m = new JMenu("File");
		
		JMenuItem newFile = new JMenuItem("New...");
		newFile.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.setText("");
				openedFile = null;
				window.setTitle(TITLE + " - unsaved");
			}
		});
		m.add(newFile);
		
		JMenuItem loadFile = new JMenuItem(getFormattedBarText("Open..."));
		loadFile.setAccelerator(KeyStroke.getKeyStroke("control O"));
		loadFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.setEnabled(false);
				JFileChooser chooser = new JFileChooser();
				int res = chooser.showOpenDialog(new JFrame());
				if(res == JFileChooser.APPROVE_OPTION) {
					openDocument(chooser.getSelectedFile(), true);
				} else {
					window.setEnabled(true);
				}
			}
		});
		m.add(loadFile);
		JMenuItem saveFile = new JMenuItem("Save File...");
		saveFile.setAccelerator(KeyStroke.getKeyStroke("control S"));
		saveFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(openedFile == null) {
					if(textArea.getText().isEmpty()) return;
					window.setEnabled(false);
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("Save New File");
					int res = chooser.showSaveDialog(new JFrame());
					if(res == JFileChooser.APPROVE_OPTION) saveDocument(chooser.getSelectedFile());
					window.setEnabled(true);
				} else saveDocument(openedFile);
				window.setTitle(TITLE + " - saved");
			}
		});
		m.add(saveFile);
		m.addSeparator();
		
		
		//Examples and tutorials //
		try {
			JMenu examples = new JMenu("Examples");
			walkDemoPath("/Examples", examples);
	
			m.add(examples);
			bar.add(m);
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		
		JMenu m2 = new JMenu("Edit");
		JMenuItem undo = new JMenuItem(getFormattedBarText("Undo"));
		undo.setAccelerator(KeyStroke.getKeyStroke("control Z"));
		undo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(historyIndex > 0) {
					historyIndex--;
					textArea.setText(history.get(historyIndex));
					if(historyIndex == 0) undo.setEnabled(false);
				}
			}
		});
		undo.setEnabled(false);
		m2.add(undo);
		m2.addSeparator();
		JMenuItem selectAll = new JMenuItem(getFormattedBarText("Select All"));
		selectAll.setAccelerator(KeyStroke.getKeyStroke("control A"));
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.selectAll();
			}
		});
		JMenuItem copyToClipboard = new JMenuItem(getFormattedBarText("Copy"));
		copyToClipboard.setAccelerator(KeyStroke.getKeyStroke("control C"));
		copyToClipboard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getSelectedText()),null);
			}
		});
		JMenuItem paste = new JMenuItem(getFormattedBarText("Paste"));
		paste.setAccelerator(KeyStroke.getKeyStroke("control V"));
		paste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					textArea.setText((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
				} catch (HeadlessException | UnsupportedFlavorException | IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		m2.add(selectAll);
		m2.add(copyToClipboard);
		m2.add(paste);
		bar.add(m2);
		
		JMenu m3 = new JMenu("Run");
		JMenuItem run = new JMenuItem(getFormattedBarText("Run"));
		run.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(p.isRunning()) return;
				console.setText("");
				runWindow.toFront();
				runWindow.setTitle("Running...");
				runWindow.setVisible(true);	
				window.setEnabled(false);
				p.setCaseSensitive(false);
				p.execute(textArea.getText(), true);
				p.setVariable("keyCode", "", false, true);
			}
		});
		m3.add(run);
		bar.add(m3);
		
		JMenu m4 = new JMenu("Help");
		JMenuItem commandCC = new JMenuItem("Command Cheatsheet");
		commandCC.setToolTipText("Runs the 'help' command");
		commandCC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(p.isRunning()) return;
				console.setText("");
				runWindow.toFront();
				runWindow.setVisible(true);
				p.execute("help", false);
			}});
		m4.add(commandCC);
		m4.addSeparator();
		JMenuItem about = new JMenuItem("Changelog");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().browse(new URI("https://github.com/DevKevYT/devscript"));
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
		});
		m4.add(about);
		m4.addSeparator();
		JMenuItem ty = new JMenuItem("Thank you! <3");
		ty.setEnabled(false);
		m4.add(ty);
		bar.add(m4);
		
		window.setJMenuBar(bar);
		
		textArea = new JTextPane();
		textArea.setFont(font);
		textArea.setLayout(null);
		textArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(!e.isControlDown()) window.setTitle(TITLE + " - unsaved");
				commandPreview.setVisible(false);
			}
			public void keyReleased(KeyEvent e) {
				if(e.isControlDown()) return;
				history.add(textArea.getText());
				if(history.size() >= 20) history.remove(0);
				historyIndex = history.size()-1;
				undo.setEnabled(true);
			}
			public void keyPressed(KeyEvent e) {}
		});
		pane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setBounds(5, 5, window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
		pane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		commandPreview = new JLabel("<html><body>println [STRING]<br>print [STRING]</body></html>");
		commandPreview.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		commandPreview.setOpaque(true);
		commandPreview.setVisible(false);
		commandPreview.setForeground(Color.darkGray);
		commandPreview.setFont(new Font("Arial", Font.BOLD, 11));
		commandPreview.setBounds(0,  0,  200, 60);
		
		layerPane = new JLayeredPane();
		layerPane.add(pane);
		layerPane.add(commandPreview);
		layerPane.setLayer(commandPreview, 999);
		window.add(layerPane);
		
		initRunWindow();
		window.pack();
		window.setSize(500, 500);
		
	}
	
	private boolean isPath(String path) {
		try {
			URI uri = ConsoleMain.class.getResource(path).toURI();
	        Path myPath;
	        if (uri.getScheme().equals("jar")) {
	        	if(fileSystem == null) {
	        		fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
	        	}
	            myPath = fileSystem.getPath(path);
	        } else {
	            myPath = Paths.get(uri);
	        }
	        Stream<Path> walk = Files.walk(myPath, 1);
	        for (Iterator<Path> it = walk.iterator(); it.hasNext();){
	        	Path p = it.next();
	        	if(!p.toString().equals(path)) {
	        		walk.close();
	        		return  true;
	            }
	        }
	        walk.close();
	        return false;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private int walkDemoPath(String path, JMenu parent) {
		try {
			URI uri = ConsoleMain.class.getResource(path).toURI();
	        Path myPath;
	        if (uri.getScheme().equals("jar")) {
	        	if(fileSystem == null) {
	        		fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
	        	}
	            myPath = fileSystem.getPath(path);
	        } else {
	            myPath = Paths.get(uri);
	        }
	        Stream<Path> walk = Files.walk(myPath, 1);
	        int fileCount = 0;
	        for (Iterator<Path> it = walk.iterator(); it.hasNext();){
	        	Path p = it.next();
	        	if(!p.toString().equals(path)) {
		        	if(isPath(p.toString())) {
		        		JMenu menu = new JMenu(p.getFileName().toString().replace("/", ""));
						parent.add(menu);
						walkDemoPath(p.toString(), menu);
		        	} else {
		        		JMenuItem example = new JMenuItem();
						example.setText(p.getFileName().toString());
						example.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {
								try {
									openDocumentByStream(p.toString());
								} catch (IOException e) {
									e.printStackTrace();
									window.setEnabled(true);
								}
							}
						});
						parent.add(example);
		        	}
	        	}
	        }
	        walk.close();
	        return fileCount;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public void setScript(String script) {
		appendToPane(textArea, script, Color.black);
	}
	
	private void openDocumentByStream(String path) throws IOException {
		history.clear();
		textArea.setText("");
		
		try (InputStream in = getClass().getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line = reader.readLine();
			while(line != null) {
				appendToPane(textArea, line + "\n", Color.black);
				line = reader.readLine();
			}
			reader.close();
			textArea.setCaretPosition(0);
			window.setEnabled(true);
			window.toFront();
			window.setTitle(TITLE);
		}
	}
	
	public void openDocument(File file, boolean save) {
		try {
			history.clear();
			textArea.setText("");
			if(file != null) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				while(line != null) {
					appendToPane(textArea, line + "\n", Color.black);
					line = reader.readLine();
				}
				reader.close();
			}
			if(save) openedFile = file;
			textArea.setCaretPosition(0);
			window.setEnabled(true);
			window.toFront();
			window.setTitle(TITLE);
		} catch (Exception e1) {
			e1.printStackTrace();
			window.setEnabled(true);
		}
	}
	
	public void saveDocument(File file) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(textArea.getText());
			writer.close();
			openedFile = file;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	JFrame saveDialog;
	JButton approveSave;
	JButton cancelSave;
	
	public void initSaveNotification() {
		saveDialog = new JFrame("Save changes to ");
	}
	
	JFrame runWindow;
	JTextArea console;
	JScrollPane consolePane;
	
	public void initRunWindow() {
		runWindow = new JFrame();
		runWindow.setVisible(false);
		runWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		runWindow.addWindowListener(new WindowListener() {
			public void windowClosed(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowClosing(WindowEvent arg0) {
				if(p.isRunning()) {
					System.out.println("Closing running instance");
					p.kill(p.getMain(), "Interrupted by program");
				}
				window.setEnabled(true);
				inputStart = 0;
				input.flush(null);
				console.setText("");
			}
			public void windowActivated(WindowEvent arg0) {}
		});
		runWindow.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				console.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				console.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		
		console = new JTextArea();
		console.setFont(font);
		console.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if(input.inputRequested() && console.getCaretPosition() < inputStart) {
					console.setCaretPosition(inputStart);
				}
			}
		});
		console.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if(!input.inputRequested()) return;
				if(console.getCaretPosition() == inputStart && e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					e.consume();
				}
			}
		});
		
		console.setEnabled(false);
		console.setDisabledTextColor(Color.DARK_GRAY);
		consolePane = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		consolePane.setBounds(5, 5, runWindow.getRootPane().getWidth()-10,  runWindow.getRootPane().getHeight()-10);
		consolePane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		consolePane.setAutoscrolls(true);
		runWindow.add(consolePane);
		
		runWindow.pack();
		runWindow.setBounds((int) (Toolkit.getDefaultToolkit().getScreenSize().width *.5f-250), (int) (Toolkit.getDefaultToolkit().getScreenSize().height *.5f-100), 500, 200);
	
		runWindow.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				if(p.isRunning()) {
					p.setVariable("keyCode", "", false, false);
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if(p.isRunning()) {
					//Set the variable "keyPressed" to the char that is pressed
					p.setVariable("keyCode", e.getKeyChar(), false, false);
				}
			}
		});
	}
	
	private String getFormattedBarText(String text) {
		return String.format("%s%20s", text, "");
	}
	
    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Consolas");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
        try {
        	tp.setCaretPosition(tp.getText().length()-1); //WTF
        } catch(Exception e) {}
    }
}
