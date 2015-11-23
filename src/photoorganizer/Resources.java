/* MediaChest $RCSfile: Resources.java,v $
 * Copyright (C) 1999-2004 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: Resources.java,v 1.167 2015/08/12 07:10:20 cvs Exp $
 */
package photoorganizer;

import javax.swing.Icon;


public interface Resources {
	// menu constraints
	public final static String MENU_PROPERTIES = "Properties...";

	public final static String MENU_EDIT_PROPS = "Edit properties...";

	public final static String MENU_DRIVE_SEL = "Change Drive";

	public final static String MENU_FILE = "File";

	public final static String MENU_EXIT = "Exit";

	public final static String MENU_TOOLS = "Tools";

	public final static String MENU_ADDTOCOLLECT = "To Selection";

	public final static String MENU_CF_TOCOLLECT = "Load Pictures";

	public final static String MENU_IPOD_SYNC = "Sync iPod";

	public final static String MENU_IPOD_WIPE = "Wipe iPod";

	public final static String MENU_ADDTOALBUM = "To Album";

	public final static String MENU_MOVETOALBUM = "Move to...";

	public final static String MENU_TOPLAYLIST = "To Play List ...";

	public final static String MENU_HELP = "Help";

	public final static String MENU_VIEW = "View";

	public final static String MENU_CONTENTS = "Contents...";

	public final static String MENU_TRANSFORM = "Transformation";

	public final static String MENU_NONE = "None";

	public final static String MENU_ROTATE90 = "Rotate 90";

	public final static String MENU_ROTATE180 = "Rotate 180";

	public final static String MENU_ROTATE270 = "Rotate 270";

	public final static String MENU_FLIP_H = "Horizontal Flip";

	public final static String MENU_FLIP_V = "Vertical Flip";

	public final static String MENU_TRANSPOSE = "Transpose";

	public final static String MENU_TRANSVERSE = "Transverse";

	public final static String MENU_ABOUT = "About...";

	public final static String MENU_REGISTER = "Register...";

	public final static String MENU_SHOW = "Show/Play";

	public final static String MENU_SELECT = "Select";

	public final static String MENU_NEW_FOLDER = "New Folder";

	public final static String MENU_REVERSE_SELECT = "Invert Selection";

	public final static String MENU_SELECTALL = "Select All";

	public final static String MENU_DELETE = "Delete";

	public final static String MENU_DELETE_INLIST = "Delete from List";

	public final static String MENU_DELETE_COMPLETELY = "Delete All";

	public final static String MENU_STARTOVER = "Start Over";

	public final static String MENU_RENAME = "Rename";

	public final static String MENU_EXTRACTTUMBNAILS = "Extract Thumbs";

	public final static String MENU_EXTRACTMARKERS = "Extract Markers";

	public final static String MENU_SEND_MAIL = "Send E-mail";

	public final static String MENU_POST_ARTICLE = "Post Article";

	public final static String MENU_PUBLISH_OVERFTP = "Over FTP";

	public final static String MENU_PUBLISH_OVERHTTP = "Over HTTP";

	public final static String MENU_PUBLISH_LOCALY = "Locally";

	public final static String MENU_UPLOADIMAGE = "Upload to a Print shop";

	public final static String MENU_POPUPWIN = "PopUp";

	public final static String MENU_PUBLISH = "Publish";

	public final static String MENU_PUBLISH_CURRENT = "This Album";

	public final static String MENU_PUBLISH_ALL = "All Albums";

	public final static String MENU_INTWIN = "GetBack";

	public final static String MENU_FULLSCREEN = "FullScreen";

	public final static String MENU_PRINT = "Print";

	public final static String MENU_VIEW_HTML = "View HTML";

	public final static String MENU_STITCH = "Stitch";

	public final static String MENU_COMMENT = "Annotation";

	public final static String MENU_SENDTO = "Send by E-mail";

	public final static String MENU_COPY_MOVE = "Copy/move";

	public final static String MENU_COPY_PLAYLIST = "Copy list";

	public final static String MENU_EXPORT = "Export";

	public final static String MENU_EXPORTTOCSV = "To CSV...";

	public final static String MENU_EXPORTTOWPL = "To WPL...";

	public final static String MENU_EXPORTTOXML = "To XML...";

	public final static String MENU_EXPORTTODSK = "To Disk...";

	public final static String MENU_EXPORTTOHTML = "To HTML...";

	public final static String MENU_IMPORT = "Import";

	public final static String MENU_IMPORTCSV = "From CSV...";

	public final static String MENU_IMPORTXML = "From XML...";

	public final static String MENU_IMPORTDSK = "From Disk...";

	public final static String MENU_OPTIONS = "Options...";

	public final static String MENU_REFRESH = "Refresh";

	public final static String MENU_NEXT = "Next";

	public final static String MENU_PREVIOUS = "Prev";

	public final static String MENU_SLIDESHOW = "Slide show";

	public final static String MENU_MOVE_ALBUM = "Move Album {0} under {1}";

	public final static String MENU_COPY_ALBUM = "Copy Album {0} under {1}";

	public final static String MENU_PLAY_LIST = "Show/play List";

	public final static String MENU_TORIPPER = "To Ripper";

	public final static String MENU_NEW_LAYOUT = "New Layout";

	public final static String MENU_RECORD_DISK = "Prepare Disk";

	public final static String MENU_LOAD_LAYOUT = "Load Layout";

	public final static String MENU_SAVE_LAYOUT = "Save Layout";

	public final static String MENU_PAGE_SETUP = "Page Setup...";

	public final static String MENU_PAGE_LAYOUT = "Page Layout...";

	public final static String MENU_ADDTO_IPOD = "To iPod";

	// public final static String MENU_ADDTO_IPOD_WO = "To iPod with options";
	public final static String MENU_RESTORE_IPOD = "Rebuild database from iPod";

	public final static String MENU_CREATE_PLAYLIST = "Create Play List";

	public final static String MENU_CREATE_MAGICPL = "Create Magic Play List...";

	public final static String MENU_EDIT_MAGICPL = "Edit Magic Play List...";

	public final static String MENU_DELETE_PLAYLIST = "Delete Playlist";

	public final static String MENU_MERGE_PLAYLIST = "Merge Playlists";

	public final static String MENU_MAKEITALL = "View/Edit info...";

	public final static String MENU_ENCODING = "Encoding...";

	public final static String MENU_UNDO = "Undo";

	public final static String MENU_FIND = "Find...";

	public final static String MENU_ADDRESSBOOK = "Addressbook...";

	public final static String MENU_RENAME_PLAYLIST = "Rename Play List";

	public final static String MENU_CHANGE_ROOT_DIR = "Change root directory...";

	public final static String MENU_FITTOSIZE = "Fit to size";

	public final static String MENU_ORIGSIZE = "Original size";

	public final static String MENU_CANCEL = "Cancel";
	
	public static final String MENU_COPY_LOCATION = "Copy Location";

	public static final String MENU_CROP = "Crop";

	public static final String MENU_COPY = "Copy";

	public static final String MENU_RESTORE = "Restore";

	public static final String[] PUBLISH_ITEMS = { MENU_PUBLISH_OVERFTP, MENU_SEND_MAIL, MENU_POST_ARTICLE,
			MENU_PUBLISH_LOCALY, MENU_PUBLISH_OVERHTTP };

	public static final String MSG_ADDED_CREATED_DATE = "Date Added {0,date,short} Date Created {1,date,short}";

	public static final String MSG_LAST_PLAYED_COUNT = "<html>Last Time Played {0,date,short} <b>{0,time,short}</b> Play Count {1,number,integer}";
	
	public static final String MSG_SRCH_BOX = "type something here";

	public final static String LIST_EMPTY = "";

	public final static String LIST_DATETIME = "DateTime";

	public final static String LIST_DATE = "Date";

	public final static String LIST_TIME = "Time";

	public final static String LIST_SHUTTER = "Shutter";

	public final static String LIST_APERTURE = "Aperture";

	public final static String LIST_FLASH = "Flash";

	public final static String LIST_QUALITY = "Quality";

	public final static String LIST_COUNTER = "Counter";

	public final static String LIST_ORIG = "OriginalName";

	public final static String LIST_ORIENTATION = "Orientation";

	public final static String LIST_ZOOM = "Zoom";

	public final static String LIST_FILESIZE = "Size";

	public final static String LIST_MAKE = "Make";

	public final static String LIST_MODEL = "Model";

	public final static String LIST_METERING = "Metering mode";

	public final static String LIST_MODE = "Exposure program";

	public final static String LIST_ONTOP = "On top";

	public final static String LIST_ONBOTTOM = "On bottom";

	public final static String LIST_ONLEFT = "On left";

	public final static String LIST_ONRIGHT = "On right";

	public final static String LIST_ALBUM = "Album";

	public final static String LIST_BAND = "Band";

	public final static String LIST_COMMENT = "Comment";

	public final static String LIST_LANGUAGE = "Language";

	public final static String LIST_GENRE = "Genre";

	public final static String LIST_GROUPING = "Grouping";

	public final static String LIST_LENGTH = "Length";

	public final static String LIST_TRACK = "Track";

	public final static String LIST_COMPOSER = "Composer";

	public final static String LIST_CONDUCTOR = "Conductor";

	public final static String LIST_BITRATE = "Bitrate";

	public final static String LIST_SAMPLERATE = "Sample Rate";

	public final static String LIST_PROTECTED = "Protected";

	public final static String LIST_PLAYMODE = "Play mode";

	public final static String LIST_ARTIST = "Artist";
	
	public final static String LIST_ALBUMARTIST = "AlbumArtist";

	public final static String LIST_TITLE = "Title";

	public final static String LIST_YEAR = "Year";

	public final static String LIST_RATING = "Rating";

	public final static String LIST_EXTENSION = "Type Ext";

	public final static String LIST_FILERULE = "FileRule";

	public final static String LIST_NOTINUSE = "Not used";

	// Caution on localization list lables, some of them could be method names
	public final static String LIST_NAME = "Name";

	public final static String LIST_TYPE = "Type";

	public final static String LIST_ATTRS = "Attributes";

	public final static String LIST_AL_LEFT = "Left";

	public final static String LIST_AL_RIGHT = "Right";

	public final static String LIST_AL_CENTER = "Center";

	public final static String LIST_IPOD_LEFT_UNATTENDED = "None";

	public final static String LIST_IPOD_ADD_DATABASE = "Add records for unlisted files in db";

	public final static String LIST_IPOD_DELETE_DISK = "Delete unlisted files from iPod";

	public final static String LIST_IPOD_DELETE_RECORD = "Delete db records without files";

	public final static String LIST_NOTRATED = "Not rated";

	public final static String LIST_DISK = "Disk";

	public final static String LIST_DAYS = "days";

	public final static String LIST_WEEKS = "weeks";

	public final static String LIST_MONTHS = "months";

	public final static String LIST_SEL_HST_RAT = "highest rating";

	public final static String LIST_SEL_LWT_RAT = "lowest rating";

	public final static String LIST_SEL_RANDOM = "random";

	public final static String LIST_SEL_MST_RCT_PL = "most recently played";

	public final static String LIST_SEL_LST_RCT_PL = "least recently played";

	public final static String LIST_SEL_MST_OFT_PL = "most often played";

	public final static String LIST_SEL_LST_OFT_PL = "least often played";

	public final static String LIST_SEL_MST_RCT_ADD = "most recently added";

	public final static String LIST_SEL_LST_RCT_ADD = "least recently added";

	public final static String LIST_MINUTES = "minutes";

	public final static String LIST_HOURS = "hours";

	public final static String LIST_MB = "MB";

	public final static String LIST_GB = "GB";

	public final static String LIST_SONGS = "songs";

	public final static String LIST_YES = "Yes";

	public final static String LIST_NO = "No";

	public final static String LIST_NOTSET = "Not set";

	public final static String LIST_SONG_NAME = "Song Name";

	// public final static String LIST_ALBUM = "Album";
	// public final static String LIST_ARTIST = "Artist";
	// public final static String LIST_BITRATE = "";
	public final static String LIST_SAMPLING_RATE = LIST_SAMPLERATE;// "Sample
																	// Rate";

	// public final static String LIST_YEAR = "Year";
	public final static String LIST_KIND = "Kind";

	public final static String LIST_LAST_MODIFY = "Date Modified";

	public final static String LIST_TRACKNUMBER = "Track Number";

	public final static String LIST_SIZE = "Size";

	// public final static String LIST_TIME = "Time";
	// public final static String LIST_COMMENT = "";
	public final static String LIST_TIME_ADDED = "Date Added";

	public final static String LIST_TIME_CREATED = "Date Created";

	public final static String LIST_PLAYCOUNT = "Play Count";

	public final static String LIST_LAST_PLAYED = "Last Played";

	// public final static String LIST_RATING = "";
	public final static String LIST_COMPILATION = "Compilation";

	public final static String LIST_START_TIME = "Start time";

	public final static String LIST_BPM = "BPM";

	public final static String LIST_EQUALISATION = "Equalisation";

	public final static String LIST_RELATIVEVOLUMENADJUSTMENT = "Volume Adj.";

	public final static String LIST_DISC_NUM = "Disc Number";

	public final static String LIST_IS = "is";

	public final static String LIST_IS_NOT = "is not";

	public final static String LIST_IS_AFTER = "is after";

	public final static String LIST_IS_BEFORE = "is before";

	public final static String LIST_IS_LAST = "is in the last";

	public final static String LIST_IS_NOT_LAST = "is not in the last";

	public final static String LIST_IS_IN_RANGE = "is in the range";

	public final static String LIST_CONTAINS = "contains";

	public final static String LIST_NOT_CONTAIN = "does not contain";

	public final static String LIST_ST_WITH = "starts with";

	public final static String LIST_EN_WITH = "ends with";

	public final static String LIST_IS_GT = "is greater than";

	public final static String LIST_IS_LS = "is less than";

	public final static String LIST_IS_SET = "is set";

	public final static String LIST_ISN_SET = "is not set";

	public final static String LIST_ALL = "all";

	public final static String LIST_ANY = "any";

	public final static String LIST_YEAR_FIRST = "Last recent year";

	public final static String LIST_YEAR_LAST = "Most recent year";

	public final static String LIST_ACOUSTIC = "Acoustic";

	public final static String LIST_BASS_BOOSTER = "Bass Booster";

	public final static String LIST_BASS_REDUCER = "Bass Reducer";

	public final static String LIST_CLASSICAL = "Classical";

	public final static String LIST_DANCE = "Dance";

	public final static String LIST_DEEP = "Deep";

	public final static String LIST_ELECTRONIC = "Electronic";

	public final static String LIST_FLAT = "Flat";

	public final static String LIST_HIP_HOP = "Hip-Hop";

	public final static String LIST_JAZZ = "Jazz";

	public final static String LIST_LATIN = "Latin";

	public final static String LIST_LOUDENESS = "Loudeness";

	public final static String LIST_LAUNGE = "Launge";

	public final static String LIST_PIANO = "Piano";

	public final static String LIST_POP = "Pop";

	public final static String LIST_R_N_B = "R&B";

	public final static String LIST_ROCK = "Rock";

	public final static String LIST_SMALL_SPEAKERS = "Small Speakers";

	public final static String LIST_SPOKEN_WORD = "Spoken Word";

	public final static String LIST_TREBLE_BOOSTER = "Treble Booster";

	public final static String LIST_TREBLE_REDUCER = "Treble Reducer";

	public final static String LIST_VOCAL_BOOSTER = "Vocal Booster";

	// on-the-go
	public final static String LIST_IPOD_OTG_IGNORE = "Ignore OTG playlists";

	public final static String LIST_IPOD_OTG_NEWNAME = "Create OTG as playlist";

	public final static String LIST_IPOD_OTG_SAMENAME = "Append to previous OTG";

	public final static String LIST_IPOD_OTG_VIEWONLY = "Do not save OTG";

	public static final String LIST_IPOD_RECOV_FIX_ARTWORK = "Fix artwork from tags";

	public final static String LIST_IPOD_SRCH_ALL = "All";

	public final static String LIST_IPOD_SRCH_TITLE = "Title";

	public final static DimensionS LIST_SIZE_640X480 = new DimensionS(640, 480);

	public final static DimensionS LIST_SIZE_800X600 = new DimensionS(800, 600);

	public final static DimensionS LIST_SIZE_1024X768 = new DimensionS(1024, 768);

	public final static DimensionS LIST_SIZE_1600X1200 = new DimensionS(1600, 1200);

	public final static DimensionS LIST_SIZE_ORIGINAL = new DimensionS(0, 0) {
		public String toString() {
			return "No resize";
		}
	};

	public final static DimensionS LIST_SIZES[] = { LIST_SIZE_ORIGINAL, LIST_SIZE_640X480, LIST_SIZE_800X600,
			LIST_SIZE_1024X768, LIST_SIZE_1600X1200 };

	public final static String NAV_LEFT = "left";

	public final static String NAV_RIGHT = "right";

	public final static String NAV_UP = "up";

	public final static String NAV_DOWN = "down";

	public final static String NAV_COLAPSE = "colapse";

	public final static String NAV_OPEN = "open";

	public final static String NAV_SELECT = "select";

	public final static String NAV_ACT = "act";

	// TODO: reconsider as array of specific mask classes
	public final static String[][] MASKS = new String[][] {
			{ "%D", "%d", "%t", "%s", "%a", "%f", "%q", "%c", "%o", "%r", "%z", "%S", "%M", "%m", "%x", "%X", "%A",
					"%b", "%C", "%L", "%g", "%l", "%T", "%e", "%E", "%n" },
			{ LIST_DATETIME, LIST_DATE, LIST_TIME, LIST_SHUTTER + '/' + LIST_BITRATE,
					LIST_APERTURE + '/' + LIST_SAMPLERATE, LIST_FLASH + '/' + LIST_PROTECTED,
					LIST_QUALITY + '/' + LIST_PLAYMODE, LIST_COUNTER, LIST_ORIG, LIST_ORIENTATION,
					LIST_ZOOM + '/' + LIST_YEAR, LIST_FILESIZE, LIST_MAKE + '/' + LIST_ARTIST,
					LIST_MODEL + '/' + LIST_TITLE, LIST_METERING, LIST_MODE, LIST_ALBUM, LIST_BAND, LIST_COMMENT,
					LIST_LANGUAGE, LIST_GENRE, LIST_LENGTH, LIST_TRACK, LIST_COMPOSER, LIST_CONDUCTOR, LIST_EXTENSION } };

	public final static String LIST_DEFAULT = "Default";

	public final static String LIST_NONE = "None";

	public final static String LIST_EXIF = "Exif";

	public final static String LIST_CIFF = "CIFF";

	public final static String LIST_JFIF = "JFXX";

	public final static String LIST_EXTRN = "From ext file";

	public final static String[] FORMATS = new String[] { LIST_NONE, LIST_EXIF, LIST_CIFF, LIST_JFIF, LIST_EXTRN };

	public final static String[] POSITIONS = new String[] { LIST_ONTOP, LIST_ONBOTTOM, LIST_ONLEFT, LIST_ONRIGHT };

	public final static String[] ROTATIONS = new String[] { "None", "HFlip", "VFlip", "Transpose", "Transverse",
			"Rot90", "Rot180", "Rot270", "Crp", "Cmt" };

	public final static String[] HORIZ_ALIGN = new String[] { LIST_AL_LEFT, LIST_AL_CENTER, LIST_AL_RIGHT };

	public final static String[] FILE_ATTRIBUTES = new String[] { LIST_EMPTY, LIST_NAME, LIST_TYPE, LIST_LENGTH,
			LIST_DATE, LIST_ATTRS };

	public final static String BORDER_BEVELLOWERED = "BevelLowered";

	public final static String BORDER_BEVELRAISED = "BevelRaised";

	public final static String BORDER_ETCHED = "Etched";

	public final static String BORDER_LINE = "Line";

	public final static String[] BORDERS = new String[] { "None", BORDER_BEVELLOWERED, BORDER_BEVELRAISED,
			BORDER_ETCHED, BORDER_LINE };

	public final static String[] HTTP_AUTHS = new String[] { "No authentication", "Login/Cookie", "Login/UID", "Login/Google auth" };

	public final static String[] HTTP_LOGIN_METHODS = new String[] { "Get", "Post" };

	public final static String[] INDEX_STYLES = new String[] { "None", "Regular", "Left side theme", "Background theme" };

	public static final int NONE_STYLE_INDEX_INDEX = 0;

	public static final int LEFT_STYLE_INDEX_INDEX = 2;

	public static final int BACKGROUND_STYLE_INDEX_INDEX = 3;

	public final static String[] POST_TYPES = new String[] { "All in one post", "Flood" };

	public static final int FLOOD_POST = 1;

	public final static String[] CHARSETS = { "Windows-1251", "Koi8-r" };

	public final static String LABEL_NAMEGENMASK = "Target name generation mask";

	public final static String LABEL_INSERT = "Insert";

	public final static String LABEL_INS_R = "<";

	public final static String LABEL_INS_L = ">";

	public final static String LABEL_KEEP_APPS = "Preserve APPs";

	public final static String LABEL_KEEP_ORIG = "Keep original";

	public final static String LABEL_ENFORCE_FMT = "Enforce format";

	public final static String LABEL_DESTINATIONDIR = "Destination folder for renaming and transformation";

	public final static String LABEL_CHOOSE_DIR = "Choose a folder";

	public final static String LABEL_DIRECTORIES = "Folders";

	public final static String LABEL_WEB_ROOT = "Web root";

	public final static String LABEL_HTML_TEMPLATE = "HTML template";

	public final static String LABEL_HTML_FILES = "HTML files";

	public final static String LABEL_DIRECTORY = "Dir";

	public final static String LABEL_CF_DIR = "Flash/Camera access";

	public final static String LABEL_FILE = "File";

	public final static String LABEL_NUM_ = "Number of ";

	public final static String LABEL_COL = "columns";

	public final static String LABEL_ROW = "rows";

	public final static String LABEL_COPY = MENU_COPY;

	public final static String LABEL_SCROLL_VERT = "Scroll vertically";

	public final static String LABEL_THUMBNAILFILE_MASK = "Thumbnail file mask";

	public final static String LABEL_THUMBNAILSIZE = "Thumbnail";

	public final static String LABEL_EDIT_NEW_NAME = "Edit name before";

	public final static String LABEL_LABEL_MASK = "Label mask";

	public final static String LABEL_TTIP_MASK = "Tooltip mask";

	public final static String LABEL_WIDTH_SHORT = "width:";

	public final static String LABEL_HEIGHT_SHORT = "height:";

	public final static String LABEL_NEW_NAME = "New name";

	public final static String LABEL_CONFIRM_DEL = "- will be removed permanently.\nAre you sure?";

	public final static String LABEL_CONFIRM_WIPE_IPOD = "All iPod data files will be removed permanently.\nAre you sure?";

	public final static String LABEL_CONFIRM_OVERWRITE = "- already exists.\nAre you sure to overwrite it?";

	public final static String LABEL_CONFIRM_UPGRADE = "A newer version of the program is available.\nWould you like to download?";
	
	public final static String LABEL_CONFIRM_ALBUM_MVCP = "Are you sure to %s the album to %s?";

	public final static String LABEL_CHOOSE_DRIVE = "Choose drive";

	public final static String LABEL_START_COUNTER = "Initial counter";

	public final static String LABEL_MENU = "Menu";

	public final static String LABEL_IMPORT = "Import";// = MENU_IMPORT

	public final static String LABEL_TOOLBAR = "ToolBar";

	public final static String LABEL_SATUSBAR = "StatusBar";

	public final static String LABEL_TABPOS = "Tab position";

	public final static String LABEL_DATEFMT = "Date format";

	public final static String LABEL_TIMEFMT = "Time format";

	public final static String LABEL_TRANS_CODE = "Transform code";

	public final static String LABEL_NOTUMBNAIL = "No thumbnail for ";

	public final static String LABEL_HTML_NAME = "Name of HTML file";

	public final static String LABEL_ALBUM_NAME = "Name of album add to";

	public final static String LABEL_ACTOR_EXIF = "Populate owner as artist";

	public final static String LABEL_MARKERS = "Markers";

	public final static String LABEL_NOJDBCWARN = "No warn if JDBC failed";

	public final static String LABEL_JDBCDRV_CLASS = "JDBC driver class name (e.g. sun.jdbc.odbc.JdbcOdbcDriver)";

	public final static String LABEL_PASSWORD = "Password";

	public final static String LABEL_USER = "User";

	public final static String LABEL_HTTP_AUTH = "Authentication";

	public final static String LABEL_HTTP_LOGIN_URL = "Login URL";

	public final static String LABEL_HTTP_LOGIN_METHOD = "Login method";

	public final static String LABEL_STATIC_QUERY = "Additional param=value&...";

	public final static String LABEL_DATABASE_URL = "Database URL";

	public final static String LABEL_FTP_HOST = "Ftp host";

	public final static String LABEL_LOGIN = "Login";

	public final static String LABEL_ALBUMNAME = "Name of album name";

	public final static String LABEL_ALBUM_ID = "Name of album Id";

	public final static String LABEL_SAVE_PSWD = "Save password";

	public final static String LABEL_CONN_TIMEOUT = "Connection timeout";

	public final static String LABEL_USE_PROXY = "Use proxy";

	public final static String LABEL_HOST = "Host";

	public final static String LABEL_PORT = "Port";

	public final static String LABEL_USE_FTP = "Ftp";

	public final static String LABEL_USE = "Use:";

	public final static String LABEL_NAME_OF_ALBUM = "Name of the album";

	public final static String LABEL_ALBUMROOT = "Albums";

	public final static String LABEL_INCLUSIVE = "add to selcted albums";

	public final static String LABEL_SHOWWARNDLG = "Confirmation dialogs";

	public final static String LABEL_ERR_FTP_LOGIN = "Ftp login's been failed, 'cause\n";

	public final static String LABEL_ERR_JDBCDRV_LOAD = "JDBC driver can't be instantiated, 'cause\n";

	public final static String LABEL_ERR_JDBC_CONN = "Couldn't establish database connection, check URL\n";

	public final static String LABEL_ERR_FTP_CONNECT = "Ftp connection failed, check host name and/or proxy settings.";

	public final static String LABEL_ERR_MAIL_SEND = "E-Mail sending failed, check e-mail settings.";

	public final static String LABEL_ERR_COPYALBUM = "The album can't be moved/copied, because it includes target album(s).";

	public final static String LABEL_ERR_NO_ALBUM_SELECTION = "No target album selected for import.";

	public final static String LABEL_COFIRMDROPTABLE = "All albums will be destroyed, please confirm.";

	public final static String LABEL_COFIRMDELETE = "Confirm deletion ";

	public final static String LABEL_ADDTRANSFORMEDTOSEL = "Add transformed to selection";

	public final static String LABEL_REMOVE_AFTER_REN = "Remove from selection";

	public final static String LABEL_USE_DIRALBUM = "Maintain albums folder";
	
	public final static String LABEL_OLD_DIRALBUM = "Old albums folder";

	public final static String LABEL_MOVETO_ALBUM = "Move picture to album folder";

	public final static String LABEL_COMMENT = "Type a commentary";

	public final static String LABEL_LOCALE = "Locale";

	public final static String LABEL_SPLIT_VERT = "Split vertically";

	public final static String LABEL_SPLIT_HORZ = "Split horizontally";

	public final static String LABEL_OUTOFMEMORY = "The system runs on very low free memory, the current operation will be terminated.";

	public final static String LABEL_USE_COMMENT = "Comment as label";

	public final static String LABEL_PIC_LOC = "Pictures location";

	public final static String LABEL_PIC_URL = "Pictures URL";

	public final static String LABEL_TN_LOC = "Thumbnails location";

	public final static String LABEL_USE_WEB_PIC = "Use picture URL";

	public final static String LABEL_ENC_FOR_CMT = "Use encoding for comments";

	public final static String LABEL_CPY_PIC = "Don't copy images";

	public final static String LABEL_NOTCOPY_THUMBS = "Don't copy thumbs&HTML";

	public final static String LABEL_NAME = "Name";

	public final static String LABEL_CLASS = "Class";

	public final static String LABEL_TOOLTIP = "Tool tip";

	public final static String LABEL_DISABLED = "Disabled";

	public final static String LABEL_ALL_DISABLED = "All disabled";

	public final static String LABEL_ORG = "Organization";

	public final static String LABEL_EMAILA = "E-mail address";

	public final static String LABEL_REPLYA = "Reply address";

	public final static String LABEL_SMTPSRV = "SMTP server";

	public final static String LABEL_SMTPPRT = "SMTP port";

	public final static String LABEL_NEWSSRV = "NNTP server";

	public final static String LABEL_NEWSPORT = "NNTP port";

	public final static String LABEL_NNTP_LOGIN_REQ = "The server requiers me to log on";

	public final static String LABEL_SECURE_LOGIN = "Log on using Secure Password Authentication";

	public final static String LABEL_PASSIVE_MODE = "Passive mode";

	public final static String LABEL_SSL = "SSL";

	public final static String LABEL_SRVTIMEOUT = "Server timeout";

	public final static String LABEL_NNTPSRV = "NNTP server";

	public final static String LABEL_NNTPPRT = "HHTP port";

	public final static String LABEL_MIN = "min";

	public final static String LABEL_SEC = "sec";

	public final static String LABEL_INPUT_URL = "Specify complete URL of created page";

	public final static String LABEL_SENDTO = MENU_SENDTO;

	public final static String LABEL_USE_LOCAL = "LOCAL";

	public final static String LABEL_USE_HTTP = "HTTP";

	public final static String LABEL_USE_SMTP = "SMTP";

	public final static String LABEL_USE_NNTP = "NNTP";

	public final static String LABEL_USE_XML = "XML";

	public final static String LABEL_UPLOAD_SERVLET_URL = "URL of upload servlet";

	public final static String LABEL_UPLOAD_DST_NAME = "Dest input name";

	public final static String LABEL_UPLOAD_DATA_NAME = "Data input name";

	public final static String LABEL_LOGIN_NAME = "Login input name";

	public final static String LABEL_PASSWORD_NAME = "Password input name";

	public final static String LABEL_FOR_NEXT_VER = "For next release";

	public final static String LABEL_NO_ALBUMNAME = "put album name here";

	public final static String LABEL_CURRENT_SELECTION = "Current selection";

	public final static String LABEL_BORDER = "Border";

	public final static String LABEL_PAGE_ENCODING = "Encoding";

	public final static String LABEL_SELECTEDBORDER = "Selected border";

	public final static String LABEL_TRACE_UPLOAD = "Trace upload";

	public final static String LABEL_INTRO_FRAMES = "Introductory frames number";

	public final static String LABEL_RECURSIVE_PLAYBACK = "Play recursively";

	public final static String LABEL_REQUEST_PLAYMODE = "Ask for play mode";

	public final static String LABEL_FILTERONCOPY = "Use query for copy";

	public final static String LABEL_REUSE_MPLAYER = "Open player in new window";

	public final static String LABEL_PAUSE_DURATION = "Play list pause duration (s)";

	public final static String LABEL_MP3_DISK = "MP3 disc";

	public final static String LABEL_AUDIO_CD = "Audio CD";

	public final static String LABEL_SIZE_650 = "650MB";

	public final static String LABEL_SIZE_700 = "700MB";

	public final static String LABEL_SIZE_CUSTOM = "Custom (MB)";

	public final static String LABEL_PLAY_74 = "74Min";

	public final static String LABEL_PLAY_80 = "80Min";

	public final static String LABEL_RIPPER_FOLDER = "Ripper folder";

	public final static String LABEL_SCHEMA_NAME = "Schema name";

	public final static String LABEL_INTROMODE = "Introductory mode";

	public final static String LABEL_RANDOM = "Random";

	public final static String LABEL_SHUFFLE = "Shuffle";

	public final static String LABEL_UNKNOWN = "Unknown";

	public final static String LABEL_BROWSERVIEW = "Browser view";

	public final static String LABEL_COLLECTVIEW = "Collection view";

	public final static String LABEL_IPODVIEW = "iPod view";

	public final static String LABEL_FITTOSIZE = MENU_FITTOSIZE;

	public final static String LABEL_INSTANTUPDATE = "Instant update";

	public final static String LABEL_2PANES = "2 panes switchable";

	public final static String LABEL_3PANES = "3 panes";

	public final static String LABEL_ID3V2TAG = "Id3 version 2 tag";

	public final static String LABEL_ID3UNICOD = "Unicode tag";

	public final static String LABEL_EXCLUDE = "Exclude";

	public final static String LABEL_DIR_ENCODING = "use encoding on brws";

	public final static String LABEL_JDK_UPGRADE = "requires JDK 1.4 or better.\n Please upgrade your JDK http://java.sun.com/j2se/1.4/";

	public final static String LABEL_INIT_FAILED = "Some of major components could not be initialized, watch for upgrades.";

	public final static String LABEL_RESTART_UGRADE = "The upgrade was successful.\n<html>To continue work, you <b>must</b> restart the software.\n"
			+ "Do you want to restart?\n<html><i>Note, if for some reason the application did not restart, then do it manually.</i></html>";

	public final static String LABEL_EMPTY_LAYOUT = "Empty {0} Layout";

	public final static String LABEL_LAYOUT_ = "{0} Layout - {1}/{2}/{3}";

	public final static String LABEL_DATA = "Data";

	public final static String LABEL_MUSIC = "Music";
	
	public final static String LABEL_PHOTOS = "Photos";

	public final static String LABEL_NEW_FOLDER = MENU_NEW_FOLDER;

	public final static String LABEL_NORMAL = "Normal <<";

	public final static String LABEL_LAF = "L&F";

	public final static String LABEL_XML_LAYOUT_FILES = "layout files (.xml)";

	public final static String LABEL_POSTTO = "News group(s) post to";

	public final static String LABEL_POST_INDEX = "Post index";

	public final static String LABEL_INDEX_SIZE = "Index size";

	public final static String LABEL_THEME_NAME = "Theme name";

	public final static String LABEL_POST_TYPE = "Post type";

	public final static String LABEL_ADD_SIGN = "Signature";

	public final static String LABEL_POST_TITLE = "Title";

	public final static String LABEL_INDEX = "INDEX";

	public final static String LABEL_GET_SCHEMA_NAME = "Specify schema name to save to, or hit cancel";

	public final static String LABEL_PRINT_OPTIONS = "Cropped and rotated to fit";

	public final static String LABEL_FULL_PG_FAX = "Full page fax print";

	public final static String LABEL_FULL_PG_PHOTO = "Full page photo print";

	public final static String LABEL_CONTACT_SHEET = "Contact sheet (35 prints per page)";

	public final static String LABEL_8_10_CUT = "8 x 10\" cutout print";

	public final static String LABEL_5_7_CUT = "5 x 7\" cutout prints";

	public final static String LABEL_4_6_CUT = "4 x 6\" cutout prints";

	public final static String LABEL_4_6_ALBUM = "4 x 6\" album prints";

	public final static String LABEL_35_5_CUT = "3\u00BD x 5\" cutout prints";

	public final static String LABEL_WALLET_PRINT = "Wallet prints";

	public final static String LABEL_REGISTER_NAME = "Registered to:";

	public final static String LABEL_IPOD_DEVICE = "iPod connected as:";

	// public final static String LABEL_IPOD_DEVICE = "Select iPod";
	public final static String LABEL_ALL_FILES = "All files";

	public final static String LABEL_PLAYLISTS = "Playlists";

	public final static String LABEL_ARTISTS = "Artists";

	public final static String LABEL_ALBUMS = LABEL_ALBUMROOT;

	public final static String LABEL_GENRES = "Genres";

	public final static String LABEL_COMPOSERS = "Composers";

	public final static String LABEL_PLAYLIST = "Playlist";

	public static final String LABEL_COMPILATIONS = "Compilations";

	public final static String LABEL_SMART_PLAYLIST = "Smart Playlist";

	public final static String LABEL_PLAYEDCOUNTS = "Counts";

	public final static String LABEL_RATING = "Rating";

	public final static String LABEL_YEAR = "Year";

	public final static String LABEL_INPUTPLAYLIST = "Specify play list name";

	public final static String LABEL_IPOD_TRANSLIT_CLASS = "Enter name of transliteration class";

	public final static String LABEL_SELECT_MERGE_NAME = "Selected merged name for selected items";

	public final static String LABEL_FILE_RULE = "File name rule";

	public final static String LABEL_PATH_RULE = "Path name rule";

	public final static String LABEL_OVERRIDETAG_BYRULE = "Override ID3 tag using the rule ";

	public final static String LABEL_NUM_ITEMS = "(s)";

	public final static String LABEL_IMAGE_FILES = "All image files";

	public final static String LABEL_FIND = "Search pattern";

	public final static String LABEL_ACTION_UNKNOWNFILES = "Recovery and database fix options";

	public final static String LABEL_BEFORE_EXIT = "You have active tasks or not synchronized data.\n"
			+ "Are you sure about exit?";

	public final static String LABEL_MORE_90DAYS = "more 90 days";

	public final static String LABEL_ONE_DAY = "24 hours";

	public final static String LABEL_TEN_DAYS = "Ten days";

	public final static String LABEL_ONE_MONTH = "30 days ago";

	public final static String LABEL_TWO_MONTHS = "60 days ago";

	public final static String LABEL_THREE_MONTHS = "90 days ago";

	public final static String LABEL_NEVER_PLAYED = "Never played";

	public final static String LABEL_FONT = "Font";

	public final static String LABEL_MATCH = "Match";

	public final static String LABEL_FOLLOW_COND = "of the following conditions:";

	public final static String LABEL_LIMIT_TO = "Limit to";

	public final static String LABEL_SELECTED_BY = "selected by";

	public final static String LABEL_MATCH_CHECK_SONG = "Match only checked songs";

	public final static String LABEL_LIVE_UPD = "Live updating";

	public final static String LABEL_KBPS = "kbps";

	public final static String LABEL_HZ = "hz";

	public final static String LABEL_PL_NODUP = "No duplicates in playlist";

	public final static String LABEL_SETOF = "set of";

	public final static String LABEL_STOPTIME = "Stop time";

	public final static String LABEL_CSV_FILE = "Comma separated file (.csv)";

	public final static String LABEL_CONFIRM_DELETE_PLAYLIST = "Is it OK to delete playlist?";

	public final static String LABEL_ACTION_OTG_LIST = "OTG list processing";

	public final static String LABEL_LOST_END_FOUND = "Lost & Found";

	public final static String LABEL_ENABLE = "Enable";

	public final static String LABEL_DISABLE = "Disable";

	public final static String LABEL_TOTAL = "Total progress";

	public final static String LABEL_OPERATION = "Current progress:";

	public final static String LABEL_WEBPAGE = "Web Page";

	public final static String LABEL_UPDATEID3 = "Update ID3";

	public final static String LABEL_SYNC_ID3 = "Sync iTunesDB/ID3";

	public final static String LABEL_TRANSLITERATION = "Transliteration";

	public final static String LABEL_ARTWORK = "Artwork";

	public final static String LABEL_ARTWORK_PREV = "Artwork preview";

	public final static String LABEL_ADD_RESIZED = "Add resized";

	public final static String LABEL_SSINT = "Slide show pause";

	public static final String LABEL_AUTOROTATE = "Auto rotate";
	
	public final static String LABEL_SHOW = "Show";

	public static final String LABEL_SEASON_N = "Season #";

	public static final String LABEL_AUTOINCR_TRACK = "Auto incremental track";

	public static final String LABEL_EPIZODE_ID = "Episode ID";

	public static final String LABEL_EPIZODE_N = "Episode #";

	public static final String LABEL_DB_HOME = "DB Home";

	public static final String LABEL_ARTWORK_FROM_ID3 = "Artwork as tag";

	public static final String LABEL_AWARE_COMPIL = "Aware of compilations";
	
	public static final String LABEL_SEPARATOR = "Separator";

	public final static String INFO_RENAMING = "Renaming";

	public final static String INFO_TRANSFORMING = "Transforming";

	public final static String INFO_REMOVING = "Removing";

	public final static String INFO_MOVING = "Moving";

	public final static String INFO_COPYING = "Copying";

	public final static String INFO_PRINTING = "Printing";

	public final static String INFO_CONNECTING = "Connecting";

	public final static String INFO_WEBPUBLISHING = "Internet publishing";

	public final static String INFO_ERR_WEBPUBLISHING = "Exception in Internet publishing, see log for details.";

	public final static String INFO_DELETED = "Deleted";

	public final static String INFO_READING = "Reading";

	public final static String INFO_WRITING = "Writing";

	public final static String INFO_LOSTFILE = "Looking for lost files";

	public final static String INFO_READINGARTWORK = "Artwork reading";

	public final static String INFO_WRITINGARTWORK = "Artwork writing";
	
	public final static String INFO_ERROR = "Error (%s) in processing request";

	public static final String INFO_REBULDARTWORK = "Rebuilding artwork from tags";

	public final static String HDR_NAME = "Name";

	public final static String HDR_LOCATION = "Location";

	public final static String HDR_COMMENT = "Comment";

	public final static String HDR_TAG = "Tag";

	public final static String HDR_VALUE = "Value";

	public final static String HDR_TYPE = "Type";

	public final static String HDR_SIZE = "Size";

	public final static String HDR_MODIFIED = "Modified";

	public final static String HDR_SHUTTER = "Shutter";

	public final static String HDR_APERTURE = "Aperture";

	public final static String HDR_FLASH = "Flash";

	public final static String HDR_QUALITY = "Quality";

	public final static String HDR_ZOOM = "Zoom";

	public final static String HDR_TAKEN = "Taken";

	public final static String HDR_LENGTH = "Time";

	public final static String HDR_YEAR = "Year";

	public final static String HDR_ALBUM = "Album";

	public final static String HDR_ARTIST = "Artist";

	public final static String HDR_BITRATE = "Bitrate";

	public final static String HDR_GENRE = "Genre";

	public final static String CMD_YES = "Yes";

	public final static String CMD_YES_ALL = "Yes to all";

	public final static String CMD_NO = "No";

	public final static String CMD_OK = "OK";

	// public final static String CMD_APPLY = "Apply";
	public final static String CMD_APPLY = "<html><i>Apply";

	public final static String CMD_CANCEL = MENU_CANCEL;

	public final static String CMD_PREV = "< Previous";

	public final static String CMD_NEXT = "Next >";

	public final static String CMD_FINISH = "Finish";

	public final static String CMD_RESUME = "Resume";

	public final static String CMD_STOP = "Stop";

	public final static String CMD_SKIP = "Skip";

	public final static String CMD_BROWSE = "Browse";

	public final static String CMD_CLOSE = "Close";

	public final static String CMD_OPEN = "Open";

	public final static String CMD_SAVE = "Save";

	public final static String CMD_EDIT = "Edit";

	public final static String CMD_RESET = "Reset";

	public final static String CMD_DEFAULT = "Default";

	public final static String CMD_CREATEDB = "Drop tables";

	public final static String CMD_INITJDBC = "Check driver";

	public final static String CMD_CREATEALBUM = "Create a new album";

	public final static String CMD_SETUP = "Setup >>";

	public final static String CMD_ADVANCED = "Advanced >>";

	public final static String CMD_COPY = "<";

	public final static String CMD_COPY_ALL = "<<";

	public final static String CMD_DELETE = ">";

	public final static String CMD_DELETE_ALL = ">>";

	public final static String CMD_HELP = MENU_HELP;

	public final static String CMD_ADDTOALBUM = MENU_ADDTOALBUM;

	public final static String CMD_ADDRESSBOOK = "Address Book";

	public final static String CMD_ADD = "Add";

	public final static String CMD_DEL = "Del";

	public final static String CMD_LEARN = "Learn";

	public final static String CMD_CHANGE_ALBUM_LOC = "Update Album Location";

	public final static String CMD_MARK = "Mark";

	public final static String CMD_TO_SEL = MENU_ADDTOCOLLECT;

	public final static String CMD_TO_ALB = MENU_ADDTOALBUM;

	public static final String CMD_REPAIRDB = "Repair DB";

	public static final String CMD_IMPORTDB = "Import DB";
	
	public final static String TITLE_HELP = MENU_HELP;

	public final static String TITLE_OPTIONS = "Options";

	public final static String TITLE_HTML_TEMPL = "Choose HTML template file";

	public final static String TITLE_SHOW = "Show";

	public final static String TITLE_PANES = "Panes";

	public final static String TITLE_PROPS_OF = "Properties of ";

	public final static String TITLE_RENAME = "Rename";
	
	public static final String TITLE_COPY = MENU_COPY;

	public final static String TITLE_DELETE = "Delete";

	public final static String TITLE_OVERWRITE = "Overwrite";

	public final static String TITLE_CHANGE_DRIVE = "Change current drive";

	public final static String TITLE_CREATEHTML = "Inet publishing";

	public final static String TITLE_SELECT_FOLDER = "Select folder";

	public final static String TITLE_SELECT_ALBUM = "Albums selection for - ";

	public final static String TITLE_ERROR = "Error";

	public final static String TITLE_COMMENT = "Commentary";

	public final static String TITLE_COFIRMATION = "Confirmation...";

	public final static String TITLE_PRINT = "Printing...";

	public final static String TITLE_FTP = "FTP settings";

	public final static String TITLE_EMAILS = "E-mail settings";

	public final static String TITLE_NEWS = "News settings";

	public final static String TITLE_HTTP = "HTTP settings";

	public final static String TITLE_SENDTO = "Send E-mail";

	public final static String TITLE_URL_SEL = "URL selection";

	public final static String TITLE_SRCALBUM = "Select imported album location";

	public final static String TITLE_ALBUMPROPS = "Album properties";

	public final static String TITLE_DEF_EMAILSUBJECT = "Information you've requested";

	public final static String TITLE_DESTWHOLEALBUM = "Select destination for whole album";

	public final static String TITLE_DESTSELALBUM = "Select destination for ";

	public final static String TITLE_WEBALBUM = "Select name of existing or new album";

	public final static String TITLE_NOW_PLAYING = "Now is playing ";

	public final static String TITLE_CLOSED = "Closed";

	public final static String TITLE_UPGRADE = "Upgrade";

	public final static String TITLE_CONTENT_SELECTION_CRITERIA = "List {0} filling rules";

	public final static String TITLE_ID3EDITOR = "Id3 tag editor";

	public final static String TITLE_UPGRDNOTIFIC = "Upgrade notification";

	public final static String TITLE_POSTTO = "Post article(s) to news group(s)";

	public final static String TITLE_NAME = "Name";

	public final static String TITLE_PRINT_LAYOUT = "Print Layout";

	// public final static String TITLE_ALL_SONGS = "All songs";
	public final static String TITLE_PLAYLIST = "Playlist name";

	public final static String TITLE_MAKEITALL = "View/Edit info";

	public final static String TITLE_ENCODING = "Select Encoding";

	public final static String TITLE_FIND = "Searching";

	public final static String TITLE_CONFIRMEXIT = "Confirm Exit";

	public final static String TITLE_DELETE_PALYLIST = "Confirm delete playlist";

	public final static String TITLE_SAVE_HTML = "Save content in HTML";

	public final static String TITLE_TRANS = "Transliteration";

	public static final String TIILE_TRANSFER_PROGRESS = "Transfer progress";

	public final static String COMP_IMAGE_VIEWER = "Image Viewer";

	public final static String COMP_MEDIA_PAYER = "Media Player";

	public final static String COMP_ALBUM_VIEWER = "Album";

	public final static String COMP_BROWSER = "Browser";

	public final static String COMP_SELECTION = "Selection";

	public final static int COMP_ALBUM_IDX = 0;

	public final static int COMP_SELECTION_IDX = 1;

	public final static int COMP_BROWSE_IDX = 2;

	public final static int COMP_RIPPER_IDX = 3;

	public final static int COMP_IPOD_IDX = 4;

	public final static String TTIP_RULEFORRENAME = "Specify a rule for renaming images";

	public final static String TTIP_RULEFORTRANSFORM = "Specify image transformation options";

	public final static String TTIP_TUMBNAILSOPTION = "Specify thumbnails panel options";

	public final static String TTIP_MISCELLANEOUS = "Specify misc options concerning layout";

	public final static String TTIP_MAIN_IFD = "Main image IFD";

	public final static String TTIP_THUMB_IFD = "Thumbnail image IFD";

	public final static String TTIP_PICTURE_INFO = "[Picture info]";

	public final static String TTIP_CAMERA_INFO = "[Camera info]";

	public final static String TTIP_DIAG_INFO = "[Diag info]";

	public final static String TTIP_HEAP = "CIFF Heap";

	public final static String TTIP_COLLECTTAB = "Use it to navigate through your selection structure";

	public final static String TTIP_BROWSETAB = "Use it to navigate through your directory structure";

	public final static String TTIP_COLLECTLIST = "Use it to place your selection";

	public final static String TTIP_IMAGETAB = "Use it to see original sized image";

	public final static String TTIP_DIRECTORYTAB = "Use it to navigate through your directory structure";

	public final static String TTIP_ALBUM = "Viewing and creation photo albums";

	public final static String TTIP_ALBUMTAB = "Use it to view content of your photo albums";

	public final static String TTIP_ALBUMOPTIONS = "Setting album options, includimg database settings";

	public final static String TTIP_PLUGINOPTIONS = "Adding, editing, and removing plug-ins";

	public final static String TTIP_WEBPUBLISH = "Web root and Ftp settings";

	public final static String TTIP_ADDTRANSFORMEDTOSEL = "Check it, if you'd like to add a transformed image to selection";

	public final static String TTIP_CREATEALBUMINCL = "Check it, if the new created album has to be included to selected";

	public final static String TTIP_GENERALPROPS = "Use it to get general information about properties requested component";

	public final static String TTIP_DETAILSPROPS = "Use it to get deeply in properties of requested component";

	public final static String TTIP_COPYALBUM = "Press this button to copy whole album or selected";

	public final static String TTIP_IMPORTALBUM = "Press this button to importing album from specified location";

	public final static String TTIP_ID3 = "Music fragment attributes extracted from ID3 tag v1 & v2";

	public final static String TTIP_MP3 = "Media attributes";

	public final static String TTIP_MEDIAOPTIONS = "Use it to set options relaited to music files";

	public final static String TTIP_GENRE = "Provide genre of music you would like to listen";

	public final static String TTIP_ARTIST = "Provide artist of music you would like to listen";

	public final static String TTIP_YEARS = "Provide years of music you would like to listen";

	public final static String TTIP_ADDITIONS = "Provide additional requirements to music you would like to listen";

	public final static String TTIP_APPEARANCE = "Customize appearance of tables and panes";

	public final static String TTIP_REMOTE_CTRL = "Allows to map keys of your remote control to the program commands";

	public final static String TTIP_RIPPER = "Allows to create audio, MP3, photo and mixed disk images";

	public final static String TTIP_RECORD_DISK = "Allows to create an image of photo, audio or MP3 disk";

	public final static String TTIP_JPEG = "Basic format properties";

	public final static String TTIP_IPODOPTIONS = "Configure access to your iPod";

	public final static String TTIP_IPOD_SYNC = "Initiate syncing with iPod";

	public final static String TTIP_IPOD_WIPE = "Delete everything and prepare for new files";

	public final static String TTIP_IPOD = "Access iPod music lists";

	public final static String TAB_COLLECTION = "Selection";

	public final static String TAB_DIRECTORY = "Directory";

	public final static String TAB_IMAGE = "Image";

	public final static String TAB_THUMBNAILS = "Thumbnails";

	public final static String TAB_RENAME = "Rename";

	public final static String TAB_TRANSFORM = "Transform";

	public final static String TAB_BROWSE = "Browse";

	public final static String TAB_MISCELLANEOUS = "Miscellaneous";

	public final static String TAB_MAIN_IFD = "Main";

	public final static String TAB_THUMBNAIL = "Thumbnail";

	public final static String TAB_PICTURE_INFO = "Picture";

	public final static String TAB_CAMERA = "Camera";

	public final static String TAB_DIAGNOSTIC = "Diagnostic";

	public final static String TAB_HEAP = "Heap";

	public final static String TAB_ALBUM = "Album";

	public final static String TAB_PLUGIN = "Plug-ins";

	public final static String TAB_MEDIAOPTIONS = "Media (MP3)";

	public final static String TAB_WEBPUBLISH = "InetPublishing";

	public final static String TAB_GENERAL = "General";

	public final static String TAB_DETAILS = "Details";

	public final static String TAB_ID3 = "ID3";

	public final static String TAB_MP3 = "Media";

	public final static String TAB_GENRE = "Genre";

	public final static String TAB_ARTIST = "Artist";

	public final static String TAB_YEARS = "Years";

	public final static String TAB_ADDITIONS = "Additions";

	public final static String TAB_APPEARANCE = "Appearance";

	public final static String TAB_REMOTE_CTRL = "Remote";

	public final static String TAB_RIPPER = "Ripper";

	public final static String TAB_JPEG = "Basic";

	public final static String TAB_IPODOPTIONS = "iPod";

	public final static String TAB_IPOD = "iPod";

	public final static String URL_HELP = "MediaChest.htm";

	public final static String TMPL_NOPUBLISHTEMPLATE = "<html>"
			+ "<body bgcolor=\"#F1D0F2\" link=\"#0000FF\" vlink=\"#800080\">"
			+ "<p align=\"center\"><h2>No publishing template HTML defined</h2></p>"
			+ "<p><small><strong>Open Settings/WebPublishing and check template location.</stron></small></p>"
			+ "<!--!%loop-->"
			+ "<a href=\"!%webp!%tnof\"><img src=\"!%tnfn\" alt=\"!%tntt\" border=0 style=\"border: 0px none\"></a><br>"
			// + "<!--!%tn+-->"
			+ "<!--!%endl-->" + "</body>" + "</html>";

	public static final String FMT_PLAYING_INFO = "<html><font face=\"{5}\"size=\"{6}\" color=\"blue\">"
			+ "<b>Title:</b>{0}<br><b>Artist:</b>{1}<br><b>Album:</b>{2}<br><b>Year:</b>{3}</font><br>"
			+ "<font size=\"1\" color=\"green\">{4}</font></html>";

	public final static String EXT_HTML = ".html";

	public final static String EXT_JPEG = "jpeg";

	public final static String EXT_JPG = ".jpg";

	public final static String EXT_PNG = ".png";

	public final static String EXT_TIFF = "tiff";

	public final static String EXT_BMP = "bmp";

	public final static String EXT_GIF = ".gif";

	public final static String EXT_MP3 = ".mp3";

	public final static String EXT_WAV = ".wav";

	public final static String EXT_SND = ".snd";

	public final static String EXT_AU = ".au";

	public final static String EXT_AIF = ".aif";

	public final static String EXT_WMA = ".wma";

	public final static String EXT_AIFC = ".aifc";

	public final static String EXT_AIFF = ".aiff";

	public final static String EXT_ASX = ".asx";

	public final static String EXT_WAX = ".max";

	public final static String EXT_M3U = ".m3u";

	public final static String EXT_WVX = ".wvx";

	public final static String EXT_XML = ".xml";

	public final static String EXT_CSV = ".csv";

	// application specific extensions
	public final static String EXT_PLX = ".plx"; // play list

	public final static String EXT_PSX = ".psx"; // play schema

	public final static String EXT_LAX = ".lax"; // layout

	public final static String IMG_LOGO = "logo.jpg";

	public final static String IMG_CHANGE_DRV = "changedrive.jpg";

	public final static String IMG_PHOTOORG = "photoorganizer.jpg";

	public final static String IMG_READCF = "readcf.jpg";

	public final static String IMG_MP3ICON = "mp3icon.jpg";
	
	public final static String IMG_CUEICON = "cueicon.jpg";

	public final static String IMG_MY_COMPUTER = "my_computer.jpg";

	public final static String IMG_HARD_DRIVE = "disk.jpg";

	public final static String IMG_DESKTOP = "desktop.jpg";

	public final static String IMG_IPOD = "ipod.jpg";

	public final static String IMG_IPOD_SMALL = "ipod_16x16.png";

	public final static String IMG_MP4ICON = "aac_ipod.jpg";

	public final static String IMG_WMAICON = "wmaicon.jpg";

	public final static String IMG_ALBUM = "album.png";

	public final static String IMG_ARTIST = "artist.png";

	public final static String IMG_SONG = "song.png";

	public final static String IMG_PLAYLIST = "playlist.png";

	public final static String IMG_PLAYLISTS = "playlists.png";

	public final static String IMG_SMART_PLAYLIST = "splaylist.png";

	public final static String IMG_GENRE = "genre.png";

	public final static String IMG_SONGS = "songs.png";

	public final static String IMG_VIDEOS = "videos.png";
	
	public final static String IMG_PHOTOS = "photos.gif";

	public final static String IMG_COMPILATION = "compilation.gif";
	
	public final static String IMG_PLAY = "play.gif";
	
	public final static String IMG_STOP = "stop.gif";
	
	public final static String IMG_SKIP = "skip.gif";
	
	public final static String IMG_ADVANCED = "advanced.gif";

	public static final String IMG_PHOTO = "photoalbum.gif";
	
	public final static String IMG_CLOSE = "close.gif";

	public final static String NO = "no";

	public final static String YES = "yes";

	public final static String N_A = "n/a";

	public final static Integer I_YES = new Integer(1);

	public final static Integer I_NO = new Integer(0);

	public final static int I_FLASH_DELAY = 8 * 1000;

	public final static java.awt.Color CLR_DLG_BLUE = new java.awt.Color(94, 94, 142);

	public final static java.awt.Font FNT_DLG_10 = new java.awt.Font("Dialog", 1, 10);

	public final static int CTRL_VERT_SIZE = 24;

	public final static int CTRL_VERT_GAP = 4;

	public final static int CTRL_HORIS_INSET = 8;

	public final static int CTRL_HORIZ_GAP = 8;

	public final static int CTRL_VERT_PREF_SIZE = 7;

	public final static java.awt.Dimension MIN_PANEL_DIMENSION = new java.awt.Dimension(40, 20);

	// Date
	public final static String DATE_FORMAT_MASK = "MM/dd/yyyy";

	public final static String TIME_FORMAT_MASK = "mm:ss";

	// XML TODO: move to separate file
	public final static String TAG_MEDIA = "media";

	public final static String TAG_FILE = "file";

	public final static String TAG_FOLDER = "folder";

	public final static String ATTR_NAME = "name";

	public final static String ATTR_MEDIATYPE = "media_type";

	public final static String ENC_UTF_8 = "UTF-8";

	public static final String LABEL_TIMEZONE = "Time Zone";

	public static final String LABEL_INFO = "Info";
	
	public static final String LABEL_SORT = "Sort";
	
	public static final String LABEL_OPT = "Options";

	public static final String LIST_SORTTITLE = "Sort Title";

	public static final String LIST_SORTALBUM = "Sort Album";

	public static final String LIST_SORTALBUMARTIST = "Sort Album-Artist";

	public static final String LIST_SORTARTIST = "Sort Artist";

	public static final String LIST_SORTCOMPOSER = "Sort Composer";

	public static final String LIST_SORTSHOW = "Sort Show";

	public static final String LIST_KEYWORDS = "Keywords";

	public static final String LIST_SKIPSHUFFL = "skip when shuffling";

	public static final String LIST_REMEMBERPLBK = "remember playback position";

	public static final String LIST_PARTGAPLESS = "part of gapless album";
    
    public static final String LIST_SKIPCOUNT = "Skip count";
    
    public static final String LIST_ALBUM_RATING = "Album Rating";
    
    public static final String LIST_ALBUM_ARTIST = "AlbumArtist";

    public static final String LIST_SKIPPEDCOUNT = "Skipped count";

    public static final String LIST_LAST_SKIPPED = "Last skipped";

	public static final String LABEL_VIDEOS = "Videos";
	
	public static final String LABEL_VIDEO = "Video";

	public static final String LABEL_VIDEO_KIND = "Video Kind";


	public static final String HDR_MOVIES = "Movies";

	public static final String HDR_MUSICVIDEO = "Music Video";

	public static final String HDR_PODCAST = "Podcast";

	public static final String HDR_TVSHOW = "TV Show";

	public static final Object LABEL_PHOTOALBUM = "Album";

	public static final String LABEL_ALLPHOTOS = "My Pictures";

	public static final String INFO_COPY_PHOTOS = "Copy photos...";

	public static final String INFO_PHOTODB_UPDATE = "Update photo DB...";

	public static final String LABEL_COPY_PHOTO = "Copy photo";

	public static final String INFO_PHOTODB_READ = "Read photo DB";
	

	}