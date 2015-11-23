/* MediaChest OptionsFrame
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  http://mediachest.sourceforge.net
 *  $Id: OptionsFrame.java,v 1.8 2007/07/27 02:58:07 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.aldan3.model.ServiceProvider;

import photoorganizer.Controller;
import photoorganizer.HelpProvider;
import photoorganizer.Persistancable;
import photoorganizer.PhotoPlugin;
import photoorganizer.Resources;

public class OptionsFrame extends JFrame implements ActionListener, Persistancable {
	public OptionsFrame(Controller controller) {
		super(Resources.TITLE_OPTIONS);
		this.controller = controller;
		setIconImage(controller.getMainIcon());
		tabbedpane = new JTabbedPane(SwingConstants.TOP);
		tabbedpane.setTabLayoutPolicy(tabbedpane.SCROLL_TAB_LAYOUT);
		// TODO: introduce new tabs
		// Directories
		// Workspace
		// Formats
		// Database
		// Web-publishing
		// TAB_MISCELLANEOUS
		int ti = 0;
		// TODO: make tab adding in a loop
		tabbedpane.insertTab(Resources.TAB_RENAME, (Icon) null, new RenameOptionsTab(controller),
				Resources.TTIP_RULEFORRENAME, ti++);
		tabbedpane.insertTab(Resources.TAB_TRANSFORM, (Icon) null, new TransformOptionsTab(controller),
				Resources.TTIP_RULEFORTRANSFORM, ti++);
		tabbedpane.insertTab(Resources.TAB_THUMBNAILS, (Icon) null, new ThumbnailsOptionsTab(controller),
				Resources.TTIP_TUMBNAILSOPTION, ti++);
		tabbedpane.insertTab(Resources.TAB_MISCELLANEOUS, (Icon) null, new MiscellaneousOptionsTab(controller),
				Resources.TTIP_MISCELLANEOUS, ti++);
		tabbedpane.insertTab(Resources.TAB_APPEARANCE, (Icon) null, new AppearanceOptionsTab(controller),
				Resources.TTIP_APPEARANCE, ti++);
		tabbedpane.insertTab(Resources.TAB_ALBUM, (Icon) null, new AlbumOptionsTab(controller),
				Resources.TTIP_ALBUMOPTIONS, ti++);
		tabbedpane.insertTab(Resources.TAB_WEBPUBLISH, (Icon) null, new WebPublishOptionsTab(controller),
				Resources.TTIP_WEBPUBLISH, ti++);
		tabbedpane.insertTab(Resources.TAB_MEDIAOPTIONS, (Icon) null, new MediaOptionsTab(controller),
				Resources.TTIP_MEDIAOPTIONS, ti++);
		tabbedpane.insertTab(Resources.TAB_IPODOPTIONS, (Icon) null, new IpodOptionsTab(controller),
				Resources.TTIP_IPODOPTIONS, ti++);
		if (controller.component(Controller.COMP_REMOTERECEIVER) != null)
			tabbedpane.insertTab(Resources.TAB_REMOTE_CTRL, (Icon) null, new RemoteOptionsTab(controller),
					Resources.TTIP_REMOTE_CTRL, ti++);
		tabbedpane.insertTab(Resources.TAB_PLUGIN, (Icon) null, new PluginOptionsTab(controller),
				Resources.TTIP_PLUGINOPTIONS, ti++);

		// add tabs required by plug-ins
		Iterator<ServiceProvider> iterator = controller.iterator();
		int p = tabbedpane.getTabCount();
		while (iterator.hasNext()) {
			try {
				PhotoPlugin plugin = (PhotoPlugin) iterator.next().getServiceProvider();
				JPanel pp = plugin.getOptionsTab();
				if (pp != null)
					tabbedpane.insertTab(plugin.getName(), (Icon) null, pp, plugin.getName(), p++);

			} catch (ClassCastException cce) {
			}
		}
		/*
		 * tabbedpane.insertTab(Resources.TAB_COLLECTION, (Icon)null, new
		 * PhotoCollectionOptionsTab(controller),
		 * Resources.TTIP_TUMBNAILSOPTION, 3);
		 */
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabbedpane, "Center");
		getContentPane().add(controller.createButtonPanel(this, true), "South");
		pack();
		setVisible(true);
		load();
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.CMD_OK)) {
			save();
			setVisible(false);
		} else if (cmd.equals(Resources.CMD_APPLY)) {
			save();
		} else if (cmd.equals(Resources.CMD_CANCEL)) {
			setVisible(false);
		} else if (cmd.equals(Resources.CMD_HELP)) {
			Component c = tabbedpane.getSelectedComponent();
			if (c != null && c instanceof HelpProvider) {
				if (help_dialog == null) {
					help_dialog = new JDialog(this, Resources.TITLE_HELP);
					help_dialog.setContentPane(helpContentPane = TwoPanesView.createFramed(true, null,
							Controller.BTN_MSK_CLOSE, new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									help_dialog.setVisible(false);
								}
							}));
					help_dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
					helpContentPane.setSize(450, 360);
					help_dialog.pack();
					// help_dialog.setSize(460, 370);
				}
				helpContentPane.setUpperText(((HelpProvider) c).getHelp());
				help_dialog.show();
			}
		}
	}

	public void load() {
		JTabbedPane tabbedpane = (JTabbedPane) getContentPane().getComponent(0);
		for (int i = 0; i < tabbedpane.getTabCount(); i++) {
			try {
				((Persistancable) tabbedpane.getComponentAt(i)).load();
			} catch (ClassCastException e) {
			}
		}
	}

	public void save() {
		JTabbedPane tabbedpane = (JTabbedPane) getContentPane().getComponent(0);
		for (int i = 0; i < tabbedpane.getTabCount(); i++) {
			try {
				((Persistancable) tabbedpane.getComponentAt(i)).save();
			} catch (ClassCastException e) {
			}
		}
	}

	protected void finalize() throws Throwable {
		if (help_dialog != null)
			help_dialog.dispose();
		super.finalize();
	}

	Controller controller;

	JDialog help_dialog;

	JTabbedPane tabbedpane;

	TwoPanesView helpContentPane;
}