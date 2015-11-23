/* MediaChest $RCSfile: HtmlProducer.java,v $
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
 * $Id: HtmlProducer.java,v 1.25 2012/08/05 06:27:48 dmitriy Exp $
 */
package photoorganizer;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.jpeg.JPEG;

import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;
import org.aldan3.util.inet.HttpUtils;

import photoorganizer.album.Access;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.ftp.Ftp;
import photoorganizer.renderer.AlbumPane;
import photoorganizer.renderer.MiscellaneousOptionsTab;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.StatusBar;
import photoorganizer.renderer.ThumbnailsOptionsTab;
import photoorganizer.renderer.WebPublishOptionsTab;

// TODO: add HTML encoding for all replaced lexems

/**
 * The class parses HTML template and it's looking for special tags
 * <ul>
 *  <li>!%loop - start of a loop
 *  <li>!%tnof - substitute original file name
 *  <li>!%tnfn - substitute thumbnail rel path
 *  <li>!%tnsn - substitute resized file name
 *  <li>!%tntt - tooltip for thumbnail
 *  <li>!%tnc - commentary at bottom edge
 *  <li>!%tnw - width of thumbnail image (optional)
 *  <li>!%tnh - height of thumbnail image (optional)
 *  <li>!%tn+ - advance to next thumbnail
 *  <li>!%endl - end of the loop
 * 
 *  <li>!%tnif - original image file name
 *  <li>!%tnsf - resized image file name
 *  <li>!%cprt - copyright note with link
 *  <li>!%alnm - album name
 *  <li>!%webp - web path on Web site to original images
 * 
 *  <li>!%cloop - loop of child albums
 *  <li>!%praln - parent album name
 *  <li>!%pralr - parent album reference in URL
 *  <li>!%ch+ - next child
 *  <li>!%cendl - end loop of child albums refs marker
 *  <li>!%chan - child album name
 *  <li>!%chap - child album URL
 *  <li>!%cendl - end marker of child loop
 *  <li>!%chan - child album name
 *  <li>!%chap - child album web path
 * 
 *  <li>!%txt - e-mail text, or description of the album
 *  <li>!%enc - page encoding
 * <ul>
 * Notice, all tags can be enclosed to HTML comment
 */
public class HtmlProducer {

	private final int HTML = 1;

	private final int END = 32;

	private final int RECORD_LOOP = 33;

	// lexem codes
	final static int PLAIN_HTML = 0;

	final static int IMAGE_LOOP_START_MARKER = 1;

	final static int IMAGE_FILE_NAME = 2;

	final static int THUMB_REL_PATH = 3;

	final static int THUMB_TOOLTIP = 4;

	final static int MIDRES_IMAGE_FILE_NAME = 5;

	final static int LOWRES_IMAGE_FILE_NAME = 6;

	final static int COMMENTARY = 7;

	final static int WIDTH_OF_THUMB = 8;

	final static int HEIGHT_OF_THUMB = 9;

	final static int TO_NEXT_IMAGE = 10;

	final static int IMAGE_LOOP_END_MARKER = 11;

	final static int IMAGE_WEB_DIR = 12;

	final static int IMAGE_FILE_PATH = 13;

	final static int COPYRIGHT = 14;

	final static int ALBUM_NAME = 15;

	final static int PARENT_ALBUM_NAME = 16;

	final static int PARENT_ALBUM_WEB_PATH = 17;

	final static int ALBUM_LOOP_START_MARKER = 18;

	final static int ALBUM_LOOP_END_MARKER = 19;

	final static int CHILD_ALBUM_NAME = 20;

	final static int CHILD_ALBUM_WEB_PATH = 21;

	final static int TO_NEXT_CHILD_ALBUM = 22;

	final static int MESSAGE = 23;

	final static int ENCODING = 24;

	final static int SIZED_IMAGE_FILE_PATH = 25;

	final static int SIZED_IMAGE_FILE_NAME = 26;

	// final static int
	final static int END_DOCUMENT = -1;

	static final String[] LEXEM_MENMONICS = { "html", // PLAIN_HTML
			"loop", // IMAGE_LOOP_START_MARKER
			"tnif", // IMAGE_FILE_NAME
			"tnfn", // THUMB_REL_PATH
			"tntt", // THUMB_TOOLTIP
			"tnhr", // MIDRES_IMAGE_FILE_NAME
			"tnlr", // LOWRES_IMAGE_FILE_NAME
			"tnc", // COMMENTARY
			"tnw", // WIDTH_OF_THUMB
			"tnh", // HEIGHT_OF_THUMB
			"tn+", // TO_NEXT_IMAGE
			"endl", // IMAGE_LOOP_END_MARKER
			"webp", // IMAGE_WEB_DIR
			"tnof", // IMAGE_FILE_PATH
			"cprt", // COPYRIGHT
			"alnm", // ALBUM_NAME
			"praln",// PARENT_ALBUM_NAME
			"pralr",// PARENT_ALBUM_WEB_PATH
			"cloop",// ALBUM_LOOP_START_MARKER
			"cendl",// ALBUM_LOOP_END_MARKER
			"chan", // CHILD_ALBUM_NAME
			"chap", // CHILD_ALBUM_WEB_PATH
			"ch+", // TO_NEXT_CHILD_ALBUM
			"txt", // MESSAGE
			"enc", // ENCODING
			"tnsn", // SIZED_IMAGE_FILE_PATH
			"tnsf" // SIZED_IMAGE_FILE_NAME
	};

	/*
	 * final static char CPERCENT = '%'; 
	 * final static char CEXLAM = '!';
	 * final static char[] LEX_MARKER = {CEXLAM, CPERCENT};
	 */

	// simplify parsing code, adding lexer returning pair lexem:code
	public HtmlProducer(Controller controller) {
		this.controller = controller;
		s = controller.getPrefs();
		album_pane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
		try {
			album_name = album_pane.getSelectionPath().toString();
		} catch (NullPointerException ne) {
			album_name = "";
		}
		encoding = MiscellaneousOptionsTab.getEncoding(controller);
		html = new StringBuffer(512);
		statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
	}

	public void produce(Courier courier, int albumId, boolean recursevly) throws IOException {
		produce(courier, albumId, -1, recursevly, null);
	}

	public void produce(Courier courier, int albumId, boolean recursevly, String message) throws IOException {
		produce(courier, albumId, -1, recursevly, message);
	}

	public void produce(Courier courier, int albumId, int parentAlbumId, boolean recursevly, String message)
			throws IOException {
		if (courier == null)
			return;
		courier.init();
		Access access = album_pane.getAccess();
		String albumName = access.getNameOfAlbum(albumId);
		try {
			statusbar.displayInfo(Resources.INFO_WEBPUBLISHING);
			// TODO find out when e-mail template has to be used
			Lexer lexer = new Lexer((String) s
					.getProperty(WebPublishOptionsTab.SECNAME, courier.getTemplatePropertyName()));
			Vector lexems = new Vector();
			int state = HTML;
			Lex l;
			do {
				l = lexer.getNextLex();
				switch (l.code) {
				case IMAGE_LOOP_START_MARKER:
					if (state == HTML) {
						lexems.removeAllElements();
						state = RECORD_LOOP;
					} else
						// loop inside loop is not allowed
						state = END;
					break;
				case IMAGE_LOOP_END_MARKER:
					// do the loop
					processImageLoop(courier, access.getAlbumContents(albumId), lexems, albumId);
					state = HTML;
					break;
				case COPYRIGHT:
					if (state == HTML)
						html.append(PhotoOrganizer.COPYRIGHT);
					break;
				case ALBUM_NAME:
					if (state == HTML)
						html.append(albumName); // &copy;
					break;
				case PARENT_ALBUM_NAME:
					if (parentAlbumId >= 0)
						html.append(access.getNameOfAlbum(parentAlbumId));
					break;
				case PARENT_ALBUM_WEB_PATH:
					if (parentAlbumId >= 0)
						html.append(HttpUtils.urlHexEncode(access.getNameOfAlbum(parentAlbumId) + Resources.EXT_HTML));
					break;
				case ALBUM_LOOP_START_MARKER:
					if (state == HTML) {
						lexems.removeAllElements();
						state = RECORD_LOOP;
					} else
						// loop inside loop is not allowed
						state = END;
					break;
				case ALBUM_LOOP_END_MARKER:
					processAlbumLoop(courier, albumId, lexems);
					state = HTML;
					break;
				case END_DOCUMENT:
					state = END;
					html.append(l.text);
					break;
				case MESSAGE:
					if (message != null)
						html.append(message);
					break;
				case ENCODING:
					if (encoding != null)
						html.append(encoding);
					break;
				default:
					if (state == HTML)
						html.append(l.text);
					else if (state == RECORD_LOOP)
						lexems.addElement(l);
				}
			} while (state != END);
			courier.deliver(html, albumName + Resources.EXT_HTML, "text/html", encoding);
			html = new StringBuffer(512); // html.setLength(0); can't be used
											// cause a bug in Sun's libs
			if (images_to_copy != null) {
				String imagePath = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier.getMediaPathProperty());
				if (imagePath == null)
					imagePath = "";
				else
					courier.checkForDestPath(imagePath);
				statusbar.clearProgress();
				statusbar.displayInfo(Resources.INFO_COPYING);
				statusbar.setProgress(images_to_copy.size());
				for (int i = 0; i < images_to_copy.size(); i++) {
					courier.deliver((String) images_to_copy.elementAt(i), imagePath);
					statusbar.tickProgress();
				}
			}
		} finally {
			statusbar.clearInfo();
			statusbar.clearProgress();
			courier.done();
		}
		if (recursevly) {
			int[] childs = album_pane.getAccess().getAlbumsId(albumId);
			for (int i = 0; i < childs.length; i++)
				produce(courier, childs[i], albumId, true, message);
		}
	}

	void processImageLoop(Courier courier, Object[] medias, Vector lexems, int albumId) throws IOException {
		String thumbsPath = null;
		String destPath = null;
		String imagePath = null;
		if (courier.isContentIncluded() == false) {
			thumbsPath = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier.getThumbnailsPathProperty());
			if (thumbsPath == null)
				thumbsPath = "";
			else
				courier.checkForDestPath(thumbsPath);
			destPath = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier.getRootPathProperty());
			if (destPath == null)
				destPath = "";
			imagePath = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier.getMediaPathProperty());
			if (imagePath == null)
				imagePath = "";
			else {
				courier.checkForDestPath(imagePath);
				if (imagePath.length() > 0 && imagePath.charAt(imagePath.length() - 1) != '/'
						&& imagePath.charAt(imagePath.length() - 1) != '\\')
					imagePath += '/'; // File.separatorChar;
			}
		}

		boolean useUrl = s.getInt(s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.USEURLPATH), 0) == 1;
		String imageURL = null;
		if (useUrl) {
			if (courier.getMediaUrlProperty() != null)
				imageURL = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier.getMediaUrlProperty());
			if (imageURL == null)
				imageURL = "";
		}
		if (s.getInt(s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.CPYPICS), 0) == 1)
			images_to_copy = new Vector(10);
		statusbar.setProgress(medias.length);
		int resizeIndex = s.getInt(s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.RESIZEIMAGE), 0);
		if (resizeIndex < 0 || resizeIndex > Resources.LIST_SIZES.length - 1)
			resizeIndex = 0;
		Dimension resized = resizeIndex == 0 ? null : Resources.LIST_SIZES[resizeIndex];
		int ii = 0; // image index
		// NOTE: current implementation create resized images for all images in
		// temp directory
		// when resized option is requested, then local temp name provided
		// 
		imageloop: while (ii < medias.length) {
			MediaFormat format = medias[ii] instanceof MediaFormat ? (MediaFormat) medias[ii] : MediaFormatFactory
					.createMediaFormat((File) medias[ii]);
			if (format == null) {
				System.err.printf("Can't process %s, skipped%n", medias[ii]);
				ii++;
				continue;
			}
			File resizedFile = null;
			for (int i = 0; i < lexems.size(); i++) {
				Lex l = (Lex) lexems.elementAt(i);
				switch (l.code) {
				case PLAIN_HTML:
					html.append(l.text);
					break;
				case IMAGE_FILE_NAME:
					html.append(format.getName());
					break;
				case THUMB_REL_PATH:
					String m = (String) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.FILEMASK);
					if (m == null || m.length() == 0)
						m = PhotoCollectionPanel.DEFTNMASK;
					html.append(courier.deliver(format, thumbsPath, m));
					break;
				case THUMB_TOOLTIP:
					String ttm = DataConv.arrayToString(s.getProperty(ThumbnailsOptionsTab.SECNAME,
							ThumbnailsOptionsTab.TOOLTIPMASK), ',');
					if (ttm != null)
						html.append(new FileNameFormat(ttm).format(format));
					break;
				case COMMENTARY:
					String cmt = album_pane.getAccess().getPictureComment(albumId, format.getFile().getPath()); // format.getUrl()
					// remove leading <html> probably
					if (cmt == null || cmt.length() == 0) // check if in the
															// image
						try {
							cmt = format.getMediaInfo().getAttribute(MediaInfo.COMMENTS).toString();
						} catch (Exception ce) {
						}
					if (cmt == null || cmt.length() == 0) {
						cmt = (String) s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.LABELMASK);
						if (cmt != null)
							html.append(new FileNameFormat(cmt).format(format));
					} else
						html.append(cmt);
					break;
				case WIDTH_OF_THUMB:
					try {
						html.append(format.getThumbnail(null).getIconWidth());
					} catch (NullPointerException npe) {
					}
					break;
				case HEIGHT_OF_THUMB:
					try {
						html.append(format.getThumbnail(null).getIconHeight());
					} catch (NullPointerException npe) {
					}
					break;
				case TO_NEXT_IMAGE:
					ii++;
					if (ii >= medias.length) {
						break imageloop;
					} else {
						format = medias[ii] instanceof MediaFormat ? (MediaFormat) medias[ii] : MediaFormatFactory
								.createMediaFormat((File) medias[ii]);
						resizedFile = null;
					}
					statusbar.tickProgress();
					break;
				case IMAGE_WEB_DIR:
					if (useUrl)
						html.append(imageURL);
					break;
				case IMAGE_FILE_PATH:
					String ifn;
					try {
						ifn = format.getName();
					} catch (NullPointerException npe) {
						break; // ifn = new
								// File(format.getLocationName()).getName();
					}
					if (useUrl) // it means
						html.append(HttpUtils.urlHexEncode(ifn));
					else {
						// what should be in the place of URL of the image?
						//
						// | local | ftp(r)| http | e-mail | svg |
						// copy image | PATH | RPATH | RPATH | IMGID | RPATH |
						// use URL | URL+N | URL+N | URL+N | URL+N | URL+N |
						// copy + URL | URL+N | URL+N | URL+N | URL+N | URL+N |
						// none | PATH | RPATH | RPATH | IMGID | RPATH |
						processPath(courier, destPath, imagePath, ifn, format.getFile(), images_to_copy);
					}
					break;
				case SIZED_IMAGE_FILE_NAME:
				case MIDRES_IMAGE_FILE_NAME:
				case LOWRES_IMAGE_FILE_NAME:
				case SIZED_IMAGE_FILE_PATH:
					// TODO make only name for name requesters
					if (resized != null) {						
						if (resizedFile == null)
							resizedFile = resizeImage(format, resized);
						if (resizedFile != null)
							if (useUrl) // it means
								html.append(HttpUtils.urlHexEncode(resizedFile.getName()));
							else
								processPath(courier, destPath, imagePath, resizedFile.getName(), resizedFile,
										images_to_copy);
					}
					break;
				}
			}
			ii++; // advance to next image automatically at the and of the
					// loop
		}
	}

	protected File resizeImage(MediaFormat format, Dimension resized) {
		// Optimization can be using resized image generated in memory only for
		// cases when resized images
		// requested in template
		// Another optimization to reuse destination, unless images in seq can
		// change sizes
		File result = null;
		if (format instanceof BasicJpeg == false) { // do not know how to resize
			return format.getFile();
		}
		OutputStream os = null;
		try {
			result = File.createTempFile( resized.toString(), ((MediaFormat) format).getName());
			result.deleteOnExit();
			JPEG.saveSizedImage(os = new FileOutputStream(result), ((BasicJpeg) format).getBufferedImage(), resized);
		} catch (IOException e) {
			System.err.println("Error in creation scaled image file:" + e);
			e.printStackTrace();
		} finally {			
			try {
				os.close();
			} catch (Exception e) {
			}
		}
		return result;
	}

	protected void processPath(Courier courier, /* StringBuffer html, */String destPath, String imagePath, String name,
			File imageFile, List copyList) {
		if (copyList != null) {
			if (courier.isLocal()) {
				try {
					// TODO: use getUrl from Java2
					// note: toURL() doesn't work on Unix (Sun's bug)
					html.append(new URL("file", "/", new File(new File(destPath, imagePath), name).getAbsolutePath())
							.toString());
				} catch (MalformedURLException mfpe) {
					html.append(imageFile.getPath());
				}
			} else {
				if (courier.isContentIncluded())
					html.append("cid:").append(imageFile.getPath()); // not
																		// java.net.URLEncoder.encode()
				else
					html.append(imagePath).append(name);
			}
			copyList.add(imageFile.getPath());
		} else {
			URL url = null;
			try {
				url = imageFile.toURL();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block

			}
			if (url != null)
				html.append(url);
			else
				html.append(imageFile.getPath());
		}
	}

	void processAlbumLoop(Courier courier, int albumId, Vector lexems) throws IOException {
		Access access = album_pane.getAccess();
		int[] childs = album_pane.getAccess().getAlbumsId(albumId);
		int ai = 0;
		albumloop: while (ai < childs.length) {
			for (int i = 0; i < lexems.size(); i++) {
				Lex l = (Lex) lexems.elementAt(i);
				switch (l.code) {
				case PLAIN_HTML:
					html.append(l.text);
					break;
				case CHILD_ALBUM_NAME:
					html.append(access.getNameOfAlbum(childs[ai]));
					break;
				case CHILD_ALBUM_WEB_PATH:
					html.append(HttpUtils.urlHexEncode(access.getNameOfAlbum(childs[ai]) + Resources.EXT_HTML));
					break;
				case TO_NEXT_CHILD_ALBUM:
					ai++;
					if (ai >= childs.length) {
						break albumloop;
					} else {

					}
					break;
				}
			}
			ai++; // advance to next child album automatically at the and of
					// the loop
		}
	}

	public void produce(Courier courier, File[] files, String message) throws IOException {
		produce(Resources.LABEL_NO_ALBUMNAME, files, courier, message);
	}

	public void produce(String desthtmlname, File[] files, Courier courier, String message) throws IOException {
		if (courier == null || desthtmlname == null)
			return;
		courier.init();
		String albumName = Resources.LABEL_CURRENT_SELECTION;
		try {
			statusbar.displayInfo(Resources.INFO_WEBPUBLISHING);
			Lexer lexer = new Lexer((String) s
					.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.HTMLTEMPL));
			Vector lexems = new Vector();
			int state = HTML;
			Lex l;
			do {
				l = lexer.getNextLex();
				switch (l.code) {
				case IMAGE_LOOP_START_MARKER:
					if (state == HTML) {
						lexems.removeAllElements();
						state = RECORD_LOOP;
					} else
						// loop inside loop is not allowed
						state = HTML;
					break;
				case IMAGE_LOOP_END_MARKER:
					// do the loop
					processImageLoop(courier, files, lexems, -1);
					state = HTML;
					break;
				case COPYRIGHT:
					if (state == HTML)
						html.append(PhotoOrganizer.COPYRIGHT);
					break;
				case ALBUM_NAME:
					if (state == HTML)
						html.append(albumName);
					break;
				case PARENT_ALBUM_NAME:
					break;
				case PARENT_ALBUM_WEB_PATH:
					break;
				case ALBUM_LOOP_START_MARKER:
					if (state == HTML) {
						lexems.removeAllElements();
						state = RECORD_LOOP;
					} else
						// loop inside loop is not allowed
						state = END;
					break;
				case ALBUM_LOOP_END_MARKER:
					// no album processing for this mode
					state = HTML;
					break;
				case END_DOCUMENT:
					state = END;
					html.append(l.text);
					break;
				case MESSAGE:
					if (message != null)
						html.append(message);
					break;
				case ENCODING:
					if (encoding != null)
						html.append(encoding);
					break;
				default:
					if (state == HTML)
						html.append(l.text);
					else if (state == RECORD_LOOP)
						lexems.addElement(l);
				}
			} while (state != END);
			courier.deliver(html, desthtmlname, "text/html", encoding);
			if (images_to_copy != null) {
				String imagePath = null;
				if (courier.isContentIncluded() == false) {
					imagePath = (String) s.getProperty(WebPublishOptionsTab.SECNAME, courier.getMediaPathProperty());
					if (imagePath == null)
						imagePath = "";
					else
						courier.checkForDestPath(imagePath);
				}
				statusbar.clearProgress();
				statusbar.displayInfo(Resources.INFO_COPYING);
				statusbar.setProgress(images_to_copy.size());
				for (int i = 0; i < images_to_copy.size(); i++) {
					courier.deliver((String) images_to_copy.elementAt(i), imagePath);
					statusbar.tickProgress();
				}
			}
		} finally {
			statusbar.clearInfo();
			statusbar.clearProgress();
			courier.done();
		}
	}

	Controller controller;

	private IniPrefs s;

	String album_name;

	AlbumPane album_pane;

	Ftp ftp_connection;

	StringBuffer html;

	Vector images_to_copy;

	StatusBar statusbar;

	String encoding;
}

class Lex {
	String text;

	int code;

	Lex(String text, int code) {
		this.text = text;
		this.code = code;
		// System.err.print("Lex: "+code+":"+text);
	}
}

class Lexer {
	final static char CPERCENT = '%';

	final static char CEXLAM = '!';

	final static char[] LEX_MARKER = { CEXLAM, CPERCENT };

	char[] stream;

	int p;

	int state;

	ResourceBundle mnemonicsBundle;

	Hashtable mnemonics;

	Lexer(String url) {
		InputStream is = null;
		// note: content length and file length are not used here to
		// improve relaiability of the code
		try {
			// try it like URL
			try {
				is = new URL(url).openStream();
			} catch (MalformedURLException mfe) {
			}
			if (is == null) { // OK, try it as a file
				is = new FileInputStream(url);
			}
			byte[] buffer = new byte[4096];
			StringBuffer sb = new StringBuffer(buffer.length);
			do {
				int cl = is.read(buffer);
				if (cl < 0)
					break;
				sb.append(new String(buffer, 0, cl));
			} while (true);
			stream = sb.toString().toCharArray();
			is.close();
		} catch (IOException ioe) {
			stream = Resources.TMPL_NOPUBLISHTEMPLATE.toCharArray();
		}
		mnemonics = new Hashtable(HtmlProducer.LEXEM_MENMONICS.length);
		for (int i = 0; i < HtmlProducer.LEXEM_MENMONICS.length; i++)
			mnemonics.put(HtmlProducer.LEXEM_MENMONICS[i], new Integer(i));
		state = STATE_IN_HTML;
	}

	final static int STATE_IN_HTML = 0;

	final static int STATE_IN_MARKER = 1;

	final static int STATE_IN_LEXEM = 2;

	final static int STATE_IN_ = 3;

	final static String DELIMS = " -/,\\()<>\"'\n\r" + LEX_MARKER[0];

	Lex getNextLex() {
		int m = p;
		int ms = LEX_MARKER.length;
		do {
			if (p >= stream.length)
				return new Lex(new String(stream, m, stream.length - m), HtmlProducer.END_DOCUMENT);
			switch (state) {
			case STATE_IN_HTML:
				if (ms > 0) {
					if (stream[p] == LEX_MARKER[LEX_MARKER.length - ms]) {
						state = STATE_IN_MARKER;
						ms--;
						return new Lex(new String(stream, m, p - m), HtmlProducer.PLAIN_HTML);
					}
				}
				break;
			case STATE_IN_MARKER:
				if (ms == 0) { // 
					state = STATE_IN_LEXEM;
					ms = LEX_MARKER.length;
				} else if (stream[p] == LEX_MARKER[LEX_MARKER.length - ms])
					ms--;
				else {
					state = STATE_IN_HTML;
					ms = LEX_MARKER.length;
				}
				break;
			case STATE_IN_LEXEM:
				if (DELIMS.indexOf(stream[p]) >= 0) {
					String text = new String(stream, m + LEX_MARKER.length, p - m - LEX_MARKER.length).toLowerCase();
					Integer li = (Integer) mnemonics.get(text);
					state = STATE_IN_HTML;
					if (li != null)
						return new Lex(text, li.intValue());
				}
			}
			p++;
		} while (true);
	}
}