/* MediaChest - $RCSfile: MediaAccess.java,v $                               
 * Copyright (C) 2001 Dmitriy Rogatkin.  All rights reserved.                         
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
 *  $Id: MediaAccess.java,v 1.23 2015/08/12 07:10:20 cvs Exp $                
 */

package photoorganizer.album;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Vector;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.BasicJpeg;

import org.aldan3.util.CsvTokenizer;

import photoorganizer.Controller;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.renderer.MiscellaneousOptionsTab;

// TODO: add reporting exceptions to UI

/**
 * this class extends basic database operation for music content
 */
public class MediaAccess extends Access {

	static final String MEDIAS_DATABASE = "Medias";

	static final String[][] MEDIAS_DATABASE_STRUCTURE = {
			{ "MediaId", " INTEGER NOT NULL UNIQUE," }, // primary key
			{ "Name", " CHAR(255) NOT NULL," }, { "LocId", " INTEGER," },
			{ "TempLoc", " CHAR," }, { "ChkSum", " INTEGER," },
			{ "Length", " INTEGER," }, { "Artist", " CHAR(255)," },
			{ "Year", " INTEGER," },
			{ "Title", " CHAR(255)," },
			{ "Album", " CHAR(100)," },
			{ "Genre", " INTEGER," }, // TINYINT
			{ "Quality", " CHAR(10)," }, { "Composer", " CHAR(100)," },
			{ "Conductor", " CHAR(100)," }, { "Band", " CHAR(100)," },
			{ "Lyricist", " CHAR(100)," }, { "Type", " INTEGER," }, // TINYINT
			{ "Commentary", " CHAR(255)" } };

	static final int MEDIAS_MEDIAID = 0;

	static final int MEDIAS_NAME = 1;

	static final int MEDIAS_LOCID = 2;

	static final int MEDIAS_TEMPLOC = 3;

	static final int MEDIAS_CHKSUM = 4;

	static final int MEDIAS_LENGTH = 5;

	static final int MEDIAS_ARTIST = 6;

	static final int MEDIAS_YEAR = 7;

	static final int MEDIAS_TITLE = 8;

	static final int MEDIAS_ALBUM = 9;

	static final int MEDIAS_GENRE = 10;

	static final int MEDIAS_QUALITY = 11;

	static final int MEDIAS_COMPOSER = 12;

	static final int MEDIAS_CONDUCTOR = 13;

	static final int MEDIAS_BAND = 14;

	static final int MEDIAS_LYRICIST = 15;

	static final int MEDIAS_TYPE = 16;

	static final int MEDIAS_COMMENTARY = 17;

	static final String IDENTIFIERS_TABLE = "Identifiers";

	static final String[] TABLES = { PICTURES_DATABASE, MEDIAS_DATABASE,
			CONNECTION_DATABASE, ALBUM_DATABASE, GROUPS_DATABASE,
			LOCATIONS_DATABASE, };

	// better approach is to create a hashtable and use table as a key to
	// description
	static final String[][][] DESCRIPTIONS = { PICTURES_DATABASE_STRUCTURE,
			MEDIAS_DATABASE_STRUCTURE, CONNECTION_DATABASE_STRUCTURE,
			ALBUM_DATABASE_STRUCTURE, GROUPS_DATABASE_STRUCTURE,
			LOCATIONS_DATABASE_STRUCTURE };

	public MediaAccess(Controller controller) {
		super(controller);
	}

	protected String[] getTables() {
		String[] result = new String[TABLES.length + 1];
		System.arraycopy(TABLES, 0, result, 0, TABLES.length);
		result[TABLES.length] = IDENTIFIERS_TABLE;
		return result;
	}

	protected String[][][] getDescriptions() {
		String[][][] result = new String[DESCRIPTIONS.length + 1][][];
		System.arraycopy(DESCRIPTIONS, 0, result, 0, DESCRIPTIONS.length);
		String[][] idents = new String[TABLES.length][];
		for (int i = 0; i < TABLES.length - 1; i++)
			idents[i] = new String[] { TABLES[i], " INTEGER," };
		idents[TABLES.length - 1] = new String[] { TABLES[TABLES.length - 1],
				" INTEGER" };
		result[TABLES.length] = idents;
		return result;
	}

	// we allow to exist different medias in the same album
	public void insertMediasToAlbum(int album, MediaFormat[] medias) {
		// find out only images first
		boolean wasImage = false;
		boolean wasAudio = false;
		boolean wasVideo = false;
		Vector otherKind = null;
		int r = 0;
		for (int f = 0; f < medias.length; f++) {

			if (medias[f].getType() != MediaFormat.STILL) {
				if (wasImage) {// mixed
					if (otherKind == null) {
						otherKind = new Vector();
						r = f;
					}
					otherKind.addElement(medias[f]);
				} else {
					wasAudio = true;
					if (r < f)
						medias[r] = medias[f];
					r++;
				}
			} else {
				if (wasAudio) {// mixed
					if (otherKind == null) {
						otherKind = new Vector();
						r = f;
					}
					otherKind.addElement(medias[f]);
				} else {
					wasImage = true;
					if (r < f)
						medias[r] = medias[f];
					r++;
				}
			}
		}
		if (wasImage) {
			if (r < medias.length && otherKind != null /* !? too strong */) {
				BasicJpeg[] res = new BasicJpeg[r];
				System.arraycopy(medias, 0, res, 0, r);
				insertPicturesToAlbum(album, res);
				insertAudiosToAlbum(album,
						(MediaFormat[]) otherKind
								.toArray(new MediaFormat[otherKind.size()]));
			} else
				insertPicturesToAlbum(album, medias);
		} else if (wasAudio) {
			if (r < medias.length && otherKind != null /* !? too strong */) {
				MediaFormat[] res = new MediaFormat[r];
				System.arraycopy(medias, 0, res, 0, r);
				insertAudiosToAlbum(album, res);
				insertPicturesToAlbum(album,
						(MediaFormat[]) otherKind
								.toArray(new MediaFormat[otherKind.size()]));
			} else
				insertAudiosToAlbum(album, medias);
		}
	}

	public synchronized void insertAudiosToAlbum(int album, MediaFormat[] medias) {
		final String INSERTAUDIOSTOALBUM_1 = SQL_INSERT + LOCATIONS_DATABASE
				+ " (" + LOCATIONS_DATABASE_STRUCTURE[LOCATIONS_LOCID][0] + ','
				+ LOCATIONS_DATABASE_STRUCTURE[LOCATIONS_DISK][0]
				+ ") VALUES (";
		final String INSERTAUDIOSTOALBUM_2 = SQL_INSERT + MEDIAS_DATABASE
				+ " (";

		final String INSERTAUDIOSTOALBUM_3 = SQL_INSERT + CONNECTION_DATABASE
				+ " (" + CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0]
				+ ',' + CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0]
				+ ',' + CONNECTION_DATABASE_STRUCTURE[CONNECTION_SEQNUMBER][0]
				+ ") VALUES (";
		for (int i = 0; i < medias.length; i++) {
			if (belongsToAlbum(album, medias[i]))
				medias[i] = null;
		}
		// move code below to constant part
		StringBuffer fieldparts = new StringBuffer(
				MEDIAS_DATABASE_STRUCTURE[0][0]);
		StringBuffer valueparts = new StringBuffer(" VALUES (?");
		for (int i = 1; i < MEDIAS_DATABASE_STRUCTURE.length; i++) {
			fieldparts.append(',').append(MEDIAS_DATABASE_STRUCTURE[i][0]);
			valueparts.append(",?");
		}
		fieldparts.append(')');
		valueparts.append(')');
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(
					INSERTAUDIOSTOALBUM_2 + fieldparts.toString()
							+ valueparts.toString());
			stmt = getAvailableConnection().createStatement();
			for (int i = 0; i < medias.length; i++) {
				try {
					if (medias[i] == null)
						continue;
					// cross pic/media tables unique id
					int medid = generateUniqueId(PICTURES_DATABASE,
							MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0]);
					if (medid == -1)
						continue;
					MediaInfo ai = medias[i].getMediaInfo();
					if (ai == null)
						continue;
					pstmt.setInt(MEDIAS_MEDIAID + 1, medid);
					// note database field index start from 1
					for (int k = 2; k <= MEDIAS_DATABASE_STRUCTURE.length; k++) {
						try {
							// TODO introduce setString with encoding
							switch (k - 1) {
							case MEDIAS_NAME:
								pstmt.setString(
										k,
										convertUsingEnc(medias[i].getFile()
												.getPath(), true, null));
								break;
							case MEDIAS_LOCID:
								pstmt.setInt(k, 0);
								break;
							case MEDIAS_TEMPLOC:
								pstmt.setString(k, "N");
								break;
							case MEDIAS_CHKSUM:
								pstmt.setInt(k, 0);
								break;
							case MEDIAS_LENGTH:
								try {
									pstmt.setInt(k, (int) ai
											.getLongAttribute(MediaInfo.LENGTH));
								} catch (Exception ie) {
									pstmt.setLong(k, -1);
								}
								break;
							case MEDIAS_ARTIST:
								pstmt.setString(
										k,
										convertUsingEnc(
												ai.getAttribute(
														MediaInfo.ARTIST)
														.toString(), true, null));
								break;
							case MEDIAS_YEAR:
								try {
									pstmt.setInt(k,
											ai.getIntAttribute(MediaInfo.YEAR));
								} catch (Exception ie) {
									pstmt.setInt(k, 0);
								}
								break;
							case MEDIAS_TITLE:
								pstmt.setString(
										k,
										convertUsingEnc(
												ai.getAttribute(MediaInfo.TITLE)
														.toString(), true, null));
								break;
							case MEDIAS_ALBUM:
								pstmt.setString(
										k,
										convertUsingEnc(
												ai.getAttribute(MediaInfo.ALBUM)
														.toString(), true, null));
								break;
							case MEDIAS_GENRE:
								try {
									pstmt.setInt(k,
											ai.getIntAttribute(MediaInfo.GENRE));
									// pstmt.setByte(k,
									// 255&ai.getIntAttribute(MediaInfo.GENRE));
								} catch (Exception ie) {
									pstmt.setInt(k, 12); // other
								}
								break;
							case MEDIAS_QUALITY:
								pstmt.setString(k,
										ai.getAttribute(MediaInfo.ESS_QUALITY)
												.toString());
								break;
							case MEDIAS_COMPOSER:
								pstmt.setString(
										k,
										convertUsingEnc(
												ai.getAttribute(
														MediaInfo.COMPOSER)
														.toString(), true, null));
								break;
							case MEDIAS_CONDUCTOR:
								pstmt.setString(k,
										ai.getAttribute(MediaInfo.CONDUCTOR)
												.toString());
								break;
							case MEDIAS_BAND:
								pstmt.setString(k,
										ai.getAttribute(MediaInfo.BAND)
												.toString());
								break;
							case MEDIAS_LYRICIST:
								pstmt.setString(k,
										ai.getAttribute(MediaInfo.LYRICIST)
												.toString());
								break;

							case MEDIAS_TYPE:
								try {
									pstmt.setInt(
											k,
											ai.getIntAttribute(MediaInfo.MPEGLEVEL));
								} catch (Exception ie) {
									pstmt.setInt(k, -1);
								}
								break;
							case MEDIAS_COMMENTARY:
								pstmt.setString(
										k,
										convertUsingEnc(
												ai.getAttribute(
														MediaInfo.COMMENTS)
														.toString(), true, null));
								break;
							default:
								pstmt.setString(k, "");
							}
						} catch (Exception e) {
							System.err.println("Problem setting field :" + k
									+ ", ex:" + e);
							pstmt.setString(k, "");
						}
					}
					int count = pstmt.executeUpdate();
					System.err.printf("Updated detail %d%n", count);
					// TODO i = i + max(seqNumber)
					count = stmt.executeUpdate(INSERTAUDIOSTOALBUM_3 + medid
							+ ',' + album + ',' + i + ')');
					System.err.printf("Updated locator %d,  %s%n", count,
							INSERTAUDIOSTOALBUM_3 + medid + ',' + album + ','
									+ i + ')');
				} catch (SQLException ex) {
					System.err.println(INSERTAUDIOSTOALBUM_2
							+ fieldparts.toString() + valueparts.toString());
					printChainedSqlException(ex);
				}
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		} finally {
			try {
				if (pstmt != null)
					pstmt.close();
				if (stmt != null)
					stmt.close();
			} catch (SQLException ex) {
				printChainedSqlException(ex);
			}
		}
	}

	public boolean belongsToAlbum(MediaFormat format) {
		final String BELONGSTOALBUM_1 = SQL_SELECT
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + SQL_FROM
				+ MEDIAS_DATABASE + SQL_WHERE
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + '=';
		if (format == null)
			return false;
		if (super.belongsToAlbum(format))
			return true;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			boolean belongs;
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(BELONGSTOALBUM_1
					+ SQLQuote(convertUsingEnc(format.getFile().getPath(),
							true, null))); // getUrl
			// could
			// be
			// better
			belongs = rs.next();
			return belongs;
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			return false;
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	public boolean belongsToAlbum(int albumId, MediaFormat format) {
		final String BELONGSTOALBUM_1 = SQL_SELECT
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + SQL_FROM
				+ MEDIAS_DATABASE + ',' + CONNECTION_DATABASE + SQL_WHERE
				+ CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0] + '='
				+ MEDIAS_DATABASE + '.'
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0] + SQL_AND
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + '=';
		final String BELONGSTOALBUM_2 = SQL_AND + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		if (format == null)
			return false;
		if (super.belongsToAlbum(albumId, format))
			return true;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			boolean belongs;
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(BELONGSTOALBUM_1
					+ SQLQuote(convertUsingEnc(format.getFile().getPath(),
							true, null)) + BELONGSTOALBUM_2 + albumId); // getUrl
			// could
			// be
			// better
			belongs = rs.next();
			return belongs;
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			return false;
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	public Object[] getAlbumContents(int album) {
		final String[] GETALBUMSCONTENTS = {
				SQL_SELECT + PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0]
						+ ","
						+ PICTURES_DATABASE_STRUCTURE[PICTURES_COMMENTARY][0]
						+ SQL_FROM + CONNECTION_DATABASE + ','
						+ PICTURES_DATABASE + SQL_WHERE + CONNECTION_DATABASE
						+ '.'
						+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0]
						+ '=' + PICTURES_DATABASE + '.'
						+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0]
						+ SQL_AND
						+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0]
						+ '=',

				SQL_SELECT + MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + ","
						+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_COMMENTARY][0]
						+ SQL_FROM + CONNECTION_DATABASE + ','
						+ MEDIAS_DATABASE + SQL_WHERE + CONNECTION_DATABASE
						+ '.'
						+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0]
						+ '=' + MEDIAS_DATABASE + '.'
						+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0]
						+ SQL_AND
						+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0]
						+ '=' };
		final String[] GETALBUMSCONTENTS_ENDS = {
				"",
				SQL_ORDERBY
						+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_SEQNUMBER][0]
						+ SQL_ASC };

		Object[] result = new File[0];
		Statement stmt = null;
		ResultSet rs = null;
		for (int i = 0; i < GETALBUMSCONTENTS.length; i++)
			try {
				stmt = getAvailableConnection().createStatement();
				rs = stmt.executeQuery(GETALBUMSCONTENTS[i] + album
						+ GETALBUMSCONTENTS_ENDS[i]);
				// System.err.printf("Album cont q:%s%n", GETALBUMSCONTENTS[i] +
				// album + GETALBUMSCONTENTS_ENDS[i]);
				while (rs.next()) {
					Object[] _tf = new Object[result.length + 1];
					System.arraycopy(result, 0, _tf, 0, result.length);
					// TODO apply encoding for file name !!!!
					_tf[result.length] = new File(convertUsingEnc(
							rs.getString(1).trim(), false, null)); // !!!!
					MediaFormat format = MediaFormatFactory.createMediaFormat(
							(File) _tf[result.length],
							useEncoding() ? MiscellaneousOptionsTab
									.getEncoding(controller) : null, false);
					if (format != null) {
						if (format instanceof BasicJpeg) // trimming after
							// conversion can be more clear
							((BasicJpeg) format).setComment(convertUsingEnc(rs
									.getString(2).trim(), false, null));
						_tf[result.length] = format;
						MediaInfo mi = format.getMediaInfo();
						if (mi instanceof AbstractImageInfo)
							((AbstractImageInfo) mi).setTimeZone(controller
									.getTimeZone());
					}
					result = _tf;
				}
			} catch (SQLException ex) {
				System.err.println(GETALBUMSCONTENTS[i] + album
						+ GETALBUMSCONTENTS_ENDS[i]);
				printChainedSqlException(ex);
			} finally {
				try {
					rs.close();
				} catch (Exception e) {
				}
				try {
					stmt.close();
				} catch (Exception e) {
				}
			}
		return result;
	}

	synchronized int generateUniqueId(String database, String field) {
		int result = 1;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			stmt.setMaxRows(1);
			rs = stmt.executeQuery(SQL_SELECT + database + SQL_FROM
					+ IDENTIFIERS_TABLE);
			if (rs.next()) {
				result = rs.getInt(1);
				int oldResult = super.generateUniqueId(database, field);
				if (rs.wasNull() || result < oldResult)
					result = oldResult;
				stmt.executeUpdate(SQL_UPDATE
						+ IDENTIFIERS_TABLE
						+ SQL_SET
						+ database
						+ '='
						+ (result + 1)
						+ (rs.wasNull() ? "" : SQL_WHERE + database + '='
								+ result));
				result++;
			} else {
				stmt.executeUpdate(SQL_INSERT + IDENTIFIERS_TABLE + " ("
						+ database + ") VALUES(" + result + ')');
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
		return result;
	}

	public synchronized void deletePicture(int mediaId) {
		final String DELETEMEDIA = SQL_DELETE + SQL_FROM + MEDIAS_DATABASE
				+ SQL_WHERE + MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0]
				+ '=';
		super.deletePicture(mediaId);

		Statement stmt = null;
		try {
			stmt = getAvailableConnection().createStatement();
			stmt.executeUpdate(DELETEMEDIA + mediaId);
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For queries " + DELETEMEDIA + mediaId);
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	public synchronized void deletePicture(int album, String name) {
		final String DELETEMEDIA_1 = SQL_SELECT + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + SQL_FROM
				+ CONNECTION_DATABASE + ',' + MEDIAS_DATABASE + SQL_WHERE
				+ CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + '='
				+ MEDIAS_DATABASE + '.'
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0] + SQL_AND
				+ CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		final String DELETEMEDIA_2 = SQL_AND + MEDIAS_DATABASE + '.'
				+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + "=";

		final String DELETEMEDIA_3 = SQL_DELETE + SQL_FROM
				+ CONNECTION_DATABASE + SQL_WHERE
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + '=';
		final String DELETEMEDIA_4 = " AND "
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		super.deletePicture(album, name);
		Statement stmt = null;
		ResultSet rs = null;
		Statement stmt2 = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(DELETEMEDIA_1 + album + DELETEMEDIA_2
					+ SQLQuote(name));
			if (rs.next()) { // should be one pair only
				stmt2 = getAvailableConnection().createStatement();
				stmt.executeUpdate(DELETEMEDIA_3 + rs.getInt(1) + DELETEMEDIA_4
						+ album);
				stmt2.close();
				stmt2 = null;
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For queries " + DELETEMEDIA_1 + album
					+ DELETEMEDIA_2 + '\'' + name + "'\n" + DELETEMEDIA_3 + '?'
					+ DELETEMEDIA_4 + album);
		} finally {
			try {
				stmt2.close();
			} catch (Exception e) {
			}
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * replace common start part in name to another part
	 */
	public synchronized void moveAlbumRoot(String oldRoot, String newRoot) {
		final String[] MOVEALBUMROOT_1 = {
				SQL_SELECT + MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + ','
						+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0]
						+ SQL_FROM + MEDIAS_DATABASE,
				SQL_SELECT + PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0]
						+ ',' + PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0]
						+ SQL_FROM + PICTURES_DATABASE };
		final String[] MOVEALBUMROOT_2 = {
				SQL_UPDATE + MEDIAS_DATABASE + SQL_SET
						+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_NAME][0] + "=?"
						+ SQL_WHERE
						+ MEDIAS_DATABASE_STRUCTURE[MEDIAS_MEDIAID][0] + "=?",
				SQL_UPDATE + PICTURES_DATABASE + SQL_SET
						+ PICTURES_DATABASE_STRUCTURE[MEDIAS_NAME][0] + "=?"
						+ SQL_WHERE
						+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + "=?" };
		if (newRoot == null || oldRoot == null)
			throw new IllegalArgumentException(
					"One or both input parameters are null.");
		final boolean useUpdate = false;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int ol = oldRoot.length();
		for (int i = 0; i < MOVEALBUMROOT_1.length; i++)
			try {
				pstmt = getAvailableConnection().prepareStatement(
						MOVEALBUMROOT_2[i]);
				stmt = getAvailableConnection().createStatement(
						useUpdate ? ResultSet.TYPE_SCROLL_INSENSITIVE
								: ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE);
				rs = stmt.executeQuery(MOVEALBUMROOT_1[i]);
				while (rs.next()) {
					String old = rs.getString(1);
					if (old != null && old.indexOf(oldRoot) == 0) {
						old = old.trim();
						// System.err.println("Replacing "+old+" by
						// "+newRoot+old.substring(ol));
						if (useUpdate) {
							rs.updateString(1, newRoot + old.substring(ol));
							rs.updateRow();
						} else {
							pstmt.setString(1, newRoot + old.substring(ol));
							pstmt.setInt(2, rs.getInt(2));
							pstmt.executeUpdate();
						}
					}
				}
			} catch (SQLException ex) {
				System.err.println(MOVEALBUMROOT_1[i]);
				printChainedSqlException(ex);
			} catch (Exception e) {
				System.err.println("Exception for " + MOVEALBUMROOT_1[i]
						+ " old " + oldRoot + " new " + newRoot + "\nor in "
						+ MOVEALBUMROOT_2[i]);
				e.printStackTrace();
			} finally {
				/*
				 * try { rs.close(); } catch(Exception e){ }
				 */
				try {
					stmt.close(); // closes rs also
				} catch (Exception e) {
				}
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}
	}

	public synchronized void changeAlbumLocation(int oldLoc, int newLoc) {
	}

	public static synchronized void importTables(File importDir, String delim, String date_format,
			Controller controller) throws SQLException, IOException, ParseException {
		MediaAccess ma = new MediaAccess(controller);
		for (int t = 0; t < TABLES.length; t++) {
			String table = TABLES[t];
			// TODO add request to get separator and date format
			if (date_format == null || date_format .isEmpty())
				date_format = "MM/dd/yyyy hh:mm:ss"; // MSACCESS
			//date_format = "yyyy-MM-dd hh:mm:ss";
			// TODO release resource as reader
			CsvTokenizer tkz = new CsvTokenizer(null, new BufferedReader(
					new FileReader(new File(importDir, table + ".txt"))), delim, true, 0);
			String[][] description = DESCRIPTIONS[t];
			String q = "insert into " + table + " (" + description[0][0];
			String v = ") values (?";

			for (int d = 1; d < description.length; d++) {
				q += "," + description[d][0];
				v += ",?";
			}
			q += v + ")";
			//System.err.println(q);
			PreparedStatement pstmt = ma.getAvailableConnection()
					.prepareStatement(q);
			while (tkz.hasMoreTokens()) {
				int d = 0;
				for (; d < description.length && tkz.hasMoreTokens(); d++) {
					String type = description[d][1].trim();
					String value = tkz.nextToken().trim();
					System.err.printf("Looking for %s==%s (%d) - %s%n", type, value, d, description[d][0]);

					if (type.startsWith("INT"))
						if (value.length() == 0 || value.equals("\\N"))
							pstmt.setNull(d + 1, Types.INTEGER);
						else
							pstmt.setInt(d + 1, Integer.parseInt(value));
					else if (type.startsWith("FLOAT"))
						if (value.length() == 0)
							pstmt.setNull(d + 1, Types.FLOAT);
						else
							pstmt.setFloat(d + 1, Float.parseFloat(value));
					else if (type.startsWith("TIMESTAMP"))
						if (value.length() == 0)
							pstmt.setNull(d + 1, Types.DATE);
						else
							pstmt.setTimestamp(d + 1, new Timestamp(
									new SimpleDateFormat(date_format,
											Locale.US).parse(value).getTime()));
					else
						pstmt.setString(d + 1, value);
				}
				for (; d < description.length; d++) {
					String type = description[d][1].trim();
					if (type.startsWith("INT"))
						pstmt.setInt(d + 1, 0);
					else if (type.startsWith("FLOAT"))
						pstmt.setFloat(d + 1, 0f);
					else if (type.startsWith("TIMESTAMP"))
						pstmt.setTimestamp(d + 1, new Timestamp(0));
					else
						pstmt.setString(d + 1, "");
				}
				pstmt.executeUpdate();
				tkz.advanceToNextLine();
			}
			pstmt.close();
		}
		// TODO add IDENTIFIERS_TABLE 
		System.err.printf("Import completed%n");
	}
	// more methods handling location
	// create location, change location info, find location
}
