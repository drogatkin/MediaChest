/* MediaChest - OGG 
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
 *  $Id: OGG.java,v 1.18 2013/06/15 05:15:43 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.aldan3.util.DataConv;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import photoorganizer.media.MediaPlayer;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;

public class OGG extends SimpleMediaFormat<OGG.OggInfo> {
	public static final String OGG = "OGG";
	public static final String VOB = "VOB";
	public static final String[] EXTENSIONS = { OGG, VOB };
	static byte[] defaultIconData;
	private static final boolean __debug = true;

	public OGG(File file, String enc) {
		super(file, enc);
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	@Override
	public String getDescription() {
		return OGG;
	}

	@Override
	OggInfo createMediaInfo(File file) {
		return new OggInfo(file, encoding);
	}

	public byte[] getThumbnailData(Dimension size) {
		synchronized (OGG.class) {
			if (defaultIconData == null) {
				defaultIconData = SimpleMediaFormat.getResource(getDefaultTNname());
			}
		}
		return defaultIconData;
	}

	@Override
	public MediaPlayer getPlayer() {
		OggPlayer result = new OggPlayer();
		result.init(this);
		return result;
	}

	static final int BUFSIZE = 4096 * 2;

	class OggInfo extends SimpleMediaInfo {
		Pattern pat = Pattern.compile("(\\d\\d)(\\s-?\\s?)(.+)");

		OggInfo(File file, String en) {
			super(file, en);
			VorbisFile vf = null;
			try {
				vf = new VorbisFile(file.getPath());
				attrsMap = new HashMap<>();
				for (Comment comment : vf.getComment()) {
					// see http://www.xiph.org/vorbis/doc/v-comment.html
					String cs = comment.query("ARTIST");
					if (cs != null)
						attrsMap.put(MediaInfo.ARTIST, cs);
					else if ((cs = comment.query("ALBUMARTIST")) != null)
						attrsMap.put(MediaInfo.ARTIST, cs);
					else if ((cs = comment.query("PERFORMER")) != null)
						attrsMap.put(MediaInfo.ARTIST, cs);
					if ((cs = comment.query("ALBUM")) != null)
						attrsMap.put(MediaInfo.ALBUM, cs);
					else if ((cs = comment.query("GENRE")) != null)
						attrsMap.put(MediaInfo.GENRE, cs);
					attrsMap.put(MediaInfo.YEAR, extractInt(comment.query("DATE")));
					attrsMap.put(MediaInfo.OFTRACKS, comment.query("TRACKTOTAL"));
					attrsMap.put(MediaInfo.ISRC, comment.query("ISRC"));
					attrsMap.put(MediaInfo.PARTOFSET, partOfSet(comment.query("DISCNUMBER"),comment.query("DISCTOTAL")));
					attrsMap.put(MediaInfo.TRACK, DataConv.toIntWithDefault(comment.query("TRACKNUMBER"), 0));
					attrsMap.put(MediaInfo.LENGTH, vf.time_total(-1));
					
					if ((cs = comment.query("TITLE")) != null)
						attrsMap.put(MediaInfo.TITLE, cs);
					else {
						// TODO make pattern configurable
						String n = file.getName();
						int p = n.lastIndexOf('.');
						if (p > 0)
							n = n.substring(0, p);

						Matcher m = pat.matcher(n);
						if (m.matches()) {
							int t = 0;
							try {
								t = Integer.parseInt(m.group(1));
								n = m.group(3);
								attrsMap.put(MediaInfo.TRACK, t);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						attrsMap.put(MediaInfo.TITLE, n);
					}
				}
				for(Info info:vf.getInfo()) {
					attrsMap.put(MediaInfo.SAMPLERATE, info.rate);
					break;
				}
				//System.err.printf("Vorbis comments:%s%n", Arrays.toString(vf.getComment()));
			} catch (JOrbisException e) {
				e.printStackTrace();
			} finally {
				try {
					vf.close();
				} catch (Exception e) {

				}
			}
		}

	}

	class OggPlayer extends SimpleMediaFormat.SimpleMediaPlayer {

		@Override
		public void init(MediaFormat mf) {
			super.init(mf);
			init_jorbis();
		}

		@Override
		public void start() {
			try {
				inputStream = mediaFormat.getAsStream();
				super.start();
			} catch (IOException e) {
				reportError(e.getMessage(), e);
			}
		}

		@Override
		public void stop() {
			super.stop();
		}

		///// audio 
		int frameSizeInBytes;
		int bufferLengthInBytes;
		int rate = 0;
		int channels = 0;
		///// Orbis
		SyncState oy;
		StreamState os;
		Page og;
		Packet op;
		Info vi;
		Comment vc;
		DspState vd;
		Block vb;
		/// working items
		int bytes;

		int convsize = BUFSIZE * 2;
		byte[] convbuffer = new byte[convsize];

		////// controls
		Status status;

		void init_jorbis() {
			oy = new SyncState();
			os = new StreamState();
			og = new Page();
			op = new Packet();

			vi = new Info();
			vc = new Comment();
			vd = new DspState();
			vb = new Block(vd);

			oy.init();
		}

		void init_audio(int channels, int rate) {
			try {
				fmt = new AudioFormat((float) rate, 16, channels, true, // PCM_Signed
						false // littleEndian
				);
				info = new DataLine.Info(SourceDataLine.class, fmt, AudioSystem.NOT_SPECIFIED);
				if (!AudioSystem.isLineSupported(info)) {
					reportError("Line " + info + " not supported.", null);
					return;
				}

				try {
					line = (SourceDataLine) AudioSystem.getLine(info);
					//line.addLineListener(this);
					line.open(fmt);
				} catch (LineUnavailableException | IllegalArgumentException ex) {
					reportError(ex.getMessage(), ex);
					return;
				}
				frameSizeInBytes = fmt.getFrameSize();
				int bufferLengthInFrames = line.getBufferSize() / frameSizeInBytes / 2;
				bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
				this.rate = rate;
				this.channels = channels;
			} catch (Exception ee) {
				reportError(ee.getMessage(), ee);
			}
		}

		SourceDataLine getOutputLine(int channels, int rate) {
			if (line == null || this.rate != rate || this.channels != channels) {
				if (line != null) {
					line.drain();
					line.stop();
					line.close();
				}
				init_audio(channels, rate);
				line.start();
			}
			return line;
		}

		@Override
		void playLoop() {
			boolean chained = false;
			loop: while (true) {
				int eos = 0;

				int index = oy.buffer(BUFSIZE);
				try {
					bytes = inputStream.read( oy.data, index, BUFSIZE);
				} catch (Exception e) {
					reportError(e.getMessage(), e);
					return;
				}
				oy.wrote(bytes);

				if (chained) { //
					chained = false; //   
				} //
				else { //
					if (oy.pageout(og) != 1) {
						if (bytes < BUFSIZE)
							break;
						reportError("Input does not appear to be an Ogg bitstream.", null);
						return;
					}
				} //
				os.init(og.serialno());
				os.reset();

				vi.init();
				vc.init();

				if (os.pagein(og) < 0) {
					// error; stream version mismatch perhaps
					reportError("Error reading first page of Ogg bitstream data.", null);
					return;
				}

				if (os.packetout(op) != 1) {
					// no page? must not be vorbis
					reportError("Error reading initial header packet.", null);
					break;
					//      return;
				}

				if (vi.synthesis_headerin(vc, op) < 0) {
					// error case; not a vorbis header
					reportError("This Ogg bitstream does not contain Vorbis audio data.", null);
					return;
				}

				int i = 0;

				while (i < 2) {
					while (i < 2) {
						int result = oy.pageout(og);
						if (result == 0)
							break; // Need more data
						if (result == 1) {
							os.pagein(og);
							while (i < 2) {
								result = os.packetout(op);
								if (result == 0)
									break;
								if (result == -1) {
									reportError("Corrupt secondary header.  Exiting.", null);
									//return;
									break loop;
								}
								vi.synthesis_headerin(vc, op);
								i++;
							}
						}
					}

					index = oy.buffer(BUFSIZE);
					try {
						bytes = inputStream.read(oy.data, index, BUFSIZE);
					} catch (Exception e) {
						reportError(e.getMessage(), e);
						return;
					}
					if (bytes == 0 && i < 2) {
						reportError("End of file before finding all Vorbis headers!", null);
						return;
					}
					oy.wrote(bytes);
				}

				convsize = BUFSIZE / vi.channels;

				vd.synthesis_init(vi);
				vb.init(vd);

				float[][][] _pcmf = new float[1][][];
				int[] _index = new int[vi.channels];

				getOutputLine(vi.channels, vi.rate);

				while (eos == 0) {
					while (eos == 0) {
						int result = oy.pageout(og);
						if (result == 0)
							break; // need more data
						if (result == -1) { // missing or corrupt data at this page position
							//	    System.err.println("Corrupt or missing data in bitstream; continuing...");
						} else {
							os.pagein(og);

							if (og.granulepos() == 0) { //
								chained = true; //
								eos = 1; // 
								break; //
							} //

							while (true) {
								result = os.packetout(op);
								if (result == 0)
									break; // need more data
								if (result == -1) { // missing or corrupt data at this page position
									// no reason to complain; already complained above

									//System.err.println("no reason to complain; already complained above");
								} else {
									// we have a packet.  Decode it
									int samples;
									if (vb.synthesis(op) == 0) { // test for success!
										vd.synthesis_blockin(vb);
									}
									while ((samples = vd.synthesis_pcmout(_pcmf, _index)) > 0) {
										float[][] pcmf = _pcmf[0];
										int bout = (samples < convsize ? samples : convsize);

										// convert doubles to 16 bit signed ints (host order) and
										// interleave
										for (i = 0; i < vi.channels; i++) {
											int ptr = i * 2;
											//int ptr=i;
											int mono = _index[i];
											for (int j = 0; j < bout; j++) {
												int val = (int) (pcmf[i][mono + j] * 32767.);
												if (val > 32767) {
													val = 32767;
												}
												if (val < -32768) {
													val = -32768;
												}
												if (val < 0)
													val = val | 0x8000;
												convbuffer[ptr] = (byte) (val);
												convbuffer[ptr + 1] = (byte) (val >>> 8);
												ptr += 2 * (vi.channels);
											}
										}
										line.write(convbuffer, 0, 2 * vi.channels * bout);
										
										vd.synthesis_read(bout);
										if (checkPause() == false)
											break loop;
									}
								}
							}
							if (og.eos() != 0)
								eos = 1;
						}
					}

					if (eos == 0) {
						index = oy.buffer(BUFSIZE);
						try {
							bytes = inputStream.read(oy.data, index, BUFSIZE);
						} catch (Exception e) {
							System.err.println(e);
							return;
						}
						if (bytes == -1) {
							break;
						}
						oy.wrote(bytes);
						if (bytes == 0)
							eos = 1;
					}
				}

				os.clear();
				vb.clear();
				vd.clear();
				vi.clear();
			}
			oy.clear();
		}
	}
}
