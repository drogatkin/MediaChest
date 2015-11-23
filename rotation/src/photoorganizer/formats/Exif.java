/* MediaChest - $RCSfile: Exif.java,v $
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
 *  $Id: Exif.java,v 1.1 2003/09/05 08:24:37 rogatkin Exp $
 */
/**
 * For building this class were used the following sources
 * 1. Thierry Bousch <bousch@topo.math.u-psud.fr>
 * 2. ISO/DIS 12234-2
 *    Photography - Electronic still picture cameras - Removable Memory
 *    Part 2: Image data format - TIFF/EP (http://www.pima.net/it10a.htm)
 * 3. <a href="http://www.pima.net/standards/it10/PIMA15740/exif.htm"> some enhancements were based on </a>
 */
package photoorganizer.formats;

import java.io.*;
import java.util.*;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.image.*;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;

// TODO: add loading custom/manufac exif specific properties from
// database in form of XML file
// where primary key is combination make+model
public class Exif extends AbstractImageInfo {
    public final static String FORMAT = "Exif";
	public static final byte[] EXIF_MARK = {0x45,0x78,0x69,0x66,0,0};
    static final int FIRST_IFD_OFF = 6;
    static final int MIN_JPEG_SIZE = 100;
    // Exif directory tag definition
    public final static int NEWSUBFILETYPE = 0xFE;
    public final static int IMAGEWIDTH =  0x100;
    public final static int IMAGELENGTH = 0x101;
    public final static int BITSPERSAMPLE = 0x102;
    public final static int COMPRESSION = 0x103;
    public final static int PHOTOMETRICINTERPRETATION = 0x106;
    public final static int FILLORDER = 0x10A;
    public final static int DOCUMENTNAME = 0x10D;
    public final static int IMAGEDESCRIPTION = 0x10E;
    public final static int MAKE = 0x10F;
    public final static int MODEL = 0x110;
    public final static int STRIPOFFSETS = 0x111;
    public final static int ORIENTATION = 0x112;
    public final static int SAMPLESPERPIXEL = 0x115;
    public final static int ROWSPERSTRIP = 0x116;
    public final static int STRIPBYTECOUNTS = 0x117;
    public final static int XRESOLUTION = 0x11A;
    public final static int YRESOLUTION = 0x11B;
    public final static int PLANARCONFIGURATION = 0x11C;
    public final static int RESOLUTIONUNIT = 0x128;
    public final static int TRANSFERFUNCTION = 0x12D;
    public final static int SOFTWARE = 0x131;
    public final static int DATETIME = 0x132;
    public final static int ARTIST = 0x13B;
    public final static int WHITEPOINT = 0x13E;
    public final static int PRIMARYCHROMATICITIES = 0x13F;
    public final static int SUBIFDS = 0x14A;
    public final static int JPEGTABLES = 0x15B;
    public final static int TRANSFERRANGE = 0x156;
    public final static int JPEGPROC = 0x200;
    public final static int JPEGINTERCHANGEFORMAT = 0x201;
    public final static int JPEGINTERCHANGEFORMATLENGTH = 0x202;
    public final static int YCBCRCOEFFICIENTS = 0x211;
    public final static int YCBCRSUBSAMPLING = 0x212;
    public final static int YCBCRPOSITIONING = 0x213;
    public final static int REFERENCEBLACKWHITE = 0x214;
    public final static int CFAREPEATPATTERNDIM = 0x828D;
    public final static int CFAPATTERN = 0x828E;
    public final static int BATTERYLEVEL = 0x828F;
    public final static int COPYRIGHT = 0x8298;
    public final static int EXPOSURETIME = 0x829A;
    public final static int FNUMBER = 0x829D;
    public final static int IPTC_NAA = 0x83BB;
    public final static int EXIFOFFSET = 0x8769;
    public final static int INTERCOLORPROFILE = 0x8773;
    public final static int EXPOSUREPROGRAM = 0x8822;
    public final static int SPECTRALSENSITIVITY = 0x8824;
    public final static int GPSINFO = 0x8825;
    public final static int ISOSPEEDRATINGS = 0x8827;
    public final static int OECF = 0x8828;
    public final static int EXIFVERSION = 0x9000;
    public final static int DATETIMEORIGINAL = 0x9003;
    public final static int DATETIMEDIGITIZED = 0x9004;
    public final static int COMPONENTSCONFIGURATION = 0x9101;
    public final static int COMPRESSEDBITSPERPIXEL = 0x9102;
    public final static int SHUTTERSPEEDVALUE = 0x9201;
    public final static int APERTUREVALUE = 0x9202;
    public final static int BRIGHTNESSVALUE = 0x9203;
    public final static int EXPOSUREBIASVALUE = 0x9204;
    public final static int MAXAPERTUREVALUE = 0x9205;
    public final static int SUBJECTDISTANCE = 0x9206;
    public final static int METERINGMODE = 0x9207;
    public final static int LIGHTSOURCE = 0x9208;
    public final static int FLASH = 0x9209;
    public final static int FOCALLENGTH = 0x920A;
    public final static int MAKERNOTE = 0x927C;
    public final static int USERCOMMENT = 0x9286;
    public final static int SUBSECTIME = 0x9290;
    public final static int SUBSECTIMEORIGINAL = 0x9291;
    public final static int SUBSECTIMEDIGITIZED = 0x9292;
    public final static int FLASHPIXVERSION = 0xA000;
    public final static int COLORSPACE = 0xA001;
    public final static int EXIFIMAGEWIDTH = 0xA002;
    public final static int EXIFIMAGELENGTH = 0xA003;
    public final static int INTEROPERABILITYOFFSET = 0xA005;
    public final static int FLASHENERGY = 0xA20B;               //  = 0x920B in TIFF/EP
    public final static int SPATIALFREQUENCYRESPONSE = 0xA20C;  //  = 0x920C    -  -
    public final static int FOCALPLANEXRESOLUTION = 0xA20E;     //  = 0x920E    -  -
    public final static int FOCALPLANEYRESOLUTION = 0xA20F;     //  = 0x920F    -  -
    public final static int FOCALPLANERESOLUTIONUNIT = 0xA210;  //  = 0x9210    -  -
    public final static int SUBJECTLOCATION = 0xA214;           //  = 0x9214    -  -
    public final static int EXPOSUREINDEX = 0xA215;             //  = 0x9215    -  -
    public final static int SENSINGMETHOD = 0xA217;             //  = 0x9217    -  -
    public final static int FILESOURCE = 0xA300;
    public final static int SCENETYPE = 0xA301;

    // Exif directory type of tag definition
    public final static int BYTE = 1;
    public final static int ASCII = 2;
    public final static int SHORT = 3;
    public final static int LONG = 4;
    public final static int RATIONAL = 5;
    public final static int SBYTE = 6;
    public final static int UNDEFINED = 7;
    public final static int SSHORT = 8;
    public final static int SLONG = 9;
    public final static int SRATIONAL = 10;

    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String EXT_BMP = "bmp";
    public static final String EXT_JPEG = "Jpeg";
	// TODO: read names from XML database on camera vendor
    public final static String [] EXPOSURE_PROGRAMS = { "P0",
	"P1", "Normal", "P3", "P5" };

    public final static String [] METERING_MODES = { "P0",
	"P1", "Normal", "P3", "PATTERN" };

    final static int DIR_ENTRY_SIZE = 12;

    public final static int [] TYPELENGTH = { 1, 1, 2, 4, 8, 1, 1, 2, 4, 8 };
	// TODO: consider replacing String name to java.io.File file
    public Exif(InputStream is, byte[] data, int offset, String name, String comments) throws FileFormatException {
        super(is, data, offset, name, comments);
        // a unusual problem is here
        // no own variables are initialized here
        // but super's constructor calls our method read, which is using
        // uninitialized local variables, so they are moved to parent
    }

    public Exif() {
		ifds = new IFD[2] ;
	intel = true;
	version = 2;
    }

    public String getFormat() {
        return FORMAT;
    }

    public static byte[] getMarkerData() {
        return new byte[] {
            (byte)0xFF, (byte)0xE1, 0, 40, (byte)0x45, (byte)0x78,
            (byte)0x69, (byte)0x66, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x49, (byte)0x2A, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x01, (byte)0x00,
            (byte)0x0F, (byte)0x01, (byte)0x02, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)26, 0, 0, 0,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)'F', (byte)'A', (byte)'K', (byte)'E', (byte)0x00, 0
        };
    }

    public Entry getTagValue(int tag, boolean main) {
        return getTagValue(new Integer(tag), -1, main);
    }

    public Entry getTagValue(Integer tag, int subTag, boolean main) {

		return ifds[main?0:1]!=null?ifds[main?0:1].getEntry(tag, subTag):null;
    }

    public void setTagValue(int tag, int subTag, Entry value, boolean main) {
        ifds[main?0:1].setEntry(new Integer(tag), subTag, value);
    }

    int getThumbnailLength() {
        Entry e = getTagValue(JPEGINTERCHANGEFORMATLENGTH, false);
        if (e == null)
            return -1;
        return ((Integer)e.getValue(0)).intValue();
    }

    int getThumbnailOffset() {
        Entry e = getTagValue(JPEGINTERCHANGEFORMAT, false);
        if (e == null)
            return -1;
        return ((Integer)e.getValue(0)).intValue();
    }

    public boolean saveThumbnailImage(StrippedJpeg im, OutputStream os) throws IOException {
        if (os == null || im == null)
            return false;
        boolean success = false;
        int length;
        int offset = getThumbnailOffset();
        if (offset > 0) {
            length = getThumbnailLength();
            if (length  > MIN_JPEG_SIZE) {
                InputStream is = im.createInputStream();
                byte[] image = new byte[length];
                skip(is, super.offset+offset+FIRST_IFD_OFF);
                read(is, image);
                is.close();
                int jpeg_offset = 0;
                while(!(image[jpeg_offset] == M_PRX && image[jpeg_offset+1] == M_SOI) && jpeg_offset < image.length-1)
                    jpeg_offset++; // skip garbage in begining including padding FF
                if (image.length-jpeg_offset > MIN_JPEG_SIZE) {
                    // if image can be consider as JPEG
                    os.write(image, jpeg_offset, image.length-jpeg_offset);
                    success = true;
                }
            }
        } else {
            // save as BMP
			// TODO: add BMP rotation
            Entry e = getTagValue(STRIPOFFSETS, false);
            if (e != null) {
                InputStream is = im.createInputStream();
                offset = ((Integer)e.getValue(0)).intValue();
                if (offset > 0) {
                    skip(is, offset);
                    e = getTagValue(STRIPBYTECOUNTS, false);
                    if (e != null) {
                        length = ((Integer)e.getValue(0)).intValue();
                        int imgwidth =0, imglength= 0;
                        e = getTagValue(IMAGEWIDTH, false);
                        if (e != null)
                            imgwidth = ((Integer)e.getValue(0)).intValue();
                        e = getTagValue(IMAGELENGTH, false);
                        if (e != null)
                            imglength = ((Integer)e.getValue(0)).intValue();
                        int bitspix = 8;
                        e = getTagValue(BITSPERSAMPLE, false);
                        if (e != null)
                            bitspix = ((Integer)e.getValue(0)).intValue();
                        int simpleperpix = 3;
                        e = getTagValue(SAMPLESPERPIXEL, false);
                        if (e != null)
                            simpleperpix = ((Integer)e.getValue(0)).intValue();
                        data = new byte[BMP24_HDR_SIZE];
                        System.arraycopy(BMP_SIG, 0, data, 0, BMP_SIG.length); offset = 2;
                        int scanline_len = (imgwidth*simpleperpix+3) & (-1<<2);
                        offset = i2bsI(offset, BMP24_HDR_SIZE+
                            scanline_len*imglength, 4);
                        offset = i2bsI(offset, 0, 4); // reserved
                        offset = i2bsI(offset, BMP24_HDR_SIZE, 4); // headersize (offset bits)
                        offset = i2bsI(offset, 0x28, 4); // infoSize
                        offset = i2bsI(offset, imgwidth, 4); // width
                        offset = i2bsI(offset, imglength, 4); // length
                        offset = i2bsI(offset, 1, 2); // biPlanes
                        offset = i2bsI(offset, simpleperpix*bitspix, 2); // bits
                        offset = i2bsI(offset, 0, 4); // biCompression
                        offset = i2bsI(offset, scanline_len*imglength, 4); // biSizeImage
                        offset = i2bsI(offset, 2834, 4); // biXPelsPerMeter
                        offset = i2bsI(offset, 2834, 4); // biYPelsPerMeter
                        offset = i2bsI(offset, 0, 4); // biClrUsed
                        offset = i2bsI(offset, 0, 4); // biClrImportant
                        os.write(data);
                        data = new byte[length];
                        read(is, data);
                        int filler = scanline_len - imgwidth*simpleperpix;
                        scanline_len = imgwidth*simpleperpix;
                        byte[] filldata = null;
                        if (filler != 0)
                            filldata = new byte[filler];
                        for(offset = length - scanline_len; offset >= 0; offset-=scanline_len) {
                            //os.write(data, offset, scanline_len);
                            for (int ro=0; ro<scanline_len; ro+=3) {
                                os.write(data[offset+ro+2]);
                                os.write(data[offset+ro+1]);
                                os.write(data[offset+ro]);
                            }
                            if (filler != 0)
                                os.write(filldata);
                        }
                    }
                }
                success = true;
            }
        }
        if (success == false)
            return super.saveThumbnailImage(im, os);
		return true;
    }

    public String getThumbnailExtension() {
        return (getThumbnailOffset() > 0)?EXT_JPEG:EXT_BMP;
    }

	public Icon getThumbnailIcon(StrippedJpeg im, Dimension size) {
		int length;
		int offset = getThumbnailOffset();
		if (offset > 0) {
			length = getThumbnailLength();
			if (length > MIN_JPEG_SIZE) { // since no relaible algorithm to be sure that thumbnail exists for TIFF
				int jpeg_offset = 0;
				try {
					InputStream is = im.createInputStream();
					byte[] image = new byte[length];
					skip(is, super.offset+offset+FIRST_IFD_OFF);
					is.read(image);
					is.close();
					while(!(image[jpeg_offset] == M_PRX && image[jpeg_offset+1] == M_SOI) && jpeg_offset < image.length-1)
						jpeg_offset++; // skip garbage in begining including padding FF
					if (jpeg_offset < image.length-MIN_JPEG_SIZE)
						return new ImageIcon(Toolkit.getDefaultToolkit().createImage(image, jpeg_offset, image.length-jpeg_offset));
				} catch (IOException e) {
				} catch (ArrayIndexOutOfBoundsException e) {
					  System.err.println("Bad index "+jpeg_offset+" for "+getName());
				}
			}
		} else {
			Entry e = getTagValue(STRIPOFFSETS, false);
			if (e != null) {
				InputStream is = im.createInputStream();
				offset = ((Integer)e.getValue(0)).intValue();
				if (offset > 0) {
					try {
						skip(is, offset);
						e = getTagValue(STRIPBYTECOUNTS, false);
						if (e != null) {
							length = ((Integer)e.getValue(0)).intValue();
							data = new byte[length];
							read(is, data);
							int imgwidth =0, imglength= 0;

							e = getTagValue(IMAGEWIDTH, false);
							if (e != null)
								imgwidth = ((Integer)e.getValue(0)).intValue();
							e = getTagValue(IMAGELENGTH, false);
							if (e != null)
								imglength = ((Integer)e.getValue(0)).intValue();
							int bitspix = 8;
							e = getTagValue(BITSPERSAMPLE, false);
							if (e != null)
								bitspix = ((Integer)e.getValue(0)).intValue();
							int simpleperpix = 3;
							e = getTagValue(SAMPLESPERPIXEL, false);
							if (e != null)
								simpleperpix = ((Integer)e.getValue(0)).intValue();
							// 1. transfer image to int array
							int [] image = new int[imgwidth*imglength];
							for (int i = 0; i < image.length; i++) {
								image[i] = ((data[i*3] & 255) <<16) + ((data[i*3+1] & 255) <<8) + (data[i*3+2] & 255) + 0xFF000000;
							}
							MemoryImageSource mis = new MemoryImageSource(imgwidth, imglength, image, 0, imgwidth);
							Image img = Toolkit.getDefaultToolkit().createImage(mis);
							image = null;
							is.close();
							return new ImageIcon(img);
						}
					} catch (IOException x) {
						x.printStackTrace();
					}
				}
			}
		}
		System.err.println("Embedded thumbnail not found for "+im.getFile());
		return null;
	}

    public int getResolutionX() {
        Entry e = getTagValue(EXIFIMAGEWIDTH, true);
        if (e != null)
            return ((Integer)e.getValue(0)).intValue();
        return -1;
    }

	public void setResolutionX(int xRes) {
		Entry e = getTagValue(EXIFIMAGEWIDTH, true);
		if (e == null) {
			e = new Entry(LONG);
			setTagValue(EXIFIMAGEWIDTH, 0, e, true);
		}
		e.setValue(0, new Integer(xRes));
	}

    public int getResolutionY() {
        Entry e = getTagValue(EXIFIMAGELENGTH, true);
        if (e != null)
            return ((Integer)e.getValue(0)).intValue();
        return -1;
    }

	public void setResolutionY(int yRes) {
		Entry e = getTagValue(EXIFIMAGELENGTH, true);
		if (e == null) {
			e = new Entry(LONG);
			setTagValue(EXIFIMAGELENGTH, 0, e, true);
		}
		e.setValue(0, new Integer(yRes));
    }

    public int getMetering() {
        Entry e = getTagValue(METERINGMODE, true);
        if (e != null)
            return ((Integer)e.getValue(0)).intValue();
        return 0;
    }

    public String getMeteringAsString() {
	int m = getMetering();
	if (m >= 0 && m < METERING_MODES.length)
		return METERING_MODES[m];
	return ""+m;
    }

    public int getExpoProgram() {
        Entry e = getTagValue(EXPOSUREPROGRAM, true);
        if (e != null)
            return ((Integer)e.getValue(0)).intValue();
        return 0;
    }

    public String getExpoProgramAsString() {
	int ep = getExpoProgram();
	if (ep >= 0 && ep < EXPOSURE_PROGRAMS.length)
		return EXPOSURE_PROGRAMS[ep];
	return ""+ep;
    }

    public String getMake() {
        Entry e = getTagValue(MAKE, true);
        if (e != null)
            return e.toString();
        return NA;
    }

    public String getModel() {
        Entry e = getTagValue(MODEL, true);
        if (e != null)
            return e.toString();
        return NA;
    }

    public String getDataTimeOriginalString() {
        Entry e = getTagValue(DATETIMEORIGINAL, true);
        if (e != null) {
            String result = e.toString();
            if (result.indexOf("0000:00:00") < 0)
            return result;
        }
        return dateformat.format(new Date());
    }

    public float getFNumber() {
        Entry e = getTagValue(FNUMBER, true);
        if (e != null)
            return ((Rational)e.getValue(0)).floatValue();
        e = getTagValue(APERTUREVALUE, true);
        if (e != null)
            return apertureToFnumber(((Rational)e.getValue(0)).floatValue());
        return -1;
    }

    public Rational getShutter() {
        Entry e = getTagValue(EXPOSURETIME, true);
        if (e != null)
            return (Rational)e.getValue(0);
        e = getTagValue(SHUTTERSPEEDVALUE, true);
        try {
            return TV_TO_SEC[(int)((Rational)e.getValue(0)).floatValue()];
        } catch(NullPointerException x) {
        } catch(ArrayIndexOutOfBoundsException x) {
        }
        return new Rational(0, 1);
    }

    public boolean isFlash() {
        Entry e = getTagValue(FLASH, true);
        if (e != null)
            return ((Integer)e.getValue(0)).intValue()==1;
        return false;
    }

	// TODO: make the coefficients camera specific
    public float getFocalLength() {
        Entry e = (Entry)getTagValue(FOCALLENGTH, true);
        if (e != null)
            return Math.round((float)(38*((Rational)e.getValue(0)).floatValue()/5.8));
        return 0;
    }

    public String getQuality() {
        // TODO: check MAKE nad read from XML database
        Entry e = getTagValue(COMPRESSEDBITSPERPIXEL, true);
        if (e == null)
            return "Unknown";
        switch (((Rational)e.getValue(0)).intValue()) {
        case 1: return "BASIC";
        case 2: return "NORMAL";
        case 4: return "FINE";
        }
        return getTagValue(COMPRESSEDBITSPERPIXEL, true).toString();
    }

    public String getReport() {
        StringBuffer report = new StringBuffer();
        Entry e = getTagValue(EXPOSURETIME, true);
        report.append("Shutter: ");
        if (e != null)
            report.append(e.toString());
        else {
            e = getTagValue(SHUTTERSPEEDVALUE, true);
            if (e != null)
            {
                report.append(e.toString());
            }
            else
                report.append(NA);
        }
        report.append(", Aperture: ");
        e = getTagValue(FNUMBER, true);
        if (e == null)
        {
            e = getTagValue(APERTUREVALUE, true);
            if (e != null)
                report.append(fnumberformat.format(((Rational)e.getValue(0)).floatValue()*0.4+1));
            else
                report.append(NA);
        } else
            report.append(fnumberformat.format(((Rational)e.getValue(0)).floatValue()));
        report.append(", Flash: ");
        e = getTagValue(FLASH, true);
        if (e != null)
            report.append((((Integer)e.getValue(0)).intValue()==1)?YES:NO);
        else
            report.append(NA);
        return report.toString();
    }

    public void readInfo() {
		ifds = new IFD[2] ;
        offset-=data.length;
        intel = data[6] == 'I';
        motorola = data[6] == 'M';
        if (!(intel || motorola))
            return;
        version = s2n(8, 2);
        processAllIFDs();
        data = null; // for gc
    }

	public void writeInfo(StrippedJpeg im, OutputStream out, int op) throws IOException {
		writeInfo(im, out, op, "ISO8859_1");
	}
	
	/** writes modified or not Exif to file
	 * APP header and its length are not included
	 * so any wrapper should do that calculation
	 */
	public void writeInfo(StrippedJpeg im, OutputStream out, int op, String encoding) throws IOException {
		// TODO: this implementation takes twice memory than needed
		// it should be rewritten using byte[] and then copying to stream
		// version returning just byte[] is also very useful
		if (ifds == null)
			throw new IllegalStateException("EXIF data not filled.");
		switch(op) {
		case StrippedJpeg.TRANSPOSE:
		case StrippedJpeg.TRANSVERSE:
		case StrippedJpeg.ROT_90:
		case StrippedJpeg.ROT_270:
			Entry resY = getTagValue(EXIFIMAGELENGTH, true);
			if (resY != null) {				
				Object yVal = resY.getValue(0);
				Entry resX = getTagValue(EXIFIMAGEWIDTH, true);
				if (resX != null) {
					resY.setValue(0, resX.getValue(0));
					resX.setValue(0, yVal);
				}
			}
			// TODO: can be join with the loop below
			Entry eRes;
			for (int i=0; i<2; i++) {
				eRes = getTagValue(XRESOLUTION, i==0?true:false);
				setTagValue(XRESOLUTION, 0, getTagValue(YRESOLUTION, i==0?true:false), i==0?true:false);
				setTagValue(YRESOLUTION, 0, eRes, i==0?true:false);
			}
			break;
		case StrippedJpeg.ROT_180: 
		case StrippedJpeg.FLIP_H:
		case StrippedJpeg.FLIP_V:
		case StrippedJpeg.NONE:
		default:
		}

		// TODO: write to out FIRST_IFD_OFF bytes
		out.write(EXIF_MARK);
		if (intel) {
			out.write('I');
			out.write('I');
		} else {
			out.write('M');
			out.write('M');
		}
		out.write(n2s(version, 2));
		
		int emptySlot = EXIF_MARK.length+2;
		// write offset of IFD
		out.write(n2s(emptySlot, 4));
        for (int k = 0; k < 2; k++) {
			//System.err.println("--->IFD "+k+" offeset "+emptySlot);
			emptySlot = writeIfd(out, emptySlot, ifds[k], im, op, encoding);
		}
	}
	
	/** writes IFD from map and returns length
	 */
	protected int writeIfd(OutputStream out, int emptySlot, IFD ifd, StrippedJpeg im, int op, String encoding) throws IOException {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(1*1024);
			int ne = (ifd.getEntries()==null?0:ifd.getEntries().size()) + (ifd.getIFDs()==null?0:ifd.getIFDs().length);
			//System.err.println("ifd= "+Integer.toHexString(ifd.getTag())+" entries "+ne+" offset 0x"+Integer.toHexString(emptySlot));
			out.write(n2s(ne,2)); // num entries
			emptySlot += ne*DIR_ENTRY_SIZE+2+4; // num entries + next slot
			Iterator it = ifd.getEntries().entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry me = (Map.Entry)it.next();
				int tag = ((Integer)me.getKey()).intValue();
				if (tag == JPEGINTERCHANGEFORMATLENGTH)  // skip it
					continue; // not robust enough, since JPEGINTERCHANGEFORMAT never can be met
				Entry e = (Entry)me.getValue();
				// TODO: consider write(e.toByteArray(intel)
				out.write(n2s(tag, 2));
				int type;
				out.write(n2s(type = e.getType(), 2));
				//System.err.println("write type "+Integer.toString(type,16)+" tag "+Integer.toString(tag, 16)+
				//				   " tag vals "+rogatkin.DataConv.arrayToString(e.getValues(), ':'));
				if (type == ASCII) {
					byte [] str = e.toString().getBytes(encoding);
					out.write(n2s(str.length+1, 4));
					if (str.length+1 > 4) {
						out.write(n2s(emptySlot, 4));						
						buf.write(str); // buf used
						buf.write(0);
						emptySlot += str.length+1;
					} else { // write data
						out.write(str);
						if (str.length < 4) // write padding
							for (int i=0; i<4-str.length; i++) // shouldn't be a stopper
								out.write(0);
					}
				} else {
					Object[] vs = e.getValues(); // can vs be null ? or have length 0
					out.write(n2s(vs.length, 4));
					int tlen = TYPELENGTH[type-1];
					if (vs.length*tlen > 4) {
						out.write(n2s(emptySlot, 4));
						boolean signed = (SBYTE == 6 || type >= SSHORT);
						boolean rational = type % RATIONAL == 0;
						for (int i=0; i<vs.length; i++) {
							if (rational) {
								buf.write(n2s(((Rational)vs[i]).getNum(), 4));
								buf.write(n2s(((Rational)vs[i]).getDen(), 4));
								emptySlot += 8;
							} else {
								buf.write(n2s(((Integer)vs[i]).intValue(), tlen));
								emptySlot += tlen;
							}
						}
					} else {
						// no check type since to small for rational
						if (tag == JPEGINTERCHANGEFORMAT) {
							int length = getThumbnailLength();
							if (length  > MIN_JPEG_SIZE) {
								// TODO: image should be already in memory, so better to use it
								int jpeg_offset = 0;
								try {
									InputStream is = im.createInputStream();
									byte[] image = new byte[length];
									skip(is, super.offset+getThumbnailOffset()+FIRST_IFD_OFF);
									is.read(image);
									is.close();
									while(!(image[jpeg_offset] == M_PRX && image[jpeg_offset+1] == M_SOI) && jpeg_offset < image.length-1)
										jpeg_offset++; // skip garbage in begining including padding FF
									if (jpeg_offset < image.length-MIN_JPEG_SIZE) {
										ByteArrayInputStream tis = new ByteArrayInputStream(image, jpeg_offset, image.length-jpeg_offset);
										int l = buf.size();
										new StrippedJpeg(tis).transform(buf, op, false, null);
										l = buf.size() - l;
										//System.err.println("New tn len:"+Integer.toHexString(l));
										out.write(n2s(emptySlot, 4));
										emptySlot += l;
										tis = null;
										// TODO: update emptySlot, and JPEGINTERCHANGEFORMATLENGTH
										// since JPEGINTERCHANGEFORMATLENGTH can be met before, the value should be skipped
										// and be written only after
										out.write(n2s(JPEGINTERCHANGEFORMATLENGTH, 2));
										out.write(n2s(LONG, 2));
										out.write(n2s(1, 4));
										out.write(n2s(l, 4));
									}
									
								}catch(Throwable t) {
									t.printStackTrace();
								}
							}
						} else {
							for (int i=0; i<vs.length; i++) {
								out.write(n2s(((Integer)vs[i]).intValue(), tlen));
							}
							if (vs.length*tlen < 4)
								for (int i=0; i<4-vs.length*tlen; i++) // shouldn't be a stopper
									out.write(0);
						}
					}
				}
			}
			// write IFDs
			IFD []ifds = ifd.getIFDs();
			for (int k=0; ifds != null && k<ifds.length; k++) {
				IFD ifd1 = ifds[k];
				out.write(n2s(ifd1.getTag(), 2));
				out.write(n2s(ifd1.getType(), 2));
				out.write(n2s(1, 4));
				out.write(n2s(emptySlot, 4));
				emptySlot = writeIfd(buf, emptySlot, ifd1, im, op, encoding);
			}
			// next IFD
			out.write(n2s(emptySlot, 4));
			// write data
			out.write(buf.toByteArray());
			
			return emptySlot;
	}
	
    protected int firstIFD() {
		//System.err.println("FIFD "+(s2n(FIRST_IFD_OFF+4, 4)+FIRST_IFD_OFF));
        return s2n(FIRST_IFD_OFF+4, 4)+FIRST_IFD_OFF;
    }

    protected int nextIFD(int ifd) {
        int entries = s2n(ifd, 2);
        return s2n(ifd + 2 + DIR_ENTRY_SIZE * entries, 4)+FIRST_IFD_OFF;
    }

    protected void processAllIFDs() {
        int iifd = 0;
		for (int i = firstIFD(); i > FIRST_IFD_OFF && iifd < 2; i = nextIFD(i)) {
			ifds[iifd] = new IFD(iifd);
			storeIFD(i, ifds[iifd]);
			iifd++;
		}
    }

    protected void storeIFD(int ifdoffset, IFD ifd) {
        int entries = s2n(ifdoffset, 2);
		//System.err.println("Store off "+ifdoffset+" tag "+Integer.toHexString(ifd.getTag())+" entries "+entries);
        for (int i=0; i<entries; i++) {
            int entry = ifdoffset + 2 + DIR_ENTRY_SIZE*i;
            int tag = s2n(entry, 2);
            int type = s2n(entry+2, 2);
            if (type < 1 || type > 10)
                continue; // not handled
            int typelen = TYPELENGTH[type-1];
            int count = s2n(entry+4, 4);
            int offset = entry+8;
            if (count*typelen > 4)
                offset = s2n(offset, 4)+FIRST_IFD_OFF;
			//System.err.println("tag "+Integer.toHexString(tag)+" type "+type+" len "+ count +" off "+offset);
            if (type == ASCII) {
                // Special case: zero-terminated ASCII string
                try {
                    ifd.addEntry(tag, new Entry(type,
                     new String(data, offset, count-1, "Default")));
                } catch(UnsupportedEncodingException e) {
					System.err.println("storeIFD: getString() "+e);
                }
            } else {
                Object[] values = new Object[count];
                boolean signed = (SBYTE == 6 || type >= SSHORT);
				for (int j=0; j<count; j++) {
					if (type % RATIONAL != 0)
						// Not a fraction
						values[j] = new Integer(s2n(offset, typelen, signed));
					else
						// The type is either 5 or 10
						values[j] = new Rational(s2n(offset, 4, signed),
												 s2n(offset+4, 4, signed));
					offset += typelen;
					// Recent Fujifilm and Toshiba cameras have a little subdirectory
					// here, pointed to by tag 0xA005. Apparently, it's the
					// "Interoperability IFD", defined in Exif 2.1.
					if ((tag == EXIFOFFSET || tag == INTEROPERABILITYOFFSET /*|| tag == MAKERNOTE*/) 
						&& j == 0 && ((Integer)values[0]).intValue() > 0 
						) {
						IFD iifd;
						storeIFD(((Integer)values[0]).intValue()+FIRST_IFD_OFF, iifd = new IFD(tag, type));
						ifd.addIFD(iifd);
					} else 
						ifd.addEntry(tag, new Entry(type, values));
                }
            }
        }
    }

	public IFD[] getIFDs() {
		return ifds;
	}

	protected int currentimage;
    protected int version;
    protected IFD []ifds;
}
