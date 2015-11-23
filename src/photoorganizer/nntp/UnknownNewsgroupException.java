/*
 * Nntp client based on ideas provided by Jonathan Payne, James Gosling
 * $Id: UnknownNewsgroupException.java,v 1.2 2001/10/19 00:52:29 rogatkin Exp $
 */

package photoorganizer.nntp;

import java.io.IOException;

public class UnknownNewsgroupException extends IOException {
    UnknownNewsgroupException(String s) {
	super(s);
    }
}
