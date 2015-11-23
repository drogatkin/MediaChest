/* MediaChest - $RCSfile: AppearanceOptionsTab.java,v $                           
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
 *  $Id: AppearanceOptionsTab.java,v 1.29 2013/05/02 04:43:46 cvs Exp $            
 */

package photoorganizer.renderer;

import static mediautil.gen.MediaInfo.ALBUM;
import static mediautil.gen.MediaInfo.APERTURE;
import static mediautil.gen.MediaInfo.ARTIST;
import static mediautil.gen.MediaInfo.BITRATE;
import static mediautil.gen.MediaInfo.BPM;
import static mediautil.gen.MediaInfo.COMMENTS;
import static mediautil.gen.MediaInfo.COMPILATION;
import static mediautil.gen.MediaInfo.COMPOSER;
import static mediautil.gen.MediaInfo.DATETIMEORIGINAL;
import static mediautil.gen.MediaInfo.EXPOPROGRAM;
import static mediautil.gen.MediaInfo.FILESIZE;
import static mediautil.gen.MediaInfo.FLASH;
import static mediautil.gen.MediaInfo.FOCALLENGTH;
import static mediautil.gen.MediaInfo.GENRE;
import static mediautil.gen.MediaInfo.LASTMODIFIED;
import static mediautil.gen.MediaInfo.LASTPLAY;
import static mediautil.gen.MediaInfo.LASTSKIPPED;
import static mediautil.gen.MediaInfo.LENGTH;
import static mediautil.gen.MediaInfo.MAKE;
import static mediautil.gen.MediaInfo.METERING;
import static mediautil.gen.MediaInfo.MODE;
import static mediautil.gen.MediaInfo.MODEL;
import static mediautil.gen.MediaInfo.PARTOFSET;
import static mediautil.gen.MediaInfo.PLAYCOUNTER;
import static mediautil.gen.MediaInfo.QUALITY;
import static mediautil.gen.MediaInfo.RATING;
import static mediautil.gen.MediaInfo.RESOLUTIONX;
import static mediautil.gen.MediaInfo.SAMPLERATE;
import static mediautil.gen.MediaInfo.SHUTTER;
import static mediautil.gen.MediaInfo.SKIPCOUNTER;
import static mediautil.gen.MediaInfo.TITLE;
import static mediautil.gen.MediaInfo.TRACK;
import static mediautil.gen.MediaInfo.YEAR;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mediautil.gen.MediaInfo;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Persistancable;
import photoorganizer.Resources;

// TODO: provide serials of static methods to access to all properties.
// And do it for all options
public final class AppearanceOptionsTab extends JPanel implements Persistancable {
	public final static String SECNAME = "AppearanceOptions";

	public final static String PANES_VIEW = "PanesConfig";

	public final static String FIT_TO_SIZE = "FitToSize";

	public final static String INSTANT_UPDATE = "InstantUpdate";

	public final static String DIR_ENCODE = "BrowseEncode";

	public final static String FILE_TAG_MAP = "FileTagMap";

	// these suffixes used to get real property names
	public final static String LABEL_ = "Label";

	public final static String METHOD_ = "Method";

	public final static String BROWSE_ = "Browse";

	public final static String COLLECT_ = "Collect";

	public final static String IPOD_ = "iPod";

	public final static String ALIGN_ = "Align";

	public final static String TYPE_ = "Type";

	public final static String LAF = "L&F";

	// TODO consider using names from resources
	public final static String[] COLUMN_CHOICES = { Resources.LIST_EMPTY, ALBUM, ARTIST, BITRATE, GENRE, LENGTH, MODE,
			TITLE, COMMENTS, COMPOSER, SAMPLERATE, TRACK, YEAR, BPM, COMMENTS, FILESIZE, PARTOFSET, COMPILATION, MAKE,
			MODEL, DATETIMEORIGINAL, APERTURE, SHUTTER, FLASH, QUALITY, FOCALLENGTH, METERING, EXPOPROGRAM,
			RESOLUTIONX, RATING, PLAYCOUNTER, LASTPLAY, LASTMODIFIED, SKIPCOUNTER, LASTSKIPPED, MediaInfo.SHOW,
			MediaInfo.SEASON_NUM, MediaInfo.EPISODE_ID, MediaInfo.EPISODE_NUM };

	public static final ColumnDescriptor[][] DEF_TBL_DESCR = {
			{
					new ColumnDescriptor(Resources.HDR_NAME, new String[] { null, null, Resources.LIST_NAME }, 0, 0),
					new ColumnDescriptor(Resources.HDR_SIZE, new String[] { null, null, Resources.LIST_LENGTH }, 2, 2),
					new ColumnDescriptor(Resources.HDR_TYPE, new String[] { MAKE, TITLE, Resources.LIST_TYPE }, 0, 0),
					new ColumnDescriptor(Resources.HDR_MODIFIED, new String[] { DATETIMEORIGINAL, LENGTH,
							Resources.LIST_DATE }, 0, 0),
					new ColumnDescriptor(Resources.HDR_SHUTTER, new String[] { SHUTTER, BITRATE, null }, 2, 2),
					new ColumnDescriptor(Resources.HDR_APERTURE, new String[] { APERTURE, SAMPLERATE, null }, 2, 2),
					new ColumnDescriptor(Resources.HDR_FLASH, new String[] { FLASH, null, null }, 1, 1),
					new ColumnDescriptor(Resources.HDR_QUALITY, new String[] { QUALITY, MODE, null }, 0, 0),
					new ColumnDescriptor(Resources.HDR_ZOOM, new String[] { FOCALLENGTH, YEAR, null }, 2, 2) },
			{
					new ColumnDescriptor(Resources.HDR_NAME, new String[] { null, null, Resources.LIST_NAME }, 0, 0),
					new ColumnDescriptor(Resources.HDR_TAKEN, new String[] { DATETIMEORIGINAL, LENGTH,
							Resources.LIST_DATE }, 0, 0),
					new ColumnDescriptor(Resources.HDR_SHUTTER, new String[] { SHUTTER, BITRATE, null }, 2, 2),
					new ColumnDescriptor(Resources.HDR_APERTURE, new String[] { APERTURE, SAMPLERATE, null }, 2, 2),
					new ColumnDescriptor(Resources.HDR_FLASH, new String[] { FLASH, null, null }, 1, 1) },
			{
					new ColumnDescriptor(Resources.HDR_NAME, new String[] { TITLE, null, Resources.LIST_NAME },
							ColumnDescriptor.STRING, 0),
					new ColumnDescriptor(Resources.HDR_ARTIST, new String[] { null, ARTIST, null },
							ColumnDescriptor.STRING, 0),
					new ColumnDescriptor(Resources.HDR_ALBUM, new String[] { SHUTTER, ALBUM, null },
							ColumnDescriptor.STRING, 2),
					new ColumnDescriptor(Resources.HDR_GENRE, new String[] { DATETIMEORIGINAL, GENRE, null },
							ColumnDescriptor.STRING, 1),
					new ColumnDescriptor(Resources.HDR_LENGTH, new String[] { LENGTH, null, null },
							ColumnDescriptor.STRING, 1) } };

	public final static int THREE_PANES = 0;

	public final static int TWO_PANES = 1;

	public final static int BROWSE_VIEW = 0;

	public final static int COLLECT_VIEW = 1;

	public final static int IPOD_VIEW = 2;

	// TODO: since it's GUI visible, move to Resources
	public final static String LIST_STRING = "String";

	public final static String LIST_BOOL = "Bool";

	public final static String LIST_NUMBER = "Number";

	public final static String LIST_DATE = "Date";

	public final static String LIST_COLOR_STRING = "ColoredString";

	public final static String[] COLUMN_TYPES = { LIST_STRING, LIST_BOOL, LIST_NUMBER, LIST_DATE, LIST_COLOR_STRING };

	public static final int MAX_COLUMNS = 10;

	public static final int IN_ROW = 2;

	public static final String[] VIEW_TABLES = { BROWSE_, COLLECT_, IPOD_ };

	public static final String[] ATTR_SEQS = { "Prime", "Second", "Third" };

	public static final String[] VIEWS = { Resources.LABEL_BROWSERVIEW, Resources.LABEL_COLLECTVIEW,
			Resources.LABEL_IPODVIEW };

	public AppearanceOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(6, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(headers = new ColumnHeaderGrid(), "0,0,2," + (MAX_COLUMNS / IN_ROW + (MAX_COLUMNS % IN_ROW & 1)));
		add(tf_label = new JTextField(), "2,0,2");
		tf_label.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent arg0) {
				updateButtonLable(arg0);
			}

			public void insertUpdate(DocumentEvent arg0) {
				updateButtonLable(arg0);
			}

			public void removeUpdate(DocumentEvent arg0) {
				updateButtonLable(arg0);
			}

			private void updateButtonLable(DocumentEvent de) {
				headers.updateSelectedText(tf_label.getText());
			}
		});
		add(cb_primeColType = new JComboBox(COLUMN_CHOICES), "2,1,2");
		add(cb_secColType = new JComboBox(COLUMN_CHOICES), "2,2,2");
		add(cb_thirdColType = new JComboBox(Resources.FILE_ATTRIBUTES), "2,3,2");
		add(cb_align = new JComboBox(Resources.HORIZ_ALIGN), "2,4");
		add(cb_dataType = new JComboBox(COLUMN_TYPES), "3,4");
		add(tf_seq = new JTextField(), "2,5");
		tf_seq.setHorizontalAlignment(JTextField.RIGHT);
		JButton bt;
		add(bt = new JButton(Resources.CMD_RESET), "3,5");
		bt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});

		JPanel canvas = new JPanel();
		canvas.setBorder(new TitledBorder(new EtchedBorder(), Resources.TITLE_PANES));
		canvas.setOpaque(false);
		add(canvas, "4,0,2,7,16,8");

		add(cb_layout = new JComboBox(new String[] { Resources.LABEL_2PANES, Resources.LABEL_3PANES }), "4,1,2,1,28");
		add(cb_dirEncoding = new JCheckBox(Resources.LABEL_DIR_ENCODING), "4,2,2,1,28");

		add(new JLabel(Resources.LABEL_LAF, SwingConstants.LEFT), "4,3,1,1,28");
		UIManager.LookAndFeelInfo[] lnfis = UIManager.getInstalledLookAndFeels();
		String[] lnf_names = new String[lnfis.length + 1];
		lnf_names[0] = ""; // UIManager.getSystemLookAndFeelClassName()
		for (int i = 0; i < lnfis.length; i++)
			lnf_names[i + 1] = lnfis[i].getName(); //lnfis[i].getClassName();
		add(cx_laf = new JComboBox(lnf_names), "5,3,1,1,-28");

		add(cb_fitToSize = new JCheckBox(Resources.LABEL_FITTOSIZE), "4,4,2,1,28");
		add(cb_instantUpdate = new JCheckBox(Resources.LABEL_INSTANTUPDATE), "4,5,2,1,28");
		/*
		 * bg_tableView = new RadioButtonsGroup(new ChangeListener() { public
		 * void stateChanged(ChangeEvent e) { // TODO: store old one if
		 * (oldTableSelected != bg_tableView.getSelectedIndex())
		 * fillCellHeaders(oldTableSelected = bg_tableView.getSelectedIndex()); }
		 * });
		 */
		add(cb_tableView = new JComboBox(VIEWS), "0,6,2");
		cb_tableView.setSelectedItem(Resources.LABEL_BROWSERVIEW);
		cb_tableView.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (oldTableSelected != cb_tableView.getSelectedIndex())
					fillCellHeaders(oldTableSelected = cb_tableView.getSelectedIndex());
			}
		});
		oldTableSelected = BROWSE_VIEW;
		oldCellSelected = -1;
		add(bt = new JButton(Resources.CMD_DEFAULT), "2,6,2");
		bt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tables = new ColumnDescriptor[VIEW_TABLES.length][];
				oldCellSelected = -1;
				save();
				load();
			}
		});
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		cb_layout.setSelectedIndex(s.getInt(s.getProperty(SECNAME, PANES_VIEW), TWO_PANES));
		cb_fitToSize.setSelected(s.getInt(s.getProperty(SECNAME, FIT_TO_SIZE), 0) == 1);
		cb_instantUpdate.setSelected(s.getInt(s.getProperty(SECNAME, INSTANT_UPDATE), 0) == 1);
		cb_dirEncoding.setSelected(s.getInt(s.getProperty(SECNAME, DIR_ENCODE), 0) == 1);
		String a = (String) s.getProperty(SECNAME, LAF);
		if (a != null)
			cx_laf.setSelectedItem(a);

		tables = readDescriptions(s);
		fillCellHeaders(cb_tableView.getSelectedIndex());
	}

	public static ColumnDescriptor[][] readDescriptions(IniPrefs s) {
		ColumnDescriptor[][] result = new ColumnDescriptor[VIEW_TABLES.length][];
		for (int t = 0; t < VIEW_TABLES.length; t++) {
			ColumnDescriptor[] tableDescriptor = new ColumnDescriptor[MAX_COLUMNS];
			int d = 0;
			boolean reqDefault = true;
			for (int c = 0; c < MAX_COLUMNS; c++) {
				String label = (String) s.getProperty(SECNAME, VIEW_TABLES[t] + LABEL_ + c);
				if (label == null || label.trim().length() == 0) {
					if (reqDefault && DEF_TBL_DESCR[t].length > c && DEF_TBL_DESCR[t][c] != null
							&& DEF_TBL_DESCR[t][c].label.length() > 0)
						tableDescriptor[d++] = (ColumnDescriptor) DEF_TBL_DESCR[t][c].clone();
					else
						continue;
				} else {
					reqDefault = false;
					String[] methods = new String[ATTR_SEQS.length];
					boolean noMethods = false;
					for (int m = 0; m < ATTR_SEQS.length; m++) {
						methods[m] = (String) s.getProperty(SECNAME, VIEW_TABLES[t] + METHOD_ + ATTR_SEQS[m] + c);
						noMethods |= methods[m] != null && methods[m].trim().length() > 0;
					}
					if (!noMethods)
						continue;
					int align = s.getInt(s.getProperty(SECNAME, VIEW_TABLES[t] + ALIGN_ + c), 0);
					int type = s.getInt(s.getProperty(SECNAME, VIEW_TABLES[t] + TYPE_ + c), 0);
					tableDescriptor[d++] = new ColumnDescriptor(label, methods, type, align);
				}
			}
			result[t] = tableDescriptor;
		}
		return result;
	}

	public void save() {
		storeAttributes(oldTableSelected, oldCellSelected);
		IniPrefs s = controller.getPrefs();
		s.setProperty(SECNAME, DIR_ENCODE, cb_dirEncoding.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, PANES_VIEW, new Integer(cb_layout.getSelectedIndex()));
		s.setProperty(SECNAME, FIT_TO_SIZE, cb_fitToSize.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, INSTANT_UPDATE, cb_instantUpdate.isSelected() ? Resources.I_YES : Resources.I_NO);
		// TODO apply L&F if changed String 
		s.setProperty(SECNAME, LAF, cx_laf.getSelectedItem());
		for (int t = 0; tables != null && t < tables.length; t++) {
			ColumnDescriptor[] tableDescriptor = tables[t];
			int d = 0;
			for (int c = 0; c < MAX_COLUMNS; c++) {
				if (tableDescriptor != null && c < tableDescriptor.length && tableDescriptor[c] != null
						&& tableDescriptor[c].label != null && tableDescriptor[c].label.trim().length() > 0) {
					s.setProperty(SECNAME, VIEW_TABLES[t] + LABEL_ + d, tableDescriptor[c].label);
					String[] methods = tableDescriptor[c].attributes;
					for (int m = 0; m < ATTR_SEQS.length; m++)
						if (methods != null && m < methods.length && methods[m] != null)
							s.setProperty(SECNAME, VIEW_TABLES[t] + METHOD_ + ATTR_SEQS[m] + d, methods[m]);
						else
							s.setProperty(SECNAME, VIEW_TABLES[t] + METHOD_ + ATTR_SEQS[m] + d, Resources.LIST_EMPTY);
					s.setProperty(SECNAME, VIEW_TABLES[t] + ALIGN_ + d, new Integer(tableDescriptor[c].align));
					s.setProperty(SECNAME, VIEW_TABLES[t] + TYPE_ + d, new Integer(tableDescriptor[c].type));
					if (d < c)
						eraseEntry(s, t, c);
					d++;
				} else
					eraseEntry(s, t, c);
			}
		}
	}

	protected void eraseEntry(IniPrefs s, int ti, int ei) {
		s.setProperty(SECNAME, VIEW_TABLES[ti] + LABEL_ + ei, null);
		for (int m = 0; m < ATTR_SEQS.length; m++)
			s.setProperty(SECNAME, VIEW_TABLES[ti] + METHOD_ + ATTR_SEQS[m] + ei, null);
		s.setProperty(SECNAME, VIEW_TABLES[ti] + ALIGN_ + ei, null);
		s.setProperty(SECNAME, VIEW_TABLES[ti] + TYPE_ + ei, null);
	}

	protected void fillCellHeaders(int tableInd) {
		if (tables == null || tables.length < tableInd || tableInd < 0)
			return;
		ColumnDescriptor[] tableDescriptor = tables[tableInd];
		for (int i = 0; i < MAX_COLUMNS; i++) {
			if (i < tableDescriptor.length) {
				ColumnDescriptor cellDescr = (ColumnDescriptor) tableDescriptor[i];
				if (cellDescr != null && cellDescr.label != null) {
					// System.err.println("Set label ["+i+"]="+cellDescr.label);
					((JToggleButton) headers.getComponent(i)).setText(cellDescr.label);
					continue;
				}
			}
			((JToggleButton) headers.getComponent(i)).setText("");
		}
		fillCellDescr(tableInd, oldCellSelected >= 0 ? oldCellSelected : 0);
		headers.setSelectedIndex(oldCellSelected >= 0 ? oldCellSelected : 0);
	}

	protected void fillCellDescr(int tableInd, int cellInd) {
		if (tables == null || tableInd < 0 || cellInd < 0)
			return;
		Object[] tableDescriptor = (Object[]) tables[tableInd];
		// System.err.println("Filling ["+tableInd+"]["+cellInd+']');
		if (tableDescriptor.length > cellInd) {
			ColumnDescriptor cellDescr = (ColumnDescriptor) tableDescriptor[cellInd];
			tf_seq.setText(String.valueOf(cellInd));
			if (cellDescr != null) {
				if (cellDescr.label != null)
					tf_label.setText(cellDescr.label);
				else
					tf_label.setText("");
				String[] methods = cellDescr.attributes;
				if (methods != null) {
					if (methods[0] != null)
						cb_primeColType.setSelectedItem(methods[0]);
					else
						cb_primeColType.setSelectedIndex(0);
					if (methods[1] != null)
						cb_secColType.setSelectedItem(methods[1]);
					else
						cb_secColType.setSelectedIndex(0);
					if (methods[2] != null)
						cb_thirdColType.setSelectedItem(methods[2]);
					else
						cb_thirdColType.setSelectedIndex(0);
					cb_dataType.setSelectedIndex(cellDescr.type);
					cb_align.setSelectedIndex(cellDescr.align);
					return;
				}
			}
		}
		cb_primeColType.setSelectedIndex(0);
		cb_secColType.setSelectedIndex(0);
		cb_thirdColType.setSelectedIndex(0);
		cb_dataType.setSelectedIndex(0);
		cb_align.setSelectedIndex(0);
		tf_label.setText("");
	}

	protected void reset() {
		fillCellDescr(oldTableSelected, oldCellSelected);
	}

	protected void storeAttributes(int tableIndex, int cellIndex) {
		if (tables == null || tableIndex < 0 || cellIndex < 0 || tableIndex >= tables.length
				|| tables[tableIndex] == null)
			return;
		int newCellIndex = cellIndex;
		try {
			newCellIndex = Integer.parseInt(tf_seq.getText().trim());
		} catch (NumberFormatException nfe) {
		}
		// System.err.println("Store ["+tableIndex+"]["+cellIndex+']');
		if (newCellIndex != cellIndex) {
			if (tables[tableIndex].length > newCellIndex) {
				// System.err.println("Swapping components "+newCellIndex +" &
				// "+cellIndex);
				ColumnDescriptor cellDescr = tables[tableIndex][newCellIndex];
				tables[tableIndex][newCellIndex] = tables[tableIndex][cellIndex];
				tables[tableIndex][cellIndex] = cellDescr;
				// System.err.println("Label of "+cellIndex+" is "+cellDescr);
				((JToggleButton) headers.getComponent(cellIndex))
						.setText((cellDescr == null || cellDescr.label == null) ? "" : cellDescr.label);
			}
			cellIndex = newCellIndex;
		}
		if (tables[tableIndex].length <= cellIndex) {
			ColumnDescriptor[] td = new ColumnDescriptor[cellIndex + 1];
			System.arraycopy(tables[tableIndex], 0, td, 0, tables[tableIndex].length);
			tables[tableIndex] = td;
		}
		ColumnDescriptor cellDescr = tables[tableIndex][cellIndex];
		if (cellDescr == null) {
			cellDescr = new ColumnDescriptor();
			tables[tableIndex][cellIndex] = cellDescr;
		}

		cellDescr.label = tf_label.getText();
		if (cellDescr.attributes == null || cellDescr.attributes.length < ATTR_SEQS.length)
			cellDescr.attributes = new String[ATTR_SEQS.length];
		cellDescr.attributes[0] = (String) cb_primeColType.getSelectedItem();
		cellDescr.attributes[1] = (String) cb_secColType.getSelectedItem();
		cellDescr.attributes[2] = (String) cb_thirdColType.getSelectedItem();
		cellDescr.type = cb_dataType.getSelectedIndex();
		cellDescr.align = cb_align.getSelectedIndex();
		((JToggleButton) headers.getComponent(cellIndex)).setText(cellDescr.label);
		// System.err.println("Label of "+cellIndex+" is "+cellDescr.label);
	}

	public static boolean needEncoding(Controller controller) {
		IniPrefs s = controller.getPrefs();
		return s.getInt(s.getProperty(SECNAME, DIR_ENCODE), 0) == 1;
	}
	
	class ColumnHeaderGrid extends JPanel implements ChangeListener {
		ColumnHeaderGrid() {
			setLayout(new GridLayout(MAX_COLUMNS / IN_ROW + (MAX_COLUMNS % IN_ROW & 1), IN_ROW));
			groupHeaders = new RadioButtonsGroup(this);
			JToggleButton column;
			for (int i = 0; i < MAX_COLUMNS; i++) {
				add(column = new JToggleButton());
				groupHeaders.add(column, i);
			}
		}

		public void stateChanged(ChangeEvent e) {
			if (oldCellSelected != groupHeaders.getSelectedIndex()
					|| oldTableSelected != cb_tableView.getSelectedIndex()) {
				storeAttributes(oldTableSelected, oldCellSelected);
				oldCellSelected = groupHeaders.getSelectedIndex();
				oldTableSelected = cb_tableView.getSelectedIndex();
				fillCellDescr(oldTableSelected, oldCellSelected);
			}
		}

		void updateSelectedText(String text) {
			int si = groupHeaders.getSelectedIndex();
			if (si >= 0) // no concurrency since created in constructor
				groupHeaders.get(si).setText(text);
		}

		void setSelectedIndex(int index) {
			groupHeaders.setSelectedIndex(index);
		}

		RadioButtonsGroup groupHeaders;
	}

	protected Controller controller;

	protected JCheckBox cb_fitToSize, cb_instantUpdate, cb_dirEncoding;

	protected JComboBox cb_primeColType, cb_secColType, cb_thirdColType, cb_dataType, cb_align, cb_layout,
			cb_tableView, cx_laf;

	protected JTextField tf_seq, tf_label;

	protected ColumnHeaderGrid headers;

	protected ColumnDescriptor[][] tables;

	protected int oldTableSelected, oldCellSelected;
}
