/* PhotoOrganizer 
 * Copyright (C) 1999 Dmitry Rogatkin.  All rights reserved.
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
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
import java.util.Date;

import javax.swing.Icon;

import mediautil.gen.FileFormatException;
import mediautil.gen.Rational;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.LLJTran;

public class SampleJpeg extends BasicJpeg {
	static final String SAMPLEFILEPATH = "DCS002";

	public SampleJpeg() {
		super(new File(SAMPLEFILEPATH), null);
		valid = true;
		try {
			imageinfo = new SampleImageInfo(null, null, 0, SAMPLEFILEPATH, "Comment field", this);
		} catch (FileFormatException ffe) {
			ffe.printStackTrace();
			valid = false;
		}
	}

	protected void read() {
	}

	public class SampleImageInfo extends AbstractImageInfo<LLJTran> {

		public SampleImageInfo(InputStream is, byte[] data, int offset, String name, String comments, LLJTran format)
				throws FileFormatException {
			super(is, data, offset, name, comments, format);
		}

		public String getFormat() {
			return Exif.FORMAT;
		}

		public void readInfo() {
			// no any real Jpeg marker information is here, so
			data = null; // for gc
		}

		public int getResolutionX() {
			return 1600;
		}

		public int getResolutionY() {
			return 1200;
		}

		public int getMetering() {
			return 1;
		}

		public int getExpoProgram() {
			return 1;
		}

		public String getMake() {
			return "Kodak";
		}

		public String getModel() {
			return "DC265";
		}

		public String getDataTimeOriginalString() {
			return dateformat.format(new Date());
		}

		public float getFNumber() {
			return 5.6f;
		}

		public Rational getShutter() {
			return new Rational(1, 250);
		}

		public boolean isFlash() {
			return false;
		}

		public float getFocalLength() {
			return 113.2f;
		}

		public String getQuality() {
			return "FINE";
		}

		public String getReport() {
			return NA;
		}

		public Icon getThumbnailIcon(Dimension size) {
			return null;
		}
	}
}
