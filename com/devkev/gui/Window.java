package com.devkev.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;

public class Window {
	
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
	
	public Window() {
		try {
	        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
	    } catch (Exception evt) {}
		font = new Font("Consolas", Font.PLAIN, 13);
		
		window = new JFrame("Devscript 1.9.0 Editor (pre alpha build)");
		window.setVisible(true);
		window.setResizable(true);
		window.setLayout(null);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				if(textArea == null) return;
				pane.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				pane.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		window.setMinimumSize(new Dimension(180, 180));
		
		p = new Process(true);
		p.addOutput(new Output() {
			public void warning(String message) {
				console.append("[WARN]" + message + "\n");
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
				System.out.println("Waiting for input...");
				waitForEnter = true;
				console.setEnabled(true);
				inputStart = console.getText().length();
				console.setCaretPosition(inputStart);
				console.setCaretColor(Color.blue);
			}
		};
		p.setInput(input);
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
				if(e.getKeyCode() == KeyEvent.VK_ENTER && input.inputRequested() && waitForEnter) {
					input.flush(console.getText().substring(inputStart, console.getCaretPosition()) + "\n");
					waitForEnter = false;
					console.setCaretColor(Color.black);
				}
				return false;
			}
		});
		
		bar = new JMenuBar();
		
		JMenu m = new JMenu("File");
		
		JMenuItem newFile = new JMenuItem("New");
		newFile.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.setText("");
				openedFile = null;
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
					try {
						textArea.setText("");
						BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()));
						String line = reader.readLine();
						while(line != null) {
							appendToPane(textArea, line + "\n", Color.black);
							line = reader.readLine();
						}
						reader.close();
						window.setEnabled(true);
						openedFile = chooser.getSelectedFile();
					} catch (Exception e1) {
						e1.printStackTrace();
						window.setEnabled(true);
					}
				} else {
					window.setEnabled(true);
				}
			}
		});
		m.add(loadFile);
		JMenuItem saveFile = new JMenuItem("Save File");
		saveFile.setAccelerator(KeyStroke.getKeyStroke("control S"));
		saveFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(openedFile == null) {
					if(textArea.getText().isEmpty()) return;
					window.setEnabled(false);
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("Save new file");
					int res = chooser.showSaveDialog(new JFrame());
					if(res == JFileChooser.APPROVE_OPTION) {
						BufferedWriter writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile()));
						try {
							writer.write(textArea.getText());
							writer.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					window.setEnabled(true);
				} else System.out.println("Overwriging file...");
				
			}
		});
		m.add(saveFile);
		bar.add(m);
		
		JMenu m2 = new JMenu("Edit");
		bar.add(m2);
		
		JMenu m3 = new JMenu("Run");
		JMenuItem run = new JMenuItem(getFormattedBarText("Run in Shell"));
		run.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(p.isRunning()) return;
				console.setText("");
				runWindow.toFront();
				runWindow.setTitle("Running...");
				runWindow.setVisible(true);	
				window.setEnabled(false);
				p.execute(textArea.getText(), true);
			}
		});
		m3.add(run);
		bar.add(m3);
		
		window.setJMenuBar(bar);
		
		textArea = new JTextPane();
		textArea.setFont(font);
		pane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setBounds(5, 5, window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
		pane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		window.add(pane);	
		
		initRunWindow();
		window.pack();
		window.setSize(500, 500);
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
				System.out.println(console.getCaretPosition() + " " + inputStart);
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
