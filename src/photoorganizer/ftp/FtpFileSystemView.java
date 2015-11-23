/* MediaChest - FtpFileSystemView
 * Copyright (C) 1999-2001 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: FtpFileSystemView.java,v 1.3 2007/07/27 02:58:04 rogatkin Exp $
 */
package photoorganizer.ftp;

import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileSystemView;

public class FtpFileSystemView extends FileSystemView {
    
    public FtpFileSystemView(Ftp ftp) {
        this.ftp = ftp;
    }

    public File createFileObject(File dir, String filename) {
        return new FtpFile(ftp, dir.getPath(), filename);
    }

    public File createFileObject(String path) {
        return new FtpFile(ftp, path);
    }

    public boolean isRoot(File f) {
        if (f instanceof FtpFile && f.getParent() == null && f.getName().length() == 0)
            return true;
        return false;
    }

    public File createNewFolder(File containingDir)
        throws IOException {
        FtpFile result = new FtpFile(ftp, containingDir.getPath()+"/NewFolder");
        if (result.mkdir())
            return result;
        return null;
    }
    
    public File[] getRoots() {
        return new File[] { new FtpFile(ftp, ""+FtpFile.ftpseparator) };
    }

    public boolean isHiddenFile(File f) {
        if (f instanceof FtpFile)
            return ((FtpFile)f).isHidden();
        return false;
    }

    public File getParentDirectory(File dir) {
        String p = dir.getParent();
        if (p == null)
            return null;
        return new FtpFile(ftp, p);
    }
	
	/*public Icon getSystemIcon(File f) {
		return super.getSystemIcon(f);
	}*/

    public File[] getFiles(File dir, boolean useFileHiding) {
        if (dir instanceof FtpFile)
            return ((FtpFile)dir).listFiles();
        else
            return new FtpFile(ftp, dir.getPath()).listFiles();
    }
	
	public Boolean isTraversable(File f) {
		return f.isDirectory()?Boolean.TRUE:Boolean.FALSE ;
	}
	
	public String getSystemDisplayName(File f) {
		System.err.println("File name '"+f.getName()+"'");
		return isRoot(f)?"Root":f.getName();
	}

    public File getHomeDirectory() {
        return new FtpFile(ftp, ftp.getHomeDirectory());
    }
	
	public boolean isFileSystemRoot(File dir) {
		return isRoot(dir);
	}
	
	public File getDefaultDirectory() {
        return getHomeDirectory();
    }
	
	public boolean isComputerNode(File dir) {
		return false;
	}
	
	public boolean isFloppyDrive(File dir) {
		return false;
	}
	
	public boolean isDrive(File dir) {
		return false;
	}
	
	protected File createFileSystemRoot(File f) {
		return new FtpFile(ftp, ""+FtpFile.ftpseparator);
	}

    private Ftp ftp;
}