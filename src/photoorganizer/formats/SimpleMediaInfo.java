/* MediaChest - $RCSfile: SimpleMediaInfo.java,v $                          
 * Copyright (C) 2001-2003 Dmitriy Rogatkin.  All rights reserved.                   
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
 *  $Id: SimpleMediaInfo.java,v 1.14 2013/05/13 00:57:56 cvs Exp $           
 */
package photoorganizer.formats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jwbroek.cuelib.CueParser;
import jwbroek.cuelib.CueSheet;
import jwbroek.cuelib.TrackData;
import mediautil.gen.MediaComponent;
import mediautil.gen.MediaInfo;

public class SimpleMediaInfo implements MediaInfo {
	File file;
	CueSheet cueSheet;
	HashMap<String, Object> attrsMap;
	String encoding;

	protected SimpleMediaInfo(File f, String e) {
		file = f;
		if (file.exists() == false || file.isDirectory())
			throw new IllegalArgumentException("File " + file + " doesn't exist or it's a directory");
		encoding = e;
		if (cueAware())
			try {
				processCue();
			} catch (IOException err) {
				System.err.printf("Exeption %s in processing .cue for %s%n", err, f);
			}
	}

	@Override
	public Object getAttribute(String attr) {
		if (LENGTH.equals(attr))
			return MP3.convertTime(getIntAttribute(LENGTH));
		if (ESS_MAKE.equals(attr))
			attr = ARTIST;
		if (attrsMap != null && attrsMap.containsKey(attr)) {
			return attrsMap.get(attr);
		}
		// try get method and move in getGenAttr
		try {
			return getClass().getMethod("get" + attr).invoke(this);
		} catch (Exception e) {
			if (__debug)
				System.err.printf("Error in obtaining atribute :%s - %s%n", attr, e);
		}

		// use string switch
		if (DATETIMEORIGINAL.equals(attr))
			return new Date(file.lastModified());
		return null;
	}

	@Override
	public boolean getBoolAttribute(String arg0) {
		if (attrsMap != null && attrsMap.containsKey(arg0)) {
			return "TRUE".equalsIgnoreCase((String) attrsMap.get(arg0));
		}
		return false;
	}

	@Override
	public double getDoubleAttribute(String arg0) {
		if (attrsMap != null && attrsMap.containsKey(arg0)) {
			try {
				Object v = attrsMap.get(arg0);
				if (v instanceof Number)
					return ((Number) v).doubleValue();
				else if (v instanceof String)
					return Double.parseDouble((String) v);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}

	@Override
	public Object[] getFiveMajorAttributes() {
		return new Object[] { getTitle(), getArtist(), getAlbum(), getYear(), getGenre() };
	}

	@Override
	public float getFloatAttribute(String arg0) {
		if (attrsMap != null && attrsMap.containsKey(arg0)) {
			try {
				Object v = attrsMap.get(arg0);
				if (v instanceof Number)
					return ((Number) v).floatValue();
				else if (v instanceof String)
					return Float.parseFloat((String) v);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}

	@Override
	public int getIntAttribute(String arg0) {
		if (attrsMap != null && attrsMap.containsKey(arg0)) {
			try {
				Object v = attrsMap.get(arg0);
				if (v instanceof Number)
					return ((Number) v).intValue();
				else if (v instanceof String)
					return Integer.parseInt((String) v);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}

	@Override
	public long getLongAttribute(String arg0) {
		if (attrsMap != null && attrsMap.containsKey(arg0)) {
			try {
				Object v = attrsMap.get(arg0);
				if (v instanceof Number)
					return ((Number) v).longValue();
				else if (v instanceof String)
					return Long.parseLong((String) v);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		if (attrsMap != null) {
			attrsMap.put(arg0, arg1);
		}
	}

	public String getName() {
		return file.getName();
	}

	public java.net.URL toURL() {
		try {
			return file.toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public String toString() {
		return String.format("Media: %s [%s]", "", file);
	}

	@Override
	public SimpleMediaComponent<TrackData>[] getComponents() {
		SimpleMediaComponent<TrackData>[] result = null;
		if (cueSheet != null) {
			List<TrackData> tds = cueSheet.getAllTrackData();
			result = new SimpleMediaComponent[tds.size()];
			int i = 0;
			for (TrackData td : tds) {
				result[i++] = new SimpleMediaComponent<TrackData>(td);
			}
			return result;
		}
		return null;
	}

	boolean cueAware() {
		return false;
	}

	void processCue() throws IOException {
		// TODO look in flac decoder readNextMetadata
		try (InputStream cueStream = getCueStream();) {
			if (cueStream == null)
				return;
			cueSheet = CueParser.parse(cueStream, encoding);
		}
	}

	protected InputStream getCueStream() throws IOException {
		String mediaName = file.getName();
		int extPos = mediaName.lastIndexOf('.');
		if (extPos > 0) {
		    if (file.getClass().getName() .contains("RemoteFile")) {
		        System.err.printf("Remote file CUE is currently not supported%n");
		        return null;
		    }
			File cueFile = new File(file.getParent(), mediaName.substring(0, extPos) + ".cue");
			if (cueFile.exists() == false)
				cueFile = new File(file.getParent(), mediaName + ".cue");
			return MediaFormatFactory.getInputStreamFactory().  getInputStream(cueFile);
		}

		return null;
	}

	public String getGenre() {
		if (cueSheet != null)
			return cueSheet.getGenre();
		if (attrsMap != null && attrsMap.containsKey(MediaInfo.GENRE)) {
			return (String) attrsMap.get(MediaInfo.GENRE);
		}
		return "";
	}

	public String getTitle() {
		if (cueSheet != null)
			return cueSheet.getTitle();
		if (attrsMap != null && attrsMap.containsKey(MediaInfo.TITLE)) {
			return (String) attrsMap.get(MediaInfo.TITLE);
		}
		return file.getName();
	}

	public String getAlbum() {
		if (cueSheet != null)
			return cueSheet.getTitle();
		if (attrsMap != null && attrsMap.containsKey(MediaInfo.ALBUM)) {
			return (String) attrsMap.get(MediaInfo.ALBUM);
		}
		return "";
	}

	public int getYear() {
		if (cueSheet != null) {
			int result = cueSheet.getYear();
			if (result > 0)
				return result;
		}
		if (attrsMap != null && attrsMap.containsKey(MediaInfo.YEAR)) {
			return (int) attrsMap.get(MediaInfo.YEAR);
		}
		return 0;
	}

	public final int extractInt(String v) {
		if (v == null || v.isEmpty())
			return 0;
		Matcher m = Pattern.compile("(\\d+)(.*)").matcher(v);
		if (m.matches()) {
			return Integer.valueOf(m.group(1)).intValue();
		}
		return 0;
	}

	public final String partOfSet(String part, String set) {
		if (part == null || set == null || part.isEmpty() || set.isEmpty())
			return "";
		return part + "/" + set;
	}

	public String getArtist() {
		if (cueSheet != null)
			return cueSheet.getPerformer();
		if (attrsMap != null && attrsMap.containsKey(MediaInfo.ARTIST)) {
			return (String) attrsMap.get(MediaInfo.ARTIST);
		}
		return "";
	}

	static class SimpleMediaComponent<B> implements MediaComponent {
		B delegate;

		SimpleMediaComponent(B d) {
			delegate = d;
		}

		@Override
		public <T> T getAttribute(String attr, T arg1) {
			try {
				return (T) delegate.getClass().getMethod("get" + attr).invoke(delegate);
			} catch (Exception e) {
				if (__debug)
					System.err.printf("Error in obtaining atribute :%s - %s%n", attr, e);
			}
			return arg1;
		}

	}

	static private final boolean __debug = false;
}
