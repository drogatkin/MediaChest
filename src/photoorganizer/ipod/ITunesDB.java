/* MediaChest - $RCSfile: ITunesDB.java,v $ 
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
 *  $Id: ITunesDB.java,v 1.158 2013/05/08 04:24:11 cvs Exp $
 */
package photoorganizer.ipod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.InflaterInputStream;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.aldan3.patrn.Visitor;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MP4;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.ipod.BaseHeader.DisplayType;

/**
 * this class provides basic operation with iTunesDB database all database data considered to be stored in memory
 */
public class ITunesDB implements TreeModel {
	public final static String IPOD_CONTROL = "iPod_Control";
	
	public final static String IPOD_CONTROL_IOS = "iTunes_Control";

	public final static String PATH_IPOD_ROOT = File.separator + IPOD_CONTROL;
	
	public final static String PATH_IPOD_ROOT_IOS = File.separator + IPOD_CONTROL_IOS;

	public final static String PATH_IPOD_ITUNES = PATH_IPOD_ROOT + File.separatorChar + "iTunes";
	
	public final static String PATH_IPOD_ITUNES_IOS = PATH_IPOD_ROOT_IOS + File.separatorChar + "iTunes";

	public final static String PATH_ITUNESDB = PATH_IPOD_ITUNES + File.separatorChar + "iTunesDB";
	
	public final static String PATH_ITUNESDB_IOS = PATH_IPOD_ITUNES_IOS + File.separatorChar + "iTunesCDB";

	public final static String PLAYCOUNTS = "Play Counts";
	
	public final static String PLAYCOUNTS_IOS = "PlayCounts.plist";

	public final static String PATH_PLAYCOUNTS = PATH_IPOD_ITUNES + File.separatorChar + PLAYCOUNTS;

	public final static String PATH_IPODMUSIC = PATH_IPOD_ROOT + "\\Music\\F{0,number,00}\\{1}";

	public final static String PATH_IPODMUSIC_MAC_IOS = ":iTunes_Control:Music:F{0,number,00}:{1}";
	
	public final static String PATH_IPODMUSIC_MAC = ":iPod_Control:Music:F{0,number,00}:{1}";

	public final static String PATH_IPODMUSIC_ROOT = PATH_IPOD_ROOT + "\\Music\\F{0,number,00}";

	public final static String OTGPLAYLISTINFO = "OTGPlaylistInfo";

	public final static String PATH_OTGPLAYLISTINFO = PATH_IPOD_ITUNES + File.separatorChar + OTGPLAYLISTINFO;

	public final static String IPOD = "IPOD";

	/** this label is important to match used by iPod */
	public final static String OTG_PLAYLIST_PREF = "On-The-Go ";

	public static final Descriptor[] DIRECTORY_ENTRIES = { new Descriptor(Resources.LABEL_PLAYLISTS, 0),
			new Descriptor(Resources.LABEL_ARTISTS, PlayItem.ARTIST),
			new Descriptor(Resources.LABEL_ALBUMS, PlayItem.ALBUM),
			new Descriptor(Resources.LABEL_GENRES, PlayItem.GENRE),
			new Descriptor(Resources.LABEL_COMPOSERS, PlayItem.COMPOSER),
			new Descriptor(Resources.LABEL_VIDEOS, PlayItem.VIDEO_KIND) };

	static final int TYP_SIZE = 30;

	public static final int MAINLIST_INIT_CAPACITY = 100;

	protected static int mainlistInitCapacity = MAINLIST_INIT_CAPACITY;

	protected static int NUM_HOLDERS = 20; // set 40 for 60GB iPod
	
	protected static int MAX_FILE_NAME_LEN = 31;
	
	// TODO: add flag to lock/unlock on time of synching
	PlayList iPodFiles;

	PlayDirectory[] directories;
	
	PhotoDB.PhotoDirectory photoDirectory;

	Map<Integer, PlayItem> directory;

	List deletedPlayLists;

	Map<String, PlayList> /* dbUpdatesOnly, */renamedPlayLists;

	Map<PlayList, PlayList> wasUpdatedCache = new HashMap<PlayList, PlayList>();

	Map<Long, PlayItem> imageConnector;

	EventListenerList listenerList = new EventListenerList();

	public ITunesDB() {
		initPlayDirectories();
	}

	/**
	 * reads content of database, usually from /iPodControl/iTunes/iTunesDB
	 */
	public synchronized long read(InputStream inStream, InputStream statStream, boolean compressed) throws IOException {
		if (debug)
			System.err.println("====READ=====");
		long timeStamp = System.currentTimeMillis();
		imageConnector = new HashMap<Long, PlayItem>();
		long result = 0;
		int numStatEntries = 0;
		BaseHeader header = new BaseHeader();
		BaseHeader.HeaderStat statHdr = null;
		if (statStream != null)
			try {
				header.read(statStream);
			} catch (IOException ioe) {
				System.err.println("An exception at reading stats:" + ioe);
			}
		if ("mhdp".equals(header.signature) == false)
			System.err.println("Invalid or unsupported format " + header.signature + " of " + PATH_PLAYCOUNTS
					+ ". Statistical information will be disabled.");
		else {
			statHdr = new BaseHeader.HeaderStat(header.totalSize);
			numStatEntries = header.num;
		}
		header.read(inStream);

		if ("mhbd".equals(header.signature) == false)
			throw new IOException("Invalid iTunesDB file format. 1st header isn't mhbd.");
		int totalSize = header.totalSize;
		int size = header.size;
		int nsd = header.num;
		if (compressed) {
		inStream = new InflaterInputStream(inStream);
		totalSize = Integer.MAX_VALUE;
		}
		for (int sdi = 0; sdi < nsd; sdi++) {
			checkEOF(totalSize - size, header);
			size += header.read(inStream);
			if ("mhsd".equals(header.signature) == false)
				throw new IOException("Header mhsd expected after mhdb or mhsd 1.");
			if (header.thingsList) { // always 1st?
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if ("mhlt".equals(header.signature) == false)
					throw new IOException("Header mhlt expected after mhsd as a file list.");
				int nfs = header.num;
				setInitialCapicity(nfs);
				if (directory == null)
					directory = new /* Weak */HashMap();
				else
					directory.clear();

				for (int i = 0; i < nfs; i++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if ("mhit".equals(header.signature) == false)
						throw new IOException("Header mhit expected.");
					//System.err.println("mhit size:"+header.size);
					PlayItem pi = new PlayItem(header.index);
					pi.set(PlayItem.LENGTH, header.length);
					pi.set(PlayItem.SIZE, header.fileSize);
					result += header.fileSize;
					pi.set(PlayItem.BITRATE, header.bitRate);
					pi.set(PlayItem.SAMPLE_RATE, header.encoding);
					pi.set(PlayItem.BPM, header.BPM);
					pi.set(PlayItem.VBR, header.vbr);
					pi.set(PlayItem.VOLUME, header.volume);
					pi.set(PlayItem.START, header.start);
					pi.set(PlayItem.STOP, header.stop);
					pi.set(PlayItem.ORDER, header.order);
					pi.set(PlayItem.NUM_TRACKS, header.tracks);
					pi.set(PlayItem.PLAYED_TIMES, header.numPlayed);
					pi.set(PlayItem.CREATE_TIME, header.createDate);
					pi.set(PlayItem.LAST_TIME, header.lastDate);
					pi.set(PlayItem.MODIFIED_TIME, header.modifiedDate);
					pi.set(PlayItem.INDEX, header.index);
					pi.set(PlayItem.DISK, header.disk);
					pi.set(PlayItem.YEAR, header.year);
					pi.set(PlayItem.NUM_DISKS, header.num_disks);
					pi.set(PlayItem.PLAYED_TIMES, header.numPlayed);
					pi.set(PlayItem.SKIPPED_TIMES, header.numSkipped);
					pi.set(PlayItem.LAST_SKIPPED_TIME, header.lastSkipped);
					pi.set(PlayItem.RATING, header.rating);
					pi.set(PlayItem.LAST_TIME, header.lastDate);
					pi.set(PlayItem.COMPILATION, header.compilation);
					pi.set(PlayItem.SKIP_SHUFFLING, header.skip_shuffling);
					pi.set(PlayItem.REMEMBER_POS, header.remember_pos);
					pi.set(PlayItem.GAPLESS_ALBUM, header.gapless_album);
					pi.set(PlayItem.EPIZODE_NUM, header.epizode);
					pi.set(PlayItem.SEASON_NUM, header.season);
					String videoKind = null;
					if (header.movie) {
						switch (header.displaytype) {
						case MusicVideo:
							videoKind = Resources.HDR_MUSICVIDEO;
							break;
						case VideoPodcast:
							videoKind = Resources.HDR_PODCAST;
							break;
						case TVShow:
							videoKind = Resources.HDR_TVSHOW;
							break;
						default:
							videoKind = Resources.HDR_MOVIES;
						}
					}
					pi.set(PlayItem.VIDEO_KIND, videoKind);
					// int reference = header.index;
					if (i < numStatEntries) {
						statHdr.read(statStream);
						pi.set(PlayItem.PLAYED_TIMES, statHdr.count + header.numPlayed);
						pi.set(PlayItem.RATING, statHdr.rating);
						if (statHdr.lastPlay != 0)
							pi.set(PlayItem.LAST_TIME, statHdr.lastPlayDate);
						pi.set(PlayItem.SKIPPED_TIMES, statHdr.skipCount + header.numSkipped);
						if (statHdr.lastSkipStamp != null)
							pi.set(PlayItem.LAST_SKIPPED_TIME, statHdr.lastSkipStamp);
					}
					if (header.numThings > 0) {
						imageConnector.put(header.hash1 + (long) (header.hash2 << 32), pi);
						//System.err.printf("Artwork item %s size %d%n", pi, header.artworkSize);
					}
					int nsh = header.num;
					for (int j = 0; j < nsh; j++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream);
						if ("mhod".equals(header.signature) == false)
							throw new IOException("Header mhod expected, but " + header.signature + " found.");
						if (debug)
							System.err.printf("%d=%s%n", header.index, header.data);
						pi.set(header.index, header.data);
					}
					pi.setState(pi.STATE_METASYNCED + pi.STATE_COPIED);
					addPlayItem(pi, null);
				}
			} else if (header.playList) {
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if ("mhlp".equals(header.signature) == false)
					throw new IOException("Header mhlp expected after mhsd as a play list.");
				int npl = header.num; // number of play lists
				getPlayLists(npl); // create playlists
				for (int np = 0; np < npl; np++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if ("mhyp".equals(header.signature) == false)
						throw new IOException("Header mhyp expected after mhlp as a start of play list.");
					if (header.visible == false && debug == false) { // skipping invisible lists
						size += header.skip(inStream);
						continue;
					}
					PlayList playList = null;
					int pln = header.numThings;
					int nipl = header.num;
					String listName = null;
					BaseHeader.Smart smart = null;
					int hash1 = header.hash1;
					int hash2 = header.hash2;
					Date last = header.lastDate;
					for (int ip = 0; ip < nipl; ip++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream);
						if ("mhod".equals(header.signature) == false)
							throw new IOException("Header mhod expected after mhyp, but " + header.signature
									+ " found.");
						if (header.index == PlayItem.TITLE)
							listName = header.data;
						else if (header.index == BaseHeader.PLAYLISTENTRY) {
							// TODO: some processing
							if (listName == null)
								listName = "List " + header.reference;
						} else if (header.index == BaseHeader.LISTATTRIBUTE) {
							if (debug) {
								System.err.println("Met list attr " + header.reference);
								int[] list = (int[]) header.complexData;
								for (int i = 0; i < list.length; i++) {
									// PlayItem playItem = (PlayItem)directory.get(new Integer(list[i]));
									// System.err.println("Item "+list[i]+"--"+sortlist.get(new Integer(list[i])));
									// System.err.println("Item "+list[i]+"--"+directory.get(new Integer(list[i])));
									if (list[i] >= 0 && list[i] < iPodFiles.size())
										System.err.println("Item " + list[i] + "--" + iPodFiles.get(list[i]));
									else
										System.err.println("Item " + list[i] + " doesn't exist.");
								}
							}
						} else if (header.index == BaseHeader.SMARTLISTDEF) {
							smart = (BaseHeader.Smart) header.complexData;
						} else if (header.index == BaseHeader.SMARTLISTENTRY) {
							smart.rules = (BaseHeader.Rules) header.complexData;
						} else
							System.err.println("Unknown type " + header.index + " of play list attribute");
					}
					playList = getPlayList(listName, pln);
					// null means main play list
					if (playList != null && playList.isChanged() == false && smart != null) {
						playList.setAttribute(BaseHeader.SMARTLISTENTRY, smart);
						playList.setAttribute(PlayList.HASH1, hash1);
						playList.setAttribute(PlayList.HASH2, hash2);
						playList.setAttribute(PlayList.LASTMOD, last);
					}
					if (debug)
						System.err.println("Smart " + smart);
					for (int pin = 0; pin < pln; pin++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream);
						if ("mhip".equals(header.signature) == false)
							throw new IOException("Header mhip (" + pin + ") expected here. \n" + header);
						int plhh = header.num;
						PlayItem playItem = (PlayItem) directory.get(new Integer(header.reference));
						if (playItem != null) { // not in remove list
							playItem = (PlayItem) playItem.clone();
							playItem.set(PlayItem.ORDER, pin + 1);
						}
						if (debug)
							System.err.println("Entry " + pin + " item id " + header.reference + " value " + playItem);
						for (int jp = 0; jp < plhh; jp++) {
							checkEOF(totalSize - size, header);
							size += header.read(inStream);
							if ("mhod".equals(header.signature) == false)
								throw new IOException("Header mhod (" + jp + ") expected here. " + header);
							if (header.index == BaseHeader.PLAYLISTENTRY) {
								if (playItem != null)
									playItem.set(PlayItem.ORDER, pin + 1/* header.reference */);
								else
									playItem = (PlayItem) directory.get(new Integer(header.reference));
							} else
								System.err.println("Unhandled play  item attribute  " + header.index);
							// can we expect something other than type 100?
						}
						if (playList != null && playItem != null && playList.isRemoved(playItem) == false) {
							playList.add(playItem);
						}
						if (debug)
							System.err.println("Play item " + playItem);
					}
					//updateMagicList(playList); // TODO move after complete
				}
			} else if (header.filesList) {
				// this header duplicate header.playList
				checkEOF(totalSize - size, header);
				if (debug) {
					size += header.skip(inStream);
				} else
					size += header.skip(inStream);
			} else if (header.albumList) {
				checkEOF(totalSize - size, header);				
				size += header.skip(inStream);
				System.err.printf("Skipping albuns%n");
			} else if (header.genius_cuid || header.mhsd_10) {
				checkEOF(totalSize - size, header);
				size += header.skip(inStream);
			} else if (header.artists) {
				checkEOF(totalSize - size, header);
				size += header.skip(inStream);
				System.err.printf("Skipping artists%n");
			} else
				throw new IOException("Unexpected type of header " + header + '.');
		}
		sort();
		// TODO update all smart lists
		if (directories[0] != null) {
			for (PlayList pl : (Collection<PlayList>) directories[0].content.values())
				updateMagicList(pl);
		}
		System.err.println("Total time:" + (System.currentTimeMillis() - timeStamp) + "ms., size: "+size);
		return result;
	}

	public void readOTGPlaylist(InputStream otgStream, boolean assignUniqueName) throws IOException {
		int currentCount;
		byte[] buf = new byte[8];
		currentCount = BasicIo.read(otgStream, buf);
		if (currentCount < buf.length)
			throw new IOException("End of stream at reading OTG list header signature. " + currentCount);
		String sign = new String(buf, 0, 4, Controller.ISO_8859_1);
		if ("mhpo".equals(sign) == false)
			throw new IOException("Invalid signature " + sign + " at reading OTG list header signature.");
		int h_len = BasicIo.s2n(buf, 4, 4, false, true);
		assert h_len > buf.length;
		buf = new byte[h_len - buf.length];
		currentCount += BasicIo.read(otgStream, buf);
		int entrySize = BasicIo.s2n(buf, 0, 4, false, true);
		int numEntries = BasicIo.s2n(buf, 4, 4, false, true);
		if (debug)
			System.err.println("?? field " + BasicIo.s2n(buf, 8, 4, false, true));
		if (numEntries <= 0)
			return; // empty otg
		assert entrySize >= 2;
		buf = new byte[entrySize * numEntries];
		currentCount += BasicIo.read(otgStream, buf);
		int i = 1;
		if (assignUniqueName)
			while (playListExists(OTG_PLAYLIST_PREF + i))
				i++;
		PlayList pl = getPlayList(OTG_PLAYLIST_PREF + i);
		if (pl == null)
			return;
		for (i = 0; i < numEntries; i++) {
			PlayItem playItem = null;
			try {
				playItem = (PlayItem) iPodFiles.get(BasicIo.s2n(buf, i * entrySize, entrySize, false, true));
			} catch (IndexOutOfBoundsException iobe) {
			}
			if (debug)
				System.err.println("PI for " + BasicIo.s2n(buf, i * entrySize, entrySize, false, true) + " is "
						+ playItem);
			if (playItem != null && pl.contains(playItem) == false) { // not in remove list
				playItem = (PlayItem) playItem.clone();
				playItem.resetState(PlayItem.STATE_METASYNCED);
				playItem.set(PlayItem.ORDER, i + 1);
				pl.add(playItem);
			}
		}
		// return currentCount;
	}

	static void checkEOF(int remain, BaseHeader header) throws IOException {
		if (remain <= 0)
			throw new IOException("Unexpected end of file (" + remain + ") after header " + header + '.');
	}

	/**
	 * used for keep tracking of entry indexes and generate new unique for new entries
	 */
	protected void registerEntry(int entryIndex) {
	}

	protected int getNextEntry() {
		return 0;
	}

	static final int MHOD_INT_TOTSIZE = 44;

	static final int MHOD_DATA1_TOTSIZE = 648;

	static final int[] LONG_SORT_LISTS = { 3, 4, 5, 7, 18 };

	static final int[] SHORT_SORT_LISTS = { 3 };

	/**
	 * writes current status of database usually in /iPodControl/iTunes/iTunesDB note that it can be doen only when actual files already copied to iPod to avoid
	 * inconsistency
	 */
	public void write(OutputStream outStream/* , OutputStream statOutStream */) throws IOException {
		if (iPodFiles == null/* || iPodFiles.size() == 0 */)
			return;
		imageConnector = new HashMap<Long, PlayItem>(); // maybe just clear when exists?
		BaseHeader headerMHBD = new BaseHeader();
		headerMHBD.makeIt(BaseHeader.MHBD);
		// calculate size
		BaseHeader headerMHSD = new BaseHeader();
		headerMHSD.makeIt(BaseHeader.MHSD);
		headerMHSD.thingsList = true;

		BaseHeader headerMHLT = new BaseHeader();
		headerMHLT.makeIt(BaseHeader.MHLT);
		headerMHSD.totalSize += headerMHLT.size;
		headerMHLT.num = iPodFiles.size();

		BaseHeader headerMHIT = new BaseHeader();
		headerMHIT.makeIt(BaseHeader.MHIT);

		BaseHeader headerMHOD = new BaseHeader();
		headerMHOD.makeIt(BaseHeader.MHOD);
		int id = 128;
		for (int i = 0; i < headerMHLT.num; i++) {
			PlayItem pi = (PlayItem) iPodFiles.get(i);
			// pi.set(pi.INDEX, indexCount++);
			// TODO: make index offset of mhit
			pi.id = id++;// headerMHBD.size+headerMHSD.totalSize;
			for (int mhi = 1; mhi < pi.MAX_PROP + 1; mhi++) {
				String s = (String) pi.get(mhi);
				if (s != null && s.length() > 0) {
					headerMHSD.totalSize += headerMHOD.size + s.length() * 2 + 16;
				}
			}
			headerMHSD.totalSize += headerMHIT.size;
		}
		headerMHBD.totalSize += headerMHSD.totalSize; // 1 mhsd for entire songs thingsList
		BaseHeader headerMHSD2 = new BaseHeader();
		headerMHSD2.makeIt(BaseHeader.MHSD);

		BaseHeader headerMHLP = new BaseHeader();
		headerMHLP.makeIt(BaseHeader.MHLP);
		headerMHLP.num = directories[0].size() + 1; // main playlist plus all others
		headerMHSD2.totalSize += headerMHLP.size;
		headerMHSD2.playList = true;

		BaseHeader headerMHYP = new BaseHeader();
		BaseHeader headerMHIP = new BaseHeader();
		headerMHIP.makeIt(headerMHIP.MHIP);
		headerMHIP.totalSize += MHOD_INT_TOTSIZE;

		for (int pli = 0; pli < headerMHLP.num; pli++) {
			headerMHYP.makeIt(headerMHYP.MHYP);
			PlayList pl = null;
			if (pli == 0) { // or add flag for main PL (all songs)
				headerMHYP.main = true;
				pl = iPodFiles;
				headerMHYP.totalSize += headerMHOD.size + IPOD.length() * 2 + 16; // playlist name header
				// mhod 52
				// attr 3 song -------------
				// attr 5 artist
				// attr 4 album
				// attr 7 genres
				// attr 18 composer?
				headerMHYP.totalSize += (headerMHOD.size + 48 + 4 * pl.size())
						* (writesortheaders_ ? LONG_SORT_LISTS.length : SHORT_SORT_LISTS.length);
			} else {
				pl = directories[0].get(pli - 1);
				String playList = pl.name;
				headerMHYP.totalSize += headerMHOD.size + playList.length() * 2 + 16; // playlist name
				if (pl.smart != null) {
					pl.clear();
					if (pl.smart.live == false)
						updateMagicList(pl);
					// mhod 50
					headerMHYP.totalSize += headerMHOD.size + pl.smart.size();
					// mhod 51
					headerMHYP.totalSize += headerMHOD.size + pl.smart.rules.size();
				}
			}
			headerMHYP.totalSize += MHOD_DATA1_TOTSIZE; // mhod type 100 totalSize 648
			headerMHYP.numThings = pl.size();
			for (int plii = 0; plii < headerMHYP.numThings; plii++) {
				headerMHYP.totalSize += headerMHIP.totalSize;
			}
			headerMHSD2.totalSize += headerMHYP.totalSize;
		}
		headerMHBD.totalSize += headerMHSD2.totalSize * 2; // these mhsds are for plays and files
		headerMHBD.num = 3;
		// start writing
		headerMHBD.write(outStream);
		headerMHSD.write(outStream);
		headerMHLT.write(outStream);
		int cnt1 = 0x79700000;
		int cnt2 = "IPOD".hashCode();
		for (int i = 0; i < headerMHLT.num; i++) {
			PlayItem pi = (PlayItem) iPodFiles.get(i);
			headerMHIT.makeIt(BaseHeader.MHIT);
			for (int mhi = 1; mhi < PlayItem.MAX_PROP + 1; mhi++) {
				String s = (String) pi.get(mhi);
				if (s != null && s.length() > 0) {
					headerMHIT.totalSize += headerMHOD.size;
					headerMHIT.totalSize += s.length() * 2 + 16;
					headerMHIT.num++;
				}
			}
			headerMHIT.index = pi.id;
			headerMHIT.length = ((Integer) pi.get(PlayItem.LENGTH)).intValue();
			headerMHIT.fileSize = ((Integer) pi.get(PlayItem.SIZE)).intValue();
			headerMHIT.bitRate = ((Integer) pi.get(PlayItem.BITRATE)).intValue();
			headerMHIT.vbr = ((Integer) pi.get(PlayItem.VBR)).intValue();
			headerMHIT.volume = ((Integer) pi.get(PlayItem.VOLUME)).intValue();
			headerMHIT.start = ((Integer) pi.get(PlayItem.START)).intValue();
			headerMHIT.stop = ((Integer) pi.get(PlayItem.STOP)).intValue();
			headerMHIT.year = ((Integer) pi.get(PlayItem.YEAR)).intValue();
			headerMHIT.order = ((Integer) pi.get(PlayItem.ORDER)).intValue();
			headerMHIT.tracks = ((Integer) pi.get(PlayItem.NUM_TRACKS)).intValue();
			headerMHIT.numPlayed = ((Integer) pi.get(PlayItem.PLAYED_TIMES)).intValue();
			headerMHIT.encoding = ((Integer) pi.get(PlayItem.SAMPLE_RATE)).intValue();
			headerMHIT.BPM = ((Integer) pi.get(PlayItem.BPM)).intValue();
			headerMHIT.compilation = ((Boolean) pi.get(PlayItem.COMPILATION)).booleanValue();

			headerMHIT.skip_shuffling = ((Boolean) pi.get(PlayItem.SKIP_SHUFFLING)).booleanValue();
			headerMHIT.gapless_album = ((Boolean) pi.get(PlayItem.GAPLESS_ALBUM)).booleanValue();
			headerMHIT.remember_pos = ((Boolean) pi.get(PlayItem.REMEMBER_POS)).booleanValue();

			headerMHIT.disk = ((Integer) pi.get(PlayItem.DISK)).intValue();
			headerMHIT.num_disks = ((Integer) pi.get(PlayItem.NUM_DISKS)).intValue();

			headerMHIT.rating = ((Integer) pi.get(PlayItem.RATING)).intValue();

			headerMHIT.createDate = (Date) pi.get(PlayItem.CREATE_TIME);
			headerMHIT.lastDate = (Date) pi.get(PlayItem.LAST_TIME);
			headerMHIT.modifiedDate = (Date) pi.get(PlayItem.MODIFIED_TIME);
			headerMHIT.lastSkipped = (Date) pi.get(PlayItem.LAST_SKIPPED_TIME);

			headerMHIT.numSkipped = ((Integer) pi.get(PlayItem.SKIPPED_TIMES)).intValue();

			headerMHIT.season = ((Integer) pi.get(PlayItem.SEASON_NUM)).intValue();
			headerMHIT.epizode = ((Integer) pi.get(PlayItem.EPIZODE_NUM)).intValue();

			String videoKind = (String) pi.get(PlayItem.VIDEO_KIND);
			if (videoKind != null) {
				headerMHIT.movie = true;
				if (Resources.HDR_MUSICVIDEO.equals(videoKind))
					headerMHIT.displaytype = DisplayType.MusicVideo;
				else if (Resources.HDR_PODCAST.equals(videoKind))
					headerMHIT.displaytype = DisplayType.VideoPodcast;
				else if (Resources.HDR_TVSHOW.equals(videoKind))
					headerMHIT.displaytype = DisplayType.TVShow;
				else
					headerMHIT.displaytype = DisplayType.Video;
			} else
				headerMHIT.displaytype = DisplayType.Audio;
			headerMHIT.hash1 = cnt1 + i;
			headerMHIT.hash2 = cnt2;
			if (pi.getImage() != null) {
				imageConnector.put(headerMHIT.hash1 + ((long) headerMHIT.hash2 << 32/* & 0xffffffff00000000l */), pi);
				headerMHIT.numThings = 1;
				MediaFormat mf = pi.getAttachedFormat();
				if (mf != null) {
					byte[] ba = mf instanceof MP3 ? ((MP3) mf).getThumbnailData(null) : (mf instanceof MP4 ? ((MP4) mf)
							.getThumbnailData(null) : null);
					if (ba != null)
						headerMHIT.artworkSize = ba.length;
				}
				//if (headerMHIT.artworkSize == 0) {
				//	System.err.printf("Artwork size not set for %s%n", pi);
				//	headerMHIT.artworkSize = 3247;
				//}
			}
			headerMHIT.write(outStream);
			for (int mhi = 1; mhi < PlayItem.MAX_PROP + 1; mhi++) {
				String s = (String) pi.get(mhi);
				if (s != null && s.length() > 0) {
					headerMHOD.makeIt(BaseHeader.MHOD);
					headerMHOD.index = mhi;
					headerMHOD.data = s;
					headerMHOD.totalSize += s.length() * 2 + 16;
					headerMHOD.write(outStream);
				}
			}
			pi.setState(BaseItem.STATE_METASYNCED); // TODO: can be a problem if writing failed
		}
		// writing 2 mhsds both are the same
		for (int hti = 0; hti < 2; hti++) {
			if (hti == 0) {
				headerMHSD2.filesList = true;
				headerMHSD2.thingsList = false;
				headerMHSD2.playList = false;
			} else {
				headerMHSD2.filesList = false;
				headerMHSD2.playList = true;
			}
			headerMHSD2.write(outStream);
			headerMHLP.write(outStream);
			Map sortIndexMap = null;
			// writing mhyps, type 7 = all songs (main) 4 = smart, 2 = regular
			for (int pli = 0; pli < headerMHLP.num; pli++) {
				headerMHYP.makeIt(headerMHYP.MHYP);
				PlayList pl = null;
				String playList = null;
				headerMHOD.makeIt(BaseHeader.MHOD);
				if (pli == 0) { // or add flag for main PL (all songs)
					headerMHYP.main = true;
					headerMHYP.visible = false;
					headerMHYP.num = 2 + (writesortheaders_ ? LONG_SORT_LISTS.length : SHORT_SORT_LISTS.length);
					pl = (PlayList) iPodFiles.clone();
					playList = IPOD;
					// common for title MHOD see below
					// mhod 52
					// attr 3 song ------------
					// attr 5 artist
					// attr 4 album
					// attr 7 genres
					// attr 18 composer?
					headerMHYP.totalSize += (headerMHOD.size + 48 + 4 * pl.size())
							* (writesortheaders_ ? LONG_SORT_LISTS.length : SHORT_SORT_LISTS.length);
				} else {
					pl = directories[0].get(pli - 1);
					playList = pl.name;
					headerMHYP.num = pl.smart == null ? 2 : 4; // for play list name
					if (pl.smart != null) {
						// mhod 50
						headerMHYP.totalSize += headerMHOD.size + pl.smart.size();
						// mhod 51
						headerMHYP.totalSize += headerMHOD.size + pl.smart.rules.size();
					}
				}
				headerMHOD.index = PlayItem.TITLE;
				headerMHOD.data = playList;
				headerMHOD.totalSize += playList.length() * 2 + 16;
				headerMHYP.totalSize += headerMHOD.totalSize;
				headerMHYP.totalSize += MHOD_DATA1_TOTSIZE; // extra dummy MHOD
				headerMHYP.numThings = pl.size();
				if (debug)
					System.err.println("Playlist " + pl + " size  " + pl.size());
				for (int plii = 0; plii < headerMHYP.numThings; plii++) {
					headerMHYP.totalSize += headerMHIP.totalSize;
				}
				headerMHYP.hash2 = ((Integer) pl.getAttribute(PlayList.HASH2)).intValue();
				headerMHYP.hash1 = ((Integer) pl.getAttribute(PlayList.HASH1)).intValue();
				headerMHYP.lastDate = (Date) pl.getAttribute(PlayList.LASTMOD);
				headerMHYP.write(outStream);
				// write title
				headerMHOD.write(outStream);
				headerMHOD.makeIt(BaseHeader.MHOD);
				headerMHOD.totalSize = MHOD_DATA1_TOTSIZE;
				headerMHOD.index = BaseHeader.PLAYLISTENTRY;
				headerMHOD.write(outStream);

				if (pli == 0) {
					// build 5 sort lists
					// TODO: another solution is keeping sort index in list items
					sortIndexMap = new HashMap(pl.size());
					for (int li = 0; li < pl.size(); li++) {
						PlayItem pi = (PlayItem) iPodFiles.get(li);
						sortIndexMap.put(new Integer(pi.id), new Integer(li));
					}
					int[] list = new int[iPodFiles.size()];
					headerMHOD.complexData = list;
					headerMHOD.numThings = list.length;
					headerMHOD.totalSize = headerMHOD.size + 48 + 4 * list.length;
					int[] sortArray;
					if (writesortheaders_)
						sortArray = LONG_SORT_LISTS;
					else
						sortArray = SHORT_SORT_LISTS;
					for (int sortI : sortArray) {
						headerMHOD.reference = sortI; // song
						Comparator c = null;
						switch (sortI) {
						case 3:
							c = new PlayItem.SongComparator();
							break;
						case 4:
							c = new PlayItem.AlbumComparator();
							break;
						case 5:
							c = new PlayItem.ArtistComparator();
							break;
						case 7:
							c = new PlayItem.GenreComparator();
							break;
						case 18:
							c = new PlayItem.ComposerComparator();
						}
						Collections.sort(iPodFiles, c);
						for (int li = 0; li < pl.size(); li++) {
							PlayItem pi = (PlayItem) iPodFiles.get(li);
							list[li++] = ((Integer) sortIndexMap.get(new Integer(pi.id))).intValue();
						}
						headerMHOD.write(outStream);
					}
				}
				if (pl.smart != null) {
					headerMHOD.makeIt(BaseHeader.MHOD);
					headerMHOD.index = BaseHeader.SMARTLISTDEF; // mhod 50
					headerMHOD.complexData = pl.smart;
					headerMHOD.totalSize = headerMHOD.size + pl.smart.size();
					headerMHOD.write(outStream);
					headerMHOD.makeIt(BaseHeader.MHOD);
					headerMHOD.index = BaseHeader.SMARTLISTENTRY; // mhod 51
					headerMHOD.complexData = pl.smart.rules;
					headerMHOD.totalSize = headerMHOD.size + pl.smart.rules.size();
					headerMHOD.write(outStream);
				}
				headerMHOD.makeIt(BaseHeader.MHOD);
				headerMHOD.totalSize = MHOD_INT_TOTSIZE;
				headerMHIP.num = 1;
				headerMHOD.index = BaseHeader.PLAYLISTENTRY;
				for (int plii = 0; plii < headerMHYP.numThings; plii++) {
					PlayItem pi = (PlayItem) pl.get(plii);
					if (debug)
						System.err.println("Writing " + pi + " id " + pi.getId());
					headerMHIP.reference = pi.getId(); // ((Integer)pi.get(pi.INDEX)).intValue();
					headerMHIP.reference11 = headerMHIP.reference + 22;
					headerMHIP.write(outStream);
					headerMHOD.reference = headerMHIP.reference;// plii+1;//((Integer)pi.get(PlayItem.ORDER)).intValue();//((Integer)sortIndexMap.get(new
					// Integer(pi.getId()))).intValue();
					headerMHOD.write(outStream);
					pi.setState(pi.STATE_METASYNCED); // TODO: can be a problem if writing failed
				}
				if (hti == 1) {
					if (pl.smart != null && pl.smart.live)
						updateMagicList(pl); // restore list content
					pl.originalName = null; // since the list synced
				}
			}
		}
		directories[0].modified = false;
		outStream.flush();
	}

	public void setInitialCapicity(int size) {
		if (size > 10)
			mainlistInitCapacity = size;
		else
			mainlistInitCapacity = MAINLIST_INIT_CAPACITY;
	}

	/**
	 * returns a collection of available play lists
	 */
	public synchronized List getPlayLists(int size) {
		List result = directories[0].directory;
		if (size > result.size())
			((ArrayList) result).ensureCapacity(size);
		return result;
	}

	public List getPlayLists() {
		return getPlayLists(5);
	}

	/**
	 * check if specified playlist name already exists
	 */
	public boolean playListExists(String playListName) {
		return directories[0].contains(playListName) || renamedPlayLists != null
				&& renamedPlayLists.containsKey(playListName);
	}

	/**
	 * get play list with specified name if playlist doesn't exist, then create and return a new one
	 * 
	 * @param playlist
	 *            name
	 * @param initial
	 *            size of playlist
	 * @return new or existing play list, null if playlist deleted, or name null, or empty, or main play list
	 */
	public PlayList getPlayList(String playList, int size) {
		if (playList == null || playList.length() == 0 || IPOD.equals(playList))
			return null; // iPodFiles
		// assure playlists
		getPlayLists();
		if (directories[0].contains(playList))
			return (PlayList) directories[0].get(playList);
		else if (renamedPlayLists != null && renamedPlayLists.containsKey(playList))
			return renamedPlayLists.get(playList);
		else if (deletedPlayLists == null || deletedPlayLists.contains(playList) == false) {
			PlayList result = new PlayList(playList, false, false);
			if (size > 10)
				result.ensureCapacity(size);
			directories[0].put(playList, result);
			return result;
		}
		return null;
	}

	public PlayList getPlayList(String playList) {
		return getPlayList(playList, TYP_SIZE);
	}

	public PlayList getExistingPlayList(String playList) {
		return (PlayList) directories[0].get(playList);
	}

	public PlayList search(String playlist, String search, int type) {
		PlayList pl = getPlayList(playlist, 0);
		if (pl == null)
			pl = iPodFiles;
		if (pl == null || search == null || search.length() == 0)
			return null;
		PlayList result = new PlayList(search);
		for (PlayItem pi : pl) {
			String m = (String) pi.get(PlayItem.TITLE);
			if (m != null && m.indexOf(search) >= 0) {
				result.add(pi);
				continue;
			}
			if (type == 0) { // type = SearchTypeall
				m = (String) pi.get(PlayItem.ALBUM);
				if (m != null && m.indexOf(search) >= 0) {
					result.add(pi);
					continue;
				}
				m = (String) pi.get(PlayItem.ARTIST);
				if (m != null && m.indexOf(search) >= 0) {
					result.add(pi);
					continue;
				}
			}
		}
		return result;
	}

	public synchronized List getDeletedItems() {
		if (iPodFiles == null)
			return null;
		return iPodFiles.getDeletedItems();
	}

	/**
	 * adds play item in a specified play list
	 */
	public synchronized PlayItem addPlayItem(PlayItem playItem, PlayList playList) {
		if (playItem.isState(BaseItem.STATE_DELETED))
			throw new IllegalArgumentException("Can't add deleted " + playItem);
		// make sure that it's added to main list
		if (iPodFiles == null) {
			iPodFiles = new PlayList(Resources.LABEL_ALL_FILES, false, true);
			iPodFiles.ensureCapacity(mainlistInitCapacity);
		}
		int pii = iPodFiles.indexOf(playItem);
		if (pii < 0) {

			if (iPodFiles.isRemoved(playItem))
				if (playItem.isState(PlayItem.STATE_METASYNCED))
					return null;
				else
					iPodFiles.restore(playItem);

			Integer ix = (Integer) playItem.get(PlayItem.INDEX);
			if (ix == null || ix.intValue() <= 0)
				ix = new Integer(getNextEntry());
			if (directory == null) // reconsider lazy initialization
				directory = new HashMap(mainlistInitCapacity);
			if (debug)
				System.err.println("Storing " + playItem + " with idx " + ix + " in entry " + iPodFiles.size());
			directory.put(ix, playItem);
			iPodFiles.add(playItem);
			registerEntry(ix.intValue());
		} else {
			PlayItem newPI = playItem;
			playItem = iPodFiles.get(pii);
			if (debug)
				System.err
						.printf(
								"Added play item '%s' already found and main play list, so only statistics get updated to '%s'%n",
								playItem, newPI);
			if ((playItem.getState() & BaseItem.STATE_METASYNCED) == BaseItem.STATE_METASYNCED) {
				playItem.set(PlayItem.PLAYED_TIMES, (Integer) newPI.get(PlayItem.PLAYED_TIMES));
				playItem.set(PlayItem.LAST_TIME, (Date) newPI.get(PlayItem.LAST_TIME));
				playItem.set(PlayItem.RATING, (Integer) newPI.get(PlayItem.RATING));
			}
		}

		if (playItem == null)
			return null;
		// add to all sortings
		for (int i = 1; i < directories.length; i++)
			directories[i].add(playItem);
		// add to play list
		if (playList != null && playList != iPodFiles) {
			PlayItem clone_pi = (PlayItem) playItem.clone();
			// TODO: consider as remove the overhead
			if (playList.smart == null || playList.smart.live == false)
				clone_pi.resetState(PlayItem.STATE_METASYNCED);
			playItem = clone_pi;
			playItem.set(playItem.ORDER, playList.size() + 1);
			playList.add(playItem);
		}
		return playItem;
	}

	public synchronized PhotoDB.PhotoItem addPhotoItem(PhotoDB.PhotoItem photo, String album) {
		if (photo.isState(BaseItem.STATE_DELETED))
			throw new IllegalArgumentException("Can't add deleted " + photo);
		// make sure that it's added to main list
		if (photoDirectory == null) {
			photoDirectory = new PhotoDB.PhotoDirectory(10);
		}
		photoDirectory.addItem(photo);
		if (album != null) {
			photoDirectory.addAlbum(album);
			photoDirectory.conect(album, photo.id);
		}
		return photo;
	}
	
	public synchronized PhotoDB.PhotoItem removePhotoItem(PhotoDB.PhotoItem photo, String album) {
		return photo;
	}
	
	public synchronized PlayItem removePlayItem(PlayItem playItem, PlayList playList) {
		playList.remove(playItem);
		if (playList == iPodFiles || playList.isVirtual() || playList.isFileDirectory()) {
			if (playList != iPodFiles)
				iPodFiles.remove(playItem);
			for (int i = 0; i < directories.length; i++)
				directories[i].remove(playItem);
		}		
		return playItem;
	}

	public void sort() {
		if (iPodFiles == null)
			return;
		synchronized (this) {
			Collections.sort(iPodFiles);
		}
		for (int i = 1; i < directories.length; i++) {
			directories[i].sort();
		}
	}

	public synchronized void refreshViews() {
		for (int i = 1; i < directories.length; i++) {
			directories[i].clear();
		}
		if (iPodFiles == null)
			return;
		Iterator i = iPodFiles.iterator();
		while (i.hasNext()) {
			PlayItem pi = (PlayItem) i.next();
			for (int j = 1; j < directories.length; j++)
				directories[j].add(pi);
		}
		sort();
		List<String> pls = getPlayLists();
		clearUpdateCache();
		for (String pln : pls) {
			PlayList pl = getExistingPlayList(pln);
			if (pl.smart != null) {
				pl.clear();
				updateMagicList(pl);
			}
		}
	}

	public void deletePlayList(String playList) {
		if (playList == null) {
			// iPodFiles = null;
		} else {
			if (directories[0].remove(playList) != null) {
				if (deletedPlayLists == null)
					deletedPlayLists = new ArrayList(4);
				deletedPlayLists.add(playList);
			}
		}
	}

	public void rearrangeBefore(String before, String list) {
		directories[0].rearrangeBefore(before, list);
	}

	public void updateMagicList(PlayList playList) {
		System.err.printf("Updating magic %s%n", playList);
		if (playList == null || playList.smart == null || iPodFiles == null || wasUpdatedCache.get(playList) != null)
			return;
		if (debug)
			System.err.printf("Updating magic %s%n", playList);
		List l = iPodFiles;
		// playList.clear();
		playList.smart.mos = (playList.smart.sort & BaseHeader.Smart.SELBY_MOST_FLAG) != 0;
		switch (playList.smart.sort) {
		case BaseHeader.Smart.SELBY_RANDOM:
			// TODO create randomizing map
			// l = iPodFiles;
			break;
		case BaseHeader.Smart.SELBY_ARTIST:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(), new PlayItem.ArtistComparator());
			break;
		case BaseHeader.Smart.SELBY_ALBUM:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(), new PlayItem.AlbumComparator());
			break;
		case BaseHeader.Smart.SELBY_GENRE:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(), new PlayItem.GenreComparator());
			break;
		case BaseHeader.Smart.SELBY_TITLE:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(), new PlayItem.SongComparator());
			break;
		case BaseHeader.Smart.SELBY_YEAR:
		case BaseHeader.Smart.SELBY_YEAR | BaseHeader.Smart.SELBY_MOST_FLAG:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(),
					new PlayItem.GenericFieldComparator/* YearComparator */(PlayItem.YEAR, playList.smart.mos));
			break;
		case BaseHeader.Smart.SELBY_RATING:
		case BaseHeader.Smart.SELBY_RATING | BaseHeader.Smart.SELBY_MOST_FLAG:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(),
					new PlayItem.GenericFieldComparator/* RatingComparator */(PlayItem.RATING, playList.smart.mos));
			break;
		case BaseHeader.Smart.SELBY_LASTPLAY:
		case BaseHeader.Smart.SELBY_LASTPLAY | BaseHeader.Smart.SELBY_MOST_FLAG:
			Collections
					.sort(l = (List) ((ArrayList) iPodFiles).clone(),
							new PlayItem.GenericFieldComparator/* LastPlayComparator */(PlayItem.LAST_TIME,
									playList.smart.mos));
			break;
		case BaseHeader.Smart.SELBY_ADDED:
		case BaseHeader.Smart.SELBY_ADDED | BaseHeader.Smart.SELBY_MOST_FLAG:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(),
					new PlayItem.GenericFieldComparator/* DateAddedComparator */(PlayItem.CREATE_TIME,
							playList.smart.mos));
			break;
		case BaseHeader.Smart.SELBY_PLAYCOUNT:
		case BaseHeader.Smart.SELBY_PLAYCOUNT | BaseHeader.Smart.SELBY_MOST_FLAG:
			Collections.sort(l = (List) ((ArrayList) iPodFiles).clone(),
					new PlayItem.GenericFieldComparator/* PlayCountComparator */(PlayItem.PLAYED_TIMES,
							!playList.smart.mos));
			break;

		}

		Iterator it = l.iterator();
		while (it.hasNext()) {
			PlayItem pi;
			if (playList.smart.match(playList, pi = (PlayItem) it.next(), this))
				addPlayItem(pi, playList);
		}
		// TODO figure out if list was changed
		wasUpdatedCache.put(playList, playList);
	}

	public void clearUpdateCache() {
		clearUpdateCache(null);
	}

	/** the method clears update cache to prevent rebuild smart playlist
	 * 
	 * @param playlist
	 */
	public void clearUpdateCache(PlayList playlist) {
		if (playlist == null)
			wasUpdatedCache.clear();
		else
			wasUpdatedCache.remove(playlist);
	}

	public synchronized void resetImagesToTags(String dev) {
		//	int i=0;
		for (PlayItem pi : iPodFiles) {
			//			try {
			MediaFormat mf = pi.getAttachedFormat();
			if (mf == null)
				mf = MediaFormatFactory.createMediaFormat(pi.getFile(dev));
			if (mf != null && mf.getMediaInfo().getAttribute(MediaInfo.PICTURE) != null)
				pi.setImage(new ArtworkDB.ImageItem());
			else
				pi.setImage(null);
			//pi.attachedMedia = null; // for GC
			//	i++;
			//}catch(Error e) {
			//System.err.printf("%s happened at %d for %s%n", e, i, pi);
			//throw e;
			//}
		}
	}

	public synchronized void renamePlayList(String playList, String newPlayList) {
		if (playList == null) {
			// iPodFiles = null;
		} else {
			PlayList pl = (PlayList) directories[0].remove(playList);
			if (pl != null) {
				pl.rename(newPlayList);
				if (renamedPlayLists == null)
					renamedPlayLists = new HashMap<String, PlayList>(4);
				renamedPlayLists.put(pl.getOrigalName(), pl);
				directories[0].put(newPlayList, pl);
			}
		}
	}

	public void clearDeleted() {
		if (iPodFiles != null)
			iPodFiles.clearDeleted();
		for (int i = 1; i < directories.length; i++) {
			directories[i].clearDeleted();
		}
		if (deletedPlayLists != null)
			deletedPlayLists.clear();
		if (renamedPlayLists != null)
			renamedPlayLists.clear();
	}

	public boolean isChanged() {
		if (debug)
			System.err.println("isChanged");
		if (iPodFiles != null && iPodFiles.isChanged())
			return true;
		if (debug)
			System.err.println("Files Ok");
		if (directories[0].isChanged())
			return true;
		List l = getPlayLists();
		for (int i = 0; i < l.size(); i++) {
			PlayList pl = getPlayList((String) l.get(i));
			if (pl != null && pl.isChanged()) {
				if (debug)
					System.err.println("PL:<" + pl + ">'s changed");
				return true;
			}
		}
		for (int i = 1; i < directories.length; i++) {
			synchronized (directories[i]) {
				int ds = directories[i].size();
				for (int j = 0; j < ds; j++) {
					if (directories[i].get(j).isChanged())
						return true;
				}
			}
		}
		if (debug)
			System.err.println("Dirs Ok");
		if (deletedPlayLists != null && deletedPlayLists.size() > 0)
			return true;
		if (debug)
			System.err.println("Deleted Files Ok");
		if (renamedPlayLists != null && renamedPlayLists.size() > 0)
			return true;
		if (debug)
			System.err.println("Renamed Files Ok");
		return false;
	}
	
	public boolean isPhotoChanged() {
		return photoDirectory != null && photoDirectory.isChanged();
	}

	public Object getRoot() {
		return this;
	}

	public Object getChild(Object parent, int index) {
		if (parent == getRoot()) {
			//System.err.printf("Child p: %s, i: %d, a: %s, d: %s%n", parent, index, iPodFiles, Arrays.toString(directories));
			if (iPodFiles != null) {
				if (index == 0)
					return iPodFiles;
				else if (index < directories.length + 1)
					return directories[index - 1];
				else if (index == directories.length + 1)
					return photoDirectory;
			} else if (photoDirectory != null)
				return photoDirectory;
		} else if (parent instanceof PlayDirectory) {
			return ((PlayDirectory) parent).get(index);
		} else if (parent instanceof PlayList) {
			return ((PlayList) parent).get(index);
		} else if (parent instanceof PhotoDB.PhotoDirectory) {
			return ((PhotoDB.PhotoDirectory) parent).getPhotos(index);
		}
		return null;
	}

	public int getChildCount(Object parent) {
		if (parent == getRoot()) {
			return (iPodFiles == null ? 0 : 1 + directories.length) + (photoDirectory==null?0:1) /*for photos*/;
		} else if (parent instanceof PlayDirectory) {
			return ((PlayDirectory) parent).size();
		} else if (parent instanceof PlayList) {
			// return ((PlayList)parent).size();
		} else if (parent instanceof PhotoDB.PhotoDirectory) {
			return ((PhotoDB.PhotoDirectory)parent).getNumberAlbums();
		}
		return 0;
	}

	public boolean isLeaf(Object node) {
		return node instanceof PlayItem || node instanceof ItemList;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		if (newValue == null || newValue.toString().length() == 0)
			return;
		Object o = path.getLastPathComponent();
		if (o instanceof PlayList) {
			renamePlayList(o.toString(), newValue.toString());
			fireTreeNodesChanged(new Object[] { getChild(this, 1) }, new Object[] { o });
		} else if (o instanceof ItemList) {
			
		}
	}

	public void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeModelListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TreeModelEvent(source, path, childIndices, children);
				((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
			}
		}
	}

	public void fireTreeNodesChanged(Object[] path, Object[] children) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeModelListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TreeModelEvent(this, path, null, children);
				System.err.println("Change event " + e);
				((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
			}
		}
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (parent == getRoot() && child != null) {
			if (child == iPodFiles)
				return 0;
			else if (child instanceof PlayDirectory) {
				for (int i = 0; i < directories.length; i++)
					if (child == directories[i])
						return i + 1;
			}
		} else if (parent instanceof PlayDirectory) {
			if (child instanceof PlayList) {
				return ((PlayDirectory) parent).indexOf(((PlayList) child).name);
			}
		}
		return -1;
	}

	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}

	public String toString() {
		return "iPod";
	}

	/**
	 * returns list of MAC file names of all files stored on iPod
	 * 
	 * @return list of MAC file names of all files stored on iPod
	 */
	public synchronized List getCopiedFiles(String dev) {
		ArrayList<String> result = new ArrayList<String>(iPodFiles != null ? iPodFiles.size() : 0);
		if (iPodFiles != null) {
			Iterator i = iPodFiles.iterator();
			while (i.hasNext()) {
				PlayItem pi = (PlayItem) i.next();
				if (pi.isState(BaseItem.STATE_COPIED)) {
					String path = (String) pi.get(PlayItem.FILENAME);
					// TODO need runtime code change for avoid each time check
					if (dev != null) {
						int p = path.lastIndexOf(':');
						String name = p < 0 ? path : path.substring(p+1);
						// TODO get "MPEG audio file" as const
						String ext = "MPEG audio file".equals(pi.get(PlayItem.FILETYPE)) ? MP3.MP3 : MP4.TYPE;
						String correctedName = makeValidFileName(name, 0, ext);
						if (name.length() > correctedName.length() || name.equalsIgnoreCase(correctedName) == false) {
							//System.err.printf("Name %s requires change to %s%n", name, correctedName);
							File f = pi.getFile(dev);
							//System.err.printf("ren %s to %s%n", f, new File(f.getParent(), correctedName));
							if (f.renameTo(new File(f.getParent(), correctedName))) {
								path = path.substring(0, p + 1) + correctedName;
								pi.set(PlayItem.FILENAME, path);
								pi.resetState(BaseItem.STATE_METASYNCED);
								System.err.printf("Updated name: %s%n", path);
							}
						}
					}
					result.add(path);
				}
			}
		}
		if (debug)
			System.err.println("Known files :\n" + result);
		return result;
	}

	/**
	 * get list all items needed to copy to iPod
	 */
	public synchronized List getToCopyList() {
		List result = new ArrayList(100);
		if (iPodFiles == null)
			return result;
		Iterator i = iPodFiles.iterator();
		while (i.hasNext()) {
			PlayItem pi = (PlayItem) i.next();
			if (pi.isState(pi.STATE_COPIED) == false) {
				result.add(pi);
			}
		}
		return result;
	}

	public void processAll(Visitor<PlayItem> visitor) {
		if (iPodFiles == null)
			return;
		Iterator i = iPodFiles.iterator();
		while (i.hasNext()) {
			// if play item is vistable then
			// ((Visitable) i.next()).accept(visitor);
			visitor.visit((PlayItem) i.next());
		}
	}

	protected void initPlayDirectories() {
		directories = new PlayDirectory[DIRECTORY_ENTRIES.length];
		for (int i = 0; i < DIRECTORY_ENTRIES.length; i++) {
			directories[i] = new PlayDirectory(DIRECTORY_ENTRIES[i]);
		}
		// TODO: read from config NUM_HOLDERS
	}

	public void setAwareCompilation(boolean on) {
		directories = Arrays.copyOf(directories, DIRECTORY_ENTRIES.length); // adjust size
		for (int i = 0; i < DIRECTORY_ENTRIES.length; i++) {
			if (DIRECTORY_ENTRIES[i].selector == PlayItem.ARTIST)
				if (on) {
					directories[i] = new PlayDirectory(DIRECTORY_ENTRIES[i]) {
						@Override
						protected String getDistinction(PlayItem playItem) {
							if (Boolean.TRUE.equals(playItem.get(PlayItem.COMPILATION)))
								return null;
							return super.getDistinction(playItem);
						}
					};
					directories = Arrays.copyOf(directories, directories.length + 1);
					directories[directories.length - 1] = new PlayDirectory(new Descriptor(
							Resources.LABEL_COMPILATIONS, PlayItem.COMPILATION)) {
						@Override
						protected String getDistinction(PlayItem playItem) {
							if (Boolean.TRUE.equals(playItem.get(PlayItem.COMPILATION)))
								return (String) playItem.get(PlayItem.ALBUM);
							return null;
						}
					};
				} else
					directories[i] = new PlayDirectory(DIRECTORY_ENTRIES[i]);
		}
	}

	public static String createFilePath(String name, int counter, int modifier, String extType) {
		return MessageFormat.format(PATH_IPODMUSIC_MAC, new Object[] { new Integer(counter % NUM_HOLDERS),
				makeValidFileName(name, modifier, extType) });
	}

	// TODO: consider using 255 entries table for different
	// languages to make the names for meaningful
	public static String makeValidFileName(String s, int mod, String extension) {
		char[] result = s.toCharArray();
		int lim = extension == null?MAX_FILE_NAME_LEN:MAX_FILE_NAME_LEN-extension.length()-1;
		// TODO converting to string and get length can be cheaper
		int rb = Math.min((mod == 0 ? lim : (lim-(int)Math.log10(mod))), result.length);
		
		int dp = -1;
		int v = result[0] & 255;
		result[0] = charTransTbl[v];
		if (result[0] == '.' || result[0] == ' ') {
			result[0] = '0';
		}
		for (int i = 1; i < rb; i++) {
			v = result[i] & 255;
			result[i] = charTransTbl[v];
			if (result[i] == '.') {
				if (dp > 0)
					result[dp] = ' ';
				dp = i;
			}
		}
		if (dp > 0)
			rb = dp;
		return new String(result, 0, rb) + (mod == 0l?"":mod) + (extension==null?"":'.' + extension);
	}

	protected static void fillTransTbl() {
		for (int i = 0; i < charTransTbl.length; i++) {
			charTransTbl[i] = (char) i;
		}
		for (int i = 0; i < 16; i++)
			charTransTbl[i] += '@';
		for (int i = 16; i < 32; i++)
			charTransTbl[i] += '@' - 16;
		for (int i = 32; i < '0'; i++)
			charTransTbl[i] += '@' - 32;
		charTransTbl['.'] = '.';
		charTransTbl['-'] = '-';
		charTransTbl[' '] = ' ';
		for (int i = ':'; i < '@'; i++)
			charTransTbl[i] += 10;
		charTransTbl['\\'] = '_';
		for (int i = '{'; i <= '~'; i++)
			charTransTbl[i] -= 16;
		for (int i = 128; i < 256; i++)
			charTransTbl[i] = charTransTbl[i - 127];
	}

	static char[] charTransTbl = new char[256];
	static {
		fillTransTbl();
	}

	public static void main(String[] args) {
		System.out.println(makeValidFileName(args[0], 0, "extension"));
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++)
				System.out.print(charTransTbl[i * 16 + j]);
			System.out.println();
		}
	}

	class u {
		u() {
			System.err.println("Debug " + debug);
		}
	}

	public static class PlayDirectory {
		public Descriptor descriptor;

		List<String> directory;

		Map content;

		boolean modified;

		public PlayDirectory(Descriptor descriptor) {
			this.descriptor = descriptor;
			directory = new ArrayList<String>(10);
			content = new WeakHashMap(10);
		}

		public int size() {
			return directory.size();
		}

		public synchronized void sort() {
			// TODO: sort will depend on type of the list
			if (descriptor.selector == PlayItem.ARTIST || descriptor.selector == PlayItem.COMPOSER)
				Collections.sort(directory, ITunesDB.SONGINFOCOMPARATOR_IA);
			else
				Collections.sort(directory, ITunesDB.SONGINFOCOMPARATOR);
			// Sorting conetns of every directory entry
			Iterator i = content.values().iterator();
			while (i.hasNext()) {
				Collections.sort((PlayList) i.next());
			}
		}

		public synchronized void clear() {
			directory.clear();
			content.clear();
		}

		public PlayList get(int index) {
			return (PlayList) content.get(directory.get(index));
		}

		public PlayList get(Object playList) {
			return (PlayList) content.get(playList);
		}

		public boolean contains(String listName) {
			return directory.contains(listName);
		}

		public int indexOf(String listName) {
			return directory.indexOf(listName);
		}

		public int entriesLike(String like, String[] result) {
			// TODO: use assumption that elements of directory sorted
			int next = 0;
			for (String entry : directory) {
				if (debug)
					System.err.printf("Checking %s against %s\n", entry, like);
				if (entry.startsWith(like))
					if (next < result.length)
						result[next++] = entry;
					else
						break;
			}
			return next;
		}

		public synchronized void rearrangeBefore(String beforeList, String listName) {
			int si = indexOf(beforeList);
			int ei = indexOf(listName);
			if (si == ei || si < 0 || ei < 0)
				return;
			// TODO: consider just swapping start/end indexes
			if (ei > si) {
				for (int i = ei; i > si; i--)
					directory.set(i, directory.get(i - 1));
				directory.set(si, listName);
			} else {
				for (int i = ei; i < si; i++)
					directory.set(i, directory.get(i + 1));
				directory.set(si, listName);
			}
			modified = true;
		}

		public boolean isChanged() {
			return modified;
		}

		public synchronized Object putObject(Object name, Object object) {
			if (name == null)
				return null;
			if (directory.contains(name) == false)
				directory.add((String) name);
			content.put(name, object);
			return object;
		}

		public PlayList put(Object listName, PlayList playList) {
			return (PlayList) putObject(listName, playList);
		}

		public PlayDirectory put(String directoryName, PlayDirectory playDirectory) {
			return (PlayDirectory) putObject(directoryName, playDirectory);
		}

		public synchronized Object remove(String listName) {
			int i = -1;
			if ((i = directory.indexOf(listName)) >= 0) {
				Object result = content.get(listName);
				directory.remove(i);
				// weak
				// content.remove(listName);
				return result;
			}
			return null;
		}

		public synchronized PlayItem remove(PlayItem playItem) {
			Iterator i = content.values().iterator();
			while (i.hasNext()) {
				PlayList pl = (PlayList) i.next();
				pl.remove(playItem);
				/*
				 * do not remove empty lists, to avoid rebuild tree if (pl.isEmpty()) { i.remove(); //?? remove(pl.toString()); }
				 */
			}
			return playItem;
		}

		public void clearDeleted() {
			Iterator i = content.values().iterator();
			while (i.hasNext()) {
				PlayList pl = (PlayList) i.next();
				pl.clearDeleted();
			}
		}

		protected String getDistinction(PlayItem playItem) {
			return (String) playItem.get(descriptor.selector);
		}

		/*
		 * public Iterator iterator() { return directory.iterator(); }
		 */

		public synchronized PlayItem add(PlayItem playItem/* , PlayList excludeList */) {
			String distinction = getDistinction(playItem);
			// System.err.println("Adding "+playItem+" to "+distinction);
			if (distinction != null) {
				PlayList pl = get(distinction);
				// if (excludeList == null || excludeList != pl) {
				if (pl == null) {
					pl = put(distinction, new PlayList(distinction)); // true, false
				}
				if (pl.contains(playItem) == false)
					pl.add(playItem);
				// }
			}
			return playItem;
		}

		public String toString() {
			return descriptor != null ? descriptor.toString() : "null";
		}
	}

	/**
	 * A comparator class for ordering, artist, songs, and albums based only on meaningful information and ignoring articles, quotes, cases.
	 * <p>
	 * <b>Note: this comparator imposes orderings that are inconsistent with equals.</b>
	 */
	public static class SongInfoComparator implements Comparator, java.io.Serializable {
		private boolean ignoreArticles;

		public SongInfoComparator() {
		}

		public SongInfoComparator(boolean ignoreArticles) {
			this.ignoreArticles = ignoreArticles;
		}

		public int compare(Object o1, Object o2) {
			return PlayItem.normalize((String) o1, ignoreArticles).compareTo(
					PlayItem.normalize((String) o2, ignoreArticles));
		}

	}

	public static final SongInfoComparator SONGINFOCOMPARATOR = new SongInfoComparator();

	public static final SongInfoComparator SONGINFOCOMPARATOR_IA = new SongInfoComparator(true);

	private static final boolean debug = false;

	 // true;

	private static final boolean writesortheaders_ = false;
}