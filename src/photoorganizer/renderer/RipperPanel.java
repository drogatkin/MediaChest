/* MediaChest - $RCSfile: RipperPanel.java,v $
 * Copyright (C) 2001 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: RipperPanel.java,v 1.39 2013/05/02 04:43:45 cvs Exp $
 */

package photoorganizer.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.event.EventListenerList;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.SAXParserFactory;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.inet.HttpUtils;
import org.aldan3.xml.XmlExposable;
import org.aldan3.xml.XmlHelper;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import photoorganizer.Controller;
import photoorganizer.Persistancable;
import photoorganizer.PhotoOrganizer;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.album.AlbumModel;
import photoorganizer.album.MediaAccess;
import photoorganizer.directory.TreeDesktopModel;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.Thumbnail;
import photoorganizer.media.PlaybackRequest;

public/* final */class RipperPanel extends JSplitPane implements ActionListener, Persistancable {
	public static final String SECNAME = "RipperPanel";

	final static int TOO_MANY_WINDOWS = 20;

	protected Controller controller;

	protected DirectoryTable folderTable;

	protected AlbumTable albumTable;

	protected RipperTable ripperTable;

	protected CollectionTable collectionTable;

	protected SAXParserFactory saxParserFactory;

	public RipperPanel(Controller controller) {
		super(VERTICAL_SPLIT);
		this.controller = controller;
		setOneTouchExpandable(true);
		access = new MediaAccess(controller);
		JTabbedPane mediaSourceTabs = new JTabbedPane(SwingConstants.TOP);
		folderTable = new DirectoryTable(new FolderTableModel(controller));
		mediaSourceTabs.insertTab(Resources.TAB_DIRECTORY, (Icon) null, new JSplitPane(HORIZONTAL_SPLIT,
				new OneThirdScroll(new DirectoryTree(folderTable)), new JScrollPane(folderTable)),
				Resources.TTIP_DIRECTORYTAB, mediaSourceTabs.getTabCount());
		albumTable = new AlbumTable(new AlbumTableModel(controller));
		mediaSourceTabs.insertTab(Resources.TAB_ALBUM, (Icon) null, new JSplitPane(HORIZONTAL_SPLIT,
				new OneThirdScroll(new AlbumTree(albumTable)), new JScrollPane(albumTable)), Resources.TTIP_ALBUM,
				mediaSourceTabs.getTabCount());
		mediaSourceTabs.insertTab(Resources.TAB_COLLECTION, (Icon) null, new JScrollPane(
				collectionTable = new CollectionTable()), Resources.TTIP_COLLECTLIST, mediaSourceTabs.getTabCount());
		setTopComponent(mediaSourceTabs);
		ripperTable = new RipperTable();
		setBottomComponent(new JSplitPane(HORIZONTAL_SPLIT, new OneThirdScroll(ripper = new RipperTree(ripperTable)),
				new JScrollPane(ripperTable)));
		setDividerLocation(0.5);
		updateUiState(controller, null, true);
	}

	public void save() {
		controller.saveTableColumns(folderTable, SECNAME, folderTable.getClass().getName());
		controller.saveTableColumns(albumTable, SECNAME, albumTable.getClass().getName());
		controller.saveTableColumns(ripperTable, SECNAME, ripperTable.getClass().getName());
		controller.saveTableColumns(collectionTable, SECNAME, collectionTable.getClass().getName());
	}

	public void load() {
		controller.loadTableColumns(folderTable, SECNAME, folderTable.getClass().getName());
		controller.loadTableColumns(albumTable, SECNAME, albumTable.getClass().getName());
		controller.loadTableColumns(ripperTable, SECNAME, ripperTable.getClass().getName());
		controller.loadTableColumns(collectionTable, SECNAME, collectionTable.getClass().getName());
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		                  
		if (cmd.equals(Resources.MENU_RECORD_DISK)) {
			((RipperModel) ripper.getModel()).doDisk(controller);
		}
	}

	static void updateUiState(Controller controller, RipperModel model, boolean clean) {
		((StatusBar) controller.component(Controller.COMP_STATUSBAR)).displayMetric(clean ? "" : model.music ? MP3
				.convertTime(model.getTime()) : BasicIo.convertLength(model.getLength()));
		controller.getUiUpdater().notify(clean ? false : model.music ? model.getTime() > 0 : model.getLength() > 0,
				UiUpdater.RIPPER_NOT_EMPTY);
	}

	class DirectoryTree extends JTree implements ActionListener {
		DirectoryTable folderTable;

		DirectoryTree(DirectoryTable folderTable) {
			super(new TreeDesktopModel(IpodOptionsTab.getEncoding(controller)) /* FileSystemModel(controller) */);
			this.folderTable = folderTable;
			setCellRenderer(((TreeDesktopModel) getModel()).adoptCellRenderer(getCellRenderer()));
			setRootVisible(false);
			addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					((FolderTableModel) DirectoryTree.this.folderTable.getModel()).updateModel(((File) e.getPath()
							.getLastPathComponent()).listFiles());
					if (DirectoryTree.this.folderTable.getRowCount() > 0)
						DirectoryTree.this.folderTable.removeRowSelectionInterval(0, DirectoryTree.this.folderTable
								.getRowCount() - 1);
					DirectoryTree.this.folderTable.revalidate();
				}
			});
			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0)
						getRMouseMenu().show(DirectoryTree.this, e.getX(), e.getY());
				}
			});
			setDragEnabled(true);
		}

		public void actionPerformed(ActionEvent a) {
			String cmd = a.getActionCommand();
			if (cmd.equals(Resources.MENU_REFRESH)) {
				((TreeDesktopModel) getModel()).reset(null);
				revalidate();
			}
		}

		JPopupMenu getRMouseMenu() { // directory tree
			JPopupMenu result = new JPopupMenu();
			JMenuItem item;
			result.add(item = new JMenuItem(Resources.MENU_REFRESH));
			item.addActionListener(this);
			// result.addSeparator();
			// result.add(item = new JMenuItem(Resources.MENU_PROPERTIES));
			// item.addActionListener(this);
			return result;
		}
	}

	class DirectoryTable extends JTable implements ActionListener {
		DirectoryTable(FolderTableModel tableModel) {
			super(tableModel);
			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0)
						getRMouseMenu(DirectoryTable.this).show(DirectoryTable.this, e.getX(), e.getY());
					else if (e.getClickCount() == 2)
						actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			});
			addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == e.VK_DELETE) {
						actionPerformed(new ActionEvent(this, 3, Resources.MENU_DELETE));
					}
				}
			});
			setDragEnabled(true);
			setTransferHandler(new MediaTransferHandler());
		}

		public void actionPerformed(ActionEvent a) {
			String cmd = a.getActionCommand();
			int[] cols = getSelectedRows();

			FolderTableModel model = (FolderTableModel) getModel();
			if (cmd.equals(Resources.MENU_SHOW)) {
				// open directory if it is
				Object[] medias = new Object[cols.length];
				for (int i = 0; i < cols.length; i++) {
					medias[i] = model.getElementAt(cols[i]);
				}
				if (medias.length == 1 && medias[0] != null && medias[0] instanceof File
						&& ((File) medias[0]).isDirectory()) {
					model.updateModel(((File) medias[0]).listFiles());
					revalidate();
					// update tree also
				} else
					new PlaybackRequest(medias, controller.getPrefs()).playList(controller);
			} else if (cmd.equals(Resources.MENU_ADDTOCOLLECT)) {
				PhotoCollectionPanel collectionpanel = (PhotoCollectionPanel) controller
						.component(Controller.COMP_COLLECTION);
				File[] medias = new File[cols.length];
				for (int i = 0; i < cols.length; i++)
					medias[i] = model.getElementAt(cols[i]) instanceof File ? (File) model.getElementAt(cols[i])
							: ((MediaFormat) model.getElementAt(cols[i])).getFile();
				collectionpanel.add(medias);
			} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
				// TODO: there is some code duplication from others similar
				// places
				AlbumPane albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
				AlbumSelectionDialog asd = albumpane.getSelectionDialog();
				asd.setTitle(Resources.TITLE_SELECT_ALBUM + ":" + cols.length);
				asd.setVisible(true);
				TreePath[] tps = asd.getSelectedAlbums();
				if (tps != null) {
					MediaFormat[] medias = new MediaFormat[cols.length];
					for (int i = 0; i < cols.length; i++)
						medias[i] = model.getElementAt(cols[i]) instanceof MediaFormat ? (MediaFormat) model
								.getElementAt(cols[i]) :
						// TODO: need check for not null and valid?
								MediaFormatFactory.createMediaFormat((File) model.getElementAt(cols[i]));
					albumpane.addToAlbum(medias, tps, false);
				}
			} else if (cmd.equals(Resources.MENU_DELETE)) {
				Object media;
				boolean requestConfirm = true;
				Object[] options = cols.length > 1 ? new Object[] { Resources.CMD_YES, Resources.CMD_NO,
						Resources.CMD_YES_ALL, Resources.CMD_CANCEL } : new Object[] { Resources.CMD_YES,
						Resources.CMD_NO };
				for (int i = 0; i < cols.length; i++) {
					media = model.getElementAt(cols[i]);
					File file = null;
					if (media instanceof File)
						file = (File) media;
					else if (media instanceof MediaFormat)
						file = ((MediaFormat) media).getFile();
					if (requestConfirm) {
						int res = JOptionPane
								.showOptionDialog(this, file.getName() + Resources.LABEL_CONFIRM_DEL,
										Resources.TITLE_DELETE, 0, JOptionPane.WARNING_MESSAGE, null, options,
										Resources.CMD_NO);
						if (res == 1)
							continue;
						else if (res == 2)
							requestConfirm = false;
						else if (res == 3)
							break;
					}
					if (file != null && !file.delete())
						((StatusBar) controller.component(Controller.COMP_STATUSBAR)).flashInfo(
								"Cannot delete " + file, true);
				}
				revalidate();
			} else if (cmd.equals(Resources.MENU_PROPERTIES) || cmd.equals(Resources.MENU_EDIT_PROPS)) {
				doProperties(cols, model, cmd.equals(Resources.MENU_EDIT_PROPS));
			}
		}

	}

	JPopupMenu getRMouseMenu(ActionListener listener) {
		JPopupMenu result = new JPopupMenu();
		// TODO: add connection to UI updater to disable items if none selected
		JMenuItem item;
		result.add(item = new JMenuItem(Resources.MENU_SHOW));
		item.addActionListener(listener);
		result.addSeparator();
		result.add(item = new JMenuItem(Resources.MENU_ADDTOCOLLECT));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_ADDTOALBUM));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_ADDTO_IPOD));
		item.addActionListener(listener);
		result.addSeparator();
		result.add(item = new JMenuItem(Resources.MENU_PROPERTIES));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_EDIT_PROPS));
		item.addActionListener(listener);
		result.addSeparator();
		result.add(item = new JMenuItem(Resources.MENU_DELETE));
		item.addActionListener(listener);
		return result;
	}

	class AlbumTree extends JTree {
		AlbumTable albumTable;

		AlbumTree(AlbumTable albumTable) {
			this.albumTable = albumTable;
			setModel(new AlbumModel(access));
			getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					((AlbumTableModel) AlbumTree.this.albumTable.getModel()).updateModel(access.getAlbumContents(access
							.getAlbumId(e.getPath())));
					if (AlbumTree.this.albumTable.getRowCount() > 0)
						AlbumTree.this.albumTable.removeRowSelectionInterval(0,
								AlbumTree.this.albumTable.getRowCount() - 1);
					AlbumTree.this.albumTable.revalidate();
				}
			});
			setDragEnabled(true);
			setTransferHandler(new MediaTransferHandler());
		}
	}

	// TODO: merge with folder table using some common interface for different
	// models
	class MediaTable extends JTable implements ActionListener {
		MediaTable(TableModel tableModel) {
			super(tableModel);
			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0)
						getRMouseMenu(MediaTable.this).show(MediaTable.this, e.getX(), e.getY());
					else if (e.getClickCount() == 2)
						actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			});
		}

		public void actionPerformed(ActionEvent a) {
			String cmd = a.getActionCommand();
			int[] cols = getSelectedRows();
			AlbumTableModel model = (AlbumTableModel) getModel();
			if (cmd.equals(Resources.MENU_SHOW)) {
				Object[] medias = new Object[cols.length];
				for (int i = 0; i < cols.length; i++)
					medias[i] = model.getElementAt(cols[i]);
				// TODO: do it only for MP3 and display images in a free panel
				new PlaybackRequest(medias, controller.getPrefs()).playList(controller);
			} else if (cmd.equals(Resources.MENU_ADDTOCOLLECT)) {
				PhotoCollectionPanel collectionpanel = (PhotoCollectionPanel) controller
						.component(Controller.COMP_COLLECTION);
				File[] medias = new File[cols.length];
				for (int i = 0; i < cols.length; i++)
					medias[i] = model.getElementAt(cols[i]) instanceof File ? (File) model.getElementAt(cols[i])
							: ((MediaFormat) model.getElementAt(cols[i])).getFile();
				collectionpanel.add(medias);
			} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
				// TODO: there is some code duplication from others similar
				// places
				AlbumPane albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
				AlbumSelectionDialog asd = albumpane.getSelectionDialog();
				asd.setTitle(Resources.TITLE_SELECT_ALBUM + ":" + cols.length);
				asd.setVisible(true);
				TreePath[] tps = asd.getSelectedAlbums();
				if (tps != null) {
					MediaFormat[] medias = new MediaFormat[cols.length];
					for (int i = 0; i < cols.length; i++)
						medias[i] = model.getElementAt(cols[i]) instanceof MediaFormat ? (MediaFormat) model
								.getElementAt(cols[i]) :
						// TODO: need check for not null and valid?
								MediaFormatFactory.createMediaFormat((File) model.getElementAt(cols[i]));
					albumpane.addToAlbum(medias, tps, false);
				}
			} else if (cmd.equals(Resources.MENU_PROPERTIES) || cmd.equals(Resources.MENU_EDIT_PROPS)) {
				doProperties(cols, model, cmd.equals(Resources.MENU_EDIT_PROPS));
			}
		}
	}

	protected void doProperties(int[] selections, BaseConfigurableTableModel model, boolean edit) {
		java.util.List medias = edit ? new ArrayList(selections.length) : null;
		for (int i = 0; i < selections.length; i++) {
			Object o = model.getElementAt(selections[i]);
			if (o instanceof MediaFormat)
				if (edit)
					medias.add((MediaFormat) o);
				else {
					PropertiesPanel.showProperties((MediaFormat) o, controller);
					if (i >= TOO_MANY_WINDOWS)
						break;
				}
			else if (o instanceof File) {
				MediaFormat format = MediaFormatFactory.createMediaFormat((File) o);
				if (format != null && format.isValid())
					if (edit)
						medias.add(format);
					else {
						PropertiesPanel.showProperties(format, controller);
						if (i >= TOO_MANY_WINDOWS)
							break;
					}
			}
		}
		if (edit)
			Id3TagEditor.editTag(controller, (MediaFormat[]) medias.toArray(new MediaFormat[medias.size()]));
	}

	class AlbumTable extends MediaTable {
		AlbumTable(AlbumTableModel albumTableModel) {
			super(albumTableModel);
			setDragEnabled(true);
			setTransferHandler(new MediaTransferHandler());
		}
	}

	class CollectionTable extends JTable implements ActionListener {
		CollectionTable() {
			super(((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION)).getModel());
			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0)
						getRMouseMenu(CollectionTable.this).show(CollectionTable.this, e.getX(), e.getY());
					else if (e.getClickCount() == 2)
						actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			});
			setDragEnabled(true);
			setTransferHandler(new MediaTransferHandler());
		}

		public void actionPerformed(ActionEvent a) {
			String cmd = a.getActionCommand();
			int[] cols = getSelectedRows();

			ListModel model = (ListModel) getModel();
			if (cmd.equals(Resources.MENU_SHOW)) {
				Object[] medias = new Object[cols.length];
				for (int i = 0; i < cols.length; i++)
					medias[i] = ((Thumbnail) model.getElementAt(cols[i])).getFormat();
				new PlaybackRequest(medias, controller.getPrefs()).playList(controller);
			} else if (cmd.equals(Resources.MENU_PROPERTIES) || cmd.equals(Resources.MENU_EDIT_PROPS)) {
				boolean edit = cmd.equals(Resources.MENU_EDIT_PROPS);
				java.util.List medias = edit ? new ArrayList(cols.length) : null;
				for (int i = 0; i < cols.length; i++) {
					Thumbnail tn = (Thumbnail) model.getElementAt(cols[i]);
					if (tn != null) // TODO: investigate why it can happen
						if (edit)
							medias.add(tn.getFormat());
						else {
							PropertiesPanel.showProperties(tn.getFormat(), controller);
							if (i >= TOO_MANY_WINDOWS)
								break;
						}
				}
				if (edit)
					Id3TagEditor.editTag(controller, (MediaFormat[]) medias.toArray(new MediaFormat[medias.size()]));
			}
		}
	}

	class RipperTree extends JTree implements ActionListener {
		RipperTable ripperTable;

		RipperTree(RipperTable ripperTable) {
			super(new RipperModel(IniPrefs.getInt(controller.getPrefs().getProperty(MediaOptionsTab.SECNAME,
					MediaOptionsTab.PLAYLIST_TYPE), MediaOptionsTab.AUDIO_MEDIA) == MediaOptionsTab.AUDIO_MEDIA));
			this.ripperTable = ripperTable;
			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0)
						getRMouseMenu().show(RipperTree.this, e.getX(), e.getY());
					else if (e.getClickCount() == 2)
						actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			});

			setTransferHandler(new MediaTransferHandler());

			addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					((AlbumTableModel) RipperTree.this.ripperTable.getModel()).updateModel((VirtualFolder) e.getPath()
							.getLastPathComponent());
					if (RipperTree.this.ripperTable.getRowCount() > 0)
						RipperTree.this.ripperTable.removeRowSelectionInterval(0, RipperTree.this.ripperTable
								.getRowCount() - 1);
					RipperTree.this.ripperTable
							.tableChanged(new TableModelEvent(RipperTree.this.ripperTable.getModel()));
				}
			});
			setEditable(true);
		}

		public void actionPerformed(ActionEvent a) {
			String cmd = a.getActionCommand();

			if (cmd.equals(Resources.MENU_NEW_FOLDER)) {
				Object lc = getSelectionPath() == null || getSelectionPath().getLastPathComponent() == null ? getModel()
						: getSelectionPath().getLastPathComponent();
				((java.util.List) lc).add(new VirtualFolder(Resources.LABEL_NEW_FOLDER + ((java.util.List) lc).size()));
				((RipperModel) getModel()).fireTreeStructureChanged(this, new Object[] { lc }, null, null);
			} else if (cmd.equals(Resources.MENU_RENAME)) {
				startEditingAtPath(getSelectionPath());
			} else if (cmd.equals(Resources.MENU_DELETE)) {
				Object lc = getSelectionPath();
				if (lc != null) {
					Object pc = ((TreePath) lc).getParentPath();
					if (pc != null)
						pc = ((TreePath) pc).getLastPathComponent();
					if (pc != null && pc instanceof java.util.List) {
						((java.util.List) pc).remove(((TreePath) lc).getLastPathComponent());
						((RipperModel) getModel()).fireTreeStructureChanged(this, new Object[] { pc }, null, null);
						updateUiState(controller, (RipperModel) getModel(), false);
					}
				}
			} else if (cmd.equals(Resources.MENU_NEW_LAYOUT)) {
				// setModel(new
				// RipperModel(Serializer.getInt(controller.getSerializer().getProperty(MediaOptionsTab.SECNAME,
				// MediaOptionsTab.PLAYLIST_TYPE)
				// , MediaOptionsTab.AUDIO_MEDIA) ==
				// MediaOptionsTab.AUDIO_MEDIA));
				((AlbumTableModel) ripperTable.getModel()).updateModel((VirtualFolder) null);
				((RipperModel) getModel())
						.reset(IniPrefs.getInt(controller.getPrefs().getProperty(MediaOptionsTab.SECNAME,
								MediaOptionsTab.PLAYLIST_TYPE), MediaOptionsTab.AUDIO_MEDIA) == MediaOptionsTab.AUDIO_MEDIA);
				((RipperModel) getModel()).fireTreeStructureChanged(this, new Object[] { getModel() }, null, null);
				updateUiState(controller, (RipperModel) getModel(), true);
			} else if (cmd.equals(Resources.MENU_LOAD_LAYOUT)) {
				JFileChooser fc = new Controller.ExtFileChooser(false, "Layout1", null, null);
				if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
					try {
						getSaxParserFactory().newSAXParser().parse(
								fc.getSelectedFile(),
								((XmlExposable) getModel()).getXmlHandler(null, null, ((XmlExposable) getModel())
										.getNameSpacePrefix(), null, null));
						((RipperModel) getModel()).fireTreeStructureChanged(this, new Object[] { getModel() }, null,
								null);
						updateUiState(controller, (RipperModel) getModel(), true);
						// update view
					} catch (javax.xml.parsers.ParserConfigurationException pce) {
						System.err.println("Can not obtain XML parser " + pce);
					} catch (IOException ioe) {
						System.err.println("Can not read file " + fc.getSelectedFile() + ' ' + ioe);
					} catch (org.xml.sax.SAXException se) {
						System.err.println("Parser failed for " + fc.getSelectedFile() + ' ' + se);
						if (se.getException() != null)
							se.getException().printStackTrace();
						else if (se instanceof SAXParseException) {
							SAXParseException spe = (SAXParseException) se;
							System.err.println(" in " + spe.getLineNumber() + ':' + spe.getColumnNumber());
						}
					}
			} else if (cmd.equals(Resources.MENU_SAVE_LAYOUT)) {
				// TODO: it can be FTP either
				JFileChooser fc = new Controller.ExtFileChooser(true, "Layout1", null, null);
				if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					if (false == fc.getSelectedFile().exists()
							|| JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, fc.getSelectedFile()
									.getName()
									+ Resources.LABEL_CONFIRM_OVERWRITE, Resources.TITLE_OVERWRITE,
									JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
						try {
							// TODO: add encoding
							// TODO: preserve name of saved/loaded layout
							// TODO: keep latest visited directory?
							Writer w = new FileWriter(fc.getSelectedFile());
							w.write(XmlHelper.getXmlHeader(null));
							w.write(((XmlExposable) getModel()).toXmlString());
							w.flush();
							w.close();
							// System.err.println("XML:"+((XmlExposable)getModel()).toXmlString());
						} catch (IOException ioe) {
							System.err.println("Can not write XML coded ripper layout to :" + fc.getSelectedFile()
									+ ' ' + ioe);
						}
				}
			} else if (cmd.equals(Resources.MENU_PROPERTIES)) {
				// ???
			}
		}

		synchronized protected SAXParserFactory getSaxParserFactory() {
			if (saxParserFactory == null)
				saxParserFactory = SAXParserFactory.newInstance();
			return saxParserFactory;
		}

		JPopupMenu getRMouseMenu() { // ripper tree menu
			JPopupMenu result = new JPopupMenu();
			JMenuItem item;
			result.add(item = new JMenuItem(Resources.MENU_NEW_FOLDER));
			item.addActionListener(this);
			result.add(item = new JMenuItem(Resources.MENU_RENAME));
			item.addActionListener(this);
			result.add(item = new JMenuItem(Resources.MENU_DELETE));
			item.addActionListener(this);
			result.addSeparator();
			result.add(item = new JMenuItem(Resources.MENU_PROPERTIES));
			item.addActionListener(this);
			result.addSeparator();
			result.add(item = new JMenuItem(Resources.MENU_NEW_LAYOUT));
			item.addActionListener(this);
			result.addSeparator();
			result.add(item = new JMenuItem(Resources.MENU_LOAD_LAYOUT));
			item.addActionListener(this);
			result.add(item = new JMenuItem(Resources.MENU_SAVE_LAYOUT));
			item.addActionListener(this);
			return result;
		}
	}

	class RipperTable extends MediaTable {
		RipperTable() {
			super(new AlbumTableModel(controller));
			addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == e.VK_DELETE) {
						((AlbumTableModel) getModel()).delete(getSelectedRows());
						RipperModel model = (RipperModel) ripper.getModel();
						updateUiState(controller, model, false);
					}
				}
			});
		}
	}

	class MediaTransferHandler extends TransferHandler {
		MediaTransferHandler() {
		}

		public int getSourceActions(JComponent c) {
			return COPY;
		}

		protected Transferable createTransferable(JComponent c) {
			if (c instanceof JTable) {
				JTable t = (JTable) c;
				int[] selections = t.getSelectedRows();
				java.util.List medias = new ArrayList();
				if (c instanceof CollectionTable) {
					ListModel model = (ListModel) t.getModel();
					for (int i = 0; i < selections.length; i++)
						medias.add(((Thumbnail) model.getElementAt(selections[i])).getFormat());
					return new TransferHelper.TransferableMedias(medias);
				} else if (c instanceof AlbumTable) {
					AlbumTableModel model = (AlbumTableModel) t.getModel();
					for (int i = 0; i < selections.length; i++)
						medias.add(model.getElementAt(selections[i]));
					return new TransferHelper.TransferableMedias(medias);
				} else if (c instanceof DirectoryTable) {
					FolderTableModel model = (FolderTableModel) t.getModel();
					for (int i = 0; i < selections.length; i++)
						medias.add(model.getElementAt(selections[i]));
					return new TransferHelper.TransferableMedias(medias);
				}
			} else if (c instanceof AlbumTree) {
				return new TransferHelper.TransferableMedias(access.getAlbumId(((AlbumTree) c).getSelectionPath()));
			}

			return null;
		}

		// TODO: do not drop non audio files on music layout
		public boolean importData(JComponent comp, Transferable t) {
			if (comp instanceof RipperTree) {
				RipperTree rt = (RipperTree) comp;
				RipperModel model = (RipperModel) rt.getModel();
				TreePath tp = rt.getSelectionPath();
				Object lc = tp == null ? null : tp.getLastPathComponent();
				VirtualFolder dropHost = (lc != null && lc instanceof VirtualFolder) ? (VirtualFolder) lc
						: (VirtualFolder) model;
				try {
					if (t.isDataFlavorSupported(TransferHelper.MIXED_FILE_MEDIA_LIST_DF)) {
						java.util.List medias = (java.util.List) t
								.getTransferData(TransferHelper.MIXED_FILE_MEDIA_LIST_DF);
						Iterator i = medias.iterator();
						while (i.hasNext()) {
							Object media = i.next();
							if (media instanceof MediaFormat)
								dropHost.addMedia((MediaFormat) media);
							else if (media instanceof File)
								dropHost.addMedia((File) media);
						}
					} else if (t.isDataFlavorSupported(TransferHelper.FILE_LIST_DF)) {
						Iterator i = ((java.util.List) t.getTransferData(TransferHelper.FILE_LIST_DF)).iterator();
						// TODO: create virtual folders if file is directory
						while (i.hasNext())
							dropHost.addMedia(i.next());
					} else if (t.isDataFlavorSupported(TransferHelper.ALBUM_NODE_INDEX_DF)) {
						// TODO: make it recursive
						Integer albumNode = (Integer) t.getTransferData(TransferHelper.ALBUM_NODE_INDEX_DF);
						VirtualFolder vf;
						vf = new VirtualFolder(access.getNameOfAlbum(albumNode.intValue()));
						dropHost.add(vf);
						Object[] formats = access.getAlbumContents(albumNode.intValue());
						for (int i = 0; i < formats.length; i++)
							vf.addMedia(formats[i]);
					}
					rt.ripperTable.revalidate();
					updateUiState(controller, model, false);
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			return false;
		}

		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
			return true;
		}
	}

	protected RipperTree ripper;

	protected MediaAccess access;
}

class OneThirdScroll extends JScrollPane {
	protected boolean vertical;

	OneThirdScroll(Component view) {
		super(view);
		this.vertical = vertical;
	}

	public Dimension getPreferredSize() {
		Dimension result = super.getPreferredSize();
		result.height = getParent().getSize().height;
		result.width = getParent().getSize().width / 3 + 1;
		return result;
	}
}

// TODO: do sync on using more methods
class MediaList extends ArrayList implements java.util.List {
	protected long length;

	protected int time;

	public boolean add(Object o) {
		if (o != null) {
			MediaFormat af = toMediaFormat(o);
			if (af != null) {
				length += af.getFileSize();
				if (af.getType() != MediaFormat.STILL)
					time += af.getMediaInfo().getLongAttribute(MediaInfo.LENGTH);
			}
		}
		return super.add(o); // af
	}

	public boolean remove(Object o) {
		if (o != null) {
			MediaFormat af = toMediaFormat(o);
			if (af != null) {
				length -= af.getFileSize();
				if (af.getType() != MediaFormat.STILL)
					time -= af.getMediaInfo().getLongAttribute(MediaInfo.LENGTH);
			}
		}
		return super.remove(o); // af
	}

	public Object set(int index, Object o) {
		MediaFormat af = toMediaFormat(o);
		if (o != null) {
			if (af != null) {
				length += af.getFileSize();
				if (af.getType() != MediaFormat.STILL)
					time += af.getMediaInfo().getLongAttribute(MediaInfo.LENGTH);
			}
		}
		o = super.set(index, o);
		af = toMediaFormat(o);
		if (af != null) {
			length -= af.getFileSize();
			if (af.getType() != MediaFormat.STILL)
				time -= af.getMediaInfo().getLongAttribute(MediaInfo.LENGTH);
		}
		return o;
	}
	
	protected static MediaFormat toMediaFormat(Object o) {		
		return o instanceof MediaFormat?(MediaFormat)o: o instanceof File?MediaFormatFactory.createMediaFormat((File) o):null;
	}

	public void clear() {
		super.clear();
		length = 0;
		time = 0;
	}

	public long getLength() {
		return length;
	}

	public int getTime() {
		return time;
	}
}

class VirtualFolder extends ArrayList implements XmlExposable {
	String name;

	MediaList medias;

	VirtualFolder(String name) {
		this.name = name;
	}

	void addMedia(MediaFormat format) {
		addMedia((Object) format);
	}

	void addMedia(File file) {
		addMedia((Object) file);
	}

	void addMedia(Object media) {
		if (medias == null)
			medias = new MediaList();
		medias.add(media);
	}

	java.util.List getContent() {
		return medias;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public long getLength() {
		return medias != null ? medias.getLength() : 0;
	}

	public int getTime() {
		return medias != null ? medias.getTime() : 0;
	}

	public String toXmlString() {
		StringBuffer xmlBuffer = new StringBuffer();
		if (medias != null) {
			Iterator i = medias.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				if (o instanceof XmlExposable)
					xmlBuffer.append(((XmlExposable) o).toXmlString());
				else if (o instanceof MediaFormat)
					XmlHelper.appendTag(xmlBuffer, Resources.TAG_MEDIA, XmlHelper.appendAttr(XmlHelper.appendAttr(null,
							Resources.ATTR_MEDIATYPE, ((MediaFormat) o).getDescription()), Resources.ATTR_NAME,
							((MediaFormat) o).getDescription()), HttpUtils.htmlEncode(((MediaFormat) o).getFile().getPath()),
							getNameSpacePrefix());
				else if (o instanceof File)
					XmlHelper.appendTag(xmlBuffer, Resources.TAG_FILE, null,
							HttpUtils.htmlEncode(((File) o).getPath()), getNameSpacePrefix());
			}
		}
		Iterator i = iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof XmlExposable)
				xmlBuffer.append(((XmlExposable) o).toXmlString());
		}
		return XmlHelper.getTag(Resources.TAG_FOLDER, XmlHelper.appendAttr(null, Resources.ATTR_NAME, name),
				xmlBuffer == null ? null : xmlBuffer.toString(), getNameSpacePrefix());
	}

	public String getNameSpacePrefix() {
		return PhotoOrganizer.PROGRAMNAME;
	}

	public String getNameSpaceUri() {
		return PhotoOrganizer.BASE_URL;
	}

	public DefaultHandler getXmlHandler(final ContentHandler parent, String namespaceURI, String localName,
			String qName, Attributes atts) {

		return new DefaultHandler() {
			String file;

			boolean open;

			ContentHandler handler, parentHandler;
			{
				parentHandler = parent;
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes)
					throws SAXException {
				if (handler != null)
					handler.startElement(uri, localName, qName, attributes);
				else {
					if ((getNameSpacePrefix() + ':' + Resources.TAG_FOLDER).equals(qName)) {
						if (!open) {
							setName(attributes.getValue(Resources.ATTR_NAME));
							open = true;
						} else {
							VirtualFolder folder = new VirtualFolder(attributes.getValue(Resources.ATTR_NAME));
							add(folder);
							handler = folder.getXmlHandler(this, uri, localName, qName, attributes);
							handler.startElement(uri, localName, qName, attributes);
						}
					} else if ((getNameSpacePrefix() + ':' + Resources.TAG_FILE).equals(qName)
							|| (getNameSpacePrefix() + ':' + Resources.TAG_MEDIA).equals(qName)) {
						file = null;
					}
				}
			}

			public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
				System.err.println("End " + namespaceURI + ':' + localName + ':' + qName + " NS "
						+ getNameSpacePrefix() + " h " + handler);
				if (handler != null)
					handler.endElement(namespaceURI, localName, qName);
				else {
					if ((getNameSpacePrefix() + ':' + Resources.TAG_FOLDER).equals(qName)) {
						if (parentHandler != null)
							parentHandler.endDocument();
						open = false;
					} else if ((getNameSpacePrefix() + ':' + Resources.TAG_FILE).equals(qName)
							|| (getNameSpacePrefix() + ':' + Resources.TAG_MEDIA).equals(qName)) {
						System.err.println("Found media " + file);
						if (handler != null)
							handler.endElement(namespaceURI, localName, qName);
						else
							addMedia(new File(HttpUtils.htmlDecode(file)));
					}
				}
			}

			public void characters(char buf[], int offset, int len) throws SAXException {
				if (handler != null)
					handler.characters(buf, offset, len);
				else {
					if (file == null)
						file = new String(buf, offset, len);
					else
						file += new String(buf, offset, len);
				}
			}

			public void endDocument() throws SAXException {
				handler = null;
			}
		};
	}
}

class RipperModel extends VirtualFolder implements TreeModel/* , Comparable */{
	EventListenerList listenerList = new EventListenerList();

	boolean music;

	public RipperModel(boolean music) {
		super("" + (Math.round(Math.random() * 10000)));
		this.music = music;
	}

	public void reset(boolean music) {
		this.music = music;
		clear();
		medias = null;
	}

	public Object getChild(Object parent, int index) {
		if (parent instanceof java.util.List)
			return ((java.util.List) parent).get(index);
		return null;
	}

	public java.lang.Object getRoot() {
		return this;
	}

	public int getChildCount(Object parent) {
		if (parent instanceof java.util.List)
			return ((java.util.List) parent).size();
		return 0;
	}

	public boolean isLeaf(Object node) {
		return false; // getChildCount(node) == 0;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		if (newValue != null) {
			VirtualFolder target = (VirtualFolder) (path == null ? this : path.getLastPathComponent());
			target.setName(newValue.toString());
		}
		fireTreeStructureChanged(this, path == null ? new Object[] { this } : path.getPath(), null, null);
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof java.util.List)
			return ((java.util.List) parent).indexOf(child);
		return -1;
	}

	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}

	public void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = 0; i < listeners.length - 1; i += 2) {
			if (listeners[i] == TreeModelListener.class) {
				if (e == null)
					e = new TreeModelEvent(source, path, childIndices, children);
				((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
			}
		}
	}

	public void fireTreeNodesChanged(Object[] path) {
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeModelListener.class) {
				if (e == null)
					e = new TreeModelEvent(this, path, null, null);
				((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
			}
		}
	}

	public boolean add(Object o) {
		if (!music)
			if (o != null)
				if (o instanceof VirtualFolder)
					return super.add(o);
				else
					throw new ClassCastException("Only VirtualFolder can be added in this way");
			else
				throw new IllegalArgumentException("Can not add null folder");
		else
			throw new UnsupportedOperationException("New folder can not be added to music layout");
	}

	public String toString() {
		if (size() == 0 && (medias == null || medias.size() == 0))
			return MessageFormat.format(Resources.LABEL_EMPTY_LAYOUT, new Object[] { music ? Resources.LABEL_MUSIC
					: Resources.LABEL_DATA });
		return MessageFormat.format(Resources.LABEL_LAYOUT_, new Object[] {
				music ? Resources.LABEL_MUSIC : Resources.LABEL_DATA, name, new Integer(size()),
				new Integer(medias == null ? 0 : medias.size()) });
	}

	public void doDisk(Controller controller) {
		IniPrefs s = controller.getPrefs();
		String destpath = (String) s.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.RIPPER_FOLDER);
		//System.out.println("Dest "+destpath);
		if (destpath == null)
			return; // TODO: warning box
		final File destDir = new File(destpath.trim());
		if (!destDir.exists()) {
			if (!destDir.mkdirs())
				return; // TODO: warning box
		}
		//System.out.println("Do disk "+music);
		final StatusBar statusBar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		if (music) {

			new Thread(new Runnable() {
				public void run() {
					MediaFormat format;
					// use clone().
					Iterator i = medias.iterator();
					while (i.hasNext()) {
						Object o = i.next();
						
						if (o instanceof File)
							format = MediaFormatFactory.createMediaFormat((File) o);
						else if (o instanceof MediaFormat)
							format = (MediaFormat) o;
						else
							format = null;
						//System.out.println("Making "+format.getType()+format.getClass().getName());
						if (format != null && format.isValid() && MP3.TYPE.equals(format.getDescription())) {
							String name = format.getName();
							int ep = name.lastIndexOf('.');
							if (ep > 0)
								name = name.substring(0, ep) + ".wav";
							File dest = new File(destDir, name);
							//System.out.println("Writing "+dest);
							try {
								Controller.convertToWav(format, dest.getPath(), statusBar);
							} catch (Exception e) {
								System.err.println("Exception at convertion of " + format + " to " + dest + ':' + e);
							}
						}
					}
				}
			}, "Convert to wav").start();
		}
	}
}

class AlbumTableModel extends BaseConfigurableTableModel {
	VirtualFolder medias;

	AlbumTableModel(Controller controller) {
		updateView(controller);
	}

	synchronized public void updateModel(VirtualFolder medias) {
		this.medias = medias;
	}

	synchronized public void updateModel(Object[] medias) {
		if (medias == null) {
			this.medias = new VirtualFolder("Empty");
			return;
		}
		clear();
		for (int i = 0; i < medias.length; i++)
			if (medias[i] instanceof File && ((File) medias[i]).isDirectory())
				this.medias.add(new VirtualFolder(((File) medias[i]).getName()));
			else
				this.medias.addMedia(medias[i]);

	}

	synchronized public void clear() {
		if (this.medias == null) {
			this.medias = new VirtualFolder("Content");
		} else {
			java.util.List content = medias.getContent();
			if (content != null)
				content.clear();
			this.medias.clear();
		}
	}

	synchronized public void updateModel(java.util.List medias) {
		if (medias == null) {
			this.medias = new VirtualFolder("Empty");
			return;
		}
		clear();
		for (int i = 0; i < medias.size(); i++) {
			if (medias.get(i) instanceof MediaFormat)
				this.medias.addMedia(((MediaFormat) medias.get(i)));
			else {
				File file = (File) medias.get(i);
				if (file.isDirectory())
					this.medias.add(new VirtualFolder(file.getName()));
				else
					this.medias.addMedia(file);
			}
		}
	}

	// folders temporary not supported
	synchronized public void delete(int[] rows) {
		int deleted = 0;
		int di;
		java.util.List list = medias.getContent();
		int maxr = list.size() - 1, minr = 0;
		for (int i = 0; i < rows.length; i++) {
			di = rows[i];
			if (di < list.size() && di >= 0) {
				list.set(di, null);
				deleted++;
				if (di > maxr)
					maxr = di;
				else if (di < minr)
					minr = di;
			}
		}
		if (deleted == 0)
			return;
		while (list.remove(null))
			;
		fireTableRowsDeleted(minr, maxr);
	}

	public int getRowCount() {
		return medias == null ? 0 : (medias.getContent() == null ? 0 : medias.getContent().size());
	}

	public Object getValueAt(int row, int column) {
		if (medias != null && row >= 0 && row < medias.getContent().size()) {
			Object o = medias.getContent().get(row);
			if (o instanceof MediaFormat)
				return getValueAt(((MediaFormat) o).getFile(), ((MediaFormat) o).getMediaInfo(), column);
			else if (o instanceof File) {
				MediaFormat media = MediaFormatFactory.createMediaFormat((File) o);
				if (media != null && media.isValid())
					return getValueAt((File) o, media.getMediaInfo(), column);
			}
		}
		return null;
	}

	public Object getElementAt(int row) {
		return medias.getContent().get(row);
	}

	protected int getDescriptionIndex() {
		return AppearanceOptionsTab.COLLECT_VIEW;
	}
}

class FolderTableModel extends BaseConfigurableTableModel {
	File[] files;

	photoorganizer.directory.FileSystem fileSystem;

	FolderTableModel(Controller controller) {
		fileSystem = new photoorganizer.directory.FileSystem();
		updateView(controller);
	}

	public void updateModel(File[] files) {
		this.files = files;
	}

	public Object getElementAt(int row) {
		if (files != null && row >= 0 && row < files.length)
			return files[row];
		return null;
	}

	public int getRowCount() {
		return files == null ? 0 : files.length;
	}

	public Object getValueAt(int row, int column) {
		if (column == 0)
			return fileSystem.getSystemIcon(files[row]);
		column--;
		if (files != null && row >= 0 && row < files.length) {
			return getValueAt(files[row], null, column);
		}
		return null;
	}

	protected int getDescriptionIndex() {
		return AppearanceOptionsTab.BROWSE_VIEW;
	}

	public int getColumnCount() {
		return super.getColumnCount() + 1;
	}

	public String getColumnName(int column) {
		if (column == 0)
			return " ";
		return super.getColumnName(column - 1);
	}

	public Class getColumnClass(int column) {
		if (column == 0)
			return Icon.class;
		return super.getColumnClass(column - 1);
	}
}

class CDModel extends BaseConfigurableTableModel {
// read about CDDB protocol here:
	// http://ftp.freedb.org/pub/freedb/latest/CDDBPROTO
	// access URL: http://cddb.cddb.com/~cddb/cddb.cgi?cmd=...&hello=dcollins+escort+cda+v2.6PL0&proto=4
	// get cd info cdda2wav.exe  -J -device 1,0,0 -N
	@Override
	protected int getDescriptionIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getRowCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getValueAt(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
