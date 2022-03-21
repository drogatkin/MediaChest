/* MediaChest - HTTPCourier 
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
 * $Id: HTTPCourier.java,v 1.31 2012/08/05 06:27:48 dmitriy Exp $
 */
package photoorganizer.courier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;

import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;
import org.aldan3.util.TemplateEngine;

import photoorganizer.Controller;
import photoorganizer.Courier;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.ThumbnailsOptionsTab;
import photoorganizer.renderer.TwoPanesView;
import photoorganizer.renderer.WebPublishOptionsTab;

// TODO: remove calculation content length, HttpURLConnection does it
// TODO: provide class implementing java.net.Authenticator for authentication
public class HTTPCourier implements Courier {
	public final static String SECNAME = "HTTPCourier";

	public final static String CRLF = "\r\n";

	final static String CONTENT_TYPE = "Content-Type";

	final static String CONTENT_LENGTH = "Content-Length";

	final static String SET_COOKIE = "Set-Cookie";

	final static String USER_AGENT = "User-Agent";

	final static String CONTENT_DISP = "Content-Disposition: form-data; name=\"";

	final static String FILENAME = "\"; filename=\"";

	final static String CONTENT_ENCODING = "Content-Encoding: ";

	final static String CONTENT_TYPE_ = CONTENT_TYPE + ": ";

	final static String MULTIPART = "multipart/form-data; boundary=";

	final static String POST_ENCODING = "application/x-www-form-urlencoded";

	final static String DEFAULT_CONTENTTYPE = "image/jpeg"; // "application/octet-stream";

	final static String SEP = "--";

	final static String COOKIE = "Cookie";

	final static String SECURE = "secure";

	final static String DEFAULT_AGENT = "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)";

	public final static int AUTH_COOKIE = 1;

	public final static int AUTH_BASE = 2;

	public static final int AUTH_GOOGLE_AUTH = 3;

	public final static int METHOD_GET = 0;

	public final static int METHOD_POST = 1;

	final static int BUFSIZE = 1024 * 16;

	final private static boolean debugHeaders = false;

	Controller controller;

	IniPrefs s;

	boolean manualMode;

	TwoPanesView mmView;

	String albumName;

	public HTTPCourier(Controller controller) {
		this(controller, null);
	}

	public HTTPCourier(Controller controller, String _albumName) {
		this.controller = controller;
		this.albumName = _albumName;
		if (this.albumName != null && this.albumName.length() == 0)
			this.albumName = null;
		s = controller.getPrefs();
		manualMode = IniPrefs.getInt(s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.HTTP_MANUAL_MODE), 0) != 0;
		if (manualMode) {
			mmView = TwoPanesView.createFramed(false, null,
					Controller.BTN_MSK_OK + Controller.BTN_MSK_CANCEL, null);
			mmView.setSize(300, 500);
		}
	}

	public void deliver(StringBuffer buf, String destPath, String contentType, String encoding)
			throws IOException {
		if (authMode == AUTH_GOOGLE_AUTH)
			return;
		HttpURLConnection con = getConnectedToPublish();
		String boundary = genBoundary();
		con.setRequestProperty(CONTENT_TYPE, MULTIPART + boundary);
		// compute content length
		String newName = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_DEST_NAME);
		if (newName == null || newName.length() == 0)
			newName = WebPublishOptionsTab.UPL_DEST_NAME;
		String dataName = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_DATA_NAME);
		if (dataName == null || dataName.length() == 0)
			dataName = WebPublishOptionsTab.UPL_DATA_NAME;
		PrintWriter osw = null;
		try {
			osw = new PrintWriter(new OutputStreamWriter(con.getOutputStream(), encoding));
		} catch (Exception e) { // UnsupportedEncodingException,
			// NullPointerException
			osw = new PrintWriter(con.getOutputStream()); // use default
			// encoding
		}
		osw.print(SEP + boundary + CRLF);
		osw.print(CONTENT_DISP);
		osw.print(newName);
		osw.print("\"" + CRLF);
		osw.print(CRLF);
		osw.print(destPath + CRLF);
		osw.print(SEP + boundary + CRLF);
		osw.print(CONTENT_DISP);
		osw.print(dataName);
		osw.print(FILENAME);
		osw.print(destPath);
		osw.print("\"" + CRLF);
		if (contentType != null && contentType.length() > 0) {
			osw.print(CONTENT_TYPE_);
			osw.print(contentType + CRLF);
		}
		if (encoding != null && encoding.length() > 0) // do not use for a
			// while
			;
		osw.print(CRLF);
		osw.print(buf + CRLF);
		osw.print(SEP + boundary + SEP + CRLF);
		osw.close();
		saveCookies(con, cookieValues);
		reportConnectionStatus(con, "HTML", false, false);
		con.disconnect();
	}

	public void deliver(String srcPath, String destPath) throws IOException {
		HttpURLConnection con = getConnectedToPublish();
		// compute content length
		String newName = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_DEST_NAME);
		if (newName == null || newName.length() == 0)
			newName = WebPublishOptionsTab.UPL_DEST_NAME;
		String name = new File(srcPath).getName();
		if (destPath == null)
			destPath = "";
		if (destPath.length() > 0)
			if (destPath.charAt(destPath.length() - 1) != '/'
					&& destPath.charAt(destPath.length() - 1) != '\\')
				destPath += '/';
		destPath += name;
		String dataName = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_DATA_NAME);
		if (dataName == null || dataName.length() == 0)
			dataName = WebPublishOptionsTab.UPL_DATA_NAME;
		// TODO: read constant parameters from configuration and add them as
		// form parameters
		String contentType = null;
		try {
			contentType = URLConnection.getFileNameMap().getContentTypeFor(srcPath);
		} catch (Throwable t) {
			// JDK 1.1
		}
		if (contentType == null || contentType.length() == 0) {
			// TODO run media file analyzer to figure out first
			contentType = DEFAULT_CONTENTTYPE;
			System.err.println("Content type for " + srcPath + " not found, " + contentType
					+ " will be used.");
		}
		String boundary = genBoundary();

		File sf = new File(srcPath);
		OutputStream os;
		if (authMode == AUTH_GOOGLE_AUTH) {
			con.setRequestProperty(CONTENT_TYPE, contentType);
			con.setRequestProperty("Slug", srcPath);
			con.setRequestMethod("POST");
			con.setRequestProperty("content-length", "" + sf.length());
			con.setFixedLengthStreamingMode((int)sf.length());
		} else {
			con.setRequestProperty(CONTENT_TYPE, MULTIPART + boundary);
		}
		PrintWriter osw = new PrintWriter(os = con.getOutputStream());
		if (authMode != AUTH_GOOGLE_AUTH) {
			osw.print(SEP + boundary + CRLF);
			osw.print(CONTENT_DISP);
			osw.print(newName);
			osw.print("\"" + CRLF);
			osw.print(CRLF);
			osw.print(destPath + CRLF);
			osw.print(SEP + boundary + CRLF);
			osw.print(CONTENT_DISP);
			osw.print(dataName);
			osw.print(FILENAME);
			osw.print(srcPath);
			osw.print("\"" + CRLF);
			osw.print(CONTENT_TYPE_);
			osw.print(contentType + CRLF);
			osw.print(CRLF);
			if (osw.checkError())
				System.err.println("Error happened in output stream at writing form fields.");
		}
		Stream.copyFile(sf, os);
		if (osw.checkError())
			System.err.println("Error happened in output stream at writing form data.");
		if (authMode != AUTH_GOOGLE_AUTH) {
			osw.print(CRLF);
			osw.print(SEP + boundary + SEP + CRLF);
			if (osw.checkError())
				System.err.println("Error happened in output stream at finishing.");
		}
		osw.close();
		saveCookies(con, cookieValues);
		reportConnectionStatus(con, "IMAGE", false, false);
		con.disconnect();
	}

	public void checkForDestPath(String path) throws IOException {
	}

	public String deliver(MediaFormat format, String destPath, String cmask) throws IOException {
		if (authMode == AUTH_GOOGLE_AUTH) // temporary hack
			return "";
		HttpURLConnection con = getConnectedToPublish();
		String boundary = genBoundary();
		con.setRequestProperty(CONTENT_TYPE, MULTIPART + boundary);
		String newName = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_DEST_NAME);
		if (newName == null || newName.length() == 0)
			newName = WebPublishOptionsTab.UPL_DEST_NAME;
		MediaInfo ii = format.getMediaInfo();
		if (ii == null)
			return "";
		if (cmask == null)
			cmask = mask;
		String name = FileNameFormat.makeValidPathName(new FileNameFormat(cmask, true).format(format), format
				.getThumbnailType());
		if (destPath == null)
			destPath = "";
		if (destPath.length() > 0)
			if (destPath.charAt(destPath.length() - 1) != '/'
					&& destPath.charAt(destPath.length() - 1) != '\\')
				destPath += '/';
		destPath += name;
		String dataName = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_DATA_NAME);
		if (dataName == null || dataName.length() == 0)
			dataName = WebPublishOptionsTab.UPL_DATA_NAME;
		String contentType = "image/" + format.getThumbnailType();
		ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
		if (ii instanceof AbstractImageInfo)
			((AbstractImageInfo) ii).saveThumbnailImage(os);
		else
			os.write(format.getThumbnailData(null/*
													 * get desired dimension
													 * from props
													 */));
		OutputStream os2;
		PrintWriter osw = new PrintWriter(os2 = con.getOutputStream());
		osw.print(SEP + boundary + CRLF);
		osw.print(CONTENT_DISP);
		osw.print(newName);
		osw.print("\"" + CRLF);
		osw.print(CRLF);
		osw.print(destPath + CRLF);
		osw.print(SEP + boundary + CRLF);
		osw.print(CONTENT_DISP);
		osw.print(dataName);
		osw.print(FILENAME);
		osw.print(name);
		osw.print("\"" + CRLF);
		osw.print(CONTENT_TYPE_);
		osw.print(contentType + CRLF);
		osw.print(CRLF);
		if (osw.checkError())
			System.err.println("Error happened in output stream at writing form fields.");
		os2.write(os.toByteArray());
		if (osw.checkError())
			System.err.println("Error happened in output stream at writing thumbnail.");
		osw.print(CRLF);
		osw.print(SEP + boundary + SEP + CRLF);
		os.flush();
		os.close();
		osw.flush();
		osw.close();
		saveCookies(con, cookieValues);
		reportConnectionStatus(con, "THUMBNAIL", false, false);
		con.disconnect();
		return destPath;
	}

	public void init() throws IOException {
		// do login if authentication requested
		authMode = IniPrefs.getInt(s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.HTTP_AUTH_SHC), 0);
		boolean isGet = IniPrefs.getInt(s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.HTTPLOGINMETHOD), METHOD_GET) == METHOD_GET;
		if (authMode == AUTH_COOKIE || authMode == AUTH_GOOGLE_AUTH) {
			cookieValues = new Hashtable(2);
			HttpURLConnection con = null;
			HttpURLConnection.setFollowRedirects(false);
			String query = URLEncoder.encode((String) s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.HTTPLOGIN_NAME));
			query += '=';
			query += URLEncoder.encode((String) s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.HTTPLOGIN));
			query += '&';
			query += URLEncoder.encode((String) s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.HTTPPASSWORD_NAME));
			query += '=';
			String sp = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.HTTPPASSWORD);
			if (sp != null) {
				try {
					query += URLEncoder.encode(DataConv.encryptXor(new String(DataConv.hexToBytes(sp),
							Controller.ISO_8859_1)));
				} catch (UnsupportedEncodingException uee) {
					throw new IOException("Unsupported encoding in password decryption: " + uee);
				}
			}
			query += '&';
			// no URL encode for additional part, TODO: make a tokenizer and do
			// encode
			query += (String) s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.HTTPSTATICQUERY);
			if (isGet) {
				try {
					URL tu = new URL((String) s.getProperty(WebPublishOptionsTab.SECNAME,
							WebPublishOptionsTab.HTTPLOGINURL)
							+ '?' + query);
					con = (HttpURLConnection) tu.openConnection();
				} catch (MalformedURLException mue) {
					throw new IOException("MalformedURLException at GET method: " + mue);
				}
				// set cookie if requested
				restoreCookies(con, cookieValues);
				con.setRequestProperty(USER_AGENT, DEFAULT_AGENT);
			} else { // POST
				try {
					con = (HttpURLConnection) new URL((String) s.getProperty(WebPublishOptionsTab.SECNAME,
							WebPublishOptionsTab.HTTPLOGINURL)).openConnection();
					con.setDoOutput(true);
					con.setUseCaches(false);
					con.setAllowUserInteraction(false);
					con.setRequestProperty(USER_AGENT, DEFAULT_AGENT);
					// set cookie if requested
					restoreCookies(con, cookieValues);
					// note: not necessary to set method, content type, and
					// length
					// HttpURLConnection does it automatically (
					// send parameters
					PrintWriter out = new PrintWriter(con.getOutputStream());
					out.print(query/* +CRLF */);
					if (out.checkError())
						System.err.println("Error happened in output stream at POST query.");
					out.close();
				} catch (MalformedURLException mue) {
					throw new IOException("MalformedURLException at POST method: " + mue);
				}
			}
			if (con != null) {
				int respCode = con.getResponseCode();
				// save cookie
				saveCookies(con, cookieValues);
				if (respCode == HttpURLConnection.HTTP_OK) {
					byte[] response = reportConnectionStatus(con, "LOGIN", false, true);
					if (authMode == AUTH_GOOGLE_AUTH) {
						Properties props = new Properties();
						props.load(new ByteArrayInputStream(response));
						auth = props.getProperty("Auth");
						// System.err.printf("Auth:%s%n", auth);
					}
				} else if (respCode >= 300 && respCode <= 305) {
					// Codes HTTP_MULT_CHOICE , HTTP_MOVED_PERM ,
					// HTTP_MOVED_TEMP, HTTP_SEE_OTHER, HTTP_NOT_MODIFIED,
					// HTTP_USE_PROXY
					String redirectPath = con.getHeaderField(Resources.HDR_LOCATION);
					reportConnectionStatus(con, "LOGIN", false, false);
					URL origUrl = con.getURL();
					con.disconnect();
					try {
						con = (HttpURLConnection) new URL(origUrl, redirectPath).openConnection();
						restoreCookies(con, cookieValues);
						con.setRequestProperty(USER_AGENT, DEFAULT_AGENT);
						saveCookies(con, cookieValues);
						reportConnectionStatus(con, "LOGIN-REDIRECT", false, false);
					} catch (MalformedURLException mue) {
						throw new IOException("MalformedURLException at redirect: " + mue);
					}
				} else
					reportConnectionStatus(con, "LOGIN", false, false);
				con.disconnect();
			}
		} // else auth not cookie
		String uploadUrl = DataConv.arrayToString(s.getProperty(WebPublishOptionsTab.SECNAME,
				WebPublishOptionsTab.UPL_SERVLET_URL), ',');
		if (uploadUrl != null) {
			if (albumName != null && albumName.length() > 0) {
				if (authMode == AUTH_GOOGLE_AUTH) {
					String createUrl = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
							WebPublishOptionsTab.HTTPALBUMNAME);
					if (createUrl != null && createUrl.length() > 0) {
						String userId = (String) s.getProperty(WebPublishOptionsTab.SECNAME,
								WebPublishOptionsTab.HTTPLOGIN);
						assert userId != null;
						int atp = userId.indexOf('@');
						if (atp > 0)
							userId = userId.substring(0, atp);
						String albumId = createAlbum(albumName, String.format(createUrl, userId));
						if (albumId != null) {
							uploadUrl = String.format(uploadUrl, userId, albumId);
						} else
							uploadUrl = String.format(uploadUrl, "default", "default");
					} else
						uploadUrl = String.format(uploadUrl, "default", "default");
				} else {
					if (uploadUrl.indexOf('?') < 0)
						uploadUrl += "?";
					else if (uploadUrl.charAt(uploadUrl.length() - 1) != '&')
						uploadUrl += "&";
					String name = s.getProperty(WebPublishOptionsTab.SECNAME,
							WebPublishOptionsTab.HTTPALBUMNAME).toString();
					if (name == null || name.length() == 0)
						name = s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.HTTPALBUMID)
								.toString();
					if (name != null && name.length() > 0) {
						uploadUrl += name;
						uploadUrl += '=';
						uploadUrl += albumName;
					}
				}
			} else if (authMode == AUTH_GOOGLE_AUTH) {
				uploadUrl = String.format(uploadUrl, "default", "default");
			}
		}
		try {
			publisherURL = new URL(uploadUrl);
		} catch (MalformedURLException mue) {
			throw new IOException("Invalid upload URL format " + uploadUrl);
		} catch (NullPointerException npe) {
			throw new IOException("Upload URL not specified " + uploadUrl);
		}

		mask = (String) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FILEMASK);
		if (mask == null || mask.length() == 0)
			mask = PhotoCollectionPanel.DEFTNMASK;
	}
	
	public Map[] getAlbumList() {
		return null;
	}

	public void done() {

	}

	public boolean isLocal() {
		return false;
	}

	public boolean isContentIncluded() {
		return false;
	}

	public String getRootPathProperty() {
		return WebPublishOptionsTab.HTTP_WEBROOT;
	}

	public String getMediaPathProperty() {
		return WebPublishOptionsTab.HTTP_IMAGEPATH;
	}

	public String getThumbnailsPathProperty() {
		return WebPublishOptionsTab.HTTP_TNWEBPATH;
	}

	public String getMediaUrlProperty() {
		return WebPublishOptionsTab.HTTP_IMAGEURL;
	}
	
	public String getTemplatePropertyName() {
		return WebPublishOptionsTab.HTMLTEMPL;
	}

	private String createAlbum(String title, String urlStr) {
		if (title == null || title.length() == 0)
			return null;

		InputStream is = null;
		char[] createAtom = null;

		try {
			is = getClass().getClassLoader().getResourceAsStream("resource/template/picasa_create_album.xml");
			if (is != null)
				createAtom = new String(Controller.readStreamInBuffer(is)).toCharArray();
		} catch (IOException e) {
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		if (createAtom == null)
			return null;
		// Now send the POST request to the appropriate Picasa Web Albums URL:
		// http://picasaweb.google.com/data/feed/api/user/userID
		// Picasa Web Albums creates a new album using the data you sent,
		// then returns an HTTP 201 CREATED status code, along with a copy
		// of the new album in the form of an <entry> element. The returned
		// entry is similar to the one you sent, but the returned one contains
		// various elements added by Picasa Web Albums, such as an <id> element.
		Properties newAlbum = new Properties();
		// consider title as set of title;description;key words
		String[] parts = title.split(";");
		newAlbum.put("title", parts[0]);
		if (parts.length > 1)
			newAlbum.put("description", parts[1]);
		if (parts.length > 2)
			newAlbum.put("location", parts[2]);
		if (parts.length > 3)
			newAlbum.put("keywords", parts[3]);
		newAlbum.put("time", "" + System.currentTimeMillis());
		OutputStreamWriter w = null;
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestMethod("POST");
			con.setRequestProperty(CONTENT_TYPE, "application/atom+xml; charset=UTF-8");
			con.setChunkedStreamingMode(112);
			con.setAllowUserInteraction(false); //
			con.setRequestProperty(USER_AGENT, DEFAULT_AGENT);
			con.setRequestProperty("Authorization", "GoogleLogin auth=" + auth);
			assert auth != null;
			new TemplateEngine().process(w = new OutputStreamWriter(con.getOutputStream(), "UTF-8"), createAtom, 0,
					createAtom.length, newAlbum, null, newAlbum, null, null);
			w.flush();
			byte[] response = reportConnectionStatus(con, "CREATE ALBUM", false, true);
			if (response != null
					&& con.getContentType().equalsIgnoreCase("application/atom+xml; charset=UTF-8")) {
				// not robust as XML parsing but fast
				// check response type on
				String albumAtom = new String(response, "UTF-8"); // get it response type
				int ip = albumAtom.indexOf("<gphoto:id>");
				if (ip > 0) {
					int eb = albumAtom.indexOf('<', ip + "<gphoto:id>".length());
					if (eb > 0)
						return albumAtom.substring(ip + "<gphoto:id>".length(), eb);
				}
				// new ByteArrayInputStream();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (w != null)
				try {
					w.close();
				} catch (IOException e) {
				}
		}
		return null;
	}

	private String genBoundary() {
		return Long.toHexString(new Random().nextLong());
	}

	private byte[] reportConnectionStatus(HttpURLConnection _con, String _msg, boolean _cookies,
			boolean _content) throws IOException {
		byte[] result = null;
		System.err.println("************* " + _msg + " *************");
		System.err.println("Request: " + _con.getURL());
		int code = _con.getResponseCode();
		System.err.println("Server returned: " + code + "/" + _con.getResponseMessage());
		for (int i = 0;; i++) {
			String hdr = _con.getHeaderField(i);
			if (hdr == null)
				break;
			System.err.printf("Response HDR: %s = %s%n", _con.getHeaderFieldKey(i), hdr);
		}
		if (code >= 300 && code <= 305)
			System.err.println("Redirect requested to: " + _con.getHeaderField(Resources.HDR_LOCATION));
		if (_cookies)
			System.err.println("Cookies: " + cookieValues);
		if (_content) {
			if (code >= 500) {
				// TODO check for null erro stream
				System.err.println("Error stream: \n"
						+ new String(Controller.readStreamInBuffer(_con.getErrorStream())));
			} else {
				result = Controller.readStreamInBuffer(_con.getInputStream());
				System.err.println("Returned content: \n" + new String(result));
			}
		}

		if (manualMode) {
			InputStream in;
			mmView.readToUpper(in = _con.getInputStream());
			in.close();
			if (Resources.CMD_OK.equals(mmView.showModal()))
				manualMode = false;
		}
		return result;
	}

	private HttpURLConnection getConnectedToPublish() throws IOException {
		HttpURLConnection result = (HttpURLConnection) publisherURL.openConnection();
		result.setDoOutput(true);
		result.setDoInput(true);
		result.setUseCaches(false);
		result.setAllowUserInteraction(false); //
		result.setRequestProperty(USER_AGENT, DEFAULT_AGENT);
		if (authMode == AUTH_COOKIE)
			restoreCookies(result, cookieValues);
		else if (authMode == AUTH_GOOGLE_AUTH) {
			if (auth != null)
				result.setRequestProperty("Authorization", "GoogleLogin auth=" + auth);
		} else if (authMode == AUTH_BASE) {

		}
		return result;
	}

	static public void saveCookies(URLConnection _con, Map _cookieValues) {
		for (int i = 0; _con.getHeaderField(i) != null || i == 0; i++) {
			if (debugHeaders)
				System.err.println("Header " + i + "-->" + _con.getHeaderFieldKey(i) + ':'
						+ _con.getHeaderField(i));
			if (SET_COOKIE.equalsIgnoreCase(_con.getHeaderFieldKey(i))) {
				String cookie = _con.getHeaderField(i);
				int pos = cookie.indexOf(';');
				if (cookie.lastIndexOf(SECURE) > pos) // skip the cookie
					// because not secure
					// conn
					continue;
				// TODO: check for expiration and remove
				if (pos > 0) {
					cookie = cookie.substring(0, pos);
					pos = cookie.indexOf('=');
					if (pos > 0)
						_cookieValues.put(cookie.substring(0, pos), cookie);
				}
			}
		}
	}

	static public void restoreCookies(URLConnection _con, Map _cookieValues) {
		String cookies = null;
		Iterator i = _cookieValues.entrySet().iterator();
		while (i.hasNext())
			if (cookies == null)
				cookies = (String) ((Map.Entry) i.next()).getValue();
			else {
				cookies += "; ";
				cookies += (String) ((Map.Entry) i.next()).getValue();
			}
		if (cookies != null)
			_con.setRequestProperty(COOKIE, cookies);
	}

	URL publisherURL;

	String mask;

	int authMode;

	Map cookieValues;

	String auth;
}

class myWriter extends OutputStreamWriter {
	myWriter(OutputStream _os, String cs) throws UnsupportedEncodingException {
		super(_os, cs);
	}

	@Override
	public void write(char[] arg0, int arg1, int arg2) throws IOException {
		System.err.print(new String(arg0, arg1, arg2));
		super.write(arg0, arg1, arg2);
	}

	@Override
	public void write(int c) throws IOException {
		System.err.print((char) c);
		super.write(c);
	}

	@Override
	public void write(String s) throws IOException {
		System.err.print(s);
		super.write(s);
	}
}
