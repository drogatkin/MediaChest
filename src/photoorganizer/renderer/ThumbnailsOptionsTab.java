/* PhotoOrganizer - $RCSfile: ThumbnailsOptionsTab.java,v $
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
 * $Id: ThumbnailsOptionsTab.java,v 1.10 2008/01/05 05:09:08 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import mediautil.image.jpeg.AbstractImageInfo;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.HelpProvider;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;

public class ThumbnailsOptionsTab extends JPanel implements DocumentListener, Persistancable, HelpProvider {
	public final static String SECNAME = "ThumbnailsOptions";

	final static String FIXAXIS = "FixedNumber";

	final static String VERTAXIS = "Vertical";

	final static String CELLWIDTH = "CellWidth";

	final static String CELLHEIGHT = "CellHeight";

	public final static String TOOLTIPMASK = "ToolTipMask";

	public final static String LABELMASK = "LabelMask";

	public final static String FILEMASK = "FileMask";

	public final static String SHOWCOMMENT = "ShowComment";

	public final static String BORDER = "Border";

	public final static String SELECTEDBORDER = "SelectedBorder";

	public ThumbnailsOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(7, 6, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
				Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(lb_col_row = new JLabel(Resources.LABEL_NUM_), "0,0,2,1");
		add(tf_fixedaxis = new JTextField(), "2,0,1,1");
		tf_fixedaxis.setHorizontalAlignment(JTextField.RIGHT);
		add(cb_scrollv = new JCheckBox(Resources.LABEL_SCROLL_VERT), "3,0,2,1,12");
		add(new JLabel(Resources.LABEL_THUMBNAILFILE_MASK), "0,2,2,1");
		add(tf_thumbfilemask = new JTextField(), "2,2,3,1");
		tf_thumbfilemask.getDocument().addDocumentListener(this);
		add(new JLabel(Resources.LABEL_THUMBNAILSIZE), "0,1,1");
		add(new JLabel(Resources.LABEL_WIDTH_SHORT, SwingConstants.RIGHT), "1,1,1,1");
		add(tf_cellwidth = new JTextField(SwingConstants.RIGHT), "2,1,1,1");
		tf_cellwidth.setHorizontalAlignment(JTextField.RIGHT);
		add(new JLabel(Resources.LABEL_HEIGHT_SHORT, SwingConstants.RIGHT), "3,1,1,1");
		add(tf_cellheight = new JTextField(SwingConstants.RIGHT), "4,1,1,1");
		tf_cellheight.setHorizontalAlignment(JTextField.RIGHT);
		add(cb_comment = new JCheckBox(Resources.LABEL_USE_COMMENT), "5,1,2");
		add(new JLabel(Resources.LABEL_TTIP_MASK), "0,3,2,1");
		add(tf_tooltipmask = new JTextField(), "2,3,3,1");
		tf_tooltipmask.getDocument().addDocumentListener(this);
		add(new JLabel(Resources.LABEL_LABEL_MASK), "0,4,2");
		add(tf_labelmask = new JTextField(), "2,4,3");
		tf_labelmask.getDocument().addDocumentListener(this);
		add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "5,2,0", tf_thumbfilemask), "5,2,0");
		add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "5,3,0", tf_tooltipmask), "5,3,0");
		add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "5,4,0", tf_labelmask), "5,4,0");
		add(new JLabel(Resources.LABEL_BORDER), "4,5");
		add(new JLabel(Resources.LABEL_SELECTEDBORDER), "0,5,2");
		add(cb_border = new JComboBox(Resources.BORDERS), "5,5,2");
		add(cb_sel_border = new JComboBox(Resources.BORDERS), "2,5,2");
		cb_scrollv.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				lb_col_row.setText(Resources.LABEL_NUM_
						+ (((AbstractButton) e.getSource()).isSelected() ? Resources.LABEL_COL : Resources.LABEL_ROW));
			}
		});
	}

	public String getHelp() {
		return FileNameFormat.formatHelp;
	}

	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void changedUpdate(DocumentEvent e) {
		Document d = e.getDocument();
		if (d == tf_tooltipmask.getDocument())
			tf_tooltipmask.setToolTipText(new FileNameFormat(tf_tooltipmask.getText()).format(controller.sampleJpeg));
		else if (d == tf_labelmask.getDocument())
			tf_labelmask.setToolTipText(new FileNameFormat(tf_labelmask.getText()).format(controller.sampleJpeg));
		else if (d == tf_thumbfilemask.getDocument())
			tf_thumbfilemask.setToolTipText(FileNameFormat.makeValidPathName(new FileNameFormat(tf_thumbfilemask
					.getText()).format(controller.sampleJpeg)));
	}

	public static Dimension getThumbnailSize(IniPrefs s) {
		Dimension result = new Dimension();
		Integer i = (Integer) s.getProperty(SECNAME, CELLWIDTH);
		if (i != null)
			result.width = i.intValue();
		if (result.width > 0) {
			i = (Integer) s.getProperty(SECNAME, CELLHEIGHT);
			if (i != null)
				result.height = i.intValue();
			if (result.height > 0)
				return result;
		}
		return (Dimension) AbstractImageInfo.DEFAULT_THUMB_SIZE.clone();
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		Integer i = (Integer) s.getProperty(SECNAME, FIXAXIS);
		if (i != null)
			tf_fixedaxis.setText(i.toString());
		i = (Integer) s.getProperty(SECNAME, VERTAXIS);
		if (i != null)
			cb_scrollv.setSelected(i.intValue() == 1);
		lb_col_row
				.setText(Resources.LABEL_NUM_ + (cb_scrollv.isSelected() ? Resources.LABEL_COL : Resources.LABEL_ROW));

		i = (Integer) s.getProperty(SECNAME, CELLWIDTH);
		if (i != null)
			tf_cellwidth.setText(i.toString());
		i = (Integer) s.getProperty(SECNAME, CELLHEIGHT);
		if (i != null)
			tf_cellheight.setText(i.toString());
		tf_tooltipmask.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, TOOLTIPMASK)), ','));
		tf_labelmask.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, LABELMASK)), ','));
		tf_thumbfilemask.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, FILEMASK)), ','));
		cb_comment.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, SHOWCOMMENT), 0) == 1);
		cb_border.setSelectedItem(s.getProperty(SECNAME, BORDER));
		cb_sel_border.setSelectedItem(s.getProperty(SECNAME, SELECTEDBORDER));
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		try {
			s.setProperty(SECNAME, FIXAXIS, new Integer(tf_fixedaxis.getText()));
		} catch (NumberFormatException e) {
		}
		s.setProperty(SECNAME, VERTAXIS, cb_scrollv.isSelected() ? Resources.I_YES : Resources.I_NO);
		try {
			s.setProperty(SECNAME, CELLWIDTH, new Integer(tf_cellwidth.getText()));
		} catch (NumberFormatException e) {
		}
		try {
			s.setProperty(SECNAME, CELLHEIGHT, new Integer(tf_cellheight.getText()));
		} catch (NumberFormatException e) {
		}
		s.setProperty(SECNAME, TOOLTIPMASK, tf_tooltipmask.getText());
		s.setProperty(SECNAME, LABELMASK, tf_labelmask.getText());
		s.setProperty(SECNAME, FILEMASK, tf_thumbfilemask.getText());
		s.setProperty(SECNAME, SHOWCOMMENT, cb_comment.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, BORDER, cb_border.getSelectedItem());
		s.setProperty(SECNAME, SELECTEDBORDER, cb_sel_border.getSelectedItem());
	}

	static public Border createBorder(String name) {
		if (Resources.BORDER_BEVELLOWERED.equals(name))
			return new BevelBorder(BevelBorder.LOWERED);
		else if (Resources.BORDER_BEVELRAISED.equals(name))
			return new BevelBorder(BevelBorder.RAISED);
		else if (Resources.BORDER_ETCHED.equals(name))
			return new EtchedBorder();
		else if (Resources.BORDER_LINE.equals(name))
			return new LineBorder(java.awt.Color.blue);
		return null;
	}

	private JTextField tf_cellwidth, tf_cellheight, tf_fixedaxis, tf_tooltipmask, tf_labelmask, tf_thumbfilemask;

	private JCheckBox cb_scrollv, cb_comment;

	private JComboBox cb_border, cb_sel_border;

	private JLabel lb_col_row;

	private Controller controller;
}