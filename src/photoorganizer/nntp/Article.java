/* MediaChest - $RCSfile: Article.java,v $ 
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * $Id: Article.java,v 1.10 2007/07/27 02:58:06 rogatkin Exp $
 */
package photoorganizer.nntp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.internet.MimeUtility;

import photoorganizer.PhotoOrganizer;

// the class does not use pipe for output stream
// and it can be problem for files bigger than
// MAX_INT minus overbytes for encoding

// TODO: an article can be in MIME format, if it's so, to add:
// - in header -
// Mime-version: 1.0
// Content-type: multipart/mixed; boundary="MS_Mac_OE_3085997491_134962_MIME_Part"
// - in body -
//> THIS MESSAGE IS IN MIME FORMAT. Since your mail reader does not understand
//this format, some or all of this message may not be legible.
// - after -
//-MS_Mac_OE_3085997491_134962_MIME_Part
//Content-type: text/plain; charset="US-ASCII"
//Content-transfer-encoding: 7bit
// some text is here
//--MS_Mac_OE_3085997491_134962_MIME_Part
//Content-type: multipart/appledouble; boundary="MS_Mac_OE_111425_3085997491_MIME_Part"
//
//--MS_Mac_OE_111425_3085997491_MIME_Part
//Content-type: application/applefile; name="Foxy40.com-971050-068.jpg"
//Content-transfer-encoding: base64
// encoded image
//
//--MS_Mac_OE_111425_3085997491_MIME_Part
//Content-type: image/jpeg; name="Foxy40.com-971050-068.jpg"; x-mac-creator="3842494D"; x-mac-type="4A504547"
//Content-disposition: attachment
//Content-transfer-encoding: base64
// encoded image
//
//--MS_Mac_OE_111425_3085997491_MIME_Part--
//
//--MS_Mac_OE_3085997491_134962_MIME_Part--

public class Article /*extends MimeMessage*/{
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, dd MMM yyyy HH:mm:ss ZZZZ");

    public static final String RELAY_VERSION = "Relay-Version";

    public static final String FROM = "From";

    public static final String REPLY_TO = "Reply-To";

    public static final String PATH = "Path";

    public static final String USER_AGENT = "User-Agent";

    public static final String NEWSGROUPS = "Newsgroups";

    public static final String SUBJECT = "Subject";

    public static final String MESSAGE_ID = "Message-ID";

    public static final String DATE = "Date";

    public static final String CONTENT_TYPE = "Content-type";

    public static final String FOLLOWUP_TO = "Followup-To";

    public static final String EXPIRES = "Expires";

    public static final String ORGANIZATION = "Organization";

    public static final String DATE_RECEIVED = "Date-Received";

    public static final String POSTING_VERSION = "Posting-Version";

    public static final String TITLE = "Title";

    public static final String ARTICLE_ID = "Article-I.D.";

    public static final String POSTED = "Posted";

    public static final String RECEIVED = "Received";

    public static final String LINES = "Lines";

    protected static final String[] headers_ORDER = { RELAY_VERSION, POSTING_VERSION, FROM, DATE, NEWSGROUPS, SUBJECT,
            MESSAGE_ID, PATH };

    private static int uniqueCounter;

    protected Map headers;

    protected String content;

    protected String site;

    protected List parts;

    protected String encoding = "ISO8859_1";

    public Article() {
        headers = new HashMap();
        parts = new ArrayList();
        try {
            site = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            site = "localhost";
        }
    }

    public Article(String encoding) {
        this();
        if (encoding != null && encoding.length() > 0)
            this.encoding = encoding;
    }

    protected void generatePostId() {
        setHeader(MESSAGE_ID, "<" + (System.currentTimeMillis() / 100l) + '-' + (uniqueCounter++) + '@' + site + '>');
    }

    public void setStandardHeaders() {
        // TODO: make more generic, specific main class free 
        /*setHeader(RELAY_VERSION, PhotoOrganizer.PROGRAMNAME+" "+
         PhotoOrganizer.VERSION+'.'+PhotoOrganizer.BUILD+" $Date: 2007/07/27 02:58:06 $; site "+site);
         setHeader(POSTING_VERSION,  PhotoOrganizer.PROGRAMNAME+" "+
         PhotoOrganizer.VERSION+'.'+PhotoOrganizer.BUILD+" $Date: 2007/07/27 02:58:06 $; site "+site);*/
        setHeader(USER_AGENT, PhotoOrganizer.PROGRAMNAME + " " + PhotoOrganizer.VERSION + '.' + PhotoOrganizer.BUILD
                + " $Date: 2007/07/27 02:58:06 $");
        setHeader(PATH, site);
        setHeader(CONTENT_TYPE, "text/plain; charset=\"" + encoding + '"');
        generatePostId();
        setDateheaders(DATE, new Date());
    }

    public void setHeader(String name, String value) {
        headers.put(name.toUpperCase(), value);
    }

    public void setHeader(String name, Object value) {
        headers.put(name.toUpperCase(), value);
    }

    public void setDateheaders(String name, Date value) {
        headers.put(name.toUpperCase(), value);
    }

    public void setLongheaders(String name, long value) {
        headers.put(name.toUpperCase(), "" + value);
    }

    public void addheaders(String name, String value) {
    }

    public void addheaders(String name, Object value) {
    }

    public void addDateheaders(String name, Date value) {
    }

    public void addLongheaders(String name, long value) {
    }

    public void setBody(String content) {
        this.content = content;
    }

    public void addPart(String name, InputStream source) {
        parts.add(new Entry(name, source));
    }

    public void addPart(String name, byte[] source) {
        parts.add(new Entry(name, source));
    }

    public void addPart(File source) {
        parts.add(new Entry(source.getName(), source));
    }

    public void resetParts() {
        parts.clear();
    }

    protected String produceheadersPart() {
        StringBuffer result = new StringBuffer(100);
        Map wm = (Map) ((HashMap) headers).clone();
        for (int i = 0; i < headers_ORDER.length; i++) {
            Object value = wm.remove(headers_ORDER[i].toUpperCase());
            if (value != null)
                result.append(headers_ORDER[i]).append(": ").append(convertToString(value)).append(NntpClient.CRLF);
        }
        Iterator it = wm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            result.append(e.getKey().toString()).append(": ").append(convertToString(e.getValue())).append(
                    NntpClient.CRLF);
        }
        return result.toString();
    }

    protected String convertToString(Object o) {
        if (o == null)
            return "null";
        if (o instanceof String)
            return (String) o;
        else if (o instanceof Date)
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.format((Date) o);
            }
        else if (o instanceof Object[]) {
            StringBuffer result = new StringBuffer(32);
            Object[] values = (Object[]) o;
            if (values.length > 0) {
                if (values[0] != null)
                    result.append(convertToString(values[0]));
                for (int j = 1; j < values.length; j++)
                    result.append(',').append(convertToString(values[j]));
            }
            return result.toString();
        } else
            // TODO: consider number also
            return o.toString();
    }

    // TODO: implement and use for better performance
    protected StringBuffer appendConverted(StringBuffer buf, Object o) {
        return buf;
    }

    // TODO: use piped streams
    public InputStream getArticle() {
        ByteArrayInputStream result = null;

        return result;
    }

    public void getArticle(OutputStream os) throws IOException {
        // TODO: add encoding
        os.write(produceheadersPart().getBytes(encoding)); // 
        // TODO: estimate post size in lines
        os.write(NntpClient.CRLF.getBytes());
        if (content != null && content.length() > 0)
            os.write(content.getBytes(encoding));
        // asure a new line
        os.write(NntpClient.CRLF.getBytes());
        Iterator it = parts.iterator();
        while (it.hasNext()) {
            Entry e = (Entry) it.next();
            Object part = e.getValue();
            /*
            if (part instanceof byte[])
                new sun.misc.UUEncoder(e.getName()).encode((byte[]) part, os);
            else if (part instanceof InputStream) {
                new sun.misc.UUEncoder(e.getName()).encode((InputStream) part, os);
            } else if (part instanceof File) {
                byte[] buf = new byte[32 * 1024];
                InputStream is = new BufferedInputStream(new FileInputStream((File) part));
                new sun.misc.UUEncoder(e.getName()).encode(is, os);
                is.close();
            } else {
                new sun.misc.UUEncoder(e.getName()).encode(parts.toString().getBytes(), os);
            }*/
            os.write(NntpClient.CRLF.getBytes());
        }
    }

    static class Entry {
        Entry(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        Object getValue() {
            return value;
        }

        String getName() {
            return name;
        }

        String name;

        Object value;
    }
}
