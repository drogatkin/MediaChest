/* MediaChest $RCSfile: JTreeTable.java,v $
 * Copyright (C) 1999-2001 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: JTreeTable.java,v 1.40 2013/05/12 03:12:42 cvs Exp $
 */
/*
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 */
package photoorganizer.directory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import mediautil.gen.MediaFormat;
import mediautil.image.jpeg.BasicJpeg;

import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.IrdControllable;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MP4;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.WMA;
import photoorganizer.media.PlaybackRequest;
import photoorganizer.renderer.AlbumPane;
import photoorganizer.renderer.AlbumSelectionDialog;
import photoorganizer.renderer.AppearanceOptionsTab;
import photoorganizer.renderer.FastMenu;
import photoorganizer.renderer.Id3TagEditor;
import photoorganizer.renderer.IpodPane;
import photoorganizer.renderer.MiscellaneousOptionsTab;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.PhotoImagePanel;
import photoorganizer.renderer.PropertiesPanel;
import photoorganizer.renderer.RenameOptionsTab;
import photoorganizer.renderer.StatusBar;
import photoorganizer.renderer.ThumbnailsPanel;

/**
 * This code is based on an example of Philip Milne, and Scott Violet
 */
public class JTreeTable extends JTable implements ActionListener, Persistancable, IrdControllable {
	final static String SECNAME = "JTreeTable";

	final static String COLWIDTH = "ColumnWidthes";

	final static String DRIVE = "Drive";

	protected TreeTableCellRenderer tree;

	protected PhotoImagePanel imagepanel;

	protected ThumbnailsPanel thumbnailspanel;

	protected PhotoCollectionPanel collectionpanel;

	protected AlbumPane albumpanel;

	protected Controller controller;

	public JTreeTable(TreeTableModel treeTableModel, Controller controller) {
		this.controller = controller;
		this.imagepanel = (PhotoImagePanel) controller.component(Controller.COMP_IMAGEPANEL);
		thumbnailspanel = (ThumbnailsPanel) controller.component(Controller.COMP_THUMBPANEL);
		collectionpanel = (PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION);
		albumpanel = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
		// Create the tree. It will be used as a renderer and editor.
		tree = new TreeTableCellRenderer(treeTableModel);

		// Install a tableModel representing the visible rows in the tree.
		super.setModel(new TreeTableModelAdapter(treeTableModel, tree));

		// Force the JTable and JTree to share their row selection models.
		ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
		tree.setSelectionModel(selectionWrapper);
		setSelectionModel(selectionWrapper.getListSelectionModel());

		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				try {
					if (isRowSelected(e.getLastIndex()) || isRowSelected(e.getFirstIndex())) {
						FileNode[] nodes = getSelectedNodes();
						if (nodes != null) {
							boolean wasDir = false;
							boolean wasImage = false;
							boolean wasAudio = false;
							boolean wasFile = false;
							MediaFormat[] formats = new MediaFormat[nodes.length];
							for (int i = 0; i < nodes.length; i++) {
								formats[i] = nodes[i].getFormat();
								wasImage |= formats[i] instanceof BasicJpeg;
								wasAudio |= formats[i] != null &&formats[i].getFormat(MediaFormat.AUDIO) != null;
								wasDir |= nodes[i].getFile().isDirectory();
								wasFile |= nodes[i].getFile().isFile();
							}
							JTreeTable.this.setCursor(Controller.WAIT_CURSOR);
							if (wasDir /* && showFolderContent */) {
								Vector targetList = new Vector(10);
								buildFormatList(nodes, targetList, true);
								JTreeTable.this.controller.getUiUpdater().notify(
										thumbnailspanel.updateMedias((MediaFormat[]) targetList
												.toArray(new MediaFormat[targetList.size()])), UiUpdater.FILE_SELECTED);
							} else
								JTreeTable.this.controller.getUiUpdater().notify(thumbnailspanel.updateMedias(formats),
										UiUpdater.FILE_SELECTED);
							JTreeTable.this.controller.updateCaption((formats[0] != null ? formats[0].toString()
									: nodes[0].toString())
									+ (formats.length > 1 ? "..." : ""));
							JTreeTable.this.controller.getUiUpdater().notify(wasDir, UiUpdater.DIRECTORY_SELECTED);
							JTreeTable.this.controller.getUiUpdater()
									.notify(wasImage, UiUpdater.GRAPHICS_FILE_SELECTED);
							JTreeTable.this.controller.getUiUpdater().notify(wasAudio, UiUpdater.MEDIA_FILE_SELECTED);
							JTreeTable.this.controller.getUiUpdater().notify(wasFile, UiUpdater.FILE_SELECTED);
							JTreeTable.this.setCursor(Controller.DEFAULT_CURSOR);
						}
					} else {
						JTreeTable.this.controller.getUiUpdater().notify(false, UiUpdater.FILE_SELECTED);
						JTreeTable.this.controller.getUiUpdater().notify(false, UiUpdater.DIRECTORY_SELECTED);
						JTreeTable.this.controller.getUiUpdater().notify(false, UiUpdater.GRAPHICS_FILE_SELECTED);
						JTreeTable.this.controller.getUiUpdater().notify(false, UiUpdater.MEDIA_FILE_SELECTED);
					}
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});

		// Install the tree editor renderer and editor.
		setDefaultRenderer(TreeTableModel.class, tree);
		setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
		setShowGrid(false); // No grid.
		setIntercellSpacing(new Dimension(0, 0)); // No intercell spacing
		// And update the height of the trees row to match that of the table.
		if (tree.getRowHeight() < 1) {
			setRowHeight(18); // Metal looks better like this.
		}

		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0) {
					Point p = ((JViewport) getParent()).getViewPosition();
					FastMenu fm = new FastMenu(JTreeTable.this, JTreeTable.this.controller);
					JMenuItem item;
					fm.add(new JPopupMenu.Separator());
					fm.add(item = new JMenuItem(Resources.MENU_COMMENT));
					item.addActionListener(JTreeTable.this);
					item.setEnabled(JTreeTable.this.controller.getUiUpdater().isEnabled(UiUpdater.MEDIA_FILE_SELECTED));
					fm.add(new JPopupMenu.Separator());
					fm.add(item = new JMenuItem(Resources.MENU_REFRESH));
					item.addActionListener(JTreeTable.this);
					fm.add(item = new JMenuItem(Resources.MENU_DRIVE_SEL));
					item.addActionListener(JTreeTable.this);
					fm.add(item = new JMenuItem(Resources.MENU_CHANGE_ROOT_DIR));
					item.addActionListener(JTreeTable.this);
					fm.show(getParent(), e.getX() - p.x, e.getY() - p.y);
				} else if (e.getClickCount() == 2) {
					actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			}
		});
		setMinimumSize(Resources.MIN_PANEL_DIMENSION);
	}

	FileNode[] getSelectedNodes() {
		TreePath[] tps = tree.getSelectionPaths();
		if (tps == null || tps.length == 0)
			return null;
		FileNode[] result = new FileNode[tps.length];
		int ri = 0;
		for (int n = 0; n < tps.length; n++) {
			Object ps = tps[n].getLastPathComponent();
			if (ps instanceof FileNode)
				result[ri++] = (FileNode) ps;
		}
		return result;
	}

	File[] getSelectedFiles() {
		TreePath[] tps = tree.getSelectionPaths();
		if (tps == null || tps.length == 0)
			return null;
		File[] result = new File[tps.length];
		int ri = 0;
		for (int n = 0; n < tps.length; n++) {
			Object ps = tps[n].getLastPathComponent();
			if (ps instanceof FileNode)
				result[ri++] = ((FileNode) ps).getFile();
		}
		// if (ri < tps.length) squeeze the array
		return result;
	}

	public void fireDriveChanged() {
		FileSystemModel treemodel = (FileSystemModel) tree.getModel();
		File[] drivers = treemodel.getRoots();
		// TODO: should have been rewritten, currently it removes first
		// and other null drives for UNIX
		Vector real_drivers = new Vector(drivers.length);
		Vector descriptions = new Vector(drivers.length);
		FileSystemView fs = FileSystemView.getFileSystemView();
		for (int i = 0; i < drivers.length; i++)
			if (drivers[i] != null) {
				real_drivers.addElement(drivers[i]);
				try {
					descriptions.addElement(fs.getSystemTypeDescription(drivers[i]) + '/'
							+ fs.getSystemDisplayName(drivers[i]));
				} catch (Exception e) {
					System.err.println("Exception " + e + " for drive " + drivers[i]);
				}
				// isFloppyDrive(
			}
		System.err.println("" + descriptions);
		drivers = new File[real_drivers.size()];
		real_drivers.copyInto(drivers);
		Object selectedValue = JOptionPane.showInputDialog(this, Resources.LABEL_CHOOSE_DRIVE,
				Resources.TITLE_CHANGE_DRIVE, JOptionPane.INFORMATION_MESSAGE, null, drivers, drivers[0]);
		if (selectedValue == null)
			return;
		treemodel.setRoot(new FileNode((File) selectedValue, MiscellaneousOptionsTab.getEncoding(controller), AppearanceOptionsTab.needEncoding(controller)));
		treemodel.fireTreeStructureChanged(this, new Object[] { selectedValue }, null, null);
	}

	public void rootDirectoryChange() {
		JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), "", null);
		String rdir = dc.getDirectory();
		if (rdir != null) {
			FileSystemModel treemodel = (FileSystemModel) tree.getModel();
			treemodel.setRoot(new FileNode(new File(rdir), MiscellaneousOptionsTab.getEncoding(controller), AppearanceOptionsTab.needEncoding(controller)));
			treemodel.fireTreeStructureChanged(this, new Object[] { rdir }, null, null);
		}
	}

	protected void refresh() {
		// TODO: clean right pane selection
		FileSystemModel treemodel = (FileSystemModel) tree.getModel();
		FileNode fn = (FileNode) treemodel.getRoot();
		treemodel.setRoot(new FileNode(fn.file, MiscellaneousOptionsTab.getEncoding(controller), AppearanceOptionsTab.needEncoding(controller)));
		treemodel.fireTreeStructureChanged(this, new Object[] { fn.file }, null, null);
		thumbnailspanel.updateMedias(new MediaFormat[0]);
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.MENU_DRIVE_SEL)
				|| (a.getSource() instanceof JButton && ((JButton) a.getSource()).getToolTipText().equals(
						Resources.MENU_DRIVE_SEL))) {
			fireDriveChanged();
			return;
		} else if (cmd.equals(Resources.MENU_REFRESH)) {
			refresh();
			return;
		} else if (cmd.equals(Resources.MENU_CHANGE_ROOT_DIR)) {
			rootDirectoryChange();
			return;
		}
		final File[] files = getSelectedFiles();
		if (files == null)
			return;
		TreePath tpchanged = tree.getSelectionPath().getParentPath();

		if (cmd.equals(Resources.MENU_PROPERTIES)) {
			MediaFormat format = getFirstSelectedMedia();
			if (format != null)
				PropertiesPanel.showProperties(format, controller);
			return;
		} else if (cmd.equals(Resources.MENU_EDIT_PROPS)) {
			MediaFormat[] formats = getAllSelectedMedias();
			if (formats != null)
				Id3TagEditor.editTag(controller, formats);
			return;

		} else if (cmd.equals(Resources.MENU_ADDTOCOLLECT) || // from tool bar
				(a.getSource() instanceof JButton && ((JButton) a.getSource()).getToolTipText().equals(
						Resources.MENU_ADDTOCOLLECT))) {
			// collectionpanel.add((File[])new
			// PlaybackRequest(files).buildList(controller));
			collectionpanel.add(new PlaybackRequest(files, controller.getPrefs()));
			return;
		} else if (cmd.equals(Resources.MENU_ADDTO_IPOD)) {
			controller.setWaitCursor(this, true);
			IpodPane ipodPanel = (IpodPane) controller.component(controller.COMP_IPODPANEL);
			List list = new ArrayList(100);
			controller.buildMediaList(list, files, new int[] { MediaFormat.AUDIO,  MediaFormat.VIDEO, MediaFormat.AUDIO+MediaFormat.AUDIO}, true);
			// System.err.println("Adding "+list.size());
			ipodPanel.add((MediaFormat[]) list.toArray(new MediaFormat[list.size()]), null, ipodPanel.INVALIDATE_ALL);
			controller.setWaitCursor(this, false);
			return;
			// } else if (cmd.equals(Resources.MENU_ADDTO_IPOD_WO)) {

		} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
			AlbumPane albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
			AlbumSelectionDialog asd = albumpane.getSelectionDialog();
			asd.setTitle(Resources.TITLE_SELECT_ALBUM + ":" + files[0] + (files.length == 1 ? "" : "..."));
			asd.setVisible(true);
			final TreePath[] tps = asd.getSelectedAlbums();
			if (tps != null) {
				final Vector totalList = new Vector(); // although I'd prefer array
				// list
				buildFormatList(files, totalList, true);
				new SwingWorker() {

                    @Override
                    protected void done() {
                        ((StatusBar) controller.component(Controller.COMP_STATUSBAR)).clearProgress();
                        super.done();
                    }

                    @Override
                    protected Object doInBackground() throws Exception {
                        ((StatusBar) controller.component(Controller.COMP_STATUSBAR)).setProgress(-1);
                        albumpanel.addToAlbum((MediaFormat[]) totalList.toArray(new MediaFormat[totalList.size()]), tps, false);
                        return null;
                    }
                            
                }.execute();
			}
		} else if (cmd.equals(Resources.MENU_RENAME)) {
			FileNode[] nodes = getSelectedNodes();
			IniPrefs s = controller.getPrefs();
			Object[] masks = RenameOptionsTab.getRenameMask(s);
			boolean requestRenameMask = masks.length == 0 || (masks.length == 1 && masks[0].toString().length() == 0)
					|| masks.length > 1 || RenameOptionsTab.askForEditMask(s);
			String mask = null;
			String errMsg;
			JPanel p = null;
			JComboBox values = null;
			if (requestRenameMask) {
				if (masks == null)
					masks = new String[0];
				values = new JComboBox(masks);
				// TODO: on changing selection change tooltip and rename name
				values.setEditable(true);
				p = new JPanel();
				p.setLayout(new BorderLayout());
				p.add(new JLabel(Resources.LABEL_NEW_NAME), "Center");
				p.add(values, "South");
			} else
				mask = masks[0].toString();
			Object[] options = nodes.length > 1 ? new Object[] { Resources.CMD_YES, Resources.CMD_NO,
					Resources.CMD_YES_ALL, Resources.CMD_CANCEL }
					: new Object[] { Resources.CMD_YES, Resources.CMD_NO };
			nodes_loop: for (int i = 0; i < nodes.length; i++) {
				MediaFormat format = nodes[i].getFormat();
				if (requestRenameMask) {
					switch (JOptionPane.showOptionDialog(this, p, Resources.TITLE_RENAME + ' ' + format.getFile(), 0,
							JOptionPane.QUESTION_MESSAGE, null, options, Resources.CMD_NO)) {
					case 1:
						continue;
					case 2:
						requestRenameMask = false;
						break;
					case 3:
						break nodes_loop;
					}
					mask = values.getSelectedItem().toString();
					if (mask.trim().length() == 0)
						continue;
				}
				if (mask.indexOf('%') >= 0 && format != null && format.isValid()) {
					String dest = mask.startsWith("./") || mask.startsWith(".\\") ? nodes[i].getFile().getParent()
							: RenameOptionsTab.getDestPath(s);
					if (format.renameTo(new File(dest, FileNameFormat.makeValidPathName(new FileNameFormat(mask, true)
							.format(format)))) == false) {
						errMsg = "Could not rename "
								+ format
								+ " to "
								+ new File(nodes[i].getFile().getParent(), FileNameFormat
										.makeValidPathName(new FileNameFormat(mask, true).format(format)));
						System.err.println(errMsg);
						((StatusBar) controller.component(Controller.COMP_STATUSBAR)).flashInfo(errMsg);
					}
				} else if (nodes[i].getFile().renameTo(
						(mask.indexOf(":\\") == 1 || mask.indexOf('/') == 0 || mask.charAt(0) == '\\') ? new File(mask)
								: new File(nodes[i].getFile().getParent(), mask)) == false) {
					errMsg = "Could not rename " + nodes[i].getFile() + " to " + mask;
					System.err.println(errMsg);
					((StatusBar) controller.component(Controller.COMP_STATUSBAR)).flashInfo(errMsg);
				}
			}
		} else if (cmd.equals(Resources.MENU_DELETE)) {
			// TODO: goup deletion as it done in ripper
			for (int i = 0; i < files.length; i++)
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, files[i].getName()
						+ Resources.LABEL_CONFIRM_DEL, Resources.TITLE_DELETE, JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE))
					if (!files[i].delete())
						((StatusBar) controller.component(Controller.COMP_STATUSBAR)).flashInfo("Cannot delete "
								+ files[i], true);

		} else if (cmd.equals(Resources.MENU_PRINT)) {
			controller.print(files);
			return;
		} else if (cmd.equals(Resources.MENU_SHOW)
				|| (a.getSource() instanceof JButton && ((JButton) a.getSource()).getToolTipText().equals(
						Resources.MENU_SHOW))) {
			MediaFormat format = getFirstSelectedMedia();
			if (format != null && format.isValid() && format instanceof BasicJpeg) {
				imagepanel.updateView((BasicJpeg) format);
				controller.updateCaption(files[0].toString());
			} else
				new PlaybackRequest(files).playList(controller);

			return;
		} else if (cmd.equals(Resources.MENU_COMMENT)) {
			MediaFormat format = getFirstSelectedMedia(); // TODO: maybe loop
			// over all
			if (format instanceof MP3)
				Id3TagEditor.editTag(controller, format);
		}
		((AbstractTreeTableModel) tree.getModel()).fireTreeStructureChanged(this, tpchanged.getPath(), null, null);
	}

	protected MediaFormat getFirstSelectedMedia() {
		FileNode[] nodes = getSelectedNodes();
		if (nodes != null) {
			for (int i = 0; i < nodes.length; i++) {
				MediaFormat result = nodes[i].getFormat();
				if (result != null && result.isValid())
					return result;
			}
		}
		return null;
	}

	protected MediaFormat[] getAllSelectedMedias() {
		FileNode[] nodes = getSelectedNodes();
		if (nodes != null) {
			List result = new ArrayList(nodes.length);
			for (int i = 0; i < nodes.length; i++) {
				MediaFormat format = nodes[i].getFormat();
				if (format != null && format.isValid())
					result.add(format);
			}
			return (MediaFormat[]) result.toArray(new MediaFormat[result.size()]);
		}
		return null;
	}

	/*
	 * Workaround for BasicTableUI anomaly. Make sure the UI never tries to
	 * paint the editor. The UI currently uses different techniques to paint the
	 * renderers and editors and overriding setBounds() below is not the right
	 * thing to do for an editor. Returning -1 for the editing row in this case,
	 * ensures the editor is never painted.
	 */
	public int getEditingRow() {
		return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;
	}

	/**
	 * Overridden to message super and forward the method to the tree. Since the
	 * tree is not actually in the component hieachy it will never receive this
	 * unless we forward it in this manner.
	 */
	public void updateUI() {
		super.updateUI();
		if (tree != null) {
			tree.updateUI();
		}
		// Use the tree's default foreground and background colors in the
		// table.
		LookAndFeel.installColorsAndFont(this, "Tree.background", "Tree.foreground", "Tree.font");
	}

	/**
	 * Overridden to pass the new rowHeight to the tree.
	 */
	public void setRowHeight(int rowHeight) {
		super.setRowHeight(rowHeight);
		if (tree != null && tree.getRowHeight() != rowHeight) {
			tree.setRowHeight(getRowHeight());
		}
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		controller.saveTableColumns(this, SECNAME, COLWIDTH);
		FileSystemModel treemodel = (FileSystemModel) tree.getModel();
		FileNode root = (FileNode) treemodel.getRoot();
		s.setProperty(SECNAME, DRIVE, root);
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		controller.loadTableColumns(this, SECNAME, COLWIDTH);
        // setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS/*AUTO_RESIZE_OFF*/);
		Object drive = s.getProperty(SECNAME, DRIVE); // can be string or
		// filenode
		if (drive != null) {
			FileSystemModel fsm = (FileSystemModel) tree.getModel();
			File r = new File(drive.toString());
			if (!r.exists())
				r = fsm.findFirstDrive();
			fsm.setRoot(new FileNode(r, MiscellaneousOptionsTab.getEncoding(controller), AppearanceOptionsTab.needEncoding(controller)));
			fsm.fireTreeStructureChanged(this, new Object[] { r }, null, null);
			revalidate();
			repaint();
		}
	}

	/**
	 * Returns the tree that is being shared between the model.
	 */
	public JTree getTree() {
		return tree;
	}

	//
	// remote controllable
	//
	public String getName() {
		return Resources.COMP_BROWSER;
	}

	public String toString() {
		return getName();
	}

	public Iterator getKeyMnemonics() {
		return Arrays.asList(new Object[0]).iterator();
	}

	public boolean doAction(String keyCode) {
		return false;
	}

	public void bringOnTop() {
		controller.selectTab(Resources.TAB_BROWSE);
	}

	// 
	// The renderer used to display the tree nodes, a JTree.
	//
	public class TreeTableCellRenderer extends JTree implements TableCellRenderer {

		protected int visibleRow;

		public TreeTableCellRenderer(TreeModel model) {
			super(model);
		}

		/**
		 * updateUI is overridden to set the colors of the Tree's renderer to
		 * match that of the table.
		 */
		public void updateUI() {
			super.updateUI();
			// Make the tree's cell renderer use the table's cell selection
			// colors.
			TreeCellRenderer tcr = getCellRenderer();
			if (tcr instanceof DefaultTreeCellRenderer) {
				DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
				// For 1.1 uncomment this, 1.2 has a bug that will cause an
				// exception to be thrown if the border selection color is
				// null.
				// dtcr.setBorderSelectionColor(null);
				dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
				dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
			}
		}

		/**
		 * Sets the row height of the tree, and forwards the row height to the
		 * table.
		 */
		public void setRowHeight(int rowHeight) {
			if (rowHeight > 0) {
				super.setRowHeight(rowHeight);
				if (JTreeTable.this != null && JTreeTable.this.getRowHeight() != rowHeight) {
					JTreeTable.this.setRowHeight(getRowHeight());
				}
			}
		}

		public void setBounds(int x, int y, int w, int h) {
			super.setBounds(x, 0, w, JTreeTable.this.getHeight());
		}

		public void paint(Graphics g) {
			g.translate(0, -visibleRow * getRowHeight());
			super.paint(g);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			if (isSelected)
				setBackground(table.getSelectionBackground());
			else
				setBackground(table.getBackground());

			visibleRow = row;
			return this;
		}
	}

	// 
	// The editor used to interact with tree nodes, a JTree.
	//
	public class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
			return tree;
		}
	}

	/**
	 * Overridden to return false, and if the event is a mouse event it is
	 * forwarded to the tree.
	 * <p>
	 * The behavior for this is debatable, and should really be offered as a
	 * property. By returning false, all keyboard actions are implemented in
	 * terms of the table. By returning true, the tree would get a chance to do
	 * something with the keyboard events. For the most part this is ok. But for
	 * certain keys, such as left/right, the tree will expand/collapse where as
	 * the table focus should really move to a different column. Page up/down
	 * should also be implemented in terms of the table. By returning false this
	 * also has the added benefit that clicking outside of the bounds of the
	 * tree node, but still in the tree column will select the row, whereas if
	 * this returned true that wouldn't be the case.
	 * <p>
	 * By returning false we are also enforcing the policy that the tree will
	 * never be editable (at least by a key sequence).
	 */
	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			for (int counter = getColumnCount() - 1; counter >= 0; counter--) {
				if (getColumnClass(counter) == TreeTableModel.class) {
					MouseEvent me = (MouseEvent) e;
					MouseEvent newME = new MouseEvent(tree, me.getID(), me.getWhen(), me.getModifiers(), me.getX()
							- getCellRect(0, counter, true).x, me.getY(), me.getClickCount(), me.isPopupTrigger());
					tree.dispatchEvent(newME);
					break;
				}
			}
		}
		return false;
	}

	protected void buildFormatList(FileNode[] nodes, Vector targetList, boolean recursively) {
		for (int i = 0; i < nodes.length; i++) {
			FileNode[] children = (FileNode[]) nodes[i].getChildren();
			if (recursively && children != null) {
				buildFormatList(children, targetList, false);
			} else {
				targetList.addElement(nodes[i].getFormat());
			}
		}
	}

	protected void buildFormatList(File[] files, Vector targetList, boolean recursively) {
		for (int i = 0; i < files.length; i++) {
			if (recursively && files[i].isDirectory()) {
				buildFormatList(files[i].listFiles(/* filter? */), targetList, recursively);
			} else {
				MediaFormat format = MediaFormatFactory.createMediaFormat(files[i], MiscellaneousOptionsTab.getEncoding(controller), false);
				if (format != null && format.isValid())
					targetList.addElement(format);
			}
		}
	}

	/**
	 * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to
	 * listen for changes in the ListSelectionModel it maintains. Once a change
	 * in the ListSelectionModel happens, the paths are updated in the
	 * DefaultTreeSelectionModel.
	 */
	class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
		/** Set to true when we are updating the ListSelectionModel. */
		protected boolean updatingListSelectionModel;

		public ListToTreeSelectionModelWrapper() {
			super();
			getListSelectionModel().addListSelectionListener(createListSelectionListener());
		}

		/**
		 * Returns the list selection model. ListToTreeSelectionModelWrapper
		 * listens for changes to this model and updates the selected paths
		 * accordingly.
		 */
		ListSelectionModel getListSelectionModel() {
			return listSelectionModel;
		}

		/**
		 * This is overridden to set <code>updatingListSelectionModel</code>
		 * and message super. This is the only place DefaultTreeSelectionModel
		 * alters the ListSelectionModel.
		 */
		public void resetRowSelection() {
			if (!updatingListSelectionModel) {
				updatingListSelectionModel = true;
				try {
					super.resetRowSelection();
				} finally {
					updatingListSelectionModel = false;
				}
			}
			// Notice how we don't message super if
			// updatingListSelectionModel is true. If
			// updatingListSelectionModel is true, it implies the
			// ListSelectionModel has already been updated and the
			// paths are the only thing that needs to be updated.
		}

		/**
		 * Creates and returns an instance of ListSelectionHandler.
		 */
		protected ListSelectionListener createListSelectionListener() {
			return new ListSelectionHandler();
		}

		/**
		 * If <code>updatingListSelectionModel</code> is false, this will
		 * reset the selected paths from the selected rows in the list selection
		 * model.
		 */
		protected void updateSelectedPathsFromSelectedRows() {
			if (!updatingListSelectionModel) {
				updatingListSelectionModel = true;
				try {
					// This is way expensive, ListSelectionModel needs an
					// enumerator for iterating.
					int min = listSelectionModel.getMinSelectionIndex();
					int max = listSelectionModel.getMaxSelectionIndex();

					clearSelection();
					if (min != -1 && max != -1) {
						for (int counter = min; counter <= max; counter++) {
							if (listSelectionModel.isSelectedIndex(counter)) {
								TreePath selPath = tree.getPathForRow(counter);

								if (selPath != null) {
									addSelectionPath(selPath);
								}
							}
						}
					}
				} finally {
					updatingListSelectionModel = false;
				}
			}
		}

		/**
		 * Class responsible for calling updateSelectedPathsFromSelectedRows
		 * when the selection of the list changse.
		 */
		class ListSelectionHandler implements ListSelectionListener {
			public void valueChanged(ListSelectionEvent e) {
				updateSelectedPathsFromSelectedRows();
			}
		}
	}
}