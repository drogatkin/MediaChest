/* MediaChest - IpodControl.java
 * Copyright (C) 1999-2008 Dmitriy Rogatkin.  All rights reserved.
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
 *
 *  $Id: IpodControl.java,v 1.42 2013/12/15 07:51:39 cvs Exp $
 * Created on 28.05.2004
 */
package photoorganizer.ipod;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.UiUpdater;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MP4;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.WMA;
import photoorganizer.ipod.PhotoDB.PhotoItem;
import photoorganizer.renderer.IpodOptionsTab;
import photoorganizer.renderer.StatusBar;

/**
 * @author dmitriy
 * 
 * 
 */

public class IpodControl {
	static int MAX_WRITE_ATTEMPTS = 3; // can be set in config?

	protected static IpodControl ipodControl;

	protected Controller controller;

	protected volatile boolean accessInProgress;

	protected IpodControl(Controller controller) {
		this.controller = controller;
		IpodControl.this.controller.getUiUpdater().registerScheduledUpdater(UiUpdater.IPOD_CONNECTED,
				new UiUpdater.StateChecker() {
					String dev;

					public boolean isEnabled() {
						boolean ipodAvail = accessInProgress == false;
						// TODO has to be synchronized
						if (ipodAvail && dev != null && dev.length() > 0 && checkDev(dev))
							return true;
						dev = IpodOptionsTab.getDevice(IpodControl.this.controller);
						if (dev != null && ipodAvail) {
							ipodAvail = checkDev(dev);
							// if (ipodAvail)
							// System.err.println("SPACE:"+ipodFile.length());
							if (ipodAvail == false) {
								try {
									new URL(dev + ITunesDB.PATH_ITUNESDB);
									ipodAvail = true;
								} catch (Exception e) {
									ipodAvail = false;
								}
							}
						}
						return ipodAvail;
					}

					public void setEnabled(boolean b) {

					}

					private boolean checkDev(String dev) {
						File ipodFile = new File(dev);
						return ipodFile.exists() && ipodFile.isDirectory();
					}

				});
	}

	public static IpodControl getIpodControl(Controller controller) {
		synchronized (IpodControl.class) {
			if (ipodControl == null)
				ipodControl = new IpodControl(controller);
		}
		return ipodControl;
	}

	/**
	 * This method tries to detect iPod connected
	 * 
	 * @return
	 */
	public static File detectIPod() {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File cn = getComputerNode(fsv, fsv.getRoots(), 0);
		if (cn != null)
			for (File f : fsv.getFiles(cn, true)) {
				
				if (fsv.getSystemTypeDescription(f) == null || fsv.getSystemTypeDescription(f).indexOf("emovable") >= 0) {
					//System.err.printf("Looing in %s%n", f);
					if (new File(f, ITunesDB.PATH_ITUNESDB).exists())
						return f;
					if (new File(f, ITunesDB.PATH_ITUNESDB_IOS).exists())
						return f;
					//else
					//	System.err.printf("Suspected %s/%s%n", f, fsv
					//			.getSystemTypeDescription(f));
				}
			}
		// check for Gnome mount
		cn = new File(System.getProperty("user.home"), ".gvfs");
		for (File f:cn.listFiles()) {
			if (f.getName().startsWith("Documents") == false)
				return f;
		}
		
		return null;
	}

	public static File getComputerNode(FileSystemView fsv, File[] roots, int level) {
		if (level > 2)
			return null;
		for (File f : roots) {
			//System.err.printf("node %s desc %s%n", f, fsv.getSystemTypeDescription(f));
			if (fsv.isComputerNode(f) || "Computer".equals(f.toString()) || "My Computer".equals(f.toString()) || "This PC".equals(f.toString()))
				return f;
			File result = getComputerNode(fsv, fsv.getFiles(f, true), level + 1);
			if (result != null)
				return result;
			if ("/".equals(f.toString())) {
				result = new File("/media/"+System.getProperty("user.name"));
				
				if (result.exists() == false || result.isDirectory() == false )
					return new File("/media");
				return result;
			}
		}
		return null;
	}

	/**
	 * The method does actual sync image of iTunesDB in memory with actual image
	 * on iPod or simulator device.
	 * 
	 */
	public synchronized void sync(final ITunesDB iTunesDb) {
		controller.getUiUpdater().notify(false, UiUpdater.IPOD_CONNECTED);
		final IniPrefs s = controller.getPrefs();
		final StatusBar statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		String dev = IpodOptionsTab.getDevice(controller);
		boolean needUpdateDb = false; // , needUpdateUsage = false;
		long usedSpace = 0;
		// delete all required deletion to be synced
		// TODO: move deletion after writing DB
		try {
			accessInProgress = true;
			List deleteList = iTunesDb.getDeletedItems();
			if (deleteList != null) {
				statusbar.displayInfo(Resources.INFO_REMOVING);
				Iterator i = deleteList.iterator();
				int delCount = 0;
				while (i.hasNext()) {
					PlayItem pi = (PlayItem) i.next();
					String fn = (String) pi.get(PlayItem.FILENAME);
					if (fn == null) {
						System.err.printf("Underneath file for %s was already deleted%n", pi);
						continue;
					}
					File f = new File(dev + fn.replace(':', File.separatorChar));
					if (f.delete()) {
						delCount++;
						pi.set(PlayItem.FILENAME, (String) null);
						pi.setState(BaseItem.STATE_COPIED);
					} else {
						if (f.exists())
							System.err.println("File '" + f + "' has not been deleted.");
						else {
							delCount++;
							pi.set(PlayItem.FILENAME, (String) null);
							pi.setState(BaseItem.STATE_COPIED);
						}
					}
				}
				needUpdateDb = delCount > 0;
				// System.err.println("Deleted "+delCount);
				statusbar.displayInfo(Resources.INFO_DELETED + " " + delCount);
			}
			InputStream is = null, isu = null;
			statusbar.displayInfo(Resources.INFO_READING);
			try {
				isu = new BufferedInputStream(IpodOptionsTab.openInputStream(controller, ITunesDB.PATH_PLAYCOUNTS),
						10 * 1024);
			} catch (IOException ioe) {
				System.err.println("Can't read " + ITunesDB.PATH_PLAYCOUNTS + " because " + ioe
						+ ". Statistics won't be updated.");
			}
			try {
				boolean compressed = false;
				String itunesDbPath = ITunesDB.PATH_ITUNESDB;
						if (IpodOptionsTab.checkFile(controller, itunesDbPath) == false) {
							itunesDbPath = ITunesDB.PATH_ITUNESDB_IOS;
							compressed = true;
						}
				usedSpace = iTunesDb.read(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller,
						itunesDbPath), 1024 * 1024), isu, compressed);
				// process all OTG lists including saved
				new File(dev + File.separatorChar + ITunesDB.PATH_IPOD_ITUNES).listFiles(new FileFilter() {
					public boolean accept(File f) {
						String n = f.getName();
						if (n.startsWith(ITunesDB.OTGPLAYLISTINFO)) {
							InputStream is = null;
							try {
								if (ITunesDB.OTGPLAYLISTINFO.equals(n)) {
									switch (IniPrefs.getInt(s.getProperty(IpodOptionsTab.SECNAME,
											IpodOptionsTab.ACTION_ON_THE_GO), IpodOptionsTab.OTG_DEF_ACTION)) {
									case 1: // create with new
										iTunesDb.readOTGPlaylist(is = new BufferedInputStream(IpodOptionsTab
												.openInputStream(controller, ITunesDB.PATH_OTGPLAYLISTINFO), 1024),
												true);
										break;
									case 2: // add to existing
										iTunesDb.readOTGPlaylist(is = new BufferedInputStream(IpodOptionsTab
												.openInputStream(controller, ITunesDB.PATH_OTGPLAYLISTINFO), 1024),
												false);
										break;
									}
								} else
									iTunesDb.readOTGPlaylist(
											is = new BufferedInputStream(new FileInputStream(f), 1024), true);
							} catch (IOException ioe) {
								if (is != null)
									statusbar.flashInfo(ioe.getMessage());
								ioe.printStackTrace();
								System.err.println("Can't read " + f + " because " + ioe);
							} finally {
								try {
									is.close();
								} catch (Exception e) {
								}
							}
						}
						return false;
					}
				});
			} catch (IOException ioe) {
				if (is != null)
					statusbar.flashInfo(ioe.getMessage());
				ioe.printStackTrace();
				System.err.println("Can't read one of " + dev + ITunesDB.PATH_ITUNESDB + " | " + dev
						+ ITunesDB.PATH_OTGPLAYLISTINFO + " because " + ioe);
			} finally {
				try {
					is.close();
				} catch (Exception e) {
				}
				try {
					isu.close();
				} catch (Exception e) {
				}
			}
			statusbar.displayInfo(Resources.INFO_READINGARTWORK);
			// attach photos
			ArtworkDB artworkDB = new ArtworkDB();
			try {
				artworkDB.read(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller,
						ArtworkDB.PATH_IPOD_ARTWORK_DB), 1024 * 1024), iTunesDb.imageConnector);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (Exception e) {
				}
				iTunesDb.imageConnector = null;
			}
			// prepare copy list
			statusbar.displayInfo(Resources.INFO_COPYING);
			List forCopy = iTunesDb.getToCopyList();
			Stream.setCopyBufferSize(1024 * 100);
			// copy files
			statusbar.setProgress(forCopy.size());
			// TODO: option to support custom dir structure should be applied
			// here
			// TODO: persistent of last used directory 00..XX(79) should be also here
			Iterator i = forCopy.iterator();
			Writer copyBatchW = null;
			for (int c = 0; i.hasNext(); c++) {
				PlayItem pi = (PlayItem) i.next();
				File sf = new File((String) pi.get(PlayItem.FILENAME));
				String ext = pi.getAttachedFormat() != null ? pi.getAttachedFormat().getFormat(0) : null;
				if (ext == null)
					ext = MP3.TYPE;
				File tf = null;
				String mfn = null;
				for (int fmc = 0; fmc < 50000; fmc++) { // TODO figure out why this constrain
					mfn = ITunesDB.createFilePath(sf.getName(), c, fmc, ext);
					tf = new File(dev + mfn.replace(':', File.separatorChar));
					if (tf.exists() == false)
						break;
				}
				if (tf.getParentFile().exists() == false)
					if (tf.getParentFile().mkdirs() == false) {
						System.err.println("Can't create directory " + tf.getParentFile());
						continue;
					}
				boolean succeeded = false;
				int ca = 0;
				while (succeeded == false && ca++ < MAX_WRITE_ATTEMPTS)
					try {
						statusbar.displayInfo(Resources.INFO_COPYING + " to " + tf);
						Stream.copyFile(sf, tf);
						needUpdateDb = succeeded = true;
					} catch (IOException ioe) {
						System.err.println("Problem of copying from " + sf + " to " + tf + " exception " + ioe);
						if (ioe.getMessage() != null
								&& ioe.getMessage().indexOf("Data error (cyclic redundancy check)") < 0)
							break;
					}
				if (succeeded == false)
					try {
						// TODO: use cp and \n for Unix
						if (copyBatchW == null)
							copyBatchW = new FileWriter("recopy.bat", true);
						copyBatchW.write("copy /v \"" + sf + "\" \"" + tf + "\"\r\n");
						needUpdateDb = true;
					} catch (IOException ioe2) {
						System.err.println("Can't write a copy command to batch file. " + ioe2);
					}
				// TODO: mark for errors and display to a user
				// TODO: needs some way to mark bad entries to avoid copy them
				// to iTunesDB
				else {
					pi.set(PlayItem.FILENAME, mfn);
					pi.setState(BaseItem.STATE_COPIED);
				}
				statusbar.tickProgress();
			}
			try {
				if (copyBatchW != null) {
					copyBatchW.close();
					copyBatchW = null;
				}
			} catch (IOException ioe) {
			}
			statusbar.clearProgress();
			Stream.setCopyBufferSize(0); // reset custom buf size
			// TODO do batch rename
			/*
			 * for (PlayItem pi:forRename) {
			 *     rename and update pi
			 * }
			*/
			int lfai = IniPrefs.getInt(s.getProperty(IpodOptionsTab.SECNAME, IpodOptionsTab.ACTION_ON_UNKNOWN), 0);
			if (lfai > 0) {
				// search for lost files
				// TODO: it's possible directly to look in main play list
				// for each found file, however it can be a bit slow
				statusbar.displayInfo(Resources.INFO_LOSTFILE);
				if (lfai < IpodOptionsTab.ACT_DEL_ITUNES)
					processUnknowns(dev, iTunesDb.getCopiedFiles(dev), lfai, iTunesDb);
				else if (lfai != IpodOptionsTab.ACT_REBUILD_ARTWORK) {
					Collection<PlayItem> hungRefs = new ArrayList<PlayItem>(10);
					for (PlayItem pi : iTunesDb.iPodFiles) {
						// TODO: scan over all file to see the corresponding
						// exist, and delete
						// the entry if doesn't
						if (new File(dev + ((String) pi.get(PlayItem.FILENAME)).replace(':', File.separatorChar)).exists() == false)
							hungRefs.add(pi); // note can't use simple
						// Iterator.remove(), since can
						// be in other lists
					}
					for (PlayItem pi : hungRefs)
						iTunesDb.removePlayItem(pi, iTunesDb.iPodFiles);
				}
			}

			needUpdateDb |= iTunesDb.isChanged();
			// needUpdateUsage

			if (needUpdateDb == false) {
				statusbar.clearInfo();
				return; // no update required, however something maybe deleted
			}
			if (lfai == IpodOptionsTab.ACT_REBUILD_ARTWORK) {
				statusbar.displayInfo(Resources.INFO_REBULDARTWORK);
				statusbar.setProgress(-1);
				iTunesDb.resetImagesToTags(dev);
				statusbar.clearProgress();
			}
			OutputStream os = null;
			// write iTunesDB
			// TODO: use file upload servlet
			File nif = new File(dev + ITunesDB.PATH_ITUNESDB + ".wrk");
			File pcf = new File(dev + ITunesDB.PATH_PLAYCOUNTS);
			statusbar.displayInfo(Resources.INFO_WRITING);
			if (nif.getParentFile().exists() == false) {
				if (nif.getParentFile().mkdirs() == false)
					System.err.println("Can't create iPod's directory structure - " + nif.getParentFile());
			}
			try {
				iTunesDb.write(os = new FileOutputStream(nif));
				os.close();
				os = null;
				File of = new File(dev + ITunesDB.PATH_ITUNESDB);
				if (of.delete() == false)
					System.err.println("Couldn't delete " + of);
				if (nif.renameTo(of) == false)
					System.err.println("Couldn't rename " + nif + " to " + of);
				iTunesDb.clearDeleted();
				if (pcf.delete() == false)
					System.err.println("Couldn't delete " + pcf);
				new File(dev + File.separatorChar + ITunesDB.PATH_IPOD_ITUNES).listFiles(new FileFilter() {
					public boolean accept(File f) {
						if (f.getName().startsWith(ITunesDB.OTGPLAYLISTINFO))
							if (f.delete() == false)
								System.err.println("Couldn't delete " + f);
						return false;
					}
				});
			} catch (IOException ioe) {
				statusbar.flashInfo(ioe.getMessage());
				ioe.printStackTrace();
			} finally {
				if (os != null)
					try {
						os.close();
					} catch (Exception e) {
					}
			}
			// writing artwork
			// TODO: check if artwork is applicable for this iPod and correct image size accordingly type
			if (iTunesDb.imageConnector != null && iTunesDb.imageConnector.size() > 0) {
				statusbar.displayInfo(Resources.INFO_WRITINGARTWORK);
				nif = new File(dev + ArtworkDB.PATH_IPOD_ARTWORK_DB + ".wrk");
				if (nif.getParentFile().exists() == false) {
					if (nif.getParentFile().mkdirs() == false)
						System.err.println("Can't create artwork iPod's directory structure - " + nif.getParentFile());
				}
				try {
					artworkDB.write(os = new BufferedOutputStream(new FileOutputStream(nif), 1024 * 1024),
							iTunesDb.imageConnector, dev);
					os.close();
					os = null;
					File of = new File(dev + ArtworkDB.PATH_IPOD_ARTWORK_DB);
					if (of.delete() == false)
						System.err.println("Couldn't delete " + of);
					if (nif.renameTo(of) == false)
						System.err.println("Couldn't rename " + nif + " to " + of);
				} catch (IOException ioe) {
					statusbar.flashInfo(ioe.getMessage());
					ioe.printStackTrace();
				} finally {
					if (os != null)
						try {
							os.close();
						} catch (Exception e) {
						}
				}
			}
			if (iTunesDb.isPhotoChanged()) {
				// scan for photos need to be copied
				boolean copyPhoto = IniPrefs.getInt(s.getProperty(IpodOptionsTab.SECNAME,
						IpodOptionsTab.COPY_ORIGINAL_PHOTO), 1) == 1;
				if (copyPhoto) {
					statusbar.displayInfo(Resources.INFO_COPY_PHOTOS);
					for (PhotoItem pi:iTunesDb.photoDirectory.getPhotos().getItems()) {
						if (pi.isState(pi.STATE_COPIED) == false) {
							String tn = pi.makeiPodFullResPath();
							try {
								File targetFile = new File(dev, PhotoDB.PATH_IPOD_PHOTOC + tn.replace(':', File.separatorChar));
								targetFile.getParentFile().mkdirs(); // assure directory
								Stream.copyFile(pi.getFile(), targetFile);
								pi.set(PlayItem.FILENAME, tn);
								pi.setState(BaseItem.STATE_COPIED);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				PhotoDB photoDB = new PhotoDB();
				statusbar.displayInfo(Resources.INFO_PHOTODB_READ);
				photoDB.directory = iTunesDb.photoDirectory;
				try {
					photoDB.read(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller, PhotoDB.PHOTO_DB_PATH)));
				}catch (IOException ioe) {
					ioe.printStackTrace();
				} finally {
					try {
						is.close();
					} catch (Exception e) {
					}
				}
				statusbar.displayInfo(Resources.INFO_PHOTODB_UPDATE);
				
				try {
					os = null;
					photoDB.write(os = IpodOptionsTab.openOutputStream(controller, PhotoDB.PHOTO_DB_PATH+".wrk"), dev/*IpodOptionsTab.getDevice(controller)*/, copyPhoto);
					os.close();
					os = null;
					if (new File(dev+PhotoDB.PHOTO_DB_PATH).delete())
						new File(dev+PhotoDB.PHOTO_DB_PATH+".wrk").renameTo(new File(dev+PhotoDB.PHOTO_DB_PATH));
					else
						System.err.println("Couldn't delete " + dev+PhotoDB.PHOTO_DB_PATH);
				} catch (IOException ioe) {
					statusbar.flashInfo(ioe.getMessage());
					ioe.printStackTrace();
				} finally {
					if (os != null)
						try {
							os.close();
						} catch (Exception e) {
						}
				}
			}
		} finally {			
			accessInProgress = false;
			// TODO clean status bar?
		}
		assert iTunesDb.isChanged() == false;
		statusbar.clearInfo();
		statusbar.displayMetric((usedSpace == 0 ? "" : BasicIo.convertLength(usedSpace) + "/")
				+ (iTunesDb.getChild(iTunesDb, 0) == null ? 0 : ((List) iTunesDb.getChild(iTunesDb, 0)).size())
				+ Resources.LABEL_NUM_ITEMS);
	}

	public boolean isSyncing() {
		return accessInProgress;
	}

	public synchronized long readDatabase(ITunesDB iTunesDb) {
		controller.getUiUpdater().notify(false, UiUpdater.IPOD_CONNECTED);
		final IniPrefs s = controller.getPrefs();
		final StatusBar statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
		InputStream is = null, isu = null;
		statusbar.displayInfo(Resources.INFO_READING);
		long usedSpace = 0;
		try {
			isu = new BufferedInputStream(IpodOptionsTab.openInputStream(controller, ITunesDB.PATH_PLAYCOUNTS),
					10 * 1024);
		} catch (IOException ioe) {
			System.err.println("Can't read " + ITunesDB.PATH_PLAYCOUNTS + " because " + ioe
					+ ". Usage statistic won't be updated.");
		}
		try {
			boolean compressed = false;
			String itunesDbPath = ITunesDB.PATH_ITUNESDB;
					if (IpodOptionsTab.checkFile(controller, itunesDbPath) == false) {
						itunesDbPath = ITunesDB.PATH_ITUNESDB_IOS;
						compressed = true;
					}
			usedSpace = iTunesDb.read(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller,
					itunesDbPath), 1024 * 1024), isu, compressed);
			is.close();
			is = null;
			switch (IniPrefs.getInt(s.getProperty(IpodOptionsTab.SECNAME, IpodOptionsTab.ACTION_ON_THE_GO),
					IpodOptionsTab.OTG_DEF_ACTION)) {
			case 1: // create with new
				iTunesDb.readOTGPlaylist(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller,
						ITunesDB.PATH_OTGPLAYLISTINFO), 1024), true);
				break;
			case 2: // add to existing
				iTunesDb.readOTGPlaylist(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller,
						ITunesDB.PATH_OTGPLAYLISTINFO), 1024), false);
				break;
			}
			statusbar.clearInfo();
		} catch (IOException ioe) {
			if (is != null)
				statusbar.flashInfo(ioe.getMessage());
			else
				statusbar.clearInfo();
			// ioe.printStackTrace();
			System.err.println("Can't read one of " + ITunesDB.PATH_ITUNESDB + " | " + ITunesDB.PATH_OTGPLAYLISTINFO
					+ " because " + ioe);
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
			try {
				isu.close();
			} catch (Exception e) {
			}
		}

		ArtworkDB artworkDB = new ArtworkDB();
		try {
			usedSpace += artworkDB.read(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller,
					ArtworkDB.PATH_IPOD_ARTWORK_DB), 1024 * 1024), iTunesDb.imageConnector);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
			iTunesDb.imageConnector = null;
		}
		
		PhotoDB photoDB = new PhotoDB();
		try {
			photoDB.read(is = new BufferedInputStream(IpodOptionsTab.openInputStream(controller, PhotoDB.PHOTO_DB_PATH)));
			iTunesDb.photoDirectory = photoDB.directory; 
		}catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}
		return usedSpace;
	}

	public synchronized void wipeAll(ITunesDB iTunesDb) {
		String dev = IpodOptionsTab.getDevice(controller);
		for (int i = 0; i < ITunesDB.NUM_HOLDERS; i++) {
			File[] delList = new File(dev
					+ MessageFormat.format(ITunesDB.PATH_IPODMUSIC_ROOT, new Object[] { new Integer(i) })).listFiles();
			if (delList == null) {
				System.err
						.println("Empty directory "
								+ new File(dev
										+ MessageFormat
												.format(ITunesDB.PATH_IPODMUSIC, new Object[] { new Integer(i) })));
				continue;
			}
			for (int j = 0; j < delList.length; j++)
				if (delList[j].delete() == false)
					System.err.println("Can't delete " + delList[j]);
		}
		if (new File(dev + ITunesDB.PATH_ITUNESDB).delete() == false)
			System.err.println("Can't delete " + dev + ITunesDB.PATH_ITUNESDB);
		File[] delList = new File(dev + ArtworkDB.PATH_IPOD_ARTWORK).listFiles();
		for (File f : delList)
			if (f.delete() == false)
				System.err.printf("Can't delete %s\n", f);
		if (new File(dev + ITunesDB.PLAYCOUNTS).delete() == false)
			System.err.println("Can't delete " + dev + ITunesDB.PLAYCOUNTS);
	}

	// TODO: add file list servlet for URL locations
	protected void processUnknowns(String dev, List allKnowns, int act, ITunesDB iTunesDb) {
		File folder = new File(dev + ITunesDB.PATH_IPOD_ROOT); // hopefully no
		// file in root
		// otherwise check for double slashes after dev
		processFolderforUnknown(dev, folder, allKnowns, act, iTunesDb);
	}

	protected void processFolderforUnknown(String dev, File folder, List allKnowns, int act, ITunesDB iTunesDb) {
		if (folder.isDirectory() == false) {
			String fn = folder.toString();
			if (fn.startsWith(dev)) {
				// TODO: reconsider for better performance
				fn = fn.substring(dev.length()); // TODO: check for Unix, can
				// be a problem
				fn = fn.replace(File.separatorChar, ':');
				if (fn.charAt(0) != ':')
					fn = ":" + fn;
			}
			if (allKnowns.contains(fn) == false) {
				//for (int k = 0; k < 3 && k < allKnowns.size(); k++)
					//System.err.print(allKnowns.get(k));
				System.err.println(" Not found " + folder + " for name " + fn);
				switch (act) {
				case IpodOptionsTab.ACT_ADDTO_IPOD:
					PlayItem pi = null;
					MediaFormat format = MediaFormatFactory.createMediaFormat(folder);
					if (format != null)
						pi = PlayItem.create(format, controller);
					if (pi != null) {
						pi = iTunesDb.addPlayItem(pi, iTunesDb.getPlayList(Resources.LABEL_LOST_END_FOUND));
						pi.set(PlayItem.FILENAME, fn);
						pi.resetState(BaseItem.STATE_METASYNCED);
						pi.setState(BaseItem.STATE_COPIED);
					}
					break;
				case IpodOptionsTab.ACT_DELFR_IPOD:
					if (folder.delete() == false)
						System.err.println("Couldn't delete " + folder);
					break;
				}
			} else
				allKnowns.remove(fn);
		} else {
			File[] files = folder.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					if (pathname.isDirectory())
						return true;
					return Controller.hasExtension(pathname, new String[] { MP3.TYPE, MP4.TYPE, MP4.MP4, MP4.AAC,
							WMA.TYPE });
				}
			});
			// TODO: can be not most effective, too many calls
			for (int i = 0; i < files.length; i++)
				processFolderforUnknown(dev, files[i], allKnowns, act, iTunesDb);
		}
	}
}
