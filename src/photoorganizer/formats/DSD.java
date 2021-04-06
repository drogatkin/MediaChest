/* MediaChest - DSD 
 * Copyright (C) 2001-2014 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: DSD.java,v 1.11 2014/12/07 07:47:48 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import mediautil.gen.MediaInfo;

import org.justcodecs.dsd.DFFFormat;
import org.justcodecs.dsd.DISOFormat;
import org.justcodecs.dsd.DSDFormat;
import org.justcodecs.dsd.DSFFormat;
import org.justcodecs.dsd.Decoder;
import org.justcodecs.dsd.DecoderInt;
import org.justcodecs.dsd.Decoder.DecodeException;
import org.justcodecs.dsd.Decoder.PCMFormat;
import org.justcodecs.dsd.Utils;

import photoorganizer.media.MediaPlayer;

public class DSD extends SimpleMediaFormat<DSD.DSDInfo> {
	public static final String DSDFORMAT = "DSD";
	public static final String DSD = "DSF";
	public static final String DIFF = "DFF";
	public static final String[] EXTENSIONS = { DSD, DIFF, "ISO" };
	static byte[] defaultIconData;
	static final int PLAY_BUF_SZ = 1024*16;

	public DSD(File file, String enc) {
		super(file, enc);
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	@Override
	DSDInfo createMediaInfo(File file) {
		return new DSDInfo(file, encoding);
	}

	@Override
	public String getDescription() {
		return DSDFORMAT;
	}

	@Override
	public String getFormat(int arg0) {
		return DSDFORMAT;
	}

	public byte[] getThumbnailData(Dimension size) {
		byte[] res = (byte[]) info.getAttribute(MediaInfo.PICTURE);
		if (res != null)
			return res;
		synchronized (DSD.class) {
			if (defaultIconData == null) {
					defaultIconData = SimpleMediaFormat.getResource(getDefaultTNname());
			}
		}
		return defaultIconData;
	}

	@Override
	public MediaPlayer getPlayer() {
		MediaPlayer result = new DSDPlayer();
		result.init(this);
		//System.err.printf("Player returned %s%n", result);
		return result;
	}

	class DSDInfo extends SimpleMediaInfo {
		DSDFormat dsd;

		protected DSDInfo(File f, String e) {
			super(f, e);
			try {
				getInfo();
			} catch (Exception ex) {
				throw new IllegalArgumentException("Can't read format", ex);
			}
		}

		private void getInfo() throws IOException, DecodeException {
			String n = file.getName().toUpperCase();
			if (n.endsWith("." + DSD)) {
				dsd = new DSFFormat();
			} else if (n.endsWith("." + "ISO")) {
				dsd = new DISOFormat();
			} else
				dsd = new DFFFormat();
			try {
				dsd.init(new Utils.RandomDSDStream(file));
				Decoder decoder = new Decoder();
				decoder.init(dsd);
				attrsMap = new HashMap<>();
				long l = decoder.getSampleCount() / decoder.getSampleRate();
				attrsMap.put(LENGTH, l);
				attrsMap.put(SAMPLERATE, decoder.getSampleRate());
				attrsMap.put(TITLE, file.getName());
				putAttribute(ARTIST, dsd.getMetadata("Artist"));
				putAttribute(TITLE, dsd.getMetadata("Title"));
				putAttribute(ALBUM, dsd.getMetadata("Album"));
				putAttribute(YEAR, dsd.getMetadata("Year"));
				putAttribute(PICTURE, dsd.getMetadata("Picture"));
				decoder.dispose();
			} finally {
				dsd.close();
			}
		}
		
		private void putAttribute(String name, Object value) {
			if (value != null)
				attrsMap.put(name, value);
		}

	}

	class DSDPlayer extends SimpleMediaFormat.SimpleMediaPlayer<DSD> {
		Decoder decoder;

		@Override
		void playLoop() {
			decoder = new Decoder();
			try {
				//System.err.printf("Play %s %n",  mediaFormat.info.dsd);
				mediaFormat.info.dsd.init(new Utils.RandomDSDStream(mediaFormat.getFile()));
				decoder.init(mediaFormat.info.dsd);
				PCMFormat pcmf = new PCMFormat();
				int de = decoder.getSampleRate()/44100 > 128?2:1;
				pcmf.sampleRate = 44100 * 2 * 2 * de;
				pcmf.bitsPerSample = 16;//8*(de+1);
				pcmf.channels = mediaFormat.info.dsd.getNumChannels();
				int channels = (pcmf.channels > 2 ? 2 : pcmf.channels);
				fmt = new AudioFormat(pcmf.sampleRate, pcmf.bitsPerSample, channels, true, pcmf.lsb);
				line = new SimpleDownSampler(fmt).getLine();
				fmt = line.getFormat();
				//line = AudioSystem.getSourceDataLine(fmt);
				decoder.setPCMFormat(pcmf);
				System.err.printf("Play %s%n", fmt);
				int[][] samples = new int[pcmf.channels][PLAY_BUF_SZ];
				int bytesChannelSample = pcmf.bitsPerSample / 8;
				int bytesSample = channels * bytesChannelSample;
				byte[] playBuffer = new byte[bytesSample * PLAY_BUF_SZ];
				if (!line.isOpen())
					line.open();
				line.start();
				decoder.seek(0);
				do {
					if (checkPause() == false)
						break;
					int nsampl = decoder.decodePCM(samples);
					//System.err.printf("sampl %d%n", nsampl);
					if (nsampl <= 0)
						break;
					int bp = 0;
					for (int s = 0; s < nsampl; s++) {
						for (int c = 0; c < channels; c++) {
							//System.out.printf("%x", samples[c][s]);
							for (int b = 0; b < bytesChannelSample; b++)
								playBuffer[bp++] = (byte) ((samples[c][s] >> (b * 8)) & 255);
						}
					}
					//for (int k=0;k<bp; k++)
					//System.out.printf("%x", playBuffer[k]);
					line.write(playBuffer, 0, bp);
					//sampleCount += nsampl;
				} while (true);
				//System.err.printf("Play ended%n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void freeResources() {
			super.freeResources();
			decoder.dispose();
		}

	}
}
