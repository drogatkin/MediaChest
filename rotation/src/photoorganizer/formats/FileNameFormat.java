/* MediaChest - $RCSfile: FileNameFormat.java,v $
 * Copyright (C) 1999-2001 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: FileNameFormat.java,v 1.2 2008/01/05 05:28:07 dmitriy Exp $
 */
package photoorganizer.formats;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileNameFormat extends Format {
	public static final String CMD_YES = "Yes";
	public static final String CMD_NO = "No";
	public final static String formatHelp = "<html>\n"
		+"<h2>Format escape sequences for pictures and music</h2>\n"
		+"<pre>\n"
		+" %D - date/time stamp in current locale,\n"
		+" %d - date stamp,\n"
		+" %t - time stamp,\n"
		+" %s - shutter speed/bitrate,\n"
		+" %a - aperture value/sample rate,\n"
		+" %f - flash/protected (yes, no),\n"
		+" %q - quality/mode\n"
		+" %c - counter, can be specified as %0..0c, if leading zeros have to be present,\n"
		+" %o - original file name,\n"
		+" %r - transformation code expanded to: r90, r270, r180, TrPose, TrVerse, HMirr, VMirr\n"
		+" %S - file size (undocumented),\n"
		+" %z - focal length (zoom)/year,\n"
		+" %M - make/artist\n"
		+" %m - model/title\n"
		+" %x - metering mode,\n"
		+" %X - exposure program\n"
		+" %n - type extension\n"
		+" Format escape sequences for music\n"
		+" %A - album,\n"
		+" %b - band,\n"
		+" %C - comment,\n"
		+" %L - language,\n"
		+" %g - genre,\n"
		+" %l - length,\n"
		+" %T - track,\n"
		+" %e - composer,\n"
		+" %E - conductor\n"
		+" </pre>";

	public static int counter;
    public static String timemask;
    public static String datemask;
    public static Object []transformCodes;

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
	final int ESC  = 1;
	final char PERCENT = '%';
	
	public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
		AbstractInfo info = null;
		long imagefilelen=0;
		try {
			info = ((AbstractFormat)obj).getInfo();
			imagefilelen = ((AbstractFormat)obj).getFileSize();
		} catch(NullPointerException npe) {
		} catch(ClassCastException e) {
			throw new IllegalArgumentException("Expected AbstractFormat, but found "+obj.getClass().getName());
		}
		char c;
		int state = TEXT;
		String lead_zeros = "";
		StringBuffer result = new StringBuffer();
		// StringCharacterIterator	
		for(int i = 0; mask != null && i < mask.length(); i++) {
			c = mask.charAt(i);
			switch(state) {
				case TEXT:
					if (c != PERCENT)
						result.append(c);
					else
						state = ESC;
					break;
				case ESC:
					state = TEXT;
					switch(c) {
						case 'D':
							DateFormat dt;
							if ((datemask == null || datemask.length() == 0) ||
								(timemask == null || timemask.length() == 0))
								dt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
							else
								dt = new SimpleDateFormat(datemask+timemask);
							result.append(getDateFormatted(dt, info));
							break;
						case 'd':
							DateFormat d;
							if (datemask == null || datemask.length() == 0)
								d = DateFormat.getDateInstance(DateFormat.SHORT);
							else
								d = new SimpleDateFormat(datemask);
							result.append(getDateFormatted(d, info));
							break;
						case 't':
							DateFormat t;
							if (timemask == null || timemask.length() == 0)
								t = DateFormat.getTimeInstance(DateFormat.SHORT);
							else
								t = new SimpleDateFormat(timemask);
							result.append(getDateFormatted(t, info));
							break;
						case 's':
							try {
								result.append(Rational.toExposureString(info.getAttribute(AbstractInfo.SHUTTER)));
							} catch(NullPointerException npe) {
							} catch(IllegalArgumentException iae) {
								  try {
									  result.append(info.getAttribute(AbstractInfo.BITRATE).toString());
								  } catch(Exception e) {
								  }
							}
							break;
						case 'a':
							try {
								result.append(info.getAttribute(AbstractInfo.APERTURE).toString());
							} catch(NullPointerException npe) {
							} catch(IllegalArgumentException iae) {
								  try {
									  result.append(info.getAttribute(AbstractInfo.SAMPLERATE).toString());
								  } catch(Exception e) {
								  }
							}
							break;
						case 'f':
							if (info != null) {
								try {
									result.append(info.getBoolAttribute(AbstractInfo.FLASH)?CMD_YES:CMD_NO);
								} catch(IllegalArgumentException iae) {
									try {
										// TODO: replace by VBR
										result.append(info.getBoolAttribute(AbstractInfo.PROTECTION)?CMD_YES:CMD_NO);
									} catch(IllegalArgumentException iae2) {
									}
								}
							}
							break;
						case 'q':
							try {
								result.append(info.getAttribute(AbstractInfo.QUALITY).toString());
							} catch(NullPointerException npe) {
							} catch(IllegalArgumentException iae) {
								  try {
									  result.append(info.getAttribute(AbstractInfo.MODE).toString());
								  } catch(Exception e) {
								  }
							}
							break;
						case 'c':
							if (lead_zeros.length() > 0)
								lead_zeros+='0';
							result.append(new DecimalFormat(lead_zeros).format(counter));
							lead_zeros = "";
							if (inccount)
								counter++;
							break;
						case 'o':
							try {
								result.append(((AbstractFormat)obj).getName());
							} catch(NullPointerException npe) {
							}
							break;
						case 'r':
							result.append((String)transformCodes[rotation]);
							break;
						case 'S':
							result.append(imagefilelen);
							break;
						case 'z':
							try {
								result.append(info.getAttribute(AbstractInfo.FOCALLENGTH).toString());
							} catch(NullPointerException npe) {
							} catch(IllegalArgumentException iae) {
								  try {
									  result.append(info.getIntAttribute(AbstractInfo.YEAR));
								  } catch(Exception e) {
								  }
							}
							break;
						case 'm':
							try {
								result.append(info.getAttribute(AbstractInfo.MODEL));
							} catch(NullPointerException npe) {
							} catch(IllegalArgumentException iae) {
								  try {
									  result.append(info.getAttribute(AbstractInfo.TITLE).toString());
								  } catch(Exception e) {
								  }
							}
							break;
						case 'M':
							try {
								result.append(info.getAttribute(AbstractInfo.MAKE).toString());
							} catch(NullPointerException npe) {
							} catch(IllegalArgumentException iae) {
								  try {
									  result.append(info.getAttribute(AbstractInfo.ARTIST).toString());
								  } catch(Exception e) {
								  }
							}
							break;
						case '0':
							lead_zeros+='0';
							state = ESC;
							break;
						case 'A':
							try {
								result.append(info.getAttribute(AbstractInfo.ALBUM).toString());
							} catch(Exception e) {
							}
							break;
						case 'b':
							try {
								result.append(info.getAttribute(AbstractInfo.BAND).toString());
							} catch(Exception e) {
							}
							break;
						case 'C':
							try {
								result.append(info.getAttribute(AbstractInfo.COMMENTS).toString());
							} catch(Exception e) {
							}
							break;
						case 'L':
							try {
								result.append(info.getAttribute(AbstractInfo.LANGUAGE).toString());
							} catch(Exception e) {
							}
							break;
						case 'l':
							try {
								result.append(info.getAttribute(AbstractInfo.ESS_TIMESTAMP));
							} catch(Exception e) {
							}
							break;
						case 'T':
							try {
								result.append(info.getIntAttribute(AbstractInfo.TRACK));
							} catch(Exception e) {
							}
							break;
						case 'e':
							try {
								result.append(info.getAttribute(AbstractInfo.COMPOSER).toString());
							} catch(Exception e) {
							}
							break;
						case 'E':
							try {
								result.append(info.getAttribute(AbstractInfo.CONDUCTOR).toString());
							} catch(Exception e) {
							}
							break;
						case 'x':
							try {
								result.append(info.getAttribute(AbstractInfo.METERING).toString());
							} catch(Exception e) {
							}
							break;
						case 'X':
							try {
								result.append(info.getAttribute(AbstractInfo.EXPOPROGRAM).toString());
							} catch(Exception e) {
							}
							break;
						case 'n':
								result.append(((AbstractFormat)obj).getType());
							break;							
						case PERCENT:
							result.append(c);
							break;
						default:
							result.append(PERCENT); result.append(c);
					}
			}
		}
		if (result.length() == 0) {
			//System.err.println("Object "+obj+" cannot be formatted, check the mask "+mask+'.');
			result.append(obj.toString());
			
			//throw new IllegalArgumentException("Object can't be formatted, check the mask "+mask+'.');
		}
		return result;
	}

	public Object parseObject (String source, ParsePosition pos) {
		return null;
	}

	public static String makeValidPathName(String name) {
		return makeValidPathName(name, null);
	}

	// TODO: scan all string for bad characters including UNICODE
	public static String makeValidPathName(String name, String ext) {
		if (name.indexOf('.') < 0 && ext != null)
			name += '.'+ext;
		char[] cs = name.toCharArray();
		boolean changed = false;
		for (int i=0; i<cs.length; i++) {
			char c = cs[i];
			if (Character.isLetterOrDigit(c) == false && c != '.' && c != ' ' 
				&& c!= '_' && c != '-' && c != '@' && c != '#' || c > 127) {
				cs[i] = '_';
				changed = true;
			}
		}
		if (changed)
			return new String(cs);
		return name;
	}

	private static String getDateFormatted(DateFormat dt, AbstractInfo info) {
		String dateFormatted = null;
		try {
			if (info != null)
				dateFormatted = dt.format(info.getAttribute(AbstractInfo.DATETIMEORIGINAL));
		} catch (IllegalArgumentException iae){
		}
		if (dateFormatted == null)
			dateFormatted = dt.format(new Date());
		return dateFormatted;
	}
	
	private String mask;
	private int rotation;
	private boolean inccount;
}