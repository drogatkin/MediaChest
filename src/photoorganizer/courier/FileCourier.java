/* MediaChest - FileCourier 
 * Copyright (C) 1999-2005 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: FileCourier.java,v 1.12 2012/08/05 06:27:48 dmitriy Exp $
 */
package photoorganizer.courier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Courier;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.ThumbnailsOptionsTab;
import photoorganizer.renderer.WebPublishOptionsTab;

public class FileCourier implements Courier {
	Controller controller;
	IniPrefs s;
	
	public FileCourier(Controller controller) {
		this.controller = controller;
		s = controller.getPrefs();
	}

	public void deliver(StringBuffer buf, String destPath, String contentType, String encoding) throws IOException {
		FileOutputStream os = new FileOutputStream(new File(root, destPath));
		byte [] ba;
		if (encoding != null && encoding.length() > 0)
			ba = buf.toString().getBytes(encoding);
		else
			ba = buf.toString().getBytes();
		os.write(ba);
		os.flush();
		os.close();
	}

	public void deliver(String srcPath, String destPath) throws IOException {
		File sf = new File(srcPath);
		Stream.copyFile(sf, new File(root+destPath, sf.getName()));
		System.err.println("Copied "+sf+" to "+root+destPath+sf.getName());
	}
	
	public String deliver(MediaFormat format, String destPath, String cmask) throws IOException {
		MediaInfo ii = format.getMediaInfo();
		if (ii == null)
			return "";
                if (cmask == null)
                    cmask = mask;
		String name = FileNameFormat.makeValidPathName(new FileNameFormat(cmask, true).format(format), format.getThumbnailType());
		File of = new File(root+destPath, name);
		FileOutputStream os = new FileOutputStream(of);
		if (ii instanceof AbstractImageInfo)
			((AbstractImageInfo)ii).saveThumbnailImage(os);
		else
			os.write(format.getThumbnailData(null/*get desired dimension from props*/));
		os.flush();
		os.close();
		return new File(destPath, name).toString();
	}

	public void checkForDestPath(String path) throws IOException {
		new File(root+path).mkdirs();
	}
	
	public void init() throws IOException {
		mask = (String)s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FILEMASK);
		if (mask == null || mask.length() == 0)
			mask = PhotoCollectionPanel.DEFTNMASK;
		root = (String)s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.LOCAL_WEBROOT);
		if (root == null)
			root = "";
		else if (root.length() > 0 && root.charAt(root.length()-1) != '/' && root.charAt(root.length()-1) != '\\')
			root += File.separatorChar;
	}

	public boolean isLocal() {
		return true;
	}
	
	public boolean isContentIncluded() {
		return false;
	}

	public void done() {

	}
	
	public String getRootPathProperty() {
		return WebPublishOptionsTab.LOCAL_WEBROOT;
	}
	
	public String getMediaPathProperty() {
		return WebPublishOptionsTab.LOCAL_IMAGEPATH;
	}
	
	public String getThumbnailsPathProperty() {
		return WebPublishOptionsTab.LOCAL_TNWEBPATH;
	}
	
	public String getMediaUrlProperty()  {
		return WebPublishOptionsTab.LOCAL_IMAGEURL;
	}
	
	public String getTemplatePropertyName() {
		return WebPublishOptionsTab.HTMLTEMPL;
	}
	
	String mask;
	String root;
}