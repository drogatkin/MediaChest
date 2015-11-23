/* MediaChest - FastMenu
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
 *  $Id: FastMenu.java,v 1.10 2007/11/22 05:30:29 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;

public class FastMenu extends JPopupMenu {
	public FastMenu(ActionListener listener, Controller controller) {
		JMenuItem item;
		boolean thumbnail = listener instanceof photoorganizer.formats.Thumbnail;
		add(item = new JMenuItem(Resources.MENU_SHOW));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		}
		addSeparator();
		add(item = new JMenuItem(Resources.MENU_ADDTOCOLLECT));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		}
		add(item = new JMenuItem(Resources.MENU_ADDTOALBUM));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		}
		addSeparator();
		add(item = new JMenuItem(Resources.MENU_ADDTO_IPOD));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		} else {
			
		}
		add(item = new JMenuItem(Resources.MENU_COPY_LOCATION));
		item.addActionListener(listener);

		addSeparator();
		add(item = new JMenuItem(Resources.MENU_RENAME));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, false);
		}
		add(item = new JMenuItem(Resources.MENU_DELETE));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, false);
		}
		addSeparator();
		add(item = new JMenuItem(Resources.MENU_PROPERTIES));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		}
		add(item = new JMenuItem(Resources.MENU_EDIT_PROPS));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		}
		addSeparator();
		add(item = new JMenuItem(Resources.MENU_PRINT));
		item.addActionListener(listener);
		if (!thumbnail) {
			setSate(controller, item, true);
		}
	}

	protected void setSate(Controller controller, JMenuItem item, boolean mediasOnly) {
		if (mediasOnly)
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.MEDIA_FILE_SELECTED)
					|| controller.getUiUpdater().isEnabled(UiUpdater.GRAPHICS_FILE_SELECTED)
					|| controller.getUiUpdater().isEnabled(UiUpdater.DIRECTORY_SELECTED));
		else
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.FILE_SELECTED)
					|| controller.getUiUpdater().isEnabled(UiUpdater.DIRECTORY_SELECTED));
	}
}