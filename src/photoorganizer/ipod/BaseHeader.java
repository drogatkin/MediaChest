/* MediaChest - $RCSfile: BaseHeader.java,v $ 
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
 *  $Id: BaseHeader.java,v 1.110 2012/11/30 08:07:05 cvs Exp $
 */
package photoorganizer.ipod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import mediautil.gen.BasicIo;

import org.aldan3.util.HexDump;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Resources;
import photoorganizer.media.ContentMatcher;

/** This class represent generic iPodDB header
 * It has certain overhead on keeping exceeded fields and copying them to actual
 * play item when they needed.
 * Xumster implementation is much smarter so consider it as an example of development 
 */
public class BaseHeader {
	public static final int MHBD = 0;

	public static final int MHSD = 1;

	public static final int MHLT = 2;

	public static final int MHLP = 3;

	public static final int MHYP = 4;

	public static final int MHIP = 5;

	public static final int MHIT = 6;

	public static final int MHOD = 7;

	public static final int MHDP = 8;

	// photo related headers
	public static final int MHFD = 9;

	public static final int MHLI = 10;

	public static final int MHII = 11;

	public static final int MHNI = 12;

	public static final int MHLA = 13;

	public static final int MHLF = 14;

	public static final int MHIF = 15;
	
	public static final int MHBA = 16;
	
	public static final int MHIA = 17;

	// public static final int

	public static final int NUM_HEADERS = MHIA + 1;

	public static final int PLAYLISTENTRY = 100;

	public static final int SMARTLISTDEF = 50;

	public static final int SMARTLISTENTRY = 51;

	public static final int LISTATTRIBUTE = 52;

	public static final int RATING_FACTOR = 20;
	
	// Photo/artwork MHOD types
	
	public static final int IMAGEALBUMNAME_STRING = 1;
	
	public static final int IMAGEFILENAME_STRING = 3;
	
	public static final int IMAGETHUMBNAIL_CONTAINER = 2;
	
	public static final int IMAGE_CONTAINER = 5;

	public enum DisplayType {
		AudioVideo, Audio, Video, Podcast, VideoPodcast, Audiobook, MusicVideo, TVShow, MusicTVShow
	};

	String signature;

	int size;

	int totalSize;

	int numThings;

	int num;

	int reference, reference11;

	int index;

	int userId;

	Date createDate, lastDate, modifiedDate, lastSkipped;

	DisplayType displaytype;

	int vbr, fileSize, length, order, encoding, bitRate, volume, start, prevRating, stop, numPlayed, numSkipped, year,
			rating, disk, num_disks, BPM, tracks, hash1, hash2, artworkSize;
	
	int mediaFormat ;

	boolean playList;

	boolean thingsList;

	boolean filesList;

	boolean albumList;
	
	boolean artists;
	
	boolean genius_cuid;

	boolean visible;

	boolean main;

	boolean compilation;

	boolean checked;
	
	boolean mhsd_10;

	String data, type;

	Object complexData;

	protected byte[] buf;

	// state of header
	int readCount;

	boolean skip_shuffling;

	boolean remember_pos;

	int gapless_data; // length in byte to last 8 frames
	
	int season, epizode;

	boolean gapless_track;

	boolean gapless_album;

	boolean movie;

	///////////////////// photo album ///////////
	
	int albumType, transitionDirection, slideDuration, transitionDuration;

	boolean playMusic, repeat, random, showTitles;
	
	short vp,hp; // vertical and horizontal paddings

	public void reset() {
		signature = null;
		size = 0;
		totalSize = 0;
		numThings = 0;
		num = 0;
		reference = 0;
		reference11 = 0;
		index = 0;
		createDate = null;
		lastDate = null;
		modifiedDate = null;
		vbr = 0;
		fileSize = 0;
		length = 0;
		order = 0;
		rating = 0;
		encoding = 0;
		bitRate = 0;
		volume = 0;
		start = 0;
		tracks = 0;
		stop = 0;
		numPlayed = 0;
		num_disks = 0;
		disk = 0;
		BPM = 0;
		hash1 = 0;
		hash2 = 0;
		playList = false;
		thingsList = false;
		visible = false;
		main = false;
		compilation = false;
		data = null;
		type = null;
		filesList = false;
		albumList = false;
		readCount = 0;
		skip_shuffling = false;
		remember_pos = false;
		gapless_data = 0;
		gapless_track = false;
		gapless_album = false;
		movie = false;
		displaytype = DisplayType.AudioVideo;
		mediaFormat = 0;
		season=epizode=0;
		artworkSize = 0;
		albumType=0; transitionDirection = 0; slideDuration = 0;  transitionDuration = 0;
		playMusic  = false; repeat  = false; random  = false; showTitles = false;
		vp=hp=0;
	}

	public int read(InputStream inStream) throws IOException {
		return read(inStream, false);
	}

	/**
	 * reads header from stream
	 * 
	 */
	public int read(InputStream inStream, boolean artwork) throws IOException {
		reset();
		// read signature
		int currentCount;
		buf = new byte[4];
		currentCount = BasicIo.read(inStream, buf);
		if (currentCount < buf.length)
			throw new IOException("End of stream at reading header signature. " + currentCount);
		readCount += currentCount;
		signature = new String(buf, Controller.ISO_8859_1);
		readCount += BasicIo.read(inStream, buf);
		size = BasicIo.s2n(buf, 0, 4, false, true);
		readSpecific(inStream, artwork);
		// skip by the end of the header
		// readCount += inStream.skip(size-readCount);
		return readCount;
	}

	public String getSignature() {
		return signature;
	}

	protected void readSpecific(InputStream inStream, boolean artwork) throws IOException {
		// TODO Album Artist, Sort Name, Artist, Album, Composer, Show, Album
		// Artist
		// Remember playback position
		// Skip when Shuffling
		// Part of a gapless album
		int currentCount;
		if (size < 8)
			throw new IOException("Header size is too small (" + size + ") " + toString());
		if (size > 1024 * 1024)
			throw new IOException("Header size is too big (" + size + ") " + toString());
		buf = new byte[size - 8];
		currentCount = BasicIo.read(inStream, buf);
		if (currentCount < buf.length)
			throw new IOException("End of stream at reading header body. " + currentCount);
		readCount += currentCount;
		// TODO: use approach "mhbdmhsdmhltmhlpmhypmhipmhitmhod
		if ("mhbd".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 20 - 8, 4, false, true);
			System.err.printf("iTunesDB version %d.%d%n", BasicIo.s2n(buf, 12 - 8, 4, false, true), BasicIo.s2n(buf,
					16 - 8, 4, false, true));
			System.err.printf("DB signature 0x%x/0x%x-%d%n", BasicIo.s2n(buf, 24 - 8, 4, false, true), BasicIo.s2n(buf,
					28 - 8, 4, false, true), BasicIo.s2n(buf, 32 - 8, 1, false, true));
		} else if ("mhsd".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			int type = BasicIo.s2n(buf, 12 - 8, 4, false, true);
			System.err.printf("==>MHSD type: %d%n", type);
			switch(type) {
			case 1:
				thingsList = true;
				break;
			case 2:
				playList = true;
				break;
			case 3:
			case 4:
				filesList = true;
				break;
			case 5:
				albumList = true;
				break;
			case 8:
				artists =  true;
				break;
			case 9:
				genius_cuid = true;
				break;
			case 10:
				mhsd_10 = true;
				break;
				default:
					System.err.printf("Unrecognized MHSD type %d%n", type);
			}
				
		} else if ("mhlt".equals(signature)) {
			num = BasicIo.s2n(buf, 8 - 8, 4, false, true);
		} else if ("mhlp".equals(signature)) {
			num = BasicIo.s2n(buf, 8 - 8, 4, false, true);
		} else if ("mhyp".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true);
			numThings = BasicIo.s2n(buf, 16 - 8, 4, false, true);
			int type = BasicIo.s2n(buf, 20 - 8, 4, false, true);
			visible = type == 0;
			main = type == 1;
			lastDate = toDate(BasicIo.s2n(buf, 24 - 8, 4, false, true));
			hash1 = BasicIo.s2n(buf, 28 - 8, 4, false, true);
			hash2 = BasicIo.s2n(buf, 32 - 8, 4, false, true);
			index = BasicIo.s2n(buf, 40 - 8, 4, false, true);
		} else if ("mhip".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true);
			reference11 = BasicIo.s2n(buf, 20 - 8, 4, false, true);
			reference = BasicIo.s2n(buf, 24 - 8, 4, false, true);
		} else if ("mhit".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true);
			index = BasicIo.s2n(buf, 16 - 8, 4, false, true);
			vbr = BasicIo.s2n(buf, 28 - 8, 2, false, true);
			compilation = BasicIo.s2n(buf, 30 - 8, 1, false, true) == 1;
			rating = BasicIo.s2n(buf, 31 - 8, 1, false, true) / RATING_FACTOR;
			modifiedDate = toDate(BasicIo.s2n(buf, 32 - 8, 4, false, true));
			fileSize = BasicIo.s2n(buf, 36 - 8, 4, false, true);
			length = BasicIo.s2n(buf, 40 - 8, 4, false, true);
			order = BasicIo.s2n(buf, 44 - 8, 4, false, true);
			tracks = BasicIo.s2n(buf, 48 - 8, 4, false, true);
			year = BasicIo.s2n(buf, 52 - 8, 4, false, true);
			bitRate = BasicIo.s2n(buf, 56 - 8, 4, false, true);
			encoding = BasicIo.s2n(buf, 62 - 8, 2, false, true); // sample
			// rate
			volume = BasicIo.s2n(buf, 64 - 8, 4, true, true);
			start = BasicIo.s2n(buf, 68 - 8, 4, false, true);
			stop = BasicIo.s2n(buf, 72 - 8, 4, false, true);
			numPlayed = BasicIo.s2n(buf, 80 - 8, 4, false, true);
			// 84
			lastDate = toDate(BasicIo.s2n(buf, 88 - 8, 4, false, true));
			disk = BasicIo.s2n(buf, 92 - 8, 4, false, true);
			num_disks = BasicIo.s2n(buf, 96 - 8, 4, false, true);
			userId = BasicIo.s2n(buf, 100 - 8, 4, false, true);
			createDate = toDate(BasicIo.s2n(buf, 104 - 8, 4, false, true)); // last
			// modified
			hash1 = BasicIo.s2n(buf, 112 - 8, 4, false, true);
			hash2 = BasicIo.s2n(buf, 116 - 8, 4, false, true);
			checked = BasicIo.s2n(buf, 120 - 8, 1, false, true) == 1;
			prevRating = BasicIo.s2n(buf, 121 - 8, 1, false, true) / RATING_FACTOR;
			BPM = BasicIo.s2n(buf, 122 - 8, 2, false, true);
			numThings = BasicIo.s2n(buf, 124 - 8, 2, false, true); // artwork num
			artworkSize = BasicIo.s2n(buf, 128 - 8, 4, false, true); // artwork size
			mediaFormat = BasicIo.s2n(buf, 144 - 8, 2, false, true);
			// 3c Apple gapless
			// 33 AAC
			// c MP3
			// 29 audible
			// 16 MPEG-2 L3
			// 20 MPEG 2.5 L3
			// 0 WAV
//			System.err.printf("Format code: 0x%x%n", );
            numSkipped = BasicIo.s2n(buf, 156 - 8, 4, false, true);
            lastSkipped = toDate(BasicIo.s2n(buf, 160 - 8, 4, false, true));
            BasicIo.s2n(buf, 164 - 8, 1, false, true); // has artwork
			skip_shuffling = BasicIo.s2n(buf, 165 - 8, 1, false, true) == 1;
			remember_pos = BasicIo.s2n(buf, 166 - 8, 1, false, true) == 1;
			movie = BasicIo.s2n(buf, 177 - 8, 1, false, true) == 1;	
			displaytype = toDisplayType(BasicIo.s2n(buf, 208 - 8, 4, false, true));
			season = BasicIo.s2n(buf, 212 - 8, 4, false, true);
			epizode = BasicIo.s2n(buf, 216 - 8, 4, false, true);
			gapless_data = BasicIo.s2n(buf, 248 - 8, 4, false, true);
			gapless_track = BasicIo.s2n(buf, 256 - 8, 1, false, true) == 1;
			gapless_album = BasicIo.s2n(buf, 258 - 8, 1, false, true) == 1;
			// 136 - (00 44 2C 47)
			// 144 - (0c)
		} else if ("mhod".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			index = BasicIo.s2n(buf, 12 - 8, 2, false, true);
			buf = new byte[totalSize - size];
			currentCount = BasicIo.read(inStream, buf);
			if (currentCount < buf.length)
				throw new IOException("End of stream at reading mhod. " + currentCount);
			readCount += currentCount;
			if (artwork) {
				type = ArtworkDB.getMHODTypeName(index);
				if (index == IMAGETHUMBNAIL_CONTAINER || index == IMAGE_CONTAINER) {
					if (debugaw) {
						System.err.printf("artwork mhod file header index 2:\n");
						HexDump.dumpBuffer(System.err, buf, 0, buf.length, 16);
					}
					// TODO read mhni separately
					InputStream innerStream = new ByteArrayInputStream(buf);
					BaseHeader innerHeader = new BaseHeader();
					innerHeader.read(innerStream);
					if (debugaw)
						System.err.printf("inner hdr %s%n", innerHeader);
					if ("mhni".equals(innerHeader.signature) == false)
						throw new IOException("Header mhni expected in mhod type 2, found " + innerHeader.signature);
					hash1 = innerHeader.hash1; // width
					hash2 = innerHeader.hash2; // height
					reference = innerHeader.reference; // offset in file
					reference11 = innerHeader.reference11; // Fnnn - nnn
					fileSize = innerHeader.fileSize;
					vp = innerHeader.vp;
					hp = innerHeader.hp;
					for (int ihi = 0; ihi < innerHeader.num; ihi++) {
						innerHeader.read(innerStream, artwork);
						if (debugaw)
							System.err.printf("inner hdr %s%n", innerHeader);
						if ("mhod".equals(innerHeader.signature) == false)
							throw new IOException("Header mhod expected as child of mhni, found "
									+ innerHeader.signature);
						data = innerHeader.data;
					}
				} else if (index == IMAGEFILENAME_STRING) {
					if (debugaw) {
						System.err.printf("artwork mhod file header index 3:\n");
						HexDump.dumpBuffer(System.err, buf, 0, buf.length, 16);
					}
					length = BasicIo.s2n(buf, 0, 4, false, true);
					reference11 = BasicIo.s2n(buf, 4, 4, false, true); // version
					if (length > 0)
						try {
							data = new String(buf, 12, length, "UTF-16LE");
						} catch (UnsupportedEncodingException uee) {
							uee.printStackTrace();
						}
				} else if (index == IMAGEALBUMNAME_STRING) {
					length = BasicIo.s2n(buf, 0, 4, false, true);
					int encoding = BasicIo.s2n(buf, 4, 4, false, true);
					// 0 - ascii, 1 - utf-8, 2 - unicode
					//System.err.printf("Encoding %d%n", encoding);
					//HexDump.dumpBuffer(System.err, buf, 0, buf.length, 16);
					if (length > 0)
						try {
							data = new String(buf, 12, length, encoding <2?"UTF-8":"UTF-16LE");
						} catch (UnsupportedEncodingException uee) {
							uee.printStackTrace();
						}
				} else {
					type = "Artwork index " + index;
					//if (debugaw)
						System.err.printf("Read artwork mhod %s%n", type);
					HexDump.dumpBuffer(System.err, buf, 0, buf.length, 16);
				}
			} else {
				// TODO: PlayItem shouldn't be used here
				if (index >= PlayItem.TITLE && index <= PlayItem.SORT_SHOW_TYPE) {
					// read body
					type = PlayItem.MHOD_TYPE[index];
					num = BasicIo.s2n(buf, 0, 4, false, true);
					int l = BasicIo.s2n(buf, 4, 4, false, true);
					if (l > 0)
						try {
							data = new String(buf, 16, l, "UTF-16LE");
						} catch (UnsupportedEncodingException uee) {
							uee.printStackTrace();
						}
				} else if (index == PLAYLISTENTRY) {
					reference = BasicIo.s2n(buf, 0, 4, false, true); // item
					// number
					if (debug)
						HexDump.dumpBuffer(System.err, buf, 0, buf.length, 0);
				} else if (index == LISTATTRIBUTE) {
					reference = BasicIo.s2n(buf, 0, 4, false, true);
					numThings = BasicIo.s2n(buf, 4, 4, false, true);
					// List list = new ArrayList(numThings);
					int[] list = new int[numThings];
					for (int i = 0; i < numThings; i++)
						list[i] = BasicIo.s2n(buf, 48 + i * 4, 4, false, true);
					complexData = list;
					// if (debug)
					// rogatkin.util.Data.dumpBuffer(System.err, buf, 0,
					// buf.length, 0);
				} else if (index == SMARTLISTDEF) {
					Smart smart = new Smart();
					smart.read(buf);
					complexData = smart;
				} else if (index == SMARTLISTENTRY) {
					Rules rules = new Rules();
					rules.read(buf);
					complexData = rules;
					// System.err.println("Rules: "+rules);

				} else {
					type = "Index " + index;
					System.err.println("Read mhod " + type);
					HexDump.dumpBuffer(System.err, buf, 0, buf.length, 16);
				}
			}
		} else if ("mhdp".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true); // size of
			// element
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true);
		} else if ("mhfd".equals(signature)) {
			// processing photo headers
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true); // size of
			// element
			num = BasicIo.s2n(buf, 20 - 8, 4, false, true);
		} else if ("mhli".equals(signature)) {
			num = BasicIo.s2n(buf, 8 - 8, 4, false, true);
		} else if ("mhii".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true);
			reference11 = BasicIo.s2n(buf, 16 - 8, 4, false, true);
			hash1 = BasicIo.s2n(buf, 20 - 8, 4, false, true);
			hash2 = BasicIo.s2n(buf, 24 - 8, 4, false, true);
			rating = BasicIo.s2n(buf, 32 - 8, 4, false, true);
			createDate = toDate(BasicIo.s2n(buf, 40 - 8, 4, false, true));
			lastDate = toDate(BasicIo.s2n(buf, 44 - 8, 4, false, true));
			fileSize = BasicIo.s2n(buf, 48 - 8, 4, false, true); // 44??
		} else if ("mhni".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true);
			reference11 = BasicIo.s2n(buf, 16 - 8, 4, false, true); // part fn
			reference = BasicIo.s2n(buf, 20 - 8, 4, false, true); // offset
			fileSize = BasicIo.s2n(buf, 24 - 8, 4, false, true);
			// TODO consider a mask & 0xffff
			vp = (short)BasicIo.s2n(buf, 28 - 8, 2, false, true); // vert padding
			hp = (short)BasicIo.s2n(buf, 30 - 8, 2, false, true); // horz padding
			hash2 = BasicIo.s2n(buf, 32 - 8, 2, false, true); // height
			hash1 = BasicIo.s2n(buf, 34 - 8, 2, false, true); // width
			//fileSize = BasicIo.s2n(buf, 40 - 8, 2, false, true);
		} else if ("mhla".equals(signature)) {
			num = BasicIo.s2n(buf, 8 - 8, 4, false, true);
		} else if ("mhlf".equals(signature)) {
			num = BasicIo.s2n(buf, 8 - 8, 4, false, true);
		} else if ("mhif".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			reference11 = BasicIo.s2n(buf, 16 - 8, 4, false, true);
			fileSize = BasicIo.s2n(buf, 20 - 8, 4, false, true);
		} else if ("mhba".equals(signature)) {
			totalSize = BasicIo.s2n(buf, 8 - 8, 4, false, true);
			num = BasicIo.s2n(buf, 12 - 8, 4, false, true); // num mhod
			numThings = BasicIo.s2n(buf, 16 - 8, 4, false, true); // num mhia
			reference11 = BasicIo.s2n(buf, 20 - 8, 4, false, true); // playlist id
			albumType = BasicIo.s2n(buf, 30 - 8, 1, false, true); // 1- master, 2- photo
			playMusic = BasicIo.s2n(buf, 31 - 8, 1, false, true) == 1;
			repeat = BasicIo.s2n(buf, 32 - 8, 1, false, true) == 1;
			random = BasicIo.s2n(buf, 33 - 8, 1, false, true) == 1;
			showTitles = BasicIo.s2n(buf, 34 - 8, 1, false, true) == 1;
			transitionDirection = BasicIo.s2n(buf, 35 - 8, 1, false, true);
			slideDuration = BasicIo.s2n(buf, 36 - 8, 4, false, true);
			transitionDuration = BasicIo.s2n(buf, 40 - 8, 4, false, true);
			hash1 = BasicIo.s2n(buf, 52 - 8, 4, false, true);
			hash2 = BasicIo.s2n(buf, 56 - 8, 4, false, true);
			reference = BasicIo.s2n(buf, 60 - 8, 4, false, true);
		} else if ("mhia".equals(signature)) {
			reference11 = BasicIo.s2n(buf, 16 - 8, 4, false, true); // image id
			//HexDump.dumpBuffer(System.err, buf, 0, buf.length, 20);
		} else
			System.err.println("Unsupported header:"+signature);
		buf = null; 
		if (debug)
			System.err.println(toString());
	}

	public int read(FileChannel inChannel) throws IOException {
		return 0;
	}

	public int write(OutputStream outStream) throws IOException {
		return write(outStream, false);
	}

	public int write(OutputStream outStream, boolean artwork) throws IOException {
		buf = new byte[size];
		int result = 0;
		// TODO: consider optimization, like a switch with int cases
		char[] ss = signature.toCharArray();
		for (int i = 0; i < ss.length; i++)
			buf[i] = (byte) (ss[i] & 255);
		BasicIo.in2s(buf, 4, size, 4);
		if ("mhbd".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, 1, 4); // version major 1
			BasicIo.in2s(buf, 16, 28, 4); // version minor 28
			BasicIo.in2s(buf, 20, num, 4);
			// 8A F0 19 16 C4 68 03 E3
			// DB id???
			BasicIo.in2s(buf, 24, 0x1619F08A, 4);
			BasicIo.in2s(buf, 28, 0xE30368C4, 4);
			BasicIo.in2s(buf, 32, 2, 1);
		} else if ("mhsd".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			if (playList)
				BasicIo.in2s(buf, 12, 2, 4);
			else if (thingsList)
				BasicIo.in2s(buf, 12, 1, 4);
			else if (filesList)
				BasicIo.in2s(buf, 12, 3, 4);
		} else if ("mhlt".equals(signature)) {
			BasicIo.in2s(buf, 8, num, 4);
		} else if ("mhit".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, num, 4);
			BasicIo.in2s(buf, 16, index, 4);
			BasicIo.in2s(buf, 20, 1, 1);
			BasicIo.in2s(buf, 28, vbr, 2);
			BasicIo.in2s(buf, 30, compilation ? 1 : 0, 1);
			BasicIo.in2s(buf, 31, rating * RATING_FACTOR, 1);
			BasicIo.in2s(buf, 32, fromDate(modifiedDate), 4);
			BasicIo.in2s(buf, 36, fileSize, 4);
			BasicIo.in2s(buf, 40, length, 4);
			BasicIo.in2s(buf, 44, order, 4);
			BasicIo.in2s(buf, 48, tracks, 4);
			BasicIo.in2s(buf, 52, year, 4);
			BasicIo.in2s(buf, 56, bitRate, 4);
			BasicIo.in2s(buf, 62, encoding, 2);
			BasicIo.in2s(buf, 64, volume, 4);
			BasicIo.in2s(buf, 68, start, 4);
			BasicIo.in2s(buf, 72, stop, 4);
			BasicIo.in2s(buf, 80, numPlayed, 4);
			BasicIo.in2s(buf, 84, numPlayed, 4);
			BasicIo.in2s(buf, 88, fromDate(lastDate), 4);
			BasicIo.in2s(buf, 92, disk, 4);
			BasicIo.in2s(buf, 96, num_disks, 4);
			BasicIo.in2s(buf, 100, userId, 4);
			BasicIo.in2s(buf, 104, fromDate(createDate), 4); // last modified
			BasicIo.in2s(buf, 108, 0, 4); // bookmark in millis
			BasicIo.in2s(buf, 112, hash1, 4);
			BasicIo.in2s(buf, 116, hash2, 4);
			BasicIo.in2s(buf, 120, checked ? 1 : 0, 1); // checked
			BasicIo.in2s(buf, 121, prevRating, 1); // app rating
			BasicIo.in2s(buf, 122, BPM, 2);
			BasicIo.in2s(buf, 124, numThings, 2); // num artworks
			BasicIo.in2s(buf, 126, 0xFFFF, 2); // 0 - for uncompressed, 1 - for
			BasicIo.in2s(buf, 128, artworkSize, 4); // artwork JPEG size
			// audible
			BasicIo.in2s(buf, 144, mediaFormat, 2);
			BasicIo.in2s(buf, 156, numSkipped, 4);
			BasicIo.in2s(buf, 160, fromDate(lastSkipped), 4);
			BasicIo.in2s(buf, 164, numThings>0?1:2, 1); // has artwork
			BasicIo.in2s(buf, 165, skip_shuffling ? 1 : 0, 1);
			BasicIo.in2s(buf, 166, remember_pos ? 1 : 0, 1);
			BasicIo.in2s(buf, 177, movie ? 1 : 0, 1);
			BasicIo.in2s(buf, 212, season, 4);
			BasicIo.in2s(buf, 216, epizode, 4);
			BasicIo.in2s(buf, 208, fromDisplayType(displaytype), 4);
			BasicIo.in2s(buf, 248, gapless_data, 4);
			BasicIo.in2s(buf, 256, gapless_track ? 1 : 0, 1);
			BasicIo.in2s(buf, 258, gapless_album ? 1 : 0, 1);
		} else if ("mhod".equals(signature)) {
			// TODO: total size can be calculated here
			// so extra checking can be eliminated after
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, index, 4);
			if (artwork) {
				if (index == 2) {

				} else if (index == 3) {
					outStream.write(buf);
					result += buf.length;
					byte[] sb = data == null ? new byte[0] : data.getBytes("UTF-16LE");
					buf = new byte[12];
					if (sb.length > 0)
						BasicIo.in2s(buf, 0, sb.length, 4);
					BasicIo.in2s(buf, 4, 2, 4); // reference11
					if (totalSize - size < sb.length + buf.length)
						throw new IOException("Total size incorrect " + totalSize + " -12 -24 < " + sb.length);
					outStream.write(buf);
					result += buf.length;
					buf = sb;
				} else
					System.err.printf("Usupported type %d of photo mhod\n", index);
			} else {
				outStream.write(buf);
				result += buf.length;
				if (index == PLAYLISTENTRY) {
					buf = new byte[totalSize - size];
					BasicIo.in2s(buf, 0, reference, 4);
					if (totalSize == 648) {
						BasicIo.in2s(buf, 16, 0x010084, 4);
						BasicIo.in2s(buf, 20, 0x05, 1);
						BasicIo.in2s(buf, 24, 0x09, 1);
						BasicIo.in2s(buf, 28, 0x03, 4);
						BasicIo.in2s(buf, 32, 0x010012, 4);
						BasicIo.in2s(buf, 48, 0xc80002, 4);
						BasicIo.in2s(buf, 64, 0x3b000d, 4);
						BasicIo.in2s(buf, 80, 0x7d0004, 4);
						BasicIo.in2s(buf, 96, 0x7d0003, 4);
						BasicIo.in2s(buf, 112, 0x500008, 4);
						BasicIo.in2s(buf, 128, 0x4f0017, 4);
						BasicIo.in2s(buf, 132, 0x1, 1);
						BasicIo.in2s(buf, 144, 0x500014, 4);
						BasicIo.in2s(buf, 148, 0x1, 1);
						BasicIo.in2s(buf, 160, 0x7d0015, 4);
						BasicIo.in2s(buf, 164, 0x1, 1);
						BasicIo.in2s(buf, 544, 0x8c, 1);
					}
				} else if (index == LISTATTRIBUTE) {
					int[] list = (int[]) complexData;
					if (list == null)
						throw new IOException("Writing a header of type " + LISTATTRIBUTE
								+ " requested, but list is null.");
					if (totalSize - size < 48 + list.length * 4)
						throw new IOException("Invalid total size " + totalSize + " when needed " + 48 + list.length
								* 4);
					buf = new byte[totalSize - size];
					BasicIo.in2s(buf, 0, reference, 4);
					if (numThings != list.length) {
						System.err.println/* throw new IOExceptions */("Data inconsistency, required " + numThings
								+ ", but actual " + list.length);
						numThings = list.length;
					}
					BasicIo.in2s(buf, 4, numThings, 4);

					for (int i = 0; i < list.length; i++)
						BasicIo.in2s(buf, 48 + i * 4, list[i], 4);
				} else if (index == SMARTLISTDEF) {
					Smart smart = (Smart) complexData;
					if (smart == null) // for debug only
						throw new IOException("Writing a header of type " + index
								+ " requested, but smart def is null.");
					buf = new byte[totalSize - size];
					if (buf.length < smart.size())
						throw new IOException("Required " + smart.size() + ", but allocated " + buf.length);
					smart.write(buf, 0);
				} else if (index == SMARTLISTENTRY) {
					Rules rules = (Rules) complexData;
					if (rules == null) // for debug only
						throw new IOException("Writing a header of type " + index + " requested, but rules are null.");
					buf = new byte[totalSize - size];
					if (buf.length < rules.size())
						throw new IOException("Required " + rules.size() + ", but allocated " + buf.length);
					rules.write(buf, 0);
				} else { // TODO: add if (index == PlayItem.ALBUM || index ==
					// PlayItem.ARTIST || ..
					byte[] sb = data == null ? new byte[0] : data.getBytes("UTF-16LE");
					buf = new byte[16];
					BasicIo.in2s(buf, 0, 1, 4);
					if (sb.length > 0)
						BasicIo.in2s(buf, 4, sb.length, 4);
					if (totalSize - size < sb.length + buf.length)
						throw new IOException("Total size incorrect " + totalSize + " -16 -24 < " + sb.length);
					outStream.write(buf);
					result += buf.length;
					buf = sb;
				}
			}
		} else if ("mhlp".equals(signature)) {
			BasicIo.in2s(buf, 8, num, 4);
		} else if ("mhyp".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, num, 4);
			BasicIo.in2s(buf, 16, numThings, 4);
			if (main)
				BasicIo.in2s(buf, 20, 1, 1);
			BasicIo.in2s(buf, 24, fromDate(lastDate), 4);
			BasicIo.in2s(buf, 28, hash1, 4);
			BasicIo.in2s(buf, 32, hash2, 4);
			BasicIo.in2s(buf, 40, index, 4);
		} else if ("mhip".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, num, 1);
			// BasicIo.in2s(buf, 16, 0, 4);
			BasicIo.in2s(buf, 24, reference, 4);
			// BasicIo.in2s(buf, 20, reference11, 4);
			BasicIo.in2s(buf, 28, 0xbafe18e8, 4);
		} else if ("mhdp".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, num, 4);
		} else if ("mhfd".equals(signature)) {
			// processing photo headers
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 16, 2, 4);
			BasicIo.in2s(buf, 20, num, 4);
			BasicIo.in2s(buf, 28, 0x64 + numThings, 4);
			BasicIo.in2s(buf, 0x30, 2, 4); // ??
		} else if ("mhli".equals(signature)) {
			BasicIo.in2s(buf, 8, num, 4);
		} else if ("mhii".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, num, 4);
			BasicIo.in2s(buf, 16, reference11, 4);
			BasicIo.in2s(buf, 20, hash1, 4);
			BasicIo.in2s(buf, 24, hash2, 4);
			BasicIo.in2s(buf, 48, fileSize, 4);
		} else if ("mhni".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 12, num, 4);
			BasicIo.in2s(buf, 16, reference11, 4);
			BasicIo.in2s(buf, 20, reference, 4);
			BasicIo.in2s(buf, 24, fileSize, 4);
			BasicIo.in2s(buf, 28, vp, 2);
			BasicIo.in2s(buf, 30, hp, 2);
			BasicIo.in2s(buf, 32, hash2, 2);
			BasicIo.in2s(buf, 34, hash1, 2);
			BasicIo.in2s(buf, 40, fileSize, 4);
		} else if ("mhla".equals(signature)) {
			BasicIo.in2s(buf, 8, num, 4);
		} else if ("mhlf".equals(signature)) {
			BasicIo.in2s(buf, 8, num, 4);
		} else if ("mhif".equals(signature)) {
			BasicIo.in2s(buf, 8, totalSize, 4);
			BasicIo.in2s(buf, 16, reference11, 4);
			BasicIo.in2s(buf, 20, fileSize, 4);
		} else if ("mhba".equals(signature)) {
		} else if ("mhia".equals(signature)) {
		}
		// if (buf != null) {
		outStream.write(buf);
		result += buf.length;
		// }
		return result;
	}

	public int skip(InputStream inStream) throws IOException {
		if (totalSize < readCount)
			throw new IOException("Can't skip beyond of the header " + totalSize + " read " + readCount);
		if (debug)
			System.err.println("Skipping all inner headers for " + signature);
		return (int) BasicIo.skip(inStream, totalSize - readCount);
	}

	@Override
	public String toString() {
		return "Header " + signature + " size " + size + ", size with body " + totalSize + " numThings " + numThings
				+ ", num " + num + "\n reference " + reference + ", reference11 " + reference11 + ", index " + index
				+ "\n createDate " + createDate + ", lastDate " + lastDate + ", modifiedDate " + modifiedDate
				+ "\n vbr " + vbr + ", fileSize " + fileSize + ", length " + length + ", order " + order + " of "
				+ tracks + ",\n encoding " + encoding + ", bitRate " + bitRate + ", volume " + volume + ", BPM " + BPM
				+ "\n, start " + start + ", stop " + stop + ", numPlayed " + numPlayed + ", num skipped " + numSkipped
				+ ", playList " + playList + " filesList " + filesList + ", albumList: " + albumList + "\n thingsList "
				+ thingsList + ", visible " + visible + ", main " + main + ", compilation " + compilation 
				+ "\n skip_shuffling = " + skip_shuffling + ", remember_pos: " + remember_pos
				+ ", gapless_data: " + gapless_data + ", gapless_track: " + gapless_track + ", gapless_album: "
				+ gapless_album + ",\n type " + type + " hash1 0x" + Integer.toHexString(hash1) + " hash2 0x"
				+ Integer.toHexString(hash2)+(data != null?"\ndata:"+data:"")
				+ "\nPhoto : albumType="+albumType+", transitionDirection = "+transitionDirection+ ", slideDuration = "+slideDuration+",  transitionDuration = "+transitionDuration
		+ "\nplayMusic  = "+playMusic+", repeat = "+repeat+ "; random  = "+ random+"; showTitles ="+ showTitles;
				}

	public static Date toDate(int timestamp) {
		if (timestamp != 0) {
			return new Date((timestamp - javaMacDateDelta) * 1000l - TimeZone.getDefault().getRawOffset());
		}
		return null;
	}

	public static int fromDate(Date timestamp) {
		if (timestamp != null) {
			return (int) ((timestamp.getTime() + TimeZone.getDefault().getRawOffset()) / 1000l + javaMacDateDelta);
		}
		return 0;
	}

	public static int fromDisplayType(DisplayType dt) {
		switch (dt) {
		case Audio:
			return 1;
		case AudioVideo:
			return 0;
		case Video:
			return 2;
		case Podcast:
			return 4;
		case VideoPodcast:
			return 6;
		case Audiobook:
			return 8;
		case MusicVideo:
			return 0x20;
		case TVShow:
			return 0x40;
		case MusicTVShow:
			return 0x80;
		}
		return 0;
	}

	public static DisplayType toDisplayType(int dt) {
		switch (dt) {
		case 0:
			return DisplayType.AudioVideo;
		case 1:
			return DisplayType.Audio;
		case 2:
			return DisplayType.Video;
		case 4:
			return DisplayType.Podcast;
		case 6:
			return DisplayType.VideoPodcast;
		case 8:
			return DisplayType.Audiobook;
		case 0x20:
			return DisplayType.MusicVideo;
		case 0x40:
			return DisplayType.TVShow;
		case 0x80:
			return DisplayType.MusicTVShow;
		}
		return DisplayType.AudioVideo;
	}

	public void makeIt(int hdrIndex) {
		reset();
		HeaderInfo i = getHeaderInfo(hdrIndex);
		signature = i.signature;
		size = i.size;
		totalSize = size;
	}

	public static HeaderInfo getHeaderInfo(int hdrIndex) {
		return HEADER_INFOS[hdrIndex];
	}

	static class HeaderInfo {
		int size;

		String signature;

		public HeaderInfo(String signature, int size) {
			this.signature = signature;
			this.size = size;
		}
	}

	static class HeaderStat {
		int count, skipCount;

		int lastPlay;

		int rating;

		Date lastPlayDate, lastSkipStamp;

		int size;

		public HeaderStat(int size) {
			this.size = size;
		}

		public int read(InputStream inStream) throws IOException {
			byte[] buf = new byte[size];
			BasicIo.read(inStream, buf);
			count = BasicIo.s2n(buf, 0, 4, false, true);
			lastPlay = BasicIo.s2n(buf, 4, 4, false, true);
			lastPlayDate = toDate(lastPlay);
			if (size >= 16)
				rating = BasicIo.s2n(buf, 12, 4, false, true) / 0x14;
			if (size >= 24)
				skipCount = BasicIo.s2n(buf, 20, 4, false, true);
			if (size >= 32)
				lastSkipStamp = toDate(BasicIo.s2n(buf, 24, 4, false, true));
			return size;
		}

		public int write(OutputStream outStream) throws IOException {
			byte[] buf = new byte[size];
			BasicIo.in2s(buf, 0, count, 4);
			if (lastPlayDate != null)
				lastPlay = (int) (lastPlayDate.getTime() / 1000) + javaMacDateDelta;
			BasicIo.in2s(buf, 4, lastPlay, 4);
			if (size == 16)
				BasicIo.in2s(buf, 12, rating * 0x14, 4);
			outStream.write(buf);
			return size;
		}
	}

	// static class HeaderSort { //52
	// }

	public static class Smart implements Serializable {
		public static final int SONGNAME = 2;

		public static final int ALBUM = 3;

		public static final int ARTIST = 4;

		public static final int BITRATE = 5;

		public static final int SAMPLING_RATE = 6;

		public static final int YEAR = 7;

		public static final int GENRE = 8;

		public static final int GROUPING = 39;

		public static final int KIND = 9;

		public static final int LAST_MODIFY = 10;

		public static final int TRACKNUMBER = 11;

		public static final int SIZE = 12;

		public static final int TIME = 13;

		public static final int COMMENT = 14;

		public static final int TIME_ADDED = 16;

		public static final int COMPOSER = 18;

		public static final int PLAYCOUNT = 22;

		public static final int LAST_PLAYED = 23;

		public static final int RATING = 25;

		public static final int COMPILATION = 31;

		public static final int BPM = 35;

		public static final int DISC_NUM = 0x18;

		public static final int PLAYLIST = 0x28;

		public static final int CATEGORY = 0x37;

		public static final int LAST_SKIPPED = 0x45;

		public static final int PODCAST = 0x39;

		public static final int SEASON = 0x3f;

		public static final int SHOW = 0x3e;

		public static final int SKIP_COUNT = 0x44;

		public static final int SORT_ALBUM = 79;

		public static final int SORT_ALBUM_ARTIST = 81;

		public static final int SORT_ARTIST = 0x50;

		public static final int SORT_COMPOSER = 82;

		public static final int SORT_SHOW = 83;

		public static final int SORT_NAME = 78;

		public static final int VIDEO_KIND = 0x3c;

		public static final int ALBUM_ARTIST = 0x47;

		public static final int DESCRIPTION = 0x36;

		public static final int ALBUM_RATING = 90;

		public static final int MODIF_NOT = 0x2000000;

		// condition code
		public static final int DATE_COND_IS = 0x100;

		public static final int DATE_COND_IS_NOT = DATE_COND_IS | MODIF_NOT;

		public static final int DATE_COND_AFTER = 0x10;

		public static final int DATE_COND_BEFORE = 0x40;

		public static final int DATE_COND_LAST = 0x200;

		public static final int DATE_COND_NOT_LAST = DATE_COND_LAST | MODIF_NOT;

		public static final int DATE_COND_IN_RANGE = 256;

		public static final int STR_COND_CONTAINS = 0x1000002;

		public static final int STR_COND_NOT_CONTAIN = 0x3000002;

		public static final int STR_COND_IS = 0x1000001;

		public static final int STR_COND_IS_NOT = 0x3000001;

		public static final int STR_COND_ST_WITH = 0x1000004;

		public static final int STR_COND_EN_WITH = 0x1000008;

		public static final int NUM_COND_IS = 1;

		public static final int NUM_COND_IS_NOT = NUM_COND_IS | MODIF_NOT;

		public static final int NUM_COND_GT = 16;

		public static final int NUM_COND_LT = 64;

		public static final int NUM_COND_IN_RANGE = 256;

		public static final int BOOL_COND_SET = 0x1;

		public static final int UNIT_MIN = 1;

		public static final int UNIT_MB = 2;

		public static final int UNIT_SONG = 3;

		public static final int UNIT_HOUR = 4;

		public static final int UNIT_GB = 5;

		public static final int SELBY_RANDOM = 2;

		public static final int SELBY_ARTIST = 5;

		public static final int SELBY_ALBUM = 4;

		public static final int SELBY_GENRE = 7;

		public static final int SELBY_TITLE = 3;

		public static final int SELBY_YEAR = 9;

		public static final int SELBY_RATING = 23;

		public static final int SELBY_LASTPLAY = 21;

		public static final int SELBY_ADDED = 16;

		public static final int SELBY_PLAYCOUNT = 20;

		public static final int SELBY_MOST_FLAG = 0x1000;

		public static int DAY_SEC = 60 * 60 * 24;

		public static int WEEK_SEC = 60 * 60 * 24 * 7;

		public static int MONTH_SEC = 2628000;

		public static final Descriptor[] LIMIT_UNITS = { new Descriptor(Resources.LIST_MINUTES, UNIT_MIN),
				new Descriptor(Resources.LIST_MB, UNIT_MB), new Descriptor(Resources.LIST_SONGS, UNIT_SONG),
				new Descriptor(Resources.LIST_HOURS, UNIT_HOUR), new Descriptor(Resources.LIST_GB, UNIT_GB) };

		public static final Descriptor[] SELECTIONS = { new Descriptor(Resources.LIST_SEL_RANDOM, SELBY_RANDOM),
				new Descriptor(Resources.LIST_ARTIST, SELBY_ARTIST), new Descriptor(Resources.LIST_ALBUM, SELBY_ALBUM),
				new Descriptor(Resources.LIST_GENRE, SELBY_GENRE),
				new Descriptor(Resources.LIST_SONG_NAME, SELBY_TITLE),
				new Descriptor(Resources.LIST_YEAR_FIRST, SELBY_YEAR),
				new Descriptor(Resources.LIST_YEAR_LAST, SELBY_YEAR | SELBY_MOST_FLAG),
				new Descriptor(Resources.LIST_SEL_HST_RAT, SELBY_RATING | SELBY_MOST_FLAG),
				new Descriptor(Resources.LIST_SEL_LWT_RAT, SELBY_RATING),
				new Descriptor(Resources.LIST_SEL_MST_RCT_PL, SELBY_LASTPLAY | SELBY_MOST_FLAG),
				new Descriptor(Resources.LIST_SEL_LST_RCT_PL, SELBY_LASTPLAY),
				new Descriptor(Resources.LIST_SEL_MST_OFT_PL, SELBY_PLAYCOUNT | SELBY_MOST_FLAG),
				new Descriptor(Resources.LIST_SEL_LST_OFT_PL, SELBY_PLAYCOUNT),
				new Descriptor(Resources.LIST_SEL_MST_RCT_ADD, SELBY_ADDED),
				new Descriptor(Resources.LIST_SEL_LST_RCT_ADD, SELBY_ADDED | SELBY_MOST_FLAG) };

		public static final Descriptor[] TYPE_STRING_RULE_ACTION = {
				new Descriptor(Resources.LIST_CONTAINS, STR_COND_CONTAINS),
				new Descriptor(Resources.LIST_NOT_CONTAIN, STR_COND_NOT_CONTAIN),
				new Descriptor(Resources.LIST_IS, STR_COND_IS), new Descriptor(Resources.LIST_IS_NOT, STR_COND_IS_NOT),
				new Descriptor(Resources.LIST_ST_WITH, STR_COND_ST_WITH),
				new Descriptor(Resources.LIST_EN_WITH, STR_COND_EN_WITH) };

		public static final Descriptor[] TYPE_LIST_RULE_ACTION = { new Descriptor(Resources.LIST_IS, NUM_COND_IS),
				new Descriptor(Resources.LIST_IS_NOT, NUM_COND_IS_NOT), };

		public static final Descriptor[] TYPE_NUM_RULE_ACTION = { // done
		new Descriptor(Resources.LIST_IS, NUM_COND_IS), new Descriptor(Resources.LIST_IS_NOT, NUM_COND_IS_NOT),
				new Descriptor(Resources.LIST_IS_GT, NUM_COND_GT), new Descriptor(Resources.LIST_IS_LS, NUM_COND_LT),
				new Descriptor(Resources.LIST_IS_IN_RANGE, NUM_COND_IN_RANGE) };

		public static final Descriptor[] TYPE_DATE_RULE_ACTION = { new Descriptor(Resources.LIST_IS, DATE_COND_IS),
				new Descriptor(Resources.LIST_IS_NOT, DATE_COND_IS_NOT),
				new Descriptor(Resources.LIST_IS_AFTER, DATE_COND_AFTER),
				new Descriptor(Resources.LIST_IS_BEFORE, DATE_COND_BEFORE),
				new Descriptor(Resources.LIST_IS_LAST, DATE_COND_LAST),
				new Descriptor(Resources.LIST_IS_NOT_LAST, DATE_COND_NOT_LAST),
				new Descriptor(Resources.LIST_IS_IN_RANGE, DATE_COND_IS) };

		public static final Descriptor[] TYPE_BOOL_RULE_ACTION = {
				new Descriptor(Resources.LIST_IS_SET, BOOL_COND_SET),
				new Descriptor(Resources.LIST_ISN_SET, BOOL_COND_SET | MODIF_NOT) };

		public static final int[] STRING_RULES = { SONGNAME, ALBUM, ARTIST, GENRE, KIND, ALBUM_ARTIST, COMMENT,
				COMPOSER, GROUPING, DESCRIPTION, SORT_ALBUM, SORT_ARTIST, SORT_ALBUM_ARTIST, SORT_NAME, SORT_SHOW,
				VIDEO_KIND

		};

		public static final PlayList MUSIC_PL = new PlayList("Music", -14846455, 186670469);

		public static final PlayList ABOOKS_PL = new PlayList("Audiobooks", -1436298077, 552671683);

		public static final PlayList MOVIES_PL = new PlayList("Movies", -300566090, -1725322510);

		public static final PlayList SHOWS_PL = new PlayList("TV Shows", -1942644246, -752264820);

		public static final PlayList[] RESERVED_PLS = { MUSIC_PL, ABOOKS_PL, MOVIES_PL, SHOWS_PL };

		public boolean live, checkRegExp, checkLimit, mos, chkdSong;

		public int item, sort, limit;

		public Rules rules;

		protected transient boolean modified = true;

		protected static boolean isType(int ruleType, int[] types) {
			for (int type : types)
				if (ruleType == type)
					return true;
			return false;
		}

		public void read(byte[] buf) { // throws IOException
			live = BasicIo.s2n(buf, 24 - 24, 1, false, true) == 1;
			checkRegExp = BasicIo.s2n(buf, 25 - 24, 1, false, true) == 1;
			checkLimit = BasicIo.s2n(buf, 26 - 24, 1, false, true) == 1;
			item = BasicIo.s2n(buf, 27 - 24, 1, false, true); // unit
			sort = BasicIo.s2n(buf, 28 - 24, 1, false, true);
			limit = BasicIo.s2n(buf, 32 - 24, 4, false, true);
			// if (item < 0 || item > LIMIT_UNITS.length -1)
			// item = 0;
			chkdSong = BasicIo.s2n(buf, 36 - 24, 1, false, true) == 1;
			mos = BasicIo.s2n(buf, 37 - 24, 1, false, true) == 1;
			if (mos)
				sort |= SELBY_MOST_FLAG;
			modified = false;
		}

		void write(byte[] buf, int offset) throws IOException {
			if (live)
				BasicIo.in2s(buf, offset + 24 - 24, 1, 1);
			if (checkRegExp)
				BasicIo.in2s(buf, offset + 25 - 24, 1, 1);
			if (checkLimit)
				BasicIo.in2s(buf, offset + 26 - 24, 1, 1);
			BasicIo.in2s(buf, offset + 27 - 24, item, 1);
			mos = (sort & SELBY_MOST_FLAG) != 0;
			BasicIo.in2s(buf, offset + 28 - 24, sort & 0xff, 1);
			BasicIo.in2s(buf, offset + 32 - 24, limit, 4);
			if (chkdSong)
				BasicIo.in2s(buf, offset + 36 - 24, 1, 1);
			if (mos)
				BasicIo.in2s(buf, offset + 37 - 24, 1, 1);
			modified = false;
		}

		public boolean isChanged() {
			return modified || rules != null && rules.isChanged();
		}

		public void touch() {
			modified = true;
		}

		public String toString() {
			return "SLst live " + live + " reg exp " + checkRegExp + " limit " + checkLimit + " mos " + mos
					+ "\n limit=" + limit + ", unit " + item + " sort " + sort + "\n rules " + rules;
		}

		public int size() {
			return 72;
		}

		/**
		 * checks if play item is matching to the smart list conditions.
		 * Generally, there is some optimization like looking only in particular
		 * place like album, artist or other sortings.
		 * 
		 * @param PlayList
		 *            to add a play item, can't be null
		 * @param PlayItem
		 *            play item to match and add
		 * @return true if added
		 */
		public boolean match(PlayList playList, PlayItem playItem, ITunesDB itunesdb) {
			if (checkLimit) { // check for it
				// TODO: limits can be cached pre-calculated
				// TODO: using an exception when limit reached to break loop
				switch (item) {
				case UNIT_MIN:
					if (((Integer) playItem.get(PlayItem.LENGTH)).intValue() + (int) playList.getLength() > limit * 60 * 1000)
						return false; // throw new LimitReachedException();
					break;
				case UNIT_MB:
					if (((Integer) playItem.get(PlayItem.SIZE)).intValue() + (int) playList.getSizeOf() > limit * 1024 * 1024)
						return false;
					break;
				case UNIT_SONG:
					if (playList.size() >= limit)
						return false;
					break;
				case UNIT_HOUR: // TODO: can be in one block with minutes
					if (((Integer) playItem.get(PlayItem.LENGTH)).intValue() + (int) playList.getLength() > limit * 60 * 1000 * 60)
						return false;
					break;
				case UNIT_GB: // can limit be just multiplied when falls
					if (((Integer) playItem.get(PlayItem.SIZE)).intValue() + playList.getSizeOf() > limit * 1024 * 1024 * 1024l)
						return false;
					break;
				}
			}

			if (checkRegExp) {
				Iterator it = rules.rules.iterator();
				while (it.hasNext()) {
					Rules.Rule r = (Rules.Rule) it.next();
					if (r.match(playList, playItem, itunesdb)) {
						if (debug)
							System.err.printf("Play item %s matched the rule %s%n", playItem, r);
						if (rules.any) {
							// playList.add(playItem);
							return true;
						}
					} else if (rules.any == false)
						return false;
				}
				if (rules.any == false) {
					// playList.add(playItem);
					return true;
				}
			} else {
				// playList.add(playItem);
				return true;
			}
			return false;
		}

		// protected void addPlayItem()
	}

	public static class Rules implements Serializable {
		static final long serialVersionUID = 11019;

		static final byte[] SLST = { 0x53, 0x4C, 0x73, 0x74 };

		public List<Rule> rules;

		public boolean any; // match any, all

		protected transient boolean modified = true;

		public void read(byte[] buf) {
			int num = BasicIo.s2n(buf, 11, 1, false, true);
			any = BasicIo.s2n(buf, 15, 1, false, true) == 1;
			rules = new ArrayList<Rule>(num);
			int offset = 160 - 24;
			for (int i = 0; i < num; i++) {
				Rule rule = new Rule();
				offset += rule.read(buf, offset);
				rules.add(rule);
			}
			modified = false;
		}

		void write(byte[] buf, int offset) throws IOException {
			if (rules == null || rules.size() == 0) {
				System.err.println("No rules.");
				return;
			}
			System.arraycopy(SLST, 0, buf, offset, SLST.length);
			BasicIo.in2s(buf, offset + 11, rules.size(), 1);
			if (any)
				BasicIo.in2s(buf, offset + 15, 1, 1);
			offset += 160 - 24;
			for (int i = 0; rules != null && i < rules.size(); i++) {
				Rule r = (Rule) rules.get(i);
				r.write(buf, offset);
				offset += r.size();
			}
			modified = false;
		}

		public int size() {
			int result = 160 - 24;
			for (int i = 0; rules != null && i < rules.size(); i++)
				result += ((Rule) rules.get(i)).size();
			return result;
		}

		public boolean isChanged() {
			if (modified)
				return true;
			if (rules != null) {
				Iterator it = rules.iterator();
				while (it.hasNext()) {
					if (((Rule) it.next()).isChanged())
						return true;
				}
			}
			return false;
		}

		public void touch() {
			modified = true;
		}

		public static class Rule implements Serializable {
			public String data;

			public int id, action, start, end, duration, unit, unitEnd;

			public int fillerStart, fillerEnd;

			protected int len;

			protected transient boolean modified = true;

			int read(byte[] buf, int offset) {
				id = BasicIo.s2n(buf, offset + 3, 1, false, true);
				action = BasicIo.s2n(buf, offset + 4, 4, false, false); // !!!
				// reverse
				// order
				len = BasicIo.s2n(buf, offset + 55, 1, false, true);
				if (debug) {
					System.err.println("Rule:");
					HexDump.dumpBuffer(System.err, buf, offset, 56 + len, 0);
				}
				if (Smart.isType(id, Smart.STRING_RULES)) {
					try {
						data = new String(buf, offset + 56, len, "UTF-16BE");
					} catch (UnsupportedEncodingException uee) {
						uee.printStackTrace();
					}
				} else {
					fillerStart = BasicIo.s2n(buf, offset + 56 + 0, 4, false, false);
					start = BasicIo.s2n(buf, offset + 56 + 4, 4, false, false); // +16
					// - 1
					duration = BasicIo.s2n(buf, offset + 56 + 12, 4, true, false);
					unit = BasicIo.s2n(buf, offset + 56 + 20, 4, false, false);
					fillerEnd = BasicIo.s2n(buf, offset + 56 + 24, 4, false, false);
					end = BasicIo.s2n(buf, offset + 56 + 28, 4, false, false);// +16
					// - 1
					unitEnd = BasicIo.s2n(buf, offset + 56 + 44, 4, false, false);
				}
				// TODO: create a specific object with common interface for
				// matching task
				normalize();
				modified = false;
				if (debug)
					System.err.println("Rule:" + toString());
				return len + 56;
			}

			void write(byte[] buf, int offset) throws IOException {
				BasicIo.in2s(buf, offset + 3, id, 1);
				BasicIo.bn2s(buf, offset + 4, action, 4); // !!! reverse order
				if (Smart.isType(id, Smart.STRING_RULES)) {

					byte[] sb = data == null ? new byte[0] : data.getBytes("UTF-16BE");
					if (sb.length > 255)
						throw new IOException("Data string is too long " + sb.length + " of " + data);
					if (sb.length != len) {
						System.err.println("Length " + len + " for '" + data + "' incorrectly set. Corrected to "
								+ sb.length + '.');
						len = sb.length;
					}
					BasicIo.in2s(buf, offset + 55, len, 1);
					System.arraycopy(sb, 0, buf, offset + 56, sb.length);
				} else {
					len = 0x44;
					BasicIo.in2s(buf, offset + 55, len, 1);
					BasicIo.bn2s(buf, offset + 56 + 0, fillerStart, 4);
					BasicIo.bn2s(buf, offset + 56 + 4, start, 4);
					BasicIo.bn2s(buf, offset + 56 + 8, duration, 8);
					BasicIo.bn2s(buf, offset + 56 + 20, unit, 4);
					BasicIo.bn2s(buf, offset + 56 + 24, fillerEnd, 4);
					BasicIo.bn2s(buf, offset + 56 + 28, end, 4);
					BasicIo.bn2s(buf, offset + 56 + 44, unitEnd, 4);
				}
				modified = false;
			}

			public boolean isChanged() {
				return modified;
			}

			public void touch() {
				modified = true;
			}

			public boolean match(PlayList playList, PlayItem playItem, ITunesDB itunesdb) {
				String s = null;
				int num = -1;
				long tstamp = -1;
				// TODO can be optimized by building list of string rules and map to playlist attr retrieval
				switch (id) {
				case Smart.SONGNAME:
					s = (String) playItem.get(PlayItem.TITLE);
					if (s == null)
						s = "";
					break;
				case Smart.ALBUM:
					s = (String) playItem.get(PlayItem.ALBUM);
					if (s == null)
						s = "";
					break;
				case Smart.ARTIST:
					s = (String) playItem.get(PlayItem.ARTIST);
					if (s == null)
						s = "";
					break;
				case Smart.GENRE:
					s = (String) playItem.get(PlayItem.GENRE);
					if (s == null)
						s = "";
					break;
				case Smart.COMMENT:
					s = (String) playItem.get(PlayItem.COMMENT);
					if (s == null)
						s = "";
					break;
				case Smart.COMPOSER:
					s = (String) playItem.get(PlayItem.COMPOSER);
					if (s == null)
						s = "";
					break;
				case Smart.GROUPING:
					s = (String) playItem.get(PlayItem.GROUPING);
					if (s == null)
						s = "";
					break;
				case Smart.KIND:
					s = (String) playItem.get(PlayItem.FILETYPE);
					if (s == null)
						s = "";
					break;
				case Smart.ALBUM_ARTIST: // can be generalized
					s = (String) playItem.get(PlayItem.ALBUM_ARTIST);
					if (s == null)
						s = "";
					break;
				case Smart.SORT_ALBUM:
					s = (String) playItem.get(PlayItem.SORT_ALBUM);
					if (s == null)
						s = "";
					break;
				case Smart.SORT_ALBUM_ARTIST:
					s = (String) playItem.get(PlayItem.SORT_ALBUM_ARTIST);
					if (s == null)
						s = "";
					break;
				case Smart.SORT_NAME:
					s = (String) playItem.get(PlayItem.SORT_TITLE);
					if (s == null)
						s = "";
					break;
				case Smart.SORT_COMPOSER:
					s = (String) playItem.get(PlayItem.SORT_COMPOSER);
					if (s == null)
						s = "";
					break;
				case Smart.SORT_ARTIST:
					s = (String) playItem.get(PlayItem.SORT_ARTIST);
					if (s == null)
						s = "";
					break;
				case Smart.SORT_SHOW:
					s = (String) playItem.get(PlayItem.SORT_SHOW_TYPE);
					if (s == null)
						s = "";
					break;
				////// start of numeric rules					
				case Smart.BITRATE:
					num = ((Integer) playItem.get(PlayItem.BITRATE)).intValue();
					break;
				case Smart.SAMPLING_RATE:
					num = ((Integer) playItem.get(PlayItem.SAMPLE_RATE)).intValue();
					break;
				case Smart.YEAR:
					num = ((Integer) playItem.get(PlayItem.YEAR)).intValue();
					break;
				case Smart.SIZE:
					num = ((Integer) playItem.get(PlayItem.SIZE)).intValue();
					break;
				case Smart.PLAYCOUNT:
					num = ((Integer) playItem.get(PlayItem.PLAYED_TIMES)).intValue();
					break;
				case Smart.SKIP_COUNT:
					num = ((Integer) playItem.get(PlayItem.SKIPPED_TIMES)).intValue();
					break;
				case Smart.TIME:
					num = ((Integer) playItem.get(PlayItem.LENGTH)).intValue();
					break;
				case Smart.BPM:
					num = ((Integer) playItem.get(PlayItem.BPM)).intValue();
					break;
				case Smart.RATING:
					num = ((Integer) playItem.get(PlayItem.RATING)).intValue() * RATING_FACTOR;
					break;
				case Smart.TRACKNUMBER:
					num = ((Integer) playItem.get(PlayItem.ORDER)).intValue();
					break;
				case Smart.TIME_ADDED:
					Date date = (Date) playItem.get(PlayItem.CREATE_TIME);					
					if (date != null) {
						tstamp = date.getTime();
					} else {
						tstamp = System.currentTimeMillis();
						if (debug)
							System.err
									.printf(
											"Added time for %s was undefined, so current assumed, it can break smart list result%n",
											playItem);
					}
					break;
				case Smart.LAST_MODIFY:
					date = (Date) playItem.get(PlayItem.MODIFIED_TIME);
					if (date != null)
						tstamp = date.getTime();
					break;
				case Smart.LAST_PLAYED:
					date = (Date) playItem.get(PlayItem.LAST_TIME);
					if (date != null)
						tstamp = date.getTime();
					else
						tstamp = 0;
					System.err.println("Lat played date:" + date + ", stamp:" + tstamp);
					break;
				case Smart.LAST_SKIPPED:
					date = (Date) playItem.get(PlayItem.LAST_SKIPPED_TIME);
					if (date != null)
						tstamp = date.getTime();
					else
						tstamp = 0;
					System.err.println("Last skipped date:" + date + ", stamp:" + tstamp);
					break;
				case Smart.COMPILATION:
					return action == Smart.BOOL_COND_SET && Boolean.TRUE.equals(playItem.get(PlayItem.COMPILATION));
				case Smart.PLAYLIST:
					List<String> pls = itunesdb.getPlayLists();

					for (String ln : pls) {
						PlayList pl = itunesdb.getExistingPlayList(ln);
						if (pl == playList)
							continue;
						if (pl.isList(start, fillerStart)) {
							itunesdb.updateMagicList(pl);
							if (pl.contains(playItem)) {
								if (Smart.NUM_COND_IS == action)
									return true;
							} else if (Smart.NUM_COND_IS_NOT == action)
								return true;
							return false;
						}
					}
					return false;
				case Smart.DISC_NUM:
					num = ((Integer) playItem.get(PlayItem.DISK)).intValue();
					break;
				}
				// System.err.println("S="+s+", ndata="+normData+", num="+num+",
				// tstamp="+tstamp);
				if (s != null && normData != null) { // string oper
					s = ContentMatcher.normalize(s.toCharArray());
					switch (action) {
					case Smart.STR_COND_CONTAINS:
						return s != null && s.indexOf(normData) >= 0;
					case Smart.STR_COND_NOT_CONTAIN:
						return s == null || s.indexOf(normData) < 0;
					case Smart.STR_COND_IS:
						return normData.equals(s);
					case Smart.STR_COND_IS_NOT:
						return normData.equals(s) == false;
					case Smart.STR_COND_ST_WITH:
						return s != null && s.startsWith(normData);
					case Smart.STR_COND_EN_WITH:
						return s != null && s.endsWith(normData);
					}
					return false;
				} else if (num >= 0) {
					// System.err.println("Rate "+num+" st "+start+" en "+end);
					switch (action) {
					case Smart.NUM_COND_IS:
						return num == start;
					case Smart.NUM_COND_IS_NOT:
						return num != start;
					case Smart.NUM_COND_GT:
						return num > start;
					case Smart.NUM_COND_LT:
						return num < start;
					case Smart.NUM_COND_IN_RANGE:
						return num >= start && num <= end;
					}
					return false;
				} else if (tstamp >= 0) {
					// System.err.println(" s "+normStart+" e "+normEnd+" t
					// "+tstamp);
					switch (action) {
					case Smart.DATE_COND_IS:
						return tstamp >= normStart && tstamp < normEnd;
					case Smart.DATE_COND_IS_NOT:
						return tstamp < normStart || tstamp > normEnd;
					case Smart.DATE_COND_AFTER:
						return tstamp > normStart;
					case Smart.DATE_COND_BEFORE:
						return tstamp < normStart;
					case Smart.DATE_COND_LAST:
						// System.err.println("last
						// "+System.currentTimeMillis()+" dur
						// "+(duration*unit*1000)+" time "+tstamp);
						return (System.currentTimeMillis() - duration * unit * 1000) < tstamp;
					case Smart.DATE_COND_NOT_LAST:
						// System.err.println("not last
						// "+System.currentTimeMillis()+" dur
						// "+(duration*unit*1000)+" time "+tstamp);
						return (System.currentTimeMillis() - duration * unit * 1000) > tstamp || tstamp == 0;
					}
					return false;
				}
				return false;
			}

			public String toString() {
				return "Rule s:" + data + "  id " + id + " action 0x" + Integer.toHexString(action) + " start " + start
						+ "(0x" + Integer.toHexString(start) + ") end " + end + "(0x" + Integer.toHexString(end)
						+ ") duration " + duration + " in " + unit + " - " + unitEnd + " secs" + " filler s 0x"
						+ Integer.toHexString(fillerStart) + " filler e 0x" + Integer.toHexString(fillerEnd);
			}

			int size() {
				if (Smart.isType(id, Smart.STRING_RULES) && data != null) {
					len = data.length() * 2;
				} else {
					len = 0x44;
				}

				return 56 + len;
			}

			public void normalize() {
				if (data != null)
					normData = ContentMatcher.normalize(data.toCharArray());
				normStart = (start - javaMacDateDelta) * 1000l;
				normEnd = (end - javaMacDateDelta) * 1000l;
			}

			private String normData;

			private long normStart, normEnd;
		}

		public String toString() {
			return "Rules any " + any + " - " + rules;
		}
	}

	public static int javaMacDateDelta;

	static final HeaderInfo[] HEADER_INFOS = new HeaderInfo[NUM_HEADERS];

	static {
		HEADER_INFOS[MHBD] = new HeaderInfo("mhbd", 104);
		HEADER_INFOS[MHSD] = new HeaderInfo("mhsd", 96);
		HEADER_INFOS[MHLT] = new HeaderInfo("mhlt", 92);
		HEADER_INFOS[MHLP] = new HeaderInfo("mhlp", 92);
		HEADER_INFOS[MHYP] = new HeaderInfo("mhyp", 108);
		HEADER_INFOS[MHIP] = new HeaderInfo("mhip", 76);
		HEADER_INFOS[MHIT] = new HeaderInfo("mhit", 388); //--
		HEADER_INFOS[MHOD] = new HeaderInfo("mhod", 24);
		HEADER_INFOS[MHDP] = new HeaderInfo("mhdp", 0x60);

		HEADER_INFOS[MHFD] = new HeaderInfo("mhfd", 0x84);
		HEADER_INFOS[MHLI] = new HeaderInfo("mhli", 0x5C);
		HEADER_INFOS[MHII] = new HeaderInfo("mhii", 0x98);
		HEADER_INFOS[MHNI] = new HeaderInfo("mhni", 0x4C);
		HEADER_INFOS[MHLA] = new HeaderInfo("mhla", 0x5C);
		HEADER_INFOS[MHLF] = new HeaderInfo("mhlf", 0x5C);
		HEADER_INFOS[MHIF] = new HeaderInfo("mhif", 0x7C);
		HEADER_INFOS[MHBA] = new HeaderInfo("mhba", 148);
		HEADER_INFOS[MHIA] = new HeaderInfo("mhia", 40);

		// adjust to Mac's Jan. 1, 1904 from 1970-01-01 00:00:00 UTC
		Calendar cal = Calendar.getInstance();
		cal.set(1970, 0, 1);
		long time1 = cal.getTimeInMillis();
		cal.set(1904, 0, 1);
		javaMacDateDelta = (int) ((time1 - cal.getTimeInMillis()) / 1000l);
	}

	/*
	 * TODO: another approach is introduce class Field { String name; int
	 * offset, size; int type; Object value; } then addField(new
	 * Field("NumberOfThings", 8, 4, INTEGER, buf)); all fields are map by field
	 * name
	 */

	private final static boolean debug =
	 true;
	//false;
	
	private final static boolean debugaw = // true;
		false;
}