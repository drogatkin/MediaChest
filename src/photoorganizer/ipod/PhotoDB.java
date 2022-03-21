/* MediaChest - PhotoDB.java
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
 *  $Id: PhotoDB.java,v 1.5 2013/04/19 02:08:36 cvs Exp $
 * Created on Mar 30, 2008
 */
package photoorganizer.ipod;

import static photoorganizer.ipod.ITunesDB.checkEOF;


import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.swing.Icon;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.gen.MediaComponent;
import photoorganizer.Resources;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.ipod.ArtworkDB.ImageItem;

public class PhotoDB {
	public final static String PHOTO_DB = "Photo Database";

	public final static String PATH_IPOD_PHOTOC = "Photos";

	public final static String PATH_PHOTO_FULL_RES = "Full Resolution";

	public final static String PHOTO_DB_PATH = PATH_IPOD_PHOTOC + File.separatorChar + PHOTO_DB;

	public enum ImageTypes5 {
		UYVY_704X480, RGB565_320X240, RGB565_123X88, RGB565_50X41
	};

	PhotoDirectory directory;

	static HashMap<ImageTypes5, ImageDescr> thumbs;

	static HashMap<Integer, ImageTypes5> typeMap;

	static {
		thumbs = new HashMap<ImageTypes5, ImageDescr>(4);
		typeMap = new HashMap<Integer, ImageTypes5>(4);
		thumbs.put(ImageTypes5.UYVY_704X480, new ImageDescr(704, 480, 691200, true, 1019));
		typeMap.put(704, ImageTypes5.UYVY_704X480);
		thumbs.put(ImageTypes5.RGB565_123X88, new ImageDescr(123, 88, 22880, false, 1024));
		typeMap.put(123, ImageTypes5.RGB565_123X88);
		thumbs.put(ImageTypes5.RGB565_320X240, new ImageDescr(320, 240, 153600, false, 1015));
		typeMap.put(320, ImageTypes5.RGB565_320X240);
		thumbs.put(ImageTypes5.RGB565_50X41, new ImageDescr(320, 240, 4100, false, 1036));
		typeMap.put(50, ImageTypes5.RGB565_50X41);
	}

	private final static boolean __debug = true;

	private final static boolean __debughard = true;

	public synchronized long read(InputStream inStream) throws IOException {
		if (__debug)
			System.err.println("====READ PHOTOS=====");
		long result = 0;
		BaseHeader header = new BaseHeader();
		header.read(inStream);
		if ("mhfd".equals(header.signature) == false)
			ArtworkDB.riseIO("mhfd", "Start", header.signature);
		int totalSize = header.totalSize;
		int size = header.size;
		while (totalSize - size > 0) {
			size += header.read(inStream);
			if (__debughard)
				System.err.printf("Reading %s%n", header);
			if ("mhsd".equals(header.signature) == false)
				ArtworkDB.riseIO("mhsd", "mhfd", header.signature);
			if (header.thingsList) { // always 1st?
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if (__debughard)
					System.err.printf("Reading %s%n", header);
				if ("mhli".equals(header.signature) == false)
					ArtworkDB.riseIO("mhli", "mhsd", header.signature);
				int nfs = header.num;
				if (directory == null)
					directory = new PhotoDirectory(nfs);
				for (int i = 0; i < nfs; i++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if (__debughard)
						System.err.printf("Reading %s%n", header);
					if ("mhii".equals(header.signature) == false)
						ArtworkDB.riseIO("mhii", "mhli", header.signature);
					PhotoItem pi = new PhotoItem(header.reference11);
					int nsh = header.num;
					for (int j = 0; j < nsh; j++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream, true);
						if (__debughard)
							System.err
									.printf("Read complex structures as mhod-mhni-mhod combined in one  %s%n", header);
						if ("mhod".equals(header.signature) == false)
							ArtworkDB.riseIO("mhod", j == 0 ? "mhii" : "mhni", header.signature);
						// TODO: make something index based shift
						if (ArtworkDB.MHOD_TYPE[BaseHeader.IMAGE_CONTAINER - 1].equals(header.type))
							pi.setFullImage(header.data);
						else if (ArtworkDB.MHOD_TYPE[BaseHeader.IMAGETHUMBNAIL_CONTAINER - 1].equals(header.type)) {
							ImageItem ii = new ImageItem() {
								@Override
								protected String getImageRootPath() {
									return PATH_IPOD_PHOTOC;
								}
							};
							ii.set(ImageItem.NOWPLAY_WIDTH, header.hash1);
							ii.set(ImageItem.NOWPLAY_HEIGHT, header.hash2);
							ii.set(ImageItem.NOWPLAY_FILE, header.data);
							ii.set(ImageItem.NOWPLAY_OFFSET, header.reference);
							ii.set(ImageItem.NOWPLAY_LENGTH, (long) header.fileSize);
							if (__debug && header.hp > 0)
								System.err.printf("Padding is : %d%n", header.hp);
							ii.set(ImageItem.NOWPLAY_HORZ_PAD, header.hp);
							ii.set(ImageItem.NOWPLAY_VERT_PAD, header.vp);
							pi.addThumbnail(ii, typeMap.get(header.hash1));
						}
						System.err
								.printf("Adding image %d x %d size %d%n", header.hash1, header.hash2, header.fileSize);
						if (__debug)
							System.err.printf("Processing image info %d %dx%d offs %d in %s(%d) %s%n", j, header.hash1,
									header.hash2, header.reference, header.data, header.fileSize, header.type);
					}
					pi.setState(BaseItem.STATE_COPIED + BaseItem.STATE_METASYNCED);
					directory.addItem(pi);
					if (__debug)
						System.err.printf("%s%n", pi);
				}
			} else if (header.playList) { // albums
				if (__debug)
					System.err.printf("Processing photo playlist%n");
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if (__debug)
					System.err.printf("Reading %s%n", header);
				if ("mhla".equals(header.signature) == false)
					ArtworkDB.riseIO("mhla", "mhsd", header.signature + " no play list");
				// process album entries
				int nae = header.num;
				for (int i = 0; i < nae; i++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if (__debug)
						System.err.printf("Header: %s%n", header);
					if ("mhba".equals(header.signature) == false)
						ArtworkDB.riseIO("mhba", "mhla", header.signature);
					int nmhod = header.num;
					int nmhai = header.numThings;
					for (int mi = 0; mi < nmhod; mi++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream, true);
						if (__debug) {
							if (__debughard)
								System.err.printf("Reading %s%n", header);
							System.err.printf("Album: %s%n", header.data);
						}
						if ("mhod".equals(header.signature) == false)
							ArtworkDB.riseIO("mhod", "mhba", header.signature);
					}
					String album = directory.addAlbum(header.data);
					for (int mi = 0; mi < nmhai; mi++) {
						checkEOF(totalSize - size, header);
						size += header.read(inStream);
						if (__debug) {
							if (__debughard)
								System.err.printf("Reading %s%n", header);
							System.err.printf("Image: %d%n", header.reference11);
						}
						if ("mhia".equals(header.signature) == false)
							ArtworkDB.riseIO("mhia", mi == 0 ? "mhod" : "mhia", header.signature);
						directory.conect(album, header.reference11);
					}
				}
			} else if (header.filesList) {
				if (__debug)
					System.err.printf("%nPROCESSING photo files%n%n%n");
				checkEOF(totalSize - size, header);
				size += header.read(inStream);
				if (__debug)
					System.err.printf("Reading %s%n", header);
				if ("mhlf".equals(header.signature) == false)
					ArtworkDB.riseIO("mhlf", "mhsd", header.signature);
				int ne = header.num;
				for (int fi = 0; fi < ne; fi++) {
					checkEOF(totalSize - size, header);
					size += header.read(inStream);
					if (__debug) {
						if (__debughard)
							System.err.printf("Reading %s%n", header);
						System.err.printf("Reading image ref %d, %s%n", header.reference11, header.fileSize);
					}
					if ("mhif".equals(header.signature) == false)
						ArtworkDB.riseIO("mhif", "mhlf", header.signature);
					result += header.fileSize;
				}
			} else if (header.albumList) {
				if (__debug)
					System.err.printf("Processing albums%n");
				checkEOF(totalSize - size, header);
				size += header.skip(inStream);
			}
		}
		return result;
	}

	public synchronized long write(OutputStream photodb, String dev, boolean copyPhoto) throws IOException {
		long result = 0;
		if (__debug)
			System.err.println("****WRITE PHOTODB*****");

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
		ItemList<PhotoItem> photos = directory.getPhotos();
		headerMHLI.num = photos.items.size();
		for (PhotoItem pi: photos.items) {
			BaseHeader headerMHII = new BaseHeader();
			headerMHII.makeIt(BaseHeader.MHII);
			
			BaseHeader headerMHOD = new BaseHeader();
			headerMHOD.makeIt(BaseHeader.MHOD);
			BaseHeader headerMHNI = new BaseHeader();
			headerMHNI.makeIt(BaseHeader.MHNI);
			if (copyPhoto && (pi.getState() & BaseItem.STATE_COPIED) > 0) {
				headerMHNI.totalSize += headerMHOD.size + pi.fullImage.length() * 2 + 0x18 + 12; //			    
				headerMHOD.totalSize += headerMHNI.totalSize;
				headerMHII.totalSize += headerMHOD.totalSize;
			}
			// 4 thumbs
			for (PhotoDB.ImageTypes5 e:PhotoDB.ImageTypes5.values()) {
				headerMHNI.totalSize += headerMHOD.size + ":Thumbs:FXXXX_1.ithmb".length() * 2 + 0x18 + 12; //			    
				headerMHOD.totalSize += headerMHNI.totalSize;
				headerMHII.totalSize += headerMHOD.totalSize;
			}
			headerMHSD.totalSize += headerMHII.totalSize;
		}
		headerMHSD.totalSize += headerMHLI.totalSize;
		headerMHFD.totalSize += headerMHSD.totalSize;
		headerMHSD.makeIt(BaseHeader.MHSD);
		

		return result;
	}

	public static class PhotoItem extends BaseItem implements MediaFormat, MediaInfo {

		MediaFormat fullMedia;

		String fullImage;

		HashMap<ImageTypes5, ImageItem> thumbnail;

		Icon thumbnailIcon;

		HashMap<String, ArtworkDB.ImageItem> thumbs;

		static int idSequence;

		PhotoItem(int id) {
			thumbnail = new HashMap<ImageTypes5, ImageItem>(ImageTypes5.values().length);
			this.id = id;
		}

		public PhotoItem(MediaFormat photo) {
			fullMedia = photo;
			setFullImage(getFile().getPath());
			id = getNextId();
		}

		public static synchronized void updateSequence(int currentId) {
			if (idSequence < currentId)
				idSequence = currentId;
		}

		public static synchronized int getNextId() {
			idSequence++;
			return idSequence;
		}

		public MediaFormat getFullMedia(String dev) {
			if (fullImage == null)
				throw new IllegalStateException("An image file path isn't set.");
			if (fullMedia == null)
				fullMedia = MediaFormatFactory.createMediaFormat(isState(STATE_COPIED) == false ? new File(fullImage)
						: new File(dev + PATH_IPOD_PHOTOC + File.separatorChar
								+ fullImage.replace(':', File.separatorChar)), null, false);
			if (thumbnailIcon == null && thumbnail != null)
				thumbnailIcon = thumbnail.get(ImageTypes5.RGB565_123X88).getImage(true, dev);
			return fullMedia;
		}

		public String makeiPodFullResPath() {
			Date date = (Date) getAttribute(MediaInfo.DATETIMEORIGINAL);
			Calendar c = Calendar.getInstance();
			File f = getFile();
			if (date == null)
				c.setTimeInMillis(f.lastModified());
			else
				c.setTime(date);
			return ":" + PATH_PHOTO_FULL_RES + ":" + c.get(Calendar.YEAR) + ":" + c.get(Calendar.MONTH) + ":"
					+ c.get(Calendar.DAY_OF_MONTH) + ":" + f.getName();
		}

		void setFullImage(String file) {
			fullImage = file;
		}

		void addThumbnail(ImageItem tn, ImageTypes5 type) {
			thumbnail.put(type, tn);
		}

		MediaFormat getFullMedia() {
			return fullMedia;

		}

		@Override
		public String toString() {
			return "photo " + id;
		}

		@Override
		Object get(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		int getId() {
			return id;
		}

		@Override
		void set(int index, int value) {
			// TODO Auto-generated method stub

		}

		@Override
		void set(int index, String value) {
			if (index == PlayItem.FILENAME)
				fullImage = value;
		}

		////// MediaFormat & Info methods //////////////
		@Override
		public InputStream getAsStream() throws IOException {
			return fullMedia.getAsStream();
		}

		@Override
		public String getDescription() {
			return "iPod Photo id: " + id;
		}

		@Override
		public File getFile() {
			return fullMedia.getFile();
		}

		@Override
		public long getFileSize() {
			return getFile().length();
		}

		@Override
		public String getFormat(int type) {
			if (type == 0 || (type & STILL) > 0)
				return fullMedia.getFormat(type);
			return null;
		}

		@Override
		public MediaInfo getMediaInfo() {
			return this;
		}

		@Override
		public String getName() {
			return getFile().getName();
		}

		@Override
		public Icon getThumbnail(Dimension size) {
			if (thumbnailIcon != null)
				return thumbnailIcon;
			if (fullMedia != null)
				return fullMedia.getThumbnail(size);
			return null;
		}

		@Override
		public byte[] getThumbnailData(Dimension size) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getThumbnailType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getType() {
			return STILL;
		}

		@Override
		public URL getUrl() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isValid() {
			return id > 0 && fullMedia != null && fullMedia.isValid();
		}

		@Override
		public boolean renameTo(File dest) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object getAttribute(String name) {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getAttribute(name);
			return null;
		}

		@Override
		public boolean getBoolAttribute(String name) {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getBoolAttribute(name);
			return false;
		}

		@Override
		public double getDoubleAttribute(String name) {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getDoubleAttribute(name);
			return 0;
		}

		@Override
		public Object[] getFiveMajorAttributes() {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getFiveMajorAttributes();
			return null;
		}

		@Override
		public float getFloatAttribute(String name) {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getFloatAttribute(name);
			return 0;
		}

		@Override
		public int getIntAttribute(String name) {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getIntAttribute(name);
			return 0;
		}

		@Override
		public long getLongAttribute(String name) {
			if (fullMedia != null)
				return fullMedia.getMediaInfo().getLongAttribute(name);
			return 0;
		}

		@Override
		public void setAttribute(String name, Object value) {
			if (fullMedia != null)
				fullMedia.getMediaInfo().setAttribute(name, value);

		}
		
		@Override
		public <C extends MediaComponent> C[] getComponents() {
			return null;
		}
	}

	public static int rgbToYUV(int rgb565) {
		// see details http://msdn2.microsoft.com/en-us/library/ms893078.aspx
		int R = (rgb565 >> 8) & 0xfff8;
		int G = (rgb565 >> 3) & 0xfff8;
		int B = (rgb565 << 3);
		int Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
		int U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
		int V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
		return (Y << 4 + ((U << 2) & 0xe) + (V & 0x3));
	}

	public static class PhotoDirectory {
		HashMap<Integer, PhotoItem> photos;

		HashMap<String, ArrayList<Integer>> albums;

		PhotoDirectory(int size) {
			if (size > 0)
				photos = new HashMap<Integer, PhotoItem>(size);
			else
				photos = new HashMap<Integer, PhotoItem>();
			albums = new HashMap<String, ArrayList<Integer>>(2);
		}

		public int conect(String album, int id) {
			if (photos.containsKey(id)) {
				ArrayList<Integer> content = albums.get(album);
				if (content != null)
					content.add(id);
				else
					throw new IllegalArgumentException("No album: " + album);
			} else
				throw new IllegalArgumentException("Item with id: " + id + " doesn't exist in the library");
			return id;
		}

		public String addAlbum(String data) {
			if (albums.containsKey(data) == false) {
				albums.put(data, new ArrayList<Integer>(20));
				return data;
			}
			return null;
		}

		ItemList<PhotoItem> getPhotos() {
			return new ItemList<PhotoItem>(photos.values(), Resources.LABEL_ALLPHOTOS);
		}

		void addItem(PhotoItem item) {
			// TODO check for duplicates
			photos.put(item.id, item);
		}

		int getNumberAlbums() {
			return albums.size();
		}

		ItemList<PhotoItem> getPhotos(int album) {
			String[] names = albums.keySet().toArray(new String[albums.size()]);
			ArrayList<PhotoItem> items = new ArrayList<PhotoItem>(100);
			for (int id : albums.get(names[album])) {
				items.add(photos.get(id));
			}
			return new ItemList<PhotoItem>(items, names[album]);

		}

		@Override
		public String toString() {
			return Resources.LABEL_PHOTOS;
		}

		public boolean isChanged() {
			for (PhotoItem pi : photos.values())
				if (pi.isState(BaseItem.STATE_COPIED + BaseItem.STATE_METASYNCED) == false)
					return true;
			return false;
		}

	}

	protected static class ImageDescr {
		int w, h, size;

		boolean yuv;

		int imageid;

		int offset;

		ImageDescr(int pw, int ph, int ps, boolean pyuv, int id) {
			w = pw;
			h = ph;
			size = ps;
			yuv = pyuv;
			imageid = id;
		}

	}
}
