/*
 * Nntp client based on ideas provided by Jonathan Payne, James Gosling * $Id: NntpClient.java,v 1.7 2007/07/27 02:58:06 rogatkin Exp $
 */

package photoorganizer.nntp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;

import sun.net.TelnetInputStream;
import sun.net.TransferProtocolClient;

public class NntpClient extends TransferProtocolClient {
	public final static String CRLF = "\r\n"; // to be moved in public constants
	public static final int NNTP_PORT = 119;
	public static int TRY_NUM = 2;

	String serverName;		/* for re-opening server connections */
	int serverPort;
	String login, password;

	/**
	 * for bean style instantiation
	 */
	public NntpClient () {
	}

	/** Create new NNTP Client connected to host <i>host</i> */
	public NntpClient (String host) throws IOException {
		this(host, NNTP_PORT);
	}
	/** Create new NNTP Client connected to host <i>host</i> and <i>port</i> */
	public NntpClient (String host, int port) throws IOException {
		openServer(host, port);
	}

	/**
	 * Open a connection to the NNTP server.
	 * @exception NntpProtocolException did not get the correct welcome message
	 */
	public void openServer(String name, int port) throws IOException {
		serverName = name;
		serverPort = port;
		super.openServer(name, port);
		int responseCode = readServerResponse();
		if (responseCode == 480)
			if (authenticate() == false)
				throw new NntpProtocolException("Not authenticated");
		else if (responseCode >= 300)
			throw new NntpProtocolException(""+serverResponse);
	}
	
	public void setCredentials(String login, String password) {
		this.login = login;
		this.password = password;
	}
	
	public boolean authenticate() throws IOException {
//		if (askServer("AUTHINFO GENERIC" + CRLF) < 400)
		int ar = askServer("AUTHINFO USER " + login + CRLF);
		//System.err.println("Send user code "+ar);
		if (ar == 381)
			ar = askServer("AUTHINFO PASS " + password + CRLF);
		//System.err.println("Send pass code "+ar);
		return ar == 281;
	}
	/**
	 * Rest connection, useful in case of errors
	 */
	public void reset() throws IOException {
		/* reconnect to the server */
		try {
			serverOutput.close();
		} catch(Exception e2) {
		}
		openServer(serverName, serverPort);
	}

    /** Sends command <i>cmd</i> to the server. */
	public int askServer(String cmd) throws IOException {
		int code = 503;
		for (int t = 0; t < TRY_NUM; t++) {
			try {
				serverOutput.print(cmd);
				code = readServerResponse();
				if (code == 480) {
					if (authenticate() == false)
						code = 502;
					else 
						t--;
				} else if (code < 500)
					return code;

				/*
				* errors codes >500 usually result from something happening
				* on the net.  Its usually profitable to disconnect and
				* reconnect
				*/
			} catch(Exception e) {
			}
		}
		return code;
	}

	InputStream makeStreamRequest(String cmd, int reply) throws IOException {
		int response;

		response = askServer(cmd + CRLF);
		if (response != reply) {
			String msg = null;
			try {
				for (int i = 0; i < 99; i++) {
					String n = (String) serverResponse.elementAt(i);
					if (msg == null)
						msg = n;
					else
						msg = msg + "\n" + n;
				}
			} catch(Exception e) {
			};
			 if (msg == null)
				 msg = "Command " + cmd + " yielded " + response + "; expecting " + reply;
			 throw new NntpProtocolException(msg);
		}
		switch (response / 100) {
		case 1:
		case 2:
			break;

		case 3:
			throw new NntpProtocolException("More input to command expected");

		case 4:
			throw new NntpProtocolException("Server error - cmd OK");

		case 5:
			throw new NntpProtocolException("Error in command: " + cmd);
		}
		return new NntpInputStream(new TelnetInputStream(serverInput, false));
	}
	String tokenize(String input)[] {
		Vector v = new Vector();
		StringTokenizer t = new StringTokenizer(input);
		String cmd[];

		while (t.hasMoreTokens())
			v.addElement(t.nextToken());
		cmd = new String[v.size()];
		for (int i = 0; i < cmd.length; i++)
			cmd[i] = (String) v.elementAt(i);

		return cmd;
	}

    /**
     * Get information about group <i>name</i>.
     * @exception UnknownNewsgroupException the group name wasn't active.
     * @exception NntpProtocolException received an unexpected reply.
     */
	public NewsgroupInfo getGroup(String name) throws IOException {
		switch (askServer("group " + name + CRLF)) {
		case 411:
			throw new UnknownNewsgroupException(name);

		default:
			throw new NntpProtocolException("unexpected reply: "
				+ getResponseString());

		case 211:
			{
				String tokens[] = tokenize(getResponseString());
				int start;
				int end;

				start = Integer.parseInt(tokens[2]);
				end = Integer.parseInt(tokens[3]);
				return new NewsgroupInfo(name, start, end);
			}
		}
	}

    /** Set the current group to <i>name</i> */
	public void setGroup(String name) throws IOException {
		if (askServer("group " + name + CRLF) != 211)
			throw new UnknownNewsgroupException(name);
	}

    /** get article <i>n</i> from the current group. */
	public InputStream getArticle(int n) throws IOException {
		return makeStreamRequest("article " + n, 220);
	}

    /** get article <i>id</i> from the current group. */
	public InputStream getArticle(String id) throws IOException {
		if (id.charAt(0) != '<')
			id = "<" + id + ">";
		return makeStreamRequest("article " + id, 220);
	}

    /** get header of article <i>n</i> from the current group. */
	public InputStream getHeader(int n) throws IOException {
		return makeStreamRequest("head " + n, 221);
	}
	/** get header of article <i>id</i> from the current group. */
	public InputStream getHeader(String id) throws IOException {
		if (id.charAt(0) != '<')
			id = "<" + id + ">";
		return makeStreamRequest("head " + id, 221);
	}
    	/** Setup to post a message.  It returns a stream
        to which the article should be written.  Returns null if the post
	is disallowed.  The article must have a properly formed RFC850 header
	and end-of-lines must by sent as \r\n.  The Article must end with
	\r\n */
	public PrintStream startPost() throws IOException {
		return askServer("post"+CRLF) == 340 ? serverOutput : null;
	}
	/** Finish posting a message.  Must be called after calling startPost
	and writing the article.  Returns true if the article is posted
	successfully. */
	public boolean finishPost() throws IOException {
		return askServer("."+CRLF) == 240;
	}

	/** sends a quit message and close connection
	 */
	public void close() {
		try {
			askServer("quit"+CRLF);
		} catch(Exception e1) {
		}
		try {
			serverOutput.close();
		} catch(Exception e1) {
		}
	}
}
