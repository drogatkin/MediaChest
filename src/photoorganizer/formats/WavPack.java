/* MediaChest - WavPack 
 * Copyright (C) 2001-2013 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: WavPack.java,v 1.24 2015/02/22 09:37:25 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;

import mediautil.gen.MediaInfo;
import photoorganizer.media.MediaPlayer;
import wavpack.Defines;
import wavpack.WavPackUtils;
import wavpack.WavpackContext;
import net.didion.loopy.FileEntry;
import net.didion.loopy.LoopyException;
import net.didion.loopy.iso9660.ISO9660FileSystem;

public class WavPack extends SimpleMediaFormat<WavPack.WavpackInfo> {
	public static final String WAVPACK = "WAVPACK";
	public static final String WV = "WV";
	public static final String ISOWV = "ISO.WV";
	public static final String[] EXTENSIONS = { WV, ISOWV };
	static byte[] defaultIconData;

	public WavPack(File file, String enc) {
		super(file, enc);
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	@Override
	WavpackInfo createMediaInfo(File file) {
		return new WavpackInfo(file, encoding);
	}

	@Override
	public String getDescription() {
		return WAVPACK;
	}

	@Override
	public String getFormat(int format) {
		return WAVPACK;
	}

	public byte[] getThumbnailData(Dimension size) {
		synchronized (WavPack.class) {
			if (defaultIconData == null) {
				defaultIconData = SimpleMediaFormat.getResource(getDefaultTNname());
			}
		}
		return defaultIconData;
	}

	@Override
	public MediaPlayer getPlayer() {
		WavPackPlayer result = new WavPackPlayer();
		result.init(this);
		return result;
	}

	class WavpackInfo extends SimpleMediaInfo {
		ISO9660FileSystem fs;
		byte[] picture;

		WavpackInfo(File file, String e) {
			super(file, e);
			close();
		}

		@Override
		boolean cueAware() {
			return true;
		}

		@Override
		protected InputStream getCueStream() {
			String mediaName = file.getName().toUpperCase();
			try {
				checkPackage();
				if (fs != null) {
					Enumeration<FileEntry> es = fs.getEntries();
					while (es.hasMoreElements()) {
						FileEntry entry = es.nextElement();
						// TODO look also for jpg file especially with preserved names as front.jpg, back.jpg annd cd.jpg
						if (entry.getName().toUpperCase().endsWith(".CUE") && entry.isDirectory() == false) {
							return fs.getInputStream(entry);
						}
					}
				} else // TODO cue can be inside as apetagsex see http://audacity.googlecode.com/svn/audacity-src/trunk/lib-src/taglib/taglib/ape/ape-tag-format.txt
					return super.getCueStream();
			} catch (Exception e) {
				System.err.printf("WAVPACK: exception %s at getting cue info%n", e);
			}
			return null;
		}

		@Override
		void processCue() throws IOException {
			super.processCue();
			InputStream wvs = null;
			try {
				wvs = getWVStream();
				WavpackContext wpc = WavPackUtils.WavpackOpenFileInput(new DataInputStream(wvs));
				if (attrsMap == null)
					attrsMap = new HashMap<>();
				long l = WavPackUtils.WavpackGetSampleRate(wpc);
				attrsMap.put(MediaInfo.SAMPLERATE, (int) l);
				attrsMap.put(MediaInfo.LENGTH, WavPackUtils.WavpackGetNumSamples(wpc) / l);
			} catch (Exception e) {
				System.err.printf("WAVPACK: exception %s at getting info%n", e);
			} finally {
				if (wvs != null)
					try {
						wvs.close();
					} catch (IOException e) {
					}
			}
		}

		void checkPackage() throws Exception {
			if (fs != null)
				return;
			String mediaName = file.getName().toUpperCase();
			if (mediaName.endsWith("." + ISOWV))
				fs = new ISO9660FileSystem(file, true);
		}

		InputStream getWVStream() throws Exception {
			checkPackage();
			if (fs != null) {
				Enumeration<FileEntry> es = fs.getEntries();
				while (es.hasMoreElements()) {
					FileEntry entry = es.nextElement();
					if (entry.getName().toUpperCase().endsWith("." + WV) && entry.isDirectory() == false) {
						return fs.getInputStream(entry);
					}
				}
				throw new IOException("No wavpack found inside ISO");
			} else
				return MediaFormatFactory.getInputStramFactory().  getInputStream(file);

		}

		void close() {
			if (fs != null)
				try {
					fs.close();
				} catch (LoopyException err) {
				}
			fs = null;
		}
	}

	class WavPackPlayer extends SimpleMediaFormat.SimpleMediaPlayer<WavPack> {

		@Override
		void playLoop() {
			int num_channels = 0;
			WavpackContext wpc = null;
			try {
				inputStream = mediaFormat.getMediaInfo().getWVStream();
				wpc = WavPackUtils.WavpackOpenFileInput(new DataInputStream(inputStream));
				//WavpackMetadata wpmd = new WavpackMetadata ();
				//MetadataUtils.read_metadata_buff(wpc, wpmd);
				
				fmt = new AudioFormat(WavPackUtils.WavpackGetSampleRate(wpc),
						WavPackUtils.WavpackGetBitsPerSample(wpc),
						num_channels = WavPackUtils.WavpackGetReducedChannels(wpc), true, false);
				line = new SimpleDownSampler(fmt).getLine();
				fmt = line.getFormat();
				if (!line.isOpen())
					line.open(fmt);
			} catch (Exception e) {
				reportError(e.getMessage(), e);
				return;
			}
			line.start();
			int[] temp_buffer = new int[Defines.SAMPLE_BUFFER_SIZE];
			int bps = WavPackUtils.WavpackGetBytesPerSample(wpc);
			byte[] pcm_buffer = new byte[temp_buffer.length*bps];
			while (true) {
				int samples_unpacked = (int) WavPackUtils.WavpackUnpackSamples(wpc, temp_buffer,
						temp_buffer.length / num_channels);
				if (samples_unpacked > 0) {
					samples_unpacked = samples_unpacked * num_channels;

					SimpleDownSampler.samplesToBytes(bps, temp_buffer, samples_unpacked, pcm_buffer, bps,
							num_channels == 2, true);
					line.write(pcm_buffer, 0, samples_unpacked * bps);
				} else
					break;
				if (checkPause() == false)
					break;
			}
		}

		@Override
		protected void freeResources() {
			super.freeResources();
			mediaFormat.getMediaInfo().close();
		}
	}
}
