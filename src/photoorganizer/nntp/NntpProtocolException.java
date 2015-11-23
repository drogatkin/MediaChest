/*
 * Nntp client based on ideas provided by Jonathan Payne, James Gosling
 * $Id: NntpProtocolException.java,v 1.2 2001/10/19 00:52:29 rogatkin Exp $
 */

package photoorganizer.nntp;

import java.io.IOException;

/**
 * This exception is thrown when unexpected results are returned by the
 * NNTP server.
 */
public class NntpProtocolException extends IOException {
    NntpProtocolException(String s) {
	super(s);
    }
}

