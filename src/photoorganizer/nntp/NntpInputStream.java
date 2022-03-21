/*
 * Nntp client based on ideas provided by Jonathan Payne, James Gosling
 * $Id: NntpInputStream.java,v 1.3 2007/07/27 02:58:06 rogatkin Exp $
 */

package photoorganizer.nntp;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class NntpInputStream extends FilterInputStream {
	int column = 0;

	boolean eofOccurred = false;

	public NntpInputStream(InputStream child) {
		super(child);
	}

	int eof() {
		eofOccurred = true;
		return -1;
	}

	/**
	 * Read data from the NNTP stream.
	 * 
	 * @exception NntpProtocolException
	 *                thrown on bad data being read
	 */
	public int read() throws IOException {
		int c;

		if (eofOccurred)
			return -1;

		c = super.read();
		if (c == '.' && column == 0) {
			c = super.read();
			if (c == '\n')
				return eof();
			if (c != '.')
				throw new NntpProtocolException("Expecting '.' - got " + c);
		}
		if (c == '\n')
			column = 0;
		else
			column += 1;
		return c;
	}

	/**
	 * Fills <i>bytes</i> with data read from the stream.
	 * 
	 * @exception NntpProtocolException
	 *                see read() above.
	 */
	public int read(byte bytes[]) throws IOException {
		return read(bytes, 0, bytes.length);
	}

	/**
	 * Reads <i>length</i> bytes into byte array <i>bytes</i> at offset <i>off</i>
	 * with data read from the stream.
	 * 
	 * @exception NntpProtocolException
	 *                see read() above.
	 */
	public int read(byte bytes[], int off, int length) throws IOException {
		int c;
		int offStart = off;

		while (--length >= 0) {
			c = read();
			if (c == -1)
				break;
			bytes[off++] = (byte) c;
		}
		return (off > offStart) ? off - offStart : -1;
	}

	/**
	 * Close the input stream. We do nothing here because we know we don't
	 * actually own this stream. We're splicing ourselves into this stream and
	 * returning EOF after we have read a subset of the input.
	 */

	public void close() {
	}
}
