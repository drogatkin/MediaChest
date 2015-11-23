/* MediaChest 
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
 *
 * $Id: JDirectoryChooser.java,v 1.4 2013/03/23 08:13:44 cvs Exp $
 */
package photoorganizer.directory;

import java.awt.Component;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import photoorganizer.*;
import photoorganizer.ftp.*;

public class JDirectoryChooser extends JFileChooser {
   
	public JDirectoryChooser (Component parent, String startfolder, 
							  FileSystemView filesystemview) {
		this(parent, startfolder, filesystemview, null, null, null, null);
	}

	public JDirectoryChooser(Component parent, String startfolder, FileSystemView filesystemview, JComponent accessory) {
		this(parent, startfolder, filesystemview, null, null, null, accessory);
	}

	public JDirectoryChooser (Component parent, String startfolder, 
							  FileSystemView filesystemview,
							  String title, String btnText, String btnTTip, JComponent accessory) {
		super(filesystemview);
		setFileSelectionMode(DIRECTORIES_ONLY);
		setMultiSelectionEnabled(false);
		File ff;
		if (startfolder == null)
			startfolder = System.getProperty("user.home");
		if (startfolder == null)
			startfolder = "/";
		if (filesystemview != null && filesystemview instanceof FtpFileSystemView) {
			//setFileSystemView(filesystemview);
			ff = filesystemview.createFileObject(startfolder);
			if (!ff.exists()) {
				ff = filesystemview.createFileObject(""+FtpFile.ftpseparator);
				ff.exists();
			}
			setCurrentDirectory(ff);
			setDialogTitle("Ftp:"+Resources.TITLE_SELECT_FOLDER);
		} else {
			ff = new File(startfolder);
			if (ff != null && ff.exists())
				setCurrentDirectory(ff);
			if (title == null)
				setDialogTitle(Resources.TITLE_SELECT_FOLDER);
			else
				setDialogTitle(title);
		}
		if (btnTTip != null)
			setApproveButtonToolTipText(btnTTip);
		if (accessory != null)
			setAccessory(accessory);
		if (APPROVE_OPTION == showDialog(parent, btnText==null?Resources.CMD_OK:btnText)) {
			directory = getSelectedFile().getAbsolutePath();
		}
	}

    public void setSelectedFile(File file) {
	// to be able to select current directory
	if (file == null) {
	    aprove_requested = true;
	    super.setSelectedFile(getCurrentDirectory());
	} else 
	    super.setSelectedFile(file);

    }

    /*public void approveSelection() {
    }*/

    public void cancelSelection() {
	if (aprove_requested)
	    approveSelection();
	else
	    super.cancelSelection();
    }

    public String getDirectory() {
        return directory;
    }
    
    public boolean accept(File f) {
        return f.isDirectory();
    }

    String directory;
    boolean aprove_requested;
}
