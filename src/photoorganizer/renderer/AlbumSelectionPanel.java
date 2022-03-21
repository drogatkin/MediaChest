/* PhotoOrganizer - AlbumSelectionPanel 
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.aldan3.app.ui.FixedGridLayout;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.album.Access;
import photoorganizer.album.AlbumModel;

public class AlbumSelectionPanel extends JPanel implements ActionListener {

	public AlbumSelectionPanel(Controller controller, TreeModel model) {
		setLayout(new FixedGridLayout(5, 12, 24, 4));
		JLabel l;
		add(new JScrollPane(t_all_albums = new JTree(model)), "0,1,5,8,28,12");
		add(tf_album = new JTextField(), "0,9,2,1,32");
		JButton bt;
		add(bt = new JButton(Resources.CMD_CREATEALBUM, UIManager.getIcon("FileChooser.newFolderIcon")), "2,9,2");
		bt.addActionListener(this);
		add(cb_include = new JCheckBox(Resources.LABEL_INCLUSIVE), "0,10,3,1,32");
		cb_include.setToolTipText(Resources.TTIP_CREATEALBUMINCL);
		add(l = new JLabel(), "0,0,5,12,6,18");
		l.setBorder(new TitledBorder(new BevelBorder(BevelBorder.RAISED), Resources.LABEL_NAME_OF_ALBUM));
		l.setOpaque(false);
	}

	public TreePath[] getSelectedAlbums() {
		return t_all_albums.getSelectionPaths();
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.CMD_CREATEALBUM)) {
			Access access = ((AlbumModel) t_all_albums.getModel()).access;
			String _an = tf_album.getText().trim();
			if (_an.length() == 0)
				return;
			TreePath[] paths = getSelectedAlbums();
			int[] insert_into = new int[] { 0 };
			if (cb_include.isSelected()) {
				if (paths != null && paths.length > 0) {
					insert_into = new int[paths.length];
					for (int i = 0; i < paths.length; i++)
						insert_into[i] = access.getAlbumId(paths[i]);
				}
			}
			int _na = access.createAlbum(insert_into[insert_into.length-1], _an);
			access.insertAlbumToAlbum(insert_into, _na);
			if (insert_into[0] == 0)
				refresh(new Object[] { t_all_albums.getModel().getRoot() });
			else
				for (int i = 0; paths != null && i < paths.length; i++)
					refresh(paths[i].getPath());
			tf_album.setText("");
		}
	}

	void refresh(Object[] path) {
		((AlbumModel) t_all_albums.getModel()).fireTreeStructureChanged(this, path, null, null);
	}

	JCheckBox cb_include;

	JTree t_all_albums;

	JTextField tf_album;
}
