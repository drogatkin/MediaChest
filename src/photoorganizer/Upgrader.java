/* PhotoOrganizer - $RCSfile: Upgrader.java,v $                               
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
 *  $Id: Upgrader.java,v 1.12 2008/03/21 05:25:33 dmitriy Exp $                
 */

package photoorganizer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import org.aldan3.util.IniPrefs;
import org.aldan3.util.Stream;

import photoorganizer.album.Access;
import photoorganizer.renderer.AlbumPane;

/**
 * this class checks for newer versions of the product available and does a
 * upgrade (if requested).
 */
public class Upgrader {
	static final String VERSION_TAG_URI = "VersionTag";

	static final String VERSION_DNLOAD_URI = "Download";

	static final String SECNAME = "Upgrader";

	static final int CHECKING_DURATION = 30 * 60 * 1000;

	static final String DL_EXT = ".dl";

	// TODO: make steps to localization
	static final String DBUPGRADENOTICE = "upgradedb.htm";

	static final String DINARIESUPGRADENOTICE = "programupgrade.htm";

	Upgrader(Controller controller) {
		this.controller = controller;
		checkJavaVersion();
		checkDBVersion();
		checkBinariesVersion();
	}

	protected void checkDBVersion() {
		ResultSet result = null;
		try {
			result = ((AlbumPane) controller.component(Controller.COMP_ALBUMPANEL)).getAccess()
					.getAvailableConnection().createStatement().executeQuery(
							Access.SQL_SELECT + "*" + Access.SQL_FROM + Access.CONNECTION_DATABASE + Access.SQL_WHERE
									+ "1<>2");
			if (result.getMetaData().getColumnCount() < Access.CONNECTION_DATABASE_STRUCTURE.length)
				controller.showHtml(Stream.streamToString(new FileInputStream(controller.getHomeDirectory()
						+ DBUPGRADENOTICE), (String) null, -1), Resources.TITLE_UPGRDNOTIFIC);
		} catch (Exception e) {
			// TODO: show message box about exception
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.getStatement().close();
				} catch (SQLException es) {
				}
				try {
					result.close();
				} catch (SQLException es) {
				}
			}
		}
	}

	// TODO: incremental upgrade, like for version 74 particular files, for 75
	// particular files
	// the upgrader maintain map with the latest version of sources for
	// destinations and then do
	// actuall download and replacement
	// new format should look like
	// version m.m
	// nn
	// source:destination
	// ...
	// empty line
	// repeat a block above

	protected void checkBinariesVersion() {
		// System.err.println("Class
		// path:"+System.getProperty("java.class.path")
		// +"\nWorking dir:"+System.getProperty("user.dir"));
		IniPrefs s = controller.getPrefs();
		String ts = (String) s.getProperty(SECNAME, VERSION_TAG_URI);
		if (ts == null)
			ts = PhotoOrganizer.BASE_URL + VERSION_TAG_URI;
		final String versionUrl = ts;
		ts = (String) s.getProperty(SECNAME, VERSION_DNLOAD_URI);
		if (ts == null)
			ts = PhotoOrganizer.BASE_URL + VERSION_DNLOAD_URI;
		final String downloadUrl = ts;
		Thread versionChecker = new Thread(new Runnable() {
			public void run() {
				do {
					try {
						BufferedReader r = new BufferedReader(new InputStreamReader(new URL(versionUrl).openStream()));
						String line = r.readLine();
						boolean needUpgrade = false;
						// expecting version M.m
						if (PhotoOrganizer.VERSION.compareTo(line) < 0)
							needUpgrade = true;
						line = r.readLine();
						if (needUpgrade == false) {
							if (line != null)
								try {
									needUpgrade = PhotoOrganizer.BUILD < Integer.parseInt(line.trim());
								} catch (Exception e) {
									System.err.println("Exception in getting build number " + e);
								}
						}
						if (needUpgrade) {
							int userInput = JOptionPane.showConfirmDialog(controller.mediachest,
									Resources.LABEL_CONFIRM_UPGRADE, Resources.TITLE_UPGRADE,
									JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
							if (JOptionPane.YES_OPTION == userInput) {

								// the following lines look like
								// sourcelocation:destinitionlocation
								while ((line = r.readLine()) != null) {
									StringTokenizer fst = new StringTokenizer(line, ":"); // jar
																							// file
																							// name
									if (fst.hasMoreTokens()) {
										String src = fst.nextToken();
										String dst = fst.hasMoreTokens() ? fst.nextToken() : src;
										try {
											BufferedInputStream dl = new BufferedInputStream(new URL(new URL(
													downloadUrl), src).openStream());
											// find home directory
											String hd = null;
											StringTokenizer st = new StringTokenizer(System
													.getProperty("java.class.path"), File.pathSeparator);
											while (st.hasMoreTokens()) {
												hd = st.nextToken();
												if (hd.endsWith(dst))
													break;
												hd = null;
											}
											File of, tf;
											if (hd != null) {
												of = new File(hd + DL_EXT);
												tf = new File(hd);
											} else {
												hd = System
														.getProperty(
																"user.dir"/* PhotoOrganizer.PROGRAMNAME+Serializer.HOMEDIRSUFX */,
																".");
												of = new File(hd, dst + DL_EXT);
												tf = new File(hd, dst);
											}
											upgradeFile(dl, of, tf);
										} catch (IOException ioe) {
											System.err.println("File " + src + " can not replace " + dst
													+ " due io problem " + ioe);
										}
									}
								}
								controller.askToRestart();
							} else if (JOptionPane.NO_OPTION == userInput)
								break;
						}
					} catch (Exception e) {
						System.err.println("Upgrade checking exception " + e + " for " + downloadUrl);
						// e.printStackTrace();
					}
					try {
						Thread.sleep(CHECKING_DURATION);
					} catch (InterruptedException ie) {
					}
				} while (true);
			}
		}, "VersionChecker");
		versionChecker.setPriority(Thread.MIN_PRIORITY);
		versionChecker.start();
	}

	protected void checkJavaVersion() {
		if (false)
			JOptionPane.showMessageDialog(controller.mediachest, PhotoOrganizer.PROGRAMNAME + ' '
					+ Resources.LABEL_JDK_UPGRADE, Resources.TITLE_UPGRADE, JOptionPane.CLOSED_OPTION
					+ JOptionPane.WARNING_MESSAGE);
	}

	protected void upgradeFile(InputStream is, File df, File tf) throws IOException {
		FileOutputStream os;
		Stream.copyStream(is, os = new FileOutputStream(df));
		os.flush();
		os.close();
		if (!tf.delete())
			System.err.println("Can not delete the upgradable file " + tf);
		if (!df.renameTo(tf)) {
			System.err.println("Can not rename the downloaded file " + df + " to the upgradable file " + tf);
			System.err.println("Copying.." + df + " to " + tf);
			Stream.copyFile(df, tf);
			System.err.println("Deleting.." + df);
			if (!df.delete())
				System.err.println("Can not delete the downloaded file " + df);
		}
	}

	protected Controller controller;
}
