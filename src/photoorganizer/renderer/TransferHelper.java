/* MediaChest - TransferHelper.java
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
 *  $Id: TransferHelper.java,v 1.24 2007/10/30 03:59:59 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.TransferHandler;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

import mediautil.gen.MediaFormat;
import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.album.MediaAccess;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MP4;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.WMA;
import photoorganizer.ipod.PlayItem;
import photoorganizer.ipod.PlayList;

public class TransferHelper {
	static final DataFlavor MEDIA_FORMAT_DF = new DataFlavor(MediaFormat.class,
			"application/x-java-jvm-local-objectref");

	static final DataFlavor FILE_FORMAT_DF = new DataFlavor(File.class, "application/x-java-jvm-local-objectref");

	static final DataFlavor ALBUM_NODE_INDEX_DF = new DataFlavor(Integer.class,
			"application/x-java-jvm-local-objectref");

	static final DataFlavor FILE_LIST_DF = DataFlavor.javaFileListFlavor;

	static final DataFlavor MIXED_FILE_MEDIA_LIST_DF = new DataFlavor(java.util.ArrayList.class,
			"application/x-java-jvm-local-objectref");

	static final DataFlavor MEDIA_PLAY_LIST_DF = new DataFlavor(java.util.ArrayList.class,
			"application/x-java-jvm-local-objectref-playitem");

	static final DataFlavor MEDIA_PLAY_LIST = new DataFlavor(photoorganizer.ipod.PlayList.class,
			"application/x-java-jvm-local-objectref-playlist");

	static public TransferHandler createTrasnsferHandler(Object owner, Controller controller) {
		if (owner instanceof PlayListPane || owner instanceof IpodPane)
			return new IpodTransferHandler(controller);
		// else if (owner instanceof MediaAccess)
		// return new MediaTransferHandler((MediaAccess)owner);
		return null;
	}

	public static class TransferableLists implements Transferable {
		PlayList playList;

		TransferableLists(PlayList pl) {
			playList = pl;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			return playList;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(MEDIA_PLAY_LIST);
		}

		public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { MEDIA_PLAY_LIST };
		}
	}

	public static class TransferableMedias implements Transferable {
		java.util.List medias;

		Integer albumId;

		boolean isPlayItems;

		TransferableMedias(java.util.List medias) {
			this.medias = medias;
		}

		TransferableMedias(java.util.List medias, boolean isPlayItems) {
			this.medias = medias;
			this.isPlayItems = isPlayItems;
		}

		TransferableMedias(int albumId) {
			this.albumId = new Integer(albumId);
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (flavor.equals(MIXED_FILE_MEDIA_LIST_DF) || flavor.equals(MEDIA_PLAY_LIST_DF))
				return medias;
			else if (flavor.equals(ALBUM_NODE_INDEX_DF))
				return albumId;
			throw new UnsupportedFlavorException(flavor);
		}

		public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { MIXED_FILE_MEDIA_LIST_DF, ALBUM_NODE_INDEX_DF, MEDIA_PLAY_LIST_DF };
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (medias != null
					&& flavor.getHumanPresentableName().equals(MIXED_FILE_MEDIA_LIST_DF.getHumanPresentableName())
					&& flavor.equals(MIXED_FILE_MEDIA_LIST_DF) && !isPlayItems)
					|| (medias != null
							&& flavor.getHumanPresentableName().equals(MEDIA_PLAY_LIST_DF.getHumanPresentableName())
							&& flavor.equals(MEDIA_PLAY_LIST_DF) && isPlayItems)
					|| (flavor.equals(ALBUM_NODE_INDEX_DF) && albumId != null);
		}
	}

	public static class IpodTransferHandler extends TransferHandler {
		protected Controller controller;

		IpodTransferHandler(Controller controller) {
			this.controller = controller;
		}

		public int getSourceActions(JComponent c) {
			return c instanceof JTree ? COPY_OR_MOVE : MOVE; // ;
		}

		protected Transferable createTransferable(JComponent c) {
			if (c instanceof JTable) {
				JTable t = (JTable) c;
				int[] selections = t.getSelectedRows();
				java.util.List medias = new ArrayList(selections.length);
				TableModel tm = t.getModel();
				if (tm instanceof ListModel) {
					ListModel model = (ListModel) tm;
					for (int i = 0; i < selections.length; i++)
						medias.add(model.getElementAt(selections[i]));
					return new TransferHelper.TransferableMedias(medias, true);
				}
			} else if (c instanceof JTree) {
				TreePath tp = ((JTree) c).getSelectionPath();
				if (tp != null || tp.getLastPathComponent() instanceof PlayList) {
					PlayList pl = (PlayList) tp.getLastPathComponent();
					if (/* pl.isVirtual()==false && */pl.isFileDirectory() == false)
						return new TransferHelper.TransferableLists(pl);
				}
			}
			return null;
		}

		public boolean importData(JComponent comp, Transferable t) {
			IpodPane ipodPane = null;
			PlayList playList = null;
			// System.err.println("Drop comp "+comp.getClass().getName()+" trans
			// "+t);
			if (comp instanceof PlayListPane) {
				playList = ((PlayListPane) comp).playList;
				ipodPane = ((PlayListPane) comp).ipodPane;
			} else if (comp instanceof IpodPane) {
				ipodPane = (IpodPane) comp;
				TreePath path = ipodPane.getSelectionPath();
				if (path != null && path.getPathCount() > 2
						&& Resources.LABEL_PLAYLISTS.equals(path.getPathComponent(1).toString()))
					playList = (PlayList) path.getLastPathComponent();
				else
					playList = null;
			}
			if (ipodPane != null) {
				try {
					if (t.isDataFlavorSupported(MIXED_FILE_MEDIA_LIST_DF)) {
						java.util.List l = (java.util.List) t.getTransferData(MIXED_FILE_MEDIA_LIST_DF);
						MediaFormat[] medias = new MediaFormat[l.size()];
						for (int i = 0; i < l.size(); i++) {
							Object media = l.get(i);
							if (media instanceof MediaFormat)
								medias[i] = (MediaFormat) media;
							else if (media instanceof File)
								medias[i] = MediaFormatFactory.createMediaFormat((File) media);
						}
						ipodPane.add(medias, playList, ipodPane.INVALIDATE_NONE);
					} else if (t.isDataFlavorSupported(FILE_LIST_DF)) {
						java.util.List l = (java.util.List) t.getTransferData(FILE_LIST_DF);
						java.util.List gl = new ArrayList(l.size());
						Controller.buildMediaList(gl, l.toArray(), new int[] { MediaFormat.AUDIO, MediaFormat.VIDEO, MediaFormat.AUDIO+MediaFormat.VIDEO }, true);
						ipodPane.add((MediaFormat[]) gl.toArray(new MediaFormat[gl.size()]), playList,
								playList == null ? ipodPane.INVALIDATE_ALL : ipodPane.INVALIDATE_TABLE);
					} else if (t.isDataFlavorSupported(ALBUM_NODE_INDEX_DF)) {
						// TODO: make it recursive
						MediaAccess access = new MediaAccess(controller);
						Integer albumNode = (Integer) t.getTransferData(ALBUM_NODE_INDEX_DF);
						Object[] formats = access.getAlbumContents(albumNode.intValue());
						MediaFormat[] medias = new MediaFormat[formats.length];
						for (int i = 0; i < formats.length; i++) {
							if (formats[i] instanceof MediaFormat)
								medias[i] = (MediaFormat) formats[i];
							else if (formats[i] instanceof File)
								medias[i] = MediaFormatFactory.createMediaFormat((File) formats[i]);
						}
						playList = ipodPane.getPlayList(access.getNameOfAlbum(albumNode.intValue()));
						ipodPane.add(medias, playList, ipodPane.INVALIDATE_ALL);
					} else if (t.isDataFlavorSupported(MEDIA_PLAY_LIST_DF)) {
						if (comp instanceof IpodPane) {
							java.util.List l = (java.util.List) t.getTransferData(MEDIA_PLAY_LIST_DF);
							ipodPane
									.add((PlayItem[]) l.toArray(new PlayItem[l.size()]), playList, 0/* ipodPane.INVALIDATE_TABLE */);
						}
					} else if (t.isDataFlavorSupported(MEDIA_PLAY_LIST)) {
						// TODO: allow to drop regular list to pupulate all
						// content unless just change position
						if (playList != null && playList.isFileDirectory() == false && playList.isVirtual() == false) {
							PlayList droppedList = (PlayList) t.getTransferData(MEDIA_PLAY_LIST);
							if (droppedList.isVirtual()) {
								ipodPane.add((PlayItem[]) droppedList.toArray(new PlayItem[droppedList.size()]),
										playList, 0);
							} else {
								ipodPane.rearrange(playList.toString(), droppedList.toString());
								ipodPane.invalidatePlayLists();
							}
						}
					}
					// updateUiState(controller, model, false);
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return false;
		}

		protected void exportDone(JComponent source, Transferable data, int action) {
			if (NONE == action)
				return;
			assert action == MOVE;
			IpodPane ipodPane = null;
			PlayList playList = null;
			// System.err.println("exportDone comp
			// "+source.getClass().getName()+" trans "+data+" act "+action);
			if (source instanceof PlayListPane) {
				playList = ((PlayListPane) source).playList;
				ipodPane = ((PlayListPane) source).ipodPane;
			} else
				return;
			if (data == null)
				return;
			if (data.isDataFlavorSupported(MEDIA_PLAY_LIST_DF)) {
				try {
					java.util.List l = (java.util.List) data.getTransferData(MEDIA_PLAY_LIST_DF);
					if (source instanceof PlayListPane) {
						int sel = ((PlayListPane) source).getSelectedRow();
						int ls = l.size();
						int p = 0;
						for (int i = (sel >= ls ? 0 : ls); i < sel; i++) {
							PlayItem pi = (PlayItem) playList.get(i);
							if (l.contains(pi))
								continue;
							else
								p++;
							pi.set(pi.ORDER, p);
							pi.resetState(pi.STATE_METASYNCED);
							if (debug__)
								System.err.println("Set b " + (p) + " for " + pi);

						}
						int gs = p;
						p += ls;
						for (int i = sel; i < playList.size(); i++) {
							PlayItem pi = (PlayItem) playList.get(i);
							if (l.contains(pi))
								continue;
							else
								p++;
							pi.set(pi.ORDER, p);
							pi.resetState(pi.STATE_METASYNCED);
							if (debug__)
								System.err.println("Set a " + (p) + " for " + pi + " ->" + pi.get(pi.ORDER));
						}
						Iterator it = l.iterator();
						for (int i = gs + 1; it.hasNext(); i++) {
							PlayItem pi = (PlayItem) it.next();
							pi.set(pi.ORDER, i);
							pi.resetState(pi.STATE_METASYNCED);
							if (debug__)
								System.err.println("Set o " + (i) + " for " + pi + " ->" + pi.get(pi.ORDER));
						}
						if (debug__)
							System.err.println("Adding/moving " + l + " to :" + playList + " before " + sel);
					}
					Collections.sort(playList);
					if (debug__) {
						System.err.printf("Sorted %s \n", playList);
						for (PlayItem pi : playList)
							System.err.printf("Item: %s: %d\n", pi, pi.get(pi.ORDER));
					}
					ipodPane.updateTable(); // ??? do we really need
				} catch (Exception /* UnsupportedFlavorException, IOException */ufe) {
				}
			} else if (data.isDataFlavorSupported(FILE_LIST_DF)) {
				// TODO: remove copyied files, but we can do that after actual
				// synching though
			}
		}

		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
			return comp instanceof PlayListPane || comp instanceof IpodPane;
		}
	}

	protected final static boolean debug__ =
	// true;
	false;
}