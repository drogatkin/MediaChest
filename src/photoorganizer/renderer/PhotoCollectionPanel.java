/* MediaChest - PhotoCollectionPanel
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
 *  $Id: PhotoCollectionPanel.java,v 1.85 2013/12/15 07:51:39 cvs Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreePath;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.jpeg.CIFF;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.JFXX;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Courier;
import photoorganizer.HtmlProducer;
import photoorganizer.IrdControllable;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.courier.FTPCourier;
import photoorganizer.courier.FileCourier;
import photoorganizer.courier.HTTPCourier;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.Thumbnail;
import photoorganizer.ipod.ITunesDB;
import photoorganizer.ipod.IpodControl;
import photoorganizer.media.ContentMatcher;
import photoorganizer.media.PlaybackRequest;

// TODO: rename to MediaCollectionPanel
public class PhotoCollectionPanel extends JTable implements ActionListener, Persistancable, IrdControllable {
	final static String SECNAME = "PhotoCollectionPanel";

	final static String COLWIDTH = "ColumnWidths";

	final static int TOO_MANY_WINDOWS = 20;

	private Controller controller;

	private Point lastmouse;

	private CollectionThumbnailsPanel thumbnailspanel;

	private AlbumPane albumpane;

	private StatusBar statusbar;

	private Class fclass;

	private boolean keeporigmarkers, transformmove, transformaddsel;

	public final static String DEFTNMASK = "tumbnail%00c.jpg";

	public static String LastHtmlName = "index";

	@SuppressWarnings("serial")
	public PhotoCollectionPanel(Controller controller) {
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.controller = controller;
		statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		thumbnailspanel = (CollectionThumbnailsPanel) controller.component(Controller.COMP_THUMBCOLLCTPANEL);
		if (thumbnailspanel != null)
			thumbnailspanel.setCollection(this);
		albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
		albumpane.setCollectionPanel(this);
		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				int m = e.getModifiers();
				lastmouse = new Point(e.getX(), e.getY());
				if ((m & InputEvent.BUTTON3_MASK) > 0)
					getRMouseMenu().show(PhotoCollectionPanel.this, e.getX(), e.getY());
				else if (e.getClickCount() == 2) {
					actionPerformed(new ActionEvent(this, 0, Resources.MENU_SHOW));
				}
			}
		});
		setAutoCreateRowSorter(true);
		setModel(new CollectionModel(controller));
		//TableSorter sorter = new TableSorter(new CollectionModel(controller));
		// 
		//setModel(sorter);
		//sorter.setTableHeader(getTableHeader());
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
				if (f < 0)
					return;
//				TableSorter ts = (TableSorter) getModel();
//				CollectionModel model = (CollectionModel) ts.getTableModel();
				CollectionModel model = (CollectionModel)getModel();
//				int m = ts.getRowCount();
				int m= model.getRowCount();
				if (m == 0)
					return;
				if (l > m - 1)
					l = m - 1;
				Thumbnail tn;
				boolean selected, were_sel = false;
				for (int i = f; i <= l; i++) {
					//int ti = ts.modelIndex(i);
					int ti = i;
					if ((tn = (Thumbnail) model.getElementAt(ti)) != null) {
						tn.select(selected = isRowSelected(ti));
						were_sel |= selected;
					}
				}
				if (!were_sel) // we can still have some selections
					were_sel = getSelectedRowCount() > 0;
				PhotoCollectionPanel.this.controller.getUiUpdater().notify(were_sel, UiUpdater.SELECTION_SELECTED);
				PhotoCollectionPanel.this.controller.getUiUpdater().notify(were_sel, UiUpdater.IS_SELECTION);
			}
		});
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == e.VK_DELETE) {
					actionPerformed(new ActionEvent(this, 3, Resources.MENU_DELETE));
				}
			}
		});
		setTransferHandler(new TransferHandler() {
			public boolean importData(JComponent comp, Transferable t) {
				try { // 
					java.util.List fileList = (java.util.List) t.getTransferData(DataFlavor.javaFileListFlavor);
					add((File[]) fileList.toArray(new File[fileList.size()]));
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}

			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
				// TODO: extend to accept URLs also
				for (int i = 0; i < transferFlavors.length; i++)
					if (transferFlavors[i].isFlavorJavaFileListType())
						return true;
				return false;
			}
		});

		setDragEnabled(true);
		controller.getUiUpdater().registerScheduledUpdater(UiUpdater.SRC_PHOTO_AVAL, new UiUpdater.StateChecker() {
			public boolean isEnabled() {
				String path = (String) PhotoCollectionPanel.this.controller.getPrefs().getProperty(
						MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.FC_FOLDER);
				if (path != null && path.length() > 0)
					return new File(path).exists();
				// TODO add auto discovery
				return true;
			}

			public void setEnabled(boolean b) {
			}
		});
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		//TableSorter ts = (TableSorter) getModel();
		//final CollectionModel model = (CollectionModel) ts.getTableModel();
		final CollectionModel model = (CollectionModel) getModel();
		// CollectionModel model = (CollectionModel) getModel();
		final int[] selections = (int[]) getSelectedRows().clone();
		//System.err.printf("Selected rows :%s%n", Arrays.toString(selections));
		if (cmd.equals(Resources.MENU_REVERSE_SELECT)) {
			int sel = rowAtPoint(lastmouse);
			if (sel >= 0) {
				if (isRowSelected(sel))
					removeRowSelectionInterval(sel, sel);
				else
					addRowSelectionInterval(sel, sel);
				controller.getUiUpdater().notify(getSelectedRowCount() > 0, UiUpdater.SELECTION_SELECTED);
			}
		} else if (cmd.equals(Resources.MENU_SELECTALL)) {
			selectAll();
		} else if (cmd.equals(Resources.MENU_CF_TOCOLLECT) || // from tool bar
				(a.getSource() instanceof JButton && ((JButton) a.getSource()).getToolTipText().equals(
						Resources.MENU_CF_TOCOLLECT))) {
			final String path = (String) controller.getPrefs().getProperty(MiscellaneousOptionsTab.SECNAME,
					MiscellaneousOptionsTab.FC_FOLDER);
			if (path != null && path.length() > 0) {
				addPath(new File(path));
			} else {
				// TODO autodiscovery of
				// pictures like E:\DCIM\102CDPFQ or /media/DACE-470F/DCIM
				// movies like E:\AVCHD\BDMV\STREAM	
				FileSystemView fsv = FileSystemView.getFileSystemView();
				File cn = IpodControl.getComputerNode(fsv, fsv.getRoots(), 0);
				File[] roots = cn != null ? fsv.getFiles(cn, true) : File.listRoots();
				//System.err.println("roots :"+Arrays.toString(roots) + " for "+cn);

				for (File f : roots) {
					//System.err.println("Descr :"+fsv.getSystemTypeDescription(f)+" of "+f);
					if (fsv.getSystemTypeDescription(f) == null
							|| fsv.getSystemTypeDescription(f).indexOf("emovable") >= 0) {
						File picsFolder = new File(f, "DCIM");
						if (picsFolder.exists() && picsFolder.isDirectory()) {
							addPath(picsFolder);
						} else {
							picsFolder = new File(f, "AVCHD/BDMV/STREAM");
							if (picsFolder.exists() && picsFolder.isDirectory()) {
								addPath(picsFolder);
							}
						}
					}
				}
			}
		} else if (cmd.equals(Resources.MENU_EXTRACTMARKERS)) {
			extractMarkers(selections);
		} else if (cmd.equals(Resources.MENU_ADDTOALBUM)) {
			AlbumSelectionDialog asd = albumpane.getSelectionDialog();
			asd.setTitle(Resources.TITLE_SELECT_ALBUM + ":" + selections.length);
			asd.setVisible(true);
			final TreePath[] tps = asd.getSelectedAlbums();
			if (tps != null) {
				IniPrefs s = controller.getPrefs();
				final boolean removeafter = IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME,
						AlbumOptionsTab.MOVETOFOLDER), 0) == 1
						&& IniPrefs.getInt(s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.USEALBUMFOLDER), 0) == 1;
				final MediaFormat[] medias = new MediaFormat[selections.length];
				int minr = 0, maxr = 0;
				for (int i = 0; i < selections.length; i++) {
					//int tsi = ts.modelIndex(selections[i]);
					int tsi = selections[i];
					medias[i] = ((Thumbnail) model.getElementAt(tsi)).getFormat();
					if (removeafter) {
						((Thumbnail) model.getElementAt(tsi)).select(false);
						model.markDelete(tsi);
						if (tsi > maxr)
							maxr = tsi;
						else if (tsi < minr)
							minr = tsi;
					}
				}
				final int fmin = minr;
				final int fmax = maxr;
				final SwingWorker worker = new SwingWorker<Boolean, Object>() {
					boolean result;
					Throwable ex;
					//Runs on the event-dispatching thread.
					@Override
					protected void done() {
						if (result) {
							if (removeafter) {
								model.removeAllMarked();
								model.fireTableRowsDeleted(fmin, fmax);
								controller.getUiUpdater().notify(false, UiUpdater.SELECTION_SELECTED);
							} else
								model.fireTableRowsUpdated(fmin, fmax);
						} else
							statusbar.flashInfo(String.format(Resources.INFO_ERROR, ex==null?"operation canceled":ex.toString()));
					}

					@Override
					protected Boolean doInBackground() throws Exception {
						try {
							result = albumpane.addToAlbum(medias, tps, false);
							return result;
						} catch(Error e) {
							ex = e;
							e.printStackTrace();
							throw e;
						} catch(RuntimeException re) {
							ex = re;
							re.printStackTrace();
							throw re;							
						}
					}
				};
				worker.execute();
			}
		} else if (cmd.equals(Resources.MENU_ADDTO_IPOD)) {
			IpodPane ipodPanel = (IpodPane) controller.component(controller.COMP_IPODPANEL);
			MediaFormat[] medias = new MediaFormat[selections.length];
			for (int i = 0; i < selections.length; i++) {
				//medias[i] = ((Thumbnail) model.getElementAt(ts.modelIndex(selections[i]))).getFormat();
				medias[i] = ((Thumbnail) model.getElementAt(selections[i])).getFormat();
			}
			// use controller method executeInBackground(Runnable, status);
			ipodPanel.add(medias, null, ipodPanel.INVALIDATE_ALL);

		} else if (cmd.equals(Resources.MENU_RENAME)) {
			rename(selections, false);
		} else if (cmd.equals(Resources.MENU_COPY)) {
			rename(selections, true);
		} else if (cmd.equals(Resources.MENU_TORIPPER)) {
			copyToRipper(selections);
		} else if (cmd.equals(Resources.MENU_DELETE)) {
			delete(selections);
		} else if (cmd.equals(Resources.MENU_SHOW)) {
			int sel = rowAtPoint(lastmouse);
			if (sel >= 0) {
				//MediaFormat format = (MediaFormat) ((Thumbnail) model.getElementAt(ts.modelIndex(sel))).getFormat();
				MediaFormat format = (MediaFormat) ((Thumbnail) model.getElementAt(sel)).getFormat();
				if (format != null && format instanceof BasicJpeg) {
					((PhotoImagePanel) controller.component(Controller.COMP_IMAGECOLLCTPANEL)).updateView(format);
					return;
				}				
				show(selections);
			}
		} else if (cmd.equals(Resources.MENU_PROPERTIES) || cmd.equals(Resources.MENU_EDIT_PROPS)) {
			java.util.List medias = new ArrayList(selections.length);
			for (int i = 0; i < selections.length; i++) {
				//Thumbnail tn = (Thumbnail) model.getElementAt(ts.modelIndex(selections[i]));
				Thumbnail tn = (Thumbnail) model.getElementAt(selections[i]);
				if (tn != null) // TODO: investigate why it can happen
					if (cmd.equals(Resources.MENU_EDIT_PROPS))
						medias.add(tn.getFormat()); // check if MP3
					else {
						PropertiesPanel.showProperties(tn.getFormat(), controller);
						if (i >= TOO_MANY_WINDOWS)
							break;
					}
			}
			if (cmd.equals(Resources.MENU_EDIT_PROPS)) {
				Id3TagEditor.editTag(controller, (MediaFormat[]) medias.toArray(new MediaFormat[medias.size()]));
			}
		} else if (cmd.equals(Resources.MENU_EXTRACTTUMBNAILS)) {
			BasicJpeg format;
			IniPrefs s = controller.getPrefs();
			String destpath = (String) s.getProperty(RenameOptionsTab.SECNAME, RenameOptionsTab.DESTFOLDER);
			if (destpath == null || destpath.length() == 0)
				destpath = controller.getPrefs().getHomeDirectory();
			String m = (String) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FILEMASK);
			if (m == null || m.length() == 0)
				m = DEFTNMASK;
			FileNameFormat fnf = new FileNameFormat(m, true);
			for (int i = 0; i < selections.length; i++) {
				// TODO: casting
			//	format = (BasicJpeg) ((Thumbnail) model.getElementAt(ts.modelIndex(selections[i]))).getFormat();
				format = (BasicJpeg) ((Thumbnail) model.getElementAt(selections[i])).getFormat();
				AbstractImageInfo ii = format.getImageInfo();
				try {
					OutputStream os = null;
					System.err.println("Saving in dir:" + destpath); // !!
					if (ii != null)
						ii.saveThumbnailImage(os = new FileOutputStream(new File(destpath, FileNameFormat
								.makeValidPathName(fnf.format(format), ii.getThumbnailExtension()))));
					if (os != null) {
						os.flush(); // just in case
						os.close();
					}
				} catch (IOException ioe) {
					System.err.println("" + ioe);
				}
			}
		} else if (cmd.equals(Resources.MENU_SEND_MAIL)) {
			if (selections.length == 0)
				return;
			new SendEmailFrame(controller, selectionsToFiles(selections));
		} else if (cmd.equals(Resources.MENU_PUBLISH_OVERFTP)) {
			publish(new FTPCourier(controller), selections);
		} else if (cmd.equals(Resources.MENU_PUBLISH_OVERHTTP)) {
			publish(new HTTPCourier(controller), selections);
		} else if (cmd.equals(Resources.MENU_PUBLISH_LOCALY)) {
			publish(new FileCourier(controller), selections);
		} else if (cmd.equals(Resources.MENU_POST_ARTICLE)) {
			if (selections.length == 0)
				return;
			new Thread(new Runnable() {
				public void run() {
					new PostNewsFrame(controller, selectionsToFormats(selections));
				}
			}, "Posting").start();
		} else if (cmd.equals(Resources.MENU_UPLOADIMAGE)) {
			if (selections.length == 0)
				return;
			final IniPrefs s = controller.getPrefs();
			// TODO: add request for album name with check box to create the
			// album, provide album list
			final String albumName = (String) JOptionPane.showInputDialog(this, Resources.LABEL_ALBUM_NAME,
					Resources.TITLE_WEBALBUM, JOptionPane.QUESTION_MESSAGE, null, null, "");
			if (albumName != null) {
				final File[] files = selectionsToFiles(selections);
				new Thread(new Runnable() {
					public void run() {
						Courier courier = null;
						try {
							// TODO some other couriers can be  used here
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
							statusbar.setProgress(files.length);
							for (int i = 0; i < files.length; i++) {
								courier.deliver(files[i].getPath(), imagePath);
								statusbar.tickProgress();
							}
							statusbar.clearInfo();
						} catch (IOException e) {
							statusbar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING);
							System.err.println("Exception in Web publishing " + e);
							e.printStackTrace();
						} finally {
							statusbar.clearProgress();
							if (courier != null)
								courier.done();
						}
					}
				}, "HTTPUpload").start();
			}
		} else if (cmd.equals(Resources.MENU_PRINT)) {
			controller.print(selectionsToFiles(selections));
		} else if (cmd.equals(Resources.MENU_SENDTO)) {
			new SendEmailFrame(controller, selectionsToFiles(selections));
		} else if (cmd.equals(Resources.MENU_PLAY_LIST)) {
			showList(getSelectedRow());
		} else if (cmd.equals(Resources.MENU_COPY_LOCATION)) {
			File[] files = selectionsToFiles(selections);
			StringBuffer selBuf = new StringBuffer(files[0].getPath());;
			for (int i=1; i<files.length; i++)
				selBuf.append(File.pathSeparatorChar).append(files[i].getPath());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();			
			clipboard.setContents(new StringSelection(selBuf.toString()), null);
		} else {
			int op;
			if ((op = Controller.convertCmdToTrnasformOp(cmd)) != -1) {
				transform(selections, op);
			}
		}
	}

	protected void addPath(final File path) {
		new SwingWorker() {

			@Override
			protected void done() {
				statusbar.clearProgress();
				updateStatusBar();
			}

			@Override
			protected Object doInBackground() throws Exception {
				statusbar.setProgress(-1);
				add(new File[] { path });
				return null;
			}
			
		}.execute();
		
	}
	
	protected String getHtmlName() {
		String htmlname = (String) JOptionPane.showInputDialog(this, Resources.LABEL_HTML_NAME,
				Resources.TITLE_CREATEHTML, JOptionPane.QUESTION_MESSAGE, null, null, LastHtmlName);
		if (htmlname != null) {
			if (htmlname.indexOf('.') < 0)
				htmlname += Resources.EXT_HTML;
			LastHtmlName = htmlname;
		}
		return htmlname;
	}

	public int add(PlaybackRequest request) {
		return add((File[]) request.playbackList, request, null);
	}

	public int add(Object[] fs) {
		return add(fs, null, null);
	}

	// TODO: optimize performance and memory usage
	public int add(Object[] fs, PlaybackRequest request, PlaybackProperties playBackProperties) {
		//TableSorter ts = (TableSorter) getModel();
		// CollectionModel model = (CollectionModel) ts.getTableModel();
		CollectionModel model = (CollectionModel) getModel();
		int end, start = model.getRowCount();
		end = start-1;
		MediaFormat[] formats = new MediaFormat[fs.length];
		String enc = MiscellaneousOptionsTab.getEncoding(controller);
		for (int i = 0; i < fs.length; i++) {
			MediaFormat format = null;
			if (fs[i] instanceof MediaFormat)
				format = (MediaFormat) fs[i];
			if (format == null && fs[i] instanceof File) {
				//System.err.printf("Process file %s%n", fs[i]);
				if (((File) fs[i]).isFile())
					format = MediaFormatFactory.createMediaFormat((File) fs[i], enc, false);
				else if (((File) fs[i]).isDirectory() && (fs.length == 1 || request == null || (request != null && request.recursive))) {
					end += add(((File) fs[i]).listFiles(), request, playBackProperties);
					continue;
				}
			}
			if (format != null && format.isValid()) {
				if (format.getType() == MediaFormat.STILL) {
					formats[i] = format;
					// TODO avoid dup of this code
					MediaInfo mi = format.getMediaInfo();
					if (mi instanceof AbstractImageInfo)
						((AbstractImageInfo) mi).setTimeZone(controller.getTimeZone());
					end++;
				} else {
					if (request == null || !request.requestOnCopy) {
						// recalculate available space
						formats[i] = format;
						end++;
					} else {
						if (playBackProperties == null)
							playBackProperties = PlaybackProperties.doPropertiesDialog(controller, (Frame) null);
						ContentMatcher matcher = playBackProperties.getMatcher();
						if (matcher != null && matcher.match(format)) {
							// recalculate available space
							formats[i] = format;
							end++;
						}
					}
				}
			} else
				System.err.printf("Media %s for %s is skipped%n", format, fs[i]);
		}
		thumbnailspanel.addImage(formats);
		//System.err.printf("Fire %d - %d%n", start, end);
		if (end >= start)
			model.fireTableRowsInserted(start, end);
		// ts.fireTableRowsInserted(start, end);
		return end;
	}

	// called from thumbnails panel
	public void add(MediaFormat format) {
		add(new Object[] { format });
	}

	protected void updateStatusBar() {
		IniPrefs s = controller.getPrefs();
		if (s
				.getInt(s.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.PLAYLIST_TYPE),
						MediaOptionsTab.AUDIO_MEDIA) == MediaOptionsTab.AUDIO_MEDIA) {
			statusbar.displayMetric(MP3.convertTime(thumbnailspanel.getTime()));
		} else {
			statusbar.displayMetric(BasicIo.convertLength(thumbnailspanel.getLength()));
		}
	}

	String transformName(File file, int op) {
		IniPrefs s = controller.getPrefs();
		String destpath = (String) s.getProperty(RenameOptionsTab.SECNAME, RenameOptionsTab.DESTFOLDER);
		if (destpath == null || destpath.length() == 0)
			destpath = controller.getPrefs().getHomeDirectory();
		// TODO: use an array
		String m = (String) s.getProperty(TransformOptionsTab.SECNAME, TransformOptionsTab.MASK);
		if (m != null) {
			MediaFormat format = MediaFormatFactory.createMediaFormat(file, MiscellaneousOptionsTab
					.getEncoding(controller), false);
			if (format != null && format.isValid())
				return destpath + File.separatorChar
						+ FileNameFormat.makeValidPathName(new FileNameFormat(m, op, true).format(format));
		}
		return destpath + File.separatorChar + file.getName();
	}

	int findIndexOf(Thumbnail im) {
		//TableSorter ts = (TableSorter) getModel();
		//CollectionModel model = (CollectionModel) ts.getTableModel();
		CollectionModel model = (CollectionModel) getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			//if (model.getElementAt(ts.modelIndex(i)) == im)
			if (model.getElementAt(i) == im)
				return i;
		}
		return -1;
	}

	File[] selectionsToFiles(int[] selections) {
		File[] files = new File[selections.length];
		//TableSorter ts = (TableSorter) getModel();
		//CollectionModel model = (CollectionModel) ts.getTableModel();
		CollectionModel model = (CollectionModel) getModel();
		for (int i = 0; i < selections.length; i++) {
			//Thumbnail tn = (Thumbnail) model.getElementAt(ts.modelIndex(selections[i]));
			Thumbnail tn = (Thumbnail) model.getElementAt(selections[i]);
			if (tn != null)
				files[i] = tn.getFormat().getFile();
		}
		return files;
	}

	MediaFormat[] selectionsToFormats(int[] selections) {
		MediaFormat[] result = new MediaFormat[selections.length];
		//TableSorter ts = (TableSorter) getModel();
		//CollectionModel model = (CollectionModel) ts.getTableModel();
		CollectionModel model = (CollectionModel) getModel();
		for (int i = 0; i < selections.length; i++) {
			//Thumbnail tn = (Thumbnail) model.getElementAt(ts.modelIndex(selections[i]));
			Thumbnail tn = (Thumbnail) model.getElementAt(selections[i]);
			if (tn != null)
				result[i] = tn.getFormat();
		}
		return result;
	}

	void extractMarkers(int[] selections) {
		//TableSorter ts = (TableSorter) getModel();
		//CollectionModel model = (CollectionModel) ts.getTableModel();
		CollectionModel model = (CollectionModel) getModel();
		String name;
		BasicJpeg im;
		AbstractImageInfo ii;
		int dp;
		for (int i = 0; i < selections.length; i++) {
			// TODO: use abstract format and info
			//im = (BasicJpeg) ((Thumbnail) model.getElementAt(ts.modelIndex(selections[i]))).getFormat();
			im = (BasicJpeg) ((Thumbnail) model.getElementAt(selections[i])).getFormat();
			ii = im.getImageInfo();
			if (ii != null) {
				name = ii.getName();
				dp = name.lastIndexOf('.');
				if (dp > 0)
					name = name.substring(0, dp);
				name = FileNameFormat.makeValidPathName(name, ii.getFormat());
				try {
					im.saveMarkers(new FileOutputStream(new File(im.getParentPath(), name)));
				} catch (IOException e) {
					System.err.println("Exception in markers extraction " + e);
				}
			}
		}
	}

	void rename(final int[] selections, final boolean copy) {
		IniPrefs s = controller.getPrefs();
		String destpath = RenameOptionsTab.getDestPath(s);
		final String value = getRenameMask(this, s, " " + destpath, copy);
		if (value == null)
			return;
		final String dp = destpath;
		statusbar.displayInfo(copy ? Resources.INFO_COPYING : Resources.INFO_RENAMING);
		statusbar.setProgress(selections.length);
		final boolean removeafter = s.getInt(s.getProperty(RenameOptionsTab.SECNAME, RenameOptionsTab.REMOVEAFTER), 0) == 1;
		final boolean auto_rotate = IniPrefs.getInt(s
				.getProperty(RenameOptionsTab.SECNAME, RenameOptionsTab.AUTOROTATE), 0) == 1;
		new Thread(new Runnable() {
			public void run() {
				MediaFormat format;
				int minr = 0, maxr = 0;
				//TableSorter ts = (TableSorter) getModel();
				//CollectionModel model = (CollectionModel) ts.getTableModel();
				CollectionModel model = (CollectionModel) getModel();
				FileNameFormat fnf = new FileNameFormat(value, true);
				for (int i = 0; i < selections.length; i++) {
					// TODO: avoid the casting
					//int tsi = ts.modelIndex(selections[i]);
					int tsi = selections[i];
					format = ((Thumbnail) model.getElementAt(tsi)).getFormat();
					File dest = new File(dp, FileNameFormat.makeValidPathName(fnf.format(format)));
					boolean success = false;
					if (copy)
						try {
							Stream.copyFile(format.getFile(), dest);
							success = true;
						} catch (IOException e) {
						}
					else
						success = format.renameTo(dest);
					if (success) {
						try {
							Date date = (Date) format.getMediaInfo().getAttribute(MediaInfo.DATETIMEORIGINAL);
							dest.setLastModified(date.getTime());
						} catch (Exception e1) {
						}
						if (tsi > maxr)
							maxr = tsi;
						else if (tsi < minr)
							minr = tsi;
					}
					if (removeafter) {
						//((Thumbnail) model.getElementAt(ts.modelIndex(selections[i]))).select(false);
						//model.markDelete(ts.modelIndex(selections[i]));
						((Thumbnail) model.getElementAt(selections[i])).select(false);
						model.markDelete(selections[i]);
					}
					statusbar.tickProgress();
				}
				if (removeafter) {
					model.removeAllMarked();
					model.fireTableRowsDeleted(minr, maxr);
					controller.getUiUpdater().notify(false, UiUpdater.SELECTION_SELECTED);
				} else
					model.fireTableRowsUpdated(minr, maxr);
				statusbar.clearInfo();
				statusbar.clearProgress();
			}
		}, Resources.INFO_RENAMING).start();
	}

	void copyToRipper(final int[] selections) {
		IniPrefs s = controller.getPrefs();
		String destpath = (String) s.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.RIPPER_FOLDER);
		if (destpath == null)
			return; // TODO: warning box
		final File destDir = new File(destpath);
		if (!destDir.exists()) {
			if (!destDir.mkdirs())
				return; // TODO: warning box
		}
		new Thread(new Runnable() {
			public void run() {
				MediaFormat format;
				//TableSorter ts = (TableSorter) getModel();
				//CollectionModel model = (CollectionModel) ts.getTableModel();
				CollectionModel model = (CollectionModel) getModel();
				for (int i = 0; i < selections.length; i++) {
					// TODO: avoid the casting
					//format = ((Thumbnail) model.getElementAt(ts.modelIndex(selections[i]))).getFormat();
					format = ((Thumbnail) model.getElementAt(selections[i])).getFormat();
					String name = format.getName();
					int ep = name.lastIndexOf('.');
					if (ep > 0)
						name = name.substring(0, ep) + Resources.EXT_WAV;
					File dest = new File(destDir, name);
					try {
						Controller.convertToWav(format, dest.getPath(), statusbar);
					} catch (Exception e) {
						System.err.println("Exception at convertion of " + format + " to " + dest + ':' + e);
					}
				}
			}
		}, "Convert to wav").start();
	}

	static String getRenameMask(Component c, IniPrefs s, String to, boolean copy) {
		Object[] masks = RenameOptionsTab.getRenameMask(s);
		if (masks.length == 0 || (masks.length == 1 && masks[0].toString().length() == 0) || masks.length > 1
				|| RenameOptionsTab.askForEditMask(s)) {
			JComboBox values = new JComboBox(masks);
			values.setEditable(true);
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(new JLabel(Resources.LABEL_NEW_NAME), "Center");
			p.add(values, "South");
			if (JOptionPane.showOptionDialog(c, p, (copy?Resources.TITLE_COPY:Resources.TITLE_RENAME) +" " + to, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, null, null) == JOptionPane.OK_OPTION)
				return values.getSelectedItem().toString();
		} else
			return masks[0].toString();
		return null;
	}

	void publish(final Courier courier, final int[] selections) {
		if (selections.length == 0)
			return;

		new Thread(new Runnable() {
			public void run() {
				IniPrefs s = controller.getPrefs();
				try {
					File[] files = selectionsToFiles(selections);
					if (IniPrefs.getInt(s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.CPYPICSONLY),
							1) == 0) {
						try {
							statusbar.displayInfo(Resources.INFO_CONNECTING);
							courier.init();
							String imagePath = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier
									.getMediaPathProperty());
							if (imagePath == null)
								imagePath = "";
							else
								courier.checkForDestPath(imagePath);
							statusbar.clearProgress();
							statusbar.displayInfo(Resources.INFO_COPYING);
							statusbar.setProgress(files.length);
							for (int i = 0; i < files.length; i++) {
								courier.deliver(files[i].getPath(), imagePath);
								statusbar.tickProgress();
							}
						} finally {
							statusbar.clearInfo();
							statusbar.clearProgress();
							courier.done();
						}
					} else { // TODO: add request for text to publish
						new HtmlProducer(controller).produce(getHtmlName(), files, courier, null);
					}
				} catch (IOException e) {
					statusbar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING);
					System.err.println("Exception in Web publishing " + e);
					e.printStackTrace();
				}
			}
		}, "Publishing").start();
	}

	// TODO: consider delete code using in other places instead of duplication
	// it
	void delete(int[] selections) {
		//TableSorter ts = (TableSorter) getModel();
		//CollectionModel model = (CollectionModel) ts.getTableModel();
		CollectionModel model = (CollectionModel) getModel();
		statusbar.displayInfo(Resources.INFO_REMOVING);
		int minr = 0, maxr = 0;
		for (int i = 0; i < selections.length; i++) {
			//int tsi = ts.modelIndex(selections[i]);
			int tsi = selections[i];
			((Thumbnail) model.getElementAt(tsi)).select(false);
			model.markDelete(tsi);
			if (tsi > maxr)
				maxr = tsi;
			else if (tsi < minr)
				minr = tsi;
		}
		model.removeAllMarked();
		model.fireTableRowsDeleted(minr, maxr);
		//tableChanged(new TableModelEvent(model, minr, maxr, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		controller.getUiUpdater().notify(false, UiUpdater.SELECTION_SELECTED);
		statusbar.clearInfo();
		updateStatusBar();
	}

	void show(int[] selections) {
		CollectionModel model = (CollectionModel) getModel();
		//TableSorter ts = (TableSorter) getModel();
		//CollectionModel model = (CollectionModel) ts.getTableModel();
		ArrayList medias = new ArrayList();
		for (int i = 0; i < selections.length; i++)
			medias.add(((Thumbnail) model.getElementAt(selections[i])).getFormat().getFile());
		int startElement = -1;
		if(selections.length == 1) {
			startElement = selections[0] +1; 
		} else if (selections.length == 0)
			startElement = 0;
		if (startElement >= 0)
			for (int i = startElement; i< model.getSize(); i++)
				medias.add(((Thumbnail) model.getElementAt(i)).getFormat().getFile());
		//medias.add(((Thumbnail) model.getElementAt(ts.modelIndex(selections[i]))).getFormat().getFile());
		new PlaybackRequest(medias.toArray(new File[medias.size()]), controller.getPrefs(), false).playList(controller);
		// new PlaybackRequest(medias, controller.getSerializer(),
		// false).playList(controller);
	}

	// prepare list from [] use Arrays.asList([])
	void showList(int start) {
		if (start < 0)
			start = 0;
		new PlaybackRequest((ListModel) getModel(), controller.getPrefs()).playList(start, controller);
	}

	void transform(final int[] selections, final int op) {
		statusbar.displayInfo(Resources.INFO_TRANSFORMING);
		statusbar.setProgress(selections.length);
		loadTransformOptions();

		new Thread(new Runnable() {
			public void run() {
				CollectionModel model = (CollectionModel) getModel();
				//TableSorter ts = (TableSorter) getModel();
				//CollectionModel model = (CollectionModel) ts.getTableModel();
				int minr = 0, maxr = 0, bound = model.getRowCount();
				String transformname;
				for (int i = 0; i < selections.length; i++) {
					//int tsi = ts.modelIndex(selections[i]);
					int tsi = selections[i];
					MediaFormat format = ((Thumbnail) model.getElementAt(tsi)).getFormat();
					transformname = transformName(format.getFile(), op);
					if (op == BasicJpeg.COMMENT) {
						if (format instanceof BasicJpeg) {
							String value = ((BasicJpeg) format).getComment();
							value = (String) JOptionPane.showInputDialog(PhotoCollectionPanel.this,
									Resources.LABEL_COMMENT, Resources.TITLE_COMMENT + " :" + format.getFile(),
									JOptionPane.QUESTION_MESSAGE, format.getThumbnail(null), null, value);
							if (value != null)
								((BasicJpeg) format).setComment(value);
							else
								continue;
						} else {
							// TODO: add/modify ID3(v2) tag
							Id3TagEditor.editTag(controller, format);
						}
					}
					if (format instanceof BasicJpeg) {
						TransformOptionsTab.setArtist(controller, (BasicJpeg) format);
						//System.err.printf("Keep markers as %b%n", keeporigmarkers);
						mediautil.gen.Log.debugLevel = mediautil.gen.Log.LEVEL_DEBUG;
						if (!((BasicJpeg) format).transform(transformname, op, keeporigmarkers, fclass)) {
							statusbar.tickProgress();
							String info = String.format("Transformation of %s to %s operation %d failed.%n", format, transformname, op);
							statusbar.displayInfo(info);
							System.err.printf (info);
							continue;
						}
						if (transformmove) {
							// TODO exclude files if taken from album, or
							// provide album replacement
							if (format.getFile().delete()) {
								model.markDelete(tsi);
								if (tsi > maxr)
									maxr = tsi;
								else if (tsi < minr)
									minr = tsi;
							}
						}
						if (transformaddsel) {
//							System.err.printf("Added transformed %s%n", transformname);
							add(new File[] { new File(transformname) });
						}
						mediautil.gen.Log.debugLevel = mediautil.gen.Log.LEVEL_INFO;
						statusbar.tickProgress();
					}
				}
				if (transformaddsel) {
					model.fireTableRowsInserted(bound, bound + selections.length - 1);
					//tableChanged(new TableModelEvent(model, bound - 1, bound + selections.length - 1,
						//	TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
				}
				if (transformmove) {
					model.removeAllMarked();
					model.fireTableRowsDeleted(minr, maxr);
					tableChanged(new TableModelEvent(model, minr, maxr, TableModelEvent.ALL_COLUMNS,
							TableModelEvent.DELETE));
					controller.getUiUpdater().notify(false, UiUpdater.SELECTION_SELECTED);
				}
				//ts.fireTableStructureChanged();
				model.fireTableStructureChanged();
				statusbar.clearInfo();
				statusbar.clearProgress();
			}
		}, Resources.INFO_TRANSFORMING).start();
	}

	void loadTransformOptions() {
		IniPrefs s = controller.getPrefs();
		keeporigmarkers = IniPrefs.getInt(s.getProperty(TransformOptionsTab.SECNAME, TransformOptionsTab.KEEP), 0) == 1;
		transformmove = IniPrefs.getInt(s.getProperty(TransformOptionsTab.SECNAME, TransformOptionsTab.MOVE), 0) == 0;
		transformaddsel = IniPrefs.getInt(s.getProperty(TransformOptionsTab.SECNAME, TransformOptionsTab.TRANSFTOSEL),
				1) == 1;
		String format = (String) s.getProperty(TransformOptionsTab.SECNAME, TransformOptionsTab.FORMAT);
		fclass = null;
		if (format != null && !keeporigmarkers) {
			if (Resources.LIST_EXIF.equals(format))
				fclass = Exif.class;
			else if (Resources.LIST_CIFF.equals(format))
				fclass = CIFF.class;
			else if (Resources.LIST_JFIF.equals(format))
				fclass = JFXX.class;
			else if (Resources.LIST_EXTRN.equals(format))
				fclass = AbstractImageInfo.class;
		}
	}

	public void save() {
		controller.saveTableColumns(this, SECNAME, COLWIDTH);
	}

	public void load() {
		controller.loadTableColumns(this, SECNAME, COLWIDTH);
	}

	JPopupMenu getRMouseMenu() {
		return new RButtonMenu();
	}

	//
	// remote controllable
	//
	public String getName() {
		return Resources.COMP_SELECTION;
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
		controller.selectTab(Resources.TAB_COLLECTION);
	}

	class RButtonMenu extends JPopupMenu {
		RButtonMenu() {
			JMenuItem item;
			this.add(item = new JMenuItem(Resources.MENU_REVERSE_SELECT));
			item.addActionListener(PhotoCollectionPanel.this);
			this.add(item = new JMenuItem(Resources.MENU_SELECTALL));
			item.addActionListener(PhotoCollectionPanel.this);
			addSeparator();
			this.add(item = new JMenuItem(Resources.MENU_SHOW));
			addSeparator();
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			this.add(item = new JMenuItem(Resources.MENU_ADDTOALBUM));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			this.add(item = new JMenuItem(Resources.MENU_TORIPPER));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));

			this.add(item = new JMenuItem(Resources.MENU_ADDTO_IPOD));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));

			this.add(item = new JMenuItem(Resources.MENU_EXTRACTMARKERS));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			addSeparator();
			this.add(item = new JMenuItem(Resources.MENU_RENAME));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			this.add(item = new JMenuItem(Resources.MENU_COPY));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			this.add(item = new JMenuItem(Resources.MENU_DELETE));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));

			this.add(item = new JMenuItem(Resources.MENU_COPY_LOCATION));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			addSeparator();
			this.add(item = new JMenuItem(Resources.MENU_PRINT));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			addSeparator();
			JMenu menu;
			this.add(menu = Controller.createTransformMenu(PhotoCollectionPanel.this));
			menu.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			addSeparator();
			this.add(item = new JMenuItem(Resources.MENU_PROPERTIES));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			this.add(item = new JMenuItem(Resources.MENU_EDIT_PROPS));
			item.addActionListener(PhotoCollectionPanel.this);
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
		}
	}

	class CollectionModel extends BaseConfigurableTableModel implements ListModel {
		Vector collection;

		CollectionModel(Controller controller) {
			updateView(controller);
		}

		protected int getDescriptionIndex() {
			return AppearanceOptionsTab.COLLECT_VIEW;
		}

		public int getRowCount() {
			return thumbnailspanel.getComponentCount();
		}

		public Object getValueAt(int row, int column) {
			MediaFormat format = ((Thumbnail) thumbnailspanel.getComponent(row)).getFormat();
			if (format == null)
				return null;
			return getValueAt(format.getFile(), format.getMediaInfo(), column);
		}

		public Object /* Thumbnail */getElementAt(int index) {
			try {
				return /* (Thumbnail) */thumbnailspanel.getComponent(index);
			} catch (ArrayIndexOutOfBoundsException e) {
				return null;
			}
		}

		void removeElementAt(int row) {
			thumbnailspanel.remove(row);
		}

		void markDelete(int row) {
			marked.addElement(thumbnailspanel.getComponent(row));
		}

		void removeAllMarked() {
			for (int i = 0; i < marked.size(); i++)
				thumbnailspanel.remove((Component) marked.elementAt(i));
			marked.removeAllElements();
			thumbnailspanel.adjustDimension();
		}

		public void fireTableRowsUpdated(int firstRow, int lastRow) {
			super.fireTableRowsUpdated(firstRow, lastRow);
			for (int i = firstRow; i <= lastRow; i++) {
				((Thumbnail) thumbnailspanel.getComponent(i)).update();
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

		Vector marked = new Vector(10);
	}
}