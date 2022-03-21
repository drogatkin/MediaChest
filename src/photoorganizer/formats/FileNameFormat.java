/*
 * MediaChest - $RCSfile: FileNameFormat.java,v $
 * Copyright (C) 1999-2001 Dmitriy Rogatkin. All rights reserved.
 * Redistribution and use in source and
 * binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  1. Redistributions of source code must retain
 * the above copyright notice, this list of conditions and the following
 * disclaimer. 
 *  2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. THIS
 * SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * $Id: FileNameFormat.java,v 1.29 2008/02/26 05:09:08 dmitriy Exp $
 */
package photoorganizer.formats;

import java.text.Format;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import photoorganizer.Resources;
//import java.io.File;
import java.util.Date;
import java.util.Formatter;
import mediautil.gen.MediaInfo;
import mediautil.gen.MediaFormat;
import mediautil.gen.Rational;
import photoorganizer.ipod.PlayItem;
import photoorganizer.renderer.IpodOptionsTab;

public class FileNameFormat extends Format {
    // TODO: move to resources
    public final static String formatHelp = "<html>\n"
            + "<h2>Format escape sequences for pictures and music</h2>\n"
            + "<pre>\n"
            + " %D - date/time stamp in current locale,\n"
            + " %d - date stamp,\n"
            + " %t - time stamp,\n"
            + " %s - shutter speed/bitrate,\n"
            + " %a - aperture value/sample rate,\n"
            + " %f - flash/protected (yes, no),\n"
            + " %Y - light source,\n"
            + " %F - flash mode,\n"
            + " %q - quality/mode\n"
            + " %c - counter, can be specified as %0..0c, if leading zeros have to be present,\n"
            + " %o - original file name,\n"
            + " %r - transformation code expanded to: r90, r270, r180, TrPose, TrVerse, HMirr, VMirr\n"
            + " %R - orientation,\n" 
            + " %S - file size (undocumented),\n"
            + " %z - focal length (zoom)/year,\n"
            + " %M - make/artist\n"
            + " %m - model/title\n"
            + " %x - metering mode,\n"
            + " %X - exposure program\n"
            + " %n - type extension\n"
            + " Format escape sequences for music\n"
            + " %A - album,\n"
            + " %b - band,\n"
            + " %C - comment,\n"
            + " %L - language,\n"
            + " %g - genre,\n"
            + " %l - length,\n"
            + " %T - track,\n"
            + " %e - composer,\n"
            + " %E - conductor\n"
            + " </pre>";

    public static int counter;

    public static String timemask;

    public static String datemask;

    public static Object[] transformCodes;

    public FileNameFormat(String mask) {
        this(mask, false);
    }

    public FileNameFormat(String mask, boolean inccount) {
        this.mask = mask;
        this.inccount = inccount;
    }

    public FileNameFormat(String mask, int rotation) {
        this(mask);
        this.rotation = rotation;
    }

    public FileNameFormat(String mask, int rotation, boolean inccount) {
        this(mask, inccount);
        this.rotation = rotation;
    }

    final int TEXT = 0;

    final int ESC = 1;

    final char PERCENT = '%';

    public StringBuffer format(Object obj, StringBuffer toAppendTo,
            FieldPosition pos) {
        MediaInfo info = null;
        PlayItem pi = null;
        long imagefilelen = 0;
        try {
            info = ((MediaFormat) obj).getMediaInfo();
            imagefilelen = ((MediaFormat) obj).getFileSize();
        } catch (NullPointerException npe) {
        } catch (ClassCastException cce) {
            try {
                pi = (PlayItem) obj;
                imagefilelen = pi.getFile(IpodOptionsTab.getDevice()).length();
            } catch (ClassCastException cce1) {
                throw new IllegalArgumentException(
                        "Expected MediaFormat or PlayItem, but found "
                                + obj.getClass().getName());
            }
        }
        char c;
        int state = TEXT;
        String lead_zeros = "";
        StringBuffer result = new StringBuffer();
        Formatter f = new Formatter(result);
        // StringCharacterIterator
        for (int i = 0; mask != null && i < mask.length(); i++) {
            c = mask.charAt(i);
            switch (state) {
            case TEXT:
                if (c != PERCENT)
                    result.append(c);
                else
                    state = ESC;
                break;
            case ESC:
                state = TEXT;
                switch (c) {
                case 'D':
                    DateFormat dt;
                    if ((datemask == null || datemask.length() == 0)
                            || (timemask == null || timemask.length() == 0))
                        dt = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                DateFormat.SHORT);
                    else
                        dt = new SimpleDateFormat(datemask + timemask);
                    if (info != null)
                        result.append(getDateFormatted(dt, info));
                    break;
                case 'd':
                    DateFormat d;
                    if (datemask == null || datemask.length() == 0)
                        d = DateFormat.getDateInstance(DateFormat.SHORT);
                    else
                        d = new SimpleDateFormat(datemask);
                    if (info != null)
                        result.append(getDateFormatted(d, info));
                    break;
                case 't':
                    DateFormat t;
                    if (timemask == null || timemask.length() == 0)
                        t = DateFormat.getTimeInstance(DateFormat.SHORT);
                    else
                        t = new SimpleDateFormat(timemask);
                    if (info != null)
                        result.append(getDateFormatted(t, info));
                    break;
                case 's':
                    if (info != null)
                        try {
                            result.append(Rational.toExposureString(info
                                    .getAttribute(MediaInfo.SHUTTER)));
                        } catch (NullPointerException npe) {
                        } catch (IllegalArgumentException iae) {
                            System.err.println("pb:"+iae);
                            try {
                                result.append(info.getAttribute(
                                        MediaInfo.BITRATE).toString());
                            } catch (Exception e) {
                            }
                        }
                    break;
                case 'a':
                    if (info != null)
                        try {
                            result.append(info.getAttribute(
                                    MediaInfo.APERTURE).toString());
                        } catch (NullPointerException npe) {
                        } catch (IllegalArgumentException iae) {
                            try {
                                result.append(info.getAttribute(
                                        MediaInfo.SAMPLERATE).toString());
                            } catch (Exception e) {
                            }
                        }
                    break;
                case 'f':
                    if (info != null) {
                        try {
                            result
                                    .append(info
                                            .getBoolAttribute(MediaInfo.FLASH) ? Resources.CMD_YES
                                            : Resources.CMD_NO);
                        } catch (IllegalArgumentException iae) {
                            try {
                                // TODO: replace by VBR
                                result
                                        .append(info
                                                .getBoolAttribute(MediaInfo.PROTECTION) ? Resources.CMD_YES
                                                : Resources.CMD_NO);
                            } catch (IllegalArgumentException iae2) {
                            }
                        }
                    }
                    break;
                case 'q':
                    if (info != null)
                        try {
                            result.append(info.getAttribute(
                                    MediaInfo.QUALITY).toString());
                        } catch (NullPointerException npe) {
                        } catch (IllegalArgumentException iae) {
                            try {
                                result.append(info.getAttribute(
                                        MediaInfo.MODE).toString());
                            } catch (Exception e) {
                            }
                        }
                    break;
                case 'c':
                    if (lead_zeros.length() > 0)
                        lead_zeros += '0';
                    result
                            .append(new DecimalFormat(lead_zeros)
                                    .format(counter));
                    if (inccount)
                        counter++;
                    lead_zeros = "";
                    break;
                case 'o':
                    if (info != null)
                        try {
                            result.append(((MediaFormat) obj).getName());
                        } catch (NullPointerException npe) {
                        }
                    else if (pi != null)
                        result.append((String) pi.get(PlayItem.FILENAME));
                    break;
                case 'r':
                    result.append((String) transformCodes[rotation]);
                    break;
                case 'R':
                    if (info != null) 
                        try {
                            result.append(info.getAttribute(MediaInfo.ORIENTATION));
                        } catch(Exception e) {
                            //e.printStackTrace();
                        }
                    break;
                case 'S':
                    result.append(imagefilelen);
                    break;
                case 'z':
                    if (info != null)
                        try {
                            result.append(info.getAttribute(
                                    MediaInfo.FOCALLENGTH).toString());
                        } catch (NullPointerException npe) {
                        } catch (IllegalArgumentException iae) {
                            try {
                                result.append(info
                                        .getIntAttribute(MediaInfo.YEAR));
                            } catch (Exception e) {
                            }
                        }
                    else if (pi != null)
                        result.append(pi.get(PlayItem.YEAR));
                    break;
                case 'm':
                    if (info != null)
                        try {
                            result
                                    .append(info
                                            .getAttribute(MediaInfo.MODEL));
                        } catch (NullPointerException npe) {
                        } catch (IllegalArgumentException iae) {
                            try {
                                result.append(info.getAttribute(
                                        MediaInfo.TITLE).toString());
                            } catch (Exception e) {
                            }
                        }
                    else if (pi != null)
                        result.append(pi.get(PlayItem.TITLE));
                    break;
                case 'M':
                    if (info != null)
                        try {
                            result.append(info.getAttribute(MediaInfo.MAKE)
                                    .toString());
                        } catch (NullPointerException npe) {
                        } catch (IllegalArgumentException iae) {
                            try {
                                result.append(info.getAttribute(
                                        MediaInfo.ARTIST).toString());
                            } catch (Exception e) {
                            }
                        }
                    else if (pi != null)
                        result.append(pi.get(PlayItem.ARTIST));
                    break;
                case '0':
                    lead_zeros += '0';
                    state = ESC;
                    break;
                case 'A':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(MediaInfo.ALBUM)
                                    .toString());
                        else if (pi != null)
                            result.append(pi.get(pi.ALBUM));
                    } catch (Exception e) {
                    }
                    break;
                case 'b':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(MediaInfo.BAND)
                                    .toString());
                    } catch (Exception e) {
                    }
                    break;
                case 'C':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(
                                    MediaInfo.COMMENTS).toString());
                        else if (pi != null)
                            result.append(pi.get(PlayItem.COMMENT));
                    } catch (Exception e) {
                    }
                    break;
                case 'L':

                    try {
                        if (info != null)
                            result.append(info.getAttribute(
                                    MediaInfo.LANGUAGE).toString());
                    } catch (Exception e) {
                    }
                    break;
                case 'g':

                    try {
                        if (info != null)
                            result.append(MP3.findGenre(info));
                        else if (pi != null)
                            result.append(pi.get(PlayItem.GENRE));
                    } catch (Exception e) {
                    }
                    break;
                case 'l':
                    try {
                        if (info != null)
                            result.append(info
                                    .getAttribute(MediaInfo.ESS_TIMESTAMP));
                    } catch (Exception e) {
                    }
                    break;
                case 'T':

                    try {
                        if (info != null)
                            result.append(info
                                    .getIntAttribute(MediaInfo.TRACK));
                        else if (pi != null)
                            f.format("%02d", new Object[] { pi.get(pi.ORDER) });
                    } catch (Exception e) {
                    }
                    break;
                case 'e':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(
                                    MediaInfo.COMPOSER).toString());
                        else if (pi != null)
                            result.append(pi.get(pi.COMPOSER));
                    } catch (Exception e) {
                    }
                    break;
                case 'E':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(
                                    MediaInfo.CONDUCTOR).toString());
                    } catch (Exception e) {
                    }
                    break;
                case 'x':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(
                                    MediaInfo.METERING).toString());
                    } catch (Exception e) {
                    }
                    break;
                case 'X':
                    try {
                        if (info != null)
                            result.append(info.getAttribute(
                                    MediaInfo.EXPOPROGRAM).toString());
                    } catch (Exception e) {
                    }
                    break;
                case 'n':
                    if (info != null)
                        result.append(((MediaFormat) obj).getFormat(((MediaFormat) obj).getType()));
                    else if (pi != null)
                        result.append(pi.get(PlayItem.FILETYPE));
                    break;
                case PERCENT:
                    result.append(c);
                    break;
                default:
                    result.append(PERCENT);
                    result.append(c);
                }
            }
        }
        if (result.length() == 0) {
            //System.err.println("Object "+obj+" cannot be formatted, check the
            // mask "+mask+'.');
            result.append(obj.toString());

            //throw new IllegalArgumentException("Object can't be formatted,
            // check the mask "+mask+'.');
        }
        return result;
    }

    public Object parseObject(String source, ParsePosition pos) {
        return null;
    }

    public static String makeValidPathName(String name) {
        return makeValidPathName(name, null);
    }

    // TODO: scan all string for bad characters including UNICODE
    public static String makeValidPathName(String name, String ext) {
        if (name.indexOf('.') < 0 && ext != null)
            name += '.' + ext;
        char[] cs = name.toCharArray();
        boolean changed = false;
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            if (Character.isLetterOrDigit(c) == false && c != '.' && c != ' '
                    && c != '_' && c != '-' && c != '@' && c != '#' /*|| c > 127*/) {
                cs[i] = '_';
                changed = true;
            }
        }
        if (changed)
            return new String(cs);
        return name;
    }

    private static String getDateFormatted(DateFormat dt, MediaInfo info) {
        String dateFormatted = null;
        try {
            if (info != null)
                dateFormatted = dt.format(info
                        .getAttribute(MediaInfo.DATETIMEORIGINAL));
        } catch (IllegalArgumentException iae) {
        }
        if (dateFormatted == null)
            dateFormatted = dt.format(new Date());
        return dateFormatted;
    }

    private String mask;

    private int rotation;

    private boolean inccount;
}