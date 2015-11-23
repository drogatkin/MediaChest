/* MediaChest - $RCSfile: PlayList.java,v $ 
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
 *  $Id: PlayList.java,v 1.42 2008/02/16 05:24:53 dmitriy Exp $
 */
package photoorganizer.ipod;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

import org.aldan3.util.inet.HttpUtils;

import photoorganizer.PhotoOrganizer;

public class PlayList extends ArrayList<PlayItem> {
	public static final int TITLE = PlayItem.TITLE;
	public static final int SMART = BaseHeader.SMARTLISTENTRY;
	public static final int ID = BaseHeader.PLAYLISTENTRY;
	public static final int ICON = 1001;
	public static final int HASH1 = 6001;
	public static final int HASH2 = 6002;
	public static final int LASTMOD = 6003;
	
	protected String name, originalName;
	protected int id;
	protected Point position;
	protected boolean virtual;
	protected boolean fileDirectory;
	protected Icon icon;
	protected int hash1, hash2;
	protected Date lastDate;
	
	public BaseHeader.Smart smart;
	
	protected List deletedItems;
	
	public PlayList(String name, boolean virtual, boolean fileDirectory) {
		this.name = name;
		this.virtual = virtual;
		this.fileDirectory = fileDirectory;
		if (name != null) {
		    hash1 = name.hashCode();
		    hash2 = (int)System.currentTimeMillis();
		}
	}

	public PlayList(String name) {
		this(name, true, false);
	}

    public PlayList(String name, int h1, int h2) {
        this(name);
        hash1 = h1;
        hash2 = h2;
    }
	
	public Object clone() {
		PlayList result = new PlayList(name, virtual, fileDirectory);
		result.ensureCapacity(size());
		result.icon = icon;
		result.smart = smart;
		result.deletedItems = deletedItems;
		result.id = id;
		result.hash1 = hash1;
		result.hash2 = hash2;
		result.lastDate = lastDate;
		synchronized(this) {
			for (int i=0; i<size(); i++)
				result.add(get(i));
		}
		return result;
	}
	
	public void setAttribute(int index, int value) {
		if (index == ID)
			id = value;
		else if (index == HASH1) {
		    if (value != 0)
		        hash1 = value;
		} else if (index == HASH2) {
		    if (value != 0)
		        hash2 = value;
		} else if (index == LASTMOD) {
		    if (value != 0)
		        lastDate = BaseHeader.toDate(value);
		} else
			throw new IllegalArgumentException("Index "+index+" is not allowed.");
	}

	public void setAttribute(int index, Object value) {
		if (index == TITLE)
			name = (String)value;
		else if (index == SMART)
			smart = (BaseHeader.Smart)value; // review
		else if (index == ICON)
			icon = (Icon)value;
		else if (index == LASTMOD)
		    lastDate = (Date)value;
		else
			throw new IllegalArgumentException("Index "+index+" is not allowed.");
	}
	
	public Object getAttribute(int index) {
		switch(index) {
		case ICON:
			return icon;
		case TITLE:
			return name;
		case SMART:
			return smart; // review
		case ID:
			return new Integer(id);
		case HASH1:
			return new Integer(hash1);
		case HASH2:
			return new Integer(hash2);
		case LASTMOD:
			return lastDate;
		default:
			throw new IllegalArgumentException("Index "+index+" is not allowed.");
		}
	}
	
	public synchronized PlayItem remove(PlayItem playItem) {
		if (super.remove(playItem)) {
			if (fileDirectory && (virtual == false) && playItem.isState(PlayItem.STATE_COPIED)) {
				if (deletedItems == null)
					deletedItems = new ArrayList(10);
				deletedItems.add(playItem); // add for file deletion
			}
			return playItem;
		}
		return null;
	}
	
	public synchronized boolean isRemoved(PlayItem playItem) {
		return deletedItems==null?false:deletedItems.contains(playItem);
	}
	
	public synchronized void restore(PlayItem playItem) {
		if (deletedItems != null)
			deletedItems.remove(playItem);
	}
	
	public List getDeletedItems() {
		return deletedItems;
	}
	
	public synchronized void clearDeleted() {
		if (deletedItems == null)
			return;
		Iterator i = deletedItems.iterator();
		while (i.hasNext()) {
			PlayItem pi = (PlayItem)i.next();
			if (pi.isState(PlayItem.STATE_DELETED + PlayItem.STATE_COPIED) &&
				pi.get(pi.FILENAME) == null) {
				i.remove();
				
			}
		}
	}
	
	public synchronized boolean isChanged() {
		if (originalName != null)
			return true;
//System.err.println("no rename");
		if (deletedItems != null && deletedItems.size() > 0)
			return true;
//System.err.println("No delete");
		if (smart != null && smart.isChanged())
			return true;
//System.err.println("No smart change");
		Iterator i = iterator();
		while(i.hasNext()) {
			PlayItem pi = (PlayItem)i.next();
			if (pi != null && pi.isState(pi.STATE_COPIED + pi.STATE_METASYNCED) == false) {
//System.err.println("PI changed "  +pi+",state "+pi.getState());
				return true;    }
		}
//System.err.println("No change");
		return false;
	}
	
	public boolean isVirtual() {
		return virtual;
	}
	
	public boolean isFileDirectory() {
		return fileDirectory;
	}
	
	public void rename(String name) {
		if (originalName == null)
			originalName = this.name;
		this.name = name;
	}
	
	public String toString() {
		return name != null?name:":"+id;
	}
	
	public boolean isList(int h1, int h2) {
	    return hash1 == h1 && hash2 == h2;
	}
	
	public String toXmlString() {
		// TODO: use class XmlBuffer
		StringBuffer result = new StringBuffer(12*1024);
		result.append("<PlayList name=\"").append(HttpUtils.htmlEncode(name)).append("\" smart=\"").append(smart==null?"no":"yes").append("\">\r\n");
		Iterator i = iterator();
		while(i.hasNext())
			result.append(((PlayItem)i.next()).toXML()).append("\r\n");
		result.append("</PlayList>");
		return result.toString();
	}

	public String toCsvString() { 
		StringBuffer result = new StringBuffer(6*1024);
		result.append(PlayItem.EXPORT_FIELDS[0].name);
		for (int i = 1; i < PlayItem.EXPORT_FIELDS.length; i++) {
			result.append(',').append(PlayItem.EXPORT_FIELDS[i].name);
		}
		result.append("\r\n");
		Iterator i = iterator();
		while(i.hasNext())
			result.append(((PlayItem)i.next()).toCSV()).append("\r\n");
		return result.toString();
	}
	
	public String toWplString(String dev) {
		// TODO: use class XmlBuffer
		StringBuffer result = new StringBuffer(12*1024);
		result.append("   <head>\n").append(
                      "        <meta name=\"Generator\" content=\"").append(PhotoOrganizer.PROGRAMNAME).append(" -- ").append(PhotoOrganizer.VERSION).append("\"/>\n").append(
                      "        <author/>\n").append(
					  "        <title>").append(name).append("</title>\n").append(
                      "   </head>\n"+
					  "   <body>\n"+
					"	<seq>\n");

		Iterator i = iterator();
		while(i.hasNext()) {
			result.append("         <media src=\"");
			PlayItem pi = (PlayItem)i.next();
			String fn = (String)pi.get(pi.FILENAME);
			if (pi.isState(PlayItem.STATE_COPIED))
				result.append(dev).append(fn.replace(':', File.separatorChar));
			else
				result.append(fn);			
			result.append("\" "/*tid="*/).append("/>\n");
		}
		result.append("        </seq>\n    </body>\n");
		return result.toString();
	}
	
	public String getOrigalName() {
		return (originalName == null)?toString():originalName;
	}

	public synchronized int hashCode() {
		// TODO here is a problem when has code is requested some other thread can modify contetnt
		// so concurrent update exception can be thrown
		return super.hashCode() + id + (name==null?0:name.hashCode());
	}
	
	/** @return  total length in ms of the play list
	 */
	public long getLength() {
		// no caching
		long result = 0;
		for (int i=0; i<size(); i++)
			result += ((Integer)((PlayItem)get(i)).get(PlayItem.LENGTH)).intValue();
		return result;
	}

	/** @return total size of all files in bytes, can work wrong if some files
	 * are bigger than 2gig
	 */
	public long getSizeOf() {
		// no caching
		long result = 0;
		for (int i=0; i<size(); i++)
			result += ((Integer)((PlayItem)get(i)).get(PlayItem.SIZE)).intValue();
		return result;
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof PlayList))
			return false;
		PlayList pl = (PlayList)o;
		return super.equals(o) && pl.id == id && 
			   (name != null?name.equals(pl.name):name==pl.name);
	}
	
	public void setViewPosition(Point position) {
		this.position = position;
	}
	
	public Point getViewPosition() {
		return position;
	}
}