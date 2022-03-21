/* MediaChest - IpodPane.java
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
 *  $Id: IpodPane.java,v 1.121 2008/04/15 23:46:26 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.Thumbnail;
import photoorganizer.ipod.BaseHeader;
import photoorganizer.ipod.BaseItem;
import photoorganizer.ipod.ContentVisualizer;
import photoorganizer.ipod.ITunesDB;
import photoorganizer.ipod.IpodControl;
import photoorganizer.ipod.ItemList;
import photoorganizer.ipod.PhotoDB;
import photoorganizer.ipod.PlayItem;
import photoorganizer.ipod.PlayList;
import photoorganizer.ipod.PhotoDB.PhotoItem;

public class IpodPane extends JTree implements ActionListener, Persistancable {

	public final static int INVALIDATE_NONE = 0;

	public final static int INVALIDATE_TREE = 1;

	public final static int INVALIDATE_TABLE = 2;

	public final static int INVALIDATE_ALL = 3;

	final static String ICONS_PROP = "JTree.icons";

	final static String SECNAME = "IpodPane";

	final static String MEDIA_LIB_DIR = "MediaLibRoot";

	protected Controller controller;

	protected PlayListPane playListPane;
	
	protected ThumbnailsPanel photoPane;

	// protected AlbumPane albumpane;
	protected StatusBar statusbar;

	protected Map actions;

	protected ITunesDB iTunesDb;

	protected List<IpodPane> mirrors;

	protected volatile boolean dragInProgress;

	public IpodPane(Controller controller) {
		this(controller, null);
	}

	public IpodPane mirror(IpodPane ipodPane) {
		if (mirrors == null)
			mirrors = new ArrayList<IpodPane>();
		IpodPane result = ipodPane == null ? new IpodPane(controller, iTunesDb) : ipodPane;
		if (ipodPane == null) {
			result.setPlayListPanel(playListPane);
			result.mirror(this);
		}
		mirrors.add(result);
		return result;
	}

	public IpodPane mirror() {
		return mirror(null);
	}

	public IpodPane(Controller controller, ITunesDB iTunesDb) {
		this.controller = controller;
		IpodOptionsTab.getTransliteration(controller);
		setModel(iTunesDb != null ? iTunesDb : new ITunesDB());
		statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		putClientProperty(ICONS_PROP, makeIcons());
		setCellRenderer(new IconCellRenderer(getCellRenderer()));
		setExpandsSelectedPaths(true);
		addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				// System.err.println("Value changed :"+e);
				Object list = e.getPath().getLastPathComponent();
				if (isDragInProgress()/* dragInProgress */) {
					if (Resources.LABEL_PLAYLISTS.equals(list.toString())) // not
						// very
						// robust
						expandPath(e.getPath());
					// scrollPathToVisible(getPath());
					return;
				}
				if (e.isAddedPath() == false) {
					// playListPane.updateList(null);
					IpodPane.this.controller.getUiUpdater().notify(false, UiUpdater.PLAYLIST_SELECTED);
					IpodPane.this.controller.getUiUpdater().notify(false, UiUpdater.PLAYLIST_MAGIC);
					return;
				}
				IpodPane.this.controller.setWaitCursor(IpodPane.this, true);
				// System.err.println("Tree sel ev: "+e.isAddedPath());
				// deselect others
				if (mirrors != null)
					for (IpodPane ipodPane : mirrors)
						ipodPane.removeSelectionRows(ipodPane.getSelectionRows());
				Object o = e.getPath().getPathCount() > 1 ? e.getPath().getPathComponent(1) : ITunesDB.IPOD;
				if (o != null)
					o = o.toString();
				IpodPane.this.controller.updateCaption((String) o);
				if (list instanceof PlayList) {
					if (playListPane.getParent() == null) {
						((JViewport)photoPane.getParent()).setView(playListPane);
					}
					playListPane.updateList(list, IpodPane.this);
					boolean on = (e.getPath().getPathCount() > 2 && (Resources.LABEL_ARTISTS.equals(o)
							|| Resources.LABEL_ALBUMS.equals(o) || Resources.LABEL_GENRES.equals(o) || Resources.LABEL_COMPOSERS
							.equals(o)))
							|| o.equals(Resources.LABEL_ALL_FILES);
					IpodPane.this.controller.getUiUpdater().notify(on, UiUpdater.IPODVIEW_SELECTED);
					on = ((PlayList) list).smart != null;
					IpodPane.this.controller.getUiUpdater().notify(on, UiUpdater.PLAYLIST_MAGIC);
				} else if (list instanceof ItemList) {
					JViewport vp = (JViewport) playListPane.getParent();
					if (vp != null) {
						if (photoPane == null) {
							photoPane = new ThumbnailsPanel(IpodPane.this.controller) {
								@Override
								void showFullImage(MediaFormat format, Thumbnail source) {
									super.showFullImage(format, source);
									imagepanel.actionPerformed(new ActionEvent(this, 0, Resources.MENU_POPUPWIN));
								}
							};
							if (mirrors != null)
								for (IpodPane ipodPane : mirrors)
									ipodPane.photoPane = photoPane;
						}
						vp.setView(photoPane);
					}
					photoPane.removeAll();
					photoPane.calculateLayout();
					String dev = IpodOptionsTab.getDevice(IpodPane.this.controller);
					//mediautil.gen.Log.debugLevel = Log.LEVEL_ERROR;
					for (PhotoItem pi:((ItemList<PhotoItem>)list).getItems()) {
						pi.getFullMedia(dev);
						photoPane.add(photoPane.createThumbnail(pi));
					}
					photoPane.adjustDimension();
				}
				// System.err.println("Selected "+o+" len
				// "+e.getPath().getPathCount());
				// TODO: can be done using play list attributes , like
				IpodPane.this.controller.getUiUpdater().notify(Resources.LABEL_PLAYLISTS.equals(o),
						UiUpdater.PLAYLIST_SELECTED);				
				IpodPane.this.controller.setWaitCursor(IpodPane.this, false);
			}
		});
		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				int m = e.getModifiers();
				new Point(e.getX(), e.getY());
				if ((m & InputEvent.BUTTON3_MASK) > 0)
					getRMouseMenu().show(IpodPane.this, e.getX(), e.getY());
			}
		});

		actions = new HashMap();
		Action a = new IpodAction(Resources.MENU_IPOD_SYNC, Controller.getResourceIcon(Resources.IMG_IPOD));
		a.putValue(a.SHORT_DESCRIPTION, Resources.TTIP_IPOD_SYNC);
		a.putValue(a.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
		// a.putValue(a.SMALL_ICON,
		// Controller.getResourceIcon(Resources.IMG_IPOD_SMALL));
		actions.put(Resources.MENU_IPOD_SYNC, a);
		setTransferHandler(TransferHelper.createTrasnsferHandler(this, controller));
		setDragEnabled(true);
		MiscellaneousOptionsTab.applyFontSettings(this, controller);
		setEditable(true);
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == e.VK_DELETE && canDeleteSelected()) {
					if (IniPrefs.getInt(IpodPane.this.controller.getPrefs().getProperty(
							MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.SHOWWARNDLG), 0) == 1
							|| JOptionPane.showConfirmDialog(IpodPane.this, Resources.LABEL_CONFIRM_DELETE_PLAYLIST,
									Resources.TITLE_DELETE_PALYLIST, JOptionPane.OK_CANCEL_OPTION,
									JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION)
						actionPerformed(new ActionEvent(this, 4, Resources.MENU_DELETE_PLAYLIST));
				}
			}
		});		

		try {
			getDropTarget().addDropTargetListener(new DropTargetAdapter() {
				// save restore need to be done only for a particular dnd
				public void dragEnter(DropTargetDragEvent dtde) {
					dragInProgress = true;
					selectedPaths = getSelectionPaths();
				}

				public void dragExit(DropTargetEvent dte) {
					dragInProgress = false;
				}

				public void drop(DropTargetDropEvent dtde) {
					dragInProgress = false;
					setSelectionPaths(selectedPaths);
				}

				TreePath[] selectedPaths;
			});
		} catch (TooManyListenersException tmle) {
			// should not happen... swing drop target is multicast
		}
		IpodControl.getIpodControl(controller);
	}
	
	private void assureListeners() {
		iTunesDb.addTreeModelListener(new TreeModelListener() {
			public void treeNodesChanged(TreeModelEvent e) {
				System.err.println("treeNodesChanged " + e);
			}

			public void treeNodesInserted(TreeModelEvent e) {
				// System.err.println("treeNodesInserted");
			}

			public void treeNodesRemoved(TreeModelEvent e) {
				// System.err.println("treeNodesRemoved");
			}

			public void treeStructureChanged(TreeModelEvent e) {				
				boolean cond = IpodPane.this.iTunesDb.getPlayLists() != null
						&& IpodPane.this.iTunesDb.getPlayLists().size() > 0;
			 //System.err.println("treeStructureChanged "+cond);
				IpodPane.this.controller.getUiUpdater().notify(cond, UiUpdater.PLAYLIST_EXISTS);
				IpodPane.this.controller.getUiUpdater().notify(false, UiUpdater.IPODVIEW_SELECTED);
			}
		});
		if (IniPrefs.getInt(controller.getPrefs().getProperty(IpodOptionsTab.SECNAME,
				IpodOptionsTab.DONOT_SHOW_ARTIST_FROM_COMPILATION), 0) == 1)
			iTunesDb.setAwareCompilation(true);

	}

	boolean isDragInProgress() {
		if (dragInProgress)
			return true;
		if (mirrors != null)
			for (IpodPane ipodPane : mirrors)
				if (ipodPane.dragInProgress)
					return true;
		return false;
	}

	public boolean isPathEditable(TreePath path) {
		// TODO: consider editables for virtual lists too
		Object list = path.getLastPathComponent();
		return list instanceof PlayList && ((PlayList) list).isVirtual() == false;
	}

	void setPlayListPanel(PlayListPane playListPane) {
		this.playListPane = playListPane;
		if (mirrors != null)
			for (IpodPane ipodPane : mirrors)
				ipodPane.playListPane = playListPane;
	}

	public Action getAction(String name) {
		return (Action) actions.get(name);
	}

	public void save() {
		// controller.getSerializer().setProperty(getClass().getName(),
		// ALL_MODE, allMode?Resources.I_YES:Resources.I_NO);
	}

	public void load() {
	}

	protected JPopupMenu getRMouseMenu() {
		JPopupMenu result = new JPopupMenu();
		if (IpodControl.getIpodControl(controller).isSyncing())
			return result;
		Action a;
		result.add(a = new AbstractAction(Resources.MENU_IPOD_SYNC) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IPOD_CONNECTED);
		result.addSeparator();
		result.add(new AbstractAction(Resources.MENU_CREATE_PLAYLIST) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		result.add(new AbstractAction(Resources.MENU_CREATE_MAGICPL) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		result.add(a = new AbstractAction(Resources.MENU_EDIT_MAGICPL) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.PLAYLIST_SELECTED);
		controller.getUiUpdater().addForNotification(a, UiUpdater.PLAYLIST_MAGIC);
		result.add(a = new AbstractAction(Resources.MENU_DELETE_PLAYLIST) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.PLAYLIST_SELECTED);

		result.add(a = new AbstractAction(Resources.MENU_RENAME_PLAYLIST) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.PLAYLIST_SELECTED);
		result.add(a = new AbstractAction(Resources.MENU_COPY_PLAYLIST) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.PLAYLIST_EXISTS);
		// controller.getUiUpdater().addForNotification(a,
		// UiUpdater.PLAYLIST_MAGIC);
		// controller.getUiUpdater().addForNotification(a,
		// UiUpdater.IPODVIEW_SELECTED);
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_MERGE_PLAYLIST) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IPODVIEW_SELECTED);
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_FIND) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IPODVIEW_SELECTED);
		result.add(new AbstractAction(Resources.MENU_REFRESH) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a);
			}
		});
		result.addSeparator();
		result.add(a = new AbstractAction(Resources.MENU_RESTORE_IPOD) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a); // use a real function name
			}
		});
		controller.getUiUpdater().addForNotification(a, UiUpdater.IPOD_CONNECTED);
		result.add(a = new AbstractAction(Resources.MENU_STARTOVER) {
			public void actionPerformed(ActionEvent a) {
				IpodPane.this.actionPerformed(a); // use a real function name
			}
		});
		return result;
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		long usedSpace = 0;
		if (Resources.MENU_IPOD_SYNC.equals(cmd)
				|| (a.getSource() instanceof JButton && ((JButton) a.getSource()).getToolTipText().equals(
						Resources.TTIP_IPOD_SYNC))) {
			// TODO: start in a separate box with progress indicator
			/* SwingUtilities.invokeLater */
			new Thread(new Runnable() {
				public void run() {
					IpodControl.getIpodControl(controller).sync(iTunesDb);
					iTunesDb.refreshViews();
					invalidateTree(null);
					updateTable();
				}
			}, "Sync").start();
		} else if (Resources.MENU_IPOD_WIPE.equals(cmd)) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, Resources.LABEL_CONFIRM_WIPE_IPOD,
					Resources.TITLE_DELETE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						controller.setWaitCursor(IpodPane.this, true);
						IpodControl.getIpodControl(controller).wipeAll(iTunesDb);
						setModel(new ITunesDB());
						invalidateTree(null);
						updateTable();
						controller.setWaitCursor(IpodPane.this, false);
					}
				});
			}
		} else if (Resources.MENU_CREATE_PLAYLIST.equals(cmd) || Resources.MENU_CREATE_MAGICPL.equals(cmd)) {
			BaseHeader.Smart smart = null;
			if (Resources.MENU_CREATE_MAGICPL.equals(cmd)) {
				smart = new BaseHeader.Smart();
				smart.checkLimit = false;
				smart.checkRegExp = true;
				smart.limit = 25;
				smart.item = smart.UNIT_SONG;
				smart.rules = new BaseHeader.Rules();
				smart.rules.rules = new ArrayList(1);
				smart.rules.rules.add(new BaseHeader.Rules.Rule());
				smart = MagicListPropEditor.doDialog(controller, smart, "", iTunesDb); // ??
				// use
				// null
				if (smart == null)
					return; // cancel selected
			}
			String playList = JOptionPane.showInputDialog(this, Resources.LABEL_INPUTPLAYLIST,
					Resources.TITLE_PLAYLIST, JOptionPane.QUESTION_MESSAGE);
			if (playList != null) {
				PlayList pl = iTunesDb.getPlayList(playList);
				pl.smart = smart;
				iTunesDb.clearUpdateCache();
				iTunesDb.updateMagicList(pl);
				// TODO: open and select in tree
				updateTable();
				invalidateTree(null);
				setSelectionPath(new TreePath(new Object[] { iTunesDb, iTunesDb.getChild(iTunesDb, 1), pl }));
			}
		} else if (Resources.MENU_EDIT_MAGICPL.equals(cmd)) {
			Object list = getSelectionPath().getLastPathComponent();
			BaseHeader.Smart magic = list != null && list instanceof PlayList ? ((PlayList) list).smart : null;
			if (magic != null) {
				magic = MagicListPropEditor.doDialog(controller, magic, list.toString(), iTunesDb);
				if (magic != null) {
					PlayList pl = (PlayList) list;
					pl.smart = magic;
					pl.clear();
					iTunesDb.clearUpdateCache();
					iTunesDb.updateMagicList(pl);
					updateTable();
				}
			}
		} else if (Resources.MENU_RENAME_PLAYLIST.equals(cmd)) {
			startEditingAtPath(getSelectionPath());
		} else if (Resources.MENU_DELETE_PLAYLIST.equals(cmd)) {
			if (getSelectionPath() != null) {
				IniPrefs s = controller.getPrefs();
				String playList = getSelectionPath().getLastPathComponent().toString();
				if (IniPrefs.getInt(
						s.getProperty(MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.SHOWWARNDLG), 0) == 1
						&& IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME,
								MiscellaneousOptionsTab.SHOWWARNDLG), 0) == 1
						&& JOptionPane.showConfirmDialog(this, Resources.LABEL_COFIRMDELETE + playList,
								Resources.TITLE_COFIRMATION, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
					return;
				iTunesDb.deletePlayList(playList);
				invalidateTree(getSelectionPath().getParentPath()); // parent
				// only
				updateTable();
			}
		} else if (Resources.MENU_MERGE_PLAYLIST.equals(cmd)) {
			TreePath[] paths = getSelectionPaths();
			if (paths == null || paths.length <= 1)
				return;
			String[] names = new String[paths.length];
			for (int i = 0; i < paths.length; i++)
				names[i] = paths[i].getLastPathComponent().toString();
			Object o = JOptionPane.showInputDialog(this, Resources.LABEL_SELECT_MERGE_NAME, Resources.TITLE_MAKEITALL,
					JOptionPane.OK_CANCEL_OPTION, null, names, names[0]);
			if (o == null)
				return;
			Object o2 = paths[0].getPathCount() > 2 ? paths[0].getPathComponent(1) : null;
			if (o2 != null && o2 instanceof ITunesDB.PlayDirectory) {
				ITunesDB.PlayDirectory playDitectory = (ITunesDB.PlayDirectory) o2;
				String name = (String) o;
				int index = playDitectory.descriptor.selector;
				// TODO: figure out which name is selected and remove from set
				for (int i = 0; i < paths.length; i++) {
					try {
						PlayList pl = (PlayList) paths[i].getLastPathComponent();
						synchronized (pl) {
							Iterator it = pl.iterator();
							while (it.hasNext()) {
								PlayItem pi = (PlayItem) it.next();
								pi.set(index, name);
								pi.resetState(pi.STATE_METASYNCED);
							}
						}
					} catch (Exception e) {
						System.err.println("Exception at merge:  " + e);
					}
				}
			}
		} else if (Resources.MENU_COPY_PLAYLIST.equals(cmd)) {
			TreePath[] paths = getSelectionPaths();
			if (paths == null || paths.length == 0)
				return;
			IniPrefs s = controller.getPrefs();
			String targetPath = (String) s.getProperty(SECNAME, MEDIA_LIB_DIR);
			targetPath = new JDirectoryChooser(this, targetPath, null).getDirectory();
			if (targetPath != null) {
				s.setProperty(SECNAME, MEDIA_LIB_DIR, targetPath);
				String dev = IpodOptionsTab.getDevice(controller);
				List<File[]> copyList = new ArrayList<File[]>(100);
				for (TreePath path : paths) {
					PlayList pl = (PlayList) path.getLastPathComponent();
					for (PlayItem pi : pl) {
						copyList
								.add(new File[] {
										pi.getFile(dev),
										new File(targetPath, IpodOptionsTab.buildPath(pi, File.separatorChar, null,
												controller)) });
					}
				}
				BatchActionWithProgress.doLongTimeOperation(controller.mediachest, copyList,
						new Controller.CopierWithProgress(true));
			}
		} else if (Resources.MENU_FIND.equals(cmd)) {
			TreePath[] paths = getSelectionPaths();
			if (paths == null || paths.length == 0)
				return;
			Descriptor d = MediaInfoInputPanel.doMediaInfoInput(controller, null, null, Resources.TITLE_FIND);
			if (d == null)
				return;
		} else if (Resources.MENU_STARTOVER.equals(cmd)) {
			setModel(new ITunesDB());
			invalidateTree(null);
			updateTable();
		} else if (Resources.MENU_EXPORTTOWPL.equals(cmd)) {
			// TODO: explain why we do it here, when xml export in play list
			// pane

			Object list = getSelectionPath().getLastPathComponent();
			Writer w = null;
			JFileChooser fc = new Controller.ExtFileChooser(true, Resources.LABEL_PLAYLIST + "- " + list, ".wpl",
					"Windows Media Player");
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				if (false == fc.getSelectedFile().exists()
						|| JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, fc.getSelectedFile().getName()
								+ Resources.LABEL_CONFIRM_OVERWRITE, Resources.TITLE_OVERWRITE,
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
					try {
						controller.setWaitCursor(getTopLevelAncestor(), true);
						w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), Resources.ENC_UTF_8);
						w.write("<?wpl version=\"1.0\"?>\n   <smil>\n");
						String dev = IpodOptionsTab.getDevice(controller);
						w.write(((PlayList) list).toWplString(dev));
						w.write("</smil>");
						w.flush();
						w.close();
						w = null;
						// System.err.println("XML:"+((XmlExposable)getModel()).toXmlString());
					} catch (IOException ioe) {
						System.err.println("Can not write WPL playlist to :" + fc.getSelectedFile() + ' ' + ioe);
					} finally {
						if (w != null)
							try {
								w.close();
							} catch (IOException ioe) {
							}
						controller.setWaitCursor(getTopLevelAncestor(), false);
					}
			}
		} else if (Resources.MENU_RESTORE_IPOD.equals(cmd)) {
			controller.setWaitCursor(IpodPane.this, true);
			setModel(new ITunesDB());
			usedSpace = IpodControl.getIpodControl(controller).readDatabase(iTunesDb);
			invalidateTree(null);
			updateTable();
			controller.setWaitCursor(IpodPane.this, false);
		} else if (Resources.MENU_REFRESH.equals(cmd)) {
			iTunesDb.refreshViews();
			invalidateTree(null);
			updateTable();
		} else if (cmd.endsWith(Resources.MENU_EXPORTTOHTML)) {
			a.setSource(null); // consumed
			Writer w = null;
			JFileChooser fc = new Controller.ExtFileChooser(true, Resources.LABEL_PLAYLIST + "- ALL",
					Resources.EXT_HTML, Resources.LABEL_WEBPAGE, Resources.TITLE_SAVE_HTML); // localize
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				if (false == fc.getSelectedFile().exists()
						|| JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, fc.getSelectedFile().getName()
								+ Resources.LABEL_CONFIRM_OVERWRITE, Resources.TITLE_OVERWRITE,
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
					try {
						controller.setWaitCursor(getTopLevelAncestor(), true);
						// TODO: read template from res (temporary from ext file
						// TODO: read encoding from config
						w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), "utf-8");
						new ContentVisualizer(controller, (ITunesDB) getModel(), Stream.streamToString(controller
								.getResourceAsStream("resource/template/ipod-html.htmp", this), "utf-8", -1), w);
					} catch (IOException ioe) {
						System.err.println("Can not write HTML coded iPod content to :" + fc.getSelectedFile() + ' '
								+ ioe);
					} finally {
						if (w != null)
							try {
								w.close();
							} catch (IOException ioe) {
							}
						controller.setWaitCursor(getTopLevelAncestor(), false);
					}
					// copy images
					File tdir = fc.getSelectedFile().getParentFile();
					InputStream is;
					OutputStream os;
					is = controller.getResourceAsStream("resource/image/minusbl.gif", this);
					try {
						if (is != null)
							try {
								Stream.copyStream(is, new FileOutputStream(new File(tdir, "minusbl.gif")));
							} finally {
								is.close();
							}
						is = controller.getResourceAsStream("resource/image/plusbl.gif", this);
						try {
							Stream.copyStream(is, new FileOutputStream(new File(tdir, "plusbl.gif")));
						} finally {
							is.close();
						}
					} catch (IOException e) {
						System.err.println("IO at copy images for html: " + e);
					}
				}
			}
		}
		statusbar.displayMetric((usedSpace == 0 ? "" : BasicIo.convertLength(usedSpace) + "/")
				+ (iTunesDb.getChild(iTunesDb, 0) == null ? 0 : ((List) iTunesDb.getChild(iTunesDb, 0)).size())
				+ Resources.LABEL_NUM_ITEMS);
	}

	public void invalidateTree(Object node) {
		// TODO: do sort only changed
		iTunesDb.sort();
		if (iTunesDb != null)
			iTunesDb.fireTreeStructureChanged(this, new Object[] { node == null ? getModel().getRoot() : node }, null,
					null);
	}

	public void invalidatePlayLists() {
		invalidateTree(iTunesDb.getChild(iTunesDb, 0));
	}

	/**
	 * Adds media files to list and creates corresponding play items
	 * 
	 * @param medias
	 * @param playList
	 * @param invalidateCode
	 */
	public void add(MediaFormat[] medias, PlayList playList, int invalidateCode) {
		// TODO: fix bad design, the selection array two times processed
		if (medias == null)
			return;
		for (int i = 0; i < medias.length; i++)
			if ((medias[i].getType() & MediaFormat.STILL) > 0)
				iTunesDb.addPhotoItem(new PhotoDB.PhotoItem(medias[i]), Resources.LABEL_ALLPHOTOS);
			else
				iTunesDb.addPlayItem(PlayItem.create(medias[i], controller), playList);
		if ((invalidateCode & INVALIDATE_TREE) != 0)
			invalidateTree(null);
		if ((invalidateCode & INVALIDATE_TABLE) != 0)
			updateTable();
	}

	/**
	 * Adds play items to list
	 * 
	 * @param playItems
	 * @param playList
	 * @param invalidateCode
	 */
	public void add(PlayItem[] playItems, PlayList playList, int invalidateCode) {
		// TODO: fix bad design, the selection array two times processed
		for (int i = 0; i < playItems.length; i++) {
			iTunesDb.addPlayItem(playItems[i], playList);
		}
		if ((invalidateCode & INVALIDATE_TREE) != 0)
			invalidateTree(null);
		if ((invalidateCode & INVALIDATE_TABLE) != 0)
			updateTable();
	}

	public void remove(PlayItem[] playItems, PlayList playList, int invalidateCode) {
		// System.err.println("Removing from "+playList);
		for (int i = 0; playItems != null && i < playItems.length; i++) {
			iTunesDb.removePlayItem(playItems[i], playList);
			playItems[i].setState(PlayItem.STATE_DELETED);
		}
	}

	/**
	 * added for future extensions, shouldn't be used now
	 */
	public void add(List someItems, PlayList playList) {
		Iterator i = someItems.iterator();
		while (i.hasNext()) {
			Object item = i.next();
			assert item != null;
			BaseItem pi = null;
			if (item instanceof File) {
				MediaFormat format = MediaFormatFactory.createMediaFormat((File) item);
				if (format != null)
					pi = PlayItem.create(format, controller);
			} else if (item instanceof PlayItem) {
				pi = (PlayItem) item;
			} else if (item instanceof MediaFormat) {
				if ((((MediaFormat)item).getType() & MediaFormat.STILL) > 0)
				pi = new PhotoDB.PhotoItem((MediaFormat) item);
				else
				pi = PlayItem.create((MediaFormat) item, controller);
			} else if (item instanceof String) {
				MediaFormat format = MediaFormatFactory.createMediaFormat(new File((String) item));
				if (format != null)
					pi = PlayItem.create(format, controller);
			} else
				System.err.println("Unknown class of item: " + item.getClass().getName());
			if (pi != null) {
				if (pi instanceof PlayItem)
					pi = iTunesDb.addPlayItem((PlayItem)pi, playList);
				else
					pi = iTunesDb.addPhotoItem((PhotoItem)pi, Resources.LABEL_ALLPHOTOS);
			}
		}
	}

	public PlayList getPlayList(String listName) {
		return iTunesDb.getPlayList(listName);
	}

	public boolean isChanged() {
		return iTunesDb.isChanged() || iTunesDb.isPhotoChanged();
	}

	public void rearrange(String before, String list) {
		iTunesDb.rearrangeBefore(before, list);
	}

	public void updateTable() {
		if (getSelectionPath() != null)
			playListPane.updateList(getSelectionPath().getLastPathComponent(), this);
		else
			playListPane.updateList(null, this);
	}

	public void setModel(TreeModel newModel) {
		try {
		if (newModel instanceof ITunesDB) {
			iTunesDb = (ITunesDB) newModel;
			assureListeners();
		} else
			iTunesDb = null;
		super.setModel(newModel);
		
		if (mirrors != null)
			for (IpodPane ipodPane : mirrors) {
				ipodPane.super_setModel(newModel);
			//	ipodPane.iTunesDb = iTunesDb;
				//ipodPane.assureListeners();
			}
		}catch(Error e){
			e.printStackTrace();
		}
	}

	protected void super_setModel(TreeModel newModel) {
		iTunesDb = (ITunesDB) newModel;
		super.setModel(newModel);
	}

	protected boolean canDeleteSelected() {
		if (getSelectionPath() == null)
			return false;
		Object plo = getSelectionPath().getLastPathComponent();
		if (plo instanceof PlayList == false)
			return false;
		PlayList pl = (PlayList) plo;
		// TODO: consider deleting content for virtual
		if (pl.isVirtual() == false && pl.isFileDirectory() == false)
			return true;
		return false;
	}

	// TODO: reconsider and add to all possible tree nodes method getIcon
	protected Map makeIcons() {
		Map icons = new HashMap();
		icons.put(Resources.LABEL_ALBUMS, controller.getResourceIcon(Resources.IMG_ALBUM));
		icons.put(Resources.LABEL_ARTISTS, controller.getResourceIcon(Resources.IMG_ARTIST));
		icons.put(Resources.LABEL_GENRES, controller.getResourceIcon(Resources.IMG_GENRE));
		icons.put(Resources.LABEL_COMPOSERS, controller.getResourceIcon(Resources.IMG_ARTIST));
		icons.put(Resources.LABEL_PLAYLIST, controller.getResourceIcon(Resources.IMG_PLAYLIST));
		icons.put(Resources.LABEL_SMART_PLAYLIST, controller.getResourceIcon(Resources.IMG_SMART_PLAYLIST));
		icons.put(Resources.LABEL_ALL_FILES, controller.getResourceIcon(Resources.IMG_SONG));
		icons.put(Resources.LABEL_PLAYLISTS, controller.getResourceIcon(Resources.IMG_PLAYLISTS));
		icons.put(Resources.LABEL_VIDEOS, controller.getResourceIcon(Resources.IMG_VIDEOS));
		icons.put(Resources.LABEL_COMPILATIONS, controller.getResourceIcon(Resources.IMG_COMPILATION));
		icons.put(Resources.LABEL_PHOTOS, controller.getResourceIcon(Resources.IMG_PHOTOS));
		icons.put(Resources.LABEL_PHOTOALBUM, controller.getResourceIcon(Resources.IMG_PHOTO));

		icons.put(ITunesDB.IPOD, controller.getResourceIcon(Resources.IMG_IPOD_SMALL));
		return icons;
	}

	static class IconCellRenderer implements TreeCellRenderer {
		TreeCellRenderer cr;

		IconCellRenderer(TreeCellRenderer cr) {
			this.cr = cr;
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			Component result = cr.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (result instanceof JLabel) {
				Icon icon = null;
				// TODO: reconsider and add to all possible tree nodes a method
				// getIcon()
				Map icons = (Map) tree.getClientProperty(ICONS_PROP);
				ITunesDB iTunesDb = (ITunesDB) tree.getModel();
				if (value instanceof ITunesDB)
					icon = (Icon) icons.get(ITunesDB.IPOD);
				else if (value instanceof PlayList) {
					PlayList pl = (PlayList) value;
					if (pl.smart != null)
						icon = (Icon) icons.get(Resources.LABEL_SMART_PLAYLIST);
					else if (pl == iTunesDb.getChild(iTunesDb, 0))
						icon = (Icon) icons.get(Resources.LABEL_ALL_FILES);
					else
						icon = (Icon) icons.get(Resources.LABEL_PLAYLIST);
				} else if (value instanceof ITunesDB.PlayDirectory) {
					if (value == iTunesDb.getPlayLists())
						icon = (Icon) icons.get(Resources.LABEL_PLAYLISTS);
					else
						icon = (Icon) icons.get(((ITunesDB.PlayDirectory) value).descriptor.name);
				} else if (value instanceof PhotoDB.PhotoDirectory)
					icon = (Icon) icons.get(Resources.LABEL_PHOTOS);
				else if (value instanceof ItemList)
					icon = (Icon) icons.get(Resources.LABEL_PHOTOALBUM);
				if (icon != null) {
					((JLabel) result).setIcon(icon);
				}
			}
			return result;
		}
	}

	class IpodAction extends AbstractAction {
		IpodAction(String name, Icon icon) {
			super(name, icon);
		}

		public void actionPerformed(ActionEvent a) {
			IpodPane.this.actionPerformed(a);
		}

	}

}