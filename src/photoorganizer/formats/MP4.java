/* MediaChest - MP4
 * Copyright (C) 2001-2003 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: MP4.java,v 1.47 2013/05/21 07:37:33 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaComponent;
import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.ImageUtil;
import photoorganizer.Controller;
import photoorganizer.Resources;

/**
 * For support AAC (QuickTime) formats
 */
public class MP4 implements MediaFormat {
	public static final String MP4 = "MP4";

	public static final String AAC = "AAC";

	public static final String TYPE = "M4A";

	public static final String ALAC = "Apple Lossless audio file";

	public static final String[] EXTENSIONS = { MP4, AAC };

	// see
	// http://developer.apple.com/documentation/QuickTime/APIREF/SOURCESIV/atomidcodes.htm
	// � -> 0xA9
	public static final int MOVIEDATAATOMTYPE = BasicIo.asInt("mdat");

	public static final int SKIPATOMTYPE = BasicIo.asInt("skip");

	public static final int FREEATOMTYPE = BasicIo.asInt("free");

	public static final int WIDEATOMTYPE = BasicIo.asInt("wide");

	public static final int MOVIEAID = BasicIo.asInt("moov");

	public static final int TRACKAID = BasicIo.asInt("trak");

	public static final int RGNCLIPAID = BasicIo.asInt("clip");

	public static final int MATTEAID = BasicIo.asInt("matt");

	public static final int EDITSAID = BasicIo.asInt("edts");

	public static final int KQTVRTRACKREFARRAYATOMTYPE = BasicIo.asInt("tref");

	public static final int MEDIAAID = BasicIo.asInt("mdia");

	public static final int MEDIAINFOAID = BasicIo.asInt("minf");

	public static final int DATAINFOAID = BasicIo.asInt("dinf");

	public static final int USERDATAAID = BasicIo.asInt("udta");

	public static final int SAMPLETABLEAID = BasicIo.asInt("stbl");

	public static final int COMPRESSEDMOVIEAID = BasicIo.asInt("cmov");

	public static final int REFERENCEMOVIERECORDAID = BasicIo.asInt("rmra");

	public static final int REFERENCEMOVIEDESCRIPTORAID = BasicIo.asInt("rmda");

	public static final int NAME = BasicIo.asInt("\u00a9nam");

	public static final int MP4_ARTIST = BasicIo.asInt("\u00a9ART");

	public static final int MP4_ALBUM = BasicIo.asInt("\u00a9alb");

	public static final int COMMENTARY = BasicIo.asInt("\u00a9cmt");

	public static final int MP4_DATE = BasicIo.asInt("\u00a9day");

	public static final int TOOL = BasicIo.asInt("\u00a9too");

	public static final int MP4_GENRE = BasicIo.asInt("gnre");

	public static final int CUSTOM_GENRE = BasicIo.asInt("\u00a9gen");
	
	public static final int MP4_LYRICS = BasicIo.asInt("\u00a9lyr");
	
	public static final int MP4_ALBUM_ARTIST = BasicIo.asInt("aART");
	// TODO other tags from http://code.google.com/p/mp4v2/wiki/iTunesMetadata

	public static final int CUSTOM_REQ = BasicIo.asInt("\u00a9req");

	public static final int MP4_TRACK = BasicIo.asInt("trkn");

	public static final int DISK_NUMBER = BasicIo.asInt("disk");

	public static final int WRITER = BasicIo.asInt("\u00a9wrt");

	public static final int GROUPING = BasicIo.asInt("\u00a9grp");

	public static final int MP4_BPM = BasicIo.asInt("tmpo");

	public static final int FREE_FORM = BasicIo.asInt("----");

	public static final int MEAN = BasicIo.asInt("mean");

	public static final int NAME_MEAN = BasicIo.asInt("mean");

	public static final int DATA = BasicIo.asInt("data");

	public static final int MP4_COMPILATION = BasicIo.asInt("cpil");

	public static final int HDRL = BasicIo.asInt("hdrl");

	public static final int HDLR = BasicIo.asInt("hdlr");

	public static final int ILST = BasicIo.asInt("ilst");

	public static final int STSAMPLEDESCAID = BasicIo.asInt("stsd");

	public static final int MEDIAHEADERAID = BasicIo.asInt("mdhd");

	public static final int MOVIEHEADERAID = BasicIo.asInt("mvhd");

	public static final int TRACKHEADERAID = BasicIo.asInt("tkhd");

	public static final int QUICKTIMEIMAGEFILEMETADATAATOM = BasicIo.asInt("meta");

	public static final int STSAMPLESIZEAID = BasicIo.asInt("stsz");

	public static final int COVERART = BasicIo.asInt("covr");

	// TODO add more tags from http://atomicparsley.sourceforge.net/mpeg-4files.html
	
	
	public static final int LOSSLESS = 1024;

	protected static final String DATA_LENGHT = "DATALENGTH";

	protected Mp4Info info;

	protected byte[] defaultIconData; // should be static

	// Use for heavy debug
	private final static boolean __debug = false;

	public MP4(File file, String encoding) {
		try {
			if (Controller.hasExtension(file, getExtensions()) == false) {
				info = null;
				return;
				// throw new Exception("Wrong extension for MP4 for file
				// "+file);
			}
			info = new Mp4Info(file);
			info.getLength();
		} catch (Exception e) {
			info = null;
			if (__debug)
				System.err.println(e.toString() + ' ' + file);
		}
	}

	String[] getExtensions() {
		return EXTENSIONS;
	}
	
	public MediaInfo getMediaInfo() {
		return info;
	}

	public boolean isValid() {
		return info != null;
	}

	public int getType() {
		int format = info.getIntAttribute(MediaInfo.FORMAT);
		int result = 0;
		if ((format & MediaInfo.CLASS_AUDIO) > 0) 
			result |= MediaFormat.AUDIO;
		
		if ((format & MediaInfo.CLASS_VIDEO) > 0)
			result |= MediaFormat.VIDEO;
		if ((format & LOSSLESS) > 0)
			result |= LOSSLESS;
		return result;
	}

	public String getDescription() {
		return getFormat(getType());
	}

	public String getFormat(int type) {
		type &= getType();
		if ((type & MediaFormat.VIDEO) > 0)
			return MP4;
		else if ((type & MediaFormat.AUDIO) > 0 || type == 0)
			if ((type & LOSSLESS) > 0)
				return ALAC;
			else
				return TYPE;
		return null;
	}

	public String getThumbnailType() {
		// info.getPicture().getBinaryContent();
		return Resources.EXT_JPEG;
	}

	public Icon getThumbnail(Dimension size) {
		// TODO: get image from id tag
		try {
			return info.getThumbnailIcon(size);
		} catch (Exception e) {
		}
		return null;
	}

	public InputStream getAsStream() throws IOException {
		if (info != null)
			if (!Controller.isJdk1_4())
				return getUrl().openStream(); // a caller can use new
			// BufferedInputStream()
			else
				return new FileInputStream(info.file);
		return null;
	}

	public byte[] getThumbnailData(Dimension size) {
			byte[] res = info.getThumbnailBytes();
			if (res != null)
				return res;
		synchronized (MP4.class) {
			if (defaultIconData == null) {
				defaultIconData = SimpleMediaFormat.getResource(Resources.IMG_MP4ICON);
			}
		}
		return defaultIconData;
	}
	

	public long getFileSize() {
		return getLength();
	}

	public long getLength() {
		if (isValid())
			return info.length();
		return -1;
	}

	public String getName() {
		if (isValid())
			return info.getName();
		return null;
	}

	public File getFile() {
		return info.file;
	}

	public boolean renameTo(File dest) {
		if (info.renameTo(dest))
			try {
				info = new Mp4Info(dest);
				return true;
			} catch (Exception e) {
			}
		return false;
	}

	public java.net.URL getUrl() {
		return info.toURL();
	}

	public String toString() {
		if (isValid())
			return info.toString();
		return super.toString();
	}

	static class AtomHeaderInfo {
		int bodySize;

		int signature;

		FieldInfo[] fields;

		// Map values;

		AtomHeaderInfo(int signature, int size, FieldInfo[] fields) {
			this.signature = signature;
			bodySize = size;
			this.fields = fields;
		}
	}

	static class FieldInfo {
		String name;

		int offset;

		FieldInfo(String name, int offset) {
			this.name = name;
			this.offset = offset;
		}
	}

	class Mp4Info implements MediaInfo {
		File file;

		long length;

		int sampleRate, channels, bitrate;

		Map attributes;

		String encoding = Resources.ENC_UTF_8;

		//protected Icon icon;

		Mp4Info(File file) throws IOException {
			this.file = file;
			attributes = new HashMap();
			readInfo();
		}

		public Object getAttribute(String name) {
			if (ESS_TIMESTAMP.equals(name) || LENGTH.equals(name))
				return MP3.convertTime(getIntAttribute(LENGTH));
			else if (ESS_MAKE.equals(name))
				return attributes.get(MediaInfo.ARTIST);
			/*else if(MediaInfo.PICTURE.equals(name)) {
				byte [] imgBytes = (byte[]) attributes.get(name);
				if (imgBytes != null) {
					ImageIcon ii = new ImageIcon(imgBytes, "Picture");
					if (ii.getIconWidth() > 0)
						return ii;
				}
			} //else*/
				return attributes.get(name);
		}

		public int getIntAttribute(String name) {
			if (LENGTH.equalsIgnoreCase(name))
				return (int) length;
			Object val = attributes.get(name);
			if (val instanceof Integer)
				return ((Integer) val).intValue();
			try {
				return Integer.parseInt(val.toString());
			} catch (Exception e) {
			}
			return 0;
		}

		public float getFloatAttribute(String name) {
			return 0;
		}

		public long getLongAttribute(String name) {
			if (LENGTH.equalsIgnoreCase(name))
				return length;
			return 0;
		}

		public boolean getBoolAttribute(String name) {
			Object val = attributes.get(name);
			if (val != null && COMPILATION.equalsIgnoreCase(name) && val instanceof Boolean)
				return ((Boolean) val).booleanValue();
			return false;
		}

		public double getDoubleAttribute(String name) {
			return 0;
		}

		public Object[] getFiveMajorAttributes() {
			try {
				fiveObjects[0] = getAttribute(TITLE)/* .getTextContent() */;
			} catch (Exception e) {
				fiveObjects[0] = Resources.LABEL_UNKNOWN;
			}
			try {
				fiveObjects[1] = getAttribute(ARTIST);
			} catch (Exception e) {
				fiveObjects[1] = Resources.LABEL_UNKNOWN;
			}
			try {
				fiveObjects[2] = getAttribute(ALBUM);
			} catch (Exception e) {
				fiveObjects[2] = Resources.LABEL_UNKNOWN;
			}
			try {
				fiveObjects[3] = new Integer(getIntAttribute(YEAR));
			} catch (Exception e) {
				fiveObjects[3] = "";
			}
			fiveObjects[4] = getAttribute(LENGTH); // this.getName();
			return fiveObjects;
		}

		public void setAttribute(String name, Object value) {
			throw new UnsupportedOperationException(String.format("%s=%s", name, value));
		}

		public boolean renameTo(File dest) {
			return false;
		}

		public java.net.URL toURL() {
			try {
				return file.toURL();
			} catch (MalformedURLException e) {
				return null;
			}
		}

		public long length() {
			return file.length();
		}

		public String getName() {
			return file.getName();
		}

		public Icon getThumbnailIcon(Dimension size) {
			byte[] picData = getThumbnailBytes();
			if (picData != null) {
				ImageIcon result = new ImageIcon(picData);
				if (size != null) {
					//System.err.printf("Scaling %s%n", size);
					result.setImage(ImageUtil.getScaled(result.getImage(), size, Image.SCALE_FAST, null));
				}
				return result;
			}
			//System.err.printf("Icon is null for %s%n", this);
			return null;
		}
		
		public byte[] getThumbnailBytes() {
			return (byte[]) getAttribute(PICTURE);
		}

		public long getLength() {
			return length;// file.length();
		}

		protected void readInfo() throws IOException {
			// read atom container header
			FileChannel channel = new FileInputStream(file).getChannel();
			try {
				while (channel.position() < channel.size())
					readAtom(channel);
			} finally {
				try {
					channel.close();
				} catch (IOException se) {
				}
			}
		}

		@Override
		public String toString() {
			return ""+getAttribute(TITLE);
		}

		protected AtomHeader readAtom(FileChannel channel) throws IOException {
			AtomHeader atom = new AtomHeader();
			atom.readHeader(channel);
			if (__debug)
				System.err.println("Read " + atom);
			if (atom.signature == MOVIEAID || atom.signature == TRACKAID || atom.signature == RGNCLIPAID
					|| atom.signature == MATTEAID || atom.signature == EDITSAID
					|| atom.signature == KQTVRTRACKREFARRAYATOMTYPE || atom.signature == MEDIAAID
					|| atom.signature == MEDIAINFOAID || atom.signature == DATAINFOAID || atom.signature == USERDATAAID
					|| atom.signature == SAMPLETABLEAID || atom.signature == COMPRESSEDMOVIEAID
					|| atom.signature == REFERENCEMOVIERECORDAID || atom.signature == REFERENCEMOVIEDESCRIPTORAID) {
				readAtomFolder(channel, atom.extSize - 8, atom.signature);
			} else if (atom.signature == SKIPATOMTYPE || atom.signature == FREEATOMTYPE
					|| atom.signature == WIDEATOMTYPE) {
				atom.toNextAtom();
			} else if (atom.signature == MOVIEDATAATOMTYPE) {
				if (length > 0) {
					bitrate = (int) (atom.extSize / length / 1000) * 8;
					attributes.put(MediaInfo.BITRATE, new Integer(bitrate));
				} else { // calculate later
					attributes.put(DATA_LENGHT, new Long(atom.extSize));
				}
				atom.toNextAtom();
			} else if (atom.signature == ILST) {
				readAtomFolder(channel, atom.extSize - 8, atom.signature);
			} else if (atom.signature == STSAMPLEDESCAID) {
				String format, encodeVendor;
				ByteBuffer bb = atom.read(8);
				int num = bb.getInt(4);
				if (__debug)
					System.err.println("Entries " + num);
				int f = attributes.get(MediaInfo.FORMAT) == null ? 0 : (Integer) attributes.get(MediaInfo.FORMAT);
				for (int i = 0; i < num; i++) {
					int size = atom.read(4).getInt();
					// System.err.println("Size of b "+size);
					bb = atom.read(4);
					size -= 8;
					format = Charset.forName("ISO8859_1").decode(bb).toString();
					if (__debug)
						System.err.printf("FORMAT %s%n", format);
					if ("avc1".equals(format) || "mp4v".equals(format))
						f |= MediaInfo.CLASS_VIDEO;
					else if ("mp4a".equals(format))
						f |= MediaInfo.CLASS_AUDIO;
					else if ("alac".equals(format))
						f |= MediaInfo.CLASS_AUDIO + LOSSLESS;
					// System.err.println("Format "+format);
					bb = atom.read(size);// .order(ByteOrder.LITTLE_ENDIAN);
					byte[] encodeVendorBytes = new byte[4];
					bb = (ByteBuffer) ((Buffer)bb).position(12);
					bb.get(encodeVendorBytes, 0, 4);
					if (encodeVendorBytes[0] != 0) {
						encodeVendor = Charset.forName("ISO8859_1").decode(ByteBuffer.wrap(encodeVendorBytes))
								.toString();
						// System.err.println("Encode "encodeVendor);
					} else {
						channels = bb.getShort(16);
						sampleRate = bb.getInt(22);
						attributes.put(MediaInfo.SAMPLERATE, new Integer(sampleRate));
					}
				}

				attributes.put(MediaInfo.FORMAT, f);
				atom.toNextAtom();
			} else if (atom.signature == QUICKTIMEIMAGEFILEMETADATAATOM) {
				atom.read(4);
				readAtomFolder(channel, atom.extSize - 8 - 4, atom.signature);
			} else if (atom.signature == NAME) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.TITLE, data.data);
			} else if (atom.signature == MP4_ARTIST) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.ARTIST, data.data);
			} else if (atom.signature == MP4_ALBUM) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.ALBUM, data.data);
			} else if (atom.signature == COMMENTARY) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.COMMENTS, data.data);
			} else if (atom.signature == MP4_DATE) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				try {
					attributes.put(MediaInfo.YEAR, new Integer(data.data));
				} catch (Exception e) {
					///
				}
			} else if (atom.signature == GROUPING) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.CONTENTGROUP, data.data);
			} else if (atom.signature == TOOL) {
				atom.toNextAtom();
			} else if (atom.signature == MP4_GENRE) {
				AtomHeader data = new AtomHeader();
				data.readHeader(channel);
				data.move(8);
				int gi = data.read(2).getShort();
				if (gi > 0)
					attributes.put(MediaInfo.GENRE, new Integer(gi - 1));
				else
					attributes.put(MediaInfo.GENRE, new Integer(12));
				data.toNextAtom();
			} else if (atom.signature == CUSTOM_GENRE) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.GENRE, data.data);
			} else if (atom.signature == MP4_TRACK) {
				AtomHeader data = new AtomHeader();
				data.readHeader(channel);
				data.move(10);
				int t = data.read(2).getShort();
				int nt = data.read(2).getShort();
				attributes.put(MediaInfo.TRACK, new Integer(t));
				attributes.put(MediaInfo.OFTRACKS, new Integer(nt));
				data.toNextAtom();
			} else if (atom.signature == DISK_NUMBER) {
				AtomHeader data = new AtomHeader();
				data.readHeader(channel);
				data.move(10);
				int d = data.read(2).getShort();
				int nd = data.read(2).getShort();
				attributes.put(MediaInfo.PARTOFSET, String.valueOf(d) + '/' + nd);
				data.toNextAtom();
			} else if (atom.signature == WRITER) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.COMPOSER, data.data);
			} else if (atom.signature == COVERART) {
				// COVERART
				DataHeaderBin data = new DataHeaderBin();
				data.read(channel);
				attributes.put(MediaInfo.PICTURE, data.data);
			} else if (atom.signature == MP4_COMPILATION) {
				AtomHeader data = new AtomHeader();
				data.readHeader(channel);
				data.move(8);
				int c = data.read(1).get();
				// System.err.println("comp "+c);
				attributes.put(MediaInfo.COMPILATION, new Boolean(c == 1));
				data.toNextAtom();
			} else if (atom.signature == MP4_BPM) {
				AtomHeader data = new AtomHeader();
				data.readHeader(channel);
				data.move(8);
				int bpm = data.read(2).getShort();
				attributes.put(MediaInfo.BPM, new Integer(bpm));
				data.toNextAtom();
			} else if (atom.signature == FREE_FORM) {
				// mean, name, data
				atom.toNextAtom();
			} else if (atom.signature == MEDIAHEADERAID) {
				ByteBuffer bb = atom.read(20);
				length = bb.getInt(16) / bb.getInt(12);
				attributes.put(MediaInfo.LENGTH, new Long(length));
				// System.err.println("Play time (duration "+bb.getInt(16)
				// +")/(time_scale "+bb.getInt(12)
				// +")="+(bb.getInt(16)/bb.getInt(12)));
				atom.toNextAtom();
			} else if (atom.signature == MP4_ALBUM_ARTIST) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.ALBUMARTIST, data.data);
			} else if (atom.signature == MP4_LYRICS) {
				DataHeader data = new DataHeader(encoding);
				data.read(channel);
				attributes.put(MediaInfo.LYRICS, data.data);
			} else if (atom.signature == MOVIEHEADERAID) {
				atom.toNextAtom();
			} else if (atom.signature == CUSTOM_REQ) {
				atom.toNextAtom();
			} else {
				atom.toNextAtom();
			}
			return atom;
		}

		protected void readAtomFolder(FileChannel channel, long size, int signature) throws IOException {
			AtomHeader atom = null;
			if (__debug)
				System.err.printf("Processing list %s size %d------------->>>%n", BasicIo.asString(signature), size);
			while (size > 0) {
				atom = readAtom(channel);
				size -= atom.extSize;
				if (__debug)
					System.err.printf("<<<>>>Size %d of %s afte %s %n", size, BasicIo.asString(signature), atom);
			}
			if (__debug)
				System.err.printf("<<<------------ exit list %s size %d%n", BasicIo.asString(signature), size);
		}
		
		@Override
		public <C extends MediaComponent> C[] getComponents() {
			return null;
		}

		protected Object[] fiveObjects = new Object[5];
	}

	class AtomHeader {
		int size;

		long extSize;

		long position;

		long start;

		int signature;

		ByteBuffer buffer;

		FileChannel channel;

		void reset() {
			size = 0;
			extSize = 0;
			position = 0;
			signature = -1;
			buffer = null;
			start = -1;
			channel = null;
		}

		void readHeader(FileChannel channel) throws IOException {
			start = channel.position();
			this.channel = channel;
			buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.BIG_ENDIAN);
			channel.read(buffer);
			position += 8;
			((Buffer)buffer).rewind();
			size = buffer.getInt();
			signature = buffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
			if (size == 1) {
				buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.BIG_ENDIAN);
				channel.read(buffer);
				position += 8;
				buffer.rewind();
				extSize = buffer.getLong();
				if (__debug)
					System.err.printf("Extend size to %d after 1%n", extSize);
			} else
				extSize = size;
		}

		void move(int offset) throws IOException {
			if (offset > extSize - position || (offset < 0 && offset < -position))
				throw new IOException("Can't move beyond of the header " + offset + " - " + (extSize - position));
			channel.position(channel.position() + offset);
			position += offset;
		}

		ByteBuffer read(int len) throws IOException {
			if (len > extSize - position || len < 0)
				throw new IOException("Can't move beyond of the header by " + len + " - " + (extSize - position));
			buffer = ByteBuffer.allocateDirect(len).order(ByteOrder.BIG_ENDIAN);
			channel.read(buffer);
			((Buffer)buffer).rewind();
			position += len;
			return buffer;
		}

		AtomHeader toNextChild() throws IOException {
			AtomHeader result = new AtomHeader();
			result.readHeader(channel);
			position += result.extSize;
			return result;
		}

		void toNextAtom() throws IOException {
			if (extSize - position > 0)
				channel.position(channel.position() + extSize - position);
			else {
				if (extSize - position < 0)
					throw new IOException("Beyond atom " + BasicIo.asString(signature) + " limit " + position
							+ " size " + extSize);
			}
		}

		// void close() {
		// position = extSize;
		// }
		@Override
		public String toString() {
			return "Atom " + BasicIo.asString(signature) + " size: " + size+", ext: "+extSize;
		}
	}

	class DataHeader extends AtomHeader {
		String encoding;

		String data;

		DataHeader(String encoding) {
			this.encoding = encoding;
		}

		void read(FileChannel channel) throws IOException {
			start = channel.position();
			this.channel = channel;
			buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.BIG_ENDIAN);
			channel.read(buffer);
			position += 4;
			((Buffer)buffer).rewind();
			size = buffer.getShort();
			if (size != 0) {
				extSize = size + 4;
				if (__debug)
					System.err.printf("Non standard data size %d%n", size);
				data = Charset.forName(encoding == null ? "ISO8859_1" : encoding).decode(read(size)).toString();
				if (__debug)
					System.err.printf("NData: %s%n", data);
			} else {
				((Buffer)buffer).rewind();
				size = buffer.getInt();
				((Buffer)buffer).rewind();
				channel.read(buffer);
				position += 4;
				((Buffer)buffer).rewind();
				signature = buffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
				extSize = size;
				if (__debug)
					System.err.printf("Data size %d  %s%n", size, BasicIo.asString(signature));
				if (signature == DATA) {
					int num = read(4).getInt();
					move(4);
					data = Charset.forName(encoding == null ? "ISO8859_1" : encoding).decode(read(size - 8 - 8))
							.toString();
					if (__debug)
						System.err.printf("Data: %s%n", data);
				} else {
					System.err.printf("Date header signature 'data' was expected but found %s (size %d)%n", BasicIo
							.asString(signature), size);
					read(size - 8);
				}
			}
			toNextAtom();
		}

		@Override
		public String toString() {
			return data == null ? "null" : data;
		}
	}

	class DataHeaderBin extends AtomHeader {
		byte[] data;

		void read(FileChannel channel) throws IOException {
			readHeader(channel);
			if (signature == DATA) {
				int num = read(4).getInt();
				move(4);
				data = new byte[size - 8 - 8];
				read(size - 8 - 8).get(data);
			} else
				System.err.println("Header data expected where " + BasicIo.asString(signature) + " met.");
			toNextAtom();
		}

		public String toString() {
			return data == null ? "null" : "Image: ";
		}
	}
}