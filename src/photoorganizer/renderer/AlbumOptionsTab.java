/* MediaChest - AlbumSelectionPanel 
 * Copyright (C) 1999-2008 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: AlbumOptionsTab.java,v 1.21 2015/08/12 07:10:20 cvs Exp $
 */
package photoorganizer.renderer;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Persistancable;
import photoorganizer.PhotoOrganizer;
import photoorganizer.Resources;
import photoorganizer.album.Access;
import photoorganizer.album.MediaAccess;
import photoorganizer.directory.JDirectoryChooser;

public class AlbumOptionsTab extends JPanel implements ActionListener, Persistancable {
	public final static String SECNAME = "AlbumOptions";

	public final static String DRIVER = "JDBCDriver";

	public final static String DBURL = "DatabaseURL";

	public final static String USER = "User";

	public final static String PSWD = "Password";
	
	public final static String DB_HOME = "DBHome";

	public final static String USEALBUMFOLDER = "AlbumFolders";

	public final static String MOVETOFOLDER = "MoveToAlbum";

	public final static String IGNORE_DB_ERR = "IgnoreDatabaseErrors";

	public final static String ALBUMROOT = "AlbumRoot";

	public final static String USEENCODING = "UseEncoding";

	public final static String DEFAULT_JDBC_DRIVER = "org.h2.Driver"; //"sun.jdbc.odbc.JdbcOdbcDriver";

	public final static String DEFAULT_DB_DRIVER = "jdbc:h2:~/";

	public final static String DEFAULT_DB_URL = DEFAULT_DB_DRIVER + PhotoOrganizer.PROGRAMNAME;

	public AlbumOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(7, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(new SLabel(Resources.LABEL_JDBCDRV_CLASS), "0,0,3");
		add(tf_driver_class = new JTextField(), "0,1,3");
		add(new SLabel(Resources.LABEL_DATABASE_URL), "3,0,2");
		add(tf_db_url = new JTextField(), "3,1,3");
		add(new SLabel(Resources.LABEL_DB_HOME), "0,2,1");
		add(tf_db_home = new JTextField(), "0,3,2");
		tf_db_home.setEnabled(false);
		tf_db_url.getDocument().addDocumentListener(new DocumentListener() {
			private void isDerby() {
				String drv_class = tf_db_url.getText();
				tf_db_home.setEnabled(drv_class.startsWith(DEFAULT_DB_DRIVER));
			}

			public void changedUpdate(DocumentEvent arg0) {
				isDerby();
			}

			public void insertUpdate(DocumentEvent arg0) {
				isDerby();
			}

			public void removeUpdate(DocumentEvent arg0) {
				isDerby();
			}
		});
		JButton btn;
		add(btn = new JButton(Resources.CMD_BROWSE), "2,3,1");
		btn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), tf_db_home.getText(), null);
				if (dc.getDirectory() != null)				
					tf_db_home.setText(dc.getDirectory());
			}});
		add(new SLabel(Resources.LABEL_USER), "3,2,1");
		add(tf_db_user = new JTextField(), "3,3,2");
		// tf_db_user.setFont(new java.awt.Font("Arial Cyr",
		// java.awt.Font.PLAIN, 12));
		// canDisplayUpTo
		add(new SLabel(Resources.LABEL_PASSWORD), "5,2,2");
		add(tf_db_pswd = new JPasswordField(), "5,3,2");
		add(btn = new JButton(Resources.CMD_CHANGE_ALBUM_LOC), "0,4,0");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				String oldLoc = JOptionPane.showInputDialog(AlbumOptionsTab.this, Resources.LABEL_OLD_DIRALBUM,
						tf_album_root.getText());
				if (oldLoc != null) {
					JDirectoryChooser dc = new JDirectoryChooser(AlbumOptionsTab.this, null, null);
					if (dc.getDirectory() != null)
						albumpane.access.moveAlbumRoot(oldLoc, dc.getDirectory());
				}
			}
		});
		add(bt_repair = new JButton(Resources.CMD_REPAIRDB), "2,4,0");
		bt_repair.addActionListener(this);
		add(btn = new JButton(Resources.CMD_CREATEDB), "4,4,0");
		btn.addActionListener(this);

		add(btn = new JButton(Resources.CMD_IMPORTDB), "5,4,0");
		btn.addActionListener(this);

		add(btn = new JButton(Resources.CMD_INITJDBC), "6,4,0"); //check driver
		btn.addActionListener(this);
		add(tf_album_root = new JTextField(), "2,5,4");
		add(bt_browse = new JButton(Resources.CMD_BROWSE), "6,5,1");
		bt_browse.addActionListener(this);
		add(cb_maintaindir = new JCheckBox(Resources.LABEL_USE_DIRALBUM), "0,5,2");
		add(cb_nowarn = new JCheckBox(Resources.LABEL_NOJDBCWARN), "0,6,0");
		add(cb_useEnc = new JCheckBox(Resources.LABEL_ENC_FOR_CMT), "2,6,3");
		add(cb_moveto = new JCheckBox(Resources.LABEL_MOVETO_ALBUM), "5,6,0");
		cb_maintaindir.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				boolean enabled = ((AbstractButton) e.getSource()).isSelected();
				tf_album_root.setEnabled(enabled);
				bt_browse.setEnabled(enabled);
				cb_moveto.setEnabled(enabled);
				bt_repair.setEnabled(enabled);
			}
		});
		albumpane = (AlbumPane) controller.component(Controller.COMP_ALBUMPANEL);
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		Access access = albumpane.access;
		if (Resources.CMD_CREATEDB.equals(cmd)) {
			if (JOptionPane.showConfirmDialog(this, Resources.LABEL_COFIRMDROPTABLE, Resources.TITLE_COFIRMATION,
					JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				access.forgetTables();
				access.createTables();
				albumpane.invalidateTree();
			}
		} else if (Resources.CMD_INITJDBC.equals(cmd)) {
			save();
			reloadDriver();
		} else if (Resources.CMD_BROWSE.equals(cmd)) {
			JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), tf_album_root.getText(), null);
			if (dc.getDirectory() != null)				
				tf_album_root.setText(dc.getDirectory());
		} else if (Resources.CMD_REPAIRDB.equals(cmd)) {
			IniPrefs s = controller.getPrefs();
			String albumRoot = (String) s.getProperty(SECNAME, ALBUMROOT);
			if (albumRoot != null) {
				controller.setWaitCursor(this, true);
				albumpane.importAlbum(0/* access.getAlbumId(Resources.LABEL_ALBUMROOT) */, albumRoot, null, false);
				controller.setWaitCursor(this, false);
			}
		} else if (Resources.CMD_IMPORTDB.equals(cmd)) {
			JPanel accessory = new JPanel(new SpringLayout());
			// TODO use FixedGridLayout
			JTextField delim = new JTextField("\\t");
			JLabel l;
			accessory.add(l = new JLabel(Resources.LABEL_SEPARATOR));
			accessory.add(delim);
			l.setLabelFor(delim);
			JTextField df = new JTextField("yyyy-MM-dd hh:mm:ss");
			accessory.add(new JLabel(Resources.LABEL_DATEFMT));
			accessory.add(df);
			l.setLabelFor(df);
			makeCompactGrid(accessory,
                    2, 2, //rows, cols
                    6, 6,        //initX, initY
                    6, 6);
			JDirectoryChooser dc = new JDirectoryChooser(this, System.getProperty("user.home"), null, accessory);
			controller.setWaitCursor(this, true);
			if (dc.getDirectory() != null)
				try {
					MediaAccess.importTables(new File(dc.getDirectory()), specialToStr(delim.getText()), df.getText(),
							controller);
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "Can't import " + e);
				}
			controller.setWaitCursor(this, false);
			/*JFileChooser fc = new JFileChooser("");
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				
			}*/
		}
	}

	private String specialToStr(String s) {
		if ("\\t".equals(s))
			return "\t";
		return s;
	}
	
	public static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad,
			int yPad) {
		SpringLayout layout;
		try {
			layout = (SpringLayout) parent.getLayout();
		} catch (ClassCastException exc) {
			System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
			return;
		}

		//Align all cells in each column and make them the same width.
		Spring x = Spring.constant(initialX);
		for (int c = 0; c < cols; c++) {
			Spring width = Spring.constant(0);
			for (int r = 0; r < rows; r++) {
				width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
			}
			for (int r = 0; r < rows; r++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
		}

		//Align all cells in each row and make them the same height.
		Spring y = Spring.constant(initialY);
		for (int r = 0; r < rows; r++) {
			Spring height = Spring.constant(0);
			for (int c = 0; c < cols; c++) {
				height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
			}
			for (int c = 0; c < cols; c++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
		}

		//Set the parent's size.
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH, y);
		pCons.setConstraint(SpringLayout.EAST, x);
	}
	
	private static SpringLayout.Constraints getConstraintsForCell(int row, int col, Container parent, int cols) {
		SpringLayout layout = (SpringLayout) parent.getLayout();
		Component c = parent.getComponent(row * cols + col);
		return layout.getConstraints(c);
	}
	 
	private void reloadDriver() {
		// Override error handling
		int saveErrHand = IniPrefs.getInt(controller.getPrefs().getProperty(AlbumOptionsTab.SECNAME,
				AlbumOptionsTab.IGNORE_DB_ERR), 1);
		controller.getPrefs().setProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.IGNORE_DB_ERR, Resources.I_NO);
		albumpane.access.init();
		controller.getPrefs().setProperty(AlbumOptionsTab.SECNAME, AlbumOptionsTab.IGNORE_DB_ERR,
				new Integer(saveErrHand));
		albumpane.invalidateTree();
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		String p = (String) s.getProperty(SECNAME, DRIVER);
		if (p == null || p.length() == 0)
			p = DEFAULT_JDBC_DRIVER;
		tf_driver_class.setText(p);
		p = (String) s.getProperty(SECNAME, DBURL);
		if (p == null || p.length() == 0)
			p = DEFAULT_DB_URL;
		tf_db_url.setText(p);
		tf_db_home.setEnabled(p.startsWith(DEFAULT_DB_DRIVER));
		p = (String) s.getProperty(SECNAME, USER);
		if (p != null && p.length() > 0)
			tf_db_user.setText(p);
		p = (String) s.getProperty(SECNAME, PSWD);
		if (p != null)
			tf_db_pswd.setText(p);
		cb_nowarn.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, IGNORE_DB_ERR), 1) == 1);
		cb_maintaindir.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, USEALBUMFOLDER), 0) == 1);
		cb_moveto.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, MOVETOFOLDER), 0) == 1);
		cb_useEnc.setSelected(IniPrefs.getInt(s.getProperty(SECNAME, USEENCODING), 0) == 1);
		p = (String) s.getProperty(SECNAME, ALBUMROOT);
		if (p != null)
			tf_album_root.setText(p);
		p = (String) s.getProperty(SECNAME, DB_HOME);
		if (p != null)
			this.tf_db_home.setText(p);
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		boolean needReload = false;
		String p = (String) s.getProperty(SECNAME, DRIVER);
		needReload |= p != null && !p.equalsIgnoreCase(tf_driver_class.getText());
		s.setProperty(SECNAME, DRIVER, tf_driver_class.getText());
		if (!needReload) {
			p = (String) s.getProperty(SECNAME, DBURL);
			needReload |= p != null && !p.equalsIgnoreCase(tf_db_url.getText());
		}
		s.setProperty(SECNAME, DBURL, tf_db_url.getText());
		if (!needReload) {
			p = (String) s.getProperty(SECNAME, USER);
			needReload |= p != null && !p.equalsIgnoreCase(tf_db_user.getText());
		}
		s.setProperty(SECNAME, USER, tf_db_user.getText());
		if (!needReload) {
			p = (String) s.getProperty(SECNAME, PSWD);
			needReload |= p != null && !p.equalsIgnoreCase(tf_db_pswd.getText());
		}
		s.setProperty(SECNAME, PSWD, new String(tf_db_pswd.getPassword()));
		// check if something above was changed, do reload driver
		s.setProperty(SECNAME, IGNORE_DB_ERR, cb_nowarn.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, USEALBUMFOLDER, cb_maintaindir.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, MOVETOFOLDER, cb_moveto.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, USEENCODING, cb_useEnc.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, ALBUMROOT, tf_album_root.getText());
		s.setProperty(SECNAME, DB_HOME, tf_db_home.getText());
		if (needReload)
			reloadDriver();
	}

	Controller controller;

	AlbumPane albumpane;

	JTextField tf_driver_class, tf_db_url, tf_db_user, tf_album_root, tf_db_home;

	JPasswordField tf_db_pswd;

	JCheckBox cb_nowarn, cb_maintaindir, cb_moveto, cb_useEnc;

	JButton bt_browse;

	JButton bt_repair;

}