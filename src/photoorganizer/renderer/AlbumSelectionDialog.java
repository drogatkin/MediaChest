/* PhotoOrganizer - AlbumSelectionDialog.java
 * Copyright (C) 1999 Dmitry Rogatkin.  All rights reserved.
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package photoorganizer.renderer;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.tree.TreePath;

import photoorganizer.Controller;
import photoorganizer.Resources;

public class AlbumSelectionDialog extends JDialog implements ActionListener {
    AlbumSelectionDialog(Controller controller) {
	super(controller.mediachest, true);
	getContentPane().add(albumselpane = new AlbumSelectionPanel(controller,
	    ((AlbumPane)controller.component(Controller.COMP_ALBUMPANEL)).getModel()), "Center");
	JPanel buttons = new JPanel();
	JButton bt;
	buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
	buttons.add(bt = new JButton(Resources.CMD_ADDTOALBUM));
	bt.addActionListener(this);
	buttons.add(bt = new JButton(Resources.CMD_CANCEL));
	bt.addActionListener(this);
	getContentPane().add(buttons, "South");
	pack();
    }

    public void actionPerformed(ActionEvent a) {
        String cmd = a.getActionCommand();
        if (cmd.equals(Resources.CMD_ADDTOALBUM)) {
            selected = albumselpane.getSelectedAlbums(); 
        } else
            selected = null;
        setVisible(false);
    }

    public TreePath[] getSelectedAlbums() {
	return selected;
    }
    
    private AlbumSelectionPanel albumselpane;
    private TreePath[] selected;
}