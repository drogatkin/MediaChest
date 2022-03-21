/* PhotoOrganizer - $RCSfile: TwoPanesView.java,v $                               
 * Copyright (C) 2001 Dmitriy Rogatkin.  All rights reserved.                         
 * Redistribution and use in source and binary forms, with or without                 
 * modification, are permitted provided that the following conditions                 
 * are met:                                                                           
 * 1. Redistributions of source code must retain the above copyright                  
 *    notice, this list of conditions and the following disclaimer.                   
 * 2. Redistributions in binary form must reproduce the above copyright               
 *    notice, this list of conditions and the following disclaimer in the             
 *    documentation and/or other materials provided with the distribution.            
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND            
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE             
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE        
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR        
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL            
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR        
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER        
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT                
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY         
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF            
 *  SUCH DAMAGE.                                                                      
 *                                                                                    
 *  Visit http://drogatkin.openestate.net to get the latest information               
 *  about Rogatkin's products.                                                        
 *  $Id: TwoPanesView.java,v 1.8 2012/10/18 06:58:59 cvs Exp $                
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import photoorganizer.Controller;
import photoorganizer.Resources;

public class TwoPanesView extends JPanel implements ActionListener {
	JTextArea lowerView;

	JEditorPane upperView;

	JScrollPane upperScroll;

	Window window;

	Object modal;

	String returnCode;

	protected TwoPanesView(boolean noLowerPanel, Window window, int btnMsk, ActionListener listener) {
		setLayout(new BorderLayout());
		add(upperScroll = new JScrollPane(upperView = new JEditorPane()), "North");
		upperView.setEditable(false);
		add(Controller.createButtonPanel(listener != null ? listener : this, btnMsk, FlowLayout.CENTER), "Center");
		if (!noLowerPanel)
			add(new JScrollPane(lowerView = new JTextArea()), "South");
		this.window = window;
		if (window != null && window instanceof JFrame) {
			JFrame frame = (JFrame) window;
			frame.setContentPane(this);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					setReturnCode(Resources.CMD_CANCEL);
					TwoPanesView.this.window.dispose();
				}
			});
		}
	}

	static public TwoPanesView createFramed(boolean noLowerPanel, Window window, int btnMsk, ActionListener listener) {
		return new TwoPanesView(noLowerPanel, window, btnMsk, listener);
	}

	public void setSize(int w, int h) {
		if (window != null)
			window.setSize(w, h);
		else
			super.setSize(w, h);
		upperScroll.setPreferredSize(new Dimension(w - 20, h * 80 / 100));
	}

	public void actionPerformed(ActionEvent a) {
		setReturnCode(a.getActionCommand());
		if (window != null)
			window.dispose();
	}

	void setReturnCode(String code) {
		returnCode = code;
		if (modal != null)
			synchronized (modal) {
				modal.notify();
			}
	}

	public void show() {
		if (window != null)
			window.setVisible(true);
	}

	// not thread safe
	public String showModal() {
		modal = new Object();
		show();
		synchronized (modal) {
			try {
				modal.wait();
			} catch (InterruptedException ie) {
				// too bad
				returnCode = null;// Resources.CMD_CANCEL;
			}
		}
		return returnCode;
	}

	public void setUpperText(String text) {
		// upperView.setEditorKit( new javax.swing.text.html.HTMLEditorKit());
		setUpperText(text, "text/html");
	}

	public void setUpperText(String text, String type) {
		// System.err.println("Editor:
		// "+upperView.getEditorKitClassNameForContentType(type));
		upperView.setContentType(type);
		upperView.setText(text);
		upperView.setDocument(upperView.getDocument());
	}

	public void readToUpper(InputStream is) throws IOException {
		upperView.setContentType("text/html");
		upperView.read(is, new javax.swing.text.html.HTMLEditorKit());
	}

	public void setUpperURL(URL url) throws IOException {
		upperView.setPage(url);
	}

	public void setLowerText(String text) {
		if (lowerView != null)
			lowerView.setText(text);
	}

	public void appendLowerText(String text) {
		if (lowerView != null)
			lowerView.setText(lowerView.getText() + text);
	}
}
