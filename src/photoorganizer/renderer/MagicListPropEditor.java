/* MediaChest - $RCSfile: MagicListPropEditor.java,v $                         
 * Copyright (C) 2001-2003 Dmitriy Rogatkin.  All rights reserved.                   
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
 *                                                                              
 *  Visit http://mediachest.sourceforge.net to get the latest information         
 *  about Rogatkin's products.                                                  
 *  $Id: MagicListPropEditor.java,v 1.40 2012/10/18 06:58:59 cvs Exp $          
 */

package photoorganizer.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.patrn.Visitor;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import photoorganizer.ipod.BaseHeader;
import photoorganizer.ipod.ITunesDB;
import photoorganizer.ipod.PlayItem;
import photoorganizer.ipod.PlayList;
import photoorganizer.ipod.BaseHeader.Smart;

public class MagicListPropEditor extends JPanel {
	Window window;

	BaseHeader.Smart magic;

	Border blackline, empty;// , raisedetched, loweredetched,

	// raisedbevel, loweredbevel;
	String ln;

	ITunesDB itdb;

	public MagicListPropEditor(Controller controller, Window window, BaseHeader.Smart magic, String ln, ITunesDB itdb) {
		setLayout(new FixedGridLayout(9, 6 + 8, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
				Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		this.window = window;
		this.magic = magic;
		if (this.magic == null)
			this.magic = new BaseHeader.Smart();
		this.ln = ln;
		this.itdb = itdb;
		blackline = BorderFactory.createLineBorder(Color.black);
		empty = BorderFactory.createEmptyBorder();
		// raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		// loweredetched =
		// BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		// raisedbevel = BorderFactory.createRaisedBevelBorder();
		// loweredbevel = BorderFactory.createLoweredBevelBorder();

		int ho = 0;
		add(cx_match = new JCheckBox(Resources.LABEL_MATCH), "0," + ho + ",1,0");
		add(cb_allany = new JComboBox(new String[] { Resources.LIST_ALL, Resources.LIST_ANY }), "1," + ho + ",1,0");
		add(new JLabel(Resources.LABEL_FOLLOW_COND), "2," + ho + ",3,0");
		ho += 1;
		add(new JScrollPane(b_rules = Box.createVerticalBox()), "0," + ho + ",9,7,0,8");
		ho += 8;
		if (magic != null && magic.rules != null && magic.rules.rules.size() > 0) {
			for (int i = 0; i < magic.rules.rules.size(); i++)
				b_rules.add(new RuleEditor((BaseHeader.Rules.Rule) magic.rules.rules.get(i), itdb, ln));
		}
		b_rules.add(Box.createVerticalGlue());
		b_rules.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Component c = b_rules.getComponentAt/* findComponentAt */(e.getPoint());
				while (c != null && c instanceof RuleEditor == false)
					c = c.getParent();
				if (c != null) {
					if (curSel >= 0) {
						Component oc = b_rules.getComponent(curSel);
						if (oc != null)
							((JComponent) oc).setBorder(empty);
					}
					((JComponent) c).setBorder(blackline);
					for (curSel = 0; curSel < b_rules.getComponentCount(); curSel++)
						if (c == b_rules.getComponent(curSel))
							break;
					b_del.setEnabled(curSel >= 0);
				}
			}
		});

		add(cx_limit = new JCheckBox(Resources.LABEL_LIMIT_TO), "0," + ho + ",1");
		add(tf_limit = new JTextField(), "1," + ho + ",1");
		add(cb_limit_u = new JComboBox(BaseHeader.Smart.LIMIT_UNITS), "2," + ho + ",1");
		add(new JLabel(Resources.LABEL_SELECTED_BY), "3," + ho + ",1");
		add(cb_selby = new JComboBox(BaseHeader.Smart.SELECTIONS), "4," + ho + ",2");
		JButton b;

		add(b = new JButton(Resources.CMD_ADD), "6," + ho + ",1,0");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO: collect data
				if (curSel >= 0) {
					b_rules.add(new RuleEditor(new BaseHeader.Rules.Rule(), MagicListPropEditor.this.itdb,
							MagicListPropEditor.this.ln), curSel);
					curSel++;
				} else {
					curSel = b_rules.getComponentCount() - 1;
					b_rules.add(new RuleEditor(new BaseHeader.Rules.Rule(), MagicListPropEditor.this.itdb,
							MagicListPropEditor.this.ln), curSel);
				}
				b_rules.validate();
				// b_rules.getParent().validate();
				// b_rules.getParent().doLayout();
				b_rules.getParent().repaint();
			}
		});
		add(b_del = new JButton(Resources.CMD_DEL), "7," + ho + ",1,0");
		b_del.setEnabled(curSel >= 0);
		b_del.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO: collect data
				if (curSel >= 0 && curSel < b_rules.getComponentCount() - 1/*
				 * &&
				 * MagicListPropEditor.this.magic.rules.rules.size()>1
				 */) {
					b_rules.remove(curSel);
					b_rules.validate();
					curSel = -1;
					b_del.setEnabled(false);
					// b_rules.invalidate();
					b_rules.doLayout();
					b_rules.repaint();
				}
			}
		});

		ho++;
		add(cx_live = new JCheckBox(Resources.LABEL_LIVE_UPD), "0," + ho + ",2");
		add(cx_mos = new JCheckBox(Resources.LABEL_MATCH_CHECK_SONG), "2," + ho + ",3");
		ho += 2;
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());

		buttons.add(b = new JButton(Resources.CMD_OK));
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO: collect data
				MagicListPropEditor.this.magic.rules.rules.clear();
				for (int i = 0; i < b_rules.getComponentCount(); i++) {
					Component c = b_rules.getComponent(i);
					if (c instanceof RuleEditor) {
						((RuleEditor) c).collect();
						BaseHeader.Rules.Rule rule = ((RuleEditor) c).getRule();
						rule.normalize();
						MagicListPropEditor.this.magic.rules.rules.add(rule);
					}
				}
				collect();
				MagicListPropEditor.this.window.dispose();
			}
		});
		buttons.add(b = new JButton(Resources.CMD_CANCEL));
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MagicListPropEditor.this.magic = null;
				MagicListPropEditor.this.window.dispose();
			}
		});

		buttons.add(new JButton(Resources.CMD_HELP));
		add(buttons, "5," + ho + ",4,2");
		update();
	}

	void update() {
		cb_allany.setSelectedIndex(magic.rules.any ? 1 : 0);
		cx_limit.setSelected(magic.checkLimit);
		cx_mos.setSelected(magic.chkdSong);
		tf_limit.setText("" + magic.limit);
		setSelectedId(cb_limit_u, magic.item);
		setSelectedId(cb_selby, magic.sort);
		cx_live.setSelected(magic.live);
		cx_match.setSelected(magic.checkRegExp);
	}

	void collect() {
		magic.rules.any = cb_allany.getSelectedIndex() == 1;
		magic.rules.touch();
		magic.checkLimit = cx_limit.isSelected();
		magic.chkdSong = cx_mos.isSelected();
		try {
			magic.limit = Integer.parseInt(tf_limit.getText());
		} catch (Exception e) {
		}
		magic.item = ((Descriptor) cb_limit_u.getSelectedItem()).selector;
		magic.sort = ((Descriptor) cb_selby.getSelectedItem()).selector;
		magic.live = cx_live.isSelected();
		magic.checkRegExp = cx_match.isSelected();
		magic.touch();
	}

	public static void setSelectedId(JComboBox cb, int id) {
		int n = cb.getItemCount();
		for (int i = 0; i < n; i++) {
			Descriptor d = (Descriptor) cb.getItemAt(i);
			if (d.selector == id) {
				cb.setSelectedItem(d);
				return;
			}
		}
	}

	public static BaseHeader.Smart doDialog(Controller controller, BaseHeader.Smart magic, String title, ITunesDB itdb) {
		if (magic == null)
			return null; // TODO: only one place knows how to create
		JDialog dialog = new JDialog(controller.mediachest, MessageFormat.format(
				Resources.TITLE_CONTENT_SELECTION_CRITERIA, title), true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final MagicListPropEditor mpe = new MagicListPropEditor(controller, dialog, magic, title, itdb);
		dialog.setContentPane(mpe);
		dialog.pack();
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				mpe.magic = null;
			}
		});
		dialog.setVisible(true);
		return mpe.magic;
	}

	JComboBox cb_allany, cb_limit_u, cb_selby;

	JTextField tf_limit;

	JCheckBox cx_limit, cx_live, cx_mos, cx_match;

	JButton b_del;

	Box b_rules;

	int curSel = -1;;
}

class RuleEditor extends JPanel {
	public static final Descriptor[] RULE_FIELDS = {
			new Descriptor(Resources.LIST_SONG_NAME, BaseHeader.Smart.SONGNAME, new StringEditPanel(PlayItem.TITLE)),
			new Descriptor(Resources.LIST_ALBUM, BaseHeader.Smart.ALBUM, new StringEditPanel(PlayItem.ALBUM)),
			new Descriptor(Resources.LIST_ALBUM_RATING, BaseHeader.Smart.ALBUM_RATING, new RateEditPanel()),
			new Descriptor(Resources.LIST_ALBUMARTIST, BaseHeader.Smart.ALBUM_ARTIST, new StringEditPanel(
					PlayItem.ALBUM)),
			new Descriptor(Resources.LIST_ARTIST, BaseHeader.Smart.ARTIST, new StringEditPanel(PlayItem.ARTIST)),
			new Descriptor(Resources.LIST_BITRATE, BaseHeader.Smart.BITRATE, new NumEditPanel(new Descriptor(
					Resources.LABEL_KBPS, 128))),
			new Descriptor(Resources.LIST_SAMPLING_RATE, BaseHeader.Smart.SAMPLING_RATE, new NumEditPanel(
					new Descriptor(Resources.LABEL_HZ, 44100))),
			new Descriptor(Resources.LIST_YEAR, BaseHeader.Smart.YEAR, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_GENRE, BaseHeader.Smart.GENRE, new StringEditPanel(PlayItem.GENRE)),
			new Descriptor(Resources.LIST_GROUPING, BaseHeader.Smart.GROUPING, new StringEditPanel()),
			new Descriptor(Resources.LIST_KIND, BaseHeader.Smart.KIND, new StringEditPanel()),
			new Descriptor(Resources.LIST_LAST_MODIFY, BaseHeader.Smart.LAST_MODIFY, new DateEditPanel()),
			new Descriptor(Resources.LIST_TRACKNUMBER, BaseHeader.Smart.TRACKNUMBER, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_SIZE, BaseHeader.Smart.SIZE, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_TIME, BaseHeader.Smart.TIME, new TimeEditPanel()),
			new Descriptor(Resources.LIST_COMMENT, BaseHeader.Smart.COMMENT, new StringEditPanel()),
			new Descriptor(Resources.LIST_TIME_ADDED, BaseHeader.Smart.TIME_ADDED, new DateEditPanel()),
			new Descriptor(Resources.LIST_COMPOSER, BaseHeader.Smart.COMPOSER, new StringEditPanel(PlayItem.COMPOSER)),
			new Descriptor(Resources.LIST_PLAYCOUNT, BaseHeader.Smart.PLAYCOUNT, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_LAST_PLAYED, BaseHeader.Smart.LAST_PLAYED, new DateEditPanel()),
			new Descriptor(Resources.LIST_RATING, BaseHeader.Smart.RATING, new RateEditPanel()),
			new Descriptor(Resources.LIST_COMPILATION, BaseHeader.Smart.COMPILATION, new ChoiceEditPanel()),
			new Descriptor(Resources.LABEL_PLAYLIST, BaseHeader.Smart.PLAYLIST, null),
			new Descriptor(Resources.LIST_DISC_NUM, BaseHeader.Smart.DISC_NUM, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_BPM, BaseHeader.Smart.BPM, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_SORTTITLE, BaseHeader.Smart.SORT_NAME, new StringEditPanel(
					PlayItem.SORT_TITLE)),
			new Descriptor(Resources.LIST_SORTALBUM, BaseHeader.Smart.SORT_ALBUM, new StringEditPanel(
					PlayItem.SORT_ALBUM)),
			new Descriptor(Resources.LIST_SORTALBUMARTIST, BaseHeader.Smart.SORT_ALBUM_ARTIST, new StringEditPanel(
					PlayItem.SORT_ALBUM_ARTIST)),
			new Descriptor(Resources.LIST_SORTARTIST, BaseHeader.Smart.SORT_ARTIST, new StringEditPanel(
					PlayItem.SORT_ARTIST)),
			new Descriptor(Resources.LIST_SORTCOMPOSER, BaseHeader.Smart.SORT_COMPOSER, new StringEditPanel(
					PlayItem.SORT_COMPOSER)),
			new Descriptor(Resources.LIST_SORTSHOW, BaseHeader.Smart.SORT_SHOW, new StringEditPanel()),
			new Descriptor(Resources.LIST_SKIPPEDCOUNT, BaseHeader.Smart.SKIP_COUNT, new NumEditPanel(null)),
			new Descriptor(Resources.LIST_LAST_SKIPPED, BaseHeader.Smart.LAST_SKIPPED, new DateEditPanel()) };

	protected BaseHeader.Rules.Rule rule;

	volatile boolean inUpdate;

	volatile ITunesDB itdb;

	RuleEditor(BaseHeader.Rules.Rule rule, ITunesDB itdb, String ln) {
		setLayout(new FixedGridLayout(8, 1, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
				Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP, Resources.CTRL_VERT_GAP, 4));
		this.rule = rule;
		this.itdb = itdb;
		for (Descriptor d : RULE_FIELDS)
			if (d.selector == BaseHeader.Smart.PLAYLIST)
				// TODO: review code since exceeding objects can be created
				d.value = new ChoicePListPanel(itdb, ln);
		add(cb_cr = new JComboBox(RULE_FIELDS), "0,0,2");
		cb_cr.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (inUpdate == false) {
					update();
				}
			}
		});

		setSelectedId(rule.id);
		update();
	}

	/*
	 * public Dimension getPreferredSize() { Dimension result =
	 * super.getPreferredSize(); System.err.println("Pref from super "+result);
	 * if (result == null) result = getLayout().preferredLayoutSize(this);
	 * System.err.println("Pref from layout "+result); return result; }
	 */
	void update() {
		try {
			inUpdate = true;
			Descriptor d = (Descriptor) cb_cr.getSelectedItem();
			if (getComponentCount() > 1)
				remove(getComponentCount() - 1);

			BaseEditPanel ep = (BaseEditPanel) ((BaseEditPanel) d.value).clone();
			ep.setDB(itdb);
			add((JComponent) ep, "2,0,6");
			ep.setData(rule);
			doLayout();
			validate();
		} finally {
			inUpdate = false;
		}
	}

	void collect() {
		rule.id = ((Descriptor) cb_cr.getSelectedItem()).selector;
		((BaseEditPanel) getComponent(1)).collect();
		rule.touch();
	}

	void setSelectedId(int id) {
		int n = cb_cr.getItemCount();
		for (int i = 0; i < n; i++) {
			Descriptor d = (Descriptor) cb_cr.getItemAt(i);
			if (d.selector == id) {
				cb_cr.setSelectedItem(d);
				return;
			}
		}
		System.err.println("No rule found for id:" + id);
	}

	BaseHeader.Rules.Rule getRule() {
		return rule;
	}

	JComboBox cb_cr;
}

abstract class BaseEditPanel extends JPanel {
	BaseHeader.Rules.Rule rule = null;// new BaseHeader.Rules.Rule();

	JComboBox cb_rule;

	ITunesDB itdb;

	volatile boolean inUpdate;

	BaseEditPanel() {
		setLayout(new FixedGridLayout(8, 1, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
				Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(cb_rule = new JComboBox(getActions()), "0,0,3");
		addSpecific();
		cb_rule.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (inUpdate == false) {
					collect();
					updateWrapper();
				}
			}
		});

	}

	void setDB(ITunesDB itdb) {
		this.itdb = itdb;
	}

	protected void updateWrapper() {
		try {
			inUpdate = true;
			update();
		} finally {
			inUpdate = false;
		}
	}

	abstract void addSpecific();

	abstract void update();

	abstract void collect();

	abstract Descriptor[] getActions();

	void setData(BaseHeader.Rules.Rule rule) {
		this.rule = rule;
		updateWrapper();
	}

	BaseHeader.Rules.Rule getData() {
		collect();
		return rule;
	}

	void setSelectedAction(int action) {
		int n = cb_rule.getItemCount();
		for (int i = 0; i < n; i++) {
			Descriptor d = (Descriptor) cb_rule.getItemAt(i);
			if (d.selector == action) {
				cb_rule.setSelectedItem(d);
				return;
			}
		}
	}

	void setSelectedAction(String name) {
		int n = cb_rule.getItemCount();
		for (int i = 0; i < n; i++) {
			Descriptor d = (Descriptor) cb_rule.getItemAt(i);
			if (d.name.equals(name)) {
				cb_rule.setSelectedItem(d);
				return;
			}
		}
	}

	public String toString() {
		return rule == null ? "null" : rule.toString();
	}

	public Object clone() {
		throw new Error();
	}
}

class StringEditPanel extends BaseEditPanel implements DocumentListener {
	JTextField value;

	int autoCompleteType;
	
	ITunesDB.PlayDirectory autocompleteDir;
	
	private volatile boolean disableAC;

	StringEditPanel() {

	}

	StringEditPanel(int autoCompleteType) {
		this.autoCompleteType = autoCompleteType;
	}

	void addSpecific() {
		add(value = new JTextField(), "3,0,5");
		value.getDocument().addDocumentListener(this);
	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_STRING_RULE_ACTION;
	}

	void update() {
		disableAC = true;
		if (rule.data != null)
			value.setText(rule.data);
		else
			value.setText("");
		setSelectedAction(rule.action);
		disableAC = false;
	}

	void collect() {
		rule.data = value.getText();
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;
	}

	public Object clone() {
		return new StringEditPanel(autoCompleteType);
	}

	public void removeUpdate(DocumentEvent e) {
		// autoComplete(e);
	}

	public void changedUpdate(DocumentEvent e) {
		// autoComplete(e);
	}

	public void insertUpdate(DocumentEvent e) {
		if (disableAC == false)
			autoComplete(e);
	}

	ITunesDB.PlayDirectory findDirectory() {
		if (autoCompleteType <= 0)
			return null;
		if (itdb == null)
			return null;
		if (autocompleteDir != null)
			return autocompleteDir;
		ITunesDB.PlayDirectory d = null;
		int n = itdb.getChildCount(itdb.getRoot());
		for (int i = 1; i < n; i++) {
			d = (ITunesDB.PlayDirectory) itdb.getChild(itdb.getRoot(), i);
			if (d.descriptor.selector == autoCompleteType) {
				System.err.printf("Found %s for %d, size\n", d, autoCompleteType);
				return d;
			}
		}
		// build a directory by type:
		autocompleteDir = new ITunesDB.PlayDirectory(new Descriptor("", autoCompleteType));
		itdb.processAll(new Visitor<PlayItem>() {
			public void visit(PlayItem t) {
				autocompleteDir.putObject((String) t.get(autoCompleteType), null);
			}
		});
		return autocompleteDir;
	}

	boolean noupdate;

	protected void autoComplete(DocumentEvent e) {
		if (noupdate)
			return;
		ITunesDB.PlayDirectory d = findDirectory();
		if (d == null)
			return;
		if (e.getLength() > 0) {
			final String vs = value.getText();
			if (vs.length() > 2) {
				final String[] acs = new String[1];
				if (d.entriesLike(vs, acs) > 0) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							noupdate = true;
							value.setText(acs[0]);
							System.err.println(acs[0]);
							value.setSelectionStart(vs.length());
							value.setSelectionEnd(acs[0].length());
							noupdate = false;
						}
					});

				}
			}
		}
	}
}

class NumEditPanel extends BaseEditPanel {
	JTextField numf, numt;

	Descriptor unit;

	NumEditPanel(Descriptor unit) {
		super();
		this.unit = unit;
		if (unit != null)
			add(new JLabel(unit.name), "7,0,1");
	}

	void addSpecific() {
		add(numf = new JTextField(), "3,0,2");
		numt = new JTextField();
	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_NUM_RULE_ACTION;
	}

	void update() {
		setSelectedAction(rule.action);
		remove(numt);
		if (BaseHeader.Smart.NUM_COND_IN_RANGE == rule.action) {
			add(numt, "5,0,2");
			numt.setText("" + rule.end);
		} else {
		}
		numf.setText("" + rule.start);
		doLayout();
		validate();
	}

	void collect() {
		try {
			rule.start = Integer.parseInt(numf.getText());
		} catch (Exception ex) {
		}
		try {
			rule.end = Integer.parseInt(numt.getText());
		} catch (Exception ex) {
		}
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;
	}

	public Object clone() {
		return new NumEditPanel(unit);
	}
}

class ChoicePListPanel extends BaseEditPanel {

	JComboBox cb_playlists;

	ITunesDB itdb;

	String ln;

	ChoicePListPanel(ITunesDB itdb, String ln) {
		this.itdb = itdb;
		this.ln = ln;
	}

	void addSpecific() {
		add(cb_playlists = new JComboBox(), "3,0,5");
	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_LIST_RULE_ACTION;
	}

	void update() {
		cb_playlists.removeAllItems();
		for (PlayList pl : Smart.RESERVED_PLS) {
			cb_playlists.addItem(pl);
			if (pl.isList(rule.start, rule.fillerStart))
				cb_playlists.setSelectedItem(pl);
		}
		System.err.printf("Idetifying list %d-%d%n", rule.start, rule.fillerStart);
		if (itdb == null)
			return;
		List<String> l = (List<String>) itdb.getPlayLists();
		for (String pl : l) {
			if (pl.equals(ln) == false) {
				PlayList plr = itdb.getExistingPlayList(pl);
				cb_playlists.addItem(plr);
				if (plr.isList(rule.start, rule.fillerStart))
					cb_playlists.setSelectedItem(plr);
			}
		}
		if (cb_playlists.getItemCount() > 0 && cb_playlists.getSelectedItem() == null)
			cb_playlists.setSelectedIndex(0);
		setSelectedAction(rule.action);
	}

	void collect() {
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;
		PlayList plr = (PlayList) cb_playlists.getSelectedItem();
		if (plr != null) {
			rule.fillerStart = (Integer) plr.getAttribute(PlayList.HASH2);
			rule.start = (Integer) plr.getAttribute(PlayList.HASH1);
		}
	}

	public Object clone() {
		return new ChoicePListPanel(itdb, ln);
	}
}

class ChoiceEditPanel extends BaseEditPanel {

	void addSpecific() {

	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_BOOL_RULE_ACTION;
	}

	void update() {
		setSelectedAction(rule.action);
	}

	void collect() {
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;
	}

	public Object clone() {
		return new ChoiceEditPanel();
	}
}

class DateEditPanel extends BaseEditPanel {
	static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Resources.DATE_FORMAT_MASK);

	JTextField datef, datet;

	JComboBox cb_unit;

	Descriptor unit;

	void addSpecific() {
		add(datef = new JTextField(), "3,0,2");
		datet = new JTextField();
		cb_unit = new JComboBox(new String[] { Resources.LIST_DAYS, Resources.LIST_WEEKS, Resources.LIST_MONTHS });
	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_DATE_RULE_ACTION;
	}

	void update() {
		remove(datet);
		if (rule.action == BaseHeader.Smart.DATE_COND_IS) {
			datef.setText(DATE_FORMAT.format(BaseHeader.toDate(rule.start)));
			datet.setText(DATE_FORMAT.format(BaseHeader.toDate(rule.end)));
			// System.err.println("Diff:"+(rule.end - rule.start)+" com
			// "+(24*60*60));
			if (rule.end - rule.start < 24 * 60 * 60) {
				setSelectedAction(Resources.LIST_IS);
			} else {
				setSelectedAction(Resources.LIST_IS_IN_RANGE);
				add(datet, "5,0,2");
				// datet.setEnabled(false);
			}
		} else {
			setSelectedAction(rule.action);
			if (rule.action != BaseHeader.Smart.DATE_COND_LAST && rule.action != BaseHeader.Smart.DATE_COND_NOT_LAST) {
				remove(cb_unit);
				// add(datet, "5,0,2");
				if (BaseHeader.toDate(rule.start) != null)
					datef.setText(DATE_FORMAT.format(BaseHeader.toDate(rule.start)));
				else
					datef.setText(DATE_FORMAT.format(new Date()));
				if (BaseHeader.toDate(rule.end) != null)
					datet.setText(DATE_FORMAT.format(BaseHeader.toDate(rule.end)));
				else
					datet.setText(DATE_FORMAT.format(new Date()));
			} else {
				// remove(datet);
				add(cb_unit, "5,0,2");
				datef.setText("" + (-rule.duration));
				if (rule.unit == BaseHeader.Smart.DAY_SEC)
					cb_unit.setSelectedIndex(0);
				else if (rule.unit == BaseHeader.Smart.WEEK_SEC)
					cb_unit.setSelectedIndex(1);
				else if (rule.unit == BaseHeader.Smart.MONTH_SEC)
					cb_unit.setSelectedIndex(2);
			}
		}
		doLayout();
		validate();
	}

	void collect() {
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;

		if (rule.action != BaseHeader.Smart.DATE_COND_LAST && rule.action != BaseHeader.Smart.DATE_COND_NOT_LAST) {
			try {
				rule.start = BaseHeader.fromDate(DATE_FORMAT.parse(datef.getText()));
			} catch (Exception ex) {
			}
			try {
				rule.end = BaseHeader.fromDate(DATE_FORMAT.parse(datet.getText()));
			} catch (Exception ex) {
			}
			if (((Descriptor) cb_rule.getSelectedItem()).name != Resources.LIST_IS_IN_RANGE)
				rule.end = rule.start + (24 * 80 * 60 - 1);
			rule.unit = 1;
			rule.unitEnd = 1;
		} else {
			// System.err.println("Dura "+datef.getText()+" sel
			// "+cb_unit.getSelectedIndex());
			// new Exception().printStackTrace();
			try {
				rule.duration = -Integer.parseInt(datef.getText());

			} catch (Exception e) {
				rule.duration = 0;
			}
			switch (cb_unit.getSelectedIndex()) {
			case 0: // days
				rule.unit = BaseHeader.Smart.DAY_SEC;
				break;
			case 1: // weeks
				rule.unit = BaseHeader.Smart.WEEK_SEC;
				break;
			case 2: // months
				rule.unit = BaseHeader.Smart.MONTH_SEC;
				break;
			}
			if (rule.action == BaseHeader.Smart.DATE_COND_LAST || rule.action == BaseHeader.Smart.DATE_COND_NOT_LAST)
				rule.fillerStart = rule.fillerEnd = 0x2dae2dae;
		}
	}

	public Object clone() {
		return new DateEditPanel();
	}
}

class TimeEditPanel extends BaseEditPanel {
	JTextField timef, timet;

	Descriptor unit;

	void addSpecific() {
		add(timef = new JTextField(), "3,0,2");
		add(timet = new JTextField(), "5,0,2");
	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_NUM_RULE_ACTION;
	}

	void update() {
		setSelectedAction(rule.action);
		remove(timet);
		if (BaseHeader.Smart.DATE_COND_IN_RANGE == rule.action) {
			add(timet, "5,0,2");
			timet.setText(MP3.formatTime(rule.end));
		} else {
		}
		timef.setText(MP3.formatTime(rule.start));
		doLayout();
		validate();
	}

	void collect() {
		try {
			rule.start = (int) MP3.parseTime(timef.getText());

		} catch (Exception ex) {
		}
		try {
			rule.end = (int) MP3.parseTime(timet.getText());
		} catch (Exception ex) {
		}
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;
	}

	public Object clone() {
		return new TimeEditPanel();
	}
}

class RateEditPanel extends BaseEditPanel {
	JComboBox ratet, ratef;

	void addSpecific() {
		add(ratef = new JComboBox(PlayItem.getRatingArray()), "3,0,2");
		add(ratet = new JComboBox(PlayItem.getRatingArray()), "5,0,2");
	}

	Descriptor[] getActions() {
		return BaseHeader.Smart.TYPE_NUM_RULE_ACTION;
	}

	void update() {
		setSelectedAction(rule.action);
		remove(ratet);
		if (BaseHeader.Smart.DATE_COND_IN_RANGE == rule.action) {
			add(ratet, "5,0,2");
			ratet.setSelectedIndex(rule.end / BaseHeader.RATING_FACTOR);
		} else {
		}
		ratef.setSelectedIndex(rule.start / BaseHeader.RATING_FACTOR);
		doLayout();
		validate();
	}

	void collect() {
		rule.start = ratef.getSelectedIndex() * BaseHeader.RATING_FACTOR;
		rule.end = ratet.getSelectedIndex() * BaseHeader.RATING_FACTOR;
		rule.action = ((Descriptor) cb_rule.getSelectedItem()).selector;
		rule.unit = 1;
		rule.unitEnd = 1;
	}

	public Object clone() {
		return new RateEditPanel();
	}
}