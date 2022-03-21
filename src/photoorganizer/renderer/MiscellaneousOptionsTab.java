/* PhotoOrganizer $RCSfile: MiscellaneousOptionsTab.java,v $
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
 * $Id: MiscellaneousOptionsTab.java,v 1.16 2008/01/05 05:09:08 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.HelpProvider;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.formats.FileNameFormat;

public class MiscellaneousOptionsTab extends JPanel implements ActionListener, Persistancable, HelpProvider {
	public final static String SECNAME = "MiscellaneousOptions";

	public final static String DATEFORMAT = "DateFormat";

	public final static String TIMEFORMAT = "TimeFormat";

	public final static String TOOLBAR = "ToolBar";

	public final static String STATUSBAR = "StatusBar";

	public final static String MENUBAR = "MenuBar";

	public final static String TABPOS = "TabPosition";

	public final static String FC_FOLDER = "FlashCardDirectory";

	public final static String SHOWWARNDLG = "ShowWarnDlg";

	public final static String LOCALE = "Locale";

	public final static String SPLITVERT = "SplitVertically";

	public final static String ENCODING = "Encoding";

	private static final Object TIMEZONE = "CameraTimeZone";

	public final static String FONT = "Font";

	public final static String SLIDESHOWPAUSE = "SlideShowPause";

	// LIST_ONTOP, LIST_ONBOTTOM, LIST_ONLEFT, LIST_ONRIGHT
	final static Integer[] TABPOSTABLE = { new Integer(SwingConstants.TOP), new Integer(SwingConstants.BOTTOM),
			new Integer(SwingConstants.LEFT), new Integer(SwingConstants.RIGHT) };

	final static String formathelp = "<html>\n" + "<pre>\n"
			+ " Symbol   Meaning                 Presentation        Example\n"
			+ " ------   -------                 ------------        -------\n"
			+ " G        era designator          (Text)              AD\n"
			+ " y        year                    (Number)            1996\n"
			+ " M        month in year           (Text &amp; Number)     July &amp; 07\n"
			+ " d        day in month            (Number)            10\n"
			+ " h        hour in am/pm (1~12)    (Number)            12\n"
			+ " H        hour in day (0~23)      (Number)            0\n"
			+ " m        minute in hour          (Number)            30\n"
			+ " s        second in minute        (Number)            55\n"
			+ " S        millisecond             (Number)            978\n"
			+ " E        day in week             (Text)              Tuesday\n"
			+ " D        day in year             (Number)            189\n"
			+ " F        day of week in month    (Number)            2 (2nd Wed in July)\n"
			+ " w        week in year            (Number)            27\n"
			+ " W        week in month           (Number)            2\n"
			+ " a        am/pm marker            (Text)              PM\n"
			+ " k        hour in day (1~24)      (Number)            24\n"
			+ " K        hour in am/pm (0~11)    (Number)            0\n"
			+ " z        time zone               (Text)              Pacific Standard Time  \n"
			+ " '        escape for text         (Delimiter)\n"
			+ " ''       single quote            (Literal)           '\n" + " </pre>";

	public MiscellaneousOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(8, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		JPanel canvas = new JPanel();
		canvas.setBorder(new TitledBorder(new EtchedBorder(), Resources.TITLE_SHOW));
		canvas.setOpaque(false);
		add(canvas, "0,0,8,4,16,8");
		add(cb_menu = new JCheckBox(Resources.LABEL_MENU), "1,1");
		add(cb_tool = new JCheckBox(Resources.LABEL_TOOLBAR), "2,1");
		add(cb_warn = new JCheckBox(Resources.LABEL_SHOWWARNDLG), "2,2,2");
		add(cb_status = new JCheckBox(Resources.LABEL_SATUSBAR), "1,2");
		add(new JLabel(Resources.LABEL_TABPOS, SwingConstants.LEFT), "6,1,2,1,34");
		add(mb_positions = new JComboBox(Resources.POSITIONS), "6,2,2,1,34");
		ButtonGroup bg = new ButtonGroup();
		add(rb_split_vert = new JRadioButton(Resources.LABEL_SPLIT_VERT), "4,1,2");
		bg.add(rb_split_vert);
		add(rb_split_horz = new JRadioButton(Resources.LABEL_SPLIT_HORZ), "4,2,2");
		bg.add(rb_split_horz);

		add(new JLabel(Resources.LABEL_LOCALE, SwingConstants.RIGHT), "0,4");
		// TODO we do not need really to keep it as a String array, Locale array is good as well
		String[] localesArray = Controller.objectArrayToStringArray(Locale.getAvailableLocales());
		Arrays.sort(localesArray);
		add(cb_locale = new JComboBox(localesArray), "1,4,0"); // getISOLanguages()

		add(new JLabel(Resources.LABEL_FONT, SwingConstants.RIGHT), "2,4,1");
		Vector fonts = new Vector();
		fonts.add(Resources.LIST_DEFAULT);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fontNames = ge.getAvailableFontFamilyNames();
		for (int i = 0; i < fontNames.length; i++) {
			for (int k = 8; k < 22; k += 2) {
				fonts.add(fontNames[i] + "-PLAIN-" + k);
				fonts.add(fontNames[i] + "-BOLD-" + k);
			}
		}
		Arrays.sort(fontNames);
		add(cb_font = new JComboBox(fonts), "3,4,2"); // getISOLanguages()

		add(new JLabel(Resources.LABEL_PAGE_ENCODING, SwingConstants.RIGHT), "4,4,2");
		add(cb_encoding = new JComboBox(Charset.availableCharsets().keySet().toArray()), "6,4,2");
		cb_encoding.setEditable(true);

		add(new JLabel(Resources.LABEL_DATEFMT, SwingConstants.RIGHT), "0,5");
		add(tf_date = new JTextField(), "1,5");
		add(new JLabel(Resources.LABEL_TIMEFMT, SwingConstants.RIGHT), "2,5");
		add(tf_time = new JTextField(), "3,5");
		
		add(new JLabel(Resources.LABEL_TIMEZONE, SwingConstants.RIGHT), "4,5,2");
		String [] tzArray = TimeZone.getAvailableIDs();
		Arrays.sort(tzArray);
		add(cb_timezones = new JComboBox(tzArray), "6,5,2");
		
		JButton button;
		add(new JLabel(Resources.LABEL_CF_DIR, SwingConstants.RIGHT), "0,6,0");
		add(tf_fc_folder = new JTextField(), "2,6,3");
		add(button = new JButton(Resources.CMD_BROWSE), "5,6,0");
		button.addActionListener(this);
		JPanel ssp = new JPanel();
		ssp.setLayout(new FlowLayout());
		ssp.add(new JLabel(Resources.LABEL_SSINT, SwingConstants.RIGHT));
		ssp.add(sp_ssin = new JSpinner());
		ssp.add(new JLabel(Resources.LABEL_SEC));
		add(ssp, "6,6,0");
	}

	public String getHelp() {
		return formathelp;
	}

	public void actionPerformed(ActionEvent a) {
		if (a.getActionCommand() == Resources.CMD_BROWSE) {
			JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), tf_fc_folder.getText(), null);
			if (dc.getDirectory() != null)
				tf_fc_folder.setText(dc.getDirectory());
		}
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		tf_date.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, DATEFORMAT)), ','));
		tf_time.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, TIMEFORMAT)), ','));
		tf_fc_folder.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, FC_FOLDER)), ','));
		cb_menu.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, MENUBAR), 1) == 1);
		cb_status.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, STATUSBAR), 1) == 1);
		cb_tool.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, TOOLBAR), 0) == 1);
		cb_warn.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, SHOWWARNDLG), 0) == 1);
		
		if (IniPrefs.getInt(s.getProperty(SECNAME, SPLITVERT), 1) == 1)
			rb_split_vert.setSelected(true);
		else
			rb_split_horz.setSelected(true);
		switch (IniPrefs.getInt(s.getProperty(SECNAME, TABPOS), SwingConstants.BOTTOM)) {
		case SwingConstants.TOP:
			mb_positions.setSelectedIndex(0);
			break;
		case SwingConstants.BOTTOM:
			mb_positions.setSelectedIndex(1);
			break;
		case SwingConstants.LEFT:
			mb_positions.setSelectedIndex(2);
			break;
		case SwingConstants.RIGHT:
			mb_positions.setSelectedIndex(3);
			break;
		}
		String a = (String) s.getProperty(SECNAME, LOCALE);
		if (a == null)
			a = Locale.getDefault().getLanguage();
		cb_locale.setSelectedItem(a);
		a = (String) s.getProperty(SECNAME, ENCODING);
		if (a != null)
			cb_encoding.setSelectedItem(a);
		else
			cb_encoding.setSelectedItem("UTF-8");
		a = (String)s.getProperty(SECNAME, TIMEZONE);
		if (a == null)
			a = TimeZone.getDefault().getID();
		if (a != null)
			cb_timezones.setSelectedItem(a);
		a = (String) s.getProperty(SECNAME, FONT);
		if (a != null)
			cb_font.setSelectedItem(a);
		else
			cb_font.setSelectedIndex(0);
		sp_ssin.setValue(IniPrefs.getInt(s.getProperty(SECNAME, SLIDESHOWPAUSE), 3));
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		FileNameFormat.datemask = tf_date.getText().trim();
		s.setProperty(SECNAME, DATEFORMAT, FileNameFormat.datemask);
		FileNameFormat.timemask = tf_time.getText().trim();
		s.setProperty(SECNAME, TIMEFORMAT, FileNameFormat.timemask);
		s.setProperty(SECNAME, FC_FOLDER, tf_fc_folder.getText());
		s.setProperty(SECNAME, ENCODING, cb_encoding.getSelectedItem());
		s.setProperty(SECNAME, MENUBAR, cb_menu.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, STATUSBAR, cb_status.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, TOOLBAR, cb_tool.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, SHOWWARNDLG, cb_warn.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, SPLITVERT, rb_split_vert.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, TABPOS, TABPOSTABLE[mb_positions.getSelectedIndex()]);
		s.setProperty(SECNAME, LOCALE, cb_locale.getSelectedItem());
		s.setProperty(SECNAME, TIMEZONE, cb_timezones.getSelectedItem());
		controller.setTimeZone(getTimeZone(controller));
		setDefaultLocale(controller);
		s.setProperty(SECNAME, FONT, cb_font.getSelectedItem());
		// alert fonts
		s.setProperty(SECNAME, SLIDESHOWPAUSE, sp_ssin.getValue());
		controller.setEncoding(getEncoding(controller));
	}

	public static String getEncoding(Controller controller) {
		String result = (String) controller.getPrefs().getProperty(SECNAME, ENCODING);
		return result == null || result.trim().length() == 0 ? null : result;
	}

	public static TimeZone getTimeZone(Controller controller) {
		String result = (String) controller.getPrefs().getProperty(SECNAME, TIMEZONE);
		try {
			return TimeZone.getTimeZone(result);
		}catch(Exception e) {
			
		}
		return TimeZone.getDefault();
	}

	public static void applyFontSettings(java.awt.Component component, Controller controller) {
		Object fo = controller.getPrefs().getProperty(SECNAME, FONT);
		if (fo == null)
			return; // no memory for default, need to restart
		if (fo instanceof String && Resources.LIST_DEFAULT.equals(fo) == false) {
			String font = (String) fo;
			component.setFont(Font.decode(font));
		}
	}

	public static void setDefaultLocale(Controller controller) {
		String locale = (String) controller.getPrefs().getProperty(SECNAME, LOCALE);
		if (locale != null && locale.length() > 0) {
			StringTokenizer st = new StringTokenizer(locale, "_");
			String l = st.hasMoreTokens() ? st.nextToken() : null;
			String c = st.hasMoreTokens() ? st.nextToken() : null;
			if (l != null)
				if (c != null)
					Locale.setDefault(new Locale(l, c));
				else
					Locale.setDefault(new Locale(l));
		}
	}

	Controller controller;

	JTextField tf_date, tf_time, tf_fc_folder;

	JCheckBox cb_menu, cb_status, cb_tool, cb_warn;

	JComboBox mb_positions, cb_locale, cb_encoding, cb_font, cb_timezones;

	JRadioButton rb_split_horz, rb_split_vert;

	JSpinner sp_ssin;
}