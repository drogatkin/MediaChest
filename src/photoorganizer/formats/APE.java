/* MediaChest - APE 
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
 *  $Id: APE.java,v 1.23 2015/02/22 09:37:26 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import photoorganizer.media.MediaPlayer;
import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.info.APEInfo;
import davaguine.jmac.info.APETag;

public class APE extends SimpleMediaFormat<APE.ApeInfo> {
	public static final String APE = "APE";
	public static final String[] EXTENSIONS = { APE };

	private static final boolean __debug = true;
	static byte[] defaultIconData;

	public APE(File file, String enc) {
		super(file, enc);

	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	@Override
	public String getDescription() {
		return APE;
	}

	@Override
	ApeInfo createMediaInfo(File file) {
		return new ApeInfo(file, encoding);
	}

	@Override
	public byte[] getThumbnailData(Dimension size) {
		synchronized (APE.class) {
			if (defaultIconData == null) {
				defaultIconData = SimpleMediaFormat.getResource(getDefaultTNname());
			}
		}
		return defaultIconData;
	}

	@Override
	public MediaPlayer getPlayer() {
		MediaPlayer result = new ApePlayer();
		result.init(this);
		return result;
	}

	class ApeInfo extends SimpleMediaInfo {
		ApeInfo(File file, String e) {
			super(file, e);
		}

		@Override
		boolean cueAware() {
			return true;
		}

		@Override
		void processCue() throws IOException {
			try {
				super.processCue();
			} catch(IOException e) {
				e.printStackTrace();
			}
			APEInfo ai = null;
			try {
				//System.out.printf("file %s%n", file);
				ai = new APEInfo(MediaFormatFactory.getInputStreamFactory().createApeFile(file), null);
				if (attrsMap == null)
					attrsMap = new HashMap<>();
				attrsMap.put(MediaInfo.LENGTH, ai.getApeInfoLengthMs() / 1000);
				attrsMap.put(MediaInfo.SAMPLERATE, ai.getApeInfoSampleRate());
				attrsMap.put(MediaInfo.BITRATE, ai.getApeInfoBitsPerSample());
				APETag at = ai.getApeInfoTag();
				if (at != null) {
					String val = at.GetFieldString(APETag.APE_TAG_FIELD_ALBUM);
					if (val != null)
						attrsMap.put(MediaInfo.ALBUM, val);
					val = at.GetFieldString(APETag.APE_TAG_FIELD_ARTIST);
					if (val != null)
						attrsMap.put(ARTIST, val);
					val = at.GetFieldString(APETag.APE_TAG_FIELD_TITLE);
					if (val != null)
						attrsMap.put(TITLE, val);
					val = at.GetFieldString(APETag.APE_TAG_FIELD_YEAR);
					if (val != null)
						attrsMap.put(YEAR, extractInt(val));
					val = at.GetFieldString(APETag.APE_TAG_FIELD_GENRE);
					if (val != null)
						attrsMap.put(GENRE, val);
					val = at.GetFieldString(APETag.APE_TAG_FIELD_TRACK);
					if (val != null)
						attrsMap.put(TRACK, extractInt(val));
					val = at.GetFieldString(APETag.APE_TAG_FIELD_COMPOSER);
					if (val != null)
						attrsMap.put(COMPOSER, val);
				}
			} finally {
				if (ai != null)
					ai.close();
			}
		}
	}

	class ApePlayer extends SimpleMediaFormat.SimpleMediaPlayer {
		public final static int BLOCKS_PER_DECODE = 9216;

		private davaguine.jmac.tools.File io;
		private IAPEDecompress decoder;
		private int nBlocksLeft;
		private int blockAlign;
		
		@Override
		public void init(MediaFormat mf) {
			super.init(mf);
			try {
				io = MediaFormatFactory.getInputStreamFactory().createApeFile(mf.getFile());
				decoder = IAPEDecompress.CreateIAPEDecompress(io);

				fmt = new AudioFormat(decoder.getApeInfoSampleRate(), decoder.getApeInfoBitsPerSample(),
						decoder.getApeInfoChannels(), true, false);
				
				//info = new DataLine.Info(SourceDataLine.class, fmt, 4000);
				line = new SimpleDownSampler(fmt).getLine();
				fmt = line.getFormat();
				if (!line.isOpen())
					line.open(fmt, millisecondsToBytes(fmt, 2000));
				line.start();
			} catch (IOException | LineUnavailableException e) {
				reportError(e.getMessage(), e);
			}
		}

		public int millisecondsToBytes(AudioFormat fmt, int time) {
			return (int) (time * (fmt.getSampleRate() * fmt.getChannels() * fmt.getSampleSizeInBits()) / 8000.0);
		}

		@Override
		public void start() {
			nBlocksLeft = decoder.getApeInfoDecompressTotalBlocks();
			blockAlign = decoder.getApeInfoBlockAlign();
			super.start();
		}

		@Override
		public void stop() {
			nBlocksLeft = 0;
			super.stop();
		}

		@Override
		void playLoop() {
			// allocate space for decompression
			byte[] spTempBuffer = new byte[blockAlign * BLOCKS_PER_DECODE];
			int chs = decoder.getApeInfoChannels();
			int tbps = decoder.getApeInfoBytesPerSample();
			boolean test = true;
			if (test) {
				int[] temp_buffer = new int[BLOCKS_PER_DECODE];
				while (nBlocksLeft > 0) {
					if (checkPause() == false)
						break;
					try {
						int nBlocksDecoded = decoder.GetData(temp_buffer, BLOCKS_PER_DECODE/2);
						if (nBlocksDecoded <= 0)
							break;
						// update amount remaining
						nBlocksLeft -= nBlocksDecoded;
						SimpleDownSampler.samplesToBytes(tbps, temp_buffer, nBlocksDecoded*chs, spTempBuffer, tbps,
								chs == 2, true);
						line.write(spTempBuffer, 0, nBlocksDecoded * blockAlign);
					} catch (IOException ioe) {
						reportError("IO in APE playback", ioe);
						break;
					}
				}
			} else {
			while (nBlocksLeft > 0) {
				if (checkPause() == false)
					break;
				try {
					int nBlocksDecoded = decoder.GetData(spTempBuffer, BLOCKS_PER_DECODE);
					if (nBlocksDecoded <= 0)
						break;
					// update amount remaining
					nBlocksLeft -= nBlocksDecoded;
					line.write(spTempBuffer, 0, nBlocksDecoded * blockAlign);
				} catch (IOException ioe) {
					reportError("IO in APE playback", ioe);
					break;
				}
			}
			}
		}

		protected void freeResources() {
			if (io != null)
				try {
					io.close();
				} catch (IOException e) {
				}
			super.freeResources();
		}
	}
}
