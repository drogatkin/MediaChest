/* MediaChest - FTPCourier 
 * Copyright (C) 1999-2000 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: FTPCourier.java,v 1.11 2012/08/05 06:27:48 dmitriy Exp $
 */
package photoorganizer.courier;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Courier;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.ftp.Ftp;
import photoorganizer.ftp.FtpCommandReply;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.ThumbnailsOptionsTab;
import photoorganizer.renderer.WebPublishOptionsTab;

// TODO: switch to FtpUrlConnection
public class FTPCourier implements Courier {
	Controller controller;

	FtpCommandReply fcr;

	String mask;

	public FTPCourier(Controller controller) {
		this.controller = controller;
	}

	public void deliver(StringBuffer buf, String destPath, String contentType, String encoding) throws IOException {
		try {
			ftp_connection.cwd();
			fcr = ftp_connection.stor(home + destPath);
			if (fcr.serverSocket != null || fcr.socket == null)
				fcr.socket = fcr.serverSocket.accept();
			OutputStream os = fcr.socket.getOutputStream();
			// TODO: start separate thread and join it after timeout
			byte[] ba;
			if (encoding != null && encoding.length() > 0)
				ba = buf.toString().getBytes(encoding);
			else
				ba = buf.toString().getBytes();
			os.write(ba);
			os.close();
		} catch (InterruptedIOException e) {
			if (fcr.serverSocket != null)
				fcr.serverSocket.close();
			throw new IOException("Accept failed for FTP socket " + e);

		} catch (IOException e) {
			if (fcr != null && fcr.serverSocket != null)
				fcr.serverSocket.close();
			throw e;
		}
		if (fcr != null) {
			if (fcr.socket != null)
				fcr.socket.close();
			if (fcr.serverSocket != null)
				fcr.serverSocket.close();
			ftp_connection.getReply(null);
		}
	}

	public void deliver(String srcPath, String destPath) throws IOException {
		if (destPath == null)
			destPath = "";
		if (destPath.length() > 0 && destPath.charAt(destPath.length() - 1) != '/')
			destPath += '/';
		ftp_connection.type("I");
		ftp_connection.cwd();
		try {
			fcr = ftp_connection.stor(home + destPath + new File(srcPath).getName());
			if (fcr.serverSocket != null || fcr.socket == null)
				fcr.socket = fcr.serverSocket.accept();

		} catch (InterruptedIOException e) {
			if (fcr.serverSocket != null)
				fcr.serverSocket.close();
			throw new IOException("Accept failed for FTP socket " + e);
		} catch (IOException e) {
			if (fcr != null && fcr.serverSocket != null)
				fcr.serverSocket.close();
			throw e;
		}

		Stream.copyFile(new File(srcPath), fcr.socket.getOutputStream());

		if (fcr != null) {
			if (fcr.socket != null)
				fcr.socket.close();
			if (fcr.serverSocket != null)
				fcr.serverSocket.close();
			ftp_connection.getReply(null);
		}
	}

	public String deliver(MediaFormat format, String destPath, String cmask) throws IOException {
		MediaInfo ii = format.getMediaInfo();
		if (ii == null)
			return "";
		if (cmask == null)
			cmask = mask;
		if (destPath == null)
			destPath = "";
		if (destPath.length() > 0 && destPath.charAt(destPath.length() - 1) != '/')
			destPath += '/';
		String name = FileNameFormat.makeValidPathName(new FileNameFormat(cmask, true).format(format), format
				.getThumbnailType());
		destPath += name;
		ftp_connection.type("I");
		fcr = ftp_connection.stor(home + destPath);
		try {
			if (fcr.serverSocket != null || fcr.socket == null)
				fcr.socket = fcr.serverSocket.accept();
		} catch (InterruptedIOException ie) {
			if (fcr.serverSocket != null)
				fcr.serverSocket.close();
			throw new IOException(fcr.toString() + " " + ie);
		}
		OutputStream os = fcr.socket.getOutputStream();
		if (ii instanceof AbstractImageInfo)
			((AbstractImageInfo) ii).saveThumbnailImage(os);
		else
			os.write(format.getThumbnailData(null/*
													 * get desired dimension
													 * from props
													 */));
		os.flush();
		os.close();
		if (fcr.socket != null)
			fcr.socket.close();
		if (fcr.serverSocket != null)
			fcr.serverSocket.close();
		fcr = ftp_connection.getReply(null);
		if (fcr.replyCode / Ftp.CODE_SCALE != Ftp.POSITIVE_COMPLETION_REPLY)
			throw new IOException(fcr.toString());
		return destPath;
	}

	public void checkForDestPath(String path) throws IOException {
		ftp_connection.cwd();
		path = path.replace('\\', '/');
		StringTokenizer pst = new StringTokenizer(path, "/");
		while (pst.hasMoreTokens()) {
			String sd = pst.nextToken();
			ftp_connection.mkd(sd);
			ftp_connection.cwd(sd);
		}
	}

	public void init() throws IOException {
		IniPrefs s = controller.getPrefs();
		ftp_connection = new Ftp(WebPublishOptionsTab.getConnectionInfo(s));
		if (!ftp_connection.isConnected) {
			JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_FTP_CONNECT,
					Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			throw new IOException("Couldn't open FTP connection for HTML file");
		}
		try {
			ftp_connection.login();
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_FTP_LOGIN + '\n' + ioe,
					Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			throw ioe;
		}
		mask = (String) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FILEMASK);
		if (mask == null || mask.length() == 0)
			mask = PhotoCollectionPanel.DEFTNMASK;
		home = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.FTP_WEBROOT);
		if (home == null || home.length() == 0) {
			home = ftp_connection.getHomeDirectory();
		}
		if (home.length() == 0)
			home = "/";
		else if (home.charAt(home.length() - 1) != '/')
			home += '/';
	}

	public void done() {
		if (ftp_connection.isConnected)
			ftp_connection.quit();
	}

	public boolean isLocal() {
		return false;
	}

	public boolean isContentIncluded() {
		return false;
	}

	public String getRootPathProperty() {
		return WebPublishOptionsTab.FTP_WEBROOT;
	}

	public String getMediaPathProperty() {
		return WebPublishOptionsTab.FTP_IMAGEPATH;
	}

	public String getThumbnailsPathProperty() {
		return WebPublishOptionsTab.FTP_TNWEBPATH;
	}

	public String getMediaUrlProperty() {
		return WebPublishOptionsTab.FTP_IMAGEURL;
	}
	
	public String getTemplatePropertyName() {
		return WebPublishOptionsTab.HTMLTEMPL;
	}

	protected void finalize() throws Throwable {
		done();
		super.finalize();
	}

	String home;

	Ftp ftp_connection;
}
