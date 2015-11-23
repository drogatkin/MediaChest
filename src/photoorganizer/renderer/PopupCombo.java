/* PopupCombo 
 * Copyright (C) 1999 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: PopupCombo.java,v 1.6 2007/07/27 02:58:08 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class PopupCombo extends JComboBox implements ActionListener {

	public PopupCombo(Object[] list, String label, JPanel parent, String bounds, JComponent populTarget) {
		super(list);
		this.populTarget = populTarget;
		setUI(new MyComboboxUi());
		setLightWeightPopupEnabled(false);
		button = new JButton(label);
		button.addActionListener(this);
		parent.add(button, bounds);
		setSelectedIndex(-1);
		setVisible(false);
	}

	public void actionPerformed(ActionEvent a) {
		if ("comboBoxChanged".equals(a.getActionCommand()))
			return;
		((JComponent) a.getSource()).setVisible(false);
		setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				grabFocus();
				showPopup(); // or setPopupVisible(true);
			}
		});
	}

	class MyComboboxUi extends javax.swing.plaf.metal.MetalComboBoxUI {
		protected javax.swing.plaf.basic.ComboPopup createPopup() {
			javax.swing.plaf.basic.BasicComboPopup popup = new MyPopup(comboBox);
			popup.getAccessibleContext().setAccessibleParent(comboBox);
			return popup;
		}
	}

	class MyPopup extends javax.swing.plaf.basic.BasicComboPopup {
		private boolean notfirsttime;

		public MyPopup(JComboBox combo) {
			super(combo);
		}

		public void show() {
			try {
				super.show();
			} catch (java.awt.IllegalComponentStateException e) {
			}
		}

		public void hide() {
			super.hide();
			// final selection
			if (!notfirsttime)
				return;
			notfirsttime = !notfirsttime;
			int sel = getSelectedIndex();
			if (sel >= 0) {
				JTextField textField = null;
				if (populTarget instanceof JTextField)
					textField = (JTextField) populTarget;
				else if (populTarget instanceof JComboBox
						&& ((JComboBox) populTarget).getEditor().getEditorComponent() instanceof JTextField)
					textField = (JTextField) ((JComboBox) populTarget).getEditor().getEditorComponent();
				if (textField != null) {
					textField.replaceSelection(photoorganizer.Resources.MASKS[0][sel]);
					textField.grabFocus();
				}
			}
			setSelectedIndex(-1);
		}

		public void removeNotify() {
			super.removeNotify();
			notfirsttime = true;
			PopupCombo.this.setVisible(false);
			button.setVisible(true);
		}
	}

	JButton button;

	JComponent populTarget;
}