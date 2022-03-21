/* MediaChest - PlayListPane
 * Copyright (C) 1999-2005 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: PlayListPane.java,v 1.92 2013/04/19 02:08:36 cvs Exp $
 */
package photoorganizer.renderer;

import static mediautil.gen.MediaInfo.ALBUM;
import static mediautil.gen.MediaInfo.ARTIST;
import static mediautil.gen.MediaInfo.BITRATE;
import static mediautil.gen.MediaInfo.BPM;
import static mediautil.gen.MediaInfo.COMMENTS;
import static mediautil.gen.MediaInfo.COMPOSER;
import static mediautil.gen.MediaInfo.DATETIMEORIGINAL;
import static mediautil.gen.MediaInfo.FILESIZE;
import static mediautil.gen.MediaInfo.FILETYPE;
import static mediautil.gen.MediaInfo.GENRE;
import static mediautil.gen.MediaInfo.LASTMODIFIED;
import static mediautil.gen.MediaInfo.LASTPLAY;
import static mediautil.gen.MediaInfo.LENGTH;
import static mediautil.gen.MediaInfo.PARTOFSET;
import static mediautil.gen.MediaInfo.PLAYCOUNTER;
import static mediautil.gen.MediaInfo.RATING;
import static mediautil.gen.MediaInfo.SAMPLERATE;
import static mediautil.gen.MediaInfo.TITLE;
import static mediautil.gen.MediaInfo.TRACK;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.tree.TreePath;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;

import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;
import org.aldan3.xml.XmlHelper;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.ipod.ArtworkDB;
import photoorganizer.ipod.BaseItem;
import photoorganizer.ipod.ITunesDB;
import photoorganizer.ipod.IpodControl;
import photoorganizer.ipod.PlayItem;
import photoorganizer.ipod.PlayList;
import photoorganizer.media.PlaybackRequest;

public class PlayListPane extends JTable implements ActionListener, Persistancable {
	final static String SECNAME = "PlayListPane";

	final static String COLWIDTH = "ColumnWidthes";

	static final int TOO_MANY_WINDOWS = 10;

	protected Controller controller;

	IpodPane ipodPane; // for friends

	protected AlbumPane albumpane;

	protected StatusBar statusbar;

	protected PlayList playList;

	public PlayListPane(Controller controller) {
		this.controller = controller;
		statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		((IpodPane) controller.component(Controller.COMP_IPODPANEL)).setPlayListPanel(this);
		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				int m = e.getModifiers();
				new Point(e.getX(), e.getY());
				if ((m & InputEvent.BUTTON3_MASK) > 0)
					getRMouseMenu().show(PlayListPane.this, e.getX(), e.getY());
				else if (e.getClickCount() == 2) {
					actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			}
		});
		setModel(new PlayListModel(controller));
		setMinimumSize(Resources.MIN_PANEL_DIMENSION);
		setFillsViewportHeight(true);
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int f = e.getFirstIndex();
				int l = e.getLastIndex();
				if (f > l) {
					l = e.getFirstIndex();
					f = e.getLastIndex();
				}
				updateState();
			}
		});
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					actionPerformed(new ActionEvent(this, 3, Resources.MENU_DELETE));
				}
			}
		});
		setTransferHandler(TransferHelper.createTrasnsferHandler(this, controller));
		setDragEnabled(true);
		MiscellaneousOptionsTab.applyFontSettings(this, controller);
	}

	// TODO: inherit from generic table view with column width persisten
	// use getSectionName()
	public void save() {
		controller.saveTableColumns(this, SECNAME, COLWIDTH);
	}

	public void load() {
		controller.loadTableColumns(this, SECNAME, COLWIDTH);
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		PlayListModel model = (PlayListModel) getModel();
		final int[] selections = (int[]) getSelectedRows().clone();
		if (cmd.equals(Resources.MENU_DELETE) || cmd.equals(Resources.MENU_DELETE_INLIST)) {
			java.util.List deletedItems = new ArrayList(selections.length);
			int mir = 99999, mar = 0;
			for (int i = 0; i < selections.length; i++) {
				deletedItems.add(model.getElementAt(selections[i]));
				if (selections[i] < mir)
					mir = selections[i];
				if (selections[i] > mar)
					mar = selections[i];
			}
			PlayItem[] removingItems = (PlayItem[]) deletedItems.toArray(new PlayItem[deletedItems.size()]);
			ipodPane.remove(removingItems, playList, IpodPane.INVALIDATE_NONE);
			if (playList.isVirtual() == false && playList.isFileDirectory() == false
					&& cmd.equals(Resources.MENU_DELETE)) {
				ipodPane.remove(removingItems, (PlayList) ipodPane.getModel()
						.getChild(ipodPane.getModel().getRoot(), 0), IpodPane.INVALIDATE_NONE);
			}
			tableChanged(new TableModelEvent(getModel(), mir, mar, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		} else if (cmd.equals(Resources.MENU_SHOW)) {
			IniPrefs s = controller.getPrefs();
			String dev = IpodOptionsTab.getDevice(controller);
			if (selections.length > 1) {
				File[] medias = new File[selections.length];
				for (int i = 0; i < selections.length; i++) {
					medias[i] = ((PlayItem) model.getElementAt(selections[i])).getFile(dev);
				}
				new PlaybackRequest(medias, s).playList(controller);
			} else if (selections.length == 1) {
				new PlaybackRequest((ListModel) getModel(), s).playList(selections[0], controller);
			}
		} else if (cmd.equals(Resources.MENU_REFRESH)) {
			if (playList != null) {
				if (playList.smart != null) {
					controller.setWaitCursor(getTopLevelAncestor(), true);
					playList.clear();
					((ITunesDB) ipodPane.getModel()).clearUpdateCache();
					((ITunesDB) ipodPane.getModel()).updateMagicList(playList);
					controller.setWaitCursor(getTopLevelAncestor(), false);
				} else
					Collections.sort(playList);
				tableChanged(new TableModelEvent(getModel()));
				//tableChanged(new TableModelEvent(getModel(), 0, 99999, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE)); 
			}
		} else if (cmd.equals(Resources.MENU_MAKEITALL)) {
			if (MediaInfoInputPanel.doMediaInfoInput(controller, playList, selections, null) != null)
				// TODO: change only selected range
				tableChanged(new TableModelEvent(getModel()));
		} else if (cmd.equals(Resources.MENU_PROPERTIES) || cmd.equals(Resources.MENU_EDIT_PROPS)) {
			String dev = IpodOptionsTab.getDevice(controller);
			MediaFormat mediaFormat = null;
			java.util.List medias = new ArrayList(selections.length);
			Map attachMap = new HashMap(selections.length);
			for (int i = 0; i < selections.length; i++) {
				PlayItem pi = (PlayItem) model.getElementAt(selections[i]);
				if (pi.getAttachedFormat() != null)
					mediaFormat = pi.getAttachedFormat();
				else {
					File mediaFile = pi.getFile(dev);
					mediaFormat = MediaFormatFactory.createMediaFormat(mediaFile);
				}
				if (mediaFormat != null)
					if (cmd.equals(Resources.MENU_EDIT_PROPS)) {
						medias.add(mediaFormat);
						attachMap.put(pi, mediaFormat);
					} else {
						PropertiesPanel.showProperties(mediaFormat, controller);
						if (i >= TOO_MANY_WINDOWS)
							break;
					}
			}
			if (cmd.equals(Resources.MENU_EDIT_PROPS)
					&& Id3TagEditor.editTag(controller, (MediaFormat[]) medias.toArray(new MediaFormat[medias.size()])) > 0) {
				for (int i = 0; i < selections.length; i++) {
					PlayItem pi = (PlayItem) model.getElementAt(selections[i]);
					PlayItem.update(pi, (MediaFormat) attachMap.get(pi), null, false);
					pi.resetState(BaseItem.STATE_METASYNCED);
					tableChanged(new TableModelEvent(getModel(), selections[i]));
				}
			}
		} else if (cmd.equals(Resources.MENU_ADDTOCOLLECT)) {
			if (selections == null || selections.length == 0)
				return;
			IniPrefs s = controller.getPrefs();
			String dev = IpodOptionsTab.getDevice(controller);
			File[] files = new File[selections.length];
			for (int i = 0; i < selections.length; i++) {
				PlayItem pi = (PlayItem) model.getElementAt(selections[i]);
				files[i] = pi.getFile(dev);
			}
			PhotoCollectionPanel collectionpanel = (PhotoCollectionPanel) controller
					.component(Controller.COMP_COLLECTION);
			collectionpanel.add(new PlaybackRequest(files, s));
		} else if (cmd.equals(Resources.MENU_TOPLAYLIST)) {
			String playList = null;
			//if (ipodPane.getSelectionPath() != null)
			//	playList = (String)ipodPane.getSelectionPath().getLastPathComponent();

			if (playList == null) {
				List lists = (List) ((ArrayList) ((ITunesDB) ipodPane.getModel()).getPlayLists()).clone();
				Iterator li = lists.iterator();
				while (li.hasNext()) {
					PlayList pl = ((ITunesDB) ipodPane.getModel()).getPlayList((String) li.next());
					if (pl.smart != null)
						li.remove();
				}
				playList = (String) JOptionPane.showInputDialog(this, Resources.LABEL_INPUTPLAYLIST,
						Resources.TITLE_PLAYLIST, JOptionPane.QUESTION_MESSAGE, null, lists.toArray(), null);
			}
			if (playList == null)
				return;
			PlayItem[] playItems = new PlayItem[selections.length];
			for (int i = 0; i < selections.length; i++) {
				playItems[i] = (PlayItem) model.getElementAt(selections[i]);
			}
			ipodPane.add(playItems, ipodPane.getPlayList(playList), IpodPane.INVALIDATE_NONE);
		} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
			AlbumPane albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
			AlbumSelectionDialog asd = albumpane.getSelectionDialog();
			asd.setTitle(Resources.TITLE_SELECT_ALBUM);
			asd.setVisible(true);
			TreePath[] tps = asd.getSelectedAlbums();
			if (tps != null) {
				IniPrefs s = controller.getPrefs();
				String dev = IpodOptionsTab.getDevice(controller);
				MediaFormat[] medias = new MediaFormat[selections.length];
				for (int i = 0; i < selections.length; i++) {
					PlayItem pi = (PlayItem) model.getElementAt(selections[i]);
					medias[i] = pi.getAttachedFormat();
					if (medias[i] == null)
						medias[i] = MediaFormatFactory.createMediaFormat(pi.getFile(dev));
				}
				albumpane.addToAlbum(medias, tps, true);
			}
		} else if (cmd.equals(Resources.MENU_ENCODING)) {
			// TODO: do encoding on particular fields
			Set charsets = new TreeSet(Charset.availableCharsets().keySet());
			charsets.add("Cp866");
			Object encoding = JOptionPane.showInputDialog(this, Resources.LABEL_PAGE_ENCODING,
					Resources.TITLE_ENCODING, JOptionPane.INFORMATION_MESSAGE, null, charsets.toArray(), null);
			if (encoding != null) {
				encoding = encoding.toString();
				for (int i = 0; i < selections.length; i++) {
					PlayItem pi = (PlayItem) model.getElementAt(selections[i]);
					MediaFormat format = MediaFormatFactory.createMediaFormat(new File(pi.get(PlayItem.FILENAME).toString()),
							(String) encoding, true);
					// TODO: can work not right if item is already on iPod 
					PlayItem.update(pi, format, IpodOptionsTab.parsePath((String) pi.get(PlayItem.FILENAME),
							(String) encoding, controller), IpodOptionsTab.isOverrideTag(controller));
				}
				// TODO: change only selected range
				tableChanged(new TableModelEvent(getModel()));
			}
		} else if (cmd.equals(Resources.MENU_UNDO)) {
			// TODO: to implement
			JOptionPane.showMessageDialog(this, "Undo not implemented yet", Resources.TITLE_ERROR,
					JOptionPane.INFORMATION_MESSAGE);
		} else if (cmd.equals(Resources.MENU_FIND)) {
			Descriptor d = MediaInfoInputPanel.doMediaInfoInput(controller, null, null, Resources.TITLE_FIND);
			if (d == null)
				return;
			int lastFound = -1;
			Pattern p = Pattern.compile(d.name);
			clearSelection();
			//if (playList != null)			
			for (int i = 0; i < playList.size(); i++) {
				PlayItem pi = (PlayItem) playList.get(i);
				Object o = pi.get(d.selector);
				if (o != null && p.matcher(o.toString()).matches()) {
					lastFound = i;
					addRowSelectionInterval(i, i);
				}
			}
			if (lastFound > 0)
				scrollRectToVisible(getCellRect(lastFound, 1, true));
		} else if (cmd.equals(Resources.MENU_SELECTALL)) {
			selectAll();
		} else if (cmd.equals(Resources.MENU_COPY_MOVE)) {
			// TODO: can't move from iPod without changing database, so maybe add that?
			IniPrefs s = controller.getPrefs();
			String dev = IpodOptionsTab.getDevice(controller);
			if (selections.length > 1) {
				String targetPath = (String) s.getProperty(IpodPane.SECNAME, IpodPane.MEDIA_LIB_DIR);
				targetPath = new JDirectoryChooser(this, targetPath, null).getDirectory();
				if (targetPath != null) {
					s.setProperty(IpodPane.SECNAME, IpodPane.MEDIA_LIB_DIR, targetPath);
					// TODO: create final copy list and run copy thread
					List<File[]> copyList = new ArrayList<File[]>(selections.length);
					for (int i = 0; i < selections.length; i++) {
						PlayItem pi = (PlayItem) model.getElementAt(selections[i]);
						File srcFile = pi.getFile(dev);
						copyList
								.add(new File[] {
										srcFile,
										new File(targetPath, IpodOptionsTab.buildPath(pi, File.separatorChar, null,
												controller)) });
					}
					BatchActionWithProgress.doLongTimeOperation(controller.mediachest, copyList,
							new Controller.CopierWithProgress(true));
				}
			} else {
				//System.err.println("FIlechooser");
				String mask = (String) s.getProperty(IpodOptionsTab.SECNAME, IpodOptionsTab.FILE_PARSE_ORDER);
				JFileChooser fc = new JFileChooser();
				//fc.setDialogTitle(Resources.TITLE_SELECT_FILENAME);
				fc.setDialogType(JFileChooser.SAVE_DIALOG);
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.showSaveDialog(this);
				File targetPath = fc.getSelectedFile();
				if (targetPath == null)
					return;
				PlayItem pi = (PlayItem) model.getElementAt(selections[0]);
				File srcFile = pi.getFile(dev);
				String ext = Controller.getFileExtension(srcFile);
				List<File[]> copyList = new ArrayList<File[]>(selections.length);
				if (targetPath.isDirectory()) {
					copyList.add(new File[] {
							srcFile,
							new File(targetPath, FileNameFormat.makeValidPathName(new FileNameFormat(mask, true)
									.format(pi))
									+ ext) });
				} else
					copyList.add(new File[] { srcFile, targetPath });
				BatchActionWithProgress.doLongTimeOperation(controller.mediachest, copyList,
						new Controller.CopierWithProgress());
			}
		} else if (cmd.equals(Resources.MENU_COPY_LOCATION)) {
			if (selections.length >= 1) {
				PlayItem pi = (PlayItem) model.getElementAt(selections[0]);
				String dev = IpodOptionsTab.getDevice(controller);
				//System.err.printf("Copy location of %d on %s%n", selections.length, dev);
				String cc = pi.getFile(dev).getPath();
				for (int i = 1; i < selections.length; i++) {
					pi = (PlayItem) model.getElementAt(selections[i]);
					cc += File.separator + pi.getFile(dev).getPath();
				}
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(cc), null);
			}
		} else if (cmd.equals(Resources.MENU_EXPORTTOXML) || cmd.equals(Resources.MENU_EXPORTTOCSV)) {
			if (playList != null) {
				a.setSource(null); // consumed
				Writer w = null;
				JFileChooser fc = cmd.equals(Resources.MENU_EXPORTTOXML) ? new Controller.ExtFileChooser(true,
						Resources.LABEL_PLAYLIST + "- " + playList, null, null) : new Controller.ExtFileChooser(true,
						Resources.LABEL_PLAYLIST + "- " + playList, Resources.EXT_CSV, Resources.LABEL_CSV_FILE);
				if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					if (false == fc.getSelectedFile().exists()
							|| JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, fc.getSelectedFile()
									.getName()
									+ Resources.LABEL_CONFIRM_OVERWRITE, Resources.TITLE_OVERWRITE,
									JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
						try {
							controller.setWaitCursor(getTopLevelAncestor(), true);
							w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), Resources.ENC_UTF_8);
							if (cmd.equals(Resources.MENU_EXPORTTOXML)) {
								w.write(XmlHelper.getXmlHeader(Resources.ENC_UTF_8));
								w.write("\r\n");
								w.write(playList.toXmlString());
							} else
								w.write(playList.toCsvString());
							w.flush();
							w.close();
							w = null;
							//System.err.println("XML:"+((XmlExposable)getModel()).toXmlString());
						} catch (IOException ioe) {
							System.err.println("Can not write XML/CSV coded playlist to :" + fc.getSelectedFile() + ' '
									+ ioe);
						} finally {
							if (w != null)
								try {
									w.close();
								} catch (IOException ioe) {
								}
							controller.setWaitCursor(getTopLevelAncestor(), false);
						}
				}
			}
		}
		updateState();
	}

	public void remove(PlayItem pi) {
		if (playList != null)
			playList.remove(pi);
	}

	public void updateList(Object list, IpodPane ipodPane) {
		this.ipodPane = ipodPane;
		//new Exception("On "+new Date() + " update pl:"+list).printStackTrace();
		if (playList != null)
			playList.setViewPosition(((JViewport) getParent()).getViewPosition());
		if (list instanceof PlayList) {
			playList = (PlayList) list;
			if (playList.getViewPosition() != null)
				((JViewport) getParent()).setViewPosition(playList.getViewPosition());
		} else
			playList = null;
		if (playList != null)
			statusbar.displayMetric(String.valueOf(playList.size()) + "/ "
					+ MP3.convertTime(playList.getLength() / 1000));

		tableChanged(new TableModelEvent(getModel(), 0, 99999, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		tableChanged(new TableModelEvent(getModel()));//, TableModelEvent.HEADER_ROW));
		updateState();
	}

	protected void updateState() {
		boolean were_sel = getSelectedRowCount() > 0;
		controller.getUiUpdater().notify(were_sel, UiUpdater.SELECTION_SELECTED);
		controller.getUiUpdater().notify(were_sel, UiUpdater.IS_SELECTION);
		if (IniPrefs.getInt(controller.getPrefs().getProperty(IpodOptionsTab.SECNAME, IpodOptionsTab.ARTWORK_PREVIEW),
				0) == 1) {
			if (were_sel) {
				PlayItem pi = (PlayItem) ((PlayListModel) getModel()).getElementAt(getSelectedRow());
				assert pi != null;
				ArtworkDB.ImageItem ii = pi.getImage();
				if (ii != null)
					setPreviewIcon(ArtworkDB.getImage(true, pi, IpodOptionsTab.getDevice(controller)));
				else
					setPreviewIcon(null);
			} else
				setPreviewIcon(null);
		}
	}

	protected void setPreviewIcon(Icon i) {
		try {
			((JLabel) ((JPanel) ((JSplitPane) getParent(/*JScrollPane*/).getParent().getParent(/*JSplitPane*/))
					.getLeftComponent()).getComponent(1)).setIcon(i);
		} catch (ArrayIndexOutOfBoundsException aio) {
		}
	}

	protected JPopupMenu getRMouseMenu() {
		JPopupMenu result = new JPopupMenu();
		if (IpodControl.getIpodControl(controller).isSyncing())
			return result;

		Action a;
		result.add(a = new AbstractAction(Resources.MENU_SHOW) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.addSeparator();
		//if (controller.getUiUpdater().isEnabled(UiUpdater.PLAYLIST_EXISTS)) {
		result.add(a = new AbstractAction(Resources.MENU_TOPLAYLIST) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.PLAYLIST_EXISTS);
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		//}
		result.add(a = new AbstractAction(Resources.MENU_ADDTOCOLLECT) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);

		result.add(a = new AbstractAction(Resources.MENU_ADDTOALBUM) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.add(a = new AbstractAction(Resources.MENU_COPY_MOVE) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.add(a = new AbstractAction(Resources.MENU_COPY_LOCATION) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_MAKEITALL) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.add(a = new AbstractAction(Resources.MENU_ENCODING) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.add(a = new AbstractAction(Resources.MENU_UNDO) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.UNDO);
		result.addSeparator();
		result.add(new AbstractAction(Resources.MENU_FIND) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		result.add(new AbstractAction(Resources.MENU_REFRESH) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_SELECTALL) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_PROPERTIES) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		result.add(a = new AbstractAction(Resources.MENU_EDIT_PROPS) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		if (playList == null)
			return result;
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_DELETE) {
			public void actionPerformed(ActionEvent a) {
				PlayListPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		if (playList.isVirtual() == false && playList.isFileDirectory() == false) {
			result.add(a = new AbstractAction(Resources.MENU_DELETE_INLIST) {
				public void actionPerformed(ActionEvent a) {
					PlayListPane.this.actionPerformed(a);
				}
			});
			controller.getUiUpdater().addForNotification(a, UiUpdater.IS_SELECTION);
		}
		return result;
	}

	class PlayListModel extends BaseConfigurableTableModel implements ListModel {

		PlayListModel(Controller controller) {
			updateView(controller);
		}

		protected int getDescriptionIndex() {
			// index of configurable list of columns 
			return AppearanceOptionsTab.IPOD_VIEW;
		}

		public int getRowCount() {
			return playList == null ? 0 : playList.size();
		}

		public boolean isCellEditable(int row, int column) {
			// TODO: reconsider the limitation
			ColumnDescriptor descriptor = colsDescriptor[column];
			return inAttributes(descriptor.attributes, MediaInfo.TITLE) || inAttributes(descriptor.attributes, LENGTH);
		}

		public Object getValueAt(int row, int column) {
			if (playList == null)
				return null;
			ColumnDescriptor descriptor = colsDescriptor[column];
			PlayItem pi = (PlayItem) playList.get(row);
			if (pi == null)
				return null;
			String attrs[] = descriptor.attributes;
			// TODO: consider more effective solution to map, for example DisplayMap [] dm = new DisplayMap[{new DisplayItem(ALBUM, PlayItem.ALBUM, null), ...];
			if (inAttributes(attrs, ALBUM))
				return pi.get(PlayItem.ALBUM);
			else if (inAttributes(attrs, TITLE)) {
				return pi.get(PlayItem.TITLE);
			} else if (inAttributes(attrs, ARTIST))
				return pi.get(PlayItem.ARTIST);
			else if (inAttributes(attrs, GENRE))
				return pi.get(PlayItem.GENRE);
			else if (inAttributes(attrs, LENGTH)) {
				Integer L = (Integer) pi.get(PlayItem.LENGTH);
				return MP3.convertTime(L.intValue() / 1000);
			} else if (inAttributes(attrs, COMPOSER))
				return pi.get(PlayItem.COMPOSER);
			else if (inAttributes(attrs, FILETYPE))
				return pi.get(PlayItem.FILETYPE);
			else if (inAttributes(attrs, FILESIZE))
				return BasicIo.convertLength(((Integer) pi.get(PlayItem.SIZE)).intValue());
			else if (inAttributes(attrs, COMMENTS))
				return pi.get(PlayItem.COMMENT);
			else if (inAttributes(attrs, BITRATE))
				return pi.get(PlayItem.BITRATE);
			else if (inAttributes(attrs, TRACK))
				return pi.get(PlayItem.ORDER);
			else if (inAttributes(attrs, PLAYCOUNTER))
				return pi.get(PlayItem.PLAYED_TIMES);
			else if (inAttributes(attrs, RATING))
				return pi.get(PlayItem.RATING);
			else if (inAttributes(attrs, LASTPLAY))
				return pi.get(PlayItem.LAST_TIME);
			else if (inAttributes(attrs, LASTMODIFIED))
				return pi.get(PlayItem.MODIFIED_TIME);
			else if (inAttributes(attrs, DATETIMEORIGINAL))
				return pi.get(PlayItem.CREATE_TIME);
			else if (inAttributes(attrs, SAMPLERATE))
				return pi.get(PlayItem.SAMPLE_RATE);
			else if (inAttributes(attrs, BPM))
				return pi.get(PlayItem.BPM);
			else if (inAttributes(attrs, PARTOFSET))
				return "" + pi.get(PlayItem.DISK) + '/' + pi.get(PlayItem.NUM_DISKS);
			else if (inAttributes(attrs, MediaInfo.SKIPCOUNTER))
				return pi.get(PlayItem.SKIPPED_TIMES);
			else if (inAttributes(attrs, MediaInfo.LASTSKIPPED))
				return pi.get(PlayItem.LAST_SKIPPED_TIME);
			// video related
			else if (inAttributes(attrs, MediaInfo.SHOW))
				return pi.get(PlayItem.SHOW);
			else if (inAttributes(attrs, MediaInfo.SEASON_NUM))
				return pi.get(PlayItem.SEASON_NUM);
			else if (inAttributes(attrs, MediaInfo.EPISODE_ID))
				return pi.get(PlayItem.EPIZODE);
			else if (inAttributes(attrs, MediaInfo.EPISODE_NUM))
				return pi.get(PlayItem.EPIZODE_NUM);
			else if (inAttributes(attrs, MediaInfo.YEAR))
				return pi.get(PlayItem.YEAR);
			///////--->>>> Insert processing new columns here

			System.err.println("Not suitable attributes for music " + DataConv.arrayToString(attrs, ';'));
			return null;
		}

		public void setValueAt(Object aValue, int row, int column) {
			PlayItem pi = (PlayItem) playList.get(row);
			if (pi == null)
				return;
			ColumnDescriptor descriptor = colsDescriptor[column];
			String attrs[] = descriptor.attributes;
			if (inAttributes(attrs, TITLE)) {
				pi.set(PlayItem.TITLE, aValue.toString());
			} else if (inAttributes(attrs, LENGTH)) {
				StringTokenizer st = new StringTokenizer(aValue.toString(), ":");
				int len = 0;
				try {
					while (st.hasMoreTokens()) {
						len = len * 60 + Integer.parseInt(st.nextToken());
					}
					pi.set(PlayItem.LENGTH, len * 1000);
				} catch (Exception e) {
					System.err.println("setValueAt (Length) " + e);
					return;
				}
			} else
				return;
			pi.resetState(BaseItem.STATE_METASYNCED);
		}

		public Object getElementAt(int index) {
			try {
				return playList.get(index);
			} catch (ArrayIndexOutOfBoundsException e) {
				return null;
			}
		}

		public void setElementAt(int index, Object element) {
			try {
				playList.set(index, (PlayItem) element);
			} catch (ArrayIndexOutOfBoundsException e) {
			}
		}

		void removeElementAt(int row) {
			playList.remove(row);
		}

		//protected Class getFirstColumnClass() {
		//	return ColoredString.class;
		//}

		public void fireTableRowsUpdated(int firstRow, int lastRow) {
			super.fireTableRowsUpdated(firstRow, lastRow);
			for (int i = firstRow; i <= lastRow; i++) {
				playList.get(i);
			}
		}

		// list model
		public int getSize() {
			return getRowCount();
		}

		public void addListDataListener(ListDataListener l) {
		}

		public void removeListDataListener(ListDataListener l) {
		}

		protected boolean inAttributes(String[] attributes, String attribute) {
			if (attributes == null || attribute == null)
				return false;
			for (int i = 0; i < attributes.length; i++)
				if (attribute.equals(attributes[i]))
					return true;
			return false;
		}
	}

}