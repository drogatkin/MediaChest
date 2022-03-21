/* MediaChest 
 * Copyright (C) 1999-2002 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: FileSystemModel.java,v 1.14 2013/06/07 01:13:56 cvs Exp $
 */
/*
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 */
package photoorganizer.directory;

import java.io.File;
import java.util.Vector;
import mediautil.gen.MediaInfo;
import photoorganizer.renderer.AppearanceOptionsTab;
import photoorganizer.Controller;
/**
 * FileSystemModel is a TreeTableModel representing a hierarchical file 
 * system. Nodes in the FileSystemModel are FileNodes which, when they 
 * are directory nodes, cache their children to avoid repeatedly querying 
 * the real file system. 
 * This code is based on Philip Milne, and Scott Violet example.
 *  
 *  
 */

public class FileSystemModel extends AbstractTreeTableModel 
	implements TreeTableModel {

	public boolean unixfilefystem = File.separatorChar == '/';

	public FileSystemModel(Controller controller) {
		updateView(controller);		
		//setRoot(new FileNode(findFirstDrive(), encoding, applyEncoding));
	}
	
	protected int getDescriptionIndex() {
		return AppearanceOptionsTab.BROWSE_VIEW;
	}
	
	protected Class getFirstColumnClass() {
		return TreeTableModel.class;
	}

	public int getRowCount() {
		return 0;
	}
	
	public Object getValueAt(int row, int column) {
		return null;
	}

	File findFirstDrive() {
		File[] drivers = getRoots();
		if (drivers.length > 1)
			return drivers[1];
		return null;
	}

	public File[] getRoots() {
		if (unixfilefystem) {
			File[] roots = new File[2];
			roots[1] = new File(File.separator);
			if(roots[1].exists() && roots[1].isDirectory()) {
				return roots;
			}
			return null;
		}
		// windows
		Vector rootsVector = new Vector();

		// Create the A: drive whether it is mounted or not
		rootsVector.addElement(new WindowsFloppy());

		// Run through all possible mount points and check
		// for their existance.
		// Z isn't limitation of OS
		for (char c = 'C'; c <= 'Z'; c++) {
			char device[] = {c, ':', File.separatorChar};
			String deviceName = new String(device);
			File deviceFile = new File(deviceName);
			if (deviceFile.exists()) {
				rootsVector.addElement(deviceFile);
			}
		}
		File[] roots = new File[rootsVector.size()];
		rootsVector.copyInto(roots);
		return roots;
	}

	class WindowsFloppy extends File {
		public WindowsFloppy() {
			// it can be drive B too
			super("A" + ":" + File.separator);
		}

		public boolean isDirectory() {
			return true;
		};
	}

    //
    // Some convenience methods. 
    //

	protected File getFile(Object node) {
		FileNode fileNode = ((FileNode)node); 
		return fileNode.getFile();       
	}
	
	protected MediaInfo getInfo(Object node) {
		FileNode fileNode = ((FileNode)node); 
		return fileNode.getFormat()!=null?fileNode.getFormat().getMediaInfo():null;       
	}
	
	protected Object[] getChildren(Object node) {
		FileNode fileNode = ((FileNode)node); 
		return fileNode.getChildren(); 
	}

	//
	// The TreeModel interface
	//

	public int getChildCount(Object node) { 
		Object[] children = getChildren(node); 
		return (children == null) ? 0 : children.length;
	}

	public Object getChild(Object node, int i) { 
		return getChildren(node)[i]; 
	}

	// The superclass's implementation would work, but this is more efficient. 
	public boolean isLeaf(Object node) { return !getFile(node).isDirectory(); }

	//
	//  The TreeTableNode interface. 
	//

	public Object getValueAt(Object node, int column) {
		return getValueAt(getFile(node), getInfo(node), column);
	}	
}



