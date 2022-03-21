/* MediaChest - $RCSfile: PropertiesPanel.java,v $
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  $Id: PropertiesPanel.java,v 1.22 2007/12/28 04:35:11 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.CIFF;
import mediautil.image.jpeg.Entry;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.IFD;
import mediautil.image.jpeg.JFXX;
import mediautil.image.jpeg.Naming;
import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;

public class PropertiesPanel extends JTabbedPane {

	public PropertiesPanel(MediaFormat format, Controller controller) {
		MediaInfo info = format.getMediaInfo();
		if (info instanceof Exif) {
			insertTab(Resources.TAB_MAIN_IFD, (Icon) null, new JScrollPane(new JTable(new PropertiesTableModel(
					(Exif) info))), Resources.TTIP_MAIN_IFD, 0);
			insertTab(Resources.TAB_THUMBNAIL, (Icon) null, new JScrollPane(new JTable(new PropertiesTableModel(
					(Exif) info, false))), Resources.TTIP_THUMB_IFD, 1);
		} else if (info instanceof JFXX) {
			insertTab(Resources.TAB_PICTURE_INFO, (Icon) null, new JScrollPane(new JTable(new HashPropTableModel(
					((JFXX) info).getPictureInfo()))), Resources.TTIP_PICTURE_INFO, 0);
			insertTab(Resources.TAB_CAMERA, (Icon) null, new JScrollPane(new JTable(new HashPropTableModel(
					((JFXX) info).getCameraInfo()))), Resources.TTIP_CAMERA_INFO, 1);
			insertTab(Resources.TAB_DIAGNOSTIC, (Icon) null, new JScrollPane(new JTable(new HashPropTableModel(
					((JFXX) info).getDiagInfo()))), Resources.TTIP_DIAG_INFO, 2);
		} else if (info instanceof CIFF) {
			insertTab(Resources.TAB_HEAP, (Icon) null, new JScrollPane(new JTable(new HashPropTableModel(((CIFF) info)
					.getProperties()))), Resources.TTIP_HEAP, 0);
		} else if ((format.getType() & MediaFormat.AUDIO )> 0) {
			JTable component;
			insertTab(Resources.TAB_ID3, (Icon) null, new JScrollPane(component = new JTable(new AttributeTableModel(
					info, MediaInfo.PLAY_ATTRIBUTES))), Resources.TTIP_ID3, 0);
			MiscellaneousOptionsTab.applyFontSettings(component, controller);
			insertTab(Resources.TAB_MP3, (Icon) null, new JScrollPane(new JTable(new AttributeTableModel(info,
					MediaInfo.MEDIA_ATTRIBUTES))), Resources.TTIP_MP3, 1);
		}
		insertTab(Resources.TAB_JPEG, (Icon) null, new ImageInfoPanel(info, format), Resources.TTIP_JPEG, getTabCount());
	}

	public void setProperties(MediaFormat format) {
	}

	public static void showProperties(MediaFormat format, Controller controller) {
		JFrame frame = new JFrame(Resources.TITLE_PROPS_OF + format.getFile());
		frame.getContentPane().add(new PropertiesPanel(format, controller));
		frame.pack();
		frame.setVisible(true);
		frame.setIconImage(controller.getMainIcon());
	}
}

class ImageInfoPanel extends JPanel {
	ImageInfoPanel(MediaInfo info, MediaFormat format) {
		setLayout(new BorderLayout());
		String basicInfo = "<html>";
		try {
			Object[] as = info.getFiveMajorAttributes();
			for (int i = 0; i < as.length; i++)
				if (as[i] != null)
					basicInfo += as[i].toString() + "<br>";
		} catch (Exception e) {
			// no five
		}
		if (basicInfo == null)
			try {
				basicInfo = (String) info.getAttribute(info.TITLE);
			} catch (Exception e) {
				// no title
			}
		if (basicInfo == null)
			basicInfo = info.toString();
		try {
			add(new JLabel(basicInfo, format.getThumbnail(AbstractImageInfo.DEFAULT_THUMB_SIZE), JLabel.CENTER),
					BorderLayout.NORTH); // , "Center"
			if (info.getAttribute(info.COMMENTS) != null) {
				JTextArea c;
				JScrollPane s;
				add(s = new JScrollPane(c = new JTextArea("" + info.getAttribute(info.COMMENTS)/* ,4,1 */)),
						BorderLayout.SOUTH);
				s.setPreferredSize(new Dimension(100, 60));
				c.setEnabled(false);
				c.setLineWrap(true);
				c.setWrapStyleWord(true);
				c.setEditable(false);
				c.setBorder(BorderFactory.createEmptyBorder());
			}
		} catch (Exception e) {
			// add(new JLabel(e.getMessage()), "Center");
		}
	}
}

class PropertiesTableModel extends AbstractTableModel {

	final String[] columnnames = { Resources.HDR_TAG, Resources.HDR_TYPE, Resources.HDR_VALUE };

	private Vector tags;

	private Exif exif;

	private boolean flag_main_image;

	public PropertiesTableModel(Exif exif) {
		this(exif, true);
	}

	public PropertiesTableModel(Exif exif, boolean main) {
		tags = new Vector();
		setExif(exif, main);
	}

	public void setExif(Exif exif) {
		setExif(exif, true);
	}

	public void setExif(Exif exif, boolean main) {
		this.exif = exif;
		flag_main_image = main;
		if (exif == null)
			return;
		IFD[] ifds = exif.getIFDs();
		addIfd(ifds[flag_main_image ? 0 : 1]);
	}

	protected void addIfd(IFD ifd) {
		try {
			Iterator i = ifd.getEntries().keySet().iterator();
			while (i.hasNext()) {
				this.tags.addElement(i.next());
			}
			IFD[] ifds = ifd.getIFDs();
			for (int k = 0; ifds != null && k < ifds.length; k++)
				addIfd(ifds[k]);
		} catch (NullPointerException npe) {
			System.err.println("No IFDs found in " + exif);
		}
	}

	public int getRowCount() {
		return tags.size();
	}

	public int getColumnCount() {
		return columnnames.length;
	}

	public Object getValueAt(int row, int column) {
		if (column == 0)
			return Naming.getTagName((Integer) tags.elementAt(row));
		Entry entry = exif.getTagValue((Integer) tags.elementAt(row), -1, flag_main_image);
		switch (column) {
		case 1:
			return Naming.getTypeName(entry.getType());
		case 2:
			return entry;
		}
		return null;
	}

	public String getColumnName(int column) {
		return columnnames[column];
	}
}

class HashPropTableModel extends AbstractTableModel {
	final String[] columnnames = { Resources.HDR_TAG, Resources.HDR_VALUE };

	Object[] tags;

	Hashtable table;

	public HashPropTableModel(Hashtable table) {
		this.table = table;
		tags = new Object[table.size()];
		Enumeration e = table.keys();
		for (int i = 0; e.hasMoreElements(); i++) {
			tags[i] = e.nextElement();
		}
	}

	public int getRowCount() {
		return tags.length;
	}

	public int getColumnCount() {
		return columnnames.length;
	}

	public String getColumnName(int column) {
		return columnnames[column];
	}

	public Object getValueAt(int row, int column) {
		switch (column) {
		case 0:
			if (tags[row] instanceof String)
				return tags[row];
			else if (tags[row] instanceof Integer)
				return Naming.getPropName((Integer) tags[row]);
			break;
		case 1:
			return table.get(tags[row]);
		}
		return null;
	}
}

class AttributeTableModel extends AbstractTableModel {
	final String[] columnnames = { Resources.HDR_TAG, Resources.HDR_VALUE };

	Object[] tags, values;

	int size;

	public AttributeTableModel(MediaInfo info, String[] atributes) {
		tags = new Object[atributes.length];
		values = new Object[atributes.length];
		for (int i = 0; i < atributes.length; i++) {
			try {
				if (MediaInfo.LENGTH.equals(atributes[i]))
					values[size] = info.getAttribute(MediaInfo.ESS_TIMESTAMP);
				else if (MediaInfo.MODE.equals(atributes[i]))
					values[size] = MP3.MODE_NAMES[info.getIntAttribute(MediaInfo.MODE)];				
				else
					values[size] = MediaInfo.GENRE.equals(atributes[i]) ? MP3.findGenre(info) : info
							.getAttribute(atributes[i]);
				if (values[size] != null) {
					if (MediaInfo.PICTURE.equals(atributes[i]) && values[size] instanceof ImageIcon) {
						ImageIcon ii = (ImageIcon)values[size];
						values[size] = String.format("%dx%d %s", ii.getIconWidth(), ii.getIconHeight(), ii.getDescription());
					}
					tags[size] = atributes[i];
					size++;
				}
			} catch (Exception iae) {
				// System.err.println("AttributeTableModel "+iae);
			}
		}
	}

	public int getRowCount() {
		return size;
	}

	public int getColumnCount() {
		return columnnames.length;
	}

	public String getColumnName(int column) {
		return columnnames[column];
	}

	public Object getValueAt(int row, int column) {
		switch (column) {
		case 0:
			return tags[row];
		case 1:
			return values[row];
		}
		return null;
	}
}
