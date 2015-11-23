/* MediaChest - $RCSfile: ContentMatcher.java,v $                          
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
 *  $Id: ContentMatcher.java,v 1.17 2012/10/18 06:58:58 cvs Exp $           
 */

package photoorganizer.media;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import photoorganizer.formats.MP3;
import photoorganizer.ipod.PlayItem;

public class ContentMatcher implements Serializable {

	public boolean match(MediaFormat format) {
		if (format.isValid() && format.getType() == MediaFormat.AUDIO) { // can use ==
																// instead of
																// equals()
			MediaInfo info = format.getMediaInfo();
			boolean match = !excludeMode;
			if (genresMap != null)
				try {
					match = excludeMode ^ genresMap.get(MP3.findGenre(info)) != null;
				} catch (Exception e) {
				}
			if (!match)
				return false;
			if (artistsMap != null)
				try {
					match = excludeMode
							^ artistsMap.get(normalize(info.getAttribute(MediaInfo.ARTIST).toString().toCharArray())) != null;
				} catch (Exception e) {
				}
			if (!match)
				return false;
			if (yearsMap != null)
				try {
					match = excludeMode ^ yearsMap.get(new Integer(info.getIntAttribute(MediaInfo.YEAR))) != null;
				} catch (Exception e) {
				}
			if (!match)
				return false;
			if (othersMap != null) {
				Iterator i = othersMap.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry e = (Map.Entry) i.next();
					// considering everythings others as type in the Map
					String name = (String) e.getKey();
					Map values = (Map) e.getValue();
					try {
						match = excludeMode
								^ values.get(normalize(info.getAttribute(name).toString().toCharArray())) != null;
					} catch (Exception ex) {
					}
					if (match)
						break;
				}
			}
			return match;
		}
		return false;
	}

	// protected static final String OTHER_ATTRIBUTES_FLOW = "";
	public boolean match(PlayItem playItem) {
		boolean match = !excludeMode;

		if (genresMap != null)
			try {
				match = excludeMode
						^ genresMap.get(normalize(playItem.get(PlayItem.GENRE).toString().toCharArray())) != null;
			} catch (Exception e) {
			}
		if (!match)
			return false;
		if (artistsMap != null)
			try {
				match = excludeMode
						^ artistsMap.get(normalize(playItem.get(PlayItem.ARTIST).toString().toCharArray())) != null;
			} catch (Exception e) {
			}
		if (!match)
			return false;
		if (yearsMap != null)
			try {
				match = excludeMode ^ yearsMap.get(playItem.get(PlayItem.YEAR)) != null;
			} catch (Exception e) {
			}
		if (!match)
			return false;
		if (othersMap != null) {
			Iterator i = othersMap.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry) i.next();
				// considering everythings others as type in the Map
				String name = (String) e.getKey();
				String value = null;
				if (name.equals(MediaInfo.COMPOSER))
					value = (String) playItem.get(PlayItem.COMPOSER);
				else if (name.equals(MediaInfo.ALBUM))
					value = (String) playItem.get(PlayItem.ALBUM);
				if (value != null) {
					Map values = (Map) e.getValue();
					try {
						match = excludeMode ^ values.get(normalize(value.toCharArray())) != null;
					} catch (Exception ex) {
					}
				}
				if (match)
					break;
			}
		}
		if (statMap != null) {

		}
		return match;

	}

	public void setArtists(Object[] artists) {
		artistsMap = buildMap(artists, true);
	}

	public void setGenres(Object[] genres) {
		// TODO: consider convert to Integer and fill the map
		genresMap = buildMap(genres, false);
	}

	public void setYears(Object[] years) {
		yearsMap = buildMap(years, false);
	}

	public void setExtraConditions(Object[] conditions) {
		if (conditions == null || conditions.length == 0)
			return;
		othersMap = new HashMap(conditions.length);
		for (int i = 0; i < conditions.length; i++) {
			String s = (String) conditions[i];
			int bp = s.indexOf('{');
			if (bp <= 0)
				continue; // invalid entry
			String value = s.substring(0, bp);
			System.err.println("Val =" + value);
			int ep = s.indexOf('}');
			String attr;
			if (ep < 0)
				attr = s.substring(bp + 1);
			else
				attr = s.substring(bp + 1, ep);
			Map values = (Map) othersMap.get(attr);
			if (values == null) {
				values = new HashMap();
				othersMap.put(attr, values);
			}
			String s1 = normalize(value.toCharArray());
			values.put(s1, s1);
		}
	}

	public void setStatParams(Object[] params) {
		if (params != null && params instanceof StatParameter[]) {
			statMap = new HashMap(5);
			for (int i = 0; i < params.length; i++)
				if (params[i] != null)
					statMap.put(((StatParameter) params[i]).name, params[i]);
		} else
			statMap = null;
	}

	public void setExcludeMode(boolean excludeMode) {
		this.excludeMode = excludeMode;
	}

	// getters
	public Object[] getGenres() {
		return genresMap.values().toArray();
	}

	protected Map buildMap(Object[] objects, boolean normalized) {
		if (objects == null || objects.length == 0)
			return null;
		HashMap result = new HashMap(objects.length);
		for (int i = 0; i < objects.length; i++)
			if (normalized) {
				char[] s = objects[i].toString().toCharArray();
				String s1 = normalize(s);
				// System.err.println("Entry "+s1+" hashcode "+s1.hashCode());
				// result.put(normalize(s), s);
				result.put(s1, s);
				// System.err.println("Added entry
				// "+Controller.toHexString(normalize(s))+'
				// '+normalize(s).hashCode());
			} else {
				result.put(objects[i], objects[i]);
			}
		return result;
	}

	/** 
	 */
	public static String normalize(char[] s) {
		// TODO: it can remove articles also
		int k = 0;
		for (int i = 0; i < s.length; i++)
			if (Character.isLetterOrDigit(s[i]))
				s[k++] = Character.toUpperCase(s[i]);
		if (k > 0)
			return new String(s, 0, k);
		return null; // new String();
		// throw new IllegalArgumentException("Argument "+new String(s)+" cannot
		// be normalized.");
	}

	public static class StatParameter {
		String name;

		Object value;

		int condition;

		public StatParameter(String name, Object value, int condition) {
			this.name = name;
			this.value = value;
			this.condition = condition;
		}
	}

	Map artistsMap, genresMap, yearsMap, othersMap, statMap;

	boolean excludeMode;
}
