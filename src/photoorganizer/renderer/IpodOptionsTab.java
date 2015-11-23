/* MediaChest - IpodOptionsTab 
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
 *  $Id: IpodOptionsTab.java,v 1.57 2013/05/02 04:43:46 cvs Exp $
 */
package photoorganizer.renderer;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MP4;
import photoorganizer.ipod.IpodControl;
import photoorganizer.ipod.PlayItem;
import photoorganizer.ipod.Transliteration;

public class IpodOptionsTab extends JPanel implements Persistancable {
	public final static String SECNAME = "IpodOptionsTab";

	public final static String IPOD_DEVICE = "iPod_HD";

	public final static String CHAR_TRANSLITER = "TransliterClass";

	public final static String PATH_PARSE_ORDER = "PathParseInfo";

	public final static String FILE_PARSE_ORDER = "FileParseInfo";

	public final static String TRANSLIT_DISABLE = "DisabledTranslit";

	public final static String ACTION_ON_UNKNOWN = "ActionOnUnknown";

	public final static String ACTION_ON_THE_GO = "Action-on-the-go_lists";

	public final static String NODUP_PLAYLIST = "PlayListNoDup";

	public final static String MAINTAIN_DIR = "MaintainDirectories";

	public final static String FILE_RULE_OVERRIDE_TAG = "OverrideTag";

	public final static String SYNC_ID3 = "SyncID3Taga";

	public final static String ARTWORK_PREVIEW = "ArtworkPreview";
	
	public final static String DONOT_SHOW_ARTIST_FROM_COMPILATION = "DonotShowArtistsCompilation";
	
	public final static String COPY_ORIGINAL_PHOTO = "CopyFullsizePhoto";

	public final static String IPOD_DEVICE_WRITE = "iPod_HD_Writer";

	public final static String DEST_PATH_FIELD = "DestPathField";

	public final static String USER = "user";

	public final static String PASSWD = "password";

	public final static String WRITE_PASSWD = "write_password";

	public final static int OTG_DEF_ACTION = 1;

	// TODO: how to initialize? better to use DIR_INFO_MAP[TITLE] =
	// Resources.LABEL_TITLE
	static final String[] DIR_INFO_MAP = { Resources.LIST_NOTINUSE, Resources.LIST_TITLE, Resources.LIST_ALBUM,
			Resources.LIST_ARTIST, Resources.LIST_COMPOSER, Resources.LIST_GENRE, Resources.LIST_YEAR,
			Resources.LIST_TRACK, Resources.LIST_FILERULE };

	static final int[] PLAYITEM_INFO_MAP = { PlayItem.TITLE, PlayItem.ALBUM, PlayItem.ARTIST, PlayItem.COMPOSER,
			PlayItem.GENRE, PlayItem.YEAR, PlayItem.ORDER, PlayItem.FILENAME };

	// TODO: pattern codes have to be in sync with Resources.MASKS and
	static final char[] PATTERN_CODE = { '\0', 'm', 'A', 'M', 'e', 'g', 'z', 'T', '\0' };

	public static final int TITLE = 0;

	public static final int ALBUM = 1;

	public static final int ARTIST = 2;

	public static final int COMPOSER = 3;

	public static final int GENRE = 4;

	public static final int YEAR = 5;

	public static final int TRACK = 6;

	public static final int FILERULE = 7;

	public static final int ACT_ADDTO_IPOD = 1;

	public static final int ACT_DELFR_IPOD = 2;

	public static final int ACT_DEL_ITUNES = 3;
	
	public static final int ACT_REBUILD_ARTWORK = 4;

	public static final int MAXINFO_FIELDS = FILERULE + 1;

	protected static Transliteration transliteration;

	protected static Map fileRuleCache;

	private static ThreadLocal<String> iPodDev = new ThreadLocal<String>() {
		protected synchronized String initialValue() {
			return "";
		}
	};

	protected Controller controller;

	public IpodOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(8, 4, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
				Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(new SLabel(Resources.LABEL_IPOD_DEVICE), "0,0,3");
		add(tf_ipod_dev = new JTextField(), "0,1,1");
		JButton btn;
		add(btn = new JButton(Resources.CMD_BROWSE), "1,1,0,1");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				JDirectoryChooser dc = new JDirectoryChooser(IpodOptionsTab.this, null, null);
				if (dc.getDirectory() != null)
					tf_ipod_dev.setText(dc.getDirectory());
			}
		});
		add(cb_maintaindir = new JCheckBox(Resources.LABEL_USE_DIRALBUM), "2,1,0");
		cb_maintaindir.setFont(Resources.FNT_DLG_10);
		cb_maintaindir.setForeground(Resources.CLR_DLG_BLUE);
		add(cb_sync_id3 = new JCheckBox(Resources.LABEL_SYNC_ID3), "4,1,0");
		cb_sync_id3.setFont(Resources.FNT_DLG_10);
		cb_sync_id3.setForeground(Resources.CLR_DLG_BLUE);

		add(cb_copyphoto = new JCheckBox(Resources.LABEL_COPY_PHOTO), "2,0,0");
		cb_copyphoto.setFont(Resources.FNT_DLG_10);
		cb_copyphoto.setForeground(Resources.CLR_DLG_BLUE);
		
		add(cb_art_prev = new JCheckBox(Resources.LABEL_ARTWORK_PREV), "4,0,0");
		cb_art_prev.setFont(Resources.FNT_DLG_10);
		cb_art_prev.setForeground(Resources.CLR_DLG_BLUE);

		add(cb_aware_comp = new JCheckBox(Resources.LABEL_AWARE_COMPIL), "6,0,0");
		cb_aware_comp.setFont(Resources.FNT_DLG_10);
		cb_aware_comp.setForeground(Resources.CLR_DLG_BLUE);
		
		add(new SLabel(Resources.LABEL_TRANSLITERATION), "6,1,0");

		add(btn = new JButton(Resources.CMD_SETUP), "7,1,0");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog d = new JDialog((Frame) SwingUtilities.windowForComponent(IpodOptionsTab.this),
						Resources.TITLE_TRANS, true);
				d.setContentPane(new TransliterSelection(IpodOptionsTab.this.controller.getPrefs()));
				d.pack();
				d.setVisible(true);
			}
		});
		add(new SLabel(Resources.LABEL_FILE_RULE), "0,2,3");
		add(tf_file_rule = new JTextField(), "0,3,2");
		add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "2,3,0", tf_file_rule), "2,4,0");

		add(new SLabel(Resources.LABEL_ACTION_OTG_LIST), "3,2,0");
		add(cb_act_otgl = new JComboBox(new String[] { Resources.LIST_IPOD_OTG_IGNORE, Resources.LIST_IPOD_OTG_NEWNAME,
				Resources.LIST_IPOD_OTG_SAMENAME, Resources.LIST_IPOD_OTG_VIEWONLY }), "3,3,0");

		add(new SLabel(Resources.LABEL_ACTION_UNKNOWNFILES), "5,2,0");
		add(
				cb_act_unknwn = new JComboBox(new String[] { Resources.LIST_IPOD_LEFT_UNATTENDED,
						Resources.LIST_IPOD_ADD_DATABASE, Resources.LIST_IPOD_DELETE_DISK,
						Resources.LIST_IPOD_DELETE_RECORD, Resources.LIST_IPOD_RECOV_FIX_ARTWORK }), "5,3,0");

		add(cb_no_dup_pl = new JCheckBox(Resources.LABEL_PL_NODUP), "6,4,0");
		cb_no_dup_pl.setFont(Resources.FNT_DLG_10);
		cb_no_dup_pl.setForeground(Resources.CLR_DLG_BLUE);

		add(new SLabel(Resources.LABEL_PATH_RULE), "0,4,2");
		add(cb_overridetag = new JCheckBox(Resources.LABEL_OVERRIDETAG_BYRULE), "2,4,0");
		cb_overridetag.setFont(Resources.FNT_DLG_10);
		cb_overridetag.setForeground(Resources.CLR_DLG_BLUE);
		add(p_name_map = createDirectoryCompMapPanel(), "0,5,8,0");

		tf_file_rule.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				tf_file_rule.setToolTipText(getHelpFileRule(tf_file_rule.getText()));
			}

			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
		});
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		String p = (String) s.getProperty(SECNAME, IPOD_DEVICE);
		if (p != null && p.length() > 0) {
			tf_ipod_dev.setText(p);
			iPodDev.set(p);
		}
		try {
			Object[] mp = (Object[]) s.getProperty(SECNAME, PATH_PARSE_ORDER);
			int cn = 0;
			for (int i = p_name_map.getComponentCount() - 1; i >= 0; i--) {
				Component c = p_name_map.getComponent(i);
				if (c instanceof JComboBox) {
					if (cn < mp.length)
						((JComboBox) c).setSelectedIndex(((Integer) mp[cn++]).intValue() + 1);
				}
			}
		} catch (Exception e) {
			System.err.printf("Excpetion at load parse map %s %n", e);
		}
		p = (String) s.getProperty(SECNAME, FILE_PARSE_ORDER);
		if (p != null && p.length() > 0)
			tf_file_rule.setText(p);
		cb_no_dup_pl.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, NODUP_PLAYLIST), 0) == 1);
		cb_overridetag.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, FILE_RULE_OVERRIDE_TAG), 0) == 1);
		cb_maintaindir.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, MAINTAIN_DIR), 0) == 1);
		cb_act_unknwn.setSelectedIndex(IniPrefs.getInt(s.getProperty(SECNAME, ACTION_ON_UNKNOWN), 0));
		cb_act_otgl.setSelectedIndex(IniPrefs.getInt(s.getProperty(SECNAME, ACTION_ON_THE_GO), OTG_DEF_ACTION));
		cb_sync_id3.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, SYNC_ID3), 0) == 1);
		cb_art_prev.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, ARTWORK_PREVIEW), 0) == 1);
		cb_aware_comp.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, DONOT_SHOW_ARTIST_FROM_COMPILATION), 0) == 1);
		cb_copyphoto.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, COPY_ORIGINAL_PHOTO), 1) == 1);
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		s.setProperty(SECNAME, IPOD_DEVICE, tf_ipod_dev.getText());
		iPodDev.set(getDevice(controller));
		transliteration = null;
		getTransliteration(controller);
		int cn = 0;
		Integer[] mp = new Integer[MAXINFO_FIELDS];
		for (int i = p_name_map.getComponentCount() - 1; i >= 0; i--) {
			Component c = p_name_map.getComponent(i);
			if (c instanceof JComboBox) {
				if (cn < mp.length)
					mp[cn++] = new Integer(((JComboBox) c).getSelectedIndex() - 1);
			}
		}
		s.setProperty(SECNAME, PATH_PARSE_ORDER, mp);
		s.setProperty(SECNAME, FILE_PARSE_ORDER, tf_file_rule.getText());
		s.setProperty(SECNAME, NODUP_PLAYLIST, cb_no_dup_pl.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, MAINTAIN_DIR, cb_maintaindir.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, FILE_RULE_OVERRIDE_TAG, cb_overridetag.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, ACTION_ON_UNKNOWN, new Integer(cb_act_unknwn.getSelectedIndex()));
		s.setProperty(SECNAME, ACTION_ON_THE_GO, new Integer(cb_act_otgl.getSelectedIndex()));
		s.setProperty(SECNAME, SYNC_ID3, cb_sync_id3.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, ARTWORK_PREVIEW, cb_art_prev.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, DONOT_SHOW_ARTIST_FROM_COMPILATION, cb_aware_comp.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, COPY_ORIGINAL_PHOTO, cb_copyphoto.isSelected() ? Resources.I_YES : Resources.I_NO);
	}

	public static JPanel createDirectoryCompMapPanel() {
		JPanel result = new JPanel();
		result.setLayout(new FlowLayout(FlowLayout.LEFT));
		result.add(new SLabel(File.separator));
		JComboBox cb;
		result.add(cb = new JComboBox(DIR_INFO_MAP));
		cb.setFont(Resources.FNT_DLG_10);
		cb.setForeground(Resources.CLR_DLG_BLUE);
		result.add(new SLabel(File.separator));
		result.add(cb = new JComboBox(DIR_INFO_MAP));
		cb.setFont(Resources.FNT_DLG_10);
		cb.setForeground(Resources.CLR_DLG_BLUE);
		result.add(new SLabel(File.separator));
		result.add(cb = new JComboBox(DIR_INFO_MAP));
		cb.setFont(Resources.FNT_DLG_10);
		cb.setForeground(Resources.CLR_DLG_BLUE);
		result.add(new SLabel(File.separator));
		result.add(cb = new JComboBox(DIR_INFO_MAP));
		cb.setFont(Resources.FNT_DLG_10);
		cb.setForeground(Resources.CLR_DLG_BLUE);
		result.add(new SLabel(File.separator));
		result.add(cb = new JComboBox(DIR_INFO_MAP));
		cb.setFont(Resources.FNT_DLG_10);
		cb.setForeground(Resources.CLR_DLG_BLUE);
		result.add(new SLabel(File.separator));
		result.add(cb = new JComboBox(DIR_INFO_MAP));
		cb.setFont(Resources.FNT_DLG_10);
		cb.setForeground(Resources.CLR_DLG_BLUE);
		result.add(new SLabel(Resources.EXT_MP3 + "/" + Resources.EXT_WAV + "/.aac"));
		return result;
	}
	
	public static String getEncoding(Controller controller) {
		if (AppearanceOptionsTab.needEncoding(controller))
			return MiscellaneousOptionsTab.getEncoding(controller);
		return null;
	}

	// TODO: reconsider to cache some values to improve performance
	// TODO: consider to pass dynamic pattern
	public static String[] parsePath(String path, String encoding, Controller controller) {
		if (encoding == null)
			encoding = getEncoding(controller);
		String[] parsed = new String[MAXINFO_FIELDS];
		// TODO: tokenizer can be better
		File f = new File(path);
		for (int i = 0; i < parsed.length; i++) {
			if (encoding == null)
				parsed[i] = f.getName();
			else
				try {
					parsed[i] = new String(f.getName().getBytes("ISO8859_1"), encoding);
				} catch (UnsupportedEncodingException uee) {
					System.err.println("Unsupported encoding " + encoding);
					parsed[i] = f.getName();
				}
			f = f.getParentFile();
			if (f == null)
				break;
		}
		int dp = parsed[0].lastIndexOf('.');
		if (dp > 0) {
			parsed[0] = parsed[0].substring(0, dp);
		}
		IniPrefs s = controller.getPrefs();
		Object[] mp = null;
		try {
			mp = (Object[]) s.getProperty(SECNAME, PATH_PARSE_ORDER);
		} catch (Exception e) {
		}
		if (mp == null)
			return parsed;
		String[] result = new String[MAXINFO_FIELDS];
		for (int i = 0; i < parsed.length && i < mp.length; i++) {
			try {
				result[((Integer) mp[i]).intValue()] = parsed[i];
			} catch (Exception e) {
				// bad index
			}
		}
		f = new File(path);
		parsed = parseFileName(f.getName(), null, encoding, controller);
		for (int i = 0; i < parsed.length; i++)
			// {
			if (parsed[i] != null && i < result.length)
				result[i] = parsed[i];
		// System.err.print(" "+DIR_INFO_MAP[i+1]+":"+result[i]); }
		// System.err.println();
		return result;
	}

	public static String getDevice(Controller controller) {
		String result = (String) controller.getPrefs().getProperty(SECNAME, IPOD_DEVICE);
		if (result != null && result.length() > 0)
			return result;
		try {
			return IpodControl.detectIPod().getPath();
		} catch (NullPointerException npe) {
		}
		return "";
	}
	
	public static String getDevice() {
		return iPodDev.get();
	}

	public static boolean isOverrideTag(Controller controller) {
		return IniPrefs.getInt(controller.getPrefs().getProperty(SECNAME, FILE_RULE_OVERRIDE_TAG), 0) == 1;
	}

	public static String[] parseFileName(String fileName, String pattern, String encoding, Controller controller) {
		// IniPrefs s = controller.getSerializer();
		if (pattern == null)
			pattern = (String) controller.getPrefs().getProperty(SECNAME, FILE_PARSE_ORDER);
		// check for pattern in cache to retrieve Descriptor[]
		String[] parsed = new String[MAXINFO_FIELDS];
		if (pattern == null)
			return parsed;
		if (encoding == null)
			encoding = getEncoding(controller);
		synchronized (IpodOptionsTab.class) {
			if (fileRuleCache == null)
				fileRuleCache = new HashMap(10);
		}
		Descriptor[] nextSep = (Descriptor[]) fileRuleCache.get(pattern);
		int pp = 0;
		if (nextSep == null) {
			nextSep = buildFileRule(pattern);
			fileRuleCache.put(pattern, nextSep);
		}
		pp = fileName.lastIndexOf('.');
		if (pp > 0)
			fileName = fileName.substring(0, pp);
		if (encoding != null && encoding.length() > 0)
			try {
				fileName = new String(fileName.getBytes("ISO8859_1"), encoding);
			} catch (UnsupportedEncodingException uee) {
				System.err.println("Unsupported encoding " + encoding);
			}
		int sp = 0, ep = 0;
		for (int i = 0; i < nextSep.length/* && nextSep[i] != null */; i++) {
			if (nextSep[i].name.length() > 0) {
				sp = fileName.indexOf(nextSep[i].name, ep);
				if (sp > 0)
					sp += nextSep[i].name.length();
				else
					break;
			} else
				sp = ep;
			// System.err.println("DO "+nextSep[i]+" in
			// ["+fileName.substring(sp));
			if (sp >= 0 && nextSep[i].selector != -1) {
				if ((i + 1) < nextSep.length/* && nextSep[i+1] != null */) {
					if (nextSep[i + 1].name.length() > 0) {
						ep = fileName.indexOf(nextSep[i + 1].name, sp);
						if (ep > sp) {
							parsed[nextSep[i].selector] = fileName.substring(sp, ep);
							continue;
						}
					} else if (nextSep[i].selector == TRACK && fileName.length() - sp > 2) {
						ep = sp + 2;
						parsed[nextSep[i].selector] = fileName.substring(sp, ep).trim();
						continue;
					}
				}
				parsed[nextSep[i].selector] = fileName.substring(sp).trim();
			} else
				break;
		}
		return parsed;
	}

	static Descriptor[] buildFileRule(String pattern) {
		ArrayList result = new ArrayList(PATTERN_CODE.length);
		char[] patar = pattern.toCharArray();
		int pp = 0;
		for (int i = 0; i < patar.length; i++) {
			if (patar[i] == '%' && i < patar.length - 1) {
				i++;
				int sel = -1;
				for (int l = 0; l < PATTERN_CODE.length; l++)
					if (PATTERN_CODE[l] == patar[i]) {
						sel = l;
						break;
					}
				if (sel > 0) {
					result.add(new Descriptor(new String(patar, pp, i - pp - 1), sel - 1));
					// System.err.println("Des "+result.size()+" =
					// '"+((Descriptor)result.get(result.size()-1)).name+"'("+((Descriptor)result.get(result.size()-1)).selector);
					pp = i + 1;
				}
			}
		}
		if (pp < patar.length) {
			result.add(new Descriptor(new String(patar, pp, patar.length - pp), -1));
			// System.err.println("Fin Des "+result.size()+" =
			// '"+((Descriptor)result.get(result.size()-1)).name+"'("+((Descriptor)result.get(result.size()-1)).selector);
		}
		return (Descriptor[]) result.toArray(new Descriptor[result.size()]);
	}

	public static String buildPath(PlayItem pi, char sep, String encoding, Controller controller) {
		IniPrefs s = controller.getPrefs();
		Object[] mp = null;
		try {
			mp = (Object[]) s.getProperty(SECNAME, PATH_PARSE_ORDER);
		} catch (Exception e) {
		}
		// TODO get "MPEG audio file" as const
		String ext = "MPEG audio file".equals(pi.get(PlayItem.FILETYPE)) ? MP3.MP3 : MP4.TYPE;
		String pattern = (String) controller.getPrefs().getProperty(SECNAME, FILE_PARSE_ORDER);
		if (IniPrefs.getInt(s.getProperty(SECNAME, MAINTAIN_DIR), 0) == 0 || mp == null)
			return FileNameFormat.makeValidPathName(new FileNameFormat(pattern, true).format(pi)) + '.' + ext;
		StringBuffer res = new StringBuffer();
		for (int i = mp.length - 1; i >= 0; i--) {
			if (mp[i] != null && mp[i] instanceof Integer && (Integer) mp[i] >= 0
					&& (Integer) mp[i] < PLAYITEM_INFO_MAP.length) {
				Object pp = pi.get(PLAYITEM_INFO_MAP[(Integer) mp[i]]);
				if (pp != null) {
					if (res.length() > 0)
						res.append(sep);
					if (PlayItem.FILENAME == PLAYITEM_INFO_MAP[(Integer) mp[i]])
						res.append(FileNameFormat.makeValidPathName(new FileNameFormat(pattern, true).format(pi)) + '.'
								+ ext);
					else
						res.append(FileNameFormat.makeValidPathName(pp.toString()));
				}
			}
		}
		res.append('.').append(ext);
		return res.toString();
	}

	static String getHelpFileRule(String pattern) {
		Descriptor[] rulePatk = buildFileRule(pattern);
		String tts = "file://";
		for (int i = 0; i < rulePatk.length; i++)
			if (rulePatk[i] != null)
				tts += rulePatk[i].name + DIR_INFO_MAP[rulePatk[i].selector + 1];
		return tts;
	}

	/**
	 * Instantiate transliteration class if any for example
	 * photoorganizer.ipod.RusTranslit
	 */
	public synchronized static Transliteration getTransliteration(Controller controller) {
		if (controller != null && transliteration == null) {
			IniPrefs s = controller.getPrefs();
			if (IniPrefs.getInt(s.getProperty(SECNAME, TRANSLIT_DISABLE), 1) == 1)
				return null;
			String p = (String) s.getProperty(SECNAME, CHAR_TRANSLITER);
			if (p != null && p.length() > 0)
				try {
					transliteration = (Transliteration) Class.forName(p).newInstance();
				} catch (Throwable t) {
					System.err.println("Can't create transliteration class " + p + " " + t);
				}
		}
		return transliteration;
	}

	// utility methods to access stream
	public static InputStream openInputStream(Controller controller, String relPath) throws IOException {
		// supported protocols: file, http, https, ftp
		// TODO: should be new URL().openStream()
		String base = getDevice(controller);
		if (base.startsWith("http:") || base.startsWith("https:")) {
			// URLEncoder.encode(relPath, Resources.ENC_UTF_8)
		} else if (base.startsWith("ftp:")) {
		} else {
			if (base.startsWith("file:"))
				return new URL(base + relPath).openStream();
			return new FileInputStream(base + relPath);
		}
		throw new IOException("Unsupported protocol " + base);
	}
	
	public static boolean checkFile(Controller controller, String relPath) {
		String base = getDevice(controller);
		return new File(base, relPath).exists();
	}

	public static OutputStream openOutputStream(Controller controller, String relPath) throws IOException {
		IniPrefs s = controller.getPrefs();
		// supported protocols: file, http, https, ftp
		String base = (String) s.getProperty(SECNAME, IPOD_DEVICE);
		if (base == null)
			base = "";
		if (base.startsWith("http:") || base.startsWith("https:")) {
		} else if (base.startsWith("ftp:")) {
		} else {
			if (base.startsWith("file:"))
				return new FileOutputStream(new URL(base + relPath).getFile());
			return new FileOutputStream(base + relPath);
		}
		throw new IOException("Unsupported protocol " + base);
	}

	JTextField tf_ipod_dev, tf_file_rule;

	JPanel p_name_map;

	JComboBox cb_act_unknwn, cb_act_otgl;

	JCheckBox cb_tr_dis, cb_maintaindir, cb_no_dup_pl, cb_overridetag, cb_sync_id3,
	cb_art_prev, cb_aware_comp, cb_copyphoto;

	protected static class TransliterSelection extends JPanel implements ActionListener {
		JTextField tf_trans_class;

		JCheckBox cb_tr_dis;

		IniPrefs s;

		TransliterSelection(IniPrefs s) {
			setLayout(new FixedGridLayout(4, 4, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			this.s = s;
			add(new SLabel(Resources.LABEL_IPOD_TRANSLIT_CLASS), "0,0,0");
			add(tf_trans_class = new JTextField(), "0,1,4");
			add(cb_tr_dis = new JCheckBox(Resources.LABEL_DISABLE), "0,2,0");
			cb_tr_dis.setRolloverEnabled(true);
			cb_tr_dis.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					JCheckBox cb = (JCheckBox) e.getSource();
					tf_trans_class.setEnabled(cb.isSelected() == false);
				}
			});
			JButton b;
			add(b = new JButton(Resources.CMD_APPLY), "2,3,0");
			b.addActionListener(this);
			add(b = new JButton(Resources.CMD_CLOSE), "3,3,0");
			b.addActionListener(this);
			load();
		}

		public void actionPerformed(ActionEvent av) {
			save();
			SwingUtilities.windowForComponent(this).dispose();
		}

		void load() {
			String p = (String) s.getProperty(SECNAME, CHAR_TRANSLITER);
			if (p != null && p.length() > 0)
				tf_trans_class.setText(p);
			cb_tr_dis.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, TRANSLIT_DISABLE), 1) == 1);
		}

		void save() {
			s.setProperty(SECNAME, CHAR_TRANSLITER, tf_trans_class.getText());
			s.setProperty(SECNAME, TRANSLIT_DISABLE, cb_tr_dis.isSelected() ? Resources.I_YES : Resources.I_NO);
		}
	}
}