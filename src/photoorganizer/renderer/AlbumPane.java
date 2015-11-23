/* MediaChest - AlbumPane.java
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
 *  $Id: AlbumPane.java,v 1.44 2014/05/16 03:08:50 cvs Exp $
 */
package photoorganizer.renderer;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import mediautil.gen.MediaFormat;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.jpeg.LLJTran;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Courier;
import photoorganizer.HtmlProducer;
import photoorganizer.IrdControllable;
import photoorganizer.Persistancable;
import photoorganizer.PhotoOrganizer;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.album.Access;
import photoorganizer.album.AlbumModel;
import photoorganizer.album.MediaAccess;
import photoorganizer.courier.FTPCourier;
import photoorganizer.courier.FileCourier;
import photoorganizer.courier.HTTPCourier;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.media.Operations;
import photoorganizer.media.PlaybackRequest;
import photoorganizer.media.Operations.CustomOperation;
import photoorganizer.media.Operations.OperationException;

public class AlbumPane extends JTree implements ActionListener, TreeSelectionListener, Shuffler,
		IrdControllable, Persistancable {
	public static final String[] IRD_NAVIGATIONS = new String[] { Resources.NAV_LEFT, Resources.NAV_RIGHT,
			Resources.NAV_UP, Resources.NAV_DOWN, Resources.NAV_COLAPSE, Resources.NAV_OPEN,
			Resources.NAV_SELECT, Resources.NAV_ACT };

	static final String ALL_MODE = "allMode";

	protected AlbumThumbnailsPanel thumbnails;

	protected PhotoCollectionPanel collectionpanel;

	protected StatusBar statusbar;

	protected boolean allMode;

	public AlbumPane(Controller controller) {
		this.controller = controller;
		access = new MediaAccess(controller);
		setModel(new AlbumModel(access));
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		addTreeSelectionListener(this);
		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0) {
					Point p = ((JViewport) getParent()).getViewPosition();
					new AlbumRightBtnMenu(AlbumPane.this).show(getParent(), e.getX() - p.x, e.getY() - p.y);
				}
			}
		});
		setEditable(true);

		setTransferHandler(new TransferHandler() {
			protected Transferable createTransferable(JComponent c) {

				if (c instanceof JTree) {
					JTree tree = (JTree) c;
					TreePath[] paths = tree.getSelectionPaths();
					return new TransferableTreeNode(paths, tree);
				}
				return null;
			}

			public int getSourceActions(JComponent c) {
				return COPY;
			}

			public boolean importData(JComponent comp, Transferable t) {
				try {
					// TODO: add drops of other types
					TreePath[] paths = ((TransferableTreeNode) t
							.getTransferData(t.getTransferDataFlavors()[0])).getTransferablePaths();
					int targetAlbum = access.getAlbumId(getSelectionPath());
					if (targetAlbum <= 0) {
						for (int i = 0; i < paths.length; i++)
							paths[i] = null;
						return false;
					}
					int srcAlbum;
					for (int i = 0; i < paths.length; i++) {
						srcAlbum = access.getAlbumId(paths[i]);
						if (srcAlbum <= 0 || srcAlbum == targetAlbum) {
							System.err
									.println("An attempt to include the album to itself, or undefined source. /id:"+srcAlbum);
							paths[i] = null;
							continue;
						}
						// System.err.println(access.getNameOfAlbum(srcAlbum)+"-->"+access.getNameOfAlbum(targetAlbum));
						if (access.insertAlbumToAlbum(new int[] { targetAlbum }, srcAlbum) < 0) {
							JOptionPane.showMessageDialog(AlbumPane.this.controller.mediachest,
									Resources.LABEL_ERR_COPYALBUM + '\n' + access.getNameOfAlbum(srcAlbum)
											+ "-->" + access.getNameOfAlbum(targetAlbum),
									Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
							continue;
						}
					}
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}

			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
				return true;
			}

			protected void exportDone(JComponent source, Transferable t, int action) {
				// System.err.println("Export done for "+action+" s "+source+" t
				// "+t);
				if ((action & MOVE) != 0)
					try {
						TreePath[] paths = ((TransferableTreeNode) t.getTransferData(t
								.getTransferDataFlavors()[0])).getTransferablePaths();
						for (int i = 0, srcAlbum; i < paths.length; i++) {
							if (paths[i] != null)
								access.deleteAlbumFrom(new int[] { access
										.getAlbumId(paths[i].getParentPath()) }, access.getAlbumId(paths[i]));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				if ((action & COPY_OR_MOVE) != 0 && t != null)
					((AlbumModel) getModel()).fireTreeStructureChanged(this, new Object[] { getModel()
							.getRoot() }, null, null);
				// super.exportDone(source, t, action);
			}

		});

		try {
			getDropTarget().addDropTargetListener(new DropTargetAdapter() {

				public void dragOver(DropTargetDragEvent dtde) {
					Point p = dtde.getLocation();
					TreePath dstPath = getClosestPathForLocation(p.x, p.y);
					if (dstPath != null)
						setSelectionPath(dstPath);
				}

				public void drop(DropTargetDropEvent dtde) {
				}
			});
		} catch (TooManyListenersException tmle) {
			assert false;
		}
		setDragEnabled(true);
	}

	public void save() {
		controller.getPrefs().setProperty(getClass().getName(), ALL_MODE,
				allMode ? Resources.I_YES : Resources.I_NO);
	}

	public void load() {
		allMode = IniPrefs.getInt(controller.getPrefs().getProperty(getClass().getName(), ALL_MODE), 0) != 0;
	}

	void setTumbnailsPanel(AlbumThumbnailsPanel thumbnails) {
		this.thumbnails = thumbnails;
	}

	void setCollectionPanel(PhotoCollectionPanel collection) {
		this.collectionpanel = collection;
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.MENU_ADDTOCOLLECT)) {
			collectionpanel.add(access.getAlbumContents(access.getAlbumId(getSelectionPath())));
		} else if (cmd.equals(Resources.MENU_ADDTO_IPOD)) {
			IpodPane ipodPanel = (IpodPane) controller.component(controller.COMP_IPODPANEL);
			List list = new ArrayList(100);
			controller.buildMediaList(list, access.getAlbumContents(access.getAlbumId(getSelectionPath())),
					new int[] { MediaFormat.AUDIO, MediaFormat.VIDEO, MediaFormat.STILL }, false);
			ipodPanel.add((MediaFormat[]) list.toArray(new MediaFormat[list.size()]), null,
					ipodPanel.INVALIDATE_ALL);
		} else if (cmd.equals(Resources.MENU_DELETE)) {
			access.deleteAlbumFrom(new int[] { access.getAlbumId(getSelectionPath().getParentPath()) },
					access.getAlbumId(getSelectionPath()));
			invalidateTree();
		} else if (cmd.equals(Resources.MENU_DELETE_COMPLETELY)) {
			access.deleteAlbum(access.getAlbumId(getSelectionPath()));
			invalidateTree();
		} else if (cmd.equals(Resources.MENU_COPY_MOVE)) {
			final int album = access.getAlbumId(getSelectionPath());
			if (album > 0) {
				IniPrefs s = controller.getPrefs();
				final boolean albummove = IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
						AlbumOptionsTab.MOVETOFOLDER), 0) == 1;
				if (JOptionPane.showConfirmDialog(this, String.format(Resources.LABEL_CONFIRM_ALBUM_MVCP,
						albummove ? Resources.INFO_MOVING : Resources.INFO_COPYING, getSelectionPath())) != JOptionPane.YES_OPTION)
					return;
				final String root = (String) s
						.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.ALBUMROOT);
				new Thread(new Runnable() {
					public synchronized void run() {
						if (albummove)
							moveAlbum(album, new File(root != null ? root : "."));
						else
							copyAlbum(album, new File(root != null ? root : "."));
						statusbar.clearInfo();
						statusbar.clearProgress();
					}
				}, "Album copyer").start();
			}
		} else if (cmd.equals(Resources.MENU_SHOW)) {
			final int playAlbumId = access.getAlbumId(getSelectionPath());
			final PlaybackRequest request = new PlaybackRequest(controller.getPrefs());
			new Thread(new Runnable() {
				public void run() {
					PlaybackProperties.doModal(controller, request);
					if (request.matcher != null)
						showAlbum(playAlbumId, request);
				}
			}, "PlayAlbum").start();

		} else if (cmd.equals(Resources.MENU_RENAME)) {
			startEditingAtPath(getSelectionPath());
		} else if (cmd.equals(Resources.MENU_PUBLISH_CURRENT)) {
			allMode = false;
		} else if (cmd.equals(Resources.MENU_SEND_MAIL)) {
			new SendEmailFrame(controller, access.getAlbumContents(access.getAlbumId(getSelectionPath())));
		} else if (cmd.equals(Resources.MENU_PUBLISH_OVERFTP)) {
			publishAlbum(new FTPCourier(controller), access.getAlbumId(getSelectionPath()), allMode);
		} else if (cmd.equals(Resources.MENU_PUBLISH_OVERHTTP)) {
			publishAlbum(new HTTPCourier(controller), access.getAlbumId(getSelectionPath()), allMode);
		} else if (cmd.equals(Resources.MENU_PUBLISH_LOCALY)) {
			publishAlbum(new FileCourier(controller), access.getAlbumId(getSelectionPath()), allMode);
		} else if (cmd.equals(Resources.MENU_POST_ARTICLE)) {
			new Thread(new Runnable() {
				public void run() {
					new PostNewsFrame(controller, access.getAlbumContents(access
							.getAlbumId(getSelectionPath())));
				}
			}, "PostingAlbum").start();
		} else if (cmd.equals(Resources.MENU_UPLOADIMAGE)) {
			new Thread(new Runnable() {
				public void run() {
					Courier courier = null;
					Object[] ac = access.getAlbumContents(access.getAlbumId(getSelectionPath()));
					IniPrefs s = controller.getPrefs();
					// TODO: add request for album name with check box to create
					// the album
					String albumName = (String) JOptionPane.showInputDialog(AlbumPane.this,
							Resources.LABEL_ALBUM_NAME, Resources.TITLE_WEBALBUM,
							JOptionPane.QUESTION_MESSAGE, null, null, "");
					if (albumName == null)
						return;
					try {
						courier = new HTTPCourier(controller, albumName);
						statusbar.displayInfo(Resources.INFO_CONNECTING);
						courier.init();
						String imagePath = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
								WebPublishOptionsTab.HTTP_IMAGEPATH);
						if (imagePath == null)
							imagePath = "";
						else
							courier.checkForDestPath(imagePath);
						statusbar.clearProgress();
						statusbar.displayInfo(Resources.INFO_COPYING);
						statusbar.setProgress(ac.length);
						for (int i = 0; i < ac.length; i++) {
							courier.deliver(ac[i] instanceof File ? ((File) ac[i]).getPath()
									: ((MediaFormat) ac[i]).getFile().getPath(), imagePath);
							statusbar.tickProgress();
						}
						statusbar.clearInfo();
					} catch (IOException e) {
						statusbar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING);
						System.err.println("Exception in Web publishing of album " + e);
						e.printStackTrace();
					} finally {
						statusbar.clearProgress();
						if (courier != null)
							courier.done();
					}
				}
			}, "AlbumHTTPUpload").start();
		} else if (cmd.equals(Resources.MENU_PUBLISH_ALL)) {
			allMode = true;
		} else if (cmd.equals(Resources.MENU_PROPERTIES)) {
			new AlbumProperties(this, access.getAlbumId(getSelectionPath()), access);
		} else if (cmd.equals(Resources.MENU_EXPORTTOCSV)) {
			JOptionPane.showMessageDialog(this, Resources.LABEL_FOR_NEXT_VER);
		} else if (cmd.equals(Resources.MENU_EXPORTTOXML)) {
			JOptionPane.showMessageDialog(this, Resources.LABEL_FOR_NEXT_VER);
		} else if (cmd.equals(Resources.MENU_EXPORTTODSK)) {
			// keep track previous exports and set start dir as previous was
			final int aid = access.getAlbumId(getSelectionPath());
			JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), "/", null,
					aid > 0 ? Resources.TITLE_DESTSELALBUM + access.getNameOfAlbum(aid)
							: Resources.TITLE_DESTWHOLEALBUM, Resources.LABEL_COPY, Resources.TTIP_COPYALBUM, null);
			if (dc.getDirectory() != null) {
				final File tarDir = new File(dc.getDirectory());
				new Thread(new Runnable() {
					public synchronized void run() {
						copyAlbum(aid, tarDir);
						statusbar.clearInfo();
						statusbar.clearProgress();
					}
				}, "Album copier").start();
			}
		} else if (cmd.equals(Resources.MENU_IMPORTCSV)) {
			JOptionPane.showMessageDialog(this, Resources.LABEL_FOR_NEXT_VER);
		} else if (cmd.equals(Resources.MENU_IMPORTDSK)) {
			final int aid = access.getAlbumId(getSelectionPath());
			if (aid < 0) {
				JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_NO_ALBUM_SELECTION,
						Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
				return;

			}
			JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), "/", null, Resources.TITLE_SRCALBUM,
					Resources.LABEL_IMPORT, Resources.TTIP_IMPORTALBUM, null);
			controller.setWaitCursor(getTopLevelAncestor(), false);
			String srcDir = dc.getDirectory();
			if (srcDir != null) {
				String albumRoot = null;
				IniPrefs s = controller.getPrefs();
				if (aid > 0
						&& IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
								AlbumOptionsTab.USEALBUMFOLDER), 0) == 1) {
					albumRoot = (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.ALBUMROOT);
					if (albumRoot != null) {
						// TODO: build a path from root to the current album
						int[] ap = access.getAlbumPath(aid);
						String sp = access.getNameOfAlbum(aid);
						for (int i = 0; i < ap.length; i++)
							sp = access.getNameOfAlbum(ap[i]) + File.separatorChar + sp;
						File file = new File(albumRoot, sp);
						file.mkdirs();
						albumRoot = file.toString();
						System.err.println("result path " + sp);
					}
				}
				importAlbum(aid, srcDir, albumRoot, IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
						AlbumOptionsTab.MOVETOFOLDER), 0) == 1);
			}
		} else if (cmd.equals(Resources.MENU_REFRESH)) {
			invalidateTree();
		}
	}

	public AlbumSelectionDialog getSelectionDialog() {
		if (albumselection == null)
			createSelectionDialog();
		return albumselection;
	}

	final synchronized void createSelectionDialog() {
		if (albumselection == null)
			albumselection = new AlbumSelectionDialog(controller);
	}

	void copyAlbum(int album, File parent) {
		Object[] content = access.getAlbumContents(album);
		File albumdir;
		if (album != 0) {
			albumdir = new File(parent, access.getNameOfAlbum(album));
			albumdir.mkdir();
		} else
			albumdir = parent;
		statusbar.displayInfo(Resources.INFO_COPYING + "->" + albumdir);
		statusbar.setProgress(content.length);
		for (int i = 0; i < content.length; i++)
			try {
				File f = content[i] instanceof File ? (File) content[i] : ((MediaFormat) content[i])
						.getFile();
				Stream.copyFile(f, new File(albumdir, f.getName()));
				statusbar.tickProgress();
			} catch (ClassCastException cce) {
			} catch (IOException ioe) {
				System.err.println("Exception " + ioe + " in copying " + content[i]);
			}
		int[] albums = access.getAlbumsId(album);
		for (int i = 0; i < albums.length; i++)
			copyAlbum(albums[i], albumdir);
	}

	void moveAlbum(int album, File parent) {
		Object[] content = access.getAlbumContents(album);
		File albumdir = new File(parent, access.getNameOfAlbum(album));
		albumdir.mkdir();
		statusbar.displayInfo(Resources.INFO_MOVING + "->" + albumdir);
		statusbar.setProgress(content.length);
		for (int i = 0; i < content.length; i++) {
			File f = content[i] instanceof File ? (File) content[i] : ((MediaFormat) content[i]).getFile();
			File oldname = new File(f, "");
			// below should be atransaction
			if (f.renameTo(new File(albumdir, f.getName()))) {
				access.renamePictureTo(album, oldname.getPath(), f.getPath());
			} else
				System.err.println("File " + f.getPath() + " hasn't been renamed");
		}
		int[] albums = access.getAlbumsId(album);
		for (int i = 0; i < albums.length; i++)
			moveAlbum(albums[i], albumdir);
	}

	public void publishAlbum(final Courier courier, final int albumId, final boolean child_also) {
		// TODO: request album html name unless it's in ASCII
		new Thread(new Runnable() {
			public void run() {
				try {
					new HtmlProducer(controller).produce(courier, albumId, child_also);
				} catch (IOException ioe) {
					statusbar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING);
					System.err.println("Exception in Inet publishing of album " + ioe);
					ioe.printStackTrace();
				}
			}
		}, "Album publishing").start();
	}

	public void showAlbum(int albumId, PlaybackRequest request) {
		// TODO: consider different directions of traveling
		Object[] playList;
		if (request.shuffled) {
			playList = Shuffler.Instance.getShuffledList(new Object[] { new Integer(albumId) }, this);
		} else {
			int[] albumIds = access.getAlbumsId(albumId);
			for (int i = 0; i < albumIds.length; i++)
				showAlbum(albumIds[i], request);
			playList = access.getAlbumContents(albumId);
		}

		controller.playMediaList(playList, request);
	}

	public void fill(List list, Object[] expandables) {
		for (int i = 0; i < expandables.length; i++) {
			if (expandables[i] instanceof Integer)
				addShuffledAlbums(list, ((Integer) expandables[i]).intValue());
			else if (expandables[i] instanceof File)
				list.add(expandables[i]);
		}
	}

	protected void addShuffledAlbums(List list, int albumId) {
		Object[] content = access.getAlbumContents(albumId);
		for (int i = 0; i < content.length; i++)
			list.add(content[i]);
		int[] albumIds = access.getAlbumsId(albumId);
		for (int i = 0; i < albumIds.length; i++)
			addShuffledAlbums(list, albumIds[i]);
	}

	public void valueChanged(TreeSelectionEvent e) {
		controller.setWaitCursor(this, true);
		thumbnails.updateImages(access.getAlbumContents(access.getAlbumId(e.getPath())));
		controller.setWaitCursor(this, false);
	}

	public boolean addToAlbum1(MediaFormat[] formats, final TreePath[] paths, boolean overrideMove) {
		IniPrefs s = controller.getPrefs();
		final boolean picmove = overrideMove ? false : IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
				AlbumOptionsTab.MOVETOFOLDER), 0) == 1;
		final boolean maintainfld = IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
				AlbumOptionsTab.USEALBUMFOLDER), 0) == 1;
		final String root = (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.ALBUMROOT);
		final boolean auto_rotate = IniPrefs.getInt(s.getProperty(RenameOptionsTab.SECNAME,
				RenameOptionsTab.AUTOROTATE), 0) == 1;
		Operations ops = (Operations) controller.getService(Operations.NAME);
		for (int i = 0; paths != null && i < paths.length; i++) {
			if (maintainfld) {
				Object[] _ps = paths[i].getPath();
				String target = root == null ? ("." + File.separatorChar) : root;
				for (int k = 1; k < _ps.length; k++)
					// we skip root element
					target += File.separator + _ps[k];
				File targetDir = new File(target);
				if (!targetDir.exists() && !targetDir.mkdirs()) {
					System.err.println("Album directory " + target
							+ " can not be created, check album root location.");
					statusbar.tickProgress();
					continue;
				}
				String mask = PhotoCollectionPanel.getRenameMask(this, s, " " + target, false);
				try {
					ops.changeMedia(formats, target, mask, auto_rotate ? Operations.AUTO : -1,
							picmove == false, new CustomOperation() {
								public boolean canDelete(MediaFormat media) {
									return !access.belongsToAlbum(media);
								}

								public void customOperation(MediaFormat media) {
								}
							});
					access.insertMediasToAlbum(access.getAlbumId(paths[i]), formats);
				} catch (OperationException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	public boolean addToAlbum(MediaFormat[] formats, final TreePath[] paths, boolean overrideMove) {
		IniPrefs s = controller.getPrefs();
		final boolean picmove = overrideMove ? false : IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
				AlbumOptionsTab.MOVETOFOLDER), 0) == 1;
		final boolean maintainfld = IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
				AlbumOptionsTab.USEALBUMFOLDER), 0) == 1;
		final String root = (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.ALBUMROOT);
		final boolean auto_rotate = IniPrefs.getInt(s.getProperty(RenameOptionsTab.SECNAME,
				RenameOptionsTab.AUTOROTATE), 0) == 1;
		System.err.printf("adding %s%n", Arrays.toString(formats));
		for (int i = 0; paths != null && i < paths.length; i++) {
			if (maintainfld) {
				Object[] _ps = paths[i].getPath();
				String target = root == null ? ("." + File.separatorChar) : root;
				for (int k = 1; k < _ps.length; k++)
					// we skip root element
					target += File.separator + _ps[k];
				File targetDir = new File(target);
				if (!targetDir.exists() && !targetDir.mkdirs())
					System.err.println("Album directory " + target
							+ " can not be created, check album root location.");
				final String value = PhotoCollectionPanel.getRenameMask(this, s, " " + target, false);
				if (value == null) // cancel chosen
					return false;
				FileNameFormat fnf = new FileNameFormat(value, true);
				// TODO add work with real model to achieve corresponding update
				// Possible problem, a user in other thread can continue working
				// on the model
				statusbar.clearProgress();
				statusbar.displayInfo(picmove ? Resources.INFO_MOVING : Resources.INFO_COPYING);
				statusbar.setProgress(formats.length);
				for (int j = 0; j < formats.length; j++) {
					File file = new File(target, FileNameFormat.makeValidPathName(fnf.format(formats[j])));
					int transOper = auto_rotate && formats[j] instanceof BasicJpeg ? Operations
							.mediaInfoToTransformOp(formats[j]) : -1;
					if (transOper > 0 && transOper != LLJTran.NONE) {
						if (((BasicJpeg) formats[j]).transform(file.getPath(), transOper, true) == false)
							System.err.println("Problem in rotation file " + formats[j].getFile()
									+ " to album location " + file);
						else {
							if (picmove && i == 0 && !access.belongsToAlbum(formats[j]))
								if (formats[j].getFile().delete() == false)
									System.err.println("Problem in deleting file " + formats[j].getFile()
											+ " after transformation.");
							formats[j] = MediaFormatFactory.createMediaFormat(file);
						}
					} else {
						boolean doCpy = true;
						if (picmove && i == 0 && !access.belongsToAlbum(formats[j])) {
							if (!formats[j].renameTo(file)) {
								System.err.println("Problem in moving file " + formats[j].getFile()
										+ " to album location " + file);
								
							} else
								doCpy = false;
						} 
						if(doCpy) {
							try {
								Stream.copyFile(formats[j].getFile(), file);
								formats[j] = MediaFormatFactory.createMediaFormat(file);
								if (formats[j] == null)
									throw new IOException("Cat'r recreate format from: "+file);
							} catch (IOException ioe) {
								System.err.println("Problem in copying file " + formats[j].getFile() + " to "
										+ file + " album. (" + ioe + ')');
							}
						}
					}
					// TODO add to album here
					statusbar.tickProgress();
				}
			}
			access.insertMediasToAlbum(access.getAlbumId(paths[i]), formats);
			statusbar.clearProgress();
			statusbar.displayInfo("");
		}
		return true;
	}

	void deletePicture(String name) {
		access.deletePicture(access.getAlbumId(getSelectionPath()), name);
	}

	void setCommentTo(String name, String comment) {
		access.setPictureComment(access.getAlbumId(getSelectionPath()), name, comment);
	}

	public String getCommentOf(String name) {
		return access.getPictureComment(access.getAlbumId(getSelectionPath()), name);
	}

	public void invalidateTree() {
		((AlbumModel) getModel()).fireTreeStructureChanged(this, new Object[] { getModel().getRoot() }, null,
				null);
		// getModel().reload();
	}

	public Access getAccess() {
		return access;
	}

	public void importAlbum(int pai, String dir, String albumDir, boolean movePic) {
		String[] content = new File(dir).list();
		if (content == null) {
			JOptionPane.showMessageDialog(this, "Directory " + dir+" is unaccessible");
			return;
		}
		Vector medias = new Vector(content.length);
		for (int i = 0; i < content.length; i++) {
			File item = new File(dir, content[i]);
			if (item.isDirectory()) {
				// create album
				int naid = access.findAlbumIn(pai, content[i]);
				if (naid < 0) {
					naid = access.createAlbum(pai, content[i]);
					access.insertAlbumToAlbum(new int[] { pai }, naid);
				}
				File nodeDir = null;
				if (albumDir != null) {
					// find and create album folder, if needed
					nodeDir = new File(albumDir, content[i]);
					nodeDir.mkdirs();
				}
				importAlbum(naid, item.toString(), nodeDir == null ? (String) null : nodeDir.toString(),
						movePic);
			} else {
				MediaFormat af = MediaFormatFactory.createMediaFormat(item);
				if (af != null && af.isValid()) {
					if (albumDir != null) {
						File newLoc = new File(albumDir, content[i]);
						boolean succ = false;
						if (movePic)
							succ = item.renameTo(newLoc);
						else
							try {
								Stream.copyFile(item, newLoc);
								succ = true;
							} catch (IOException ioe) {
								System.err.println(ioe);
							}
						if (succ) {
							MediaFormat naf = MediaFormatFactory.createMediaFormat(newLoc);
							if (naf != null && naf.isValid())
								af = naf;
						}
					}
					medias.addElement(af);
				}
			}
		}
		if (medias.size() > 0)
			access.insertMediasToAlbum(pai, (MediaFormat[]) medias.toArray(new MediaFormat[medias.size()]));
	}

	// remote controllable
	public String getName() {
		return Resources.COMP_ALBUM_VIEWER;
	}

	public String toString() {
		return getName();
	}

	public Iterator getKeyMnemonics() {
		return Arrays.asList(IRD_NAVIGATIONS).iterator();
	}

	public boolean doAction(String keyCode) {
		return false;
	}

	public void bringOnTop() {
		controller.selectTab(Resources.TAB_ALBUM);
	}

	/*
	 * void copyAlbumTo(String dir, int pai) { int[] ai =
	 * access.getAlbumsId(pai); for(int i=0; i<ai.length; i++) { File nodeDir =
	 * new FIle(dir, access.getNameOfAlbum(ai[i])); nodeDir.mkdir();
	 * copyAlbumTo(nodeDir.toString(), ai[i]); } File[] a =
	 * access.getAlbumContents(pai);
	 * statusbar.displayInfo(Resources.INFO_COPYING+"->"+dir);
	 * statusbar.setProgress(a.length); for(int i=0; i<a.length; i++) try {
	 * Controller.copyFile(a[i], new File(dir, a[i].getName()));
	 * statusbar.tickProgress(); } catch(IOException ioe) {
	 * System.err.println("Exception "+ioe+" in copying "+a[i]); } }
	 */

	class AlbumRightBtnMenu extends JPopupMenu {
		AlbumRightBtnMenu(ActionListener listener) {
			controller.getUiUpdater().notify(getSelectionPath() != null, UiUpdater.ALBUM_SELECTED);
			JMenuItem item;
			add(item = new JMenuItem(Resources.MENU_ADDTOCOLLECT));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_ADDTO_IPOD));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_SHOW));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_COPY_MOVE));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_DELETE));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			add(item = new JMenuItem(Resources.MENU_DELETE_COMPLETELY));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			add(item = new JMenuItem(Resources.MENU_RENAME));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			addSeparator();

			JMenu menu = PhotoOrganizer.getPublishMenu(controller, listener, UiUpdater.ALBUM_SELECTED);// new
			// JMenu(Resources.MENU_PUBLISH);
			menu.addSeparator();
			ButtonGroup group = new ButtonGroup();
			menu.add(item = new JRadioButtonMenuItem(Resources.MENU_PUBLISH_CURRENT));
			group.add(item);
			item.setSelected(allMode == false);
			item.addActionListener(listener);
			menu.add(item = new JRadioButtonMenuItem(Resources.MENU_PUBLISH_ALL));
			group.add(item);
			item.setSelected(allMode);
			item.addActionListener(listener);
			add(menu);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_PROPERTIES));
			item.addActionListener(listener);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.ALBUM_SELECTED));
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_REFRESH));
			item.addActionListener(listener);
		}
	}

	private Controller controller;

	private AlbumSelectionDialog albumselection;

	MediaAccess access;

	static class TransferableTreeNode implements Transferable {
		public static DataFlavor TREE_NODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
				+ ";class=java.lang.String", "Media Album");

		private static DataFlavor[] albumNodeFlavors = new DataFlavor[] { TREE_NODE_FLAVOR };

		TransferableTreeNode(TreePath[] treePaths, JTree tree) {
			this.treePaths = treePaths;
			this.tree = tree;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return albumNodeFlavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(albumNodeFlavors[0]);
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (flavor.equals(albumNodeFlavors[0]))
				return this;
			throw new UnsupportedFlavorException(flavor);
		}

		public TreePath[] getTransferablePaths() {
			return treePaths;
		}

		protected TreePath[] treePaths;

		protected JTree tree;
	}

}