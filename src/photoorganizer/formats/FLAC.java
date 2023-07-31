/* MediaChest - FLAC 
 * Copyright (C) 2001-2012 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: FLAC.java,v 1.41 2015/02/22 09:37:25 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import mediautil.gen.MediaInfo;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.Picture;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.util.ByteData;

import photoorganizer.Resources;
import photoorganizer.media.MediaPlayer;

public class FLAC extends SimpleMediaFormat<FLAC.FlacInfo> {
	public static final String FLAC = "FLAC";
	public static final String[] EXTENSIONS = { FLAC };
	static protected byte[] defaultIconData;

	private static final boolean __debug = true;

	public FLAC(File file, String enc) {
		super(file, enc);
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	@Override
	FlacInfo createMediaInfo(File file) {
		return new FlacInfo(file, encoding);
	}

	@Override
	public String getDescription() {
		return FLAC;
	}

	@Override
	public String getFormat(int arg0) {
		return FLAC;
	}

	@Override
	public byte[] getThumbnailData(Dimension arg0) {
		synchronized (FLAC.class) {
			if (defaultIconData == null) {
				defaultIconData = getResource(Resources.IMG_MP4ICON);
			}
		}
		return defaultIconData;
	}

	@Override
	public MediaPlayer getPlayer() {
		FlacPlayer result = new FlacPlayer();
		result.init(this);
		return result;
	}

	class FlacInfo extends SimpleMediaInfo {

		FlacInfo(File file, String e) {
			super(file, e);
		}

		@Override
		boolean cueAware() {
			return true;
		}

		@Override
		void processCue() throws IOException {
			//super.processCue();
			try (InputStream inputStream = MediaFormatFactory.getInputStramFactory().  getInputStream(file)) {
				FLACDecoder flacDec = new FLACDecoder(inputStream);
				attrsMap = new HashMap<>();
				for (Metadata md : flacDec.readMetadata()) {
					//System.err.printf("Reading metadata%s%n", md);
					if (md instanceof StreamInfo) {
						StreamInfo si = (StreamInfo) md;
						if (si.getSampleRate() > 0) {
							long l = si.getTotalSamples() / si.getSampleRate();
							attrsMap.put(MediaInfo.LENGTH, l);
							attrsMap.put(MediaInfo.SAMPLERATE, si.getSampleRate());	
						}						
					} else if (md instanceof VorbisComment) {
						VorbisComment comment = (VorbisComment) md;
						putAttr(comment, "ARTIST", MediaInfo.ARTIST);
						putAttr(comment, "ALBUM", MediaInfo.ALBUM);
						putAttr(comment, "GENRE", MediaInfo.GENRE);
						putAttr(comment, "TITLE", MediaInfo.TITLE);
						putIntAttr(comment, "TRACKNUMBER", MediaInfo.TRACK);
						putIntAttr(comment, "DATE", MediaInfo.YEAR);
						putIntAttr(comment, "TRACKTOTAL", MediaInfo.OFTRACKS);
						putAttr(comment, "ISRC", MediaInfo.ISRC);
						putIntAttr(comment, "DISCTOTAL", MediaInfo.PARTOFSET + 1);
						putIntAttr(comment, "DISCNUMBER", MediaInfo.PARTOFSET + 0);
						if (attrsMap.get(MediaInfo.PARTOFSET + 1) != null)
					    	attrsMap.put(MediaInfo.PARTOFSET,
								attrsMap.get(MediaInfo.PARTOFSET + 0) + "/" + attrsMap.get(MediaInfo.PARTOFSET + 1));
						else
							attrsMap.put(MediaInfo.PARTOFSET,
									attrsMap.get(MediaInfo.PARTOFSET + 0) + "/");
					} else if (md instanceof Picture) {
						// TODO add a code to convert pic data in thumbnail
						//System.err.printf("MD: %s of %s%n", md, md.getClass());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*try (InputStream inputStream = new FileInputStream(file)) {
				FLACDecoder flacDec = new FLACDecoder(inputStream);
				length = flacDec.getStreamInfo().calcLength();
				Metadata metadata;
				do {
					metadata = flacDec.readNextMetadata();
					System.err.printf("Metadata %s of %s%n", metadata, metadata.getClass());
				} while (!metadata.isLast());
			} catch(Exception e) {
				e.printStackTrace();
			}*/
		}

		private void putAttr(VorbisComment comment, String name, String attr) {
			String[] vals = comment.getCommentByName(name);
			if (vals != null && vals.length > 0)
				attrsMap.put(attr, vals[0]);
		}

		private void putIntAttr(VorbisComment comment, String name, String attr) {
			String[] vals = comment.getCommentByName(name);
			if (vals != null && vals.length > 0)
				attrsMap.put(attr, extractInt(vals[0]));
		}
	}

	class FlacPlayer extends SimpleMediaFormat.SimpleMediaPlayer<FLAC> implements PCMProcessor {

		FLACDecoder decoder;

		@Override
		void playLoop() {
			try {
				decoder = new FLACDecoder(inputStream = mediaFormat.getAsStream());
				decoder.addPCMProcessor(this);
				decoder.decode();
			} catch (IOException e) {
				if (e.getMessage().equalsIgnoreCase("Stream Closed") == false)
					reportError("IO", e);
			}
			if (decoder.getBadFrames() > 0)
				reportError(String.format("Completed with %d bad frames%n", decoder.getBadFrames()), null);
		}

		@Override
		public void processPCM(ByteData pcm) {
			line.write(pcm.getData(), 0, pcm.getLen());
			if (checkPause() == false) {
				decoder.removePCMProcessor(this);
			}
		}

		@Override
		public void processStreamInfo(StreamInfo streamInfo) {
			try {
				fmt = new AudioFormat(streamInfo.getSampleRate(), streamInfo.getBitsPerSample(),
						streamInfo.getChannels(), true, false);
				info = new DataLine.Info(SourceDataLine.class, fmt, AudioSystem.NOT_SPECIFIED);
				line = new SimpleDownSampler(fmt).getLine();
				fmt = line.getFormat();
				if (!line.isOpen())
					line.open(fmt, AudioSystem.NOT_SPECIFIED);
				line.start();
				//FloatControl volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				//volume.setValue(3.0f);
				//System.err.println("Current volume :"+volume.getValue()+" of "+volume.getMaximum());
			} catch (LineUnavailableException e) {
				reportError("Line unavailable", e);
				try {
					inputStream.close();
				} catch (IOException ie) {

				}
			}
		}
	}
}
