/* MediaChest -  $RCSfile: Access.java,v $
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
 * $Id: Access.java,v 1.33 2013/04/09 07:30:00 cvs Exp $
 */
package photoorganizer.album;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import mediautil.gen.MediaFormat;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.BasicJpeg;

import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.renderer.AlbumOptionsTab;
import photoorganizer.renderer.MiscellaneousOptionsTab;

public class Access {
	/**
	 * database structures Pictures
	 * 
	 * PicId INT U K Name CHAR(255) LocId INT ChkSum INT Size INT Created
	 * TIMESTAMP Shutter Aperture Flash Zoom Quality Make Model Resolution
	 * Commentary
	 * 
	 * Connection PicId INT K AlbumId INT
	 * 
	 * Album
	 * 
	 * AlbumId INT U K --->>GroupId INT Name CHAR(255)
	 * 
	 * Groups
	 * 
	 * GroupId INT K (parent album id) AlbumId INT
	 * 
	 * Locations
	 * 
	 * LocId INT U K Disk CHAR(255)
	 */

	static final String PICTURES_DATABASE = "Pictures";

	static final String[][] PICTURES_DATABASE_STRUCTURE = { { "PicId", " INTEGER NOT NULL UNIQUE," },
			{ "Name", " CHAR(255) NOT NULL," }, { "LocId", " INTEGER," }, { "ChkSum", " FLOAT," },
			{ "Size", "   INTEGER," }, { "Created", " TIMESTAMP," }, { "Shutter", " FLOAT," },
			{ "Aperture", " FLOAT," }, { "Flash", " CHAR," }, { "Zoom", " FLOAT," }, { "Quality", " CHAR(40)," },
			{ "Make", " CHAR(30)," }, // VCHAR
			{ "Model", " CHAR(30)," }, { "Resolution", " CHAR(10)," }, { "Commentary", " CHAR(255)" } };

	static final int PICTURES_PICID = 0;

	static final int PICTURES_NAME = 1;

	static final int PICTURES_LOCID = 2;

	static final int PICTURES_CHKSUM = 3;

	static final int PICTURES_SIZE = 4;

	static final int PICTURES_CREATED = 5;

	static final int PICTURES_SHUTTER = 6;

	static final int PICTURES_APERTURE = 7;

	static final int PICTURES_FLASH = 8;

	static final int PICTURES_ZOOM = 9;

	static final int PICTURES_QUALITY = 10;

	static final int PICTURES_MAKE = 11;

	static final int PICTURES_MODEL = 12;

	static final int PICTURES_RESOLUTION = 13;

	static final int PICTURES_COMMENTARY = 14;

	static final String DIR_ENC = "ISO-8859-1";

	public static final String CONNECTION_DATABASE = "Connection";

	public static final String[][] CONNECTION_DATABASE_STRUCTURE = { { "PicId", " INTEGER NOT NULL," },
			{ "AlbumId", " INTEGER," }, { "SeqNumber", " INTEGER," }, { "Target", " CHAR(10)" } };

	public static final int CONNECTION_PICID = 0;

	public static final int CONNECTION_ALBUMID = 1;

	public static final int CONNECTION_SEQNUMBER = 2;

	public static final int CONNECTION_TARGET = 3;

	static final String ALBUM_DATABASE = "Album";
	
	static final String PRIMARY_KEY = "PRIMARY KEY"; // removed from creation

	static final String[][] ALBUM_DATABASE_STRUCTURE = { { "AlbumId", " INTEGER NOT NULL UNIQUE," },
	// --->>{"GroupId", " INTEGER,"},
			{ "Name", "    CHAR(255)" } };

	static final int ALBUM_ALBUMID = 0;

	// --->>static final int ALBUM_GROUPID = 2;
	static final int ALBUM_NAME = 1;

	static final String GROUPS_DATABASE = "Groups";

	static final String[][] GROUPS_DATABASE_STRUCTURE = { { "GroupId", " INTEGER NOT NULL," }, // PRIMARY
			// KEY
			{ "AlbumId", " INTEGER NOT NULL" } };

	static final int GROUPS_GROUPID = 0;

	static final int GROUPS_ALBUMID = 1;

	static final String LOCATIONS_DATABASE = "Locations";

	static final String[][] LOCATIONS_DATABASE_STRUCTURE = { { "LocId", " INTEGER NOT NULL UNIQUE," },
			{ "Disk", " CHAR(255)" } };

	static final int LOCATIONS_LOCID = 0;

	static final int LOCATIONS_DISK = 1;

	static final String[] TABLES = { PICTURES_DATABASE, CONNECTION_DATABASE, ALBUM_DATABASE, GROUPS_DATABASE,
			LOCATIONS_DATABASE };

	// better approach is to create a hashtable and use table as a key to
	// description
	static final String[][][] DESCRIPTIONS = { PICTURES_DATABASE_STRUCTURE, CONNECTION_DATABASE_STRUCTURE,
			ALBUM_DATABASE_STRUCTURE, GROUPS_DATABASE_STRUCTURE, LOCATIONS_DATABASE_STRUCTURE };

	public static final String SQL_INSERT = "INSERT INTO ";

	public static final String SQL_SELECT = "SELECT ";

	public static final String SQL_FROM = " FROM ";

	public static final String SQL_WHERE = " WHERE ";

	public static final String SQL_DELETE = "DELETE ";

	public static final String SQL_UPDATE = "UPDATE ";

	public static final String SQL_SET = " SET ";

	public static final String SQL_AND = " AND ";

	public static final String SQL_ORDERBY = " ORDER BY ";

	public static final String SQL_ASC = " ASC";

	public static final String SQL_DESC = " DESC";

	static DecimalFormat APERTURE_FMT = new DecimalFormat("#0.#");

	static DecimalFormat SHUTTER_FMT = new DecimalFormat("#0.####");

	static DecimalFormat FOCAL_FMT = new DecimalFormat("##0.#");

	boolean[] table_present;

	protected Access(Controller controller) {
		this.controller = controller;
		// DriverManager.setLogWriter(new PrintWriter(System.err)); // for JDK
		// 1.2
		// DriverManager.setLogStream(System.err);
		table_present = new boolean[getTables().length];
		init();
	}

	protected String[] getTables() {
		return TABLES;
	}

	protected String[][][] getDescriptions() {
		return DESCRIPTIONS;
	}

	public void init() {
		forgetTables();
		IniPrefs s = controller.getPrefs();
		String db_url = (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.DBURL);
		if (db_url == null)
			return;
		if (db_url.startsWith(AlbumOptionsTab.DEFAULT_DB_URL)) {
			String db_home = (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.DB_HOME);
			if (db_home != null)
				System.setProperty("Dderby.system.home", db_home);
		}
		Driver jdbc_driver = null;
		String cause = "";
		try {
			jdbc_driver = DriverManager.getDriver(db_url);
		} catch (SQLException e) {
			cause = e.toString();
		} catch (Exception e) {
			cause = e.toString();
		}
		if (jdbc_driver == null) {
			try {
				String className = (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.DRIVER);
				jdbc_driver = (Driver) Class.forName(className).newInstance();
			} catch (Exception e) {
				cause = e.toString();
			}
		}
		if (jdbc_driver == null) {
			if (IniPrefs.getInt(controller.getPrefs().getProperty(AlbumOptionsTab.SECNAME,
					AlbumOptionsTab.IGNORE_DB_ERR), 1) == 0)
				JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_JDBCDRV_LOAD + cause,
						Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			return;
		}
		closeAllConnections();
		// get just one connection
		try {
			connection = DriverManager.getConnection(db_url, (String) s.getProperty(AlbumOptionsTab.SECNAME,
					AlbumOptionsTab.USER), (String) s.getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.PSWD));
			checkForWarning(connection.getWarnings());
			DatabaseMetaData dma = connection.getMetaData();

			System.err.println("Connected to " + dma.getURL());
			System.err.println("Driver       " + dma.getDriverName());
			System.err.println("Version      " + dma.getDriverVersion());
			System.err.println("===DRIVER CAPABILITIES===");
			System.err
					.println("-" + (dma.supportsPositionedUpdate() ? " " : " doesn't ") + "support positioned update");
			System.err
					.println("-" + (dma.supportsPositionedDelete() ? " " : " doesn't ") + "support positioned delete");
			System.err.println("-" + (dma.supportsANSI92EntryLevelSQL() ? " " : " doesn't ")
					+ "support ANSI92 entry level");
			System.err.println("-" + (dma.supportsANSI92FullSQL() ? " " : " doesn't ") + "support full ANSI92");
		} catch (SQLException ex) {
			// TODO create db if 1) derby 2) right exception
			printChainedSqlException(ex);
			if (IniPrefs.getInt(controller.getPrefs().getProperty(AlbumOptionsTab.SECNAME,
					AlbumOptionsTab.IGNORE_DB_ERR), 1) == 0)
				JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_JDBC_CONN + ex,
						Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			if (IniPrefs.getInt(controller.getPrefs().getProperty(AlbumOptionsTab.SECNAME,
					AlbumOptionsTab.IGNORE_DB_ERR), 1) == 0)
				JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_JDBC_CONN + e,
						Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			return;
		}
		getMetaInfo();
	}

	public void forgetTables() {
		for (int i = 0; i < table_present.length; i++)
			table_present[i] = false;
	}

	void getMetaInfo() {
		String[] tables = getTables();
		try {
			DatabaseMetaData dbmd = getAvailableConnection().getMetaData();
			String[] tblTypes = new String[1];
			tblTypes[0] = "TABLE";
			ResultSet rset = dbmd.getTables(null, null, null, tblTypes);
			while (rset.next()) {
				String table = rset.getString(3);
				for (int i = 0; i < tables.length; i++) {
					if (tables[i].equals(table.trim()))
						table_present[i] = true;
				}
			}
			rset.close();
		} catch (SQLException e) {
			printChainedSqlException(e);
		}
	}

	public void createTables() {
		try {
			Statement stmt = getAvailableConnection().createStatement();
			String[] tables = getTables();
			String[][][] descriptions = getDescriptions();
			for (int i = 0; i < tables.length; i++) {
				if (!table_present[i]) {
					StringBuffer csql = null;
					System.err.println("Creation of " + tables[i]);
					try {
						try {
							stmt.executeUpdate("DROP TABLE " + tables[i]);
						} catch (Exception e) {
							System.err.println("DROP Sql: " + "DROP TABLE " + tables[i] + ";" + e);
							// do nothing
						}
						csql = new StringBuffer("CREATE TABLE ");
						csql.append(tables[i]);
						csql.append(" (");
						for (int j = 0; j < descriptions[i].length; j++) {
							csql.append(descriptions[i][j][0]);
							csql.append(descriptions[i][j][1]);
						}
						csql.append(")");
						stmt.executeUpdate(csql.toString());
					} catch (SQLException ex) {
						printChainedSqlException(ex);
						System.err.println("Sql: " + csql.toString());
					}
				}
			}
			stmt.close();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		}
	}

	int generateUniqueId(String database, String field) {
		int result = 1;
		try {
			Statement stmt = getAvailableConnection().createStatement();
			stmt.setMaxRows(1);
			ResultSet rs = stmt.executeQuery("SELECT DISTINCT " + field + " FROM " + database + " WHERE " + field
					+ " < 2147483647 ORDER BY 1 DESC");
			if (rs.next())
				result = rs.getInt(field) + 1;
			//System.err.printf("Generating %d of %s in %s%n", result, field, database);
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		}
		return result;
	}

	public synchronized int createAlbum(int parent, String album) {

		final String CREATEALBUM = SQL_INSERT + ALBUM_DATABASE + " (" + ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0]
				+ "," + ALBUM_DATABASE_STRUCTURE[ALBUM_NAME][0] + ") VALUES (?,?)";
		final String CHECKALBUM = SQL_SELECT + ALBUM_DATABASE+"."+ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + SQL_FROM + ALBUM_DATABASE
				+ "," + GROUPS_DATABASE + SQL_WHERE 
				+ ALBUM_DATABASE+"."+ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0]+"="+GROUPS_DATABASE+"."+GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0]
				+SQL_AND+GROUPS_DATABASE+"."+GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0]+"=?"+SQL_AND
				+ ALBUM_DATABASE+"."+ALBUM_DATABASE_STRUCTURE[ALBUM_NAME][0] + "=?" ;
		int result;
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = getAvailableConnection();
			pstmt = con.prepareStatement(CHECKALBUM);
			//System.err.printf("%s %s %d%n", CHECKALBUM, album, parent);
			pstmt.setInt(1, parent);
			pstmt.setString(2, album);
			rs = pstmt.executeQuery();
			if (rs.next() == false) {
				pstmt.close();
				result = generateUniqueId(ALBUM_DATABASE, ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0]);
				pstmt = con.prepareStatement(CREATEALBUM);
				pstmt.setInt(1, result);
				pstmt.setString(2, album);
				pstmt.executeUpdate();
				//System.err.println("!!!!!!!!!!!!!!CREATED");
			} else
				result = rs.getInt(1);
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			result = -1;
		} finally {
			close(rs);
			close(pstmt);
			//close(con);
		}
		return result;
	}

	private void close(Statement stmt) {
		if (stmt != null)
			try {
				stmt.close();
			} catch (SQLException e) {
			}
	}

	/*private void close(Connection con) {
		if (con != null)
			try {
				con.commit();
				con.close();
			} catch (SQLException e) {
			}
	}*/

	private void close(ResultSet rs) {
		if (rs != null)
			try {
				rs.close();
			} catch (SQLException e) {
			}
	}

	/**
	 * album path name in format name1.name2.name3..nameN
	 */
	public int getAlbumId(String path) {
		if (path == null || path.length() == 0)
			return -1;
		int result = 0;
		StringTokenizer sp = new StringTokenizer(path, ".");
		while (sp.hasMoreTokens()) {
			String subname = sp.nextToken();
			result = findAlbumIn(result, subname);
		}
		return result;
	}

	public int getAlbumId(TreePath path) {
		if (path == null || path.getPathCount() == 0)
			return -1;
		int result = 0;
		Object[] _ps = path.getPath();
		for (int i = 1; i < _ps.length; i++)
			// we skip root element
			result = findAlbumIn(result, _ps[i].toString());
		return result;
	}

	public int findAlbumIn(int parent, String name) {
		final String FINDALBUMIN = SQL_SELECT + ALBUM_DATABASE + '.' + ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0]
				+ SQL_FROM + ALBUM_DATABASE + ',' + GROUPS_DATABASE + SQL_WHERE + GROUPS_DATABASE + '.'
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + '=' + ALBUM_DATABASE + '.'
				+ ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + " AND " + GROUPS_DATABASE + '.'
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + "=? AND " + ALBUM_DATABASE + '.'
				+ ALBUM_DATABASE_STRUCTURE[ALBUM_NAME][0] + "=?";
		int result = -1;
		try {
			PreparedStatement pstmt = getAvailableConnection().prepareStatement(FINDALBUMIN);
			pstmt.setInt(1, parent);
			pstmt.setString(2, name);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt(1);
			}
			//System.err.printf("Find %s %d in %s =%d%n", FINDALBUMIN, parent, name, result);
			rs.close();
			pstmt.close();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		}
		return result;
	}

	public int[] getAlbumPath(int album) {
		final String GETPARENTALBUM = SQL_SELECT + GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + SQL_FROM
				+ GROUPS_DATABASE + SQL_WHERE + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + '=';
		int result[] = new int[0];
		int caid = album;
		do {
			Statement stmt = null;
			ResultSet rs = null;

			try {
				stmt = getAvailableConnection().createStatement();
				rs = stmt.executeQuery(GETPARENTALBUM + caid);
				if (rs.next()) {
					caid = rs.getInt(1);
					if (caid > 0) {
						int[] ta = new int[result.length + 1];
						ta[0] = caid;
						System.arraycopy(result, 0, ta, 1, result.length);
						result = ta;
					}
				} else
					caid = -1;
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			} catch (SQLException ex) {
				printChainedSqlException(ex);
				caid = -1;
			} finally {
				if (rs != null)
					try {
						rs.close();
					} catch (SQLException ex) {
					}
				if (stmt != null)
					try {
						stmt.close();
					} catch (SQLException ex) {
					}
			}

		} while (caid > 0);
		return result;
	}

	boolean isInheritedFrom(int album, int[] albums) {
		int[] children = getAlbumsId(album);
		if (children.length == 0)
			return false;
		if (createIntersection(children, albums).length > 0)
			return true;
		for (int i = 0; i < children.length; i++) {
			if (isInheritedFrom(children[i], albums))
				return true;
		}
		return false;
	}

	int[] createIntersection(int[] a1, int[] a2) {
		int[] result = new int[0];
		for (int i = 0; i < a1.length; i++) {
			for (int j = 0; j < a2.length; j++) {
				if (a1[i] == a2[j]) {
					int[] ta = new int[result.length + 1];
					System.arraycopy(result, 0, ta, 0, result.length);
					ta[result.length] = a1[i];
					result = ta;
				}
			}
		}
		return result;
	}

	public synchronized int insertAlbumToAlbum(int[] parents, int album) {
		// foreign keys emulation
		final String INSERTALBUMTOALBUM_1 = SQL_SELECT + ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + " FROM "
				+ ALBUM_DATABASE + " WHERE " + ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + '=';
		final String INSERTALBUMTOALBUM_2 = SQL_INSERT + GROUPS_DATABASE + " ("
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + ',' + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0]
				+ ") VALUES (";
		final String INSERTALBUMTOALBUM_3 = SQL_SELECT + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + SQL_FROM
				+ GROUPS_DATABASE + SQL_WHERE + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + "=%d AND "
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + "=%d";
		// TODO: check for possible loop, like
		// aaa,bbb
		// bbb,ccc
		// ccc,aaa
		// aaa->bbb->ccc->aaa ...
		// i.e. no parents in the inserted album included
		// do traversing the inserted album and check for parent
		if (isInheritedFrom(album, parents))
			return -1;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			boolean present;
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(INSERTALBUMTOALBUM_1 + album);
			present = rs.next();
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
			if (!present)
				return -1; // throw new Exception()
		} catch (SQLException ex) {
			System.err.println("Error in execution query: " + INSERTALBUMTOALBUM_1 + album);
			printChainedSqlException(ex);
			return -1; // throw new Exception()
		} finally {
			close(rs);
			close(stmt);
		}
		int i = 0;
		try {
			// check if there is an album with the same id				
			stmt = getAvailableConnection().createStatement();
			for (i = 0; i < parents.length; i++) {
				rs = stmt.executeQuery(String.format(INSERTALBUMTOALBUM_3, album, parents[i]));
				if (rs.next() == false)
					stmt.executeUpdate(INSERTALBUMTOALBUM_2 + parents[i] + "," + album + ')');
			}
		} catch (SQLException ex) {
			System.err.println("Error in execution query: " + INSERTALBUMTOALBUM_2 + parents[i] + "," + album + ')');
			System.err.println("Or in "+ String.format(INSERTALBUMTOALBUM_3, album, parents[i]));
			printChainedSqlException(ex);
		} finally {
			close(rs);
			close(stmt);
		}

		return album;
	}

	public boolean belongsToAlbum(MediaFormat format) {
		final String BELONGSTOALBUM_1 = SQL_SELECT + PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + SQL_FROM
				+ PICTURES_DATABASE + SQL_WHERE + PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + '=';
		if (format == null)
			return false;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			boolean belongs;
			stmt = getAvailableConnection().createStatement();
			rs = stmt
					.executeQuery(BELONGSTOALBUM_1 + SQLQuote(convertUsingEnc(format.getFile().getPath(), true, null))); // getUrl
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
		final String BELONGSTOALBUM = SQL_SELECT + PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + SQL_FROM
				+ PICTURES_DATABASE + ',' + CONNECTION_DATABASE + SQL_WHERE + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + '=' + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + SQL_AND
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + '=';
		final String BELONGSTOALBUM_2 = SQL_AND + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		if (format == null)
			return false;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			boolean belongs;
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(BELONGSTOALBUM + SQLQuote(convertUsingEnc(format.getFile().getPath(), true, null))
					+ BELONGSTOALBUM_2 + albumId); // getUrl
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

	public synchronized void insertPicturesToAlbum(int album, MediaFormat[] formats) {
		final String INSERTPICTURESTOALBUM_1 = SQL_INSERT + LOCATIONS_DATABASE + " ("
				+ LOCATIONS_DATABASE_STRUCTURE[LOCATIONS_LOCID][0] + ','
				+ LOCATIONS_DATABASE_STRUCTURE[LOCATIONS_DISK][0] + ") VALUES (";
		final String INSERTPICTURESTOALBUM_2 = SQL_INSERT + PICTURES_DATABASE + " (";

		final String INSERTPICTURESTOALBUM_3 = SQL_INSERT + CONNECTION_DATABASE + " ("
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + ','
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + ") VALUES (";
		for (int i = 0; i < formats.length; i++)
			if (belongsToAlbum(album, formats[i]))
				formats[i] = null;
		// move code below to constant part
		StringBuffer fieldparts = new StringBuffer(PICTURES_DATABASE_STRUCTURE[0][0]);
		StringBuffer valueparts = new StringBuffer(" VALUES (?");
		for (int i = 1; i < PICTURES_DATABASE_STRUCTURE.length; i++) {
			fieldparts.append(',').append(PICTURES_DATABASE_STRUCTURE[i][0]);
			valueparts.append(",?");
		}
		fieldparts.append(')');
		valueparts.append(')');
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(
					INSERTPICTURESTOALBUM_2 + fieldparts.toString() + valueparts.toString());
			stmt = getAvailableConnection().createStatement();
			for (int i = 0; i < formats.length; i++) {
				try {
					if (formats[i] == null)
						continue;
					int picid = generateUniqueId(PICTURES_DATABASE, PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0]);
					if (picid == -1)
						throw new SQLException("Can't generate unique id.");
					if (formats[i] instanceof BasicJpeg == false)
						throw new SQLException("An attempt to insert wrong type:" + formats[i].getClass().getName());
					AbstractImageInfo ii = ((BasicJpeg) formats[i]).getImageInfo();
					if (ii == null)
						continue;
					
					pstmt.setInt(1, picid);
					// note database field index start from 1
					for (int k = 2; k <= PICTURES_DATABASE_STRUCTURE.length; k++) {
						switch (k - 1) {
						case PICTURES_NAME:
							pstmt.setString(k, convertUsingEnc(formats[i].getFile().getPath(), true, null));
							break;
						case PICTURES_LOCID:
							pstmt.setInt(k, 0);
							break;
						case PICTURES_CHKSUM:
							pstmt.setInt(k, 0);
							break;
						case PICTURES_SIZE:
							pstmt.setInt(k, (int) formats[i].getFile().length());
							break;
						case PICTURES_CREATED:
							pstmt.setTimestamp(k, new Timestamp(ii.getDateTimeOriginal().getTime()));
							break;
						case PICTURES_SHUTTER:
							pstmt.setString(k, SHUTTER_FMT.format(ii.getShutter().floatValue()));
							break;
						case PICTURES_APERTURE:
							pstmt.setString(k, APERTURE_FMT.format(ii.getFNumber()));
							break;
						case PICTURES_FLASH:
							pstmt.setString(k, ii.isFlash() ? "Y" : "N");
							break;
						case PICTURES_ZOOM:
							pstmt.setString(k, FOCAL_FMT.format(ii.getFocalLength()));
							break;
						case PICTURES_QUALITY:
							pstmt.setString(k, ii.getQuality());
							break;
						case PICTURES_MAKE:
							pstmt.setString(k, ii.getMake());
							break;
						case PICTURES_MODEL:
							pstmt.setString(k, ii.getModel());
							break;
						case PICTURES_RESOLUTION:
							pstmt.setString(k, ii.getResolutionX() + "x" + ii.getResolutionY());
							break;
						case PICTURES_COMMENTARY:
							pstmt.setString(k, "");
							break;
						default:
							// never here??
							pstmt.setObject(k, null);
						}
					}
					pstmt.executeUpdate();
					stmt.executeUpdate(INSERTPICTURESTOALBUM_3 + picid + ',' + album + ')');
				} catch (SQLException ex) {
					printChainedSqlException(ex);
				} catch (Exception ex) {
					ex.printStackTrace();
				} catch (Error er) {
					er.printStackTrace();
				}
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		} finally {
			try {
				pstmt.close();
			} catch (Exception e) {
			}
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	public Object[] getAlbumContents(int album) {
		final String GETALBUMCONTENTS = SQL_SELECT + PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + SQL_FROM
				+ PICTURES_DATABASE + "," + CONNECTION_DATABASE + SQL_WHERE + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=' + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + SQL_AND
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';

		File[] result = new File[0];
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(GETALBUMCONTENTS + album);
			while (rs.next()) {
				File[] _tf = new File[result.length + 1];
				System.arraycopy(result, 0, _tf, 0, result.length);
				_tf[result.length] = new File(convertUsingEnc(rs.getString(1).trim(), false, null));
				result = _tf;
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		} finally {
			try {
				rs.close();
				stmt.close();
			} catch (Exception ex) {
			}
		}
		return result;
	}

	public String getNameOfAlbum(int groupid) {
		final String GETNAMEOFALBUM_1 = SQL_SELECT + ALBUM_DATABASE_STRUCTURE[ALBUM_NAME][0] + SQL_FROM
				+ ALBUM_DATABASE + " WHERE " + ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + "=";
		String result = "Not found";
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(GETNAMEOFALBUM_1 + groupid);
			if (rs.next()) {
				result = rs.getString(1).trim();
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query " + GETNAMEOFALBUM_1 + groupid);
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

	public int getAlbumsCount(int groupid) {
		final String GETALBUMSCOUNT_1 = "SELECT COUNT(*) FROM " + GROUPS_DATABASE + SQL_WHERE
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + '=';
		int result = 0;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(GETALBUMSCOUNT_1 + groupid);
			if (rs.next()) {
				result = rs.getInt(1);
			}
		} catch (NullPointerException ex) {
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		} finally {
			try {
				rs.close();
				stmt.close();
			} catch (Exception e) {
			}
		}
		return result;
	}

	public int[] getAlbumsId(int groupid) {
		final String GETALBUMSID_1 = SQL_SELECT + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + SQL_FROM
				+ GROUPS_DATABASE + SQL_WHERE + GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + "=";
		int[] result = new int[0];
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(GETALBUMSID_1 + groupid);
			int[] _ta;
			while (rs.next()) {
				_ta = result;
				result = new int[_ta.length + 1];
				System.arraycopy(_ta, 0, result, 0, _ta.length);
				result[_ta.length] = rs.getInt(1); //
			}
			rs.close();
			stmt.close();
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

	public synchronized boolean renameAlbumTo(int album, String newname) {
		final String RENAMEALBUMTO_1 = SQL_UPDATE + ALBUM_DATABASE + SQL_SET + ALBUM_DATABASE_STRUCTURE[ALBUM_NAME][0]
				+ "=?" + SQL_WHERE + ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + '=';
		PreparedStatement pstmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(RENAMEALBUMTO_1 + album);
			pstmt.setString(1, newname);
			pstmt.executeUpdate();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query: " + RENAMEALBUMTO_1 + album);
			return false;
		} finally {
			try {
				pstmt.close();
			} catch (Exception e) {
			}
		}
		return true;
	}

	public synchronized void deleteAlbumFrom(int[] parents, int album) {
		// detach the album from parents, and if it doesn't have more
		// parent, just delete it with all children
		final String DELETEALBUMFROM_1 = SQL_DELETE + SQL_FROM + GROUPS_DATABASE + SQL_WHERE
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + "=?" + " AND "
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + "=?";
		final String DELETEALBUMFROM_2 = SQL_SELECT + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + SQL_FROM
				+ GROUPS_DATABASE + SQL_WHERE + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + '=';
		PreparedStatement pstmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(DELETEALBUMFROM_1);
			// delete from all parents
			for (int i = 0; i < parents.length; i++) {
				try {
					pstmt.setInt(1, parents[i]);
					pstmt.setInt(2, album);
					pstmt.executeUpdate();
				} catch (SQLException ex) {
					printChainedSqlException(ex);
					System.err.println("For query " + DELETEALBUMFROM_1 + " p1=" + parents[i] + " p2=" + album);
				}
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("Couldn't prepare statement " + DELETEALBUMFROM_1);
			return;
		} finally {
			if (pstmt != null)
				try {
					pstmt.close();
				} catch (SQLException ex) {
				}
		}

		// check if it still has a parent
		boolean hasParent = false;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(DELETEALBUMFROM_2 + album);
			hasParent = rs.next();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query " + DELETEALBUMFROM_2 + album);
		} finally {
			if (rs != null)
				try {
					rs.close();
				} catch (SQLException ex) {
				}
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ex) {
				}
		}
		if (!hasParent)
			deleteAlbum(album);
	}

	public synchronized void deleteAlbum(int album) {
		final String DELETEALBUM_1 = SQL_SELECT + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + SQL_FROM
				+ GROUPS_DATABASE + SQL_WHERE + GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + '=';
		final String DELETEALBUM_2 = SQL_SELECT + CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + SQL_FROM
				+ CONNECTION_DATABASE + SQL_WHERE + CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		final String DELETEALBUM_3 = SQL_DELETE + SQL_FROM + GROUPS_DATABASE + SQL_WHERE
				+ GROUPS_DATABASE_STRUCTURE[GROUPS_GROUPID][0] + '=';
		final String DELETEALBUM_4 = " OR " + GROUPS_DATABASE_STRUCTURE[GROUPS_ALBUMID][0] + '=';
		final String DELETEALBUM_5 = SQL_DELETE + SQL_FROM + ALBUM_DATABASE + SQL_WHERE
				+ ALBUM_DATABASE_STRUCTURE[ALBUM_ALBUMID][0] + '=';
		final String DELETEALBUM_6 = SQL_DELETE + SQL_FROM + CONNECTION_DATABASE + SQL_WHERE
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(DELETEALBUM_1 + album);
			while (rs.next()) {
				deleteAlbum(rs.getInt(1));
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query " + DELETEALBUM_1 + album);
		} finally {
			if (rs != null)
				try {
					rs.close();
				} catch (SQLException ex) {
				}
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ex) {
				}
		}
		stmt = null;
		rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(DELETEALBUM_2 + album);
			while (rs.next()) {
				deletePicture(rs.getInt(1));
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query " + DELETEALBUM_2 + album);
		} finally {
			if (rs != null)
				try {
					rs.close();
				} catch (SQLException ex) {
				}
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ex) {
				}
		}
		stmt = null;
		try {
			stmt = getAvailableConnection().createStatement();
			stmt.executeUpdate(DELETEALBUM_3 + album + DELETEALBUM_4 + album);
			stmt.executeUpdate(DELETEALBUM_5 + album);
			stmt.executeUpdate(DELETEALBUM_6 + album);
			stmt.close();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For queries " + DELETEALBUM_3 + album + DELETEALBUM_4 + album + '\n' + DELETEALBUM_5
					+ album + '\n' + DELETEALBUM_6 + album);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ex) {
				}
		}
	}

	public synchronized void deletePicture(int album, String name) {
		final String DELETEPICTURE_1 = SQL_SELECT + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[PICTURES_PICID][0] + SQL_FROM + CONNECTION_DATABASE + ','
				+ PICTURES_DATABASE + SQL_WHERE + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=' + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + " AND " + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		final String DELETEPICTURE_2 = " AND " + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + "=";

		final String DELETEPICTURE_3 = SQL_DELETE + SQL_FROM + CONNECTION_DATABASE + SQL_WHERE
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_PICID][0] + '=';
		final String DELETEPICTURE_4 = " AND " + CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		Statement stmt = null;
		ResultSet rs = null;
		Statement stmt2 = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(DELETEPICTURE_1 + album + DELETEPICTURE_2 + SQLQuote(name));
			if (rs.next()) { // should be one pair only
				stmt2 = getAvailableConnection().createStatement();
				stmt.executeUpdate(DELETEPICTURE_3 + rs.getInt(1) + DELETEPICTURE_4 + album);
				stmt2.close();
				stmt2 = null;
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For queries " + DELETEPICTURE_1 + album + DELETEPICTURE_2 + name + "'\n"
					+ DELETEPICTURE_3 + '?' + DELETEPICTURE_4 + album);
			ex.printStackTrace();
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

	public synchronized void deletePicture(int picture) {
		final String DELETEPICTURE_5 = SQL_DELETE + SQL_FROM + PICTURES_DATABASE + SQL_WHERE
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=';
		Statement stmt = null;
		try {
			stmt = getAvailableConnection().createStatement();
			stmt.executeUpdate(DELETEPICTURE_5 + picture);
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For queries " + DELETEPICTURE_5 + picture);
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	boolean useEncoding() {
		return IniPrefs.getInt(controller.getPrefs().getProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.USEENCODING),
				0) == 1;
	}

	public synchronized void setPictureComment(int picture, String comment) {
		final String SETPICTURECOMMENT_1 = SQL_UPDATE + PICTURES_DATABASE + SQL_SET
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_COMMENTARY][0] + "=? " + SQL_WHERE
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=';
		PreparedStatement pstmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(SETPICTURECOMMENT_1 + picture);
			pstmt.setString(1, convertUsingEnc(comment, true, null));
			pstmt.executeUpdate();

		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query: " + SETPICTURECOMMENT_1 + picture);
		} finally {
			try {
				pstmt.close();
			} catch (Exception e) {
			}
		}
	}

	public synchronized void setPictureComment(int album, String pic_loc, String comment) {
		final String SETPICTURECOMMENT_2 = SQL_UPDATE + CONNECTION_DATABASE + ',' + PICTURES_DATABASE + SQL_SET
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_COMMENTARY][0] + "=? " + SQL_WHERE + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=' + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + " AND " + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		final String SETPICTURECOMMENT_3 = " AND " + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + "=?";
		PreparedStatement pstmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(SETPICTURECOMMENT_2 + album + SETPICTURECOMMENT_3);
			pstmt.setString(1, convertUsingEnc(comment, true, null));
			pstmt.setString(2, pic_loc);
			pstmt.executeUpdate();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query: " + SETPICTURECOMMENT_2 + album + SETPICTURECOMMENT_3 + pic_loc + '\'');
		} finally {
			try {
				pstmt.close();
			} catch (Exception e) {
			}
		}
	}

	String convertUsingEnc(String _s, boolean _encode, String _charset) {
		if (useEncoding()) {
			if (_charset == null)
				_charset = MiscellaneousOptionsTab.getEncoding(controller);
			String res = null;
			if (_encode) {
				try {
					res = new String(_s.getBytes(_charset), DIR_ENC);
				} catch (UnsupportedEncodingException uee) {
				} catch (NullPointerException npe) {
				}
			} else {
				try {
					res = new String(_s.getBytes(DIR_ENC), _charset);
				} catch (UnsupportedEncodingException uee) {
				} catch (NullPointerException npe) {
				}
			}
			return res != null ? res : _s;
		} else
			return _s;
	}

	public synchronized void renamePictureTo(int album, String pic_loc, String new_loc) {
		final String RENAMEPICTURETO_2 = SQL_UPDATE + CONNECTION_DATABASE + ',' + PICTURES_DATABASE + SQL_SET
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + "=? " + SQL_WHERE + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=' + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + " AND " + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		final String RENAMEPICTURETO_3 = " AND " + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + "=";
		PreparedStatement pstmt = null;
		try {
			pstmt = getAvailableConnection().prepareStatement(
					RENAMEPICTURETO_2 + album + RENAMEPICTURETO_3 + SQLQuote(pic_loc));
			pstmt.setString(1, new_loc);
			pstmt.executeUpdate();
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For query: " + RENAMEPICTURETO_2 + album + RENAMEPICTURETO_3 + pic_loc + "'(?)"
					+ new_loc);
		} finally {
			try {
				pstmt.close();
			} catch (Exception e) {
			}
		}
	}

	public synchronized String getPictureComment(int album, String pic_loc) {
		final String GETPICTURECOMMENT_1 = SQL_SELECT + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_COMMENTARY][0] + SQL_FROM + CONNECTION_DATABASE + ','
				+ PICTURES_DATABASE + SQL_WHERE + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[PICTURES_PICID][0] + '=' + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_PICID][0] + " AND " + CONNECTION_DATABASE + '.'
				+ CONNECTION_DATABASE_STRUCTURE[CONNECTION_ALBUMID][0] + '=';
		final String GETPICTURECOMMENT_2 = " AND " + PICTURES_DATABASE + '.'
				+ PICTURES_DATABASE_STRUCTURE[PICTURES_NAME][0] + "=";
		String result = null;
		if (album < 0)
			return result;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAvailableConnection().createStatement();
			rs = stmt.executeQuery(GETPICTURECOMMENT_1 + album + GETPICTURECOMMENT_2 + SQLQuote(pic_loc));
			if (rs.next()) { // should be one pair only
				result = rs.getString(1);
				if (result != null)
					result = result.trim();
			}
		} catch (NullPointerException ex) {
		} catch (SQLException ex) {
			printChainedSqlException(ex);
			System.err.println("For queries " + GETPICTURECOMMENT_1 + album + GETPICTURECOMMENT_2 + pic_loc + '\'');
			ex.printStackTrace();
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
		return convertUsingEnc(result, false, null);
	}

	public Connection getAvailableConnection() throws SQLException {
		// support only one connection, b'cause no method release the connection
		if (connection == null || connection.isClosed())
			init();
		else
			try {
				if (connection.isValid(1 * 1000) == false)
					init();
			} catch (java.lang.UnsupportedOperationException ue) {
				System.err.printf("Older driver is used %s%n", ue);
			}
		if (connection == null)
			throw new SQLException("No suitable JDBC driver, or JDBC settings set not properly");
		return connection;
	}

	public void closeAllConnections() {
		try {
			if (connection != null) {
				connection.commit();
				connection.close();
				connection = null;
			}
		} catch (SQLException ex) {
			printChainedSqlException(ex);
		}
	}

	protected void finalize() throws Throwable {
		closeAllConnections();
		super.finalize();
	}

	public static String SQLQuote(String s) {
		StringBuffer res = new StringBuffer(s.length() + 2);
		char sa[] = s.toCharArray();

		res.append('\'');
		int i = 0, p0 = 0;
		for (; i < sa.length; i++) {
			if (sa[i] == '\'') {
				res.append(sa, p0, i - p0 + 1).append(sa[i]);
				p0 = i + 1;
			}
		}
		if (p0 < i)
			res.append(sa, p0, i - p0);

		res.append('\'');

		return res.toString();
	}

	public static void printChainedSqlException(SQLException ex) {
		System.err.println("*** SQLException caught ***");
		while (ex != null) {
			System.err.println("state:      " + ex.getSQLState());
			System.err.println("message:    " + ex.getMessage());
			System.err.println("error code: " + ex.getErrorCode());
			ex.printStackTrace();
			if ((ex = ex.getNextException()) != null)
				System.err.println("ooooo>>>");

		}
		System.err.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
	}

	private static boolean checkForWarning(SQLWarning warn) throws SQLException {
		if (warn != null) {
			System.err.println("*** SQLWarning ***");
			while (warn != null) {
				System.err.println("state:      " + warn.getSQLState());
				System.err.println("message:    " + warn.getMessage());
				System.err.println("error code: " + warn.getErrorCode());
				warn = warn.getNextWarning();
				if (warn != null)
					System.err.println("ooooo>>>");
			}
			System.err.println("********===********");
			return true;
		}
		return false;
	}

	protected Controller controller;

	private Connection connection;
}
