/* MediaChest - $RCSfile: PlayItem.java,v $ 
 * Copyright (C) 1999-2006 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: PlayItem.java,v 1.86 2013/05/08 03:49:48 cvs Exp $
 */
package photoorganizer.ipod;

import static photoorganizer.Resources.LIST_ACOUSTIC;
import static photoorganizer.Resources.LIST_ALBUM;
import static photoorganizer.Resources.LIST_ARTIST;
import static photoorganizer.Resources.LIST_BASS_BOOSTER;
import static photoorganizer.Resources.LIST_BASS_REDUCER;
import static photoorganizer.Resources.LIST_BITRATE;
import static photoorganizer.Resources.LIST_BPM;
import static photoorganizer.Resources.LIST_CLASSICAL;
import static photoorganizer.Resources.LIST_COMMENT;
import static photoorganizer.Resources.LIST_COMPILATION;
import static photoorganizer.Resources.LIST_COMPOSER;
import static photoorganizer.Resources.LIST_DANCE;
import static photoorganizer.Resources.LIST_DEEP;
import static photoorganizer.Resources.LIST_ELECTRONIC;
import static photoorganizer.Resources.LIST_FLAT;
import static photoorganizer.Resources.LIST_GENRE;
import static photoorganizer.Resources.LIST_HIP_HOP;
import static photoorganizer.Resources.LIST_JAZZ;
import static photoorganizer.Resources.LIST_KIND;
import static photoorganizer.Resources.LIST_LAST_MODIFY;
import static photoorganizer.Resources.LIST_LAST_PLAYED;
import static photoorganizer.Resources.LIST_LATIN;
import static photoorganizer.Resources.LIST_LAUNGE;
import static photoorganizer.Resources.LIST_LOUDENESS;
import static photoorganizer.Resources.LIST_NONE;
import static photoorganizer.Resources.LIST_NOTRATED;
import static photoorganizer.Resources.LIST_PIANO;
import static photoorganizer.Resources.LIST_PLAYCOUNT;
import static photoorganizer.Resources.LIST_POP;
import static photoorganizer.Resources.LIST_RATING;
import static photoorganizer.Resources.LIST_ROCK;
import static photoorganizer.Resources.LIST_R_N_B;
import static photoorganizer.Resources.LIST_SAMPLING_RATE;
import static photoorganizer.Resources.LIST_SIZE;
import static photoorganizer.Resources.LIST_SMALL_SPEAKERS;
import static photoorganizer.Resources.LIST_SPOKEN_WORD;
import static photoorganizer.Resources.LIST_TIME;
import static photoorganizer.Resources.LIST_TIME_ADDED;
import static photoorganizer.Resources.LIST_TITLE;
import static photoorganizer.Resources.LIST_TRACKNUMBER;
import static photoorganizer.Resources.LIST_TREBLE_BOOSTER;
import static photoorganizer.Resources.LIST_TREBLE_REDUCER;
import static photoorganizer.Resources.LIST_VOCAL_BOOSTER;
import static photoorganizer.Resources.LIST_YEAR;
import static photoorganizer.Resources.LIST_ALBUMARTIST;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.StringTokenizer;

import javax.swing.Icon;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;

import org.aldan3.util.inet.HttpUtils;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.renderer.IpodOptionsTab;
import de.vdheide.mp3.MP3File;

public class PlayItem extends BaseItem implements Comparable {
	static final String[] MHOD_TYPE = { "Unknown0", "Title", "Filename", "Album", "Artist", "Genre", "Filetype",
			"Equalizersetting", "Comment", "Unknown 9", "Unknown 10", "Unknown 11", "Composer", "Grouping",
			"Description", "Enclouse URL", "Podcast", "Chapter", "Subtitle", "Show", "Episode", "Network",
			"Album&Artist", "Sort artist", "Keywords", "Not use 25", "Not use 26", "Sort name", "Sort album",
			"Sort Album Artist", "Sort composer", "Sort show" };

	public static final int TITLE = 1;

	public static final int FILENAME = 2;

	public static final int ALBUM = 3;

	public static final int ARTIST = 4;

	public static final int GENRE = 5;

	public static final int FILETYPE = 6;

	public static final int EQ_SETTING = 7;

	public static final int COMMENT = 8;

	public static final int GROUPING = 13;

	public static final int COMPOSER = 12;

	public static final int SHOW = 19;

	public static final int EPIZODE = 20;

	public static final int ALBUM_ARTIST = 22;

	public static final int KEYWORDS = 24;

	public static final int SORT_TITLE = 27;

	public static final int SORT_ARTIST = 23;

	public static final int SORT_ALBUM_ARTIST = 29;

	public static final int SORT_ALBUM = 28;

	public static final int SORT_COMPOSER = 30;

	public static final int SORT_SHOW_TYPE = 31;

	static final int MAX_PROP = SORT_SHOW_TYPE;

	public static final int VIDEO_KIND = 11111;

	public static final int CATEGORY_TYPE = VIDEO_KIND + 1;

	static final int START_INT_PROP = 100;

	public static final int SIZE = START_INT_PROP + 1;

	public static final int LENGTH = START_INT_PROP + 2;

	public static final int BITRATE = START_INT_PROP + 3;

	public static final int VBR = START_INT_PROP + 4;

	public static final int VOLUME = START_INT_PROP + 5;

	public static final int START = START_INT_PROP + 6;

	public static final int STOP = START_INT_PROP + 7;

	public static final int ORDER = START_INT_PROP + 8;

	public static final int INDEX = START_INT_PROP + 9;

	public static final int YEAR = START_INT_PROP + 10;

	public static final int PLAYED_TIMES = START_INT_PROP + 11;

	public static final int RATING = START_INT_PROP + 12;

	public static final int DISK = START_INT_PROP + 13;

	public static final int NUM_DISKS = START_INT_PROP + 14;

	public static final int SAMPLE_RATE = START_INT_PROP + 15;

	public static final int BPM = START_INT_PROP + 16;

	public static final int NUM_TRACKS = START_INT_PROP + 17;

	public static final int SKIPPED_TIMES = START_INT_PROP + 18;

	public static final int SEASON_NUM = START_INT_PROP + 19;

	public static final int EPIZODE_NUM = START_INT_PROP + 20;

	static final int END_INT_PROP = EPIZODE_NUM;

	static final int START_TIMESTAMP_PROP = 200;

	public static final int CREATE_TIME = START_TIMESTAMP_PROP + 1; // added time

	public static final int LAST_TIME = START_TIMESTAMP_PROP + 2;

	public static final int MODIFIED_TIME = START_TIMESTAMP_PROP + 3;

	public static final int LAST_SKIPPED_TIME = START_TIMESTAMP_PROP + 4;

	static final int END_TIMESTAMP_PROP = LAST_SKIPPED_TIME;

	static final int START_BOOL_PROP = 300;

	public static final int COMPILATION = START_BOOL_PROP + 1;

	public static final int SKIP_SHUFFLING = START_BOOL_PROP + 2;

	public static final int REMEMBER_POS = START_BOOL_PROP + 3;

	public static final int GAPLESS_ALBUM = START_BOOL_PROP + 4;

	static final int END_BOOL_PROP = GAPLESS_ALBUM;

	static final int START_OBJ_PROP = 400;

	public static final int ARTWORK = START_OBJ_PROP + 1;

	public static final int END_OBJ_PROP = ARTWORK;

	//public static final int PLAYLISTENTRY = 100;

	public static final int MAX_RATING = 5;

	public static final Descriptor[] EXPORT_FIELDS = { new Descriptor(LIST_TITLE, TITLE),
			new Descriptor(LIST_ALBUM, ALBUM), new Descriptor(LIST_ARTIST, ARTIST),
			new Descriptor(LIST_ALBUMARTIST, ALBUM_ARTIST), new Descriptor(LIST_BITRATE, BITRATE),
			new Descriptor(LIST_SAMPLING_RATE, SAMPLE_RATE), new Descriptor(LIST_YEAR, YEAR),
			new Descriptor(LIST_GENRE, GENRE), new Descriptor(LIST_KIND, FILETYPE),
			new Descriptor(LIST_LAST_MODIFY, MODIFIED_TIME), new Descriptor(LIST_TRACKNUMBER, ORDER),
			new Descriptor(LIST_SIZE, SIZE), new Descriptor(LIST_TIME, LENGTH), new Descriptor(LIST_COMMENT, COMMENT),
			new Descriptor(LIST_TIME_ADDED, CREATE_TIME), new Descriptor(LIST_COMPOSER, COMPOSER),
			new Descriptor(LIST_PLAYCOUNT, PLAYED_TIMES), new Descriptor(LIST_LAST_PLAYED, LAST_TIME),
			new Descriptor(LIST_RATING, RATING), new Descriptor(LIST_COMPILATION, COMPILATION),
			new Descriptor(LIST_BPM, BPM) };

	public static final Descriptor[] EQUALISATIONS = { new Descriptor(LIST_NONE, -1, null),
			new Descriptor(LIST_ACOUSTIC, 100, "#!#100#!#"), new Descriptor(LIST_BASS_BOOSTER, 101, "#!#101#!#"),
			new Descriptor(LIST_BASS_REDUCER, 102, "#!#102#!#"), new Descriptor(LIST_CLASSICAL, 103, "#!#103#!#"),
			new Descriptor(LIST_DANCE, 104, "#!#104#!#"), new Descriptor(LIST_DEEP, 105, "#!#105#!#"),
			new Descriptor(LIST_ELECTRONIC, 106, "#!#106#!#"), new Descriptor(LIST_FLAT, 107, "#!#107#!#"),
			new Descriptor(LIST_HIP_HOP, 108, "#!#108#!#"), new Descriptor(LIST_JAZZ, 109, "#!#109#!#"),
			new Descriptor(LIST_LATIN, 110, "#!#110#!#"), new Descriptor(LIST_LOUDENESS, 111, "#!#111#!#"),
			new Descriptor(LIST_LAUNGE, 112, "#!#112#!#"), new Descriptor(LIST_PIANO, 113, "#!#113#!#"),
			new Descriptor(LIST_POP, 114, "#!#114#!#"), new Descriptor(LIST_R_N_B, 115, "#!#115#!#"),
			new Descriptor(LIST_ROCK, 116, "#!#116#!#"), new Descriptor(LIST_SMALL_SPEAKERS, 117, "#!#117#!#"),
			new Descriptor(LIST_SPOKEN_WORD, 118, "#!#118#!#"), new Descriptor(LIST_TREBLE_BOOSTER, 119, "#!#119#!#"),
			new Descriptor(LIST_TREBLE_REDUCER, 120, "#!#120#!#"),
			new Descriptor(LIST_VOCAL_BOOSTER, 121, "#!#121#!#"), };

	String properties[];

	// TODO: convert some int props to long
	int i_properties[];

	long l_properties[];

	Date d_properties[];

	boolean b_properties[];

	Object o_properties[];

	protected MediaFormat attachedMedia;

	//
	protected PlayItem delegatePlayItem;

	protected ArtworkDB.ImageItem imageItem;

	protected String videoKind;

	protected PlayItem() {
		// for cloning
	}

	public PlayItem(int id) {
		this.id = id;
		properties = new String[MAX_PROP];
		i_properties = new int[END_INT_PROP - START_INT_PROP];
		i_properties[INDEX - START_INT_PROP - 1] = id;
		d_properties = new Date[END_TIMESTAMP_PROP - START_TIMESTAMP_PROP];
		b_properties = new boolean[END_BOOL_PROP - START_BOOL_PROP];
		o_properties = new Object[END_OBJ_PROP - START_OBJ_PROP];
	}

	public Object clone() {
		return new PlayItemClone(this);
	}

	public static PlayItem create(MediaFormat attachedMedia, Controller controller) {
		// the id will be set later
		// there are actually two approaches here
		// 1. register each existing number, then based on max or
		// some gaps to generate new numbers
		// 2. regenerate all numbers at writing time based on directory
		PlayItem result = new PlayItem(0);
		String fileName = attachedMedia.getFile().getAbsolutePath();
		result.set(FILENAME, fileName);
		update(result, attachedMedia, IpodOptionsTab.parsePath(fileName, null, controller), IpodOptionsTab
				.isOverrideTag(controller));
		return result;
	}

	public static void update(PlayItem playItem, MediaFormat attachedMedia, String[] defaults, boolean override) {
		playItem.attachedMedia = attachedMedia;
		MediaInfo info = attachedMedia.getMediaInfo();
		if (defaults == null) {
			// TODO: possible to use a loop
			defaults = new String[IpodOptionsTab.MAXINFO_FIELDS];
			defaults[IpodOptionsTab.TITLE] = (String) playItem.get(TITLE);
			defaults[IpodOptionsTab.ALBUM] = (String) playItem.get(ALBUM);
			defaults[IpodOptionsTab.ARTIST] = (String) playItem.get(ARTIST);
			defaults[IpodOptionsTab.COMPOSER] = (String) playItem.get(COMPOSER);
			defaults[IpodOptionsTab.GENRE] = (String) playItem.get(GENRE);
			defaults[IpodOptionsTab.YEAR] = playItem.get(YEAR) != null ? playItem.get(YEAR).toString() : null;
			defaults[IpodOptionsTab.TRACK] = playItem.get(ORDER) != null ? playItem.get(ORDER).toString() : null;
		}
		setWithDefault(playItem, info, MediaInfo.TITLE, TITLE, defaults[IpodOptionsTab.TITLE], override);
		playItem.set(FILETYPE, MP3.MP3.equals(playItem.attachedMedia.getFormat(MediaFormat.AUDIO)) ? "MPEG audio file"
				: (playItem.attachedMedia.getType() & MediaFormat.VIDEO) > 0 ? "MPEG-4 video file"
						: playItem.attachedMedia.getDescription());
		if ((playItem.attachedMedia.getType() & MediaFormat.VIDEO) > 0) {
			playItem.videoKind = Resources.HDR_MOVIES;
			playItem.set(REMEMBER_POS, true);
			playItem.set(SKIP_SHUFFLING, true);
		} else {
			setWithDefault(playItem, info, MediaInfo.ALBUM, ALBUM, defaults[IpodOptionsTab.ALBUM], override);
			setWithDefault(playItem, info, MediaInfo.ARTIST, ARTIST, defaults[IpodOptionsTab.ARTIST], override);
			setWithDefault(playItem, info, MediaInfo.COMPOSER, COMPOSER, defaults[IpodOptionsTab.COMPOSER], override);
			if (override && defaults[IpodOptionsTab.GENRE] != null)
				playItem.set(GENRE, defaults[IpodOptionsTab.GENRE]);
			else
				try {
					String genre = MP3.findGenre(info);
					if (genre != null && genre.length() > 0)
						playItem.set(GENRE, genre);
					else
						playItem.set(GENRE, defaults[IpodOptionsTab.GENRE]);
				} catch (Exception e) {
					playItem.set(GENRE, defaults[IpodOptionsTab.GENRE]);
				}
			playItem.set(EQ_SETTING, (String) null);
		}
		setWithDefault(playItem, info, MediaInfo.COMMENTS, COMMENT, (String) null, false);
		setWithDefault(playItem, info, MediaInfo.LENGTH, LENGTH, 0, 1000, -1);
		setWithDefault(playItem, info, MediaInfo.BPM, BPM, 0, 1, -1);
		int year = 0;
		if (defaults[IpodOptionsTab.YEAR] != null)
			try {
				year = Integer.parseInt(defaults[IpodOptionsTab.YEAR]);
			} catch (Exception e) {
				System.err.println("Can't parse year " + defaults[IpodOptionsTab.YEAR]);
			}
		if (override && year > 0)
			playItem.set(YEAR, year);
		else
			setWithDefault(playItem, info, info.YEAR, YEAR, year, 1, 0);
		playItem.set(SIZE, (int) playItem.attachedMedia.getFileSize());
		playItem.set(BITRATE, info.getIntAttribute(info.BITRATE));
		playItem.set(SAMPLE_RATE, info.getIntAttribute(info.SAMPLERATE));
		try {
			playItem.set(VBR, info.getBoolAttribute(info.VBR) ? 256 : 0);
		} catch (Exception e) {
		}
		try {
			StringTokenizer st = new StringTokenizer((String) info.getAttribute(info.PARTOFSET), "/");
			if (st.hasMoreTokens()) {
				playItem.set(DISK, Integer.parseInt(st.nextToken()));
				if (st.hasMoreTokens())
					playItem.set(NUM_DISKS, Integer.parseInt(st.nextToken()));
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		playItem.set(VOLUME, 0);
		playItem.set(START, 0);
		playItem.set(STOP, 0);
		int track = 0;
		if (defaults[IpodOptionsTab.TRACK] != null)
			try {
				track = Integer.parseInt(defaults[IpodOptionsTab.TRACK]);
			} catch (Exception e) {
				System.err.println("Can't parse track " + defaults[IpodOptionsTab.TRACK]);
			}
		if (override && track > 0)
			playItem.set(ORDER, track);
		else
			setWithDefault(playItem, info, MediaInfo.TRACK, ORDER, track, 1, 0);
		setWithDefault(playItem, info, MediaInfo.PLAYCOUNTER, PLAYED_TIMES, 0, 1, 0);
		try {
			playItem.set(COMPILATION, info.getBoolAttribute(MediaInfo.COMPILATION));
		} catch (java.lang.IllegalArgumentException iae) {
		}
		playItem.set(CREATE_TIME, new Date());
		playItem.set(LAST_TIME, (Date) null);
		playItem.set(MODIFIED_TIME, new Date(playItem.attachedMedia.getFile().lastModified()));
		Icon ico = attachedMedia.getThumbnail(null);				
		if (ico != null)
			playItem.setImage(new ArtworkDB.ImageItem());
		playItem.resetState(STATE_METASYNCED);
	}

	static void setWithDefault(PlayItem playItem, MediaInfo info, String indexSrc, int indexDst, String defValue,
			boolean override) {
		String setVal = null;
		if (override && defValue != null)
			setVal = defValue;
		else
			try {
				setVal = (String) info.getAttribute(indexSrc);
				if (setVal == null || setVal.length() == 0)
					setVal = defValue;
			} catch (IllegalArgumentException iae) {
				setVal = defValue;
			}
		Transliteration tl = IpodOptionsTab.getTransliteration(null);
		try {
			playItem.set(indexDst, tl == null ? setVal : tl.translite(setVal));
		} catch (Throwable t) {
			System.err.println("An exception in trransit ignored, value not set. " + setVal + " " + t);
		}
	}

	static void setWithDefault(PlayItem playItem, MediaInfo info, String indexSrc, int indexDst, int defValue,
			int factor, int invalid) {
		int value = invalid;
		try {
			value = info.getIntAttribute(indexSrc);
		} catch (IllegalArgumentException iae) {
			try {
				value = (int) info.getLongAttribute(indexSrc);
			} catch (IllegalArgumentException iae1) {
				value = defValue;
			}
		}
		if (value == invalid)
			value = defValue;
		playItem.set(indexDst, value * factor);
	}

	static void setWithDefault(PlayItem playItem, MediaInfo info, String indexSrc, int indexDst, int defValue) {
		setWithDefault(playItem, info, indexSrc, indexDst, defValue, 1, 0);
	}

	public void set(int index, int value) {
		if (index > START_INT_PROP && index <= END_INT_PROP)
			i_properties[index - START_INT_PROP - 1] = value;
		else if (index > START_TIMESTAMP_PROP && index <= END_TIMESTAMP_PROP) {
			d_properties[index - START_TIMESTAMP_PROP - 1] = value == 0 ? null : new Date(value * 1000l);
		}
	}

	public void set(int index, String value) {
		if (index - 1 < properties.length)
			properties[index - 1] = value;
		else if (index > START_INT_PROP && index <= END_INT_PROP)
			try {
				if (value != null && value.length() > 0)
					i_properties[index - START_INT_PROP - 1] = Integer.parseInt(value);
			} catch (Exception e) {
				System.err.println("Value " + value + " can't be converted to int for index " + index);
			}
		else if (index > START_BOOL_PROP && index <= END_BOOL_PROP)
			try {
				if (value != null && value.length() > 0)
					b_properties[index - START_BOOL_PROP - 1] = Boolean.valueOf(value).booleanValue();
			} catch (Exception e) {
				System.err.println("Value " + value + " can't be converted to boolean for index " + index);
			}
		else if (index > START_OBJ_PROP && index <= END_OBJ_PROP)
			o_properties[index - START_OBJ_PROP - 1] = value;
		else if (index == VIDEO_KIND)
			videoKind = value;
		else
			System.err.println("Attempt to set invalid index " + index + " value " + value);
	}

	public void set(int index, Date value) {
		d_properties[index - START_TIMESTAMP_PROP - 1] = value;
	}

	public void set(int index, boolean value) {
		b_properties[index - START_BOOL_PROP - 1] = value;
	}

	/*public void set(int index, File value) {
	 String name = value.getName();
	 int xp = name.lastIndexOf('.');
	 if (xp > 0)
	 properties[index-1] = name.substring(0, xp);
	 else
	 properties[index-1] = name;
	 }*/

	public Object get(int index) {
		if (index > 0 && index <= MAX_PROP)
			return properties[index - 1];
		if (index > START_INT_PROP && index <= END_INT_PROP)
			return new Integer(i_properties[index - START_INT_PROP - 1]);
		if (index > START_TIMESTAMP_PROP && index <= END_TIMESTAMP_PROP)
			return d_properties[index - START_TIMESTAMP_PROP - 1];
		if (index > START_BOOL_PROP && index <= END_BOOL_PROP)
			return new Boolean(b_properties[index - START_BOOL_PROP - 1]);
		if (index > START_OBJ_PROP && index <= END_OBJ_PROP)
			return o_properties[index - START_OBJ_PROP - 1];
		if (index == VIDEO_KIND) {
			return videoKind;
		}

		return null; // ?? exception ??
		//delegatePlayItem==null?
	}

	public void setImage(ArtworkDB.ImageItem ii) {
		imageItem = ii;
	}

	public ArtworkDB.ImageItem getImage() {
		return imageItem;
	}

	public String getTypeName(int type) {
		if (type >= 0 && type < MHOD_TYPE.length)
			return MHOD_TYPE[type];
		return "Type " + type + " is invalid";
	}

	public void syncTag(String dev) {
		// can do for id3 only
		if (attachedMedia == null && dev != null)
			attachedMedia = MediaFormatFactory.createMediaFormat(getFile(dev));
		if (attachedMedia == null || attachedMedia.getType() != MediaFormat.AUDIO) {
			System.err.println("Can't access media file, or it isn't MP3 [" + getFile(dev));
			return;
		}
		MediaInfo info = attachedMedia.getMediaInfo();
		syncAttr(info, MediaInfo.TITLE, TITLE);
		syncAttr(info, MediaInfo.ALBUM, ALBUM);
		syncAttr(info, MediaInfo.ARTIST, ARTIST);
		syncAttr(info, MediaInfo.COMMENTS, COMMENT);
		syncAttr(info, MediaInfo.COMPOSER, COMPOSER);
		syncAttr(info, MediaInfo.GENRE, GENRE);
		syncAttr(info, MediaInfo.BPM, BPM);
		syncAttr(info, MediaInfo.PLAYCOUNTER, PLAYED_TIMES);
		syncAttr(info, MediaInfo.POPULARIMETER, RATING);
		syncAttr(info, MediaInfo.TRACK, ORDER);
		syncAttr(info, MediaInfo.OFTRACKS, NUM_TRACKS);
		syncAttr(info, MediaInfo.YEAR, YEAR);
		syncAttr(info, MediaInfo.LASTPLAY, LAST_TIME);
		syncAttr(info, MediaInfo.COMPILATION, COMPILATION);
		syncAttr(info, MediaInfo.RELATIVEVOLUMENADJUSTMENT, VOLUME);
		syncAttr(info, MediaInfo.GROUPIDENTIFICATIONREGISTRATION, GROUPING);
		try {
			info.setAttribute(MediaInfo.PARTOFSET, "" + get(DISK) + '/' + get(NUM_DISKS));
		} catch (Exception e) {
			System.err.println("Field " + MediaInfo.PARTOFSET + " can't be set to '" + get(DISK) + '/' + get(NUM_DISKS)
					+ "' " + e);
		}
		try {
			((MP3File) info).update();
		} catch (Exception e) {
			System.err.println("Problem sync ID3 tag, " + e);
		}
		attachedMedia = null; // for GC
	}

	protected void syncAttr(MediaInfo info, String infoType, int sourceIndex) {
		try {
			info.setAttribute(infoType, get(sourceIndex));
		} catch (Exception e) {
			System.err.println("Field " + infoType + " can't be set to '" + get(sourceIndex) + "' " + e);
		}
	}

	public File getFile(String dev) {
		if (get(FILENAME) == null)
			throw new IllegalStateException("File name isn't set.");
		return isState(STATE_COPIED) == false ? new File((String) get(FILENAME)) : new File(dev
				+ ((String) get(FILENAME)).replace(':', File.separatorChar));
	}

	public MediaFormat getAttachedFormat() {
		return attachedMedia;
	}

	public int getId() {
		return delegatePlayItem != null ? delegatePlayItem.getId() : id;
	}

	public String toString() {
		return properties[TITLE - 1] == null ? "" + id : properties[TITLE - 1];
	}

	public String toXML() {
		if (delegatePlayItem != null)
			return delegatePlayItem.toXML();
		StringBuffer result = new StringBuffer(120);
		result.append("\t<song>\r\n");
		for (int i = 0; i < EXPORT_FIELDS.length; i++) {
			Object o = get(EXPORT_FIELDS[i].selector);
			if (o != null) {
				String name = EXPORT_FIELDS[i].name.replace(' ', '_');
				result.append("\t\t<").append(name).append('>');
				result.append(HttpUtils.htmlEncode(o.toString())).append("</");
				result.append(name).append(">\r\n");
			}
		}
		result.append("\t</song>");
		return result.toString();

	}

	public String toCSV() {
		if (delegatePlayItem != null)
			return delegatePlayItem.toCSV();
		StringBuffer result = new StringBuffer(120);
		for (int i = 0; i < EXPORT_FIELDS.length; i++) {
			Object o = get(EXPORT_FIELDS[i].selector);
			if (i > 0)
				result.append(',');
			if (o != null)
				result.append('"').append(o.toString()).append('"');
		}
		return result.toString();
	}

	/** compares in order artist->album->(disk|track|title)
	 */
	public int compareTo(Object o) {
		// TODO: pull in a separate class Comparator to provide customization

		if (o == null)
			return -1;
		if (o == this)
			return 0;
		if (o instanceof PlayItem == false)
			throw new IllegalArgumentException("An attempt to compare play item to " + o.getClass().getName());
		PlayItem pi = (PlayItem) o;
		//System.err.printf("Comaring %s tp %s %d / %d -- %d / %d\n", this, pi, i_properties[ORDER-START_INT_PROP-1], pi.i_properties[ORDER-START_INT_PROP-1],
		//        get(ORDER), pi.get(ORDER));
		if (pi.delegatePlayItem != null && delegatePlayItem != null) // in playlist
			return (Integer) get(ORDER) - (Integer) pi.get(ORDER);
		//return i_properties[ORDER-START_INT_PROP-1] - pi.i_properties[ORDER-START_INT_PROP-1];
		// album
		if (properties[ALBUM - 1] == null) {
			if (pi.properties[ALBUM - 1] != null)
				return 1;
			else
				return 0;
		} else {
			if (pi.properties[ALBUM - 1] == null)
				return -1;
		}
		int result = normalize(properties[ALBUM - 1], false).compareTo(
				normalize(pi.properties[ALBUM - 1], false));
		if (result != 0)
			return result;
		// disk no
		result = i_properties[DISK - START_INT_PROP - 1] - pi.i_properties[DISK - START_INT_PROP - 1];
		if (result != 0)
			return result;
		// track
		result = i_properties[ORDER - START_INT_PROP - 1] - pi.i_properties[ORDER - START_INT_PROP - 1];
		if (result != 0)
			return result;
		if (b_properties[COMPILATION - START_BOOL_PROP - 1] == false) {
			if (properties[ARTIST - 1] == null) {
				if (pi.properties[ARTIST - 1] != null)
					return 1;
				return 0;
			} else {
				if (pi.properties[ARTIST - 1] == null)
					return -1;
			}
			result = normalize(properties[ARTIST - 1], true).compareTo(
					normalize(pi.properties[ARTIST - 1], true));
			if (result != 0)
				return result;
		}
		// title
		if (properties[TITLE - 1] == null) {
			if (pi.properties[TITLE - 1] != null)
				return 1;
			else
				return 0;
		} else {
			if (pi.properties[TITLE - 1] == null)
				return -1;
		}
		return normalize(properties[TITLE - 1], false).compareTo(normalize(pi.properties[TITLE - 1], false));
	}

	public static String[] getRatingArray() {
		// TODO: cache result
		String[] ratings = new String[MAX_RATING + 1];
		ratings[0] = LIST_NOTRATED;
		ratings[1] = "*";
		for (int r = 2; r < PlayItem.MAX_RATING + 1; r++)
			ratings[r] = ratings[r - 1] + '*';
		return ratings;
	}

	/** string normalization
	 */
	public static String normalize(String s, boolean ignoreArticles) {
		if (s == null)
			return null;
		s = s.toUpperCase().trim();
		if (s.startsWith("'"))
			s = s.substring(1);
		else if (s.startsWith("\""))
			s = s.substring(1);
		else if (s.startsWith("\u00AB"))
			s = s.substring(1);
		if (ignoreArticles) {
			if (s.startsWith("THE "))
				s = s.substring(4);
			if (s.startsWith("A "))
				s = s.substring(2);
			if (s.startsWith("AN "))
				s = s.substring(3);
			if (s.startsWith("DIE "))
				s = s.substring(4);
			if (s.startsWith("DE "))
				s = s.substring(3);
			if (s.startsWith("LA "))
				s = s.substring(3);
		}
		return s;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o instanceof PlayItem == false)
			return false;
		//return Arrays.equals(properties, ((PlayItem)o).properties);
		//title, album, artist, genre, composer, filesize are the same
		if (properties[FILENAME - 1] != null
				&& properties[FILENAME - 1].equals(((PlayItem) o).properties[FILENAME - 1]))
			return true;

		if (properties[TITLE - 1] != null) {
			if (properties[TITLE - 1].equals(((PlayItem) o).properties[TITLE - 1]) == false)
				return false;
		} else if (((PlayItem) o).properties[TITLE - 1] != null)
			return false;
		if (properties[ALBUM - 1] != null) {
			if (properties[ALBUM - 1].equals(((PlayItem) o).properties[ALBUM - 1]) == false)
				return false;
		} else if (((PlayItem) o).properties[ALBUM - 1] != null)
			return false;
		if (properties[ARTIST - 1] != null) {
			if (properties[ARTIST - 1].equals(((PlayItem) o).properties[ARTIST - 1]) == false)
				return false;
		} else if (((PlayItem) o).properties[ARTIST - 1] != null)
			return false;
		if (properties[GENRE - 1] != null) {
			if (properties[GENRE - 1].equals(((PlayItem) o).properties[GENRE - 1]) == false)
				return false;
		} else if (((PlayItem) o).properties[GENRE - 1] != null)
			return false;
		if (properties[COMPOSER - 1] != null) {
			if (properties[COMPOSER - 1].equals(((PlayItem) o).properties[COMPOSER - 1]) == false)
				return false;
		} else if (((PlayItem) o).properties[COMPOSER - 1] != null)
			return false;
		return i_properties[SIZE - START_INT_PROP - 1] == (((PlayItem) o).i_properties[SIZE - START_INT_PROP - 1]);
	}

	@Override
	public int hashCode() {

		return (properties[FILENAME - 1] == null ? 0 : properties[FILENAME - 1].hashCode())
				^ (properties[TITLE - 1] == null ? 0 : properties[TITLE - 1].hashCode())
				^ (properties[ARTIST - 1] == null ? 0 : properties[ARTIST - 1].hashCode())
				^ (properties[ALBUM - 1] == null ? 0 : properties[ALBUM - 1].hashCode())
				^ (properties[GENRE - 1] == null ? 0 : properties[GENRE - 1].hashCode());

	}

	protected static class PlayItemClone extends PlayItem {
		protected int index;

		PlayItemClone(PlayItem origPlayItem) {
			delegatePlayItem = origPlayItem;
			// Notice that no System.arraycopy(properties, 0, result.properties, 0, properties.length); here
			// since we need synchronize changes
			properties = delegatePlayItem.properties;
			i_properties = delegatePlayItem.i_properties;
			index = i_properties[ORDER - START_INT_PROP - 1];
			d_properties = delegatePlayItem.d_properties;
			b_properties = delegatePlayItem.b_properties;
			o_properties = delegatePlayItem.o_properties;
			l_properties = delegatePlayItem.l_properties;
			videoKind = delegatePlayItem.videoKind;
			state = delegatePlayItem.state;
			id = delegatePlayItem.id;
		}

		public void set(int index, int value) {
			if (ORDER == index) {
				this.index = value;
			} else
				super.set(index, value);
		}

		public void set(int index, String value) {
			if (ORDER == index) {
				try {
					this.index = new Integer(value);
				} catch (NumberFormatException nfe) {

				}
			} else
				super.set(index, value);
		}

		public Object get(int index) {
			if (ORDER == index) {
				return this.index;
			} else
				return super.get(index);
		}

		@Override
		public int getState() {
			return delegatePlayItem.getState();
		}

		@Override
		public boolean isState(int mask) {
			return delegatePlayItem.isState(mask);
		}

		@Override
		public void resetState(int state) {
			delegatePlayItem.resetState(state);
		}

		@Override
		public void setState(int state) {
			delegatePlayItem.setState(state);
		}
	}

	public static class SongComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
				throw new ClassCastException("Can't compare non PlayItem");
			return compare((PlayItem) o1, (PlayItem) o2, TITLE, false);
		}

		public int compare(PlayItem pi1, PlayItem pi2, int index, boolean norm) {
			// TODO: perfomance wise it can be better to separate compare
			// for different types in separate methods

			if (index > 0 && index <= MAX_PROP) {
				if (pi1.properties[index - 1] == null) {
					if (pi2.properties[index - 1] != null)
						return 1;
					else
						return 0;
				} else {
					if (pi2.properties[index - 1] == null)
						return -1;
				}
				//			if (norm)
				return normalize(pi1.properties[index - 1], norm).compareTo(normalize(pi2.properties[index - 1], norm));
				//			else
				//				return pi1.properties[index-1].compareTo(pi2.properties[index-1]);
			} else if (index > START_INT_PROP && index <= END_INT_PROP) {
				return pi1.i_properties[index - START_INT_PROP - 1] - pi2.i_properties[index - START_INT_PROP - 1];
			} else if (index > START_TIMESTAMP_PROP && index <= END_TIMESTAMP_PROP) {
				if (pi1.d_properties[index - START_TIMESTAMP_PROP - 1] == null) {
					if (pi2.d_properties[index - START_TIMESTAMP_PROP - 1] != null)
						return 1;
					else
						return 0;
				} else {
					if (pi2.d_properties[index - START_TIMESTAMP_PROP - 1] == null)
						return -1;
				}

				return pi1.d_properties[index - START_TIMESTAMP_PROP - 1].compareTo(pi2.d_properties[index
						- START_TIMESTAMP_PROP - 1]);
			}
			throw new IllegalArgumentException("Item index isn't supported " + index);
		}

		public boolean equals(Object obj) {
			return super.equals(obj);
		}

	}

	public static class ArtistComparator extends SongComparator {

		public int compare(Object o1, Object o2) {
			if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
				throw new ClassCastException("Can't compare non PlayItem");
			PlayItem pi1 = (PlayItem) o1;
			PlayItem pi2 = (PlayItem) o2;
			int result = compare(pi1, pi2, ARTIST, true);
			if (result != 0)
				return result;
			else
				return compare(pi1, pi2, TITLE, false);
		}
	}

	public static class AlbumComparator extends SongComparator {

		public int compare(Object o1, Object o2) {
			if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
				throw new ClassCastException("Can't compare non PlayItem");
			PlayItem pi1 = (PlayItem) o1;
			PlayItem pi2 = (PlayItem) o2;
			int result = compare(pi1, pi2, ALBUM, false);
			if (result != 0)
				return result;
			else {
				result = compare(pi1, pi2, ARTIST, true);
				if (result != 0)
					return result;
				else {
					result = pi1.i_properties[ORDER - START_INT_PROP - 1]
							- pi2.i_properties[ORDER - START_INT_PROP - 1];
					if (result != 0)
						return result;
					else
						return compare(pi1, pi2, TITLE, false);
				}
			}
		}
	}

	public static class GenreComparator extends AlbumComparator {

		public int compare(Object o1, Object o2) {
			if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
				throw new ClassCastException("Can't compare non PlayItem");
			PlayItem pi1 = (PlayItem) o1;
			PlayItem pi2 = (PlayItem) o2;
			int result = compare(pi1, pi2, GENRE, true);
			if (result != 0)
				return result;
			else
				return super.compare(pi1, pi2);
		}
	}

	public static class ComposerComparator extends AlbumComparator {

		public int compare(Object o1, Object o2) {
			if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
				throw new ClassCastException("Can't compare non PlayItem");
			PlayItem pi1 = (PlayItem) o1;
			PlayItem pi2 = (PlayItem) o2;
			int result = compare(pi1, pi2, COMPOSER, true);
			if (result != 0)
				return result;
			else
				return super.compare(pi1, pi2);
		}
	}

	public static class GenericFieldComparator extends SongComparator {
		boolean most;

		int field;

		public GenericFieldComparator(int field, boolean most) {
			this.most = most;
			this.field = field;
		}

		public int compare(Object o1, Object o2) {
			if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
				throw new ClassCastException("Can't compare non PlayItem");
			int result = compare((PlayItem) o1, (PlayItem) o2, field, false);
			return most ? result : -result;
		}
	}
	/*
	 public static class YearComparator extends SongComparator {
	 boolean most;
	 public YearComparator(boolean most) {
	 this.most = most;
	 }
	 
	 public int compare(Object o1, Object o2) {
	 if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
	 throw new ClassCastException("Can't compare non PlayItem");
	 PlayItem pi1 = (PlayItem)o1;
	 PlayItem pi2 = (PlayItem)o2;
	 int result = compare(pi1, pi2, YEAR, false);
	 return most?result:-result;
	 }
	 }

	 public static class RatingComparator extends SongComparator {
	 boolean most;
	 public RatingComparator(boolean most) {
	 this.most = most;
	 }
	 
	 public int compare(Object o1, Object o2) {
	 if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
	 throw new ClassCastException("Can't compare non PlayItem");
	 int result = compare((PlayItem)o1, (PlayItem)o2, RATING, false);
	 return most?result:-result;
	 }
	 }

	 public static class LastPlayComparator extends SongComparator {
	 boolean most;
	 public LastPlayComparator(boolean most) {
	 this.most = most;
	 }
	 
	 public int compare(Object o1, Object o2) {
	 if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
	 throw new ClassCastException("Can't compare non PlayItem");
	 int result = compare((PlayItem)o1, (PlayItem)o2, LAST_TIME, false);
	 return most?result:-result;
	 }
	 }
	 public static class DateAddedComparator extends SongComparator {
	 boolean most;
	 public DateAddedComparator(boolean most) {
	 this.most = most;
	 }
	 
	 public int compare(Object o1, Object o2) {
	 if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
	 throw new ClassCastException("Can't compare non PlayItem");
	 int result = compare((PlayItem)o1, (PlayItem)o2, CREATE_TIME, false);
	 return most?result:-result;
	 }
	 }
	 public static class PlayCountComparator extends SongComparator {
	 boolean most;
	 public PlayCountComparator(boolean most) {
	 this.most = most;
	 }
	 
	 public int compare(Object o1, Object o2) {
	 if (o1 instanceof PlayItem == false || o2 instanceof PlayItem == false)
	 throw new ClassCastException("Can't compare non PlayItem");
	 int result = compare((PlayItem)o1, (PlayItem)o2, PLAYED_TIMES, false);
	 return most?result:-result;
	 }
	 }*/
}
