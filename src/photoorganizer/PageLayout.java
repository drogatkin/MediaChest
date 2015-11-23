/* PhotoOrganizer - $RCSfile: PageLayout.java,v $                               
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
 *  $Id: PageLayout.java,v 1.7 2012/10/18 06:59:01 cvs Exp $                
 */
package photoorganizer;

import java.awt.print.PageFormat;
import java.awt.print.Paper;

import javax.swing.JOptionPane;

import org.aldan3.util.IniPrefs;

public class PageLayout {
	public final static String SECNAME = "PageLayout";

	public final static String SCHEMA = "Schema";

	public final static String ORIENTATION = "Orientation";

	public final static String PREPRINT_COMMENTS = "PreprintComments";

	public final static String HEIGHT = "Height";

	public final static String WIDTH = "Width";

	public final static String IMAGEABLEHEIGHT = "ImageableHeight";

	public final static String IMAGEABLEWIDTH = "ImageableWidth";

	public final static String IMAGEABLEX = "ImageableX";

	public final static String IMAGEABLEY = "ImageableY";

	public final static String DEFAULT_LAYOUT = Resources.LABEL_4_6_CUT;

	public final static LayoutDef[] LAYOUT_DEFS = {
			new LayoutDef(Resources.LABEL_FULL_PG_FAX, new RowDef[] { new RowDef(1, 8, 10, true) }),
			new LayoutDef(Resources.LABEL_FULL_PG_PHOTO, new RowDef[] { new RowDef(1, 8, 10, true) }),
			new LayoutDef(Resources.LABEL_CONTACT_SHEET, new RowDef[] { new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false),
					new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false), new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false),
					new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false), new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false),
					new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false), new RowDef(5, 8.0 / 5.0, 10.0 / 7.0, false) }),
			new LayoutDef(Resources.LABEL_8_10_CUT, new RowDef[] { new RowDef(1, 8, 10, true) }),
			new LayoutDef(Resources.LABEL_5_7_CUT, new RowDef[] { new RowDef(1, 7, 5, false),
					new RowDef(1, 7, 5, false) }),
			new LayoutDef(Resources.LABEL_4_6_CUT,
					new RowDef[] { new RowDef(2, 4, 6, true), new RowDef(1, 6, 4, false) }),
			new LayoutDef(Resources.LABEL_4_6_ALBUM, new RowDef[] { new RowDef(1, 6, 4, false),
					new RowDef(1, 6, 4, false) }),
			new LayoutDef(Resources.LABEL_35_5_CUT, new RowDef[] { new RowDef(2, 3.5, 5, true),
					new RowDef(2, 3.5, 5, true) }),
			new LayoutDef(Resources.LABEL_WALLET_PRINT, new RowDef[] { new RowDef(3, 8.0 / 3.0, 10.0 / 3.0, true),
					new RowDef(3, 8.0 / 3.0, 10.0 / 3.0, true), new RowDef(3, 8.0 / 3.0, 10.0 / 3.0, true) }) };

	public static String[] getLayoutNames() {
		String result[] = new String[LAYOUT_DEFS.length];
		for (int i = 0; i < LAYOUT_DEFS.length; i++)
			result[i] = LAYOUT_DEFS[i].name;
		return result;
	}

	public static boolean layoutDialog(IniPrefs s) {
		String result = (String) s.getProperty(SECNAME, SCHEMA);
		if (result == null)
			result = DEFAULT_LAYOUT;
		result = (String) JOptionPane.showInputDialog(null, Resources.LABEL_PRINT_OPTIONS,
				Resources.TITLE_PRINT_LAYOUT, JOptionPane.QUESTION_MESSAGE, null, getLayoutNames(), result);
		if (result != null) {
			try {
				s.setProperty(SECNAME, SCHEMA, result);
			} catch (Exception e) {
			}
			return true;
		}
		return false;
	}

	public static String getLayoutSchemaName(IniPrefs s) {
		try {
			String result = (String) s.getProperty(SECNAME, SCHEMA);
			for (int i = 0; i < LAYOUT_DEFS.length; i++)
				if (LAYOUT_DEFS[i].name.equals(result))
					return result;
		} catch (Exception e) {
		}
		return DEFAULT_LAYOUT;
	}

	public static int getLayoutSchemaIndex(IniPrefs s) {
		try {
			String result = (String) s.getProperty(SECNAME, SCHEMA);
			for (int i = 0; i < LAYOUT_DEFS.length; i++)
				if (LAYOUT_DEFS[i].name.equals(result))
					return i;
		} catch (Exception e) {
		}
		for (int i = 0; i < LAYOUT_DEFS.length; i++)
			if (LAYOUT_DEFS[i].name.equals(DEFAULT_LAYOUT))
				return i;
		// should be an exception here
		return -1;
	}

	public static LayoutDef getLayoutSchema(IniPrefs s) {
		try {
			String result = (String) s.getProperty(SECNAME, SCHEMA);
			for (int i = 0; i < LAYOUT_DEFS.length; i++)
				if (LAYOUT_DEFS[i].name.equals(result))
					return LAYOUT_DEFS[i];
		} catch (Exception e) {
		}
		for (int i = 0; i < LAYOUT_DEFS.length; i++)
			if (LAYOUT_DEFS[i].name.equals(DEFAULT_LAYOUT))
				return LAYOUT_DEFS[i];
		// should be an exception here
		return null;
	}

	public static int getImagesOnPage(IniPrefs s) {
		return getLayoutSchema(s).getNumImages();
	}

	// TODO: add orientation
	public static void save(IniPrefs s, PageFormat page) {
		s.setProperty(SECNAME, HEIGHT, "" + page.getHeight());
		s.setProperty(SECNAME, WIDTH, "" + page.getWidth());
		s.setProperty(SECNAME, IMAGEABLEHEIGHT, "" + page.getImageableHeight());
		s.setProperty(SECNAME, IMAGEABLEWIDTH, "" + page.getImageableWidth());
		s.setProperty(SECNAME, IMAGEABLEX, "" + page.getImageableX());
		s.setProperty(SECNAME, IMAGEABLEY, "" + page.getImageableY());
	}

	public static void load(IniPrefs s, PageFormat page) {
		Paper paper = new Paper();
		try {
			paper.setSize(Double.valueOf((String) s.getProperty(SECNAME, WIDTH)).doubleValue(), Double.valueOf(
					(String) s.getProperty(SECNAME, HEIGHT)).doubleValue());
			paper.setImageableArea(Double.valueOf((String) s.getProperty(SECNAME, IMAGEABLEX)).doubleValue(), Double
					.valueOf((String) s.getProperty(SECNAME, IMAGEABLEY)).doubleValue(), Double.valueOf(
					(String) s.getProperty(SECNAME, IMAGEABLEWIDTH)).doubleValue(), Double.valueOf(
					(String) s.getProperty(SECNAME, IMAGEABLEHEIGHT)).doubleValue());
			page.setPaper(paper);
		} catch (Exception e) {
			e.printStackTrace();
			// not correct data
		}
	}

	public static class RowDef {
		public int num;

		public double width;

		public double height;

		public boolean orient;

		public RowDef(int num, double width, double height, boolean rotated) {
			this.num = num;
			this.width = width;
			this.height = height;
			this.orient = rotated;
		}
	}

	public static class LayoutDef {
		public String name;

		public RowDef[] rowDefs; // in form Dimension, num, orient

		public LayoutDef(String name, RowDef[] rowDefs) {
			this.name = name;
			this.rowDefs = rowDefs;
		}

		public int getNumImages() {
			int result = 0;
			for (int i = 0; i < rowDefs.length; i++)
				result += rowDefs[i].num;
			return result;
		}
	}
}
