/* MediaChest - $RCSfile: ThumbnailsPanel.java,v $
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
 *  $Id: ThumbnailsPanel.java,v 1.35 2008/04/15 23:14:42 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.TreePath;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;

import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.Thumbnail;

public class ThumbnailsPanel extends JPanel {
	public static final int THUMB_SIZE_X = 162;

	public static final int THUMB_SIZE_Y = 142; // should be

	// 960*THUMB_SIZE_X/1280+FONT_HEIGHT

	static final int THUMBS_IN_ROW = 5;

	private int labelTextHeight = 16;

	// labelTextHeight = new Label("WWW").getPreferredSize().height;

	public ThumbnailsPanel(Controller controller) {
		this.controller = controller;
		collectionpanel = (PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION);
		albumpanel = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
		setMinimumSize(Resources.MIN_PANEL_DIMENSION);
		setImageView();
		imagepanel.setThumbnailsPanel(this);
	}

	void setImageView() {
		imagepanel = (PhotoImagePanel) controller.component(Controller.COMP_IMAGEPANEL);
	}

	void calculateLayout() {
		IniPrefs s = controller.getPrefs();
		reg_border = ThumbnailsOptionsTab.createBorder((String) s.getProperty(ThumbnailsOptionsTab.SECNAME,
				ThumbnailsOptionsTab.BORDER));
		selected_border = ThumbnailsOptionsTab.createBorder((String) s.getProperty(ThumbnailsOptionsTab.SECNAME,
				ThumbnailsOptionsTab.SELECTEDBORDER));

		Integer i = (Integer) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.VERTAXIS);
		v = THUMBS_IN_ROW;
		h = 0;
		if (i != null && i.intValue() == 0) {
			i = (Integer) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FIXAXIS);
			if (i != null) {
				v = 0;
				h = i.intValue();
			}
		} else {
			i = (Integer) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FIXAXIS);
			if (i != null) {
				h = 0;
				v = i.intValue();
			}
		}
		sx = THUMB_SIZE_X;
		sy = THUMB_SIZE_Y;
		i = (Integer) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.CELLWIDTH);
		if (i != null)
			sx = i.intValue();
		else
			s.setProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.CELLWIDTH, new Integer(sx));
		i = (Integer) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.CELLHEIGHT);
		if (i != null)
			sy = i.intValue();
		else
			s.setProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.CELLHEIGHT, new Integer(sy));
		setLayout(new GridLayout(h, v));
	}

	public Component createThumbnail(MediaFormat format) {
		if (format == null)
			return null;
		// check for duplicates
		int cc = getComponentCount();
		for (int i = 0; i < cc; i++) {
			Component c = this.getComponent(i);
			if (c instanceof ThumbnailComponent && format.equals(((ThumbnailComponent) c).getFormat()))
				return c;
		}
		return new ThumbnailComponent(format);
	}

	public Dimension getPreferredSize() {
		return preferredsize;
	}

	public void updateImages(Object format) {
		updateImages(new Object[] { format });
	}

	public long getLength() {
		return length;
	}

	public int getTime() {
		return time;
	}

	public void removeAll() {
		super.removeAll();
		length = 0;
		time = 0;
	}

	public boolean updateMedias(MediaFormat[] formats) {
		removeAll();
		calculateLayout();
		boolean fsel = false;
		for (int i = 0; formats != null && i < formats.length; i++) {
			if (isLowMemory(true))
				break;
			if (formats[i] != null && formats[i].isValid()) {
				add(createThumbnail(formats[i]));
				fsel |= true;
			}
		}
		adjustDimension();
		return fsel;
	}

	public boolean updateImages(Object[] medias) {
		removeAll();
		calculateLayout();
		MediaFormat format = null;
		boolean fsel = false;
		for (int i = 0; medias != null && i < medias.length; i++) {
			if (isLowMemory(true))
				break;
			if (medias[i] instanceof MediaFormat) {
				format = (MediaFormat) medias[i];
			} else if (medias[i] instanceof File) {
				File f = (File) medias[i];
				if (f.isFile())
					format = MediaFormatFactory.createMediaFormat(f);
				else if (f.isDirectory() && medias.length == 1) {
					fsel |= updateImages(f.listFiles());
				}
			}
			if (format != null && format.isValid()) {
				add(createThumbnail(format));
				fsel |= true;
			}
		}
		adjustDimension();
		return fsel;
	}

	public void addImage(MediaFormat[] formats) {
		calculateLayout();
		for (MediaFormat format : formats)
			add(createThumbnail(format));
		adjustDimension();
	}

	public void addImage(File[] files) {
		MediaFormat[] formats = new MediaFormat[files.length];
		int c = 0;
		for (File file : files) {
			MediaFormat format = MediaFormatFactory.createMediaFormat(file);
			if (format != null && format.isValid())
				formats[c++] = format;
		}
		addImage(formats);
	}

	public Component add(Component c) {
		Component result = null;
		if (c != null && c instanceof ThumbnailComponent) {
			MediaFormat af = ((ThumbnailComponent) c).getFormat();
			length += af.getFileSize();
			if ((af.getType() & (MediaFormat.AUDIO+MediaFormat.VIDEO)) > 0)
				time += af.getMediaInfo().getLongAttribute(MediaInfo.LENGTH);
			result = super.add(c);
		}
		return result;
	}

	public void remove(Component c) {
		super.remove(c);
		if (c != null && c instanceof ThumbnailComponent) {
			MediaFormat af = ((ThumbnailComponent) c).getFormat();
			length -= af.getFileSize();
			if ((af.getType() & MediaFormat.AUDIO) > 0)
				time -= af.getMediaInfo().getLongAttribute(MediaInfo.LENGTH);
		}
	}

	void adjustDimension() {
		if (v != 0)
			preferredsize = new Dimension(v * sx, (getComponentCount() / v + 1) * sy);
		else
			preferredsize = new Dimension((getComponentCount() / h + 1) * sx, h * sy);
		setSize(preferredsize);
		revalidate();
		repaint();
	}

	JPopupMenu getRightButtonMenu(ActionListener listener, boolean use_alternative) {
		return new FastMenu(listener, controller);
	}

	void doSpecificAction(MediaFormat format, ActionEvent a, Thumbnail source) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.MENU_ADDTOCOLLECT)) {
			collectionpanel.add(format);
		} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
			AlbumSelectionDialog asd = albumpanel.getSelectionDialog();
			asd.setTitle(Resources.TITLE_SELECT_ALBUM + ":" + format);
			asd.setVisible(true);
			TreePath[] tps = asd.getSelectedAlbums();
			if (tps != null) {
				albumpanel.addToAlbum(new MediaFormat[] {  format }, tps, false);
				IniPrefs s = controller.getPrefs();
				if (IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.MOVETOFOLDER), 0) == 1
						&& IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.USEALBUMFOLDER), 0) == 1)
					;
			}
		} else if (cmd.equals(Resources.MENU_SHOW)) {
			showFullImage(format, source);
		} else if (cmd.equals(Resources.MENU_RENAME)) {
			IniPrefs s = controller.getPrefs();
			boolean success = false;
			Object[] masks = RenameOptionsTab.getRenameMask(s);
			boolean askEdit = masks.length == 0 || (masks.length == 1 && masks[0].toString().length() == 0)
					|| masks.length > 1 || RenameOptionsTab.askForEditMask(s);
			if (format.isValid() && askEdit == false) {
				success = format.renameTo(new File(format.getFile().getParent(), FileNameFormat
						.makeValidPathName(new FileNameFormat(masks[0].toString(), true).format(format))));
			} else {
				JComboBox values = new JComboBox(masks);
				values.setEditable(true);
				JPanel p = new JPanel();
				p.setLayout(new BorderLayout());
				p.add(new JLabel(Resources.LABEL_NEW_NAME), "Center");
				p.add(values, "South");
				if (JOptionPane.showOptionDialog(this, p, Resources.TITLE_RENAME + ' ' + format.getFile(),
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
					String value = values.getSelectedItem().toString();
					String dest = value.startsWith("./") || value.startsWith(".\\") ? format.getFile().getParent()
							: RenameOptionsTab.getDestPath(s);
					if (new File(value).isAbsolute())
						success = format.renameTo(new File(value));
					else
						success = format.renameTo(new File(dest, (format.isValid() ? FileNameFormat
								.makeValidPathName(new FileNameFormat(value, true).format(format)) : value)));
				}
			}
			if (success)
				source.update();
		} else if (cmd.equals(Resources.MENU_DELETE)) {
			IniPrefs s = controller.getPrefs();
			if (IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.SHOWWARNDLG), 0) == 1
					&& JOptionPane.showConfirmDialog(this, Resources.LABEL_COFIRMDELETE + format.getFile(),
							Resources.TITLE_COFIRMATION, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;
			if (format.getFile().delete()) {
				remove((Component) source);
				adjustDimension();
			}
		} else if (cmd.equals(Resources.MENU_PRINT)) {
			controller.print(new Object[] { format });
		}
	}

	public String getImageTitle(MediaFormat format, boolean thumbnail) {
		String o = null;
		boolean showComment = IniPrefs.getInt(controller.getPrefs().getProperty(ThumbnailsOptionsTab.SECNAME,
				ThumbnailsOptionsTab.SHOWCOMMENT), 0) == 1;
		MediaInfo info = format.getMediaInfo();
		if (showComment)
			try {
				o = info.getAttribute(MediaInfo.COMMENTS) != null ? info.getAttribute(MediaInfo.COMMENTS).toString()
						: null;
			} catch (IllegalArgumentException iae) {
				System.err.println("" + format + ' ' + iae);
			}
		if (o == null || o.length() == 0)
			o = (String) controller.getPrefs()
					.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.LABELMASK);
		if (thumbnail) {
			if (o != null)
				try {
					return new FileNameFormat(o).format(format);
				} catch (IllegalArgumentException iae) {
					System.err.println("" + format + ' ' + iae);
				}
			else if (info != null)
				try {
					return info.getAttribute(MediaInfo.DATETIMEORIGINAL).toString();
				} catch (IllegalArgumentException iae) {
					return info.toString();
				}
		} else {
			if (info != null)
				return Resources.LABEL_NOTUMBNAIL + info.getAttribute(MediaInfo.ESS_MAKE);
		}
		return Resources.LABEL_NOTUMBNAIL + format.getName();
	}

	void showFullImage(MediaFormat format, Thumbnail source) {
		// TODO: avoid that
		if (format != null && format.isValid())
			if ((format.getType() & MediaFormat.STILL) > 0)
				imagepanel.updateView(format);
			else
				try {
					controller.playMedia(format, 0);
				} catch (IOException ioe) {
					System.err.println("" + format + ' ' + ioe);
				}
		controller.updateCaption(format.getName());
		setCurrent(source);
	}

	public void setCurrent(Thumbnail tn) {
		current_thumb = tn;
	}

	public boolean showNext() {
		Thumbnail tn;
		for (int i = 0; i < getComponentCount() - 1; i++) {
			if (current_thumb == getComponent(i) || current_thumb == null) {
				tn = (Thumbnail) getComponent(i + 1);
				doSpecificAction(tn.getFormat(), new ActionEvent(tn, 0, Resources.MENU_SHOW), tn);
				current_thumb = tn;
				return true;
			}
		}
		return false;
	}

	public boolean showPrev() {
		Thumbnail tn;
		for (int i = getComponentCount() - 1; i > 0; i--) {
			if (current_thumb == getComponent(i) || current_thumb == null) {
				tn = (Thumbnail) getComponent(i - 1);
				doSpecificAction(tn.getFormat(), new ActionEvent(tn, 0, Resources.MENU_SHOW), tn);
				current_thumb = tn;
				return true;
			}
		}
		return false;
	}

	protected boolean isLowMemory(boolean notify) {
		// TODO: better showing notification get from settings
		if (controller.isLowMemory()) {
			if (notify)
				JOptionPane.showMessageDialog(this, Resources.LABEL_OUTOFMEMORY, Resources.TITLE_ERROR,
						JOptionPane.ERROR_MESSAGE);
			return true;
		}
		return false;
	}

	class ThumbnailComponent extends JLabel implements ActionListener, Thumbnail {
		Dimension preferredsize = new Dimension(ThumbnailsPanel.this.sx, ThumbnailsPanel.this.sy);

		Dimension thumbnailSize = new Dimension(preferredsize.width - 2 * 2, preferredsize.height - 2 * 2
				- labelTextHeight); // text height

		private MediaFormat format;

		public ThumbnailComponent() {
			super(new ImageIcon());
			// setHorizontalAlignment(SwingConstants.CENTER)
			setHorizontalTextPosition(SwingConstants.CENTER);
			setVerticalTextPosition(SwingConstants.BOTTOM);
			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					int m = e.getModifiers();
					if (e.getClickCount() == 2 && (m & InputEvent.BUTTON1_MASK) > 0) {
						// show full image
						doSpecificAction(format, new ActionEvent(ThumbnailComponent.this, 0, Resources.MENU_SHOW),
								ThumbnailComponent.this);
					} else if ((m & InputEvent.BUTTON3_MASK) > 0)
						getRightButtonMenu(ThumbnailComponent.this, e.isControlDown()).show(
								ThumbnailComponent.this,
								e.getX(),
								controller.adjustMenuY(ThumbnailComponent.this.getLocationOnScreen().y + e.getY(), 250)
										- ThumbnailComponent.this.getLocationOnScreen().y);
				}
			});
			setBorder(reg_border);
		}

		public Dimension getPreferredSize() {
			return preferredsize;
		}

		public void actionPerformed(ActionEvent a) {
			String cmd = a.getActionCommand();
			if (cmd.equals(Resources.MENU_PROPERTIES)) {
				PropertiesPanel.showProperties(format, controller);
			} else if (cmd.equals(Resources.MENU_EDIT_PROPS)) {
				Id3TagEditor.editTag(controller, format);
			} else if (cmd.equals(Resources.MENU_ADDTO_IPOD)) {
				IpodPane ipodPanel = (IpodPane) controller.component(controller.COMP_IPODPANEL);
				ipodPanel.add(new MediaFormat[] { format }, null, ipodPanel.INVALIDATE_ALL);
			} else if (cmd.equals(Resources.MENU_COPY_LOCATION)) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(format.getFile().getPath()), null);
			} else
				doSpecificAction(format, a, this);
		}

		public ThumbnailComponent(MediaFormat format) {
			this();
			updateImage(format);
		}

		void updateImage(MediaFormat format) {
			this.format = format;
			String o = DataConv.arrayToString(controller.getPrefs().getProperty(ThumbnailsOptionsTab.SECNAME,
					ThumbnailsOptionsTab.TOOLTIPMASK), ',');
			Icon ico = format.getThumbnail(thumbnailSize);
			if (o != null) {
				setToolTipText(new FileNameFormat(o).format(format));
			} else
				setToolTipText(format.getMediaInfo().toString());
			if (ico != null) {
				setIcon(ico);
				setText(getImageTitle(format, true));
			} else
				setText(getImageTitle(format, false));
		}

		public void paint(Graphics g) {
			try {
				super.paint(g);
			} catch (NullPointerException e) {
				// work around Sun's VM bug
			}
		}

		public MediaFormat getFormat() {
			return format;
		}

		public void select(boolean on) {
			setBorder(on ? selected_border : reg_border);
		}

		public void update() {
			updateImage(format);
		}

		public String toString() {
			return format.getName();
		}
	}

	Dimension preferredsize = new Dimension(0, 160);

	Controller controller;

	private Thumbnail current_thumb;

	protected PhotoCollectionPanel collectionpanel;

	protected AlbumPane albumpanel;

	protected PhotoImagePanel imagepanel;

	protected int time; // time of playing

	protected long length; // length of content in bytes

	private int v, h;

	private int sx, sy; // cell size

	private Border reg_border, selected_border;
}
