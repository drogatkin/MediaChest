/* MediaChest - Ftp
 *
 * AUTHOR: dragones
 * Revised by Dmitry Rogatkin
 * $Id: Ftp.java,v 1.3 2007/07/27 02:58:04 rogatkin Exp $
 */
package photoorganizer.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

public class Ftp {
	/* Static Globals ****************************************** */

	public static final int DATA_PORT = 20;

	public static final int CONTROL_PORT = 21;

	public static final int BLOCK_SIZE = 8192;

	static public final int POSITIVE_PRELIMINARY_REPLY = 1;

	static public final int POSITIVE_COMPLETION_REPLY = 2;

	static public final int POSITIVE_INTERMEDIATE_REPLY = 3;

	static public final int TRANSIENT_NEGATIVE_REPLY = 4;

	static public final int PERMANENT_NEGATIVE_REPLY = 5;

	static final int NOOP_INTERVAL = 5 * 60 * 1000;

	public static final int CODE_SCALE = 100;

	/* Instance Globals *************************************** */

	String wd; // work directory

	// Sockets

	protected Socket ftpSocket;

	protected Socket dataSocket;

	// Streams

	protected PrintStream ftp_os;

	protected BufferedReader ftp_is;

	// Flags

	protected boolean usePasv;

	protected boolean useProxy;

	// Passive Connection Server Data Address/Port

	protected InetAddress pasvAddress;

	protected int pasvPort = 0;

	// Connection Flag

	public boolean isConnected;

	public boolean keep_connection;

	// Keep Alive Thread

	KeepAliveThread kat;

	FtpConnectionInfo ftpconnectioninfo;

	public Ftp(FtpConnectionInfo ftpconnectioninfo) {
		this(ftpconnectioninfo, true);
	}

	public Ftp(FtpConnectionInfo ftpconnectioninfo, boolean keepalive) {
		// Set Globals
		this.ftpconnectioninfo = ftpconnectioninfo;
		keep_connection = keepalive;
		// Open Connection
		openControlConnection();
	}

	public void setPassiveMode(boolean on) {
		usePasv = on;
	}

	public boolean isPassiveMode() {
		return usePasv;
	}

	public String getHomeDirectory() {
		return ftpconnectioninfo.startDirectory;
	}

	public void login() throws IOException {
		FtpCommandReply r = user();
		if ((r.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
			throw new IOException((r.replyMessage != null && r.replyMessage.size() > 0) ? (String) r.replyMessage
					.elementAt(0) : "Command user failed");
		r = pass();
		if ((r.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
			throw new IOException((r.replyMessage != null && r.replyMessage.size() > 0) ? (String) r.replyMessage
					.elementAt(0) : "Command pass failed");
		if (ftpconnectioninfo.startDirectory == null || ftpconnectioninfo.startDirectory.length() == 0) {
			r = pwd();
			if (r.replyMessage.size() > 0 && (r.replyCode / Ftp.CODE_SCALE) == Ftp.POSITIVE_COMPLETION_REPLY) {
				StringTokenizer st = new StringTokenizer((String) r.replyMessage.elementAt(0), " ");
				if (st.hasMoreTokens()) {
					st.nextToken();
					if (st.hasMoreTokens()) {
						ftpconnectioninfo.startDirectory = st.nextToken();
						if (ftpconnectioninfo.startDirectory.charAt(0) == '"')
							ftpconnectioninfo.startDirectory = ftpconnectioninfo.startDirectory.substring(1,
									ftpconnectioninfo.startDirectory.length() - 1);
					} else
						ftpconnectioninfo.startDirectory = "";
				}
			}

		}
	}

	/* Commands *********************************************** */

	synchronized public FtpCommandReply abor() {
		// ABOR <CRLF>
		/*
		 * Abort the previous FTP service command and any transfer of data
		 */

		// update GUI abort
		byte AbortSequence[] = new byte[4];
		AbortSequence[0] = (byte) 255; // IAC
		AbortSequence[1] = (byte) 244; // IP
		AbortSequence[2] = (byte) 255; // IAC
		AbortSequence[3] = (byte) 242; // SYNC
		ftp_os.write(AbortSequence, 0, 4);

		FtpCommandReply fcr = getReply("ABOR");

		if (fcr == null) {
			openControlConnection();
			// Refresh
			return fcr;
		} else if (fcr.replyCode / CODE_SCALE == POSITIVE_COMPLETION_REPLY)
			return fcr;
		else {
			return getReply(null);
		}
	}

	synchronized public FtpCommandReply acct(String account_information) {
		// ACCT <SP> <account-information> <CRLF>
		// Specifies the user's account to the server
		// TODO: Take action necessitated by reply code

		return getReply("ACCT " + account_information);
	}

	synchronized public FtpCommandReply allo(String decimal_integer) {
		// ALLO <SP> <decimal-integer> <CRLF>
		// Might be required by some servers to reserve storage

		return getReply("ALLO " + decimal_integer);
	}

	synchronized public FtpCommandReply allo(String decimal_integer1, String decimal_integer2) {
		// ALLO <SP> <decimal-integer> <SP> R <SP> <decimal-integer> <CRLF>

		return getReply("ALLO " + decimal_integer1 + " R " + decimal_integer2);
	}

	synchronized public FtpCommandReply appe(String pathname) {
		// APPE <SP> <pathname> <CRLF>
		// Append file to existing file or create

		return getReply("APPE " + pathname);
	}

	synchronized public FtpCommandReply cdup() {
		// CDUP <CRLF>
		// change to parent directory

		return getReply("CDUP");
	}

	/**
	 * CWD <SP> <pathname> <CRLF> change working directory
	 */
	synchronized public FtpCommandReply cwd() {
		return cwd(ftpconnectioninfo.startDirectory);
	}

	synchronized public FtpCommandReply cwd(String pathname) {
		return getReply("CWD " + pathname);
	}

	synchronized public FtpCommandReply dele(String pathname) {
		// DELE <SP> <pathname> <CRLF>
		// Delete file specified in the pathname

		return getReply("DELE " + pathname);
	}

	synchronized public FtpCommandReply help() {
		// HELP <CRLF>
		// Causes the server to send helpful information

		return getReply("HELP");
	}

	synchronized public FtpCommandReply help(String string) {
		// HELP <SP> <string> <CRLF>
		return getReply("HELP " + string);
	}

	/**
	 * LIST <SP> <pathname> <CRLF>
	 */
	synchronized public FtpCommandReply list() {
		return list(null);
	}

	synchronized public FtpCommandReply list(String pathname) {
		Socket pasvSocket = null;
		ServerSocket serverSocket = null;
		FtpCommandReply list_fcr = null;

		if (usePasv) {
			list_fcr = pasv();
			if ((list_fcr.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
				return list_fcr;
			pasvSocket = getPasvSocket();
		} else {
			serverSocket = getServerSocket();
			port(serverSocket);
		}

		list_fcr = getReply("LIST" + (pathname != null ? " " + pathname : ""));
		if ((list_fcr.replyCode / CODE_SCALE) == POSITIVE_PRELIMINARY_REPLY) {
			if (usePasv)
				list_fcr = getScreenData(pasvSocket);
			else
				list_fcr = getScreenData(serverSocket);
		}

		try {
			if (usePasv)
				pasvSocket.close();
			else
				serverSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return list_fcr;
	}

	synchronized public FtpCommandReply mkd(String pathname) {
		// MKD <SP> <pathname> <CRLF>
		// Creates a directory specified by path

		return getReply("MKD " + pathname);
	}

	synchronized public FtpCommandReply mode(String mode_code) {
		// MODE <SP> <mode-code> <CRLF>
		// Specifies the transfer mode

		return getReply("MODE " + mode_code);
	}

	/** NLST <CRLF> */
	public FtpCommandReply nlst() {
		return nlst(null);
	}

	/**
	 * Send a name list NLST <SP> <pathname> <CRLF>
	 */
	synchronized public FtpCommandReply nlst(String pathname) {

		Socket pasvSocket = null;
		ServerSocket serverSocket = null;
		FtpCommandReply nlst_fcr = null;

		if (usePasv) {
			nlst_fcr = pasv();
			if ((nlst_fcr.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
				return nlst_fcr;
			pasvSocket = getPasvSocket();
		} else {
			serverSocket = getServerSocket();
			port(serverSocket);
		}

		nlst_fcr = getReply("NLST" + (pathname != null && pathname.length() > 0 ? " " + pathname : ""));

		if ((nlst_fcr.replyCode / CODE_SCALE) == POSITIVE_PRELIMINARY_REPLY) {
			if (usePasv)
				nlst_fcr = getScreenData(pasvSocket);
			else
				nlst_fcr = getScreenData(serverSocket);
		}

		try {
			if (usePasv)
				pasvSocket.close();
			else
				serverSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return nlst_fcr;
	}

	synchronized public FtpCommandReply noop() {
		// NOOP <CRLF>
		// No Action, Server send OK reply

		return getReply("NOOP");
	}

	/**
	 * PASS <SP> <password> <CRLF> Gives user's password to the server
	 */
	synchronized public FtpCommandReply pass(String password) {
		ftpconnectioninfo.setPassword(password);
		return pass();
	}

	synchronized public FtpCommandReply pass() {
		return getReply("PASS " + ftpconnectioninfo.getPassword());
	}

	synchronized public FtpCommandReply pasv() {
		// PASV <CRLF>
		// requests the server to listen on a data port

		FtpCommandReply pasv_fcr = getReply("PASV");

		// Parse out address and port

		String message = (String) pasv_fcr.replyMessage.elementAt(0);

		int beginIndex = message.indexOf('(');
		int endIndex = message.indexOf(')');
		if ((pasv_fcr.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
			return pasv_fcr;

		StringTokenizer address_port_tokens = new StringTokenizer(message.substring(beginIndex + 1, endIndex), ",");

		try {
			pasvAddress = InetAddress.getByName(address_port_tokens.nextToken() + "." + address_port_tokens.nextToken()
					+ "." + address_port_tokens.nextToken() + "." + address_port_tokens.nextToken());
		} catch (UnknownHostException e) {

		}

		pasvPort = Integer.parseInt(address_port_tokens.nextToken()) << 8;
		pasvPort += Integer.parseInt(address_port_tokens.nextToken());

		return pasv_fcr;
	}

	synchronized public FtpCommandReply port(ServerSocket serverSocket) {
		// PORT <SP> <host-port> <CRLF>
		// Specifies the host-port for the data connection to the server

		byte[] addrbytes = null;

		try {
			// get ip address in high byte order
			addrbytes = serverSocket.getInetAddress().getLocalHost().getAddress();
		} catch (UnknownHostException e) {
		}

		// tell server what port we are listening on
		short addrshorts[] = new short[4];

		// problem: bytes greater than 127 are printed as negative numbers
		for (int i = 0; i <= 3; i++) {
			addrshorts[i] = (short) addrbytes[i];
			if (addrshorts[i] < 0)
				addrshorts[i] += (short) 256;
		}

		int localport = serverSocket.getLocalPort();

		// Send port command to server

		String command = "PORT " + addrshorts[0] + "," + addrshorts[1] + "," + addrshorts[2] + "," + addrshorts[3]
				+ "," + ((localport & 0xff00) >> 8) + "," + (localport & 0x00ff);

		return getReply(command);
	}

	synchronized public FtpCommandReply pwd() {
		// PWD <CRLF>
		// Print working directory

		return getReply("PWD");
	}

	synchronized public FtpCommandReply quit() {
		if (!isConnected)
			return null;
		// QUIT <CRLF>
		/*
		 * terminates a USER and control connection if a file transfer is in
		 * progress, the server will remain open for a result response then
		 * close
		 */

		FtpCommandReply reply = null;

		try {
			try {
				reply = getReply("QUIT");
			} catch (NullPointerException e) { // some other exceptions can
												// happen on quit too
				debugMsg("Exception on Quit", e);
			}
			ftp_os.close();
			ftp_os = null;

			ftp_is.close();
			ftp_is = null;

			ftpSocket.close();
			ftpSocket = null;

			if (keep_connection && kat != null) {
				keep_connection = false;
				kat.interrupt();
				kat = null;
			}
			isConnected = false;
		} catch (IOException e) {
			debug(e);
		}

		return reply;
	}

	synchronized public FtpCommandReply rein() {
		// REIN <CRLF>
		/*
		 * terminates a USER flush all I/O and account information allow
		 * transfers in progress to be completed all parameters are reset to
		 * defaults control connection is left open
		 */

		return getReply("REIN");
	}

	synchronized public FtpCommandReply rest(String marker) {
		// REST <SP> <marker> <CRLF>
		/*
		 * Argument represents the server marker at which file transfer is to be
		 * restarted
		 */

		return getReply("REST " + marker);
	}

	synchronized public FtpCommandReply retr(String pathname, File file, int size) {
		// RETR <SP> <pathname> <CRLF>
		// Retrieve ("Get") a file

		ServerSocket serverSocket = null;
		Socket pasvSocket = null;

		if (usePasv) {
			pasvSocket = getPasvSocket();
		} else {
			serverSocket = getServerSocket();
		}

		FtpCommandReply retr_fcr = getReply("RETR " + pathname);

		if ((retr_fcr.replyCode / CODE_SCALE) == POSITIVE_PRELIMINARY_REPLY) {
			if (usePasv)
				retr_fcr = getFileData(file, pasvSocket, size);
			else
				retr_fcr = getFileData(file, serverSocket, size);
		}

		try {
			if (usePasv)
				pasvSocket.close();
			else
				serverSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return retr_fcr;
	}

	synchronized public FtpCommandReply rmd(String pathname) {
		// RMD <SP> <pathname> <CRLF>
		// Remove directory

		return getReply("RMD " + pathname);
	}

	synchronized public FtpCommandReply rnfr(String pathname) {
		// RNFR <SP> <pathname> <CRLF>
		// Rename From

		return getReply("RNFR " + pathname);
	}

	synchronized public FtpCommandReply rnto(String pathname) {
		// RNTO <SP> <pathname> <CRLF>
		// Rename To

		return getReply("RNTO " + pathname);
	}

	synchronized public FtpCommandReply site(String string) {
		// SITE <SP> <string> <CRLF>
		// Used by the server to provide services specific to the system

		return getReply("SITE " + string);
	}

	synchronized public FtpCommandReply smnt(String pathname) {
		// SMNT <SP> <pathname> <CRLF>
		// Allows the user to mount a different file system data structure

		return getReply("SMNT " + pathname);
	}

	synchronized public FtpCommandReply stat() {
		// STAT <CRLF>
		// Causes a status message to be sent from the server

		return getReply("STAT");
	}

	synchronized public FtpCommandReply stat(String pathname) {
		// STAT <SP> <pathname> <CRLF>

		return getReply("STAT " + pathname);
	}

	synchronized public FtpCommandReply stor(String pathname) throws IOException {
		ServerSocket serverSocket = null;
		Socket pasvSocket = null;
		FtpCommandReply stor_fcr;

		if (usePasv) {
			stor_fcr = pasv();
			if ((stor_fcr.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
				throw new IOException((String) stor_fcr.replyMessage.elementAt(0));
			pasvSocket = getPasvSocket();
		} else {
			serverSocket = getServerSocket();
			port(serverSocket);
		}

		// ok, send command
		stor_fcr = getReply("STOR " + pathname);

		if ((stor_fcr.replyCode / CODE_SCALE) == POSITIVE_PRELIMINARY_REPLY) {
			// listen on data port

			if (usePasv)
				stor_fcr.socket = pasvSocket;
			else
				stor_fcr.serverSocket = serverSocket;
		} else
			throw new IOException((String) stor_fcr.replyMessage.elementAt(0));
		return stor_fcr;
	}

	/**
	 * STOR <SP> <pathname> <CRLF> Store ("put") a file
	 */
	synchronized public FtpCommandReply stor(String pathname, int size) {
		ServerSocket serverSocket = null;
		Socket pasvSocket = null;
		FtpCommandReply stor_fcr;

		if (usePasv) {
			stor_fcr = pasv();
			if ((stor_fcr.replyCode / CODE_SCALE) >= TRANSIENT_NEGATIVE_REPLY)
				return stor_fcr;
			pasvSocket = getPasvSocket();
		} else {
			serverSocket = getServerSocket();
			port(serverSocket);
		}

		// ok, send command

		int lastindexof = pathname.lastIndexOf(File.separatorChar);
		if (lastindexof == -1)
			lastindexof = 0;

		stor_fcr = getReply("STOR " + pathname.substring(lastindexof + 1, pathname.length()));

		// guess this should be an exception if false

		if ((stor_fcr.replyCode / CODE_SCALE) == POSITIVE_PRELIMINARY_REPLY) {
			// listen on data port

			if (usePasv)
				stor_fcr = putFileData(pathname, pasvSocket, size);
			else
				stor_fcr = putFileData(pathname, serverSocket, size);
		}

		try {
			if (usePasv)
				pasvSocket.close();
			else
				serverSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return stor_fcr;
	}

	synchronized public FtpCommandReply stou() {
		// STOU <CRLF>
		/*
		 * Store a unique file into directory TODO: play around with this
		 */

		return getReply("STOU");
	}

	synchronized public FtpCommandReply stru(String structure_code) {
		// STRU <SP> <structure-code> <CRLF>
		// Specifies the file structure

		return getReply("STRU " + structure_code);
	}

	synchronized public FtpCommandReply syst() {
		// SYST <CRLF>
		// Find out the type of operating system on the server

		return getReply("SYST");
	}

	synchronized public FtpCommandReply type(String type_code) {
		// TYPE <SP> <type-code> <CRLF>
		// Specifies the representation type

		return getReply("TYPE " + type_code);
	}

	synchronized public FtpCommandReply type(String type_code, String form_code) {
		return getReply("TYPE " + type_code + " " + form_code);
	}

	synchronized public FtpCommandReply type(String type_code, Integer byte_size) {
		return getReply("TYPE " + type_code + " " + byte_size);
	}

	/**
	 * USER <SP> <username> <CRLF> Identifies the user to the server
	 */
	synchronized public FtpCommandReply user() {
		return getReply("USER " + ftpconnectioninfo.user
				+ (ftpconnectioninfo.useProxy ? ("@" + ftpconnectioninfo.host) : ""));
	}

	synchronized public FtpCommandReply user(String username) {
		ftpconnectioninfo.user = username;
		return user();
	}

	/* Private Functions ************************************** */

	private Vector addVector(Vector v1, Vector v2) {
		// Adds v2 to the end of v1

		int size = v2.size();

		for (int i = 0; i < size; ++i)
			v1.addElement(v2.elementAt(i));

		return v1;
	}

	private Socket getClientSocket(ServerSocket serverSocket) {
		Socket clientSocket = null;

		try {
			clientSocket = serverSocket.accept();
		} catch (InterruptedIOException e) {
			debug(e);
		} catch (IOException e) {
			debug(e);
		}

		return clientSocket;
	}

	private FtpCommandReply getFileData(File file, ServerSocket serverSocket, int size) {
		Socket clientSocket = getClientSocket(serverSocket);
		if (clientSocket == null)
			return new FtpCommandReply(null, 0, null, null);

		FtpCommandReply fcr = getFileData1(file, clientSocket, size);

		try {
			clientSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return fcr;
	}

	private FtpCommandReply getFileData(File file, Socket socket, int size) {
		return getFileData1(file, socket, size);
	}

	private FtpCommandReply getFileData1(File file, Socket socket, int size) {
		try {
			// Declare and initialize file
			InputStream is = socket.getInputStream();
			byte b[] = new byte[BLOCK_SIZE];
			int amount;

			// open file and write data

			int count = 0;
			RandomAccessFile outfile = new RandomAccessFile(file, "rw");

			while ((amount = is.read(b)) != -1) {
				count += amount;

				if (count <= size)
					;
				// update GUI (session.name, count, size)
				else
					;
				// update GUI (session.name, count, size)

				outfile.write(b, 0, amount);
			}

			// Clean up

			outfile.close();
			is.close();
		} catch (IOException e) {
			debug(e);
		}

		return getReply(null);
	}

	private Socket getPasvSocket() {
		Socket pasvSocket = null;

		try {
			pasvSocket = new Socket(pasvAddress, pasvPort);
		} catch (IOException e) {
			debug(e);
		}

		return pasvSocket;
	}

	synchronized public FtpCommandReply getReply(String command) {
		Vector message = new Vector();
		String line = null;
		int code = 0;
		if (command != null)
			ftp_os.print(command + "\r\n");
		// System.err.println("Processing FTP command: "+command); //@@@@@@@@@
		try {
			line = ftp_is.readLine();
			message.addElement(line);
			code = Integer.parseInt(line.substring(0, 3));
			if (line.length() > 3 && line.charAt(3) == '-')
				do {
					line = ftp_is.readLine();
					message.addElement(line);
					if (line.length() > 3 && Character.isDigit(line.charAt(0)) && Character.isDigit(line.charAt(1))
							&& Character.isDigit(line.charAt(2)) && line.charAt(3) == ' '
							&& code == Integer.parseInt(line.substring(0, 3)))
						break;
				} while (true);
		} catch (IOException e) {
			debug(e);
			if (message.size() == 0)
				message.addElement(e.getMessage());
		} catch (NullPointerException npe) {
			debugMsg("FTP server returned null response for " + command, npe);
		}
		// System.err.println("FTP server returned: "+code+' '+message);
		// //@@@@@@@@@
		return new FtpCommandReply(command, code, message, null);
	}

	private FtpCommandReply getScreenData(ServerSocket serverSocket) {
		Socket clientSocket = getClientSocket(serverSocket);

		if (clientSocket == null)
			return new FtpCommandReply(null, 0, null, null);

		FtpCommandReply fcr = getScreenData1(clientSocket);

		try {
			clientSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return fcr;
	}

	private FtpCommandReply getScreenData(Socket socket) {
		return getScreenData1(socket);
	}

	private FtpCommandReply getScreenData1(Socket socket) {
		Vector replyData = new Vector();

		try {
			InputStream is = socket.getInputStream();
			byte b[] = new byte[BLOCK_SIZE];
			int amount;

			// Loop through bytes on input socket, dividing into lines

			while ((amount = is.read(b)) != -1) {
				String crumbs = null;
				int i, count;

				for (i = 0, count = 0; i < amount; ++i, ++count) {
					if ((char) b[i] == '\n') {
						String line = new String(b, i - count, count).trim();

						if (crumbs != null) {
							replyData.addElement(crumbs + line);
							crumbs = null;
						} else
							replyData.addElement(line);

						count = 0;
					}
				}

				if (count > 0) {
					crumbs = new String(b, i - count, count);
				}
			}
			// Close socket and streams
			is.close();
			socket.close();
		} catch (IOException e) {
			debug(e);
		}

		// return complete server reply
		FtpCommandReply finish = getReply(null);

		return new FtpCommandReply(null, finish.replyCode, finish.replyMessage, replyData);
	}

	private ServerSocket getServerSocket() {
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(0); // use any available port
		} catch (IOException e) {
			debug(e);
		}

		return serverSocket;
	}

	private void openControlConnection() {

		ConnectThread ct = new ConnectThread(ftpconnectioninfo);
		setPassiveMode(!ftpconnectioninfo.active);
		ct.start();

		try {
			ct.join(ftpconnectioninfo.timeout);
		} catch (java.lang.InterruptedException e) {
		}

		ftpSocket = ct.result();

		if (ftpSocket != null) {
			isConnected = true;

			try {
				ftp_os = new PrintStream(ftpSocket.getOutputStream());
				ftp_is = new BufferedReader(new InputStreamReader(ftpSocket.getInputStream()));
				System.err.println(getReply(null).toString());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Listen to Server
			// Start Keep Alive Thread
			if (keep_connection) {
				this.kat = new KeepAliveThread();
				kat.start();
			}
		}
	}

	protected void finalize() throws Throwable {
		if (isConnected) {
			quit();
		}
		super.finalize();
	}

	private FtpCommandReply putFileData(String pathname, ServerSocket serverSocket, int size) {
		Socket clientSocket = getClientSocket(serverSocket);
		if (clientSocket == null)
			return new FtpCommandReply(null, 0, null, null);

		FtpCommandReply fcr = putFileData1(pathname, clientSocket, size);

		try {
			clientSocket.close();
		} catch (IOException e) {
			debug(e);
		}

		return fcr;
	}

	private FtpCommandReply putFileData(String pathname, Socket socket, int size) {
		return putFileData1(pathname, socket, size);
	}

	private FtpCommandReply putFileData1(String pathname, Socket socket, int size) {
		try {
			OutputStream outdataport = socket.getOutputStream();

			byte b[] = new byte[BLOCK_SIZE];

			// open file
			RandomAccessFile infile = new RandomAccessFile(pathname, "r");

			// do actual upload
			int amount, count = 0;

			// *** read returns 0 at end of file, not -1 as in api
			while ((amount = infile.read(b)) != 0) {
				count += amount;

				// pass stat string here
				// update progress count, size
				outdataport.write(b, 0, amount);
			}

			infile.close();
			outdataport.close();
		} catch (IOException e) {
			debug(e);
		}

		return getReply(null);
	}

	static void debug(Throwable e) {
		e.printStackTrace();
	}

	static void debugMsg(String msg, Throwable e) {
		System.err.println(msg + "(" + e + ')');
		e.printStackTrace();
	}

	class KeepAliveThread extends Thread {

		public void run() {
			while (keep_connection) {
				try {
					sleep(NOOP_INTERVAL); // Sleep constant amount of time
											// (correct?)
					noop();
				} catch (InterruptedException e) {
				} // no need to catch
			}
		}
	}
}

class ConnectThread extends Thread {
	private Socket ftpSocket = null;

	FtpConnectionInfo ci;

	public ConnectThread(FtpConnectionInfo ci) {
		this.ci = ci;
	}

	public void run() {
		try {
			if (ci.useProxy)
				ftpSocket = new Socket(InetAddress.getByName(ci.proxyHost), ci.proxyPort);
			else
				ftpSocket = new Socket(InetAddress.getByName(ci.host), Ftp.CONTROL_PORT);
		} catch (java.net.UnknownHostException e) {
		} catch (java.io.IOException e) {
			Ftp.debug(e);
		}
	}

	public Socket result() {
		return ftpSocket;
	}
}
