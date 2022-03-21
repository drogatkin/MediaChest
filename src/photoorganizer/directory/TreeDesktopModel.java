/* MediaChest - $RCSfile: TreeDesktopModel.java,v $                                                  
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL     
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT         
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY  
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF     
 *  SUCH DAMAGE.                                                               
 *                                                                             
 *  Visit http://mediachest.sourceforge.net to get the latest information        
 *  about Rogatkin's products.                                                 
 *  $Id: TreeDesktopModel.java,v 1.10 2012/10/18 06:59:01 cvs Exp $                      
 */

package photoorganizer.directory;

import java.awt.Component;
import java.io.File;
import java.io.UnsupportedEncodingException;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;

public class TreeDesktopModel extends DefaultTreeModel {
	photoorganizer.directory.FileSystem fileSystem;

	java.io.FileFilter fileFilter;

	String encoding;

	public TreeDesktopModel(String encoding) {
		this((java.io.FileFilter) null);
		this.encoding = encoding;
	}

	public TreeDesktopModel(java.io.FileFilter fileFilter) {
		super(null);
		reset(fileFilter);
	}

	public void reset(java.io.FileFilter fileFilter) {
		this.fileFilter = fileFilter;
		fileSystem = new photoorganizer.directory.FileSystem();
	}

	public TreeCellRenderer adoptCellRenderer(TreeCellRenderer renderer) {
		return new FileSystemCellRenderer(renderer);
	}

	public Object getChild(Object parent, int index) {
		System.err.println("Looking for child of " + parent + " index " + index);
		File[] result;
		if (parent == this)
			result = fileSystem.getRoots();
		else {
			result = fileSystem.getFiles((File) parent, false);
			int i;
			for (i = 0; i < result.length; i++) {
				if (result[i].isDirectory())
					index--;
				if (index == -1) {
					index = i;
					break;
				}
			}
			if (i > result.length)
				return null;
		}
		// System.err.println("Res l: "+result.length+" index "+index);
		if (index < result.length)
			return result[index];
		return null;
	}

	public int getChildCount(Object parent) {
		if (parent == this)
			return fileSystem.getRoots().length;
		else {
			File[] files = fileSystem.getFiles((File) parent, false);
			int result = 0;
			for (int i = 0; i < files.length; i++)
				if (files[i].isDirectory())
					result++;
			return result;
		}
	}

	public int getIndexOfChild(Object parent, Object child) {
		File[] result;
		if (parent == this)
			result = fileSystem.getRoots();
		else
			result = fileSystem.getFiles((File) parent, false);
		int index = -1;
		for (int i = 0; i < result.length; i++) {
			if (result[i].isDirectory())
				index++;
			if (result[i].equals(child))
				return index;
		}
		return -1;
	}

	public Object getRoot() {
		return this;
	}

	public boolean isLeaf(Object node) {
		if (node == this)
			return false;
		return !((File) node).isDirectory();
	}

	class FileSystemCellRenderer extends JLabel implements TreeCellRenderer {
		TreeCellRenderer superRenderer;

		FileSystemCellRenderer(TreeCellRenderer renderer) {
			superRenderer = renderer;
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			if (value instanceof File) {
				return ownDraw((File) value, fileSystem.getSystemIcon((File) value));
			}
			return superRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		}

		protected Component ownDraw(File file, Icon icon) {
			String tl = file.getName().length() > 0 && file.toString().endsWith(file.getName()) ? file.getName() : file
					.toString();
			try {
				if (encoding != null)
					tl = new String(tl.getBytes(), encoding);
			} catch (UnsupportedEncodingException uee) {
			}

			if (superRenderer instanceof JLabel) {
				((JLabel) superRenderer).setText(tl);
				((JLabel) superRenderer).setIcon(icon);
				return (JLabel) superRenderer;
			} else {
				setText(tl);
				setIcon(icon);
				return this;
			}
		}
	}
}
