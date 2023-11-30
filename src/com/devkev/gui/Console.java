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

public class Console extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private com.devkev.devscript.raw.Process p;
	private Window parent;
	
	volatile boolean waitForEnter = false;
	volatile int inputStart = 0;
	
	public Console(com.devkev.devscript.raw.Process p, Window parent) {
		this.p = p;
		this.parent = parent;
		init();
	}
	
	private void init() {
		setFont(new Font("Consolas", Font.PLAIN, 12));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 500, 239);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JButton terminate = new JButton("");
		terminate.setIcon(new ImageIcon("C:\\Users\\Philipp\\Desktop\\terminate-active.png"));
		terminate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		terminate.setBounds(451, 166, 23, 23);
		contentPane.add(terminate);
		
		JButton rerun = new JButton("");
		rerun.setIcon(new ImageIcon("C:\\Users\\Philipp\\Desktop\\rerun-active.png"));
		rerun.setBounds(418, 166, 23, 23);
		contentPane.add(rerun);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 464, 144);
		contentPane.add(scrollPane);
		
		JTextArea console = new JTextArea();
		scrollPane.setViewportView(console);
		
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
				console.setText("");
			}
			public void windowActivated(WindowEvent arg0) {}
		});
		
		addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				console.setSize(parent.window.getRootPane().getWidth() - 10, parent.window.getRootPane().getHeight()-parent.bar.getHeight() - 10);
				console.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		
	}
}
