/* MediaChest - M4A 
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
 *  $Id: M4A.java,v 1.16 2015/02/22 09:37:25 cvs Exp $
 */
package photoorganizer.formats;

import java.io.File;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import mediautil.gen.MediaFormat;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;
import photoorganizer.media.MediaPlayer;

import com.beatofthedrum.alacdecoder.AlacContext;
import com.beatofthedrum.alacdecoder.AlacUtils;
import com.beatofthedrum.alacdecoder.AlacInputStreamImpl;

public class M4A extends MP4 {
	public static final String M4A = "M4A";
	public static final String[] EXTENSIONS = { M4A };
	static protected byte[] defaultIconData;

	public M4A(File file, String enc) {
		super(file, enc);
	}

	@Override
	public String getFormat(int arg0) {
		return M4A;
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	public MediaPlayer getPlayer() {
		if ((getType() & LOSSLESS) > 0) {
			M4APlayer result = new M4APlayer();
			result.init(this);
			return result;
		} else if ((getType() & MediaFormat.VIDEO) == 0) {
			// AACPlayer result = new AACPlayer();
			MP4Player result = new MP4Player();
			result.init(this);
			return result;
		}
		throw new IllegalArgumentException("Lossy isn't supported yet");
	}

	class M4APlayer extends SimpleMediaFormat.SimpleMediaPlayer {
		AlacContext ac;

		@Override
		void playLoop() {
			int num_channels;
			try {
				ac = AlacUtils.AlacOpenFileInput(new AlacInputStreamImpl(
						MediaFormatFactory.getInputStreamFactory().getInputStream(mediaFormat.getFile())));
				if (ac.error) {
					reportError(statusMessage, null);
					return;
				}

				fmt = new AudioFormat(AlacUtils.AlacGetSampleRate(ac), AlacUtils.AlacGetBitsPerSample(ac),
						num_channels = AlacUtils.AlacGetNumChannels(ac), true, false);
				info = new DataLine.Info(SourceDataLine.class, fmt, AudioSystem.NOT_SPECIFIED);

				// info = new DataLine.Info(SourceDataLine.class, fmt, 4000);
				line = new SimpleDownSampler(fmt).getLine();
				fmt = line.getFormat();
				if (!line.isOpen())
					line.open(fmt, AudioSystem.NOT_SPECIFIED);
			} catch (IOException e) {
				reportError(String.format("IO with %s", mediaFormat.getFile()), e);
				return;
			} catch (LineUnavailableException e) {
				reportError(String.format("Can't obtain line with %s", fmt), e);
				return;
			}
			line.start();

			byte[] pcmBuffer = new byte[65536];

			int bytes_unpacked;

			int[] pDestBuffer = new int[1024 * 24 * 3]; // 24kb buffer = 4096 frames = 1 alac sample (we support max
														// 24bps)

			int bps = AlacUtils.AlacGetBytesPerSample(ac);

			while (true) {
				bytes_unpacked = AlacUtils.AlacUnpackSamples(ac, pDestBuffer);
				if (bytes_unpacked > 0) {
					SimpleDownSampler.samplesToBytes(bps, pDestBuffer, bytes_unpacked / bps, pcmBuffer, bps,
							num_channels == 2, true);
					line.write(pcmBuffer, 0, bytes_unpacked);
				} else
					break;

				if (checkPause() == false)
					break;
			} // end of while
		}

		@Override
		protected void freeResources() {
			if (ac != null)
				AlacUtils.AlacCloseFile(ac);
			super.freeResources();
		}
	}

	class AACPlayer extends SimpleMediaFormat.SimpleMediaPlayer {

		@Override
		void playLoop() {
			byte[] b;
			try {
				final ADTSDemultiplexer adts = new ADTSDemultiplexer(mediaFormat.getAsStream());
				final Decoder dec = new Decoder(adts.getDecoderSpecificInfo());
				final SampleBuffer buf = new SampleBuffer();
				while (true) {
					b = adts.readNextFrame();
					dec.decodeFrame(b, buf);

					if (line == null) {
						fmt = new AudioFormat(buf.getSampleRate(), buf.getBitsPerSample(), buf.getChannels(), true,
								true);
						line = AudioSystem.getSourceDataLine(fmt);
						line.open();
						line.start();
					}
					b = buf.getData();
					if (b.length > 0) {
						line.write(b, 0, b.length);
					} else
						break;
					if (checkPause() == false)
						break;
				}
			} catch (Exception e) {
				reportError(e.getMessage(), e);
			}
		}

		/*
		 * @Override void freeResources() { super.freeResources(); }
		 */
	}

	class MP4Player extends SimpleMediaFormat.SimpleMediaPlayer {
		private RandomAccessFile raf;

		@Override
		void playLoop() {
			byte[] b;
			AudioTrack track;
			Decoder dec;
			try {
				// create container
				final MP4Container cont = new MP4Container(raf = new RandomAccessFile(mediaFormat.getFile(), "r"));
				final Movie movie = cont.getMovie();
				// find AAC track
				final List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
				if (tracks.isEmpty())
					throw new Exception("movie does not contain any AAC track");
				track = (AudioTrack) tracks.get(0);

				// create audio format
				fmt = new AudioFormat(track.getSampleRate(), track.getSampleSize(), track.getChannelCount(), true,
						true);
				line = AudioSystem.getSourceDataLine(fmt);
				line.open();
				line.start();
				// create AAC decoder
				dec = new Decoder(track.getDecoderSpecificInfo());
			} catch (Exception e) {
				reportError("AAC exception", e);
				// AudioSystem.
				return;
			}
			// decode
			Frame frame;
			final SampleBuffer buf = new SampleBuffer();
			while (track.hasMoreFrames()) {
				try {
					frame = track.readNextFrame();
					dec.decodeFrame(frame.getData(), buf);
					b = buf.getData();
					if (b.length > 0) {
						line.write(b, 0, b.length);
					} else
						break;
				} catch (IOException e) {
					reportError("AAC exception", e);
					break;
				}
				if (checkPause() == false)
					break;
			}
		}

		@Override
		protected void freeResources() {
			try {
				raf.close();
			} catch (IOException e) {
			}
			super.freeResources();
		}
	}
}
