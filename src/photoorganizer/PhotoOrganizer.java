/** MediaChest 
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: PhotoOrganizer.java,v 1.165 2014/11/25 04:12:23 cvs Exp $
 */
package photoorganizer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import org.aldan3.model.ServiceProvider;
import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.directory.FileSystemModel;
import photoorganizer.directory.JTreeTable;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.ipod.BaseItem;
import photoorganizer.ird.IrdReceiver;
import photoorganizer.renderer.AppearanceOptionsTab;
import photoorganizer.renderer.IpodOptionsTab;
import photoorganizer.renderer.IpodPane;
import photoorganizer.renderer.MediaPlayerPanel;
import photoorganizer.renderer.MiscellaneousOptionsTab;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.PluginOptionsTab;
import photoorganizer.renderer.RenameOptionsTab;
import photoorganizer.renderer.RipperPanel;
import photoorganizer.renderer.SearchPanel;
import photoorganizer.renderer.StatusBar;
import photoorganizer.renderer.ToolbarPlayer;
import photoorganizer.renderer.TransformOptionsTab;

public class PhotoOrganizer extends JFrame implements /* Program, */IrdControllable {

	public static final String PROGRAMNAME = "MediaChest";

	public static final String VERSION = "version 2.3";

	public static final int BUILD = 218;

	public static final String COPYRIGHT = "Copyright \u00a9 1999-2023 Dmitriy Rogatkin";

	public static final String DEDICATED = "For my wife Olga";

	public static final String HOME_PAGE = "http://mediachest.sourceforge.net/index.html";

	public static final String BASE_URL = "http://mediachest.sourceforge.net/";

	public static final String KEY_BROWSE = "Browse";

	public static final String KEY_SELECTION = "Selection";

	public static final String KEY_ALBUM = "Album";

	// static final String [] IRD_CMDS = {KEY_BROWSE, KEY_SELECTION, KEY_ALBUM};

	static final String SECNAME = PROGRAMNAME;

	public static final String BOUNDS = "Bounds";

	public static final String DIVIDERDIR = "DividerBrowsePos";

	public static final String DIVIDERCOL = "DividerCollectionPos";

	public static final String DIVIDERALBUM = "DividerAlbumPos";

	public static final String DIVIDERRIPPER = "DividerRipperPos";

	public static final String DIVIDERIPOD = "DivideriPodPos";

	public static final String TB_ORIENT = "ToolBarOrientation";

	public static final String DIVIDERIPODTREE = "IpodTreeSplitterPos";

	public static void main(String[] args) {
		new PhotoOrganizer();
	}

	public PhotoOrganizer() {
		FlashWindow fw = new FlashWindow();
		// pros
		// can expand component to access to new component without changing
		// constructors
		// or adding new methods
		// cons
		// longer access to other component,
		// class cast isn't reliable
		// the order of creation can be important, if we access a controller in
		// constructor
		// to get an access to other components
		controller = new Controller(this);
		try {
			System.setErr(new PrintStream(Controller.getWSAwareOutStream(controller.getHomeDir(), PROGRAMNAME + ".log"), true, System.getProperty("mediachest.log.encoding", Controller.ISO_8859_1)));
		} catch (IOException e) {
			System.err.println(PROGRAMNAME + ": Can not redirect error stream.");
		}
		controller.updateCaption(null);
		setIconImage(controller.getMainIcon());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (controller.iconize() == false)
					controller.exit();
				else
					setVisible(false);
				super.windowClosing(e);
			}

			public void windowClosed(WindowEvent we) {
				System.exit(0);
			}

		});

		irdKeyMap = new Hashtable();
		boolean iniStat = true;
		String packageName = getClass().getPackage().getName();
		iniStat &= controller.add(packageName + ".renderer.StatusBar", Controller.COMP_STATUSBAR);
		iniStat &= controller.add(packageName + ".renderer.PhotoImagePanel", Controller.COMP_IMAGEPANEL);
		iniStat &= controller.add(packageName + ".renderer.PhotoImagePanel", Controller.COMP_IMAGECOLLCTPANEL);
		iniStat &= controller.add(packageName + ".renderer.PhotoImagePanel", Controller.COMP_IMAGEALBUMPANEL);
		iniStat &= controller.add(packageName + ".renderer.AlbumPane", Controller.COMP_ALBUMPANEL);
		iniStat &= controller
				.add(packageName + ".renderer.CollectionThumbnailsPanel", Controller.COMP_THUMBCOLLCTPANEL);
		iniStat &= controller.add(packageName + ".renderer.IpodPane", Controller.COMP_IPODPANEL);
		iniStat &= controller.add(packageName + ".renderer.PlayListPane", Controller.COMP_PLAYLISTPANEL);
		irdKeyMap.put(KEY_ALBUM, Controller.COMP_ALBUMPANEL);
		iniStat &= controller.add(packageName + ".renderer.PhotoCollectionPanel", Controller.COMP_COLLECTION);
		irdKeyMap.put(KEY_SELECTION, Controller.COMP_COLLECTION);
		iniStat &= controller.add(packageName + ".renderer.WebAlbumPane", Controller.COMP_WEBALBUMPANEL);
		iniStat &= controller.add(packageName + ".renderer.ThumbnailsPanel", Controller.COMP_THUMBPANEL);
		controller.add(new JTreeTable(new FileSystemModel(controller), controller), Controller.COMP_DIRTREE);
		irdKeyMap.put(KEY_BROWSE, Controller.COMP_DIRTREE);
		iniStat &= controller.add(packageName + ".renderer.AlbumThumbnailsPanel", Controller.COMP_ALBUMTHUMBPANEL);
		iniStat &= controller.add(packageName + ".renderer.RipperPanel", Controller.COMP_RIPPERPANEL);

		if (!iniStat) {
			JOptionPane.showMessageDialog(null, Resources.LABEL_INIT_FAILED, Resources.TITLE_ERROR,
					JOptionPane.CLOSED_OPTION + JOptionPane.WARNING_MESSAGE);
			new Upgrader(controller);
			System.exit(255);
		}
		// TODO: switch to java.util.prefs.Preferences
		IniPrefs s = controller.prefs;
		mediautil.gen.Log.debugLevel = IniPrefs.getInt(s.getProperty("MediaUtil", "LogLevel"), 0);
		Object a = s.getProperty(AppearanceOptionsTab.SECNAME, AppearanceOptionsTab.LAF);
		try {
			// com.incors.plaf.kunststoff.KunststoffLookAndFeel
			String lnf_class = UIManager.getSystemLookAndFeelClassName();
			for (UIManager.LookAndFeelInfo lnfis: UIManager.getInstalledLookAndFeels())
				if (lnfis.getName().equals(a))
					lnf_class = lnfis.getClassName(); 
			if (lnf_class != null && lnf_class.length() > 0)
				UIManager.setLookAndFeel(lnf_class);
		} catch (UnsupportedLookAndFeelException ulaf) {
			System.err.println(a + " L&F is not supported.");
		} catch (Exception e) {
			System.err.println("Cannot instantiate L&F '" + a + "' " + e);
		}
		int split = IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.SPLITVERT),
				1) == 1 ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;

		final JTabbedPane gentabpane = new JTabbedPane(IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME,
				MiscellaneousOptionsTab.TABPOS), SwingConstants.BOTTOM));
		JTabbedPane uptabbedpane;
		JScrollPane botmpane;
		JSplitPane splitPane;
		// Album tab
		botmpane = new JScrollPane((Component) controller.component(Controller.COMP_ALBUMTHUMBPANEL));
		uptabbedpane = new JTabbedPane(SwingConstants.LEFT);
		uptabbedpane.insertTab(Resources.TAB_ALBUM, (Icon) null, new JScrollPane((Component) controller
				.component(Controller.COMP_ALBUMPANEL)),
		// new JSplitPane(split, true,
				// new
				// JScrollPane(controller.component(Controller.COMP_ALBUMPANEL)),
				// new
				// JScrollPane(controller.component(Controller.COMP_WEBALBUMPANEL))),
				Resources.TTIP_ALBUMTAB, 0);

		uptabbedpane.insertTab(Resources.TAB_IMAGE, (Icon) null, (Component) controller
				.component(Controller.COMP_IMAGEALBUMPANEL), Resources.TTIP_IMAGETAB, 1);

		gentabpane.insertTab(Resources.TAB_ALBUM, (Icon) null, splitPane = new JSplitPane(split, true, uptabbedpane,
				botmpane), Resources.TTIP_ALBUM, Resources.COMP_ALBUM_IDX);
		splitPane.setOneTouchExpandable(true);

		// Selection tab
		uptabbedpane = new JTabbedPane(SwingConstants.LEFT);
		uptabbedpane.insertTab(Resources.TAB_COLLECTION, (Icon) null, new JScrollPane((Component) controller
				.component(Controller.COMP_COLLECTION)), Resources.TTIP_COLLECTTAB, 0);
		uptabbedpane.insertTab(Resources.TAB_IMAGE, (Icon) null, (Component) controller
				.component(Controller.COMP_IMAGECOLLCTPANEL), Resources.TTIP_IMAGETAB, 1);

		botmpane = new JScrollPane((Component) controller.component(Controller.COMP_THUMBCOLLCTPANEL));

		gentabpane.insertTab(Resources.TAB_COLLECTION, (Icon) null, splitPane = new JSplitPane(split, true,
				uptabbedpane, botmpane), Resources.TTIP_COLLECTLIST, Resources.COMP_SELECTION_IDX);
		splitPane.setOneTouchExpandable(true);

		// Browse tab
		uptabbedpane = new JTabbedPane(SwingConstants.LEFT);
		uptabbedpane.insertTab(Resources.TAB_DIRECTORY, (Icon) null, new JScrollPane((Component) controller
				.component(Controller.COMP_DIRTREE)), Resources.TTIP_DIRECTORYTAB, 0);
		uptabbedpane.insertTab(Resources.TAB_IMAGE, (Icon) null, (Component) controller
				.component(Controller.COMP_IMAGEPANEL), Resources.TTIP_IMAGETAB, 1);

		/*
		 * JSplitPane upsplittedpane = new
		 * JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new
		 * JScrollPane(controller.component(Controller.COMP_DIRTREE)),
		 * controller.component(Controller.COMP_IMAGEPANEL));
		 */

		botmpane = new JScrollPane((Component) controller.component(Controller.COMP_THUMBPANEL));

		gentabpane.insertTab(Resources.TAB_BROWSE, (Icon) null, new JSplitPane(split, true, /* upsplittedpane */
		uptabbedpane, botmpane), Resources.TTIP_BROWSETAB, Resources.COMP_BROWSE_IDX);

		// Ripper tab
		gentabpane.insertTab(Resources.TAB_RIPPER, (Icon) null, (Component) controller
				.component(Controller.COMP_RIPPERPANEL), Resources.TTIP_RIPPER, Resources.COMP_RIPPER_IDX);

		// iPod tab
		JSplitPane splitPane2 = new JSplitPane(split == JSplitPane.HORIZONTAL_SPLIT ? JSplitPane.VERTICAL_SPLIT
				: JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(((IpodPane) controller
				.component(Controller.COMP_IPODPANEL)).mirror()), new JScrollPane((Component) controller
				.component(Controller.COMP_IPODPANEL)));
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(splitPane2, BorderLayout.CENTER);
		if (IniPrefs.getInt(s.getProperty(IpodOptionsTab.SECNAME, IpodOptionsTab.ARTWORK_PREVIEW), 0) == 1)
			p.add(new JLabel((Icon)null, SwingConstants.CENTER) {
				public Dimension getPreferredSize() {
					return BaseItem.NOWPLAY_SIZE;
				}
			}, split == JSplitPane.HORIZONTAL_SPLIT ? BorderLayout.SOUTH : BorderLayout.EAST);
		gentabpane.insertTab(Resources.TAB_IPOD, (Icon) null, splitPane = new JSplitPane(split, true, p,
				new JScrollPane((Component) controller.component(Controller.COMP_PLAYLISTPANEL))), Resources.TTIP_IPOD,
				Resources.COMP_IPOD_IDX);
		splitPane2.setOneTouchExpandable(true);
		// ((Component)controller.component(Controller.COMP_IPODPANEL)).setParent(null);
		// splitPane2.setLeftComponent(controller.component(Controller.COMP_IPODPANEL));
		splitPane.setOneTouchExpandable(true);

		// add ext panels from the prepared list
		// check for custom tab panels and add them also
		boolean pluginsEnabled = IniPrefs.getInt(s.getProperty(PluginOptionsTab.SECNAME, PluginOptionsTab.ALLDISABLED),
				0) == 0;
		for (int i = 1; pluginsEnabled; i++) {

			String extPanelName = (String) s.getProperty(PluginOptionsTab.SECNAME, PluginOptionsTab.CLASS + i);
			if (extPanelName == null)
				break;
			if (IniPrefs.getInt(s.getProperty(PluginOptionsTab.SECNAME, PluginOptionsTab.DISABLED + 1), 0) == 1)
				continue; // the plugin disabled
			String extPanelId = (String) s.getProperty(PluginOptionsTab.SECNAME, PluginOptionsTab.NAME + i);
			if (extPanelId == null)
				extPanelId = "plug-in" + i;
			if (controller.add(extPanelName, extPanelId)) // put the name to
															// ext panels list
				gentabpane.insertTab(extPanelId, (Icon) null, (Component) controller.component(extPanelId), (String) s
						.getProperty(PluginOptionsTab.SECNAME, PluginOptionsTab.TOOLTIP + i), gentabpane.getTabCount());
			else
				System.err.println("The plug-in " + extPanelName + " has not been added, see exceptions above.");
		}

		gentabpane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JComponent c = null;
				try {
					c = ((PhotoPlugin) gentabpane.getSelectedComponent()).getStatusBar();
				} catch (ClassCastException cce) {
				}
				((StatusBar) controller.component(Controller.COMP_STATUSBAR)).setCustomStatus(c);
				// TODO: make it more robust, since ctabs can be shuffled
				// controller.getUiUpdater().notify(Resources.COMP_IPOD_IDX ==
				// gentabpane.getSelectedIndex(),UiUpdater.IPODVIEW_SELECTED);
				// todo: add menu updating also
			}
		});

		getContentPane().add(gentabpane, BorderLayout.CENTER);
		pack();
		load();
		// TODO make it a method initIRD(
		IrdReceiver irdReceiver;
		try {
			(irdReceiver = new IrdReceiver()).init(/* controller */);
			if (irdReceiver.getPort() != null) {
				controller.add(irdReceiver, Controller.COMP_REMOTERECEIVER);
				irdReceiver.register(this);
				irdReceiver.register(MediaPlayerPanel.getIrdControllable(controller));
				irdReceiver.register((IrdControllable) controller.component(Controller.COMP_ALBUMPANEL));
				irdReceiver.register((IrdControllable) controller.component(Controller.COMP_COLLECTION));
				irdReceiver.register((IrdControllable) controller.component(Controller.COMP_DIRTREE));
				for (int i = 0; i < gentabpane.getTabCount(); i++) {
					Object o = gentabpane.getComponentAt(i);
					if (o instanceof IrdControllable)
						irdReceiver.register((IrdControllable) o);
				}
				irdReceiver.standBy(s);
				irdReceiver.setOnTop(this);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println("Exception " + t
					+ " in initializing of IRD receiver, the functionality will be suspended.");
		}

		setVisible(true);
		controller.load();
		controller.getUiUpdater().reset();
		fw.dispose();
		new Upgrader(controller);
	}

	public void setTitle(String title) {
		super.setTitle(title + " - " + PhotoOrganizer.PROGRAMNAME + " " + PhotoOrganizer.VERSION);
	}

	public String getName() {
		return PROGRAMNAME;
	}

	public String toString() {
		return getName();
	}

	public String getVersion() {
		return VERSION;
	}

	public void save() {
		IniPrefs s = controller.prefs;
		Rectangle d = getBounds();
		Integer[] b = new Integer[4];
		b[0] = new Integer(d.x);
		b[1] = new Integer(d.y);
		b[2] = new Integer(d.width);
		b[3] = new Integer(d.height);
		s.setProperty(SECNAME, BOUNDS, b);
		// TODO: consider loop here
		JSplitPane sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0))
				.getComponentAt(Resources.COMP_BROWSE_IDX);
		s.setProperty(SECNAME, DIVIDERDIR, new Integer(sp.getDividerLocation()));
		sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(Resources.COMP_SELECTION_IDX);
		s.setProperty(SECNAME, DIVIDERCOL, new Integer(sp.getDividerLocation()));
		sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(Resources.COMP_ALBUM_IDX);
		s.setProperty(SECNAME, DIVIDERALBUM, new Integer(sp.getDividerLocation()));
		sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(Resources.COMP_RIPPER_IDX);
		s.setProperty(SECNAME, DIVIDERRIPPER, new Integer(sp.getDividerLocation()));
		sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(Resources.COMP_IPOD_IDX);
		s.setProperty(SECNAME, DIVIDERIPOD, new Integer(sp.getDividerLocation()));
		sp = (JSplitPane) ((JPanel) sp.getLeftComponent()).getComponent(0);
		s.setProperty(SECNAME, DIVIDERIPODTREE, new Integer(sp.getDividerLocation()));
		s.setProperty(RenameOptionsTab.SECNAME, RenameOptionsTab.COUNTER, new Integer(FileNameFormat.counter));
		Component c;
		for (int i = 0; i < getContentPane().getComponentCount(); i++)
			if ((c = getContentPane().getComponent(i)) instanceof JToolBar)
				s.setProperty(SECNAME, TB_ORIENT, new Integer(((JToolBar) c).getOrientation()));
	}

	public void load() {
		IniPrefs s = controller.prefs;
		Object[] b = (Object[]) s.getProperty(SECNAME, BOUNDS);
		if (b != null && b.length == 4 && b[0] instanceof Integer) {
			setBounds(((Integer) b[0]).intValue(), ((Integer) b[1]).intValue(), ((Integer) b[2]).intValue(),
					((Integer) b[3]).intValue());
		}
		JSplitPane sp = null;
		Integer l = (Integer) s.getProperty(SECNAME, DIVIDERDIR);
		if (l != null) {
			sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0))
					.getComponentAt(Resources.COMP_BROWSE_IDX);
			sp.setDividerLocation(l.intValue());
		}
		l = (Integer) s.getProperty(SECNAME, DIVIDERCOL);
		if (l != null) {
			sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0))
					.getComponentAt(Resources.COMP_SELECTION_IDX);
			sp.setDividerLocation(l.intValue());
		}
		l = (Integer) s.getProperty(SECNAME, DIVIDERALBUM);
		if (l != null) {
			sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(Resources.COMP_ALBUM_IDX);
			sp.setDividerLocation(l.intValue());
		}
		l = (Integer) s.getProperty(SECNAME, DIVIDERRIPPER);
		if (l != null) {
			sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0))
					.getComponentAt(Resources.COMP_RIPPER_IDX);
			sp.setDividerLocation(l.intValue());
		}
		l = (Integer) s.getProperty(SECNAME, DIVIDERIPOD);
		if (l != null) {
			sp = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(Resources.COMP_IPOD_IDX);
			sp.setDividerLocation(l.intValue());
			l = (Integer) s.getProperty(SECNAME, DIVIDERIPODTREE);
			if (l != null) {
				sp = (JSplitPane) ((JPanel) sp.getLeftComponent()).getComponent(0);
				sp.setDividerLocation(l.intValue());
			}
		}
		if (IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.MENUBAR), 1) == 1)
			setJMenuBar(createMenu());
		else {
			getContentPane().getComponent(0).addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					if ((e.getModifiers() & InputEvent.BUTTON3_MASK) > 0) {
						new ROptionsMenu().show(PhotoOrganizer.this, e.getX(), controller.adjustMenuY(e.getY(), 230));
					}
				}
			});
		}
		if (IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.TOOLBAR), 0) == 1) {
			int orientation = s.getInt(s.getProperty(SECNAME, TB_ORIENT), JToolBar.HORIZONTAL);
			String position = BorderLayout.NORTH;
			if (orientation == JToolBar.VERTICAL) {
				position = BorderLayout.WEST;
			}
			getContentPane().add(createToolBar(orientation), position);
		}
		if (IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME, MiscellaneousOptionsTab.STATUSBAR), 1) == 1) {
			getContentPane().add((Component) controller.component(Controller.COMP_STATUSBAR), BorderLayout.SOUTH);
		}
		// load FileNameFormat data
		FileNameFormat.datemask = DataConv.arrayToString(s.getProperty(MiscellaneousOptionsTab.SECNAME,
				MiscellaneousOptionsTab.DATEFORMAT), ',');
		FileNameFormat.timemask = DataConv.arrayToString(s.getProperty(MiscellaneousOptionsTab.SECNAME,
				MiscellaneousOptionsTab.TIMEFORMAT), ',');
		FileNameFormat.counter = IniPrefs.getInt(s.getProperty(RenameOptionsTab.SECNAME, RenameOptionsTab.COUNTER), 0);
		Object[] os;
		FileNameFormat.transformCodes = new Object[Resources.ROTATIONS.length];
		// System.arraycopy(Resources.ROTATIONS, 0,
		// FileNameFormat.transformCodes, 0, Resources.ROTATIONS.length);
		try {
			os = (Object[]) s.getProperty(TransformOptionsTab.SECNAME, TransformOptionsTab.TRANSFORM);
			if (os == null)
				throw new Exception("A transform option's missed in configuration file");
		} catch (Exception e) {
			os = Resources.ROTATIONS;
		}
		System.arraycopy(os, 0, FileNameFormat.transformCodes, 0, Math.min(os.length,
				FileNameFormat.transformCodes.length));
		MiscellaneousOptionsTab.setDefaultLocale(controller);
		controller.setTimeZone(MiscellaneousOptionsTab.getTimeZone(controller));
		controller.setEncoding(MiscellaneousOptionsTab.getEncoding(controller));
	}

	// IRD
	public Iterator getKeyMnemonics() {
		return irdKeyMap.keySet().iterator();
	}

	public boolean doAction(String keyCode) {
		try {
			// System.err.println("Doing action "+keyCode);
			IrdControllable component = (IrdControllable) controller.component((String) irdKeyMap.get(keyCode));
			component.bringOnTop();
			if (component.getKeyMnemonics() != null)
				((IrdReceiver) controller.component(Controller.COMP_REMOTERECEIVER)).setOnTop(component);
			return true;
		} catch (Throwable t) {
			if (t instanceof ThreadDeath)
				throw (ThreadDeath)t;
			t.printStackTrace();
		}
		return false;
	}

	public void bringOnTop() {		
		requestFocus();
	}
	
	// TODO: consider use AbstractAction derived
	protected JMenuBar createMenu() {
		JMenuBar menubar = new JMenuBar();
		JMenu menu, menu2;
		JMenuItem item;
		menubar.add(menu = new JMenu(Resources.MENU_FILE));
		menu.setActionCommand(Resources.MENU_FILE);
		menu.add(item = new JMenuItem(Resources.MENU_DRIVE_SEL));
		item.addActionListener((JTreeTable) controller.component(Controller.COMP_DIRTREE));
		menu.add(item = new JMenuItem(Resources.MENU_ADDTOCOLLECT));
		item.addActionListener((JTreeTable) controller.component(Controller.COMP_DIRTREE));
		controller.getUiUpdater().addForNotification(item, UiUpdater.DIRECTORY_SELECTED);
		menu.add(item = new JMenuItem(Resources.MENU_RECORD_DISK));
		item.setToolTipText(Resources.TTIP_RECORD_DISK);
		item.addActionListener((RipperPanel) controller.component(Controller.COMP_RIPPERPANEL));
		controller.getUiUpdater().addForNotification(item, UiUpdater.RIPPER_NOT_EMPTY);
		Action a = ((IpodPane) controller.component(Controller.COMP_IPODPANEL)).getAction(Resources.MENU_IPOD_SYNC);
		menu.add(a);
		controller.getUiUpdater().addForNotification(a, UiUpdater.IPOD_CONNECTED);
		// menu.add(item = new JMenuItem(Resources.MENU_IPOD_SYNC));
		// item.setToolTipText(Resources.TTIP_IPOD_SYNC);
		// item.addActionListener((IpodPane)controller.component(Controller.COMP_IPODPANEL));
		// controller.getUiUpdater().addForNotification(item,
		// UiUpdater.IPOD_CONNECTED);
		menu.add(item = new JMenuItem(Resources.MENU_IPOD_WIPE));
		item.setToolTipText(Resources.TTIP_IPOD_WIPE);
		item.addActionListener((IpodPane) controller.component(Controller.COMP_IPODPANEL));
		controller.getUiUpdater().addForNotification(item, UiUpdater.IPOD_CONNECTED);
		menu.add(item = new JMenuItem(Resources.MENU_CF_TOCOLLECT));
		item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
		controller.getUiUpdater().addForNotification(item, UiUpdater.SRC_PHOTO_AVAL);
		menu.addSeparator();
		menu.add(item = new JMenuItem(Resources.MENU_PROPERTIES));
		item.addActionListener((JTreeTable) controller.component(Controller.COMP_DIRTREE));
		controller.getUiUpdater().addForNotification(item, UiUpdater.FILE_SELECTED);
		menu.addSeparator();
		menu2 = new JMenu(Resources.MENU_EXPORT);
		menu.add(menu2);
		menu2.add(item = new JMenuItem(Resources.MENU_EXPORTTOCSV));
		item.addActionListener(controller);
		menu2.add(item = new JMenuItem(Resources.MENU_EXPORTTOXML));
		item.addActionListener(controller);
		menu2.add(item = new JMenuItem(Resources.MENU_EXPORTTODSK));
		item.addActionListener(controller);
		menu2.add(item = new JMenuItem(Resources.MENU_EXPORTTOWPL));
		item.addActionListener((IpodPane) controller.component(Controller.COMP_IPODPANEL));
		controller.getUiUpdater().addForNotification(item, UiUpdater.PLAYLIST_SELECTED);
		menu2.add(item = new JMenuItem(Resources.MENU_EXPORTTOHTML));
		item.addActionListener((IpodPane) controller.component(Controller.COMP_IPODPANEL));
		controller.getUiUpdater().addForNotification(item, UiUpdater.IPODVIEW_SELECTED);

		menu2 = new JMenu(Resources.MENU_IMPORT);
		menu.add(menu2);
		menu2.add(item = new JMenuItem(Resources.MENU_IMPORTCSV));
		item.addActionListener((ActionListener) controller.component(Controller.COMP_ALBUMPANEL));
		menu2.add(item = new JMenuItem(Resources.MENU_IMPORTXML));
		item.addActionListener(controller);
		menu2.add(item = new JMenuItem(Resources.MENU_IMPORTDSK));
		item.addActionListener((ActionListener) controller.component(Controller.COMP_ALBUMPANEL));
		menu.addSeparator();
		menu.add(item = new JMenuItem(Resources.MENU_PAGE_SETUP));
		item.addActionListener(controller);
		menu.add(item = new JMenuItem(Resources.MENU_PAGE_LAYOUT));
		item.addActionListener(controller);
		menu.addSeparator();
		addPluginMenus(menu);
		menu.add(item = new JMenuItem(Resources.MENU_EXIT));
		item.setAccelerator(KeyStroke.getKeyStroke("alt F4"));
		item.addActionListener(controller);
		menubar.add(menu = new JMenu(Resources.MENU_TOOLS));
		menu.setActionCommand(Resources.MENU_TOOLS);
		menu.add(menu2 = Controller.createTransformMenu((PhotoCollectionPanel) controller
				.component(Controller.COMP_COLLECTION)));
		controller.getUiUpdater().addForNotification(menu2, UiUpdater.SELECTION_SELECTED);
		menu.addSeparator();
		menu.add(item = new JMenuItem(Resources.MENU_EXTRACTMARKERS));
		item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
		controller.getUiUpdater().addForNotification(item, UiUpdater.SELECTION_SELECTED);
		menu.add(item = new JMenuItem(Resources.MENU_EXTRACTTUMBNAILS));
		item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
		controller.getUiUpdater().addForNotification(item, UiUpdater.SELECTION_SELECTED);
		menu.add(getPublishMenu(controller, (PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION),
				UiUpdater.SELECTION_SELECTED));
		menu.add(item = new JMenuItem(Resources.MENU_UPLOADIMAGE));
		item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
		controller.getUiUpdater().addForNotification(item, UiUpdater.SELECTION_SELECTED);
		menu.addSeparator();
		menu.add(item = new JMenuItem(Resources.MENU_VIEW_HTML));
		item.addActionListener(controller);
		menu.add(item = new JMenuItem(Resources.MENU_PLAY_LIST));
		controller.getUiUpdater().addForNotification(item, UiUpdater.IS_SELECTION);
		item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
		menu.add(item = new JMenuItem(Resources.MENU_ADDRESSBOOK));
		item.addActionListener(controller);
		menu.addSeparator();
		addPluginMenus(menu);
		menu.add(item = new JMenuItem(Resources.MENU_OPTIONS));
		item.addActionListener(controller);
		menubar.add(Box.createRigidArea(new Dimension(10,0)));
		menubar.add(new SearchPanel(controller));
		menubar.add(Box.createRigidArea(new Dimension(15,0)));		
		menubar.add( new ToolbarPlayer(controller));
		menubar.add(Box.createRigidArea(new Dimension(10,0)));
		menubar.add(Box.createHorizontalGlue());
		menubar.add(menu = new JMenu(Resources.MENU_HELP));
		menu.add(item = new JMenuItem(Resources.MENU_CONTENTS));
		item.addActionListener(controller);
		menu.add(item = new JMenuItem(Resources.MENU_ABOUT));
		item.addActionListener(controller);
		menu.addSeparator();
		menu.add(item = new JMenuItem(Resources.MENU_REGISTER));
		item.addActionListener(controller);
		menubar.add(Box.createRigidArea(new Dimension(5,0)));
		try {
			menubar.setHelpMenu(menu);
		} catch (Error e) {
			System.err.println(e);
		}
		return menubar;
	}

	public static JMenu getPublishMenu(Controller controller, ActionListener al, int uc) {
		JMenu result = new JMenu(Resources.MENU_PUBLISH);
		JMenuItem item;
		for (int i = 0; i < Resources.PUBLISH_ITEMS.length; i++) {
			result.add(item = new JMenuItem(Resources.PUBLISH_ITEMS[i]));
			item.addActionListener(al);
			controller.getUiUpdater().addForNotification(item, uc);
		}
		return result;
	}

	void addPluginMenus(JMenu menu) {
		Iterator<ServiceProvider> iterator = controller.iterator();
		while (iterator.hasNext()) {
			try {
				if (((PhotoPlugin) iterator.next().getServiceProvider()).addMenuElements(menu))
					menu.addSeparator();
			} catch (ClassCastException cce) {
			}
		}
	}

	JToolBar createToolBar(int orientation) {
		JToolBar result = new JToolBar(orientation);
		JButton btn;
		result.add(btn = new JButton(Controller.getResourceIcon(Resources.IMG_READCF)));
		btn.setToolTipText(Resources.MENU_CF_TOCOLLECT);
		btn.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
		controller.getUiUpdater().addForNotification(btn, UiUpdater.SRC_PHOTO_AVAL);
		result.add(btn = new JButton(Controller.getResourceIcon(Resources.IMG_PHOTOORG)));
		btn.setToolTipText(Resources.MENU_ADDTOCOLLECT);
		btn.addActionListener((JTreeTable) controller.component(Controller.COMP_DIRTREE));
		controller.getUiUpdater().addForNotification(btn, UiUpdater.DIRECTORY_SELECTED);
		result.add(btn = new JButton(Controller.getResourceIcon(Resources.IMG_CHANGE_DRV)));
		btn.setToolTipText(Resources.MENU_DRIVE_SEL);
		btn.addActionListener((JTreeTable) controller.component(Controller.COMP_DIRTREE));
		result.add(((IpodPane) controller.component(Controller.COMP_IPODPANEL)).getAction(Resources.MENU_IPOD_SYNC));
		return result;
	}

	class FlashWindow extends JWindow {
		public FlashWindow() {
			getContentPane().add(new JLabel(Controller.getResourceIcon(Resources.IMG_LOGO)));
			pack();
			Dimension d = getToolkit().getScreenSize();
			Dimension d2 = getPreferredSize();
			setLocation((d.width - d2.width) / 2, (d.height - d2.height) / 2);
			setVisible(true);
			if (System.getProperty("os.name").indexOf("Windows") < 0)
				toBack();
		}
	}

	class ROptionsMenu extends JPopupMenu {
		public ROptionsMenu() {
			JMenuItem item;
			add(item = new JMenuItem(Resources.MENU_EXTRACTTUMBNAILS));
			item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
			item.setEnabled(controller.getUiUpdater().isEnabled(UiUpdater.SELECTION_SELECTED));
			add(getPublishMenu(controller, (PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION),
					UiUpdater.SELECTION_SELECTED));
			add(item = new JMenuItem(Resources.MENU_VIEW_HTML));
			item.addActionListener(controller);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_CF_TOCOLLECT));
			item.addActionListener((PhotoCollectionPanel) controller.component(Controller.COMP_COLLECTION));
			// todo: add addPopupPluginMenus(this);
			addSeparator();
			add(item = new JMenuItem(Resources.MENU_OPTIONS));
			item.addActionListener(controller);
		}
	}

	private Controller controller;

	private Map irdKeyMap;
}
