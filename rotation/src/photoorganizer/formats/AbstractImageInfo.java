/* MediaChest $RCSfile: AbstractImageInfo.java,v $
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
 *  $Id: AbstractImageInfo.java,v 1.1 2003/09/05 08:24:36 rogatkin Exp $
 */
package photoorganizer.formats;
import java.util.Date;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.awt.Image;
import java.awt.Dimension;
import javax.swing.Icon;

public abstract class AbstractImageInfo extends BasicIo implements AbstractInfo {
	public static final Dimension DEFAULT_THUMB_SIZE = new Dimension (120, 96);
    public static final String EXT_JPEG = "Jpeg";

	final static DateFormat dateformat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	final static DecimalFormat fnumberformat = new DecimalFormat("F1:#0.0#");
	static final String NA = "n/a";
	final static byte [] BMP_SIG = { 0x42, 0x4D };
	final static int BMP24_HDR_SIZE = 54;
	
	protected static final Class [] EMPTY_PARAMS = {};
	
	// conversions
	public final static double[] AV_TO_FSTOP = 
	{1, 1.4, 2, 2.8, 4, 5.6, 8, 11, 16, 22, 32 };
	public final static Rational[] TV_TO_SEC =
	{new Rational(1,1), new Rational(1,2), new Rational(1,4), new Rational(1,8),
			new Rational(1,15), new Rational(1,30), new Rational(1,60), new Rational(1,125),
			new Rational(1,250), new Rational(1,500), new Rational(1,1000), new Rational(1,2000),
			new Rational(1,4000), new Rational(1,8000), new Rational(1,16000) };
	
	public AbstractImageInfo() {
	}

	public AbstractImageInfo(InputStream is, byte[] data, int offset, String name, String comments) throws FileFormatException {
		this.is = is;
		this.data = data;
		this.offset = offset;
		this.name = name;
		this.comments = comments;
		readInfo();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public abstract void readInfo() throws FileFormatException;

	public abstract String getFormat();
	
	public abstract int getResolutionX();

	public abstract int getResolutionY();
	
	public abstract String getMake();

	public abstract String getModel();
	
	public abstract String getDataTimeOriginalString();

	public abstract float getFNumber();

	public abstract Rational getShutter();

	public abstract boolean isFlash();

	public abstract String getQuality();

	public abstract float getFocalLength();

	public abstract int getMetering(); // matrix, dot, CenterWeightedAverage..
	
	public abstract int getExpoProgram(); // full automatic, ...

	public abstract String getReport();
	
	public abstract Icon getThumbnailIcon(StrippedJpeg im, Dimension size);

	public String toString() {
		String result = getReport();
		if (result != null && result.length() > 0)
			return result;
		return super.toString();
	}
	/** returns for format such attributes as: title, artist, album, year, file
	 */
	public Object[] getFiveMajorAttributes() {
		return fiveObjects;
	}
	
	public Icon getThumbnailIcon(StrippedJpeg im) { 
		return getThumbnailIcon(im, null);
	}
	
	public String getThumbnailExtension() {
		return EXT_JPEG;
	}

	public String getComments() {
		return comments;
	}

	/** saves thumbnail image to specified path
	 */
	public boolean saveThumbnailImage(StrippedJpeg im, OutputStream os/*, Dimension size*/) throws IOException {
		if (os == null || im == null)
			return false;
		return false;
	}

	public Date getDateTimeOriginal() {
		try {
			return dateformat.parse(getDataTimeOriginalString());
		} catch (NullPointerException e) {
		} catch (ParseException e) {
			  System.err.println(""+e);
		}
		return new Date();
	}

	// conversions
	public float apertureToFnumber(float aperture) {
		try {
			int si = (int)aperture;
			float result = (float)AV_TO_FSTOP[si];
			aperture -= si;
			if (aperture != 0)
				result += (AV_TO_FSTOP[si+1]-AV_TO_FSTOP[si])*aperture;
			return result;
		} catch(ArrayIndexOutOfBoundsException e) {
		}
		return -1;
	}
	// interface AbstractInfo
	
	public void setAttribute(String name, Object value) {
		if (COMMENTS.equals(name))
			comments = value.toString();
		else
			throw new RuntimeException("Calling this method not allowed by AbstractImageInfo implementation.");
	}
	
	public Object getAttribute(String name) {
		// TODO: get index from lookup map and use switch
		if (ESS_CHARACHTER.equals(name)) 			
			return getShutter();
		else if (ESS_TIMESTAMP.equals(name))
			return getDateTimeOriginal();
		else if (ESS_QUALITY.equals(name))
			return getQuality();
		else if (ESS_MAKE.equals(name))
			return getMake();
		else
			return getGenericAttribute(name);
	}
	
	public int getIntAttribute(String name) {
		if (ESS_CHARACHTER.equals(name))
			return (int)getFocalLength();
		else {
			Object result = getGenericAttribute(name);
			if (result != null) {
				if (result instanceof Integer)
					return ((Integer)result).intValue();
			} else
				return 0;
		}
		throw new IllegalArgumentException("Not supported attribute name for int "+name);
	}

	public float getFloatAttribute(String name) {
		if (ESS_CHARACHTER.equals(name))
			return getFNumber();
		else {
			Object result = getGenericAttribute(name);
			if (result != null) {
				if (result instanceof Float)
					return ((Float)result).floatValue();
			} else
				return 0;
		}
		throw new IllegalArgumentException("Not supported attribute name for float "+name);
	}
	
	public long getLongAttribute(String name) {
		throw new IllegalArgumentException("Not supported attribute name for long "+name);
	}
	
	public double getDoubleAttribute(String name) {
		throw new IllegalArgumentException("Not supported attribute name for double "+name);
	}
	
	public boolean getBoolAttribute(String name) {
		if (ESS_CHARACHTER.equals(name))
			return isFlash();
		return getGenericBoolAttribute(name).booleanValue();
	}	

	protected Object getGenericAttribute(String name) {
		try {
			return getClass().getMethod("get"+name, EMPTY_PARAMS).invoke(this, EMPTY_PARAMS);
		} catch(Throwable t) {
			throw new IllegalArgumentException("Not supported attribute "+name+" <<"+t);
		}
	}
	
	protected Boolean getGenericBoolAttribute(String name) {
		try {
			return (Boolean)getClass().getMethod("is"+name, EMPTY_PARAMS).invoke(this, EMPTY_PARAMS);
		} catch(Throwable t) {			
			try {
				return (Boolean)getGenericAttribute(name);
			} catch(Throwable t2) {
				throw new IllegalArgumentException("Not supported boolean attribute "+name+" <<"+t2+" <<"+t);
			}				
		}		
	}

	transient protected InputStream is;
	protected int offset;
	protected String name, comments;
	protected Object [] fiveObjects = new Object[5];
	// protected File file;
}