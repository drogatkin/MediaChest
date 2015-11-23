/** MediaChest - MailCourier 
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
 *  $Id: MailCourier.java,v 1.14 2012/08/05 06:27:48 dmitriy Exp $
 */
package photoorganizer.courier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;

import org.aldan3.util.inet.HttpUtils;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;
import photoorganizer.Controller;
import photoorganizer.Courier;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import photoorganizer.renderer.WebPublishOptionsTab;

public class MailCourier implements Courier {
	Multipart multipart, alternative;

	Controller controller;

	int counter;

	public MailCourier(Controller controller, Multipart multipart, Multipart alternative) {
		this.controller = controller;
		this.multipart = multipart;
		this.alternative = alternative;
	}

	public void deliver(StringBuffer buf, String destPath, String contentType, String encoding) throws IOException {
		try {
			MimeBodyPart bp = new MimeBodyPart();
			bp.setDataHandler(new DataHandler(new TextDataSource(buf, contentType, encoding)));
			alternative.addBodyPart(bp);
		} catch (MessagingException me) {
			throw new IOException("" + me);
		}
	}

	public void deliver(String srcPath, String destPath) throws IOException {
		try {
			// multipart.addBodyPart(new MimeBodyPart(new
			// FileInputStream((String)images_to_copy.elementAt(i))));
			MimeBodyPart bp = new MimeBodyPart();
			bp.setDataHandler(new DataHandler(new ImageDataSource(srcPath)));
			bp.addHeader("Content-ID", "<" + srcPath + '>');
			String fileName = new File(srcPath).getName();
			if (fileName.length() > 78) {
				// do split as in RFC 2184 http://www.faqs.org/rfcs/rfc2184.html
				fileName = fileName.substring(0, 77);
			}
			fileName = HttpUtils.urlEncode(fileName);
			bp.addHeader("Content-Disposition", "attachment; filename="+fileName);
			// see RFC 2183 ,  http://www.faqs.org/rfcs/rfc2183.html
			multipart.addBodyPart(bp);
		} catch (MessagingException me) {
			throw new IOException("" + me);
		}
	}

	public String deliver(MediaFormat mf, String destPath, String cmask) throws IOException {
		try {
			if (mf.isValid() == false)
				return "";
			MediaInfo mi = mf.getMediaInfo();
			ThumbnailImageDataSource ids = new ThumbnailImageDataSource("media" + counter++, mf.getThumbnailType());
			if (mi instanceof AbstractImageInfo)
				((AbstractImageInfo) mi).saveThumbnailImage(ids.getOutputStream());
			else
				ids.getOutputStream().write(mf.getThumbnailData(null/*
																	 * get
																	 * desired
																	 * dimension
																	 * from
																	 * props
																	 */));
			MimeBodyPart bp = new MimeBodyPart();
			bp.setDataHandler(new DataHandler(ids));
			bp.addHeader("Content-ID", "<" + ids.getName() + '>');
			// bp.setContentType(ids.getContentType());
			multipart.addBodyPart(bp);
			ids.close();
			return "cid:" + ids.getName();// not java.net.URLEncoder.encode()
		} catch (MessagingException me) {
			throw new IOException("" + me);
		}
	}

	public void checkForDestPath(String path) throws IOException {
	}

	public void init() throws IOException {
	}

	public void done() {

	}

	public boolean isLocal() {
		return false;
	}

	public boolean isContentIncluded() {
		return true;
	}

	public String getRootPathProperty() {
		assert false;
		return null;
	}

	public String getMediaPathProperty() {
		assert false;
		return null;
	}

	public String getThumbnailsPathProperty() {
		assert false;
		return null;
	}

	public String getMediaUrlProperty() {
		assert false;
		return null;
	}
	
	public String getTemplatePropertyName() {
		return WebPublishOptionsTab.EMAIL_HTMLTEMPL;
	}
}

class TextDataSource implements DataSource {
	StringBuffer html;

	String contentType, encoding;

	TextDataSource(StringBuffer html, String contentType, String encoding) {
		this.html = html;
		this.contentType = contentType;
		this.encoding = encoding;
	}

	public InputStream getInputStream() throws IOException {
		if (encoding != null && encoding.length() > 0)
			return new ByteArrayInputStream(html.toString().getBytes(encoding));
		else
			return new ByteArrayInputStream(html.toString().getBytes());
	}

	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	public String getContentType() {
		return contentType + ((encoding != null && encoding.length() > 0) ? ("; charset=" + encoding) : "");
	}

	public String getName() {
		return "Album";
	}
}

class ImageDataSource implements DataSource {
	String path;

	InputStream is;

	ImageDataSource(String path) {
		this.path = path;
	}

	synchronized public InputStream getInputStream() throws IOException {
		close();
		is = new FileInputStream(path);
		return is;
	}

	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	public String getContentType() {
		String name = getName();
		int dp = name.lastIndexOf('.');
		String ext;
		if (dp < 0)
			ext = Resources.EXT_JPEG;
		else
			ext = name.substring(dp + 1);
		if ("jpg".equalsIgnoreCase(ext) || ext.length() == 0)
			ext = Resources.EXT_JPEG;
		else if (MP3.TYPE.equalsIgnoreCase(ext))
			return "audio/mpeg";
		return "image/" + ext;
	}

	public void close() throws IOException {
		if (is != null)
			is.close();
		is = null;
	}

	public String getName() {
		return new File(path).getName();
	}

	/*
	 * protected void finalize() throws Throwable { super.finalize(); }
	 */
}

class ThumbnailImageDataSource implements DataSource {
	ByteArrayOutputStream os;

	String name, ext;

	ThumbnailImageDataSource(String name, String ext) {
		this.name = name;
		this.ext = ext;
		os = new ByteArrayOutputStream(8096);
	}

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(os.toByteArray());
	}

	public OutputStream getOutputStream() throws IOException {
		return os;
	}

	public String getContentType() {
		return "image/" + ext;
	}

	public String getName() {
		return name;
	}

	public void close() throws IOException {
		os.flush();
		os.close();
	}
}
