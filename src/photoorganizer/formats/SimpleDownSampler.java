/* MediaChest - $RCSfile: SimpleDownSampler.java,v $                          
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
 *  $Id: SimpleDownSampler.java,v 1.17 2014/12/26 07:37:11 cvs Exp $           
 */
package photoorganizer.formats;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SimpleDownSampler implements InvocationHandler {

	static final int _16_BITS_DEPTH = 16;
	int requestedBytes;
	SourceDataLine origLine, line;
	Exception lastError;
	DitherState dithers[];
	int resample;

	public SimpleDownSampler(AudioFormat fmt) throws LineUnavailableException {
		//if (AudioSystem.isLineSupported(info)) {
		int requestedBits = fmt.getSampleSizeInBits();
		try {
			if (System.getProperty("java.runtime.name").startsWith("OpenJDK") == false
					|| requestedBits <= _16_BITS_DEPTH) {
				origLine = AudioSystem.getSourceDataLine(fmt);
				origLine.open();
			}
		} catch (LineUnavailableException | java.lang.IllegalArgumentException e) {
			lastError = e;
			origLine = null;
		}
		if (origLine == null) {
			float tsr = fmt.getSampleRate();
			if (_16_BITS_DEPTH >= requestedBits && tsr <= 192000)
				throw new LineUnavailableException("Line: " + fmt);
			resample = 1;
			while (tsr / resample > 192000) {
				resample *= 2;
			}
			AudioFormat format = new AudioFormat(fmt.getSampleRate() / resample, _16_BITS_DEPTH, fmt.getChannels(),
					true, false);
			requestedBytes = requestedBits / 8;
			origLine = AudioSystem.getSourceDataLine(format);
			line = (SourceDataLine) Proxy.newProxyInstance(origLine.getClass().getClassLoader(),
					new Class[] { SourceDataLine.class }, this);
			dithers = new DitherState[2]; // channels
			dithers[0] = new DitherState();
			dithers[1] = new DitherState();
			System.err.printf("Line %s obtained for %d/%f%n", format, requestedBits, fmt.getSampleRate());
		} else
			line = origLine;
	}

	public SourceDataLine getLine() {
		return line;
	}

	int peak;
	long peakCnt;
	int low;
	long lowCnt;

	// TODO move down sampling in decoder
	public int write(byte[] buf, int off, int len) {
		// resample the buff
		int rp = off;
		int channel = 0;
		int meanBytes = requestedBytes > 3 ? requestedBytes - 3 : 0;

		for (int os = off, n = off + len; os < n;) {
			// TODO provide alternative just copy two significant bytes
			int j = os + requestedBytes - 1;
			int sample = buf[j--];
			//boolean neg = (sample & 0x80) != 0; 
			while (j >= os + meanBytes) {
				sample <<= 8;
				sample |= buf[j--] & 0xff;
			}
			//if (neg)
				//sample = - sample;
			os += requestedBytes;
			// TODO resample as shifting to 6 and then mask high bits
			/*if (peak == 0 || Math.abs(sample) > peak) {
				peak = Math.abs(sample);
				peakCnt = 0;
			}*/
			/*if (Math.abs(sample) == peak)
				peakCnt ++;*/
			/*if (sample > 0x3fffff)
				peakCnt ++;
			
			if (low == 0 || Math.abs(sample) < low) {
				low = Math.abs(sample);
				lowCnt = 0;
			}
			if (Math.abs(sample) == low)
				lowCnt ++;
				*/
			//sample = (int) Math.round((double)1.5* sample);
			dithers[channel].accum += sample;
			dithers[channel].sn++;
			if (dithers[channel].sn < resample)
				continue;
			sample = (int) (dithers[channel].accum / resample);
			switch (requestedBytes) {
			case 1:
			case 2:
				break;
			case 4:
				sample >>= 8;
			case 3:
				sample = convert24to16(sample, dithers[channel]); // TODO actually should take param like 32 to 16
				break;
			default:
				throw new IllegalArgumentException("Unsupported sample bits depth :" + requestedBytes);
			}
			dithers[channel].sn = 0;
			dithers[channel].accum = 0;
			buf[rp++] = (byte) (sample & 0xff);
			sample >>= 8;
			buf[rp++] = (byte) (sample & 0xff);
			channel ^= 1;
		}
		return origLine.write(buf, off, rp) * requestedBytes / 2;
	}

	@Override
	public Object invoke(Object proxy, Method met, Object[] args) throws Throwable {
		if ("write".equals(met.getName())) {
			return write((byte[]) args[0], (int) args[1], (int) args[2]);
		} else if("open".equals(met.getName())) {
			if (line.isOpen())
				return null;
		} /*else if("close".equals(met.getName())) {
			System.err.printf("Free resources and get statistics%n");
			printStatistics();
			}*/
		return met.invoke(origLine, args);
	}

	// TODO add dithering for downsampling
	public static int samplesToBytes(int inbps, int[] samples, int len, byte[] bytes, int outbps, boolean twoChannels,
			boolean littleEndian) {
		if (littleEndian == false || bytes.length < len * outbps || outbps < 1 || outbps > 4 || inbps < outbps
				|| samples.length < len)
			throw new IllegalArgumentException("Insufficient buffer size, or wrong sampling depth is requiested");
		int ti = 0;
		int channel = 0;
		int seq = twoChannels ? 1 : 0;
		int shift = 8 * (inbps - outbps);
		//int mask = -1 << shift;
		for (int si = 0; si < len; si++) {
			int s = samples[si];
			//System.err.printf("%d / %x", s, s);
			s >>= shift; // down sampler
			// TODO assert if sample value greater than max for bps
			int bi = ti;
			ti += outbps;
			while (bi < ti) {
				bytes[bi++] = (byte) (s & 0xff);
				s >>= 8;
			}
			/*System.err.printf(" - %x %x%n", bytes[ti-2], bytes[ti-1]);
			if (si > 50 && s<0)
				throw new RuntimeException("Stop testing");*/
			channel ^= seq;
		}
		return ti;
	}

	void printStatistics() {
		System.err.printf("Peak %d(%x) - %d, noise %d(%x) - %d%n", peak, peak, peakCnt, low, low, lowCnt);
	}

	static long prng(int state) {
		return (state * 0x0019660dL + 0x3c6ef35fL) & 0xffffffffL;
	}

	static int convert24to16(int sample, DitherState dither) {
		final int MIN = -8388608;
		final int MAX = 8388607;
		int output, random;

		/* noise shape */
		sample += dither.error[0] - dither.error[1] + dither.error[2];

		dither.error[2] = dither.error[1];
		dither.error[1] = dither.error[0] / 2;

		/* bias */
		output = sample + 0x80;
		/* dither */
		random = (int) prng(dither.random);
		output += (random & 0xff) - (dither.random & 0xff);

		dither.random = random;

		/* clip */
		if (output > MAX) {
			output = MAX;

			if (sample > MAX)
				sample = MAX;
		} else if (output < MIN) {
			output = MIN;

			if (sample < MIN)
				sample = MIN;
		}

		/* error feedback */
		dither.error[0] = sample - (output & 0xffffff00);

		/* scale */
		return output >> 8;
	}

	static class DitherState {
		int random;
		int[] error = new int[3];
		long accum;
		int sn;
	}

}
