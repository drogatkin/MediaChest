/* MediaChest - MP4
 * Copyright (C) 2001-2004 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: WMA.java,v 1.20 2015/02/22 09:37:25 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.swing.Icon;

import mediautil.gen.BasicIo;
import mediautil.gen.MediaFormat;

import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.media.MediaPlayer;

public class WMA extends SimpleMediaFormat<WMA.WMAInfo> {
	public static final String WMA = "WMA";

	public static final String WAV = "WAV";

	public static final String TYPE = WAV;

	public static final String EXTENSIONS[] = { WAV, WMA };

	// public static final Integer DATA_START_OFF = new Integer(1);

	protected byte[] defaultIconData;

	protected static Icon icon;

	public WMA(File file, String enc) {
		super(file, enc);
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}
	
	@Override
	WMAInfo createMediaInfo(File file) {
		try {
			return new WMAInfo(file, encoding);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getType() {
		return MediaFormat.AUDIO;
	}

	public String getDescription() {
		return TYPE;
	}

	@Override
	public MediaPlayer getPlayer() {
		WavPlayer result = new WavPlayer();
		result.init(this);
		return result;
	}

	public String getFormat(int type) {
		if ((type & MediaFormat.AUDIO) > 0 || type == 0)
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
		return icon;
	}

	public InputStream getAsStream() throws IOException {
		if (info != null)
			return MediaFormatFactory.getInputStreamFactory().  getInputStream(info.file);
		return null;
	}

	public byte[] getThumbnailData(Dimension size) {
		synchronized (WMA.class) {
			if (defaultIconData == null) {
				defaultIconData = SimpleMediaFormat.getResource(Resources.IMG_WMAICON);
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
				info = new WMAInfo(dest, encoding);
				return true;
			} catch (Exception e) {
			}
		return false;
	}

	public java.net.URL getUrl() {
		return info.toURL();
	}

	public void saveData(File file) throws IOException {
		info.saveData(new FileOutputStream(file));
	}

	public String toString() {
		if (isValid())
			return info.toString();
		return super.toString();
	}

	static final String[] FIVE_MAJOR_ATTR = {};

	class WMAInfo extends SimpleMediaInfo {

		long dataStart, dataLen;

		WMAInfo(File file, String e) throws IOException {
			super(file, e);
			readInfo();
		}

		protected void readInfo() throws IOException {
			attrsMap = new HashMap<>();
			// read atom container header
			FileChannel channel = MediaFormatFactory.getInputStreamFactory().getInputChannel(file);
			int point = 12;
			ByteBuffer buffer = readChunkHeader(channel, 12);
			if (buffer.getInt() != BasicIo.asInt("RIFF"))
				throw new IOException("No RIFF signature in " + getName());
			int glen = buffer.getInt();
			if (buffer.getInt() != BasicIo.asInt("WAVE"))
				throw new IOException("No WAVE signature in " + getName());
			while (point < glen) {
				buffer = readChunkHeader(channel, 8);
				point += 8;
				int signature = buffer.getInt();// order(ByteOrder.LITTLE_ENDIAN).;
				int len = buffer.getInt();
				if (signature == BasicIo.asInt("fmt ")) {
					buffer = readChunkHeader(channel, len);
					short s = buffer.getShort(2);
					if (s == 1)
						attrsMap.put(MODE, "mono");
					else if (s == 2)
						attrsMap.put(MODE, "stereo");
					else
						attrsMap.put(MODE, "Channels-" + s);
					attrsMap.put(SAMPLERATE, new Integer(buffer.getInt(4)));
					attrsMap.put("byterate", new Integer(buffer.getInt(8)));
					attrsMap.put(BITRATE, new Integer(buffer.getShort(14)));
				} else if (signature == BasicIo.asInt("rgad")) {
					channel.position(channel.position() + len);
				} else if (signature == BasicIo.asInt("data")) {
					attrsMap.put(LENGTH, new Long(len / getIntAttribute("byterate")));
					// check if MP3 inside
					// new ID3v2(
					dataStart = channel.position();
					dataLen = len;
					channel.position(channel.position() + len);
				} else if (signature == BasicIo.asInt("cue ")) {
					channel.position(channel.position() + len);
				} else if (signature == BasicIo.asInt("fact")) {
					channel.position(channel.position() + len);
				} else {
					if ((len & 1) == 1) len++;
					channel.position(channel.position() + len);
					// throw new IOException("Not supported signature
					// "+BasicIo.asString(signature)+" in "+getName());
					System.err.println("Not supported signature " + BasicIo.asString(signature) + " in " + getName()
							+ " ignored.");
				}
				point += len;
			}
		}

		void saveData(OutputStream out) throws IOException {
			InputStream in = new FileInputStream(file);
			// TODO: how many actually skipped
			in.skip(dataStart);
			Stream.copyStream(in, out, dataLen);
			in.close();
			out.close();
		}

		protected ByteBuffer readChunkHeader(FileChannel channel, int size) throws IOException {
			ByteBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
			channel.read(buffer);
			buffer.rewind();
			return buffer;
		}

		public int getIntAttribute(String name) {
			Object o = attrsMap.get(name);
			if (o == null)
				return 0;
			if (o instanceof Integer)
				return ((Integer) o).intValue();
			else if (o instanceof Long)
				return ((Long) o).intValue();
			return 0; // should be Exception
		}

		public float getFloatAttribute(String name) {
			return ((Float) attrsMap.get(name)).floatValue();
		}

		public long getLongAttribute(String name) {
			return ((Long) attrsMap.get(name)).longValue();
		}

		public boolean getBoolAttribute(String name) {
			try {
				return ((Boolean) attrsMap.get(name)).booleanValue();
			} catch (Exception e) {
			}
			throw new IllegalArgumentException("No attribute for " + name);
		}

		public double getDoubleAttribute(String name) {
			return ((Double) attrsMap.get(name)).doubleValue();
		}

		public Object[] getFiveMajorAttributes() {
			Object[] result = new Object[5];
			for (int i = 0; i < FIVE_MAJOR_ATTR.length; i++)
				result[i] = getAttribute(FIVE_MAJOR_ATTR[i]);
			return result;
		}

		public void setAttribute(String name, Object value) {
			attrsMap.put(name, value);
		}

		public boolean renameTo(File dest) {
			boolean result = file.renameTo(dest);
			if (result)
				file = dest;
			return result;
		}

		public java.net.URL toURL() {
			try {
				return file.toURL();
			} catch (java.net.MalformedURLException mfu) {
			}
			return null;
		}

		public long length() {
			return file.length();
		}

		public String getName() {
			return file.getName();
		}

		public String toString() {
			return getName();
		}

		public Icon getThumbnailIcon(Dimension size) {
			synchronized (WMA) {
				if (icon == null) // TODO: use WAV icon
					icon = Controller.getResourceIcon(Resources.IMG_MP3ICON);
			}
			return icon;
		}

		public long getLength() {
			return getLongAttribute(LENGTH);
		}

	}

	class WavPlayer extends SimpleMediaFormat.SimpleMediaPlayer<WMA> {

		@Override
		void playLoop() {
			try {
				fmt = new AudioFormat((int) WMA.this.info.getAttribute(WMAInfo.SAMPLERATE),
						(int) WMA.this.info.getAttribute(WMAInfo.BITRATE),
						"mono".equals(WMA.this.info.getAttribute(WMAInfo.MODE)) ? 1 : 2, true, false);
				line = new SimpleDownSampler(fmt).getLine();
				int block = fmt.getFrameSize()  * 2000;
				byte[] playBuf = new byte[block];
				fmt = line.getFormat();
				if (!line.isOpen())
					line.open(fmt);
				inputStream = new FileInputStream(WMA.this.info.file);
				// TODO: how many actually skipped
				inputStream.skip(WMA.this.info.dataStart);
				line.start();
				for (;;) {
					int len = inputStream.read(playBuf);
					//System.out.printf("0x%s%n", Utils.toHexString(0, 24,playBuf));
					if (len > 0)
						line.write(playBuf, 0, len);
					if (len < playBuf.length)
						break;
					if (checkPause() == false)
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		System.out.println("Data extractor from RIFF");
		if (args.length == 0) {
			System.out.println("Usage: WMA input_RIFF_file [output_file]");
			System.exit(255);
		}
		try {
			String outName;
			if (args.length == 1)
				outName = (args[0].length() > 4 ? args[0].substring(0, args[0].length() - 4) : args[0]) + ".dat";
			else
				outName = args[1];
			File inf = new File(args[0]);
			if (inf.isDirectory()) {
				File[] files = inf.listFiles(new FileFilter() {
					public boolean accept(File path) {
						String pathname = path.getAbsolutePath();
						int l = pathname.length();
						return l > 4 && pathname.regionMatches(true, l - 4, ".wav", 0, 4);
					}
				});
				for (int i = 0; i < files.length; i++) {
					outName = files[i].getPath();
					System.out.println("Extracting " + outName + "...");
					new WMA(files[i], null).saveData(new File(outName.substring(0, outName.length() - 4) + ".dat"));
				}
			} else
				new WMA(inf, null).saveData(new File(outName));

		} catch (IOException ioe) {
			System.err.println("Problem extractin data from " + args[0] + "  " + ioe);
		}
	}
}