/* MediaChest - $RCSfile: BasicIo.java,v $ 
 * Copyright (C) 1999-2002 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: BasicIo.java,v 1.2 2004/02/14 06:56:18 rogatkin Exp $
 */
package photoorganizer.formats;

import java.io.*;
import java.util.*;

public class BasicIo {
    static final byte  M_SOF0   = (byte)0xC0; // Start Of Frame N
    static final byte  M_SOF1   = (byte)0xC1; // N indicates which compression process
    static final byte  M_SOF2   = (byte)0xC2; // Only SOF0-SOF2 are now in common use 
    static final byte  M_SOF3   = (byte)0xC3;
    public final byte  M_DHT    = (byte)0xC4;
    static final byte  M_SOF5   = (byte)0xC5; // NB: codes C4 and CC are NOT SOF markers
    static final byte  M_SOF6   = (byte)0xC6;
    static final byte  M_SOF7   = (byte)0xC7;
    static final byte  M_JPG    = (byte)0xC8;
    static final byte  M_SOF9   = (byte)0xC9;
    static final byte  M_SOF10  = (byte)0xCA;
    static final byte  M_SOF11  = (byte)0xCB;
    static final byte  M_SOF13  = (byte)0xCD;
    static final byte  M_SOF14  = (byte)0xCE;
    static final byte  M_SOF15  = (byte)0xCF;
    static final byte  M_RST0   = (byte)0xD0;
    static final byte  M_RST1   = (byte)0xD1;
    static final byte  M_RST2   = (byte)0xD2;
    static final byte  M_RST3   = (byte)0xD3;
    static final byte  M_RST4   = (byte)0xD4;
    static final byte  M_RST5   = (byte)0xD5;
    static final byte  M_RST6   = (byte)0xD6;
    static final byte  M_RST7   = (byte)0xD7;
    static final byte  M_SOI    = (byte)0xD8; // Start Of Image (beginning of datastream)
    static final byte  M_EOI    = (byte)0xD9; // End Of Image (end of datastream)
    static final byte  M_SOS    = (byte)0xDA; // Start Of Scan (begins compressed data)
    public final byte  M_DQT    = (byte)0xDB;
    public final byte  M_DNL    = (byte)0xDC;
    public final byte  M_DRI    = (byte)0xDD;
    public final byte  M_DHP    = (byte)0xDE;
    public final byte  M_EXP    = (byte)0xDF;
    static final byte  M_APP0   = (byte)0xE0; // Application-specific marker, type N
    static final byte  M_APP12  = (byte)0xEC; // (we don't bother to list all 16 APPn's)
    static final byte  M_COM    = (byte)0xFE; // COMment
    static final byte  M_PRX    = (byte)0xFF; // Prefix

    int i2bsI(int offset, int value, int length) { // for Intel
		for (int i=0,s=0;i<length;i++,s+=8)
			data[offset+i] = (byte)(value >> s);
		return offset+length;
	}

	int bs2i(int offset, int length) {
		int val = 0;
		for (int i=0;i<length;i++)
			val = (val<<8) + (data[offset+i] & 255);
		return val;
	}

	int s2n(int offset, int length) {
		return s2n(offset, length, false);
	}
	
	int s2n(int offset, int length, boolean signed) {
		int val = 0;
		if (intel) { 
			int shift = 0;
			for (int i=offset;i<(length+offset) && i<data.length;i++) {
				val = val + ((data[i] & 255) << shift);
				shift += 8;
			}
		} else if (motorola) { 
			for (int i=0;i<length;i++)
				val = (val<<8) + (data[offset+i] & 255);
		}
		if (signed) { 
			int msb = 1 << (8*length - 1);
			if ((val & msb) > 0)
				val = val - (msb << 1);
		}
		return val;
	}
	
	void n2s(byte[] result, int offset, int value, int length) {
		if (motorola) {
			for(int i=0; i<length; i++) {
				result[offset+length-i-1] = (byte)(value & 255);
				value >>= 8;
			}
		} else {
			for(int i=0; i<length; i++) {
				result[offset+i] = (byte)(value & 255);
				value >>= 8;
			}
		}
	}

	void bn2s(byte[] result, int offset, int value, int length) {
		for(int i=0; i<length; i++) {
			result[offset+length-i-1] = (byte)(value & 255);
			value >>= 8;
		}
	}

	byte[] n2s(int value, int length) {
		byte[] result = new byte[length];
		n2s(result, 0, value, length);
		return result;
	}
	
	byte[] bn2s(int value, int length) {
		byte[] result = new byte[length];
		bn2s(result, 0, value, length);
		return result;
	}

	String s2a(int offset, int length) {
		//int endpos;
		//for (endpos = offset; endpos < offext+length && data[pos] != 0; endpos++);
		String result = null;
		try {
			//result = new String(data, offset, endpos-offext, "Default"));
			result = new String(data, offset, length, "Default");
		} catch(UnsupportedEncodingException e) {
		}
		//if (endpos < offext+length) {
		//    offset
		//}
		return result;
	}

	boolean isSignature(int offset, String signature) {
		for (int i=0; i<signature.length(); i++) {
			if (signature.charAt(i) != (data[offset+i] & 255))
				return false;
		}
		return true;
	}

	public static void skip(InputStream is, long n) throws IOException {
		if (n == 0) 
			return;
		long lefttoskip = n;
		do {
			lefttoskip -= is.skip(lefttoskip);
		} while (lefttoskip > 0);
	}

	public static int read(InputStream is, byte [] data) throws IOException {
		int lefttoread = data.length;
		int rl;
		while (lefttoread > 0 && lefttoread <= data.length) {
			rl = is.read(data, data.length-lefttoread, lefttoread);
			if (rl < 0)
				return data.length-lefttoread;
			lefttoread -= rl;
		}
		return data.length ;
	}

	public static String convertLength(long l) {
		if (l/1024/1024/1024 > 0)
			return ""+(l/1024/1024/1024)+/*','+((l%(1024*1024*1024))/1024/1024)+*/"GB";
		else if (l/1024/1024 > 0)
			return ""+(l/1024/1024)+/*','+((l%(1024*1024))/1024)+*/"MB";
		else if ((l/1024) > 0)
			return ""+(l/1024)+/*','+(l%1024)+*/"KB";
		else
			return ""+l;
	}

	boolean intel, motorola;

	byte[] data;
}