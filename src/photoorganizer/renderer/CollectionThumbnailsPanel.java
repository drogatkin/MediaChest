/* PhotoOrganizer - CollectionThumbnailsPanel
 * Copyright (C) 1999-2003 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: CollectionThumbnailsPanel.java,v 1.11 2008/04/15 23:14:42 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import mediautil.gen.MediaFormat;
import mediautil.image.jpeg.BasicJpeg;

import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.formats.Thumbnail;

public class CollectionThumbnailsPanel extends ThumbnailsPanel {
	public CollectionThumbnailsPanel(Controller controller) {
		super(controller);
	}

	void setImageView() {
		imagepanel = (PhotoImagePanel) controller.component(Controller.COMP_IMAGECOLLCTPANEL);
	}

	void setCollection(PhotoCollectionPanel collectionpanel) {
		this.collectionpanel = collectionpanel;
	}

	JPopupMenu getRightButtonMenu(ActionListener listener, boolean use_alternative) {
		if (use_alternative)
			return collectionpanel.getRMouseMenu();
		else
			return new CollectThumbsRightBtnMenu(listener);
	}

	void doSpecificAction(MediaFormat format, ActionEvent a, Thumbnail source) {
		String cmd = a.getActionCommand();
		int op;
		if (cmd.equals(Resources.MENU_SHOW)) {
			showFullImage(format, source);
		} else if (cmd.equals(Resources.MENU_DELETE)) {
			collectionpanel.delete(new int[] { collectionpanel.findIndexOf(source) });
		} else if (cmd.equals(Resources.MENU_RENAME)) {
			collectionpanel.rename(new int[] { collectionpanel.findIndexOf(source) }, false);
		} else if (cmd.equals(Resources.MENU_COPY)) {
			collectionpanel.rename(new int[] { collectionpanel.findIndexOf(source) }, true);
		} else if (cmd.equals(Resources.MENU_REVERSE_SELECT)) {
			int sel = collectionpanel.findIndexOf(source);
			if (sel >= 0) {
				if (collectionpanel.isRowSelected(sel))
					collectionpanel.removeRowSelectionInterval(sel, sel);
				else
					collectionpanel.addRowSelectionInterval(sel, sel);
				controller.getUiUpdater().notify(collectionpanel.getSelectedRowCount() > 0,
						UiUpdater.SELECTION_SELECTED);
			}
		} else if (cmd.equals(Resources.MENU_PRINT)) {
			controller.print(new Object[] { format });
		} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
			AlbumPane albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
			AlbumSelectionDialog asd = albumpane.getSelectionDialog();
			asd.setTitle(Resources.TITLE_SELECT_ALBUM + ":" + format);
			asd.setVisible(true);
			TreePath[] tps = asd.getSelectedAlbums();
			if (tps != null) {
				// do not mix albums types for a while
				if (albumpane.addToAlbum(new MediaFormat[] { format }, tps, false)) {
					IniPrefs s = controller.getPrefs();
					if (IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.MOVETOFOLDER), 0) == 1
							&& IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.USEALBUMFOLDER),
									0) == 1)
						collectionpanel.delete(new int[] { collectionpanel.findIndexOf(source) });
				}
			}
		} else if ((op = Controller.convertCmdToTrnasformOp(cmd)) != -1) {
			collectionpanel.transform(new int[] { collectionpanel.findIndexOf(source) }, op);
		}
	}

	class CollectThumbsRightBtnMenu extends JPopupMenu {
		CollectThumbsRightBtnMenu(ActionListener listener) {
			JMenuItem item;
			add(item = new JMenuItem(Resources.MENU_REVERSE_SELECT));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_SHOW));
			item.addActionListener(listener);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_ADDTOALBUM));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_ADDTO_IPOD));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_COPY));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_RENAME));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_DELETE));
			item.addActionListener(listener);
			add(item = new JMenuItem(Resources.MENU_COPY_LOCATION));
			item.addActionListener(listener);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_PRINT));
			item.addActionListener(listener);
			addSeparator();
			this.add(Controller.createTransformMenu(listener));
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_PROPERTIES));
			item.addActionListener(listener); // setPopupSize(this.getPreferredSize());
			add(item = new JMenuItem(Resources.MENU_EDIT_PROPS));
			item.addActionListener(listener);
		}
	}
}