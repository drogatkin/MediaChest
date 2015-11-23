/* PhotoOrganizer - AlbumProperties 
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

import static photoorganizer.Resources.TTIP_GENERALPROPS;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.BevelBorder;

import photoorganizer.*;
import photoorganizer.album.*;

public class AlbumProperties extends JDialog {
	Access access;

	int id;

	AlbumProperties(Component parent, int albumid, Access access) {
		super((Frame) SwingUtilities.windowForComponent(parent), Resources.TITLE_ALBUMPROPS, true);
		this.access = access;
		this.id = albumid;
		JTabbedPane tabbedpane = new JTabbedPane(SwingConstants.TOP);
		tabbedpane.insertTab(Resources.TAB_GENERAL, (Icon) null, new GeneralPropsTab(), TTIP_GENERALPROPS, 0);
		tabbedpane.insertTab(Resources.TAB_DETAILS, (Icon) null, new DetailsPropsTab(), Resources.TTIP_DETAILSPROPS, 1);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabbedpane, "Center");
		pack();
		setVisible(true);
	}

	class GeneralPropsTab extends JPanel {
		GeneralPropsTab() {
			setLayout(new BorderLayout());
			add(new JLabel("Album: " + access.getNameOfAlbum(id), SwingConstants.CENTER), "North");
			JScrollPane acl;
			add(acl = new JScrollPane(new JList(access.getAlbumContents(id))), "Center");
			acl.setBorder(new BevelBorder(BevelBorder.LOWERED));
			setBorder(new EmptyBorder(6, 8, 8, 26));
		}
	}

	class DetailsPropsTab extends JPanel {
		DetailsPropsTab() {
			setLayout(new BorderLayout());
			add(new JLabel("No details for album " + access.getNameOfAlbum(id), SwingConstants.CENTER), "Center");
		}
	}

}