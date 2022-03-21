/* MediaChest - $RCSfile: BaseConfigurableTableModel.java,v $                           
 * Copyright (C) 2001-2002 Dmitriy Rogatkin.  All rights reserved.                    
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
 *  $Id: BaseConfigurableTableModel.java,v 1.20 2013/05/02 04:43:45 cvs Exp $            
 */
package photoorganizer.renderer;

import java.io.File;
import java.util.Date;
import java.text.DateFormat;
import javax.swing.table.AbstractTableModel;
import java.io.UnsupportedEncodingException;

import mediautil.gen.MediaInfo;
import mediautil.gen.Rational;
import photoorganizer.Resources;
import photoorganizer.Controller;
import photoorganizer.formats.*;

public abstract class BaseConfigurableTableModel extends AbstractTableModel {
	// The the returned file length for directories. 
	public static final Long ZERO = new Long(0);

	// Types and names of the columns.
	protected ColumnDescriptor[] colsDescriptor;

	protected String encoding;
	
	protected boolean applyEncoding;

	protected abstract int getDescriptionIndex();

	public void updateView(Controller controller) {
		colsDescriptor = AppearanceOptionsTab.readDescriptions(controller.getPrefs())[getDescriptionIndex()];
		encoding = MiscellaneousOptionsTab.getEncoding(controller);
		applyEncoding = AppearanceOptionsTab.needEncoding(controller);
		//revalidate();
	}

	protected Class getFirstColumnClass() {
		return null;
	}

	public int getColumnCount() {
		for (int result = colsDescriptor.length - 1; result >= 0; result--)
			if (colsDescriptor[result] != null)
				return result + 1;
		return 0;
	}

	public String getColumnName(int column) {
		return colsDescriptor[column].label;
	}

	public int getColumnAlignment(int column) {
		return colsDescriptor[column].align;
	}

	public Class getColumnClass(int column) {
		if (column == 0) {
			Class result = getFirstColumnClass();
			if (result != null)
				return result;
		}

		switch (colsDescriptor[column].type) {
		case ColumnDescriptor.STRING:
			return String.class;
		case ColumnDescriptor.BOOL:
			return Boolean.class;
		case ColumnDescriptor.NUMBER:
			return Number.class;
		case ColumnDescriptor.DATE:
			return Date.class;
		case ColumnDescriptor.INT:
			return Integer.class;
		case ColumnDescriptor.FLOAT:
			return Float.class;
		case ColumnDescriptor.RATIONAL:
			return Rational.class;
		case ColumnDescriptor.COLOR_STRING:
			//return ColoredString.class;
		}
		return String.class;
	}

	/*
	 public Object getValueAt(int row, int column) {
	 // race condition, if the row changing
	 return getValueAt(getFile(row), getInfo(row), column);	
	 }
	 
	 public Object getValueAt(Object node, int column) {
	 return getValueAt(getFile(node), getInfo(node), column);
	 }*/

	protected Object getValueAt(File file, MediaInfo info, int column) {
		ColumnDescriptor descriptor = colsDescriptor[column];
		String methods[] = descriptor.attributes;
		Object result = null;
		if (methods == null)
			return result;
		if (methods.length > 0 && info != null) {
			for (int i = 0; i < methods.length && i < 2; i++) {
				if (methods[i] != null && methods[i].length() > 0)
					result = getInfoAttribute(info, methods[i], descriptor.type);
				if (result != null)
					return result;
			}
		}
		if (methods.length > 2) {
			if (Resources.LIST_NAME.equals(methods[2])) {
				result = file.getName();
				try {
					if (encoding != null)
						result = new String(((String) result).getBytes("ISO8859_1"), encoding);
				} catch (UnsupportedEncodingException uee) {
				}
			} else if (Resources.LIST_TYPE.equals(methods[2]))
				result = file.isDirectory() ? Resources.LABEL_DIRECTORY : Resources.LABEL_FILE;
			else if (Resources.LIST_LENGTH.equals(methods[2]))
				result = file.isFile() ? new Long(file.length()) : ZERO;
			else if (Resources.LIST_DATE.equals(methods[2])) {
				if (descriptor.type == ColumnDescriptor.DATE)
					result = new Date(file.lastModified());
				else
					result = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(file.lastModified()));
			} else if (Resources.LIST_ATTRS.equals(methods[2]))
				result = "" + (file.isDirectory() ? "d" : ".") + (file.isFile() ? "f" : ".")
						+ (file.isHidden() ? "h" : ".");
		}

		return result;
	}

	protected Object getInfoAttribute(MediaInfo info, String name, int type) {
		if (name == null || name.length() == 0) {
			new Exception("Empty attribute requested").printStackTrace();
			return null;
		}
		Object result = null;
		try {
			switch (type) {
			case ColumnDescriptor.STRING:
			case ColumnDescriptor.NUMBER:
			case ColumnDescriptor.COLOR_STRING:
				if (MediaInfo.GENRE.equals(name))
					try {
						result = MP3.findGenre(info);
						break;
					} catch (Exception e) {
					}
				result = info.getAttribute(name);
				break;
			case ColumnDescriptor.BOOL:
				result = info.getBoolAttribute(name) ? Boolean.TRUE : Boolean.FALSE;
				break;
			case ColumnDescriptor.DATE:
				result = info.getAttribute(name);
				if (result instanceof Date)
					result = DateFormat.getDateInstance(DateFormat.SHORT).format(result);
				break;
			case ColumnDescriptor.INT:
				result = new Integer(info.getIntAttribute(name));
				break;
			case ColumnDescriptor.FLOAT:
				result = new Float(info.getFloatAttribute(name));
				break;
			//case ColumnDescriptor.COLOR_STRING:
			//	result = new ColoredString((String)info.getAttribute(name), Color.black );
			//	break;
			}
		} catch (Exception e) {
			if (__debug)
				System.err.printf("Attribute %s of %d read failed %s%n", name, type, e);
		}
		return result;
	}

	public Object getElementAt(int row) {
		return null;
	}

	private final static boolean __debug = false;
}
