/* MediaChest 
 * Copyright (C) 1999 Dmitriy Rogatkin.  All rights reserved.
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
 *
 * $Id: FileNode.java,v 1.6 2013/06/07 01:13:56 cvs Exp $
 */
package photoorganizer.directory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;

import mediautil.gen.MediaFormat;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.renderer.MiscellaneousOptionsTab;

/* A FileNode is a derivative of the File class - though we delegate to 
 * the File object rather than subclassing it. It is used to maintain a 
 * cache of a directory's children and therefore avoid repeated access 
 * to the underlying file system during rendering. 
 */
public class FileNode { 
    File     file; 
    Object[] children;

    MediaFormat format;
	String encoding;
	
	boolean applyEnc;

	public FileNode(File file, String encoding, boolean applyForName) { 
		this.encoding = encoding;
		this.file = file;
		this.applyEnc = applyForName;
		//System.err.printf("Probing %s%n", file);
		if (file.isFile())
			format = MediaFormatFactory.createMediaFormat(file, encoding, false);
		//System.err.printf("Finishing as %s%n", format);
	}

	public void resetChildren() {
		children = null;
	}

    /**
     * Returns the the string to be used to display this leaf in the JTree.
     */
	public String toString() { 
		String result = (file.getName().length() == 0)?
						file.toString():
						file.getName();
		try {
			if (encoding != null && applyEnc)
				result = new String(((String)result).getBytes("ISO8859_1"), encoding);
		} catch(UnsupportedEncodingException uee) {
			System.err.println("Unsupported encoding "+encoding);
		}
		return result;
	}

	public File getFile() {
		return file; 
	}

	public MediaFormat getFormat() {
		return format; 
	}

    /**
     * Loads the children, caching the results in the children ivar.
     */
	protected Object[] getChildren() {
		if (children != null) {
			return children; 
		}
		try {
			String[] files = file.list();
			if(files != null) {
				String path = file.getPath();
				children = new FileNode[files.length];
				for(int i = 0; i < children.length; i++) {
					children[i] = new FileNode(new File(path, files[i]), encoding, applyEnc);
				}
				Arrays.sort((FileNode[])children, new Comparator<FileNode>() {
					public int compare(FileNode arg0, FileNode arg1) {
						int s1 = arg0.getFile().isDirectory()?0:1;
						int s2 = arg1.getFile().isDirectory()?0:1;
						if (s1 == s2)
							return arg0.getFile().getName().compareTo(arg1.getFile().getName());
						return s1 - s2;
					}});
			}
		} catch (SecurityException se) {}
		return children; 
	}
}
