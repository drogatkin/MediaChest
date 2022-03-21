/* MediaChest - AlbumThumbnailsPanel.java
 * Copyright (C) 1999-2001 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: AlbumThumbnailsPanel.java,v 1.12 2007/12/04 05:06:32 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import mediautil.gen.MediaFormat;
import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.formats.Thumbnail;

public class AlbumThumbnailsPanel extends ThumbnailsPanel {
	public AlbumThumbnailsPanel(Controller controller) {
		super(controller);
		albumpanel.setTumbnailsPanel(this);
	}

	void setImageView() {
		imagepanel = (PhotoImagePanel) controller.component(Controller.COMP_IMAGEALBUMPANEL);
	}

	JPopupMenu getRightButtonMenu(ActionListener listener, boolean use_alternative) {
		return new AlbumThumbsRightBtnMenu(listener);
	}

	void doSpecificAction(MediaFormat format, ActionEvent a, Thumbnail source) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.MENU_SHOW)) {
			showFullImage(format, source);
		} else if (cmd.equals(Resources.MENU_DELETE) || cmd.equals(Resources.MENU_MOVETOALBUM)) {
			if (cmd.equals(Resources.MENU_MOVETOALBUM))
				if (addToAlbum(format) == false)
					return;
			albumpanel.deletePicture(format.getFile().getPath());
			super.doSpecificAction(format, a, source); // delete it from disk
		} else if (cmd.equals(Resources.MENU_COMMENT)) {
			String value = albumpanel.getCommentOf(format.getFile().getPath());
			value = (String) JOptionPane.showInputDialog(this, Resources.LABEL_COMMENT, Resources.TITLE_COMMENT,
					JOptionPane.QUESTION_MESSAGE, null, null, value);
			if (value != null) {
				albumpanel.setCommentTo(format.getFile().getPath(), (String) value);
				source.update();
			}
		} else if (cmd.equals(Resources.MENU_ADDTOCOLLECT)) {
			collectionpanel.add(format);
		} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
			addToAlbum(format);
		} else if (cmd.equals(Resources.MENU_PRINT)) {
			controller.print(new Object[] { format });
		}
	}

	public String getImageTitle(MediaFormat format, boolean thumbnail) {
		String result = albumpanel.getCommentOf(format.getFile().getPath());
		if (result == null || result.length() == 0)
			result = super.getImageTitle(format, thumbnail);
		return result;
	}

	protected boolean addToAlbum(MediaFormat format) {
		// no way to copy comment
		AlbumSelectionDialog asd = albumpanel.getSelectionDialog();
		asd.setTitle(Resources.TITLE_SELECT_ALBUM + format.getFile());
		asd.setVisible(true);
		TreePath[] tps = asd.getSelectedAlbums();
		if (tps != null) {
			albumpanel.addToAlbum(new MediaFormat[] { format }, tps, false);
			return true;
		}
		return false;
	}

	class AlbumThumbsRightBtnMenu extends JPopupMenu {
		AlbumThumbsRightBtnMenu(ActionListener listener) {
			JMenuItem item;
			add(item = new JMenuItem(Resources.MENU_SHOW));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_ADDTOCOLLECT));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_ADDTO_IPOD));
			item.addActionListener(listener);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_ADDTOALBUM));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_MOVETOALBUM));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_DELETE));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_COPY_LOCATION));
			item.addActionListener(listener);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_COMMENT));
			item.addActionListener(listener);
			// item.setEnabled(false);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_PROPERTIES));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_EDIT_PROPS));
			item.addActionListener(listener);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_PRINT));
			item.addActionListener(listener);
		}
	}
}