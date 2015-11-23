/* MediaChest - $RCSfile: StrippedJpeg.java,v $
 * Copyright (C) 1999-2003 Dmitriy Rogatkin.  All rights reserved.
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
 *	$Id: StrippedJpeg.java,v 1.5 2005/08/13 04:14:26 rogatkin Exp $
 *
 * Some ideas and algorithms were borrowed from:
 * Thomas G. Lane, and James R. Weeks
 */
package photoorganizer.formats;

import java.io.*;
import java.util.*;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class StrippedJpeg extends BasicIo {
    public static final String JFIF = "JFIF";

    public static final String FPXR = "FPXR";

    public static final String JPEG = "JPEG";

    // TODO: move rotation constants to some interface
    public static final int NONE = 0;

    public static final int FLIP_H = 1;

    public static final int FLIP_V = 2;

    public static final int TRANSPOSE = 3;

    public static final int TRANSVERSE = 4;

    public static final int ROT_90 = 5;

    public static final int ROT_180 = 6;

    public static final int ROT_270 = 7;

    public static final int COMMENT = 8;

    public static final int DCTSIZE2 = 64;

    public static final int DCTSIZE = 8;

    public static final int HUFF_LOOKAHEAD = 8;

    public static final int BYTE_SIZE = 8;

    private static final int MAX_CLEN = 32;

    static final int jpegzigzagorder[] = { 0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7,
            13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24,
            31, 40, 44, 53, 10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46,
            51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61, 35, 36, 48, 49, 57, 58,
            62, 63 };

    static final int jpegnaturalorder[] = { 0, 1, 8, 16, 9, 2, 3, 10, 17, 24,
            32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13,
            6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23,
            30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47,
            55, 62, 63, };

    protected String artist;

    private static String enc;

    public StrippedJpeg(File file) {
        this.file = file;
        markerid = new byte[2];
        read();
    }

    public StrippedJpeg(InputStream inStream) {
        markerid = new byte[2];
        this.inStream = inStream;
    }

    public static void setEncoding(String enc) {
        StrippedJpeg.enc = enc;
    }

    public static String getEncoding() {
        return enc;
    }

    public boolean isValid() {
        return valid;
    }

    public String getLocationName() {
        return file == null ? null : file.getAbsolutePath();
    }

    public String getName() {
        if (file == null)
            return "Unknown/Stream";
        return file.getName();
    }

    public String toString() {
        return getName();
    }

    public String getParentPath() {
        return file == null ? null : file.getParent();
    }

    public long getFileSize() {
        return length();
    }

    public long length() {
        return file == null ? -1 : file.length();
    }

    public File getFile() {
        return file;
    }

    public java.net.URL getUrl() {
        if (file == null)
            return null;
        try {
            return file.toURL();
        } catch (java.net.MalformedURLException me) {
            return null;
        }
    }

    public Date getDateTimeOriginal() {
        return file == null ? new Date() : new Date(file.lastModified());
    }

    public boolean renameTo(File dest) {
        if (file == null)
            return false;
        if (file.renameTo(dest)) {
            file = dest;
            try {
                imageinfo.setName(file.getName());
            } catch (NullPointerException e) {
            }
            return true;
        }
        return false;
    }

    public InputStream createInputStream() {
        try {
            if (valid) {
                readcounter = 0;
                if (file == null) {
                    if (inStream != null)
                        return inStream;
                    else
                        valid = false;
                } else {
                    return new BufferedInputStream(new FileInputStream(file));
                }
            }
        } catch (FileNotFoundException e) {
            valid = false;
        }
        return null;
    }

    public InputStream getAsStream() {
        return createInputStream();
    }

    public Image getImage() {
        if (valid)
            return Toolkit.getDefaultToolkit().getImage(getLocationName());
        return null;
    }

    public AbstractImageInfo getImageInfo() {
        return imageinfo;
    }

    public AbstractInfo getInfo() {
        return getImageInfo();
    }

    public String getType() {
        return JPEG; //getImageInfo().getFormat();
    }

    public String getThumbnailType() {
        return getImageInfo().getThumbnailExtension();
    }

    public String getComment() {
        return out_comment;
    }

    public void setComment(String comment) {
        in_comment = comment;
        if (imageinfo != null)
            imageinfo.setAttribute(imageinfo.COMMENTS, in_comment);
    }

    // TODO: reconsider raising an exception instead of return code
    public boolean transform(String destname, int op) {
        return transform(destname, op, true);
    }

    public boolean transform(String destname, int op, boolean preserve_appxs) {
        try {
            return transform(new BufferedOutputStream(new FileOutputStream(
                    destname), 4096), op, preserve_appxs, null);
        } catch (FileNotFoundException fne) {
            System.err.println(fne + " in saving of " + getName());
        }
        return false;
    }

    public boolean transform(String destname, int op, boolean preserve_appxs,
            Class custom_appx) {
        try {
            return transform(new BufferedOutputStream(new FileOutputStream(
                    destname), 4096), op, preserve_appxs, custom_appx);
        } catch (FileNotFoundException fne) {
            System.err.println(fne + " in saving of " + getName());
        }
        return false;
    }

    public boolean transform(OutputStream outStream, int op,
            boolean preserve_appxs, Class custom_appx) {
        // added return exec condition instead passing up exceptions
        read(false, preserve_appxs);
        try {
            switch (op) {
            case TRANSPOSE:
            case TRANSVERSE:
            case ROT_90:
            case ROT_270:
                transposeImageParameters();
                transposeQTable();
                break;
            case ROT_180:
            case FLIP_H:
            case FLIP_V:
            case NONE:
            default:
            }
            save(outStream, op, custom_appx);
        } catch (IOException e) {
            System.err.println(e + " in saving of " + getName());
            e.printStackTrace();
            return false;
        } finally {
            freeMemory();
        }
        return true;
    }

    void save(OutputStream os, int op, Class custom_appx) throws IOException {
		if (canBeProcessed) {
            // Huffman tables are recalculated since there may be new coeffs
            // not present in the exisint Huffman Tables.
            if(huffGen == null)
                huffGen = new HuffGenerator();
            huffGen.init();
            gatheringStats = true;
             // Dry Run to get frequency counts of DC/AC symbols to be encoded
             // No output is written. The transforming routines like
             // rotate90DCT called by writeDCT also honour gatheringStats
             // variable and do not actually modify the DCT array.
            writeDCT(null, op);
            // Disable Stat gathering and enabling writeDCT to write output
            gatheringStats = false;
            generateHuffTables(huffGen); // Load the new Huffman tables
            huffGen.freeMemory();
        }
        writeMarkerSOI(os);
        // write format specific marker
        writeNewMarker(os, custom_appx);
        writeMarkerAppXs(os);
        if (op == COMMENT)
            writeMarkerComment(os, in_comment, enc);
        else
            writeMarkerComment(os, "Put your copyright here"
                    + (out_comment.length() == 0 ? "" : "\n") + out_comment,
                    enc);
        writeMarkerDQT(os);
        writeMarkerDHT(os);
        writeMarkerDRI(os);
        if (canBeProcessed) {
            writeMarkerSOF0(os);
            writeMarkerSOS(os);
            writeDCT(os, op);
            writeMarkerEOI(os);
        } else {
            writeUnprocessedMarkers(os);
            writeRawDCT(os);
        }
        os.flush();
        os.close();
        os = null;
    }

    public void setArtist(String val) {
        artist = val;
    }

    private void generateHuffTables(HuffGenerator huff) throws IOException
    {
        int c, htInfo;

        ByteArrayOutputStream bs = new ByteArrayOutputStream();

        /* Write out huff info */
        huff.writeHuffTables(bs);

        data = bs.toByteArray();

        /*Re-initialize Hufftables */
		dc_valoffset = new int[0][0];
		dc_maxcode = new int[0][0];
		dc_huffval = new int[0][0];
		enc_dc_matrix = new int[0][][];
		dc_huffbits = new int[0][0];
		dc_ix = new int[0];
		
		ac_valoffset = new int[0][0];
		ac_maxcode = new int[0][0];
		ac_huffval = new int[0][0];
		enc_ac_matrix = new int[0][][];
		ac_huffbits = new int[0][0];
		ac_ix = new int[0];
        readDHT(null, data.length);
    }

    private void freeMemory() {
        dct_coefs = null;
        dc_valoffset = null;
        dc_maxcode = null;
        dc_huffval = null;
        enc_dc_matrix = null;
        dc_huffbits = null;
        dc_ix = null;

        ac_valoffset = null;
        ac_maxcode = null;
        ac_huffval = null;
        enc_ac_matrix = null;
        ac_huffbits = null;
        ac_ix = null;
        q_table = null;
        q_ix = null;
        q_prec = null;
        // free extra storage for 
        appxs = null;
        tables = null;
        markers = null;
        rawDct = null;
    }

    void allocateTables() {
        // empty table creation
        dc_valoffset = new int[0][0];
        dc_maxcode = new int[0][0];
        dc_huffval = new int[0][0];
        enc_dc_matrix = new int[0][][];
        dc_huffbits = new int[0][0];
        dc_ix = new int[0];

        ac_valoffset = new int[0][0];
        ac_maxcode = new int[0][0];
        ac_huffval = new int[0][0];
        enc_ac_matrix = new int[0][][];
        ac_huffbits = new int[0][0];
        ac_ix = new int[0];
        q_table = new int[0][0];
        q_ix = new int[0];
        q_prec = new int[0];
    }

    void read() {
        read(true, false);
    }

    void read(boolean info_only, boolean keep_appxs) {
        out_comment = "";
        if (file != null)
            valid = file.isFile();
        else
            valid = inStream != null;
        if (!valid)
            return;
        int len;
        InputStream is = null;
        try {
            is = createInputStream();
            if (!valid)
                return;
            valid = false;
            canBeProcessed = true; //22
            markers: for (;;) {
                if (is.read(markerid) != markerid.length) {
                    // Wrong length read for marker header
                    break markers;
                }
                readcounter += markerid.length;
                if (markerid[0] != M_PRX) {
                    // Wrong start signature markerid[0]
                    if (readcounter == markerid.length) {
                        // try TIFF
                        intel = markerid[0] == 'I' && markerid[1] == 'I';
                        motorola = markerid[0] == 'M' && markerid[1] == 'M';
                        if (intel || motorola) {
                            data = new byte[6];
                            if (is.read(data) == data.length
                                    && ((intel && data[0] == 42 && data[1] == 0) || (motorola
                                            && data[1] == 42 && data[0] == 0))) {
                                readcounter += data.length;
                                imageinfo = new TiffExif(is, data, readcounter,
                                        getName(), intel);
                            }
                        } else if (markerid[0] == Flashpix.SIGNATURE[0]
                                && markerid[1] == Flashpix.SIGNATURE[1]) { // try Flashpix
                            data = new byte[6];
                            if (is.read(data) == data.length
                                    && data[0] == Flashpix.SIGNATURE[2]
                                    && data[1] == Flashpix.SIGNATURE[3]
                                    && data[2] == Flashpix.SIGNATURE[4]
                                    && data[3] == Flashpix.SIGNATURE[5]
                                    && data[4] == Flashpix.SIGNATURE[6]
                                    && data[5] == Flashpix.SIGNATURE[7]) {
                                readcounter += data.length;
                                // disable flashpix
                                /*try {
                                 imageinfo = new Flashpix(is, data, readcounter, getName());
                                 } catch(FileFormatException ffe) {
                                 }*/
                            }
                        }
                    }
                    valid = imageinfo != null;
                    break markers;
                }
                byte markercode = markerid[1];
                System.err.println("Doing marker "
                        + Integer.toHexString(markercode));
                switch (markercode) {
                case M_SOI:
                    // proceed to next marker
                    if (!info_only)
                        allocateTables();
                    break;
                case M_APP0:
                case M_APP0 + 1:
                case M_APP0 + 2:
                case M_APP0 + 3:
                case M_APP0 + 4:
                case M_APP0 + 5:
                case M_APP0 + 6:
                case M_APP0 + 7:
                case M_APP0 + 8:
                case M_APP0 + 9:
                case M_APP0 + 10:
                case M_APP0 + 11:
                case M_APP12:
                case M_APP12 + 1:
                case M_APP12 + 2:
                case M_APP12 + 3:
                    // application specific marker found, just skip it
                    if (!info_only) {
                        if (keep_appxs)
                            addAppx(len = readMarker(is), markercode);
                        else {
                            if (is.read(markerid) != markerid.length) {
                                // Wrong length read for marker header
                                break markers;
                            }
                            readcounter += markerid.length;
                            data = markerid;
                            len = bs2i(0, 2) - 2;
                            skip(is, len);
                            readcounter += len;
                            System.err.println("Marker APP"
                                    + ((markercode & 255) - (M_APP0 & 255))
                                    + " is found, skipped " + len + 2 + " ("
                                    + getLocationName() + ")");
                        }
                    } else {
                        len = readMarker(is);
                        if (keep_appxs)
                            addAppx(len, markercode);
                        valid = true;
                        System.err.println("Signature "
                                + new String(data, 0, 4));
                        if (isSignature(0, JFIF)) {
                            // x'FF', APP0, length, identifier, version, 
                            // units, Xdensity, Ydensity, Xthumbnail, Ythumbnail, (RGB)n
                            int version = bs2i(5, 2);
                            int units = bs2i(7, 1);
                            int xden = bs2i(8, 2);
                            int yden = bs2i(10, 2);
                            int x = bs2i(12, 1);
                            int y = bs2i(13, 1);
                            int thumbnailsize = 3 * x * y;
                            if (x > 0 && y > 0)
                                System.err.println("Thumbnail " + x + "x" + y
                                        + " in APP0");
                        } else if (isSignature(0, JFXX.FORMAT))
                            imageinfo = new JFXX(is, data, readcounter,
                                    getName(), out_comment);
                        else if (isSignature(0, Exif.FORMAT)) {
                            imageinfo = new Exif(is, data, readcounter,
                                    getName(), out_comment);
                            if (keep_appxs)
                                exifIndex = appxs.length - 1;
                        } else if ((isSignature(0, CIFF.II) || isSignature(0,
                                CIFF.MM))
                                && isSignature(6, CIFF.FORMAT))
                            imageinfo = new CIFF(is, data, readcounter,
                                    getName(), out_comment);
                        else if (isSignature(0, FPXR))
                            ;
                        else if (imageinfo == null) {
                            System.err.println("unhandled APP marker "
                                    + Integer.toHexString(markercode)
                                    + " length " + len + " data "
                                    + new String(data, 0, len));
                            //imageinfo = new JPEG(is, data, readcounter, getName(), out_comment, frm_x, frm_y);
                            //break markers; // no reason to read any markers after
                        }
                        System.err.println("Image info " + imageinfo);
                    }
                    break;
                case M_DQT:
                    if (info_only) {
                        valid = true;
                        data = markerid;
                        break markers;
                    }
                    len = readMarker(is);
                    int[] wt1d,
                    wt2d[];
                    int pos = 0;
                    int lim;
                    while (pos < len) {
                        int tabnum = q_prec.length;
                        wt1d = new int[tabnum + 1];
                        System.arraycopy(q_ix, 0, wt1d, 0, tabnum);
                        q_ix = wt1d;
                        q_ix[tabnum] = data[pos] & 15;
                        wt1d = new int[tabnum + 1];
                        System.arraycopy(q_prec, 0, wt1d, 0, tabnum);
                        q_prec = wt1d;
                        q_prec[tabnum] = ((data[pos++] >> 4) & 15) == 0 ? 8
                                : 16;
                        wt2d = new int[tabnum + 1][DCTSIZE2];
                        System.arraycopy(q_table, 0, wt2d, 0, tabnum);
                        q_table = wt2d;
                        lim = pos + DCTSIZE2;
                        if (lim > len)
                            lim = len;
                        for (int i = 0; pos < lim; i++)
                            q_table[tabnum][i] = data[pos++] & 255;
                    }
                    break;
                case M_DHT:
                    if (info_only) {
                        valid = true;
                        data = markerid;
                        break markers;
                    }
                    len = readDHT(is);
                    break;
                case M_SOF0:
                case M_SOF1:
                    if (info_only) {
                        valid = true;
                        data = markerid;
                        System.err.println("Abandoned M_SOF0 " + M_SOF0 + "   "
                                + markerid[1]);
                        break markers;
                    }
                    len = readMarker(is);
                    frm_precision = data[0] & 255;
                    frm_x = bs2i(3, 2);
                    frm_y = bs2i(1, 2);
                    components_in_frame = data[5] & 255;
                    System.err.println("Frame, precision " + frm_precision);
                    System.err.println("X= " + frm_x + ", Y= " + frm_y);
                    System.err.println("Components " + components_in_frame
                            + " (" + getLocationName() + ")");
                    V = new int[components_in_frame];
                    H = new int[components_in_frame];
                    QT = new int[components_in_frame];
                    ID = new int[components_in_frame];
                    pos = 6;
                    maxHi = maxVi = 0;
                    int sampling = 0;
                    mcusize = 0;
                    for (int i = 0; i < components_in_frame; i++) {
                        ID[i] = data[pos++] & 255;
                        sampling = ((sampling << 8) + (data[pos] & 255));
                        H[i] = (data[pos] >> 4) & 15;
                        if (H[i] > maxHi)
                            maxHi = H[i];
                        V[i] = data[pos++] & 15;
                        if (V[i] > maxVi)
                            maxVi = V[i];
                        mcusize += H[i] * V[i];
                        QT[i] = data[pos++] & 255;
                    }
                    break;
                case M_SOF2:
                    len = readMarker(is);
                    System.err.println("Progressive, Huffman not supported in "
                            + " (" + getLocationName() + ")");
                    canBeProcessed = false; //22
                    if (info_only == false)
                        addMarker(len, markercode);
                    break;
                case M_SOF9:
                    len = readMarker(is);
                    System.err
                            .println("Extended sequential, arithmetic not supported"
                                    + " (" + getLocationName() + ")");
                    canBeProcessed = false; //22
                    if (info_only == false)
                        addMarker(len, markercode);
                    break;
                case M_SOF10:
                    len = readMarker(is);
                    System.err.println("Progressive, arithmetic not supported"
                            + " (" + getLocationName() + ")");
                    canBeProcessed = false; //22
                    if (info_only == false)
                        addMarker(len, markercode);
                    break;
                case M_SOF3:
                case M_SOF5:
                case M_SOF6:
                case M_SOF7:
                case M_JPG:
                case M_SOF11:
                case M_SOF13:
                case M_SOF14:
                case M_SOF15:
                    len = readMarker(is);
                    System.err.println("One of the unsupported SOF markers:\n"
                            + "Lossless, Huffman\n"
                            + "Differential sequential, Huffman\n"
                            + "Differential progressive, Huffman\n"
                            + "Differential lossless, Huffman\n"
                            + "Reserved for JPEG extensions\n"
                            + "Lossless, arithmetic\n"
                            + "Differential sequential, arithmetic\n"
                            + "Differential progressive, arithmetic\n"
                            + "Differential lossless, arithmetic" + " ("
                            + getLocationName() + ")");
                    canBeProcessed = false; //22
                    if (info_only == false)
                        addMarker(len, markercode);
                    break;
                case M_SOS:
                    len = readMarker(is);
                    if (canBeProcessed) { //22 
                        components_in_scan = data[0] & 255;
                        pos = 1;
                        comp_ids = new int[components_in_scan];
                        dc_table = new int[components_in_scan];
                        ac_table = new int[components_in_scan];
                        for (int i = 0; i < components_in_scan; i++) {
                            comp_ids[i] = data[pos++] & 255;
                            dc_table[i] = (data[pos] >> 4) & 15;
                            ac_table[i] = data[pos++] & 15;
                        }
                        _Ss = data[pos++] & 255;
                        _Se = data[pos++] & 255;
                        _Ah = (data[pos] >> 4) & 15;
                        _Al = data[pos] & 15;
                        readDCT(is);
                    } else {
                        addMarker(len, markercode);
                        System.err.println("Read raw dct");
                        readRawDCT(is);
                        valid = true;
                        break markers;
                    }
                    break;
                case M_COM:
                    len = readMarker(is);
                    if (out_comment.length() > 0)
                        out_comment += '\n';
                    try {
                        out_comment += new String(data, 0, len, enc);
                    } catch (UnsupportedEncodingException uee) {
                        out_comment += new String(data, 0, len);
                    } catch (NullPointerException npe) {
                        out_comment += new String(data, 0, len);
                    }
                    break;
                case M_EOI:
                    valid = true;
                    break markers;
                case M_DRI:
                    if (info_only) {
                        valid = true;
                        data = markerid;
                        break markers;
                    }
                    len = readMarker(is);
                    if (len != 2)
                        throw new IOException("Wrong length of DRI marker "
                                + len + " (" + getLocationName() + ")");
                    restart_interval = bs2i(0, 2);
                    System.err.println("Restart interval " + restart_interval);
                    break;
                case M_PRX:
                    // skip 0xFF filling
                    break;
                default:
                    len = readMarker(is);
                    if ((0xfffffff0 & markercode) == 0xfffffff0)
                        break markers; // it's MP3
                    else
                        System.err.println("Unsupported marker "
                                + Integer.toHexString(markercode) + " length "
                                + len + " (" + getLocationName() + ")");
                }
            }
            if (valid) {
                if (info_only) {
                    if (imageinfo == null) // create some image info 
                        imageinfo = new JPEG(is, data, readcounter, getName(),
                                out_comment, frm_x, frm_y);
                } else
                    System.err.println("0x" + Integer.toHexString(readcounter)
                            + "(" + readcounter + ") byte(s) read in "
                            + getName());
            }
        } catch (Exception e) { // NullPointerException, IOException
            valid = false;

            e.printStackTrace();
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException ioe) {
                    // can't do much
                }
        }

    }

    private void addMarker(int len, byte markercode) {
        //System.err.println("Adding non processed marker "+Integer.toHexString(markercode)+" len "+len);
        if (tables == null)
            tables = new Hashtable();
        if (markers == null)
            markers = new byte[1];
        else {
            byte[] ta = new byte[markers.length + 1];
            System.arraycopy(markers, 0, ta, 0, markers.length);
            markers = ta;
        }
        markers[markers.length - 1] = markercode;
        byte[] marker = new byte[len + 4];
        marker[0] = M_PRX;
        marker[1] = markercode;
        // not relaible code, because we suppose to markerid contains a packed len
        System.arraycopy(markerid, 0, marker, 2, 2);
        System.arraycopy(data, 0, marker, 4, len);
        tables.put("" + markercode, marker);
    }

    private void addAppx(int len, byte markercode) {
        if (appxs == null)
            appxs = new byte[0][];
        byte[] ta[] = new byte[appxs.length + 1][];
        System.arraycopy(appxs, 0, ta, 0, appxs.length);
        appxs = ta;
        appxs[appxs.length - 1] = new byte[len + 4];
        appxs[appxs.length - 1][0] = M_PRX;
        appxs[appxs.length - 1][1] = markercode;
        // not relaible code, because we suppose to markerid contains a packed len
        System.arraycopy(markerid, 0, appxs[appxs.length - 1], 2, 2);
        System.arraycopy(data, 0, appxs[appxs.length - 1], 4, len);
    }

    public void saveMarkers(OutputStream os) throws IOException {
        try {
            read(true, true);
            if (os != null) {
                writeMarkerAppXs(os);
                os.close();
            }
        } finally {
            appxs = null;
        }
    }

    int readMarker(InputStream is) throws IOException, FileFormatException {
        if (is.read(markerid) != markerid.length) {
            throw new FileFormatException("Wrong length read for marker header");
        }
        readcounter += markerid.length;
        data = markerid;
        int len = bs2i(0, 2) - 2;
        data = new byte[len];
        read(is, data);
        readcounter += len;
        return len;
    }

    // writes stored APPs
    void writeMarkerAppXs(OutputStream os) throws IOException {
        if (appxs == null)
            return;
        for (int i = 0; i < appxs.length; i++) {
            os.write(appxs[i]);
        }
    }

    void writeMarkerSOI(OutputStream os) throws IOException {
        os.write(M_PRX);
        os.write(M_SOI);
    }

    // TODO: this method should be exclusive with writeMarkerAppXs
    void writeNewMarker(OutputStream os, Class custom_appx) throws IOException {
        if (custom_appx == null)
            return;
        if (custom_appx == JFXX.class)
            os.write(JFXX.getMarkerData());
        else if (custom_appx == Exif.class)
            os.write(Exif.getMarkerData());
        else if (custom_appx == AbstractImageInfo.class) {
            String name = getName();
            int dp = name.lastIndexOf('.');
            if (dp > 0)
                name = name.substring(0, dp + 1);
            else
                name += '.';
            File ff;
            if (file != null
                    && (ff = new File(file.getParent(), name + Exif.FORMAT))
                            .exists()) {
                try {
                    byte[] buf = new byte[(int) ff.length()];
                    FileInputStream fis = new FileInputStream(ff);
                    read(fis, buf);
                    os.write(buf);
                    fis.close();
                } catch (IOException e) {
                    System.err.println("Exception in reading exif marker " + e);
                }
            }
        }
    }

    void writeMarkerComment(OutputStream os, String comment, String enc)
            throws IOException {
        os.write(M_PRX);
        os.write(M_COM);
        int size = 2;
        try {
            data = comment.getBytes(enc);
        } catch (UnsupportedEncodingException uee) {
            data = comment.getBytes();
        } catch (NullPointerException npe) {
            data = comment.getBytes();
        }
        size += data.length;
        os.write(size >> 8);
        os.write(size & 255);
        os.write(data);
    }

    void writeMarkerDHT(OutputStream os) throws IOException {
        os.write(M_PRX);
        os.write(M_DHT);
        int size = 2;
        for (int i = 0; i < ac_ix.length; i++)
            size += 1 + 16 + ac_huffval[i].length;
        for (int i = 0; i < dc_ix.length; i++)
            size += 1 + 16 + dc_huffval[i].length;

        os.write(size >> 8);
        os.write(size & 255);

        for (int i = 0; i < dc_ix.length; i++) {
            os.write(dc_ix[i]);
            for (int k = 0; k < dc_huffbits[i].length; k++)
                os.write(dc_huffbits[i][k]);
            for (int k = 0; k < dc_huffval[i].length; k++)
                os.write(dc_huffval[i][k]);
        }

        for (int i = 0; i < ac_ix.length; i++) {
            os.write(ac_ix[i] + 0x10);
            for (int k = 0; k < ac_huffbits[i].length; k++)
                os.write(ac_huffbits[i][k]);
            for (int k = 0; k < ac_huffval[i].length; k++)
                os.write(ac_huffval[i][k]);
        }
    }

    void writeMarkerDQT(OutputStream os) throws IOException {
        if (!valid)
            throw new IOException(
                    "Can't write marker DQT, because an error happened at reading ("
                            + getLocationName() + ")");
        os.write(M_PRX);
        os.write(M_DQT);
        int size = 2 + q_ix.length * (1 + DCTSIZE2);
        os.write(size >> 8);
        os.write(size & 255);
        for (int i = 0; i < q_ix.length; i++) {
            os.write(q_ix[i] + (q_prec[i] == 8 ? 0 : 0x10));
            for (int k = 0; k < DCTSIZE2; k++)
                os.write(q_table[i][k]);
        }
    }

    void writeMarkerDRI(OutputStream os) throws IOException {
        if (restart_interval != 0) {
            os.write(M_PRX);
            os.write(M_DRI);
            os.write(0);
            os.write(4);
            os.write(restart_interval >> 8);
            os.write(restart_interval & 255);
        }
    }

    void writeMarkerSOF0(OutputStream os) throws IOException {
        os.write(M_PRX);
        os.write(M_SOF0);
        int size = 2 + 1 + 2 + 2 + 1 + components_in_frame * (1 + 1 + 1);
        os.write((size >> 8) & 255);
        os.write(size & 255);
        os.write(frm_precision);
        os.write(frm_y >> 8);
        os.write(frm_y & 255);
        os.write(frm_x >> 8);
        os.write(frm_x & 255);
        os.write(components_in_frame);
        for (int i = 0; i < components_in_frame; i++) {
            os.write(ID[i]);
            os.write((H[i] << 4) + V[i]);
            os.write(QT[i]);
        }
    }

    void writeMarkerSOS(OutputStream os) throws IOException {
        os.write(M_PRX);
        os.write(M_SOS);
        int size = 2 + 1 + components_in_scan * (1 + 1) + 1 + 1 + 1;
        os.write(size >> 8);
        os.write(size & 255);
        os.write(components_in_scan);
        for (int i = 0; i < components_in_scan; i++) {
            os.write(comp_ids[i]);
            os.write((dc_table[i] << 4) + ac_table[i]);
        }
        os.write(_Ss);
        os.write(_Se);
        os.write((_Ah << 4) + _Al);
    }

    void writeMarkerEOI(OutputStream os) throws IOException {
        os.write(M_PRX);
        os.write(M_EOI);
    }

    void writeUnprocessedMarkers(OutputStream os) throws IOException {
        if (tables != null && markers != null) {
            for (int i = 0; i < markers.length; i++) {
                byte[] marker = (byte[]) tables.get("" + markers[i]);
                //System.err.println("Writing "+Integer.toHexString(markers[i])+" of "+marker);
                if (marker != null)
                    os.write(marker);
            }
        }
    }

    void writeRawDCT(OutputStream os) throws IOException {
        if (rawDct != null)
            os.write(rawDct);
    }

    void readRawDCT(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024 * 4);
        byte[] buffer = new byte[1024 * 4];
        int len;
        while ((len = is.read(buffer)) > 0) {
            readcounter += len;
            os.write(buffer, 0, len);
        }
        rawDct = os.toByteArray();
    }

	int readDHT(InputStream is) throws IOException {
        return readDHT(is, 0);
    }

    /* This code is for reading the DHT data and generating Huff related tables.
     * It is reused for regenerating Huff tables for writing out the image. The
     * Huff table info expected after the marker is put in the data array
     * according to the JPEG spec and the lenAvailable parameter is passed. */
	int readDHT(InputStream is, int lenAvailable) throws IOException {
		int result = lenAvailable;
        if(result <= 0)
		    result = readMarker(is);
		int base = 0;
		do {
			boolean is_ac = (data[base] & 255) > 15;
			int tbl_ix;
			if (is_ac)
				tbl_ix = (data[base] & 255)-16;
			else
				tbl_ix = data[base] & 255;
			int [][]wt2d, enc_matrix, wt3d[];
			int []wt1d;
			int tabnum=0;
			int ii;
			if (!is_ac) {
				enc_matrix = new int[12][2];
				tabnum = dc_valoffset.length;
				wt2d = new int[tabnum+1][];
				System.arraycopy(dc_valoffset, 0, wt2d, 0, tabnum);
				dc_valoffset = wt2d;
				wt2d = new int[tabnum+1][];
				System.arraycopy(dc_maxcode, 0, wt2d, 0, tabnum);
				dc_maxcode = wt2d;
				wt2d = new int[tabnum+1][];
				System.arraycopy(dc_huffval, 0, wt2d, 0, tabnum);
				dc_huffval = wt2d;
				wt2d = new int[tabnum+1][];
				System.arraycopy(dc_huffbits, 0, wt2d, 0, tabnum);
				dc_huffbits = wt2d;
				wt3d = new int[tabnum+1][][];
				System.arraycopy(enc_dc_matrix, 0, wt3d, 0, tabnum);
				enc_dc_matrix = wt3d;
				wt1d = new int[tabnum+1];
				System.arraycopy(dc_ix, 0, wt1d, 0, tabnum);
				dc_ix = wt1d;
				dc_ix[tabnum] = tbl_ix;
			} else {
				enc_matrix = new int[255][2];
				tabnum = ac_valoffset.length;
				wt2d = new int[tabnum+1][];
				System.arraycopy(ac_valoffset, 0, wt2d, 0, tabnum);
				ac_valoffset = wt2d;
				wt2d = new int[tabnum+1][];
				System.arraycopy(ac_maxcode, 0, wt2d, 0, tabnum);
				ac_maxcode = wt2d;
				wt2d = new int[tabnum+1][];
				System.arraycopy(ac_huffval, 0, wt2d, 0, tabnum);
				ac_huffval = wt2d;
				wt2d = new int[tabnum+1][];
				System.arraycopy(ac_huffbits, 0, wt2d, 0, tabnum);
				ac_huffbits = wt2d;
				wt3d = new int[tabnum+1][][];
				System.arraycopy(enc_ac_matrix, 0, wt3d, 0, tabnum);
				enc_ac_matrix = wt3d;
				wt1d = new int[tabnum+1];
				System.arraycopy(ac_ix, 0, wt1d, 0, tabnum);
				ac_ix = wt1d;
				ac_ix[tabnum] = tbl_ix;
			}
			int []huffsize = new int[257];
			int []huffcode = new int[257];
			int []huffbits = new int[16];
			int p=0;
			for (int l=1; l<=16; l++) {
				huffbits[l-1] = ii = (data[base+l] & 255);
				while(ii-->0)
					huffsize[p++] = l;
			}
			huffsize[p] = 0;
			int numsymbols = p;
			// check for legal huffman code tree
			int code = 0;
			int si = huffsize[0];
			p = 0;
			while (huffsize[p]!=0) {
				while (huffsize[p] == si) {
					huffcode[p++] = code++;
				}
				// code is now 1 more than the last code used for codelength si; but
				// it must still fit in si bits, since no code is allowed to be all ones.
				if (code >= (1 << si))
					throw new IOException("Bad huffman code table ("+getLocationName()+")");
				code <<= 1;
				si++;
			}
			// Figure F.15: generate decoding tables for bit-sequential decoding 
			int []valoffset = new int[17];
			int []maxcode = new int[18];
			p = 0;
			for (int l = 1; l <= 16; l++) {
				if (data[base+l] != 0) {
					// valoffset[l] = huffval[] index of 1st symbol of code length l,
					// minus the minimum code of length l
					valoffset[l] = p - huffcode[p];
					p += (data[base+l] & 255);
					maxcode[l] = huffcode[p-1];
				} else {
					maxcode[l] = -1;	// -1 if no codes of this length
				}
			}
			maxcode[17] = -1;
			int []huffval = new int[numsymbols];
			// fill values
			for (int l = 0; l < numsymbols; l++) {
				huffval[l] = data[base+l+17] & 255;
				enc_matrix[huffval[l]][0] = huffcode[l];
				enc_matrix[huffval[l]][1] = huffsize[l];
			}

			if (!is_ac) {
				dc_valoffset[tabnum] = valoffset;
				dc_maxcode[tabnum] = maxcode;
				dc_huffval[tabnum] = huffval;
				enc_dc_matrix[tabnum] = enc_matrix;
				dc_huffbits[tabnum] = huffbits;
			} else {
				ac_valoffset[tabnum] = valoffset;
				ac_maxcode[tabnum] = maxcode;
				ac_huffval[tabnum] = huffval;
				enc_ac_matrix[tabnum] = enc_matrix;
				ac_huffbits[tabnum] = huffbits;
			}
			base += (numsymbols+17);
		} while (base < result);
		return result;
	}

    int restarts_to_go;

    void readDCT(InputStream is) throws IOException {
        int[] last_dc = new int[components_in_scan];
        int[][] DCT = new int[2][DCTSIZE2];
        int curcoef;
        restarts_to_go = restart_interval;
        if (_Ss != 0 || _Se != (DCTSIZE2 - 1) || _Ah != 0 || _Al != 0)
            System.err.println("Not sequential image, Ss=" + _Ss + " Se=" + _Se
                    + " Ah=" + _Ah + " Al=" + _Al);
        int widthMCU = (frm_x + DCTSIZE * maxHi - 1) / (DCTSIZE * maxHi);
        int heightMCU = (frm_y + DCTSIZE * maxVi - 1) / (DCTSIZE * maxVi);
        System.err.println("Size in MCU " + widthMCU + "x" + heightMCU);
        HuffDecoder decoder = new HuffDecoder(is);
        dct_coefs = new int[heightMCU][widthMCU][mcusize][2][];
        for (int iy = 0; iy < heightMCU; iy++) {
            for (int ix = 0; ix < widthMCU; ix++) {
                // start decode MCU
                int mcuc = 0;
                try {
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        for (int k = 0; k < last_dc.length; k++)
                            last_dc[k] = 0;
                        decoder.restart();
                    }
                    for (int c = 0; c < components_in_scan; c++) {
                        for (int b = 0; b < V[c] * H[c]; b++) {
                            decoder.setTables(false, dc_table[c]);
                            last_dc[c] = decoder.extend(decoder.decode(1))
                                    + last_dc[c];
                            curcoef = 0;
                            DCT[0][curcoef] = last_dc[c];
                            DCT[1][curcoef++] = 0;
                            // decode ACs
                            decoder.setTables(true, ac_table[c]);
                            int ac, v;
                            for (int ci = 1; ci < DCTSIZE2; ci++) {
                                ac = decoder.decode(1);
                                v = (ac >> 4);
                                ac &= 15;
                                if (ac != 0) {
                                    ci += v;
                                    if (ci > DCTSIZE2 - 1) {
                                        System.err.println("Invalid AC index "
                                                + ci);
                                        ci = DCTSIZE2 - 1;
                                    }
                                    ac = decoder.extend(ac);
                                    DCT[0][curcoef] = ac;
                                    DCT[1][curcoef++] = ci;
                                } else {
                                    if (v != 15)
                                        break;
                                    ci += v;
                                }
                            }
                            dct_coefs[iy][ix][mcuc] = new int[2][curcoef];
                            System.arraycopy(DCT[0], 0,
                                    dct_coefs[iy][ix][mcuc][0], 0, curcoef);
                            System.arraycopy(DCT[1], 0,
                                    dct_coefs[iy][ix][mcuc][1], 0, curcoef);
                            mcuc++;
                        }
                    }
                    restarts_to_go--;
                } catch (RestartException re) {
                    restarts_to_go = 0;
                    System.err.println("Restart exception ");
                }
            }
        }
        if (restart_interval != 0 && restarts_to_go == 0) {
            // last restart marker can be here
            is.read(markerid);
            if (markerid[1] == M_EOI)
		valid = true;
       }
    }

    void transposeImageParameters() {
        int t = frm_x;
        frm_x = frm_y;
        frm_y = t;
        for (int c = 0; c < components_in_scan; c++) {
            t = V[c];
            V[c] = H[c];
            H[c] = t;
        }
    }

    void transposeQTable() {
        int t;
        for (int k = 0; k < q_table.length; k++) {
            for (int i = 0; i < DCTSIZE; i++) {
                for (int j = 0; j < i; j++) {
                    t = q_table[k][jpegzigzagorder[i*DCTSIZE+j]];
                    q_table[k][jpegzigzagorder[i*DCTSIZE+j]] = q_table[k][jpegzigzagorder[j*DCTSIZE+i]];
                    q_table[k][jpegzigzagorder[j*DCTSIZE+i]] = t;
                }
            }
        }
    }

    void writeDCT(OutputStream os, int op) throws IOException {
        if (!valid)
            throw new IOException(
                    "Can't write DCT, because an error happened at reading ("
                            + getLocationName() + ")");
        int[] last_dc = new int[components_in_scan];
        int off;
        HuffEncoder encoder = new HuffEncoder(os);
        int restarts_to_go = restart_interval;
        switch (op) {			// consider appraoch when gathring info for huffman coeff			// on second path just use already converted dct
        case TRANSPOSE:
            for (int ix = 0; ix < dct_coefs[0].length; ix++) {
                for (int iy = 0; iy < dct_coefs.length; iy++) {
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int mx = 0; mx < V[c]; mx++) {
                                for (int my = 0; my < H[c]; my++) {
                                    last_dc[c] = encoder.encode(
                                            transposeDCT(dct_coefs[iy][ix][off
                                                    + my * V[c] + mx]),
                                            last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
            break;
        case ROT_90:
            for (int ix = 0; ix < dct_coefs[0].length; ix++) {
                for (int iy = dct_coefs.length - 1; iy >= 0; iy--) {
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int mx = 0; mx < V[c]; mx++) {
                                for (int my = H[c] - 1; my >= 0; my--) {
                                    last_dc[c] = encoder.encode( 										gatheringStats?
                                            rotate90DCT(dct_coefs[iy][ix][off
																		  + my * V[c] + mx]):											dct_coefs[iy][ix][off
																		  + my * V[c] + mx],
                                            last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    } catch (NullPointerException npe) {

                    }
                }
            }
            break;
        case ROT_270:
            for (int ix = dct_coefs[0].length - 1; ix >= 0; ix--) {
                for (int iy = 0; iy < dct_coefs.length; iy++) {
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int mx = V[c] - 1; mx >= 0; mx--) {
                                for (int my = 0; my < H[c]; my++) {
                                    last_dc[c] = encoder.encode(
                                            rotate270DCT(dct_coefs[iy][ix][off
                                                    + my * V[c] + mx]),
                                            last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
            break;
        case TRANSVERSE:
            for (int ix = dct_coefs[0].length - 1; ix >= 0; ix--) {
                for (int iy = dct_coefs.length - 1; iy >= 0; iy--) {
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int mx = V[c] - 1; mx >= 0; mx--) {
                                for (int my = H[c] - 1; my >= 0; my--) {
                                    last_dc[c] = encoder.encode(
                                            transverseDCT(dct_coefs[iy][ix][off
                                                    + my * V[c] + mx]),
                                            last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
            break;
        case FLIP_H:
            for (int iy = 0; iy < dct_coefs.length; iy++) {
                for (int ix = dct_coefs[iy].length - 1; ix >= 0; ix--) {
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int my = 0; my < V[c]; my++) {
                                for (int mx = H[c] - 1; mx >= 0; mx--) {
                                    last_dc[c] = encoder.encode(
                                            flipHDct(dct_coefs[iy][ix][off + my
                                                    * H[c] + mx]), last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
            break;
        case FLIP_V:
            for (int iy = dct_coefs.length - 1; iy >= 0; iy--) {
                for (int ix = 0; ix < dct_coefs[iy].length; ix++) {
                    // start encode MCU
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int my = V[c] - 1; my >= 0; my--) {
                                for (int mx = 0; mx < H[c]; mx++) {
                                    last_dc[c] = encoder.encode(
                                            flipVDct(dct_coefs[iy][ix][off + my
                                                    * H[c] + mx]), last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
            break;
        case ROT_180:
            for (int iy = dct_coefs.length - 1; iy >= 0; iy--) {
                for (int ix = dct_coefs[iy].length - 1; ix >= 0; ix--) {
                    // start encode MCU
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int my = V[c] - 1; my >= 0; my--) {
                                for (int mx = H[c] - 1; mx >= 0; mx--) {
                                    last_dc[c] = encoder.encode(
                                            rotate180Dct(dct_coefs[iy][ix][off
                                                    + my * H[c] + mx]),
                                            last_dc[c]);
                                }
                            }
                            off += V[c] * H[c];
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
            break;
        case NONE:
        default:
            for (int iy = 0; iy < dct_coefs.length; iy++) {
                for (int ix = 0; ix < dct_coefs[iy].length; ix++) {
                    // start encode MCU
                    off = 0;
                    if (restart_interval != 0 && restarts_to_go == 0) {
                        restarts_to_go = restart_interval;
                        if (_Ss == 0) {
                            for (int k = 0; k < last_dc.length; k++)
                                last_dc[k] = 0;
                        }
                        encoder.restart();
                    }
                    try {
                        for (int c = 0; c < components_in_scan; c++) {
                            encoder.setTables(ac_table[c], dc_table[c]);
                            for (int b = 0; b < V[c] * H[c]; b++) {
                                last_dc[c] = encoder.encode(
                                        dct_coefs[iy][ix][off], last_dc[c]);
                                off++;
                            }
                        }
                        restarts_to_go--;
                    } catch (RestartException re) {
                        //re.printStackTrace();
                        restarts_to_go = 0;
                    }
                }
            }
        }
       if (restart_interval != 0 && restarts_to_go == 0)
          encoder.restart();
       encoder.flush();
    }

    // Utility method to copy dct coeffictients for transforming methods
    // to avoid changing the main array while gatheringStats
    private static int[][] cloneDct(int [][]dct)
    {
		int i;
        int retVal[][] = new int[dct.length][];
        for(i=0; i<dct.length; ++i)
        {
            retVal[i] = new int[dct[i].length];
            System.arraycopy(dct[i], 0, retVal[i], 0, retVal[i].length);
        }

        return retVal;
    }

    int[][] transposeDCT(int[][] dct) {
        int i, k;
        if(gatheringStats)
            dct = cloneDct(dct);
        int[] tmp_dct = new int[DCTSIZE2];
        for (i = 0; i < dct[0].length; i++) {
            k = jpegnaturalorder[dct[1][i]];
            k = ((k & 7) << 3) + (k >> 3);
            tmp_dct[jpegzigzagorder[k]] = dct[0][i];
        }
        for (i = k = 1; i < tmp_dct.length && i < dct[0].length; i++) {
            dct[1][i] = i;
            dct[0][i] = tmp_dct[i];
        }
        return dct;
    }


    int[][] rotate90DCT(int[][] dct) {
        int i, k, m;
        int[] tmp_dct = new int[DCTSIZE2];
        for (i = 0; i < dct[0].length; i++) {
            k = jpegnaturalorder[dct[1][i]];
            k = ((k & 7) << 3) + (k >> 3);
            tmp_dct[jpegzigzagorder[k]] = (k & 1) == 1 ? -dct[0][i] : dct[0][i];
        }
        for (i = k = 1; i < tmp_dct.length; i++) {
            if (tmp_dct[i] != 0) {
                dct[1][k] = i;
                dct[0][k] = tmp_dct[i];
                k++;
            }
        }

        return dct;
    }
    
    int[][] rotate270DCT(int[][] dct) {
        int i, k;
        if(gatheringStats)
            dct = cloneDct(dct);
        int[] tmp_dct = new int[DCTSIZE2];
        for (i = 0; i < dct[0].length; i++) {
            k = jpegnaturalorder[dct[1][i]];
            k = ((k & 7) << 3) + (k >> 3);
            tmp_dct[jpegzigzagorder[k]] = (k & 8) == 8 ? -dct[0][i] : dct[0][i];
        }
        for (i = k = 1; i < tmp_dct.length && i < dct[0].length; i++) {
            dct[1][i] = i;
            dct[0][i] = tmp_dct[i];
        }
        return dct;
    }

    int[][] transverseDCT(int[][] dct) {
        int i, k;
        if(gatheringStats)
            dct = cloneDct(dct);
        int[] tmp_dct = new int[DCTSIZE2];
        boolean neg;
        for (i = 0; i < dct[0].length; i++) {
            k = jpegnaturalorder[dct[1][i]];
            neg = (k & 1) != 0;
            k = ((k & 7) << 3) + (k >> 3);
            neg ^= (k & 1) != 0;
            tmp_dct[jpegzigzagorder[k]] = neg ? -dct[0][i] : dct[0][i];
        }
        for (i = k = 1; i < tmp_dct.length && i < dct[0].length; i++) {
            dct[1][i] = i;
            dct[0][i] = tmp_dct[i];
        }
        return dct;
    }

    int[][] flipHDct(int[][] dct) {
        if(gatheringStats)
            dct = cloneDct(dct);
        for (int k = 0; k < dct[0].length; k++) {
            if ((jpegnaturalorder[dct[1][k]] & 1) != 0)
                dct[0][k] = -dct[0][k];
        }
        return dct;
    }

    int[][] flipVDct(int[][] dct) {
        if(gatheringStats)
            dct = cloneDct(dct);
        for (int k = 0; k < dct[0].length; k++) {
            if ((jpegnaturalorder[dct[1][k]] & 8) == 8)
                dct[0][k] = -dct[0][k];
        }
        return dct;
    }

    int[][] rotate180Dct(int[][] dct) {
        if(gatheringStats)
            dct = cloneDct(dct);
        for (int k = 0; k < dct[0].length; k++) {
            // For even row, negate every odd column.
            // For odd row, negate every even column.
            if (((jpegnaturalorder[dct[1][k]] & 9) == 1)
                    || ((jpegnaturalorder[dct[1][k]] & 9) == 8))
                dct[0][k] = -dct[0][k];
        }
        return dct;
    }

    // trimRightEdge
    // trimBottomEdge

    class HuffDecoder {
        private InputStream is;

        int bit_buff;

        int bit_buff_len;

        int marker;

        //        long marker_offset;
        int next_restart_num;

        int[] cur_maxcode, cur_huffval, cur_valoffset;

        HuffDecoder(InputStream is) {
            this.is = is;
        }

        void setTables(boolean ac, int index) {
            if (ac) {
                // find index of table
                for (int i = 0; i < ac_ix.length; i++) {
                    if (ac_ix[i] == index) { // found
                        cur_maxcode = ac_maxcode[i];
                        cur_huffval = ac_huffval[i];
                        cur_valoffset = ac_valoffset[i];
                        break;
                    }
                }
            } else {
                // find index of table
                for (int i = 0; i < dc_ix.length; i++) {
                    if (dc_ix[i] == index) { // found
                        cur_maxcode = dc_maxcode[i];
                        cur_huffval = dc_huffval[i];
                        cur_valoffset = dc_valoffset[i];
                        break;
                    }
                }
            }
        }

        void checkBitBuffer(int len) throws IOException, RestartException {
            if (bit_buff_len < len) {
                if (len > 16) // !!!
                    throw new IOException(
                            "An attempt to read more than 16 bit (inbuff="
                                    + bit_buff_len + ", len=" + len + ") ("
                                    + getLocationName() + ")");
                do {
                    bit_buff <<= BYTE_SIZE;
                    bit_buff |= read();
                    bit_buff_len += BYTE_SIZE;
                } while (bit_buff_len < len);
            }
        }

        int read() throws IOException, RestartException {
            int result = is.read();
            readcounter++;
            //if (readcounter > 0x251520)
            //    System.err.println("read 0x"+Integer.toHexString(result)+" ("+Integer.toHexString(readcounter));
            if (result == -1)
                throw new IOException("End of file reached at " + readcounter
                        + " (" + getLocationName() + ")");
            if (result == (M_PRX&255)) {
                do {
                    result = is.read();
                    if (result == -1)
                        throw new IOException("End of file reached at " + readcounter
                                + " (" + getLocationName() + ")");
                    
                    readcounter++;
                } while (result == (M_PRX&255));
                if (result == 0)
                    result = (M_PRX&255);
                else {
                    // marker found
                    marker = result;
                    //marker_offset = readcounter;
                    if (marker == ((M_RST0 & 255) + next_restart_num)) {
                        System.err.println("RSTM"+next_restart_num+" found 0x"
                                + Integer.toHexString(marker) + " offset 0x"
                                + Long.toHexString(readcounter));
                        next_restart_num = (next_restart_num + 1) & 7;
                    } else {
                        System.err.println("Wrong marker found 0x"
                                + Integer.toHexString(marker) + " offset 0x"
                                + Long.toHexString(readcounter));
                        throw new IOException(
                                "Restart markers are messed up at 0x"
                                        + Integer.toHexString(readcounter)
                                        + " (" + getLocationName() + ")");
                    }
                    if (restarts_to_go == restart_interval) { //here we expected
                        result = is.read();
                        if (result == -1)
                            throw new IOException("End of file reached at " + readcounter
                                    + " (" + getLocationName() + ")");
                        readcounter++;
                        if (result == (M_PRX&255)) {
                            throw new IOException("Found one more marker at "+Long.toHexString(readcounter));
                        }
                    } else {
                        throw new RestartException(next_restart_num);
                    }
                }
            }
            return result;
        }

        int getBits(int len) throws IOException, RestartException {
            checkBitBuffer(len);
            bit_buff_len -= len;
            return (bit_buff >> bit_buff_len) & (0xffff >> (16 - len));
        }

        int extend(int n_bits) throws IOException, RestartException {
            if (n_bits == 0)
                return 0;
            int result = getBits(n_bits);
            return ((result) < (1 << ((n_bits) - 1)) ? (result)
                    + (((-1) << (n_bits)) + 1) : (result));
        }

        int decode(int min_bits) throws IOException, RestartException {
            int l = min_bits;
            // decode has determined that the code is at least min_bits 
            // bits long, so fetch that many bits in one swoop.
            int code = getBits(l);

            // Collect the rest of the Huffman code one bit at a time.
            // This is per Figure F.16 in the JPEG spec.
            while (code > cur_maxcode[l]) {
                if (code < 0)
                    System.err.println("Negative code 0x"
                            + Integer.toHexString(code) + " max 0x"
                            + Integer.toHexString(cur_maxcode[l]));
                code <<= 1;
                code |= getBits(1);
                //code &= 0xFFFF;
                if (++l > 16)
                    throw new IOException(
                            "Corrupted JPEG data: bad Huffman code, 0x"
                                    + Integer.toHexString(code) + " max 0x"
                                    + Integer.toHexString(cur_maxcode[l])
                                    + " at 0x"
                                    + Integer.toHexString(readcounter) + " ("
                                    + getLocationName() + ")");
            }
            return cur_huffval[code + cur_valoffset[l]];
        }

        void restart() {
            bit_buff_len = 0;
            bit_buff = 0;
            System.err.println("Restart at offset 0x"
                    + Long.toHexString(readcounter));
        }
    }

    class HuffEncoder {
        private int bufferputbits;
        private int bufferputbuffer;
        private OutputStream outputstream;
        private int[][] dc_ecodetable, ac_ecodetable;
        int next_restart_num;
        int acTblIndex, dcTblIndex;

        public HuffEncoder(OutputStream os) {
            outputstream = os;
        }

        void setTables(int iac, int idc) {
            boolean dt_found = false, at_found = false;
            for (int i = 0; i < ac_ix.length; i++) {
                if (ac_ix[i] == iac) { // found
					ac_ecodetable = enc_ac_matrix[i];
					this.acTblIndex = i;					at_found = true;
                    break;
                }
            }
            for (int i = 0; i < dc_ix.length; i++) {
                if (dc_ix[i] == idc) { // found
                    dc_ecodetable = enc_dc_matrix[i];
                    this.dcTblIndex = i;
                    dt_found = true;
                    break;
                }
            }
            if (at_found == false || dt_found == false)
                System.err.println("One of tables not found for a " + iac + " "
                        + at_found + " d " + idc + " " + dt_found);
        }

        int encode(int coef[][], int last_dc) throws IOException,
                RestartException {
            if (coef == null || coef[0] == null)
                throw new RestartException(0);
            int temp, temp2, nbits, k, r, i;
            // The DC portion
            temp = temp2 = coef[0][0] - last_dc;
            if (temp < 0) {
                temp = -temp;
                temp2--;
            }
            nbits = 0;
            while (temp != 0) {
                nbits++;
                temp >>= 1;
            }
            writeCode(dc_ecodetable[nbits][0], dc_ecodetable[nbits][1]);
            if(gatheringStats)
                huffGen.updateDCCount(dcTblIndex, nbits);
            // The arguments in bufferIt are code and size.
            if (nbits != 0)
                writeCode(temp2, nbits);

            // The AC portion
            for (k = 1; k < coef[0].length; k++) {
                r = coef[1][k] - coef[1][k - 1] - 1;
                while (r > 15) { // write for 0 ig gap > 15
                    writeCode(ac_ecodetable[0xF0][0], ac_ecodetable[0xF0][1]);
                    if(gatheringStats)
                        huffGen.updateACCount(acTblIndex, 0xF0);
                    r -= 16;
                }
                temp = temp2 = coef[0][k];
                if (temp < 0) {
                    temp = -temp;
                    temp2--;
                }
                nbits = 1; // temp is never 0
                while ((temp >>= 1) != 0)
                    nbits++;
                i = (r << 4) + nbits;
                writeCode(ac_ecodetable[i][0], ac_ecodetable[i][1]);
                if(gatheringStats)
                    huffGen.updateACCount(acTblIndex, i);
                writeCode(temp2, nbits);
            }

            if ((63 - coef[1][coef[1].length - 1]) > 0) { // mark that the end of data
                writeCode(ac_ecodetable[0][0], ac_ecodetable[0][1]);
                if(gatheringStats)
                    huffGen.updateACCount(acTblIndex, 0);
            }

            return coef[0][0];
        }

		void restart() throws IOException {			if(gatheringStats)
				return;

			//System.err.println("flush of 0x"+Integer.toHexString(bufferputbuffer)+" len "+bufferputbits);
			flush();
			outputstream.write(M_PRX);
			outputstream.write((M_RST0 & 255) + next_restart_num);
			next_restart_num = (next_restart_num + 1) & 7;
			bufferputbits = bufferputbuffer = 0;
		}

        // Uses an integer long (32 bits) buffer to store the Huffman encoded bits
        // and sends them to out stream by the byte.

        void writeCode(int putbuffer, int size) throws IOException {			if(gatheringStats)				return;
            int putbits = bufferputbits;
            putbuffer &= (1 << size) - 1;
            putbits += size;
            putbuffer <<= 24 - putbits;
            putbuffer |= bufferputbuffer;
            int c;
            while (putbits >= 8) {
                c = ((putbuffer >> 16) & 0xff);
                outputstream.write(c);
                if (c == 0xff)
                    outputstream.write(0);
                putbuffer <<= 8;
                putbits -= 8;
            }
            bufferputbuffer = putbuffer;
            bufferputbits = putbits;
        }

        void flush() throws IOException {			if(gatheringStats)				return;
            int putbuffer = bufferputbuffer;
            int putbits = bufferputbits;
            int c;
            while (putbits >= 8) {
                c = (putbuffer >> 16) & 0xff;
                outputstream.write(c);
                if (c == 0xFF)
                    outputstream.write(0);
                putbuffer <<= 8;
                putbits -= 8;
            }
            if (putbits > 0) {
                c = (putbuffer >> 16) | (0xff >> putbits);
                outputstream.write(c);
            }
            bufferputbuffer = putbuffer;
            bufferputbits = putbits;
        }
    }

    private class HuffGenerator
    {
        /* Symbol(byte) frequencies for each of the [da]HuffTblCount Huff tables
         * */
        private int dc_count[][];
        private int ac_count[][];

        /* Equivalent to constructor. Allocates frequency arrays */
        public void init()
        {
            /* Allocate space for frequency table */
            dc_count = new int[dc_ix.length][257];
            ac_count = new int[ac_ix.length][257];
        }

        public void freeMemory()
        {
            dc_count = null;
            ac_count = null;
        }

        public HuffGenerator()
        {
        }

        public void updateDCCount(int tableIndex, int symbol)
        {
            ++dc_count[tableIndex][symbol];
        }

        public void updateACCount(int tableIndex, int symbol)
        {
            ++ac_count[tableIndex][symbol];
        }

        /* Code is modified from jpeg_gen_optimal_table function in jchuff.c
         * file of IJG code. It writes to bs the Number of symbols for each of
         * 1..16 code lengths followed by the actual codes for lenghts 1..16 as
         * expected in the JPEG format. If bs is a ByteArrayOutputStream then
         * the bytes can be recovered from it. */
        private void genOptimalTable(OutputStream os, int freq[])
                throws IOException
        {
            int bits[] = new int[MAX_CLEN+1];	/* bits[k] = # of symbols with code length k */
            int codesize[] = new int[257];		/* codesize[k] = code length of symbol k */
            int others[] = new int[257];		/* next symbol in current branch of tree */
            int c1, c2;
            int i, j;
            int v;
            /* This algorithm is explained in section K.2 of the JPEG standard */
      
            for (i = 0; i < 257; i++)
              others[i] = -1;		/* init links to empty */
            
            freq[256] = 1;		/* make sure 256 has a nonzero count */
            /* Including the pseudo-symbol 256 in the Huffman procedure guarantees
             * that no real symbol is given code-value of all ones, because 256
             * will be placed last in the largest codeword category.
             */
      
            /* Huffman's basic algorithm to assign optimal code lengths to symbols */
      
            for (;;) {
              /* Find the smallest nonzero frequency, set c1 = its symbol */
              /* In case of ties, take the larger symbol number */
              c1 = -1;
              v = 1000000000;
              for (i = 0; i <= 256; i++) {
                if (freq[i] > 0 && freq[i] <= v) {
              v = freq[i];
              c1 = i;
                }
              }
      
              /* Find the next smallest nonzero frequency, set c2 = its symbol */
              /* In case of ties, take the larger symbol number */
              c2 = -1;
              v = 1000000000;
              for (i = 0; i <= 256; i++) {
                if (freq[i] > 0 && freq[i] <= v && i != c1) {
              v = freq[i];
              c2 = i;
                }
              }
      
              /* Done if we've merged everything into one frequency */
              if (c2 < 0)
                break;
              
              /* Else merge the two counts/trees */
              freq[c1] += freq[c2];
              freq[c2] = 0;
      
              /* Increment the codesize of everything in c1's tree branch */
              codesize[c1]++;
              while (others[c1] >= 0) {
                c1 = others[c1];
                codesize[c1]++;
              }
              
              others[c1] = c2;		/* chain c2 onto c1's tree branch */
              
              /* Increment the codesize of everything in c2's tree branch */
              codesize[c2]++;
              while (others[c2] >= 0) {
                c2 = others[c2];
                codesize[c2]++;
              }
            }
      
            /* Now count the number of symbols of each code length */
            for (i = 0; i <= 256; i++) {
              if (codesize[i] > 0) {
                /* The JPEG standard seems to think that this can't happen, */
                /* but I'm paranoid... */
                if (codesize[i] > MAX_CLEN)
                    throw new RuntimeException("Internal Error regenerating Huff Tables: Code Length " + codesize[i] + " for symbol " + i + " > 32");
      
                bits[codesize[i]]++;
              }
            }
      
            /* JPEG doesn't allow symbols with code lengths over 16 bits, so if the pure
             * Huffman procedure assigned any such lengths, we must adjust the coding.
             * Here is what the JPEG spec says about how this next bit works:
             * Since symbols are paired for the longest Huffman code, the symbols are
             * removed from this length category two at a time.  The prefix for the pair
             * (which is one bit shorter) is allocated to one of the pair; then,
             * skipping the BITS entry for that prefix length, a code word from the next
             * shortest nonzero BITS entry is converted into a prefix for two code words
             * one bit longer.
             */
            
            for (i = MAX_CLEN; i > 16; i--) {
              while (bits[i] > 0) {
                j = i - 2;		/* find length of new prefix to be used */
                while (bits[j] == 0)
              j--;
                
                bits[i] -= 2;		/* remove two symbols */
                bits[i-1]++;		/* one goes in this length */
                bits[j+1] += 2;		/* two new symbols in this length */
                bits[j]--;		/* symbol of this length is now a prefix */
              }
            }
      
            /* Remove the count for the pseudo-symbol 256 from the largest codelength */
            while (bits[i] == 0) //?? && i > 0)		/* find largest codelength still in use */
              i--;
            bits[i]--;
            
            /* Output final symbol counts (only for lengths 1..16) */
            for(i=1; i<=16; ++i)
                os.write(bits[i]);
            
            /* Output a list of the symbols sorted by code length */
            /* It's not real clear to me why we don't need to consider the codelength
             * changes made above, but the JPEG spec seems to think this works.
             */
            for (i = 1; i <= MAX_CLEN; i++) {
              for (j = 0; j <= 255; j++) {
                if (codesize[j] == i) {
                  os.write(j);
                }
              }
            }
        }

        /* Writes out the Huffman Tables to os as per JPEG spec excluding
         * marker and length */
        private void writeHuffTables(OutputStream os) throws IOException
        {
            int tableIndex, htInfo;
 
            /* Write out dc huff tables */
            for (tableIndex = 0; tableIndex < dc_ix.length; tableIndex++) {
                htInfo = dc_ix[tableIndex];
                os.write(htInfo);
                genOptimalTable(os, dc_count[tableIndex]);
            }
 
            /* Write out ac huff tables */
            for (tableIndex = 0; tableIndex < ac_ix.length; tableIndex++) {
                htInfo = 16+ac_ix[tableIndex];
                os.write(htInfo);
                genOptimalTable(os, ac_count[tableIndex]);
            }
        }
    }

    class RestartException extends Exception {
        RestartException(int scan) {
            this.scan = scan;
        }

        int scan;
    }

    private int readcounter;

    // APP headers storage
    private int[][] app_store;

    // image parameters
    private int components_in_scan;

    private int components_in_frame;

    private int frm_precision;

    private int[] comp_ids;

    private int[] dc_table;

    private int[] ac_table;

    private int _Ss, _Se, _Ah, _Al;

    // frame parameters
    private int frm_x;

    private int frm_y;

    private int[] V, H, QT, ID;

    private int maxHi, maxVi;

    private int mcusize;

    private int restart_interval;

    private int[][] dc_valoffset;

    private int[][] dc_maxcode;

    private int[][] dc_huffval;

    private int[] dc_ix;

    private int[][] ac_valoffset;

    private int[][] ac_maxcode;

    private int[][] ac_huffval;

    private int[][] dc_huffbits, ac_huffbits;

    private int[] ac_ix;

    private int[][] q_table;

    private int[] q_ix;

    private int[] q_prec;

    private int[][][][][] dct_coefs;

    private int[][][] enc_ac_matrix;

    private int[][][] enc_dc_matrix;

    protected boolean valid;

    protected boolean canBeProcessed; //22

    protected File file;

    protected InputStream inStream;

    protected byte[] markerid;

    protected AbstractImageInfo imageinfo;

    protected byte[] appxs[];

    protected Map tables;

    protected byte[] markers;

    protected byte[] rawDct;

    protected int exifIndex;

     private HuffGenerator huffGen;

     private boolean gatheringStats;

    String in_comment, out_comment;

    // public static method section
    public static Dimension getImageSize(Image image, final boolean sizeOnly) {
        final Dimension imageSize = new Dimension();
        synchronized (imageSize) {
            imageSize.width = image.getWidth(new ImageObserver() {
                public boolean imageUpdate(Image img, int infoflags, int x,
                        int y, int width, int height) {
                    //System.err.println("Sizing image, flags "+infoflags);
                    if ((sizeOnly && (infoflags & (WIDTH + HEIGHT)) == (WIDTH + HEIGHT))
                            || (infoflags & FRAMEBITS) == ALLBITS
                            || (infoflags & ABORT) == ABORT
                            || (infoflags & ERROR) == ERROR)
                        synchronized (imageSize) {
                            imageSize.width = width;
                            imageSize.height = height;
                            imageSize.notify();
                            //System.err.println("Returned size at flags "+infoflags);
                            return false;
                        }
                    //return width <= 0 || height <= 0;
                    return true;
                }
            });
            if (imageSize.width < 0) {
                //System.err.println(" Size wait "+imageSize.width+'x');
                try {
                    imageSize.wait(60 * 60 * 1000);
                } catch (Exception ie) {
                }
            } else {
                imageSize.height = image.getHeight(null);
                //System.err.println(" Size instant "+imageSize.width+'x'+imageSize.height);
            }
        }
        return imageSize;
    }

    public static void main(String[] args) {
        System.out.println("Lossless image transformer");
        try {
            System.setErr(new PrintStream(new FileOutputStream("rotate.log"),
                    true));
        } catch (IOException e) {
            System.err.println("Can't redirect error stream.");
        }
        if (args.length == 1) {
            System.out.println("Testing order tables.");
            for (int i = 0; i < jpegnaturalorder.length; i++)
                if (jpegzigzagorder[jpegnaturalorder[i]] != i)
                    System.out.println("Invalid table entry " + i);
            //else
            //	System.out.println(" "+i+" OK.");
            System.exit(0);
        }
        if (args.length < 3) {
            System.out
                    .println("Usage StrippedJpeg source_name dest_name op [[comment] [encoding]]\n"
                            + "   where op:  FLIP_H = 1\n"
                            + "              FLIP_V = 2\n"
                            + "              TRANSPOSE = 3\n"
                            + "              TRANSVERSE = 4\n"
                            + "              ROT_90 = 5\n"
                            + "              ROT_180 = 6\n"
                            + "              ROT_270 = 7\n"
                            + "              COMMENT = 8");
            System.exit(255);
        } else if (Integer.parseInt(args[2]) != COMMENT)
            new StrippedJpeg(new File(args[0])).transform(args[1], Integer
                    .parseInt(args[2]));
        else {
            StrippedJpeg j = new StrippedJpeg(new File(args[0]));
            if (args.length > 3) {
                if (args.length > 4)
                    j.setEncoding(args[4]);
                j.setComment(args[3]);
            }
            j.transform(args[1], COMMENT);
        }
    }
}

class JPEG extends AbstractImageInfo {
    int width, height, precision;

    byte[] header = new byte[2];

    public JPEG(InputStream is, byte[] data, int offset, String name,
            String comments, int width, int height) throws FileFormatException {
        super(is, data, offset, name, comments);
        if (this.width <= 0 && width > 0) {
            this.width = width;
            this.height = height;
        } else {
            int len;
            if (data.length == 2) { // too bad, marker can be unread
                len = readMarker(is, true);
                if (len <= 0)
                    return;
                if (data[1] >= M_SOF0 && data[1] <= M_SOF15 && data[1] != M_DHT
                        && data[1] != M_JPG) {
                    precision = (this.data[0] & 255) * (this.data[5] & 255);
                    this.width = bs2i(3, 2);
                    this.height = bs2i(1, 2);
                    return;
                }
            }
            do {
                len = readMarker(is, false);
                if (len <= 0)
                    break;
                if (header[1] >= M_SOF0 && header[1] <= M_SOF15
                        && header[1] != M_DHT && header[1] != M_JPG) {
                    precision = (this.data[0] & 255) * (this.data[5] & 255);
                    this.width = bs2i(3, 2);
                    this.height = bs2i(1, 2);
                    break;
                }
            } while (true);
        }
    }

    public int getResolutionX() {
        return width;
    }

    public int getResolutionY() {
        return height;
    }

    public int getMetering() {
        return 0;
    }

    public int getExpoProgram() {
        return 0;
    }

    public String getFormat() {
        return NA;
    }

    public void readInfo() {
        data = null; // for gc
    }

    public String getMake() {
        return NA;
    }

    public String getModel() {
        return NA;
    }

    public String getDataTimeOriginalString() {
        return dateformat.format(new Date(/*lastModified()*/));
    }

    public float getFNumber() {
        return 0;
    }

    public Rational getShutter() {
        return new Rational(0, 1);
    }

    public boolean isFlash() {
        return false;
    }

    public float getFocalLength() {
        return 0;
    }

    public String getQuality() {
        return NA;
    }

    public String getReport() {
        return NA;
    }

    public boolean saveThumbnailImage(StrippedJpeg im, OutputStream os/*, Dimension size*/)
            throws IOException {
        return false;
    }

    public Icon getThumbnailIcon(StrippedJpeg im, Dimension size) {
        int w = 100;
        int h = 100;
        int pix[] = new int[w * h];
        int index = 0;
        for (int y = 0; y < h; y++) {
            int red = (y * 255) / (h - 1);
            for (int x = 0; x < w; x++) {
                int blue = (x * 255) / (w - 1);
                pix[index++] = (255 << 24) | (red << 16) | blue;
            }
        }
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(
                new MemoryImageSource(w, h, pix, 0, w)));
    }

    int readMarker(InputStream is, boolean bodyOnly) {
        try {
            if (bodyOnly == false)
                if (is.read(header) < header.length)
                    return -1;
            data = new byte[2];
            if (is.read(data) < data.length)
                return -1;
            int len = bs2i(0, 2) - 2;
            data = new byte[len];
            return read(is, data) + header.length + 2;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}