/* PhotoOrganizer - SearchPanel.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
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
 *  Visit http://PhotoOrganizer.sourceforge.net to get the latest information
 *  about Rogatkin's products.                                                        
 *  $Id: SearchPanel.java,v 1.8 2012/10/18 06:58:59 cvs Exp $                
 *  Created on Dec 19, 2006
 *  @author Dmitriy
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.ipod.ITunesDB;

public class SearchPanel extends JPanel /*implements Action */{

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(210, Math.max(t_search.getPreferredSize().height, 28));
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public SearchPanel(Controller c) {
		controller = c;
		this.setLayout(new BorderLayout()); // consider BoxLayout with X_AXIS 
		add(t_search = new JTextField(Resources.MSG_SRCH_BOX), BorderLayout.CENTER);
		t_search.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent arg0) {
				//System.err.printf("Focus %s%n", t_search.getText());
				if (Resources.MSG_SRCH_BOX.equals(t_search.getText()))
					t_search.setText("");
			}

			public void focusLost(FocusEvent arg0) {

			}
		});
		add(c_type = new JComboBox(new String[] { Resources.LIST_IPOD_SRCH_ALL, Resources.LIST_IPOD_SRCH_TITLE }),
				BorderLayout.WEST);
		JButton b;
		add(b = new JButton(">"), BorderLayout.EAST);
		ActionListener al;
		b.addActionListener(al = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// get ipod all files, do search by criteria, build a list, display
				IpodPane ip = (IpodPane) controller.component(Controller.COMP_IPODPANEL);
				ITunesDB itdb = (ITunesDB) (ip).getModel();
				if (itdb != null)
					((PlayListPane) controller.component(Controller.COMP_PLAYLISTPANEL)).updateList(itdb.search(null,
							t_search.getText(), c_type.getSelectedIndex()), ip);
			}
		});
		t_search.addActionListener(al);
	}

	private JTextField t_search;

	private JComboBox c_type;

	private Controller controller;
}
