package com.devkev.gui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import java.awt.Font;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;
import java.awt.Color;
import javax.swing.JLabel;

public class Console extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private com.devkev.devscript.raw.Process p;
	private Window parent;
	
	JTextArea consoleText;
	JScrollPane consolePane;
	JLabel consoleStatus;
	
	volatile boolean waitForEnter = false;
	volatile int inputStart = 0;
	
	public Console(com.devkev.devscript.raw.Process p, Window parent) {
		setTitle("DevScript Console");
		this.p = p;
		this.parent = parent;
		init();
	}
	
	private void init() {
		setFont(new Font("Consolas", Font.PLAIN, 12));
		setBounds(100, 100, 500, 239);
		setFocusableWindowState(true);
		setEnabled(true);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JButton terminate = new JButton("");
		terminate.setToolTipText("Terminate");
		terminate.setBorder(new LineBorder(new Color(0, 0, 0)));
		terminate.setIcon(new ImageIcon(Console.class.getResource("/icon/terminate-active.png")));
		terminate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		terminate.setBounds(456, 5, 23, 23);
		terminate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(parent.p.isRunning()) {
					parent.p.kill(parent.p.getMain(), "Terminated by DevScript Console");
					consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
				}
			}
		});
		
		contentPane.add(terminate);
		
		JButton rerun = new JButton("");
		rerun.setToolTipText("Rerun Script");
		rerun.setBorder(new LineBorder(new Color(0, 0, 0)));
		rerun.setIcon(new ImageIcon(Console.class.getResource("/icon/rerun-active.png")));
		rerun.setBounds(429, 5, 23, 23);
		rerun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(p.isRunning()) return;
				
				consoleText.setText("");
				toFront();
				setEnabled(true);
				consoleStatus.setText("Running ...");
				setVisible(true);	
				//window.setEnabled(false);
				
				p.execute(parent.textArea.getText(), true);
				p.setVariable("keyCode", "", false, true);
			}
		});
		contentPane.add(rerun);
		
		consolePane = new JScrollPane();
		consolePane.setBounds(0, 35, 484, 165);
		contentPane.add(consolePane);
		
		consoleText = new JTextArea();
		consolePane.setViewportView(consoleText);
		
		consoleStatus = new JLabel("");
		consoleStatus.setBounds(5, 5, 414, 23);
		contentPane.add(consoleStatus);
		
		addKeyListener(new KeyListener() {
			
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
		
		addWindowListener(new WindowListener() {
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
				parent.window.setEnabled(true);
				inputStart = 0;
				parent.input.flush(null);
				consoleText.setText("");
			}
			public void windowActivated(WindowEvent arg0) {}
		});
		
		addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				consolePane.setSize(getRootPane().getWidth(), getRootPane().getHeight()-parent.bar.getHeight() - 10);
				terminate.setLocation(getRootPane().getWidth() - 28, 5);
				rerun.setLocation(getRootPane().getWidth() - 58, 5);
				consolePane.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		
	}
}
