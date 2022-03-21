/* MediaChest - ArtworkDB.java
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
 *
 *  $Id: ArtworkDB.java,v 1.36 2009/01/12 03:28:40 dmitriy Exp $
 * Created on Mar 30, 2005
 */

package photoorganizer.ipod;

import static photoorganizer.ipod.BaseItem.IMG_FILE_LENGTHS;
import static photoorganizer.ipod.ITunesDB.PATH_IPOD_ROOT;
import static photoorganizer.ipod.ITunesDB.checkEOF;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import mediautil.gen.MediaFormat;
import mediautil.image.ImageUtil;

import org.aldan3.util.Stream;

import photoorganizer.formats.MediaFormatFactory;

/**
 * @author dmitriy
 * 
 * 
 */
public class ArtworkDB {
	public final static String ARTWORK_DB = "ArtworkDB";

	public final static String PATH_IPOD_ARTWORK = PATH_IPOD_ROOT + File.separatorChar + "Artwork";

	public final static String PATH_IPOD_ARTWORK_DB = PATH_IPOD_ARTWORK + File.separatorChar + ARTWORK_DB;

	public final static String NEW_SUFF = ".new";

	protected final static int NUM_IMAGES = 2;

	protected final static int CORREL_ID = 0x64;

	protected final static int FILENAME_THUMB_ID = BaseItem.IPOD_PHOTO?0x3f8:0x404;

	protected List<Integer> storageFiles;

	public synchronized long read(InputStream inStream, Map<Long, PlayItem> connections) throws IOException {
		if (debug)
			System.err.println("====READ ARTWORK=====");
		
		long result = 0;
		BaseHeader header = new BaseHeader();
		header.read(inStream);
		if (debug)
			System.err.printf("Reading %s%n", header);
		if ("mhfd".equals(header.signature) == false)
			riseIO("mhfd", "Start", header.signature);
		int totalSize = header.totalSize;
		int size = header.size;
		while (totalSize - size > 0) {
			size += header.read(inStream);
			if (debug)
				System.err.printf("Reading %s%n", header);
			if ("mhsd".equals(header.signature) == false)
				riseIO("mhsd", "mhfd", header.signature);
			if (header.thingsList) { // always 1st?
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if (debug)
					System.err.printf("Reading %s%n", header);
				if ("mhli".equals(header.signature) == false)
					riseIO("mhli", "mhsd", header.signature + " no file list");
				int nfs = header.num;
				for (int i = 0; i < nfs; i++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if (debug)
						System.err.printf("Reading %s%n", header);
					if ("mhii".equals(header.signature) == false)
						riseIO("mhii", "mhli", header.signature);
					ImageItem ii = new ImageItem();
					ii.id = header.reference11;
					if (header.fileSize == 0)
						System.err.println("Filesize 0 in header: " + header);
					ii.set(ImageItem.ORIGIMAGE_LENGTH, (long) header.fileSize);
					result += header.fileSize; // ??
					// attach to play item
					PlayItem pi = connections == null ? null : connections.get(header.hash1
							+ (long) (header.hash2 << 32));
					if (pi != null)
						pi.setImage(ii);
					else if (debug)
						System.err.printf("Can't attach image to playitem by id: %d", header.hash1
							+ (long) (header.hash2 << 32));
					int nsh = header.num;
					for (int j = 0; j < nsh; j++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream, true);
						if (debug)
							System.err.printf("Read complex structures as mhod-mhni-mhod combined in one hdr %s%n", header);
						if ("mhod".equals(header.signature) == false)
							riseIO("mhod", "mhii", header.signature);
						// TODO: make something index based shift
						if (j == 0) {
							ii.set(ImageItem.WIDTH, header.hash1);
							ii.set(ImageItem.HEIGHT, header.hash2);
							ii.set(ImageItem.TUMBNAIL_FILE, header.data);
							ii.set(ImageItem.THUMBNAIL_OFFSET, header.reference);
							ii.set(ImageItem.THUMBNAIL_LENGTH, (long) header.fileSize);
						} else {
							ii.set(ImageItem.NOWPLAY_WIDTH, header.hash1);
							ii.set(ImageItem.NOWPLAY_HEIGHT, header.hash2);
							ii.set(ImageItem.NOWPLAY_FILE, header.data);
							ii.set(ImageItem.NOWPLAY_OFFSET, header.reference);
							ii.set(ImageItem.NOWPLAY_LENGTH, (long) header.fileSize);
						}
					}
					ii.setState(BaseItem.STATE_METASYNCED + BaseItem.STATE_COPIED);
					if (debug)
						System.err.printf("Play item %s offsets/length/size %d, %d, %dx%d (now play %d - %d, %dx%d)%n", pi, ii.get(ImageItem.THUMBNAIL_OFFSET), ii
								.get(ImageItem.THUMBNAIL_LENGTH), ii.get(ImageItem.WIDTH), ii.get(ImageItem.HEIGHT), 
								ii.get(ImageItem.NOWPLAY_OFFSET), ii.get(ImageItem.NOWPLAY_LENGTH), ii.get(ImageItem.NOWPLAY_WIDTH), ii.get(ImageItem.NOWPLAY_HEIGHT));
				}
			} else if (header.playList) {
				checkEOF(totalSize - size, header);
				System.err.printf("%n===Processing albums for artwork==%n%n");
				size += header.read(inStream);
				if ("mhla".equals(header.signature) == false)
					riseIO("mhla", "mhsd", header.signature + " no play list");
				// process album entries
				int nae = header.num;
				for (int i = 0; i < nae; i++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if ("mhba".equals(header.signature) == false)
						riseIO("mhba", "mhla", header.signature);
					if (debug)
						System.err.printf("Header: %s%n", header);
					int nmhod = header.num;
					int nmhai = header.numThings;
					for (int mi = 0; mi < nmhod; mi++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream, true);
						if ("mhod".equals(header.signature) == false)
							riseIO("mhod", "mhba", header.signature);
						if (debug)
							System.err.printf("==>>>>>>>>Alum: %s%n", header.data);
					}
					//String album = directory.addAlbum(header.data);
					for (int mi = 0; mi < nmhai; mi++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream);
						if ("mhia".equals(header.signature) == false)
							riseIO("mhia", mi == 0 ? "mhod" : "mhia", header.signature);
						if (debug) 
							System.err.printf("++++++++>>>>>>>Image: %d%n%s%n", header.reference11, header);
						//directory.conect(album, header.reference11);
					}
				}
			} else if (header.filesList) {
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if (debug)
					System.err.printf("Reading %s%n", header);
				if ("mhlf".equals(header.signature) == false)
					riseIO("mhlf", "mhsd", header.signature + " no file list");
				int nfs = header.num;
				storageFiles = new ArrayList<Integer>(nfs);
				for (int i = 0; i < nfs; i++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if (debug)
						System.err.printf("Reading %s%n", header);
					storageFiles.add(header.reference11);
					if ("mhif".equals(header.signature) == false)
						riseIO("mhif", "mhlf", header.signature);
				}
			} else
				throw new IOException("Unrecognizable type of mhsd - " + header);
		}
		return result;
	}

	static void riseIO(String header, String after, String found) throws IOException {
		throw new IOException(MessageFormat.format("Header {0} expected after {1}, but found {2}.", new Object[] {
				header, after, found }));
	}

	/**
	 * This method populate artwork database, due specific of storing images
	 * this method stands apart of main design, when a caller deal with
	 * file/stream manipulation. This design can be changed, when the method
	 * produces a job for a caller to fill images presentation files.
	 * 
	 * @param outStream
	 * @param connection to playitems having artwork
	 * @param access to iPod root directory
	 */
	public void write(OutputStream outStream, Map<Long, PlayItem> connections, String dev) throws IOException {
		if (debug)
			System.err.println("****WRITE ARTWORK*****");
		long timeMark = System.currentTimeMillis();
		BaseHeader headerMHFD = new BaseHeader();
		headerMHFD.makeIt(BaseHeader.MHFD);
		headerMHFD.num = 3;
		// size calculation iteration
		BaseHeader headerMHSD = new BaseHeader();
		headerMHSD.makeIt(BaseHeader.MHSD);
		headerMHSD.thingsList = true;

		BaseHeader headerMHLI = new BaseHeader();
		headerMHLI.makeIt(BaseHeader.MHLI);

		// loop to find all songs having artwork
		headerMHLI.num = 0;
		BaseHeader headerMHII = new BaseHeader();
		headerMHII.makeIt(BaseHeader.MHII);
		BaseHeader headerMHOD = new BaseHeader();
		headerMHOD.makeIt(BaseHeader.MHOD);
		BaseHeader headerMHNI = new BaseHeader();
		headerMHNI.makeIt(BaseHeader.MHNI);
		// it seems that all images are in one file
		// only offset incremented, so
		// it makes sense to reread image for pi which gets it not
		// from pic
		int id = CORREL_ID;
		int fileId = FILENAME_THUMB_ID;
		OutputStream[] imageOutStreams = new OutputStream[2];
		try {
			imageOutStreams[0] = new FileOutputStream(dev + PATH_IPOD_ARTWORK + File.separatorChar
					+ getFileName(fileId, 1, false) + NEW_SUFF);
			imageOutStreams[1] = new FileOutputStream(dev + PATH_IPOD_ARTWORK + File.separatorChar
					+ getFileName(fileId + 1, 1, false) + NEW_SUFF);

			Map<String, int[]> albums = new HashMap<String, int[]>(connections.size()/2+1); // album
			Map<String, ImageItem> imageitems = new HashMap<String, ImageItem>(connections.size()/2+1);
			// name
			// ->
			// image
			// id
			List<Integer> imageFiles = new ArrayList<Integer>();
			imageFiles.add(fileId);
			imageFiles.add(fileId + 1);
			Iterator<Long> i = connections.keySet().iterator();
			// this fragment assume that file name always the same
			// it can be a strategy to advance to next names as only offset
			// reaches max_int
			int[] currentOffsets = new int[2];
			currentOffsets[0] = 0;
			currentOffsets[1] = 0;
			while (i.hasNext()) {
				Long lk = i.next();
				PlayItem pi = connections.get(lk);

				headerMHLI.num++;
				// 
				String albumArtistUn = getArtworkId(pi);
				int[] offsets = albums.get(albumArtistUn); // not quite
				ImageItem ii = imageitems.get(albumArtistUn);
				// reliable to be unique

				if (offsets == null) {
					ii = pi.getImage();
					imageitems.put(albumArtistUn, ii);
					// Assign a new offset for album
					// do not spread thumbnail and now play to different file
					// indexes
					offsets = new int[NUM_IMAGES];
					System.arraycopy(currentOffsets, 0, offsets, 0, currentOffsets.length);
					albums.put(albumArtistUn, offsets);
					storeImages(pi, imageOutStreams[0], imageOutStreams[1], dev);
					if (Integer.MAX_VALUE - currentOffsets[0] > IMG_FILE_LENGTHS[0]) { // check only for longest file
						if (debug)
							System.err.printf("Artwork images files offsets %d, %d before%n", currentOffsets[0], currentOffsets[1]);
						currentOffsets[0] += ((Long) ii.get(ImageItem.THUMBNAIL_LENGTH)).intValue();
						currentOffsets[1] += ((Long) ii.get(ImageItem.NOWPLAY_LENGTH)).intValue();
						if (debug)
							System.err.printf("Artwork images files offsets %d, %d after%n", currentOffsets[0], currentOffsets[1]);
					} else {
						if (debug)
							System.err.printf("Rolling artwork images files%n");
						fileId += 2;
						imageFiles.add(fileId);
						imageFiles.add(fileId + 1);
						currentOffsets[0] = 0;
						currentOffsets[1] = 0;
						try {
							imageOutStreams[0].close();
							imageOutStreams[1].close();
						} catch (IOException ioe) {

						}
						imageOutStreams[0] = new FileOutputStream(dev + PATH_IPOD_ARTWORK + File.separatorChar
								+ getFileName(fileId, 1, false) + NEW_SUFF);
						imageOutStreams[1] = new FileOutputStream(dev + PATH_IPOD_ARTWORK + File.separatorChar
								+ getFileName(fileId + 1, 1, false) + NEW_SUFF);
					}
				} else {
					if (debug)
						System.err.printf("No artwork writing for %s since it is in an album which is covered already %s %n", pi, albumArtistUn);
					pi.getImage().setState(BaseItem.STATE_COPIED);
				}
				assert ii != null;
				// define 2 entries for thumbnail and now play of the following
				// headers
				// mhod
				// mhni
				// mhod
				headerMHII.makeIt(BaseHeader.MHII);

				for (int mhi = 0; mhi < NUM_IMAGES; mhi++) {
					// len of :fxxxx.ithmb
					headerMHOD.makeIt(BaseHeader.MHOD);
					headerMHNI.makeIt(BaseHeader.MHNI);

					String s = getFileName((fileId + mhi), 1, true);
					// 
					headerMHNI.totalSize += headerMHOD.size + s.length() * 2 + 0x18 + 12; //			    
					headerMHOD.totalSize = +headerMHNI.totalSize;
					headerMHII.totalSize += headerMHOD.totalSize;
					// TODO: reconsider the settings
					ii.set(mhi == 0 ? ImageItem.THUMBNAIL_OFFSET : ImageItem.NOWPLAY_OFFSET, offsets[mhi]);
					ii.set(mhi == 0 ? ImageItem.TUMBNAIL_FILE : ImageItem.NOWPLAY_FILE, s);
					// ii.set(mhi==0?ii.WIDTH:ii.SMALL_WIDTH,mhi==0?ii.THUMBNAIL_SIZE.width:ii.NOWPLAY_SIZE.width);
					// ii.set(mhi==0?ii.HEIGHT:ii.SMALL_HEIGHT,mhi==0?ii.THUMBNAIL_SIZE.height:ii.NOWPLAY_SIZE.height);
				}
				headerMHLI.totalSize += headerMHII.totalSize;
			}
			headerMHSD.totalSize += headerMHLI.totalSize;
			headerMHFD.totalSize += headerMHSD.totalSize; // mhsd 1 for actual
			// files
			headerMHFD.numThings = headerMHLI.num * 2; // vague
			BaseHeader headerMHSD2 = new BaseHeader();
			headerMHSD2.makeIt(BaseHeader.MHSD);
			headerMHSD2.playList = true;
			BaseHeader headerMHLA = new BaseHeader();
			headerMHLA.makeIt(BaseHeader.MHLA);
			headerMHSD2.totalSize += headerMHLA.size;
			headerMHFD.totalSize += headerMHSD2.totalSize;
			BaseHeader headerMHSD3 = new BaseHeader();
			headerMHSD3.makeIt(BaseHeader.MHSD);
			headerMHSD3.filesList = true;
			BaseHeader headerMHLF = new BaseHeader();
			headerMHLF.makeIt(BaseHeader.MHLF);
			headerMHSD3.totalSize += headerMHLF.size;
			// loop by unique ids in albums
			BaseHeader headerMHIF = new BaseHeader();
			headerMHIF.makeIt(BaseHeader.MHIF);
			headerMHSD3.totalSize += headerMHIF.size * imageFiles.size();
			headerMHFD.totalSize += headerMHSD3.totalSize;
			if (debug) {
				System.err.printf("Header size calculation took %d ms.%n", (System.currentTimeMillis() - timeMark));
				timeMark = System.currentTimeMillis();
			}
			// ====================== start writing =============================
			headerMHFD.write(outStream);
			headerMHSD.write(outStream);
			headerMHLI.write(outStream);
			i = connections.keySet().iterator();
			id = CORREL_ID;
			fileId = FILENAME_THUMB_ID;
			BaseHeader[][] imageHeaders = new BaseHeader[NUM_IMAGES][3];
			for (int mhi = 0; mhi < NUM_IMAGES; mhi++) {
				imageHeaders[mhi][0] = new BaseHeader();
				imageHeaders[mhi][1] = new BaseHeader();
				imageHeaders[mhi][2] = new BaseHeader();
			}
			currentOffsets[0] = 0;
			currentOffsets[1] = 0;
			while (i.hasNext()) {
				Long lk = i.next();
				PlayItem pi = connections.get(lk);

				headerMHLI.num--;
				headerMHII.makeIt(BaseHeader.MHII);
				headerMHOD.makeIt(BaseHeader.MHOD);
				headerMHII.num = NUM_IMAGES;
				headerMHII.reference11 = id++; // order of inc
				headerMHII.hash1 = (int) (lk & 0xffffffff);
				headerMHII.hash2 = (int) ((lk >> 32) & 0xffffffff);
				headerMHII.fileSize = 33923; // size in bytes of orig source image
				String albumArtistUn = getArtworkId(pi);
				int[] offsets = albums.get(albumArtistUn);
				assert offsets != null;
				ImageItem ii = imageitems.get(albumArtistUn);
				for (int mhi = 0; mhi < NUM_IMAGES; mhi++) {
					ii.set(mhi == 0 ? ImageItem.THUMBNAIL_OFFSET : ImageItem.NOWPLAY_OFFSET, offsets[mhi]);
					imageHeaders[mhi][0].makeIt(BaseHeader.MHOD);
					imageHeaders[mhi][0].index = 2;
					imageHeaders[mhi][1].makeIt(BaseHeader.MHNI);
					imageHeaders[mhi][1].reference11 = fileId + mhi;
					imageHeaders[mhi][1].fileSize = ((Long) ii.get(mhi == 0 ? ImageItem.THUMBNAIL_LENGTH : ImageItem.NOWPLAY_LENGTH))
							.intValue();
					if (debug && imageHeaders[mhi][1].fileSize == 0)
						System.err.printf("Image size not set for %s / %s%n", pi, ii);
					imageHeaders[mhi][1].num = 1;
					imageHeaders[mhi][1].reference = offsets[mhi];
					if (debug)
						System.err.printf("Image %s offset %d%n", pi, offsets[mhi]);
					imageHeaders[mhi][1].hash1 = ((Integer) ii.get(mhi == 0 ? ImageItem.WIDTH : ImageItem.NOWPLAY_WIDTH)).intValue();
					imageHeaders[mhi][1].hash2 = ((Integer) ii.get(mhi == 0 ? ImageItem.HEIGHT : ImageItem.NOWPLAY_HEIGHT)).intValue();
					imageHeaders[mhi][2].makeIt(BaseHeader.MHOD);
					imageHeaders[mhi][2].data = getFileName(imageHeaders[mhi][1].reference11, 1, true);
					imageHeaders[mhi][2].index = 3;
					imageHeaders[mhi][2].reference11 = 2;
					imageHeaders[mhi][2].totalSize += imageHeaders[mhi][2].data.length() * 2 + 12;
					imageHeaders[mhi][1].totalSize += imageHeaders[mhi][2].totalSize;
					imageHeaders[mhi][0].totalSize += imageHeaders[mhi][1].totalSize;
					headerMHII.totalSize += imageHeaders[mhi][0].totalSize;
				}
				headerMHII.write(outStream);
				for (int mhi = 0; mhi < NUM_IMAGES; mhi++) {
					imageHeaders[mhi][0].write(outStream, true); // mhod
					imageHeaders[mhi][1].write(outStream); // mhni
					imageHeaders[mhi][2].write(outStream, true); // mhod
				}
				ii.setState(BaseItem.STATE_METASYNCED);
				if (debug) {
					System.err.printf("Headers writing time took %d ms.%n", (System.currentTimeMillis() - timeMark));
				}
			}

			assert headerMHLI.num == 0;
			headerMHSD2.write(outStream);
			headerMHLA.write(outStream);
			headerMHSD3.write(outStream);
			headerMHLF.num = imageFiles.size();
			headerMHLF.write(outStream);
			for (int fi = 0; fi < headerMHLF.num; fi++) {
				headerMHIF.reference11 = imageFiles.get(fi);
				headerMHIF.fileSize = IMG_FILE_LENGTHS[(fi & 1)];
				headerMHIF.write(outStream);
			}
			// delete old files
			if (storageFiles != null)
				for (Integer fi : storageFiles) {
					if (new File(dev + PATH_IPOD_ARTWORK + File.separatorChar + getFileName(fi, 1, false)).delete() == false)
						System.err.printf("Can't delete %s%n", getFileName(fi, 1, false));
				}
			storageFiles = imageFiles;
		} finally {
			try {
				imageOutStreams[0].close();
				imageOutStreams[1].close();
			} catch (IOException ioe) {

			}
		}
		// rename
		if (storageFiles != null)
			for (Integer fi : storageFiles)
				if (new File(dev + PATH_IPOD_ARTWORK + File.separatorChar + getFileName(fi, 1, false) + NEW_SUFF)
						.renameTo(new File(dev + PATH_IPOD_ARTWORK + File.separatorChar + getFileName(fi, 1, false))) == false)
					System.err.printf("Can't rename from %s" + NEW_SUFF + " to %s%n", getFileName(fi, 1, false),
							getFileName(fi, 1, false));
	}

	protected String getFileName(int id, int sub, boolean iPod) {
		return (iPod ? ":F" : "F") + id + '_' + sub + ".ithmb";
	}

	protected static String getArtworkId(PlayItem pi) {
		return pi.get(PlayItem.ALBUM) + "#$%" + pi.get(PlayItem.ARTIST);
	}

	protected ImageItem storeImages(PlayItem pi, OutputStream tnos, OutputStream npos, String dev) throws IOException {
		ImageItem ii = pi.getImage();
		assert ii != null;
		if (ii.isState(BaseItem.STATE_COPIED)) {
			for (int i = 0; i < NUM_IMAGES; i++) {
				String fn = (String) ii.get(i == 0 ? ImageItem.TUMBNAIL_FILE : ImageItem.NOWPLAY_FILE);
				fn = convertToOsPath(fn);
				if (debug)
					System.err.printf("Image[%d] %s copied from offs %d len %d%n", i, pi, ((Integer) ii
							.get(i == 0 ? ImageItem.THUMBNAIL_OFFSET : ImageItem.NOWPLAY_OFFSET)).intValue(), ((Long) ii
							.get(i == 0 ? ImageItem.THUMBNAIL_LENGTH : ImageItem.NOWPLAY_LENGTH)).longValue());
				if (((Long) ii.get(i == 0 ? ImageItem.THUMBNAIL_LENGTH : ImageItem.NOWPLAY_LENGTH)) == 0) {
					System.err.println("Image length not specified for " + pi);
					return ii;
				}
				InputStream is = new FileInputStream(dev + PATH_IPOD_ARTWORK + fn);
				try {
					if (is.skip(((Integer) ii.get(i == 0 ? ImageItem.THUMBNAIL_OFFSET : ImageItem.NOWPLAY_OFFSET)).intValue()) != ((Integer) ii
							.get(i == 0 ? ImageItem.THUMBNAIL_OFFSET : ImageItem.NOWPLAY_OFFSET)).intValue())
						throw new IOException("Skip failed.");
					/*
					 * int len =
					 * i==0?(Integer)ii.get(ii.WIDTH)*(Integer)ii.get(ii.HEIGHT)*2:
					 * (Integer)ii.get(ii.SMALL_WIDTH)*(Integer)ii.get(ii.SMALL_HEIGHT)*2;
					 * ii.set(i==0?ii.THUMBNAIL_LENGTH:ii.NOWPLAY_LENGTH,
					 * (long)len);
					 */
					Stream.copyStream(is, i == 0 ? tnos : npos, /* len */
					((Long) ii.get(i == 0 ? ImageItem.THUMBNAIL_LENGTH : ImageItem.NOWPLAY_LENGTH)).longValue());
				} finally {
					is.close();
				}
			}
			ii.setState(BaseItem.STATE_COPIED);
			return ii;
		}
		// new image from ID3
		String imagePath = (String) pi.get(PlayItem.ARTWORK); // TODO: make it URL
		if (imagePath != null) { // ext image
			if (debug)
				System.err.printf("Image %s from file %s%n", pi, imagePath);
			Image im = Toolkit.getDefaultToolkit().createImage(imagePath);
			if (im == null)
				throw new IOException(String.format("Can't read image from %s", imagePath));
			for (int i = 0; i < NUM_IMAGES; i++) {
				Dimension d = i == 0 ? BaseItem.THUMBNAIL_SIZE : BaseItem.NOWPLAY_SIZE;
				ii.storeImage(d, ImageUtil.getScaled(im, d, Image.SCALE_FAST, null), i == 0 ? tnos : npos);
			}
			ii.setState(BaseItem.STATE_COPIED);
			return ii;
		}
		if (debug)
			System.err.printf("Image %s from ID3%n", pi);
		MediaFormat mf = pi.getAttachedFormat();
		if (mf == null)
			mf = MediaFormatFactory.createMediaFormat(pi.getFile(dev));
		if (mf != null) {
			for (int i = 0; i < NUM_IMAGES; i++) {
				Dimension d = i == 0 ? BaseItem.THUMBNAIL_SIZE : BaseItem.NOWPLAY_SIZE;
				Icon ico = mf.getThumbnail(d);
				assert ico instanceof ImageIcon;
				ii.storeImage(d, ((ImageIcon) ico).getImage(), i == 0 ? tnos : npos);
			}
			ii.setState(BaseItem.STATE_COPIED);
		} else {
			System.err.printf("No images can be retrieved from %s%N", pi.getFile(dev));
		}
		return ii;
	}

	public static String convertToOsPath(String path) {
		if (path == null)
			return null;
		return path.replace(':', File.separatorChar);
	}

	public static ImageIcon getImage(boolean thumbnail, PlayItem pi, String path) {
		ImageItem ii = pi.getImage();
		if (ii == null)
			return null;
		if (ii.isState(BaseItem.STATE_COPIED))
			return ii.getImage(thumbnail, path);
		if (debug)
			System.err.printf("Image not set (as copied) for %s%n", pi);
		String imagePath = (String) pi.get(PlayItem.ARTWORK); // TODO: make it URL
		if (imagePath != null) {// ext image
			// check if URL
			if (new File(imagePath).exists())
				return new ImageIcon(Toolkit.getDefaultToolkit().createImage(imagePath));
			else
				try {
					return new ImageIcon(Toolkit.getDefaultToolkit().createImage(new URL(imagePath)));
				} catch (MalformedURLException e) {
					System.err.println("Can't load picture using: " + imagePath + ", " + e);
				}
		}
		return (ImageIcon) pi.getAttachedFormat().getThumbnail(thumbnail ? BaseItem.THUMBNAIL_SIZE : ii.NOWPLAY_SIZE);
	}

	public static final String[] MHOD_TYPE = { "Albumname",  "Imagelocator", "Filename", "Unknown", "Fullimagename"};

	public static String getMHODTypeName(int type) {
		type -= 1;
		if (type >= 0 && type < MHOD_TYPE.length)
			return MHOD_TYPE[type];
		return "MHOD type = " + (type + 1) + " is not supported";
	}

	
	public static class ImageItem extends BaseItem {
		// TODO: solve design problem, create ImageItem for every
		// image with type, or store all images in one ImageItem

		public static final int WIDTH = 0;

		public static final int HEIGHT = 1;

		public static final int NOWPLAY_HEIGHT = 3;

		public static final int NOWPLAY_WIDTH = 2;

		public static final int THUMBNAIL_OFFSET = 4;

		public static final int NOWPLAY_OFFSET = 5;
		
		public static final int NOWPLAY_VERT_PAD = 6;
		
		public static final int NOWPLAY_HORZ_PAD = 7;
		
		public static final int THUMBNAIL_VERT_PAD = 8;
		
		public static final int THUMBNAIL_HORZ_PAD = 9;
		
		public static final int INT_PROP_START = WIDTH;

		public static final int INT_PROP_END = THUMBNAIL_HORZ_PAD;

		public static final int TUMBNAIL_IMAGE = 100;

		public static final int NOWPLAY_IMAGE = 101;

		public static final int TUMBNAIL_FILE = 200;

		public static final int NOWPLAY_FILE = 201;

		public static final int STR_PROP_START = TUMBNAIL_FILE;

		public static final int STR_PROP_END = NOWPLAY_FILE;

		public static final int ORIGIMAGE_LENGTH = 300;

		public static final int THUMBNAIL_LENGTH = 301;

		public static final int NOWPLAY_LENGTH = 302;

		public static final int LONG_PROP_START = ORIGIMAGE_LENGTH;

		public static final int LONG_PROP_END = NOWPLAY_LENGTH;

		Image imageTumbnail;

		Image imageNowPlay;

		URL sourceImageUrl;

		String[] s_props;

		long[] l_props;

		int[] i_props;

		public ImageItem() {
			s_props = new String[STR_PROP_END - STR_PROP_START + 1];
			l_props = new long[LONG_PROP_END - LONG_PROP_START + 1];
			i_props = new int[INT_PROP_END - INT_PROP_START + 1];
		}

		public void set(int index, Object data) {
			set(index, data.toString());
		}

		public void set(int index, String data) {
			s_props[index - STR_PROP_START] = data;
		}

		public void set(int index, int data) {
			i_props[index - INT_PROP_START] = data;
		}

		public void set(int index, long data) {
			l_props[index - LONG_PROP_START] = data;

		}

		public Object get(int index) {
			// TODO: possible optimization based on knowledge intervals
			// boundaries
			if (index >= INT_PROP_START && index <= INT_PROP_END)
				return i_props[index - INT_PROP_START];
			else if (index >= LONG_PROP_START && index <= LONG_PROP_END)
				return l_props[index - LONG_PROP_START];
			else if (index >= STR_PROP_START && index <= STR_PROP_END)
				return s_props[index - STR_PROP_START];
			return null;
		}

		public int getId() {
			return id;
		}

		protected String getImageRootPath() {
			return PATH_IPOD_ARTWORK;
		}
		
		public ImageIcon getImage(boolean full, String path) {
			InputStream iis = null;
			try {
				String fn = (String) get(full ? NOWPLAY_FILE : TUMBNAIL_FILE);
				if (fn == null || fn.length() == 0)
					throw new IOException("No image file defined for song");
				if (fn.charAt(0) == ':')
					fn = File.separator + fn.substring(1).replace(':', File.separatorChar);
				if (debug)
					System.err.println("Getting image: " + path + PATH_IPOD_ARTWORK + fn);
				iis = new FileInputStream(path + getImageRootPath() + fn);
				int w = i_props[(full ?NOWPLAY_WIDTH : WIDTH ) - INT_PROP_START];
				int h = i_props[(full ? NOWPLAY_HEIGHT : HEIGHT ) - INT_PROP_START];
				int hp = i_props[(full ? NOWPLAY_HORZ_PAD : THUMBNAIL_HORZ_PAD ) - INT_PROP_START];
				int vp = i_props[(full ? NOWPLAY_VERT_PAD : THUMBNAIL_VERT_PAD ) - INT_PROP_START];
				int pix[] = new int[w * h];
				int index = 0;
				if (hp != 0) {
					// correct padding
					int size = (int) l_props[(full ? NOWPLAY_LENGTH : THUMBNAIL_LENGTH ) - LONG_PROP_START];
					if (debug)
						System.err.printf("Old padding %d%n", hp);
					hp = size/(h+vp)/2-w;
					if (debug)
						System.err.printf("Corrected padding %d%n", hp);
				}
				FileChannel fc = ((FileInputStream) iis).getChannel();
				fc.position(i_props[(full ? NOWPLAY_OFFSET: THUMBNAIL_OFFSET ) - INT_PROP_START]);
				ByteBuffer bb = ByteBuffer.allocate(1024);
				bb = bb.order(ByteOrder.LITTLE_ENDIAN);
				bb.position(bb.capacity());
				if (debug)
					System.err.printf("Image %dx%d paddings (%d-%d) at %d%n", w, h, hp, vp, i_props[(full ? NOWPLAY_OFFSET : THUMBNAIL_OFFSET )
							- INT_PROP_START]);
				for (int y = 0; y < h+vp; y++) {
					for (int x = 0; x < w+hp; x++) {
						if (bb.hasRemaining() == false) {
							fc.read((ByteBuffer) bb.clear());
							bb.rewind();
						}
						short color = bb.getShort();
						if (x < w && y < h) {
							int red = (color & (((1 << 5) - 1) << 11)) >> (11 - 3);
							int green = (color & (((1 << 6) - 1) << 5)) >> (5 - 2);
							int blue = (color & ((1 << 5) - 1)) << 3;
							pix[index++] = 0xff000000 | (red << 16) | (green << 8) | blue;
						} 
					}
				}
				fc.close();
				return new ImageIcon(Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, pix, 0, w)));
				// getImage()
			} catch (IOException io) {
				System.err.println("Problem of reading image " + io);
			} finally {
				if (iis != null)
					try {
						iis.close();
					} catch (IOException io) {
					}
			}
			return null;
		}

		protected void writeBlack(Dimension d, OutputStream os) throws IOException {
			// TODO improve performance by allocating byte array for one scan
			for (int j = 0; j < d.height; j++)
				for (int i = 0; i < d.width; i++) {
					os.write(0);
					os.write(0);
				}
			os.flush();
		}

		public void storeImage(Dimension d, Image image, OutputStream os) throws IOException {
			if (debug && (d.height <= 0 || d.width <= 0))
				new Exception("Passed zero dimension").printStackTrace();
			// check status if it is on iPod, just copy
			// size may need to use from it
			Dimension nd = ImageUtil.getImageSize(image, true);
			int w = nd.width;
			int h = nd.height;
			if (w <= 0 || h <= 0) {
				if (debug)
					System.err.println("Problem in size calc.");
				w = d.width;
				h = d.height;
			}
			int[] pixels = new int[w * h];
			PixelGrabber pg = new PixelGrabber(image, 0, 0, w, h, pixels, 0, w);
			try {
				pg.grabPixels();
			} catch (InterruptedException e) {
				System.err.println("Interrupted waiting for pixels!");
				writeBlack(d, os);
				return;
			}
			if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
				System.err.println("Image fetch aborted or errored");
				writeBlack(d, os);
				return;
			}
			// FileChannel fc = os.getChannel() ;
			os = new BufferedOutputStream(os, 1024 * 20);
			if (pg.getHeight() != h || pg.getWidth() != w)
				System.err.printf("Grab buffer size mismatch %dx%d -> %dx%d%n", w, h, pg.getWidth(), pg.getHeight());
			if (debug)
				System.err.printf("Write im %dx%d%n", w, h);
			byte[] p565 = new byte[2];
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					int p = pixels[j * w + i];
					p565[1] = (byte) (((p >> 16) & 0xf8) + ((p >> (8 + 5)) & 0x7));
					p565[0] = (byte) (((p >> (8 - 3)) & 0xe0) + ((p >> 3) & 0x1f));
					os.write(p565);
				}

				for (int i = 0; i < d.width - w; i++) {
					os.write(0);
					os.write(0);
				}
			}
			os.flush();
			if (d.width >= NOWPLAY_SIZE.width) {
				set(NOWPLAY_LENGTH, (long) d.width * h * 2);
				set(NOWPLAY_WIDTH, d.width);
				set(NOWPLAY_HEIGHT, h);
			} else {
				set(THUMBNAIL_LENGTH, (long) d.width * h * 2);
				set(WIDTH, d.width);
				set(HEIGHT, h);
			}
			// getPixels();
		}
	}

	private static final boolean debug = //true;
		false;

}
