/* MediaChest - TransformOptionsTab
 * Copyright (C) 1999-2000 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: TransformOptionsTab.java,v 1.10 2007/07/27 02:58:09 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mediautil.image.jpeg.BasicJpeg;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.HelpProvider;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;

public class TransformOptionsTab extends JPanel implements DocumentListener, ItemListener, Persistancable, HelpProvider {

	public final static String SECNAME = "TransformOptions";

	final static String MASK = "Mask";

	final static String MOVE = "Move";

	final static String FORMAT = "Format";

	final static String KEEP = "Keep";

	public final static String POPUL_ARTIST = "PopulateOwnerAsArtist";

	public final static String TRANSFORM = "TransformCode";

	final static String TRANSFTOSEL = "TransformedToSelection";

	public TransformOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(6, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(new SLabel(Resources.LABEL_NAMEGENMASK), "0,0,4,1");
		add(tf_mask = new JTextField(), "0,1,3,1");
		tf_mask.getDocument().addDocumentListener(this);
		add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "3,1,0", tf_mask), "3,1,0");
		add(cb_actor = new JCheckBox(Resources.LABEL_ACTOR_EXIF), "4,1,2");
		add(cb_move = new JCheckBox(Resources.LABEL_KEEP_ORIG), "0,2,2,1");
		add(cb_tosel = new JCheckBox(Resources.LABEL_ADDTRANSFORMEDTOSEL), "2,2,2");
		cb_tosel.setToolTipText(Resources.TTIP_ADDTRANSFORMEDTOSEL);
		JPanel canvas = new JPanel();
		canvas.setBorder(new TitledBorder(new EtchedBorder(), Resources.LABEL_MARKERS));
		canvas.setOpaque(false);
		add(canvas, "0,3,4,4,4,12");
		bg_format = new ButtonGroup();
		add(rb_kp = new JRadioButton(Resources.LABEL_KEEP_APPS), "0,4,2,1,18");
		bg_format.add(rb_kp);
		add(rb_en = new JRadioButton(Resources.LABEL_ENFORCE_FMT), "0,5,2,1,18");
		bg_format.add(rb_en);
		rb_en.setSelected(true);
		add(mb_format = new JComboBox(Resources.FORMATS), "2,5,0,1,24");
		mb_format.setLightWeightPopupEnabled(false);
		canvas = new JPanel();
		canvas.setBorder(new TitledBorder(new EtchedBorder(), Resources.LABEL_TRANS_CODE));
		canvas.setOpaque(false);
		add(canvas, "4,2,2,4,4,4");
		add(mb_trans_op = new JComboBox(Resources.ROTATIONS), "4,3,2,1,24");
		mb_trans_op.setLightWeightPopupEnabled(false);
		mb_trans_op.addItemListener(this);
		add(tf_trans = new JTextField(), "4,4,2,1,24");
		tf_trans.getDocument().addDocumentListener(this);
	}

	public String getHelp() {
		return FileNameFormat.formatHelp;
	}

	public void itemStateChanged(ItemEvent e) {
		int s = e.getStateChange();
		if (s == ItemEvent.SELECTED) {
			tf_trans.setText((String) transcodes[mb_trans_op.getSelectedIndex()]);
		} else { // ItemEvent.DESELECTED
		}
	}

	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void changedUpdate(DocumentEvent e) {
		if (e.getDocument() == tf_mask.getDocument())
			tf_mask.setToolTipText(FileNameFormat.makeValidPathName(new FileNameFormat(tf_mask.getText())
					.format(controller.sampleJpeg)));
		else if (e.getDocument() == tf_trans.getDocument()) {
			transcodes[mb_trans_op.getSelectedIndex()] = tf_trans.getText();
		}
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		String mask = (String) s.getProperty(SECNAME, MASK);
		if (mask != null)
			tf_mask.setText(mask);
		cb_tosel.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, TRANSFTOSEL), 1) == 1);
		cb_move.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, MOVE), 0) == 1);
		cb_actor.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, POPUL_ARTIST), 0) == 1);
		Integer i = (Integer) s.getProperty(SECNAME, KEEP);
		if (i != null) {
			if (i.intValue() == 1)
				rb_kp.setSelected(true);
			else
				rb_en.setSelected(true);
		}
		String format = (String) s.getProperty(SECNAME, FORMAT);
		if (format != null)
			mb_format.setSelectedItem(format);
		transcodes = FileNameFormat.transformCodes;
		mb_trans_op.setSelectedIndex(0);
		tf_trans.setText((String) transcodes[0]);
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		s.setProperty(SECNAME, MASK, tf_mask.getText());
		s.setProperty(SECNAME, MOVE, cb_move.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, POPUL_ARTIST, cb_actor.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, FORMAT, mb_format.getSelectedItem());
		s.setProperty(SECNAME, TRANSFTOSEL, cb_tosel.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, KEEP, (rb_kp.getModel() == bg_format.getSelection()) ? Resources.I_YES : Resources.I_NO);
		FileNameFormat.transformCodes = transcodes;
		s.setProperty(SECNAME, TRANSFORM, FileNameFormat.transformCodes);
	}

	public static void setArtist(Controller controller, BasicJpeg format) {
		if (IniPrefs.getInt(controller.getPrefs().getProperty(SECNAME, POPUL_ARTIST), 0) == 1) {
			Object n = controller.getPrefs().getProperty(Controller.REGISTER, Controller.NAME);
			if (n != null)
				format.setArtist(n.toString());
		}
	}

	Controller controller;

	JTextField tf_mask, tf_trans;

	JComboBox mb_mask, mb_format, mb_trans_op;

	JCheckBox cb_move, cb_tosel, cb_actor;

	JRadioButton rb_en, rb_kp;

	ButtonGroup bg_format;

	Object[] transcodes;
}