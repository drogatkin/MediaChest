/* PhotoOrganizer - PluginOptionsTab
 * Copyright (C) 1999-2000 Dmitry Rogatkin.  All rights reserved.
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
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Persistancable;
import photoorganizer.Resources;

public class PluginOptionsTab extends JPanel implements ActionListener, Persistancable {
	public static final String SECNAME = "plugins";

	public static final String CLASS = "class";

	public static final String NAME = "name";

	public static final String TOOLTIP = "tooltip";

	public static final String DISABLED = "disabled";

	public static final String ALLDISABLED = "allDisabled";

	static final String DEF_NAME = "plug-in";

	Controller controller;

	public PluginOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(14, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP, 1));
		add(new JScrollPane(l_plugins = new JList(plugins = new PluginList())), "0,1,5,5");
		// l_plugins.setBorder(new EtchedBorder());
		add(new JLabel(Resources.LABEL_NAME), "7,0,5,1");
		add(tf_name = new JTextField(), "7,1, 4");
		add(cb_disabled = new JCheckBox(Resources.LABEL_DISABLED), "11,1, 3");
		JButton b;
		add(b = new JButton(Resources.LABEL_INS_R), "5,2, 2,1,12");
		b.addActionListener(this);
		add(new JLabel(Resources.LABEL_CLASS), "7,2,5,1");
		add(tf_class = new JTextField(), "7,3, 6");
		add(b = new JButton(Resources.LABEL_INS_L), "5,4, 2,1,12");
		b.addActionListener(this);
		add(new JLabel(Resources.LABEL_TOOLTIP), "7,4,5,1");
		add(tf_ttip = new JTextField(), "7,5, 7");
		add(cb_allDisabled = new JCheckBox(Resources.LABEL_ALL_DISABLED), "2,6, 4");
		l_plugins.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int i = l_plugins.getSelectedIndex();
				if (i >= 0) {
					PluginInfo pi = (PluginInfo) plugins.getElementAt(i);
					tf_class.setText(pi.class_);
					tf_name.setText(pi.name);
					if (pi.tooltip != null)
						tf_ttip.setText(pi.tooltip);
					else
						tf_ttip.setText("");
					cb_disabled.setSelected(pi.disabled);
					if (lastSelected >= 0) {
						pi = (PluginInfo) plugins.getElementAt(lastSelected);
						pi.class_ = tf_class.getText();
						pi.name = tf_name.getText();
						pi.tooltip = tf_ttip.getText();
						pi.disabled = cb_disabled.isSelected();
					}
					lastSelected = i;
				}
			}
		});
	}

	public void actionPerformed(ActionEvent a) {
		if (a.getActionCommand() == Resources.LABEL_INS_R) {
			plugins.add(new PluginInfo(tf_class.getText(), tf_name.getText(), tf_ttip.getText()));
		} else if (a.getActionCommand() == Resources.LABEL_INS_L) {
			int i = l_plugins.getSelectedIndex();
			if (i >= 0) {
				// we don't do deactivation the plug-in, just removing it from
				// cfg
				tf_class.setText("");
				tf_name.setText("");
				tf_ttip.setText("");
				cb_disabled.setSelected(false);
				plugins.removeElementAt(i);
			}
		}
		l_plugins.updateUI();
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		for (int i = 1;; i++) {
			String extPanelName = (String) s.getProperty(SECNAME, CLASS + i);
			if (extPanelName == null)
				break;
			PluginInfo pi = new PluginInfo();
			pi.class_ = extPanelName;
			extPanelName = (String) s.getProperty(SECNAME, NAME + i);
			if (extPanelName == null)
				extPanelName = DEF_NAME + i;
			pi.name = extPanelName;
			pi.tooltip = (String) s.getProperty(SECNAME, TOOLTIP + i);
			pi.disabled = IniPrefs.getInt(s.getProperty(SECNAME, DISABLED + i), 0) == 1;
			plugins.add(pi);
		}
		l_plugins.updateUI();
		cb_allDisabled.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, ALLDISABLED), 0) == 1);
	}

	public void save() {
		// lack of JList design, no way to send event sel changed
		PluginInfo pi;
		if (lastSelected >= 0) {
			pi = (PluginInfo) plugins.getElementAt(lastSelected);
			pi.class_ = tf_class.getText();
			pi.name = tf_name.getText();
			pi.tooltip = tf_ttip.getText();
			pi.disabled = cb_disabled.isSelected();
			lastSelected = -1;
		}
		IniPrefs s = controller.getPrefs();
		// remove all we had first
		for (int i = 1;; i++) {
			Object o = s.getProperty(SECNAME, CLASS + i);
			if (o == null)
				break;
			s.setProperty(SECNAME, CLASS + i, null);
			s.setProperty(SECNAME, NAME + i, null);
			s.setProperty(SECNAME, TOOLTIP + i, null);
			s.setProperty(SECNAME, DISABLED + i, null);
		}
		for (int i = 0; i < plugins.getSize(); i++) {
			pi = (PluginInfo) plugins.getElementAt(i);
			s.setProperty(SECNAME, CLASS + (i + 1), pi.class_);
			s.setProperty(SECNAME, NAME + (i + 1), pi.name);
			s.setProperty(SECNAME, TOOLTIP + (i + 1), pi.tooltip);
			s.setProperty(SECNAME, DISABLED + (i + 1), pi.disabled ? Resources.I_YES : Resources.I_NO);
		}
		s.setProperty(SECNAME, ALLDISABLED, cb_allDisabled.isSelected() ? Resources.I_YES : Resources.I_NO);
	}

	JList l_plugins;

	JTextField tf_name, tf_class, tf_ttip;

	JCheckBox cb_disabled, cb_allDisabled;

	PluginList plugins;

	int lastSelected = -1;

	class PluginList extends AbstractListModel {
		Vector storge;

		PluginList() {
			storge = new Vector();
		}

		void add(PluginInfo pi) {
			storge.addElement(pi);
		}

		public void removeElementAt(int index) {
			storge.removeElementAt(index);
		}

		public Object getElementAt(int index) {
			return storge.elementAt(index);
		}

		public int getSize() {
			return storge.size();
		}
	}
}

class PluginInfo {
	String name;

	String class_;

	String tooltip;

	boolean disabled;

	PluginInfo() {
	}

	PluginInfo(String _class, String name, String tooltip) {
		class_ = _class;
		this.name = name;
		this.tooltip = tooltip;
	}

	public String toString() {
		return name + " (" + class_ + ')';
	}
}
