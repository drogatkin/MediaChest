/* MediaChest - MP3 
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
 *  $Id: MP3.java,v 1.68 2013/05/17 07:57:17 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Date;

import javax.sound.sampled.AudioFormat;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.JavaSoundAudioDevice;
import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.ImageUtil;
import mediautil.gen.MediaComponent;

import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.media.MediaPlayer;
import de.vdheide.mp3.Bytes;
import de.vdheide.mp3.FrameDamagedException;
import de.vdheide.mp3.ID3v2DecompressionException;
import de.vdheide.mp3.ID3v2IllegalVersionException;
import de.vdheide.mp3.ID3v2WrongCRCException;
import de.vdheide.mp3.MP3File;
import de.vdheide.mp3.NoMP3FrameException;
import de.vdheide.mp3.TagContent;

public class MP3 implements MediaFormat {
	public static final String LAYER_NAMES[] = { "I", "II", "III" };

	public static final String S_FREQ[] = { "44.1", "48", "32", "0" };

	public static final String MODE_NAMES[] = { "stereo", "j-stereo", "dual-ch", "mono" };

	public final static String GENRES[] = { "Blues", // 0
			"Classic Rock", // 1
			"Country", // 2
			"Dance", // 3
			"Disco", // 4
			"Funk", // 5
			"Grunge", // 6
			"Hip-Hop", // 7
			"Jazz", // 8
			"Metal", // 9
			"New Age", // 10
			"Oldies", // 11
			"Other", // 12
			"Pop", // 13
			"R&B", // 14
			"Rap", // 15
			"Reggae", // 16
			"Rock", // 17
			"Techno", // 18
			"Industrial", // 19
			"Alternative", // 20
			"Ska", // 21
			"Death Meta", // 22
			"Pranks", // 23
			"Soundtrack", // 24
			"Euro-Techno", // 25
			"Ambient", // 26
			"Trip-Hop", // 27
			"Vocal", // 28
			"Jazz+Funk", // 29
			"Fusion", // 30
			"Trance", // 31
			"Classical", // 32
			"Instrumental", // 33
			"Acid", // 34
			"House", // 35
			"Game", // 36
			"Sound Clip", // 37
			"Gospel", // 38
			"Noise", // 39
			"AlternRock", // 40
			"Bass", // 41
			"Soul", // 42
			"Punk", // 43
			"Space", // 44
			"Meditative", // 45
			"Instrumental Pop", // 46
			"Instrumental Rock", // 47
			"Ethnic", // 48
			"Gothic", // 49
			"Darkwave", // 50
			"Techno-Industrial", // 51
			"Electronic", // 52
			"Pop-Folk", // 53
			"Eurodance", // 54
			"Dream", // 55
			"Southern Rock", // 56
			"Comedy", // 57
			"Cult", // 58
			"Gangsta", // 59
			"Top 40", // 60
			"Christian Rap", // 61
			"Pop/Funk", // 62
			"Jungle", // 63
			"Native American", // 64
			"Cabaret", // 65
			"New Wave", // 66
			"Psychadelic", // 67
			"Rave", // 68
			"Showtunes", // 69
			"Trailer", // 70
			"Lo-Fi", // 71
			"Tribal", // 72
			"Acid Punk", // 73
			"Acid Jazz", // 74
			"Polka", // 75
			"Retro", // 76
			"Musical", // 77
			"Rock & Roll", // 78
			"Hard Rock", // 79
			// WinAmp expanded this table with next codes:
			"Folk", // 80
			"Folk-Rock", // 81
			"National Folk", // 82
			"Swing", // 83
			"Fast Fusion", // 84
			"Bebob", // 85
			"Latin", // 86
			"Revival", // 87
			"Celtic", // 88
			"Bluegrass", // 89
			"Avantgarde", // 90
			"Gothic Rock", // 91
			"Progressive Rock", // 92
			"Psychedelic Rock", // 93
			"Symphonic Rock", // 94
			"Slow Rock", // 95
			"Big Band", // 96
			"Chorus", // 97
			"Easy Listening", // 98
			"Acoustic", // 99
			"Humour", // 100
			"Speech", // 101
			"Chanson", // 102
			"Opera", // 103
			"Chamber Music", // 104
			"Sonata", // 105
			"Symphony", // 106
			"Booty Brass", // 107
			"Primus", // 108
			"Porn Groove", // 109
			"Satire", // 110
			"Slow Jam", // 111
			"Club", // 112
			"Tango", // 113
			"Samba", // 114
			"Folklore", // 115
			"Ballad", // 116
			"Poweer Ballad", // 117
			"Rhytmic Soul", // 118
			"Freestyle", // 119
			"Duet", // 120
			"Punk Rock", // 121
			"Drum Solo", // 122
			"A Capela", // 123
			"Euro-House", // 124
			"Dance Hall", // 125
			"Goa", // 126
			"Drum & Bass", // 127
			"Club House", // 128
			"Hardcore", // 129
			"Terror", // 130
			"Indie", // 131
			"BritPop", // 132
			"NegerPunk", // 133
			"Polsk Punk", // 134
			"Beat", // 135
			"Christian Gangsta", // 136
			"Heavy Metal", // 137
			"Black Metal", // 138
			"Crossover", // 139
			"Contemporary C", // 140
			"Christian Rock", // 141
			"Merengue", // 142
			"Salsa", // 143
			"Thrash Metal", // 144
			"Anime", // 145
			"JPop", // 146
			"SynthPop" // 147
	};

	public static final String[] PICTURE_TYPE = { // Descriptor[]
	// new Descriptor(Resources.LIST_PICT_OTHER, IMAGE_TYPE_OTHER),
			"Other", "32x32 pixels 'file icon' (PNG only)", // $01
			"Other file icon", // $02
			"Cover (front)", // $03
			"Cover (back)", // $04
			"Leaflet page", // $05
			"Media (e.g. lable side of CD)", // $06
			"Lead artist/lead performer/soloist", // $07
			"Artist/performer", // $08
			"Conductor", // $09
			"Band/Orchestra", // $0A
			"Composer", // $0B
			"Lyricist/text writer", // $0C
			"Recording Location", // $0D
			"During recording", // $0E
			"During performance", // $0F
			"Movie/video screen capture", // $10
			"A bright coloured fish", // $11
			"Illustration", // $12
			"Band/artist logotype", // $13
			"Publisher/Studio logotype" // $14
	};

	public static final byte IMAGE_TYPE_OTHER = 0;

	public static final byte IMAGE_TYPE_32x32PNG_ICON = 1;

	public static final int GENRE_OTHER = 12;

	public static final String MP3 = "MP3";

	public static final String TYPE = MP3;

	public static final String[] EXTENSIONS = { TYPE, "MP2" };

	protected static final Class[] EMPTY_PARAMS = {};

	protected static Icon icon = Controller.getResourceIcon(Resources.IMG_MP3ICON);

	protected static byte[] defaultIconData;

	Mp3Info info;

	public MP3(File file, String encoding) {
		try {
			if (Controller.hasExtension(file, EXTENSIONS) == false)
				throw new Exception("Wrong extension for MP3 for file " + file);
			info = new Mp3Info(file, encoding);
			info.getLength();
		} catch (Exception e) {
			info = null;
			//System.err.println(e.toString()+' '+file+" "+e);
			//e.printStackTrace();
		}
	}

	public MediaInfo getMediaInfo() {
		return info;
	}

	public boolean isValid() {
		return info != null;
	}

	public int getType() {
		return MediaFormat.AUDIO;
	}

	public String getDescription() {
		return MP3;
	}

	public String getFormat(int type) {
		if ((type & MediaFormat.AUDIO) > 0 || type == 0)
			return MP3;
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
			if (!Controller.isJdk1_4())
				return getUrl().openStream(); // a caller can use new BufferedInputStream()
			else
				return new FileInputStream(info);
		return null;
	}

	public byte[] getThumbnailData(Dimension size) {
		try {
			byte[] res = info.getPicture().getBinaryContent();
			if (res != null)
				return res;
		} catch (de.vdheide.mp3.FrameDamagedException fe) {
		} catch (Exception e) {
		}
		synchronized (MP3) {
			if (defaultIconData == null) {
				InputStream st = null;
				try { // TODO use Java 7 resource freeing
					BufferedInputStream bis = new BufferedInputStream(st = Controller.class.getClassLoader()
							.getResourceAsStream("resource/image/" + Resources.IMG_MP3ICON));
					if (st == null)
						System.err.printf("Can't get stream for %s%n", "resource/image/" + Resources.IMG_MP3ICON);
					ByteArrayOutputStream bos = new ByteArrayOutputStream(8 * 1024);
					Stream.copyStream(bis, bos);
					defaultIconData = bos.toByteArray();
					bos.close();
					bis.close();
				} catch (Exception e) { // io or null ptr
					e.printStackTrace();
					defaultIconData = new byte[0];
				}
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
		return info;
	}

	public boolean renameTo(File dest) {
		if (info.renameTo(dest))
			try {
				info = new Mp3Info(dest, info.getTagsEncoding());
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		return false;
	}

	public java.net.URL getUrl() {
		try {
			return info.toURL();
		} catch (java.net.MalformedURLException me) {
			return null;
		}
	}

	public String toString() {
		if (isValid())
			return info.toString();
		return super.toString();
	}

	/**
	 * conver time in seconds to formatted string
	 */
	public static String convertTime(long time) {
		time = Math.abs(time);
		return (time / (24 * 60 * 60) > 0 ? String.valueOf(time / 60 / 60 / 24) + "d:" : "")
				+ (time % (24 * 60 * 60) / 60 / 60 < 10 ? "0" : "") + String.valueOf(time % (24 * 60 * 60) / 60 / 60)
				+ ':' + (time % (60 * 60) / 60 < 10 ? "0" : "") + (time % (60 * 60) / 60) + ':'
				+ (time % 60 < 10 ? "0" : "") + (time % 60);
	}

	/**
	 * parses a string in format (dd)d:hh:mm:ss.mss, <br>
	 * to ms value
	 */
	public static long parseTime(String time) throws ParseException {
		if (time == null || time.length() == 0)
			return 0;
		char[] pl = time.toCharArray();
		boolean nextCol = false, in_ms = false;
		int val = 0;
		int d = 0, h = 0, m = 0, s = 0, p = 0;
		for (int c = 0; c < pl.length; c++) {
			if (Character.isDigit(pl[c])) {
				if (nextCol)
					throw new ParseException("':' expected.", c);
				val = val * 10 + pl[c] - '0';
			} else if (pl[c] == ':') {
				if (nextCol)
					d = val;
				else {
					if (m != 0) {
						if (h == 0) {
							h = m;
						} else {
							d = h;
							h = m;
						}
					}
					m = val;
				}
				nextCol = false;
				val = 0;
			} else if (pl[c] == '.') {
				if (nextCol)
					throw new ParseException("':' expected.", c);
				in_ms = true;
				s = val;
				val = 0;
			} else if (pl[c] == 'd' || pl[c] == 'D') {
				nextCol = true;
			} else
				throw new ParseException("Not allowed symbol '" + pl[c] + "\'.", c);
		}
		if (in_ms)
			p = val;
		else
			s = val;
		// System.err.println("parse:"+time+" "+d+" "+h+" "+m+" "+s+" . "+p);
		return p + s * 1000l + m * 60 * 1000l + h * 60 * 60 * 1000l + d * 60 * 60 * 24 * 1000l;
	}

	/**
	 * formats time in ms in the string (dd)d:hh:mm:ss.mss, <br>
	 * for example 10d:12:34:56.103
	 */
	public static String formatTime(long time) {
		if (time <= 0)
			return "0:00";
		StringBuffer result = new StringBuffer();
		int val = 0;
		if (time % 1000 > 0)
			result.append('.').append(String.valueOf(time % 1000));
		time /= 1000;

		if (time > 0) {
			val = (int) time % 60;
			result.insert(0, val);
			if (val < 10)
				result.insert(0, '0');
			time /= 60;
			if (time > 0) {
				result.insert(0, ':');
				val = (int) time % 60;
				result.insert(0, val);
				if (val < 10)
					result.insert(0, '0');
				time /= 60;
				if (time > 0) { // hours
					result.insert(0, ':');
					val = (int) time % 24;
					result.insert(0, val);
					if (val < 10)
						result.insert(0, '0');
					time /= 24;
					if (time > 0) {
						result.insert(0, "d:");
						result.insert(0, val);
					}
				}
			} else
				result.insert(0, "0:");
		}
		// System.err.println("Format:"+time+" to "+result);

		return result.toString();
	}

	public static String findGenre(MediaInfo info) {
		Object genre = info.getAttribute(info.GENRE);
		if (genre != null) {
			if (genre instanceof String) {
				String sGenre = (String) genre;
				int op = sGenre.indexOf('(');
				if (op >= 0) {
					int cp = sGenre.indexOf(')', op);
					if (cp > op) {
						try {
							int iGenre = Integer.parseInt(sGenre.substring(op + 1, cp));
							if (iGenre < 0)
								iGenre = -iGenre;
							return sGenre.substring(0, op) + GENRES[iGenre] + sGenre.substring(cp + 1);
						} catch (Exception e3) {
							System.err.printf("Can't retrieve genre from '(number)' %s %s %n", sGenre, e3);
						}
					}
				}
				return sGenre;
			} else if (genre instanceof Integer) {
				return GENRES[((Integer) genre).intValue()];
			} else
				throw new IllegalArgumentException("Genre value is type " + genre.getClass().getName());
		}
		try {
			return GENRES[info.getIntAttribute(MediaInfo.GENRE)];
		} catch (Exception e2) {
			System.err.println("Can't retrieve genre from number " + e2);
		}
		return null;
	}

	protected class Mp3Info extends MP3File implements MediaInfo {
		Mp3Info(File file, String encoding) throws IOException, NoMP3FrameException, ID3v2WrongCRCException,
				ID3v2DecompressionException, ID3v2IllegalVersionException {
			// TODO: more clear about encoding
			super(file, "", encoding == null ? Controller.getEncoding() : encoding);
		}

		public void setAttribute(String name, Object value) {
			try {
				getClass().getMethod("set" + name, new Class[] { value.getClass() }).invoke(this,
						new Object[] { value });
			} catch (Throwable t) {
				if (t instanceof InvocationTargetException) {
					riseException(name, value, t, "");
				}
				try {
					TagContent content = new TagContent();
					Method m = getClass().getMethod("set" + name, new Class[] { TagContent.class });
					if (value instanceof Boolean)
						value = ((Boolean) value).booleanValue() ? new Integer(1) : new Integer(0);
					if (value instanceof Number) {
						// try to set binary
						content.setContent(Bytes.longToByteArray(((Number) value).longValue(), -1));
						try {
							m.invoke(this, new Object[] { content });
							return;
						} catch (InvocationTargetException ite) {
							System.err.println("Non String value " + value + " for " + name + " converted to String.");
							value = value.toString();
						}
					}
					content.setContent((String) value);
					m.invoke(this, new Object[] { content });
				} catch (Throwable t2) {
					// t2.printStackTrace();
					riseException(name, value, t2, "");
				}
			}
		}

		private void riseException(String name, Object value, Throwable t, String pref) {
			if (pref == null)
				pref = "";
			else
				pref += ": ";
			if (t instanceof InvocationTargetException && ((InvocationTargetException) t).getTargetException() != null) {
				t = ((InvocationTargetException) t).getTargetException();
				// t.printStackTrace();
			}
			throw new IllegalArgumentException(pref + "Not supported set attribute name " + name + " to value " + value
					+ " <<" + t);
		}

		public Object getAttribute(String name) {
			try {
				if (ESS_CHARACHTER.equals(name))
					return new Integer(getBitrate());
				else if (ESS_TIMESTAMP.equals(name) || LENGTH.equals(name)) {
					return convertTime(getLength());
				} else if (ESS_QUALITY.equals(name) || MODE.equals(name))
					return MODE_NAMES[getMode()];
				else if (ESS_MAKE.equals(name))
					return getArtist().getTextContent();
				else {
					Object result = getGenericAttribute(name);
					if (result instanceof TagContent)
						if (PICTURE.equals(name)) {
							// System.err.println("Picture type: "+((TagContent)result).getType()+
							// " Sub type: "+Controller.bytesToHex(((TagContent)result).getBinarySubtype())+
							// " Description: "+((TagContent)result).getDescription());
							if (((TagContent) result).getBinaryContent() != null) {
								ImageIcon ii = new ImageIcon(((TagContent) result).getBinaryContent(),
										((TagContent) result).getDescription());
								if (ii.getIconWidth() > 0)
									return ii;
								System.err.printf("Corrupted image for %s%n", this);
							}
							return null;
						} else
							return ((TagContent) result).getTextContent();
					return result;
				}
			} catch (FrameDamagedException fde) {
				fde.printStackTrace();
			}
			return null;
		}

		public Icon getThumbnailIcon() {
			return getThumbnailIcon(null);
		}

		public Icon getThumbnailIcon(Dimension size) {
			try {
				ImageIcon result = new ImageIcon(getPicture().getBinaryContent());
				if (size != null) {
					result.setImage(ImageUtil.getScaled(result.getImage(), size, Image.SCALE_FAST, null));
				}
				return result;

			} catch (FrameDamagedException fde) {
			}
			return null;
		}

		public Image getImage() {
			try {
				return Toolkit.getDefaultToolkit().createImage(getPicture().getBinaryContent());
			} catch (Exception e) {
				System.err.println("Can't create image " + e);
			}
			return null;
		}

		public int getIntAttribute(String name) {
			if (ESS_CHARACHTER.equals(name)) {
				try {
					Date date = getYear().getDateContent();
					if (date != null)
						return date.getYear();
					return Integer.parseInt(getYear().getTextContent());
				} catch (Exception e) {
					return 0;
				}
			} else if (LENGTH.equals(name))
				return (int) getLength();
			Object result = getGenericAttribute(name);
			if (result != null) {
				if (result instanceof TagContent) {
					String ns = ((TagContent) result).getTextContent();
					if (ns != null) {
						if (TRACK.equals(name)) {
							int sp = ns.indexOf('/');
							if (sp > 0)
								ns = ns.substring(0, sp);
						} else if (OFTRACKS.equals(name)) {
							int sp = ns.indexOf('/');
							if (sp > 0)
								ns = ns.substring(sp + 1);
						}
						try {
							return Integer.parseInt(ns);
						} catch (NumberFormatException nfe) {
						}
					} else {
						byte[] bv = ((TagContent) result).getBinaryContent();
						if (bv != null)
							return new Integer((int) Bytes.byteArrayToLong(bv, 0, bv.length));
					}
				} else if (result instanceof Integer)
					return ((Integer) result).intValue();
				else if (result instanceof String) {
					try {
						return Integer.parseInt((String) result);
					} catch (NumberFormatException nfe) {
					}
				}
			} else
				throw new NullPointerException("Int attribute not set");

			throw new IllegalArgumentException("Not supported attribute '" + name + "' : '"+result+"' for int.");
		}

		public float getFloatAttribute(String name) {
			if (ESS_CHARACHTER.equals(name))
				return ((float) getSamplerate()) / 1000f;
			else {
				Object result = getGenericAttribute(name);
				if (result != null) {
					if (result instanceof TagContent) {
						try {
							return new Float(((TagContent) result).getTextContent()).floatValue();
						} catch (Exception e) {
						}
					} else if (result instanceof Float)
						return ((Float) result).floatValue();
				} else
					return 0;
			}
			throw new IllegalArgumentException("Not supported attribute name for float " + name);
		}

		public long getLongAttribute(String name) {
			Object result = getGenericAttribute(name);
			if (result != null) {
				if (result instanceof TagContent) {
					String ns = ((TagContent) result).getTextContent();
					if (ns != null) {
						if (TRACK.equals(name)) {
							int sp = ns.indexOf('/');
							if (sp > 0)
								ns = ns.substring(0, sp);
						} else if (OFTRACKS.equals(name)) {
							int sp = ns.indexOf('/');
							if (sp > 0)
								ns = ns.substring(sp + 1);
						}
						try {
							return Long.parseLong(ns);
						} catch (NumberFormatException nfe) {
						}
					}
				}
				if (result instanceof Long)
					return ((Long) result).longValue();
				else if (result instanceof String) {
					try {
						return Long.parseLong((String) result);
					} catch (NumberFormatException nfe) {
					}
				}
			} else
				return 0;
			throw new IllegalArgumentException("Not supported attribute name for long " + name);
		}

		public double getDoubleAttribute(String name) {
			Object result = getGenericAttribute(name);
			if (result != null) {
				if (result instanceof TagContent) {
					try {
						return new Double(((TagContent) result).getTextContent()).doubleValue();
					} catch (Exception e) {
					}
				} else if (result instanceof Double)
					return ((Double) result).doubleValue();
			} else
				return 0;
			throw new IllegalArgumentException("Not supported attribute name for double " + name);
		}

		public boolean getBoolAttribute(String name) {
			Object result = null;
			try {
				result = ESS_CHARACHTER.equals(name) ? getCompilation() : getGenericAttribute(name);
			} catch (de.vdheide.mp3.FrameDamagedException fde) {
			}
			if (result != null) {
				if (result instanceof TagContent) {
					return "1".equals(((TagContent) result).getTextContent());
				} else if (result instanceof Boolean)
					return ((Boolean) result).booleanValue();
			} else
				return false;
			throw new IllegalArgumentException("Not supported attribute name for boolean " + name);
		}

		public String toString() {
			try {
				return (getAttribute(TITLE) != null ? (String) getAttribute(TITLE) + ' ' : "")
						+ (getAttribute(ESS_MAKE) != null ? getAttribute(ESS_MAKE) : super.toString()) + ' '
						+ getAttribute(ESS_CHARACHTER) + ' ' + getAttribute(ESS_TIMESTAMP) + ' '
						+ getFloatAttribute(ESS_CHARACHTER);
			} catch (IllegalArgumentException iae) {
			}
			return super.toString();
		}

		/**
		 * returns for format such attributes as: title, artist, album, year,
		 * file
		 */

		public Object[] getFiveMajorAttributes() {
			try {
				fiveObjects[0] = getAttribute(TITLE)/* .getTextContent() */;
				if (fiveObjects[0] == null)
					fiveObjects[0] = Resources.LABEL_UNKNOWN;
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
				Date date = getYear().getDateContent();
				if (date != null)
					fiveObjects[3] = date.getYear();
				else
					fiveObjects[3] = new Integer(getIntAttribute(YEAR));
			} catch (Exception e) {
				fiveObjects[3] = "";
			}
			fiveObjects[4] = this.getName();
			return fiveObjects;
		}

		protected Object getGenericAttribute(String name) {
			try {
				return getClass().getMethod("get" + name, EMPTY_PARAMS).invoke(this, EMPTY_PARAMS);
			} catch (Throwable t) {
				throw new IllegalArgumentException(
						"Not supported attribute name for "
								+ name
								+ " <<"
								+ (t instanceof InvocationTargetException ? ((InvocationTargetException) t)
										.getTargetException() : t));
			}
		}

		@Override
		public <C extends MediaComponent> C[] getComponents() {
			return null;
		}

		protected Object[] fiveObjects = new Object[5];
	}

	public MediaPlayer getPlayer() {
		return new MP3Player(this);
	}

	public static class MP3Player extends SimpleMediaFormat.SimpleMediaPlayer {

		private Decoder decoder;
		private AudioDevice audio;
		private Bitstream bitstream;

		public MP3Player(MediaFormat mf) {
			init(mf);
		}

		@Override
		void playLoop() {
			decoder = new Decoder();
			try {
				audio = FactoryRegistry.systemRegistry().createAudioDevice();
			} catch (Exception e) {
				reportError(e.getMessage(), e);
				return;
			}
			//audio.open(decoder);
			try {
				bitstream = new Bitstream(inputStream = mediaFormat.getAsStream());
				for (int f = 0; (introSecs == 0 || f < introSecs); f++) {
					if (checkPause() == false)
						break;
					Header h = bitstream.readFrame();
					if (h == null)
						break;
					// sample buffer set when decoder constructed
					SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
					// TODO can be code for skipping or going back
					if (!audio.isOpen()) {
						((JavaSoundAudioDevice) audio).open(new AudioFormat(decoder.getOutputFrequency(), 16, decoder
								.getOutputChannels(), true, false));
						//System.err.println("Opened audio as "+af+'\n'+h);
					}
					//synchronized(MediaPlayerPanel.this) {	
					audio.write(output.getBuffer(), 0, output.getBufferLength());
					// TODO after skip this position is wrong
					positionSecs = audio.getPosition() / 1000;
					if (progress != null)
						progress.setValue(positionSecs);
					 
					//}
					bitstream.closeFrame();
				}
			} catch (Exception e) {
				reportError(e.getMessage(), e);
			}
		}

		@Override
		protected void freeResources() {
			if (audio != null)
				try {
					audio.close();
				} catch (Exception e) {
					System.err.printf("Line closing exception: %s%n", e);
				}
			if (bitstream != null)
				try {
					bitstream.close();
				} catch (BitstreamException e) {
				}
			super.freeResources();
		}
	}
}
