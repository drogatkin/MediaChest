/* MediaChest $RCSfile: FtpFile.java,v $
 * $Id: FtpFile.java,v 1.6 2001/11/06 09:40:44 rogatkin Exp $ 
 * Copyright (C) 1999 Dmitriy Rogatkin.  All rights reserved.
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
 */
package photoorganizer.ftp;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.text.*;

public class FtpFile extends File {
    final static String PATHDELIMS = "/\\";
    public final static char ftpseparator = '/';
 
    public FtpFile(Ftp ftp, String parent) {
	super(parent);
        this.ftp = ftp;
        path = parent;
		if (path.indexOf(':') > 0) // work around of Sun's bug
			path = ""+ftpseparator;
//			new Exception("Called with path "+path).printStackTrace();
    }

    public FtpFile(Ftp ftp, String parent, String name) {
        this(ftp, parent);
        if (path.length()>0 && path.charAt(path.length()-1) == ftpseparator)
            path += name;
        else
            path += ftpseparator + name;
    }

    /**
    drwxr-x---   2 dmitry   other        1024 May  5  1998 .wastebasket

    */
    public boolean parseListLine(String line) {
	StringTokenizer st = new StringTokenizer(line, " \n\r");
	// TODO: reconsider using positional approach
	String comp;
	if (st.hasMoreTokens()) {
		comp = st.nextToken();
		if (comp.length() < 3)
			return false;
		isdirectory = comp.charAt(0) == 'd';
		canread = comp.charAt(1) == 'r';
		canwrite = comp.charAt(2) == 'w';
		if (st.hasMoreTokens()) {
			comp = st.nextToken();
			if (st.hasMoreTokens()) {
				comp = st.nextToken();
				if (st.hasMoreTokens()) {
					comp = st.nextToken();
					if (st.hasMoreTokens()) {
						try {
							length = Integer.parseInt(st.nextToken());
						} catch(NumberFormatException e) {
						}
						if (st.hasMoreTokens()) {
							comp = st.nextToken();
							if (st.hasMoreTokens()) {
								comp += " ";
								comp += st.nextToken();
								if (st.hasMoreTokens()) {
									comp += " ";
									comp += st.nextToken();
									try {
										modified = dateformat.parse(comp).getTime();
									} catch (NullPointerException e) {
									} catch (ParseException e) {
										  try {
											  modified = thisYearFormat.parse(/*thisYear+' '+*/comp).getTime();
										  } catch (ParseException e1) {
											  System.err.println("Neither of patterns: '"+thisYearFormat.toPattern()
																 +"' nor '"+dateformat.toPattern()+"' worked. \n"+e+'\n'+e1);
										  }
									}
									if (st.hasMoreTokens()) {
										comp = st.nextToken();
										if (comp.equals(".") || comp.equals(".."))
											return false;
										if (st.hasMoreTokens()) {
											comp += ' ';
											comp += st.nextToken();
										}
										if (path.length()>0 && path.charAt(path.length()-1) == ftpseparator)
											path += comp;
										else
											path += ftpseparator + comp;
										checked = true;
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	return false;
	}
    
    public boolean equals(Object obj) {
        if (!checked)
            exists();
	if (obj instanceof File)
	        return path.equals(((File)obj).getPath());
	else
        	return path.equals(obj.toString());
    }

    public boolean isDirectory() {
        if (!checked)
            exists();
	return isdirectory;
    }
    
    public boolean isFile() {
        if (!checked)
            exists();
	return isfile;
    }
    
    public boolean isAbsolute() {
        if (!checked)
            exists();
	return path.length() > 0 && path.charAt(0) == ftpseparator;
    }
    
    public boolean isHidden() {
        if (!checked)
            exists();
	return false;
    }

    public long lastModified() {
	return modified;
    }

    public long length() {
	return length;
    }
    
    public String[] list() {
	return list(null);
    }

    public String[] list(FilenameFilter filter) {
	Vector names = new Vector();
	FtpCommandReply r = ftp.list(path);
        if (r.replyData != null) {
	    FtpFile f = new FtpFile(ftp, path);
            for (int i = 0; i<r.replyData.size(); i++) {
		if (f.parseListLine((String)r.replyData.elementAt(i)))
		    if (filter == null ||  filter.accept(this, f.getName()))
		    names.addElement(f.getName());
            }
        }
	String[] result = new String[names.size()];
	names.copyInto(result);
	return result;
    }

    public File[] listFiles() {
        return this.listFiles((FileFilter)null);
    }

    interface FileFilter {
        public boolean accept(File pathname);
    }

	public File[] listFiles(FileFilter filter) {
		Vector files = new Vector();
		System.err.println("Getting list for path "+path);
		FtpCommandReply r = ftp.cwd(path);
		r = ftp.list();
		if (r.replyData != null) {
			FtpFile f;
			for (int i = 0; i<r.replyData.size(); i++) {
				System.err.println("Cr:'"+path+"'");
				f = new FtpFile(ftp, path);
				if (f.parseListLine((String)r.replyData.elementAt(i)))
					if (filter == null || filter.accept(f))
						files.addElement(f);
			}
		}
		File[] result = new File[files.size()];
		files.copyInto(result);
		return result;
	}

    public String getPath() {
        return path;
    }
    
    public String getCanonicalPath() {
        return getAbsolutePath();
    }
    
    public String getAbsolutePath() {
        if (!isAbsolute()) {
            FtpCommandReply r = ftp.pwd();
            if (r.replyMessage != null && r.replyMessage.size() == 1) {
                StringTokenizer st = new StringTokenizer((String)r.replyMessage.elementAt(0), " ");
                if (st.hasMoreTokens())
                    st.nextToken(); // skip return code
                if (st.hasMoreTokens()) {
                    String result = st.nextToken();
                    return result.substring(1, result.length()-1)+ftpseparator+path;
                }
            }
        }
        return path;
    }
    
    public String getName() {
        String result=null;
        StringTokenizer st = new StringTokenizer(path, PATHDELIMS);
        while (st.hasMoreTokens())
            result = st.nextToken();
        if (result == null || (result.length()>1 && result.charAt(1) == ':'))
            result = "";
	return result;
    }
    
    public String getParent() {
        String result=null, part=null;
        StringTokenizer st = new StringTokenizer(getAbsolutePath(), PATHDELIMS);
        while (st.hasMoreTokens()) {
            if (result == null)
                result = "";
            else
                result += part;
            part = ftpseparator+st.nextToken();
        }
        if (result != null && result.length() == 0 && isAbsolute())
            result += ftpseparator;
        return result;
    }
    
    public boolean exists() {
        FtpCommandReply r = ftp.nlst(path);
        if (r.replyData != null) {
            int n = r.replyData.size();
            isdirectory = n > 1;
            isfile = n == 1;
        } else {
            r = ftp.pwd();
            if ((r.replyCode / Ftp.CODE_SCALE) == Ftp.POSITIVE_COMPLETION_REPLY &&
                r.replyMessage != null && r.replyMessage.size() == 1) {
                StringTokenizer st = new StringTokenizer((String)r.replyMessage.elementAt(0), " ");
                if (st.hasMoreTokens())
                    st.nextToken(); // skip return code
                if (st.hasMoreTokens()) {
                    String pwd = st.nextToken("\"");
                    r = ftp.cwd(path);
                    if ((r.replyCode / Ftp.CODE_SCALE) == Ftp.POSITIVE_COMPLETION_REPLY) {
                        isdirectory = true;
                        r = ftp.cwd(pwd);
                    }
                }
            }
        }
        checked = true;
        return isdirectory || isfile;
    }

    public boolean canRead() {
	return canread;
    }
    
    public boolean canWrite() {
	return canwrite;
    }

    public int hashCode() {
        return path.hashCode() ^ 1234321;
    }
    
    public boolean mkdir() {
        FtpCommandReply r = ftp.cwd(getParent());
        if ((r.replyCode / Ftp.CODE_SCALE) >= Ftp.TRANSIENT_NEGATIVE_REPLY)
            return false;
        r = ftp.mkd(getName());
        System.err.println(""+r);
        if ((r.replyCode / Ftp.CODE_SCALE) >= Ftp.TRANSIENT_NEGATIVE_REPLY)
            return false;
        return true;
    }
    
    public boolean renameTo(File dest) {
        FtpCommandReply r = ftp.cwd(getParent());
        if ((r.replyCode / Ftp.CODE_SCALE) >= Ftp.TRANSIENT_NEGATIVE_REPLY)
            return false;
        r = ftp.rnfr(getName());
        if ((r.replyCode / Ftp.CODE_SCALE) >= Ftp.TRANSIENT_NEGATIVE_REPLY)
            return false;
        r = ftp.rnto(dest.getName());
        System.err.println(""+r);
        if ((r.replyCode / Ftp.CODE_SCALE) >= Ftp.TRANSIENT_NEGATIVE_REPLY)
            return false;
        return true;
    }

    final static SimpleDateFormat dateformat = new SimpleDateFormat("MMM dd yyyy", Locale.US);
    final static SimpleDateFormat thisYearFormat = new SimpleDateFormat(/*"yyyy */"MMM dd HH:mm", Locale.US);
    final static String thisYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR ));
    private Ftp ftp;
    private boolean checked;
    private boolean isdirectory;
    private boolean isfile;
    private boolean canread;
    private boolean canwrite;
    private String  path;
    private long    length;
    private long    modified;
}