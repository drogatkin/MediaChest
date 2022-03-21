/* MediaChest - $RCSfile: RenameOptionsTab.java,v $ 
 * Copyright (C) 1999-2004 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: RenameOptionsTab.java,v 1.15 2008/12/19 05:42:28 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.HelpProvider;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.formats.FileNameFormat;

/**
 * Options tab used for defining media file rename options
 */
public final class RenameOptionsTab extends JPanel implements ActionListener, FocusListener, Persistancable,
		HelpProvider {
	// TODO: consider use gadget factory to get custom gadgets
	// and operate with abstract gadgets
	public final static String SECNAME = "RenameOptions";

	public final static String MASK = "Mask";

	public final static String ASKEDIT = "AskEdit";

	public final static String REMOVEAFTER = "RemoveAfterRename";

	public final static String COUNTER = "Counter";
	
	public final static String AUTOROTATE  = "AutoRotate";

	final static String DESTFOLDER = "DestinationFolder";

	public RenameOptionsTab(final Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(5, 5, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
				Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(new SLabel(Resources.LABEL_NAMEGENMASK), "0,0,4,1");
		add(cb_masks = new JComboBox(maskItems = new DefaultComboBoxModel()), "0,1,3");
		cb_masks.setEditable(true);
		add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "3,1,0", cb_masks), "3,1,0");
		cb_masks.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Object item = cb_masks.getSelectedItem();
				if (item != null && item.toString().trim().length() > 0) {
					if (maskItems.getIndexOf(item) < 0)
						cb_masks.addItem(item);
					cb_masks.setToolTipText(FileNameFormat.makeValidPathName(new FileNameFormat(item.toString())
							.format(controller.sampleJpeg)));
				} else
					cb_masks.removeItem(e.getItem());
			}
		});
		add(cb_editnewname = new JCheckBox(Resources.LABEL_EDIT_NEW_NAME), "0,2,2,1,12");
		add(cb_remafter = new JCheckBox(Resources.LABEL_REMOVE_AFTER_REN), "0,3,2,1,12");
		add(cb_autorotate = new JCheckBox(Resources.LABEL_AUTOROTATE), "2,3,1,1");
		add(new SLabel(Resources.LABEL_START_COUNTER, SwingConstants.LEFT), "3,2,2,1");
		add(tf_counter = new JTextField(), "3,3,1,1");
		tf_counter.setHorizontalAlignment(JTextField.RIGHT);
		add(new SLabel(Resources.LABEL_DESTINATIONDIR), "0,4,4,1");
		add(tf_destdir = new JTextField(), "0,5,4,1");
		add(bt_brws_dstdir = new JButton(Resources.CMD_BROWSE), "4,5,0");
		bt_brws_dstdir.addActionListener(this);
		tf_counter.addFocusListener(this);
	}

	public String getHelp() {
		return FileNameFormat.formatHelp;
	}

	public void actionPerformed(ActionEvent a) {
		if (a.getSource() == bt_brws_dstdir) {
			JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), tf_destdir.getText(), null);
			if (dc.getDirectory() != null)
				tf_destdir.setText(dc.getDirectory());
		}
	}

	public void focusGained(FocusEvent e) {
		tf_counter.setText("" + FileNameFormat.counter);
	}

	public void focusLost(FocusEvent e) {
	}

	public void load() {
		IniPrefs s = controller.getPrefs();

		Object m = s.getProperty(SECNAME, MASK);
		if (m != null) {
			if (m instanceof Object[]) {
				Object[] ma = (Object[]) m;
				for (int i = 0; i < ma.length; i++)
					maskItems.addElement(ma[i]);
			} else
				maskItems.addElement(m);
		}
		cb_editnewname.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, ASKEDIT), 0) == 1);
		cb_remafter.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, REMOVEAFTER), 0) == 1);
		cb_autorotate.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, AUTOROTATE), 0) == 1);
		tf_counter.setText("" + FileNameFormat.counter);
		String f = (String) s.getProperty(SECNAME, DESTFOLDER);
		if (f != null)
			tf_destdir.setText(f);
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		if (maskItems.getSize() == 1)
			s.setProperty(SECNAME, MASK, maskItems.getElementAt(0).toString());
		else if (maskItems.getSize() > 1) {
			String[] allMasks = new String[maskItems.getSize()];
			for (int i = 0; i < maskItems.getSize(); i++)
				allMasks[i] = maskItems.getElementAt(i).toString();
			s.setProperty(SECNAME, MASK, allMasks);
		}
		s.setProperty(SECNAME, ASKEDIT, cb_editnewname.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, REMOVEAFTER, cb_remafter.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, AUTOROTATE, cb_autorotate.isSelected() ? Resources.I_YES : Resources.I_NO);
		
		s.setProperty(SECNAME, DESTFOLDER, tf_destdir.getText());
		Integer i = Resources.I_NO;
		try {
			i = new Integer(tf_counter.getText());
		} catch (NumberFormatException e) {
		}
		s.setProperty(SECNAME, COUNTER, i);
		FileNameFormat.counter = i.intValue();
	}

	public static Object[] getRenameMask(IniPrefs s) {
		Object o = s.getProperty(SECNAME, MASK);
		if (o == null)
			return new String[0];
		else if (o instanceof Object[])
			return (Object[]) o;
		else
			return new String[] { o.toString() };
	}

	public static boolean askForEditMask(IniPrefs s) {
		return IniPrefs.getInt(s.getProperty(SECNAME, ASKEDIT), 0) == 1;
	}

	public static String getDestPath(IniPrefs s) {
		String result = (String) s.getProperty(SECNAME, DESTFOLDER);
		if (result == null || result.length() == 0)
			return s.getHomeDirectory();
		return result;
	}

	Controller controller;

	private JTextField tf_counter, tf_destdir;

	private JButton bt_brws_dstdir;

	private JCheckBox cb_editnewname, cb_remafter, cb_autorotate;

	private JComboBox cb_masks;

	private DefaultComboBoxModel maskItems;
}
