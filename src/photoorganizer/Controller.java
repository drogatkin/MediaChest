/* MediaChest $RCSfile: Controller.java,v $
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
 * $Id: Controller.java,v 1.90 2013/06/01 07:56:26 cvs Exp $
 */
package photoorganizer;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.Obuffer;
import mediautil.gen.MediaFormat;
import mediautil.gen.Version;
import mediautil.image.jpeg.LLJTran;

import org.aldan3.app.Desktop;
import org.aldan3.app.Registry;
import org.aldan3.model.ServiceProvider;
import org.aldan3.util.Crypto;
import org.aldan3.util.IniPrefs;

import photoorganizer.formats.APE;
import photoorganizer.formats.FLAC;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MP4;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.OGG;
import photoorganizer.formats.SampleJpeg;
import photoorganizer.ird.IrdReceiver;
import photoorganizer.media.Operations;
import photoorganizer.media.PlaybackRequest;
import photoorganizer.renderer.BatchActionWithProgress;
import photoorganizer.renderer.IpodPane;
import photoorganizer.renderer.MediaOptionsTab;
import photoorganizer.renderer.MediaPlayerPanel;
import photoorganizer.renderer.OptionsFrame;
import photoorganizer.renderer.PagePrep;
import photoorganizer.renderer.PhotoCollectionPanel;
import photoorganizer.renderer.StatusBar;
import photoorganizer.renderer.TwoPanesView;
import photoorganizer.renderer.WebPublishOptionsTab;
import addressbook.AddressBookFrame;

public class Controller extends Registry implements ActionListener, Persistancable {
	// component constraint
	public final static Integer COMP_IMAGEPANEL = new Integer(1);

	public final static Integer COMP_DIRTREE = new Integer(2);

	public final static Integer COMP_THUMBPANEL = new Integer(3);

	public final static Integer COMP_FORMATS = new Integer(4);

	public final static Integer COMP_COLLECTION = new Integer(5);

	public final static Integer COMP_THUMBCOLLCTPANEL = new Integer(6);

	public final static Integer COMP_IMAGECOLLCTPANEL = new Integer(7);

	public final static Integer COMP_ALBUMPANEL = new Integer(8);

	public final static Integer COMP_STATUSBAR = new Integer(9);

	public final static Integer COMP_IMAGEALBUMPANEL = new Integer(10);

	public final static Integer COMP_ALBUMTHUMBPANEL = new Integer(11);

	public final static Integer COMP_WEBALBUMPANEL = new Integer(12);

	public final static Integer DESCR_TABLEVIEWS = new Integer(14);

	public final static Integer COMP_REMOTERECEIVER = new Integer(15);

	public final static Integer COMP_RIPPERPANEL = new Integer(16);

	public final static Integer COMP_IPODPANEL = new Integer(17);

	public final static Integer COMP_PLAYLISTPANEL = new Integer(18);
	
	public final static Integer COMP_EMBEDDED_PLAYER = 19;

	public static final String REGISTER = "Register";

	public static final String NAME = "Name";

	public static final int BTN_MSK_OK = 1;

	public static final int BTN_MSK_APLY = 2;

	public static final int BTN_MSK_CANCEL = 4;

	public static final int BTN_MSK_HELP = 8;

	public static final int BTN_MSK_CLOSE = 16;

	public static final int BTN_MSK_PREV = 32;

	public static final int BTN_MSK_NEXT = 64;

	public static final int BTN_MSK_FINISH = 128;

	public final static Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);

	public final static Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

	public static final String ISO_8859_1 = "ISO-8859-1";

	public static final String UTF8 = "utf-8";

	public static String RESOURCE_PREFIX;

	public SampleJpeg sampleJpeg;

	public MediaPlayerPanel mediaPlayer;

	public static final boolean java2 = true;

	protected IniPrefs prefs;

	public final PhotoOrganizer mediachest;

	protected static String encoding;

	protected TimeZone timezone;

	protected List<MemoryPoolMXBean> memPools;
	
	public final Crypto crypto;
	
	protected static String[] GENRES; // cache for sorted genres
	
	protected TrayIcon trayIcon;
	
	private boolean _debug;

	public Controller(PhotoOrganizer mediachest) {
		// super(mediachest);
		this.mediachest = mediachest;
		sampleJpeg = new SampleJpeg();
		filter = new HTMLFilter();
		uiupdater = new UiUpdater();
		register(prefs = new IniPrefs("" + mediachest, "UTF-8") {
			public String getPreferredServiceName() {
				return AddressBookFrame.PREFS_NAME;
			}
		});
		mainicon = getResourceIcon(Resources.IMG_PHOTOORG).getImage();
		// setup memory usage monitoring
		memPools = ManagementFactory.getMemoryPoolMXBeans();
		for (MemoryPoolMXBean memPool : memPools) {
			if (memPool.isUsageThresholdSupported())
				try {
					MemoryUsage mu = memPool.getUsage();
					if (mu != null)
						memPool.setUsageThreshold(mu.getMax() * 80 / 100);
				} catch (IllegalArgumentException iae) {
					iae.printStackTrace();
				}
		}
		prefs.load();
		register(new Operations(this));
		crypto = new Crypto(""+prefs.getProperty(REGISTER, NAME));
		if (SystemTray.isSupported()) {
			trayIcon = new TrayIcon(mainicon);
			trayIcon.setImageAutoSize(true);
			trayIcon.setToolTip(PhotoOrganizer.PROGRAMNAME);
			trayIcon.setPopupMenu(new PopupMenu());
		}
		_debug = false; //"1".equals(prefs.getProperty(PhotoOrganizer.PROGRAMNAME, "debug"));
	}

	public String getHomeDir() {
		return System.getProperty(PhotoOrganizer.PROGRAMNAME + ".home",
				System.getProperty("user.home"));
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.MENU_OPTIONS)) {
			if (options == null) {
				options = new OptionsFrame(this);
			} else
				options.setVisible(true);
		} else if (cmd.equals(Resources.MENU_ABOUT)) {
			JOptionPane.showMessageDialog(mediachest, "<html><i>"
					+ PhotoOrganizer.PROGRAMNAME
					+ "\n"
					+ PhotoOrganizer.VERSION
					+ '.'
					+ PhotoOrganizer.BUILD +"(Maintanance build "+BuildStamp.BUILD_STAMP+")"
					//+ PhotoOrganizer.BUILD +"(release preview)"
					+ '\n'
					+ PhotoOrganizer.COPYRIGHT
					+ '\n'
					+ "<html><b>"
					+ PhotoOrganizer.DEDICATED
					+ '\n'
					+ "Java "
					+ System.getProperty("java.version")
					+ (java2 ? (" spec " + System.getProperty("java.specification.name")) : "")
					+ '\n'
					+ "JVM "
					+ System.getProperty("java.vm.name")
					+ " OS "
					+ System.getProperty("os.name")
					+ ' '
					+ System.getProperty("os.version")
					+ ' '
					+ System.getProperty("os.arch")
					+ '\n'
					+ (java2 ? ("Available " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB, free "
							+ (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB\n") : "")
							+"Mediautil version "+Version.MAJOR+"."+Version.SUBVERSION+"."+Version.LOAD+Version.COMMENT+"\n"
							+"This product contains portions of software under GPL, BSD, MPL, and EPL licenses\n"
					+ Locale.getDefault().getDisplayLanguage() + '-' + Locale.getDefault().getDisplayCountry()
					+ getOwnerInfo(), Resources.MENU_ABOUT,
					JOptionPane.PLAIN_MESSAGE, getResourceIcon(Resources.IMG_LOGO));
		} else if (cmd.equals(Resources.MENU_REGISTER)) {
			Object n = JOptionPane.showInputDialog(mediachest, Resources.LABEL_REGISTER_NAME, prefs.getProperty(
					REGISTER, NAME));
			if (n != null)
				prefs.setProperty(REGISTER, NAME, n);
		} else if (cmd.equals(Resources.MENU_CONTENTS)) {
			File helpIndexFile = new File(System.getProperty("user.dir"),"doc"+File.separatorChar+Resources.URL_HELP);
			if (helpIndexFile.exists() == true) 
					Desktop.showUrl(helpIndexFile.toURI().toASCIIString());
			else 
					JOptionPane.showMessageDialog(mediachest, String.format("Help file %s not found", helpIndexFile));
		} else if (cmd.equals(Resources.MENU_VIEW_HTML)) {
			String d = (String) prefs.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.HTTP_WEBROOT);
			if (d == null || d.length() == 0)
				d = prefs.getHomeDirectory();
			Object[] options = { Resources.CMD_OPEN, Resources.CMD_CANCEL, Resources.CMD_BROWSE };
			JOptionPane pane = new JOptionPane(Resources.LABEL_INPUT_URL, JOptionPane.QUESTION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
			pane.setWantsInput(true);
			pane.setInitialSelectionValue("http://"
					+ prefs.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.HOSTNAME) + '/'
					+ PhotoCollectionPanel.LastHtmlName);
			pane.selectInitialValue();

			JDialog dialog = pane.createDialog(mediachest, Resources.TITLE_URL_SEL);
			dialog.show();
			String selectedValue = (String) pane.getValue();

			if (selectedValue == JOptionPane.UNINITIALIZED_VALUE || options[0].equals(selectedValue)) {
				// user entered URL and selected open
				Desktop.showUrl((String) pane.getInputValue());
			} else if (options[1].equals(selectedValue)) {
				// user selected cancel
			} else if (selectedValue != null) {
				// user wants to open a file
				JFileChooser fc = new JFileChooser(d);
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				fc.setDialogTitle(Resources.TITLE_HTML_TEMPL);
				if (fc.showOpenDialog(mediachest) == JFileChooser.APPROVE_OPTION)
					Desktop.showUrl(fc.getSelectedFile().getAbsolutePath());
			}
		} else if (cmd.equals(Resources.MENU_PAGE_SETUP)) {
			doPageSetup(false);
		} else if (cmd.equals(Resources.MENU_PAGE_LAYOUT)) {
			PageLayout.layoutDialog(prefs);
		} else if (cmd.equals(Resources.MENU_ADDRESSBOOK)) {
			// TODO: value should be calculated as base app value + component
			// index
			Frame abf = (Frame) component(AddressBookFrame.COMP_ADDRESSBOOK);
			if (abf == null) {
				abf = new AddressBookFrame(this);
				add(abf, AddressBookFrame.COMP_ADDRESSBOOK);
			} else {
				if (abf.getState() == Frame.ICONIFIED)
					abf.setState(Frame.NORMAL);
				if (abf.isShowing() == false)
					abf.setVisible(true);
				abf.requestFocus();
			}
		} else if (cmd.equals(Resources.MENU_EXIT)) {
			exit();
		} else if (cmd.equals(Resources.MENU_RESTORE)) {
			restoreIconizedApp();
		} else {
			// send command to active component
			// TODO: how to find, search for JTabbedPane and then
			// getSelectedComponent()
			Iterator<ServiceProvider> iterator = iterator();
			while (iterator.hasNext()) {
				Object o = iterator.next().getServiceProvider();
				if (o instanceof Component && o instanceof ActionListener) {
					Component c = (Component) o;
					Container t = null;
					boolean processed = false;
					do {
						t = c.getParent();
						if (t != null && t instanceof JTabbedPane && ((JTabbedPane) t).getSelectedComponent() == c) {
							// System.err.println("Focused "+o);
							((ActionListener) o).actionPerformed(a);
							processed = a.getSource() == null;
							break;
						}
						c = t;
					} while (c != null);
					if (processed)
						break;
				}
			}
		}
	}
	
	void exit() {
		if (canClose()) {
			close();
			mediachest.dispose();
		}		
	}

	public IniPrefs getPrefs() {
		return prefs;
	}

	public boolean canClose() {
		IpodPane ipodPane = (IpodPane) component(COMP_IPODPANEL);
		return !(ipodPane != null && ipodPane.isChanged() && JOptionPane.showConfirmDialog(mediachest,
				Resources.LABEL_BEFORE_EXIT, Resources.TITLE_CONFIRMEXIT, JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION);
	}

	public void doPageSetup(boolean defaultPage) {
		if (printer == null) {
			printer = PrinterJob.getPrinterJob();
			page = printer.defaultPage();
			PageLayout.load(prefs, page);
		}
		if (defaultPage == false) {
			page = printer.pageDialog(page);
			PageLayout.save(prefs, page);
		}
	}

	public void setWaitCursor(Container comp, boolean on) {
		if (comp == null) {
			comp = mediachest;
		} else if (comp instanceof JComponent)
			comp = ((JComponent) comp).getTopLevelAncestor();
		if (on)
			comp.setCursor(WAIT_CURSOR);
		else
			comp.setCursor(DEFAULT_CURSOR);
	}

	public static InputStream getResourceAsStream(String path, Object context) {
		InputStream result = null;
		try {
			ClassLoader cl = Controller.class.getClassLoader();
			if (cl instanceof java.net.URLClassLoader)
				result = cl.getResourceAsStream((java2 ? "" : "/") + (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "")
						+ path);
			else
				result = ClassLoader.getSystemResourceAsStream((java2 ? "" : "/")
						+ (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "") + path);
			if (result == null && context != null) {
				result = context.getClass().getClassLoader().getResourceAsStream(
						(java2 ? "" : "/") + (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "") + path);
			}
			if (result == null)
				throw new Exception("Can't obtain res stream");
		} catch (Exception e) {
			if (true)
				System.err.println("Couldn't obtain resource " + (java2 ? "" : "/")
						+ (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "") + path + ", 'cause " + e);
		}
		return result;
	}

	public boolean isLowMemory() {
		for (MemoryPoolMXBean memPool : memPools) {
			if (memPool.isUsageThresholdSupported() && memPool.isUsageThresholdExceeded())
				return true;
		}
		return false;
	}
	
	public boolean isDebug() {
		return _debug;
	}

	public void print(final Object[] medias) {
		print(medias, false);
	}

	public void print(final Object[] medias, boolean silent) {
		doPageSetup(true);
		printer.setJobName(Resources.TITLE_PRINT);
		printer.setPrintable(new PagePrep(medias, this), page);
		if (silent || printer.printDialog()) {
			final StatusBar statusbar = (StatusBar) component(COMP_STATUSBAR);
			statusbar.displayInfo(Resources.INFO_PRINTING);
			System.err.println("We are in isEventDispatchThread() " + SwingUtilities.isEventDispatchThread());
			new Thread(new Runnable() {
				public void run() {
					try {
						printer.print();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					statusbar.clearInfo();
					statusbar.clearProgress();
				}
			}, "Printing...").start();
		}
	}

	public UiUpdater getUiUpdater() {
		return uiupdater;
	}

	public Image getMainIcon() {
		return mainicon;
	}

	public static ImageIcon getResourceIcon(String name) {
		name = "resource/image/" + name;
		try {
			ClassLoader cl = Controller.class.getClassLoader();
			//System.err.printf("URLS %s%n", Arrays.toString(((java.net.URLClassLoader) cl).getURLs()));
			if (cl instanceof java.net.URLClassLoader)
				return new ImageIcon(((java.net.URLClassLoader) cl).findResource((java2 ? "" : "/")
						+ (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "") + name));
			else
				return new ImageIcon(cl.getResource((java2 ? "" : "/")
						+ (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "") + name));
		} catch (Exception e) {
			if (true)
				System.err.println("Couldn't load graphical resource " + (java2 ? "" : "/")
						+ (RESOURCE_PREFIX != null ? RESOURCE_PREFIX : "") + name + ", 'cause " + e);
		}
		return null; // new ImageIcon();
	}

	public ResourceBundle getResource(String name) {
		// check in cache
		InputStream is = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream("resource/text/" + name + ".res");
			return new PropertyResourceBundle(is);
		} catch (IOException ioe) {
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
			}
		}
		return null;
	}

	public boolean selectTab(String tabName) {
		JTabbedPane tp = (JTabbedPane) mediachest.getContentPane().getComponent(0);
		int ti = tp.indexOfTab(tabName);
		if (ti >= 0) {
			tp.setSelectedIndex(ti);
			return true;
		}
		return false;
	}

	public String getHomeDirectory() {
		String result = System.getProperty(mediachest.getName() + IniPrefs.HOMEDIRSUFX, ".");
		if (!result.endsWith("/") || !result.endsWith("\\") || !result.endsWith(":"))
			result += File.separatorChar;
		return result;
	}

	public void updateCaption(String str) {
		if (str == null)
			str = "";
		mediachest.setTitle(str);
	}

	public Object component(int id) {
		ServiceProvider sp = getService(String.valueOf(id));
		if (sp != null)
			return sp.getServiceProvider();
		// System.err.printf("Component %d not found%n", id);
		return null;
	}

	public Object component(String id) {
		return getService(id).getServiceProvider();
	}

	public void add(final Object service, final int id) {
		add(service, String.valueOf(id));
	}

	public void add(final Object service, final String id) {
		ServiceProvider provider = getService(id);
		if (provider != null)
			unregister(provider);
		register(new ServiceProvider() {
			public String getPreferredServiceName() {
				return id;
			}

			public Object getServiceProvider() {
				return service;
			}
		});
	}

	public boolean add(String className, int id) {
		return add(className, String.valueOf(id));
	}

	public boolean add(String className, String id) {
		if (className == null) {
			ServiceProvider sp = getService(id);
			return sp == null?false:unregister(sp) != null;
		}
		Class cl = null;

		try {
			cl = Class.forName(className);
			add(cl.getConstructor(Controller.class).newInstance(Controller.this), id);
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			System.err.println(e);
			try {
				add(cl.newInstance(), id);
				return true;
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			if (e.getCause() != null)
				e.getCause().printStackTrace();
		}
		return false;
	}

	public TimeZone getTimeZone() {
		return timezone;
	}

	public void setTimeZone(TimeZone tz) {
		timezone = tz;
	}

	public void showHelp(Component parentComponent, String helpContent) {
		JDialog helpDialog = null;
		if (helpDialog == null) {

			Window window = getWindowForComponent(parentComponent);
			if (window instanceof Frame) {
				helpDialog = new JDialog((Frame) window, Resources.TITLE_HELP, false);
			} else {
				helpDialog = new JDialog((Dialog) window, Resources.TITLE_HELP, false);
			}
			final Dialog innerDialog = helpDialog;
			TwoPanesView helpContentPane = TwoPanesView.createFramed(true, null, BTN_MSK_CLOSE, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					innerDialog.dispose();
				}
			});
			helpDialog.setContentPane(helpContentPane);
			helpDialog.setDefaultCloseOperation(helpDialog.EXIT_ON_CLOSE);
			helpContentPane.setSize(450, 360);
			helpDialog.pack();
		}

		((TwoPanesView) helpDialog.getContentPane()).setUpperText(getResource("help").getString(helpContent));
		helpDialog.setVisible(true);
	}

	public Window getWindowForComponent(Component parentComponent) throws HeadlessException {
		if (parentComponent == null)
			return mediachest;
		if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
			return (Window) parentComponent;
		return getWindowForComponent(parentComponent.getParent());
	}

	public int adjustMenuY(int y, int h) {
		int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
		if (y > screen_height - h)
			return screen_height - h;
		else
			return y;
	}

	public FileFilter getHtmlFilter() {
		return filter;
	}

	public static OutputStream getWSAwareOutStream(String path, String name) throws IOException {
		return new FileOutputStream(new File(path, name));
	}

	public void save() {
		Iterator<ServiceProvider> iterator = iterator();
		while (iterator.hasNext()) {
			Object o = iterator.next().getServiceProvider();
			if (o instanceof Persistancable)
				((Persistancable) o).save();
		}
	}

	public void load() {
		Iterator<ServiceProvider> iterator = iterator();
		while (iterator.hasNext()) {
			Object o = iterator.next().getServiceProvider();
			if (o instanceof Persistancable)
				((Persistancable) o).load();
		}
	}

	public void close() {
		save();
		mediachest.save();
		prefs.save();
	}

	// all background processes have to register and unregister to keep a track
	// of active threads
	public void registerBackgroundWork(Object work, boolean addIt) {
	}

	public List getRegisteredWorks() {
		return null;
	}

	public static JPanel createButtonPanel(ActionListener al, boolean helpButton) {
		return createButtonPanel(al, BTN_MSK_OK + BTN_MSK_APLY + BTN_MSK_CANCEL + (helpButton ? BTN_MSK_HELP : 0),
				FlowLayout.RIGHT);
	}

	public static JPanel createButtonPanel(ActionListener al, int buttonMask, int align) {
		JButton btn;
		JPanel result = new JPanel();
		result.setLayout(new FlowLayout(align));
		if ((buttonMask & BTN_MSK_PREV) != 0) {
			result.add(btn = new JButton(Resources.CMD_PREV));
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_OK) != 0) {
			result.add(btn = new JButton(Resources.CMD_OK));
			btn.setDefaultCapable(true);
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_APLY) != 0) {
			result.add(btn = new JButton(Resources.CMD_APPLY));
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_NEXT) != 0) {
			result.add(btn = new JButton(Resources.CMD_NEXT));
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_CLOSE) != 0) {
			result.add(btn = new JButton(Resources.CMD_CLOSE));
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_FINISH) != 0) {
			result.add(btn = new JButton(Resources.CMD_FINISH));
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_CANCEL) != 0) {
			result.add(btn = new JButton(Resources.CMD_CANCEL));
			btn.addActionListener(al);
		}
		if ((buttonMask & BTN_MSK_HELP) != 0) {
			result.add(btn = new JButton(Resources.CMD_HELP));
			btn.addActionListener(al);
		}
		return result;
	}

	public static JMenu createTransformMenu(ActionListener listener) {
		JMenu result = new JMenu(Resources.MENU_TRANSFORM);
		JMenuItem item;
		result.add(item = new JMenuItem(Resources.MENU_NONE));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_ROTATE90));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_ROTATE180));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_ROTATE270));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_FLIP_H));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_FLIP_V));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_TRANSPOSE));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_TRANSVERSE));
		item.addActionListener(listener);
		result.addSeparator();
		result.add(item = new JMenuItem(Resources.MENU_COMMENT));
		item.addActionListener(listener);
		result.add(item = new JMenuItem(Resources.MENU_CROP));
		item.addActionListener(listener);
		return result;
	}

	public static int convertCmdToTrnasformOp(String cmd) {
		int op = -1;
		if (cmd.equals(Resources.MENU_ROTATE90)) {
			op = LLJTran.ROT_90;
		} else if (cmd.equals(Resources.MENU_ROTATE180)) {
			op = LLJTran.ROT_180;
		} else if (cmd.equals(Resources.MENU_ROTATE270)) {
			op = LLJTran.ROT_270;
		} else if (cmd.equals(Resources.MENU_FLIP_H)) {
			op = LLJTran.FLIP_H;
		} else if (cmd.equals(Resources.MENU_FLIP_V)) {
			op = LLJTran.FLIP_V;
		} else if (cmd.equals(Resources.MENU_TRANSPOSE)) {
			op = LLJTran.TRANSPOSE;
		} else if (cmd.equals(Resources.MENU_TRANSVERSE)) {
			op = LLJTran.TRANSVERSE;
		} else if (cmd.equals(Resources.MENU_NONE)) {
			op = LLJTran.NONE;
		} else if (cmd.equals(Resources.MENU_COMMENT)) {
			op = LLJTran.COMMENT;
		} else if (cmd.equals(Resources.MENU_CROP)) {
			op = LLJTran.CROP;
		}
		return op;
	}

	public void setEncoding(String encoding) {
		Controller.encoding = encoding;
	}

	public static String getEncoding() {
		if (encoding != null)
			return encoding;
		return Charset.defaultCharset().name();
	}
	
	public synchronized static String[] getSortedGenres() {
		if (GENRES == null) {
			GENRES = Arrays.copyOf(MP3.GENRES, MP3.GENRES.length);
			Arrays.sort(GENRES);
		}
		return GENRES;
	}

	public void saveTableColumns(JTable table, String secName, String tableName) {
		Integer[] w = new Integer[table.getColumnCount()];
		for (int i = 0; i < w.length; i++) {
			try {
				w[i] = new Integer(table.getColumn(table.getColumnName(i)).getWidth());
			} catch (IllegalArgumentException iae) {
				w[i] = Resources.I_NO;
			}
		}
		prefs.setProperty(secName, tableName, w);
	}

	public void loadTableColumns(JTable table, String secName, String tableName) {
		Object[] w = (Object[]) prefs.getProperty(secName, tableName);
		if (w == null)
			return;
		for (int i = 0; i < w.length; i++) {
			table.getColumn(table.getColumnName(i)).setWidth(((Integer) w[i]).intValue());
			table.getColumn(table.getColumnName(i)).setPreferredWidth(((Integer) w[i]).intValue());
		}
	}

	public void askToRestart() {
		if (JOptionPane.showConfirmDialog(mediachest, Resources.LABEL_RESTART_UGRADE, Resources.TITLE_UPGRADE,
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			Runtime rt = Runtime.getRuntime();
			try { // System.getProperty("java.home")+File.separatorChar
				// +"bin"+File.separatorChar +
				rt.exec((File.separatorChar == '/' ? "java" : "javaw") + " -cp \""
						+ System.getProperty("java.class.path") + "\" -Djdbc.drivers=\""
						+ System.getProperty("jdbc.drivers") + "\" -D" + mediachest.getName() + IniPrefs.HOMEDIRSUFX
						+ "=" + System.getProperty(mediachest.getName() + IniPrefs.HOMEDIRSUFX, ".") + " -D"
						+ addressbook.AddressBookFrame.PROGRAMNAME + IniPrefs.HOMEDIRSUFX + "=\""
						+ System.getProperty(addressbook.AddressBookFrame.PROGRAMNAME + IniPrefs.HOMEDIRSUFX, ".")
						+ "\" photoorganizer.PhotoOrganizer");
				// rt.exec("cmd /c MediaChest.bat");
			} catch (IOException ioe) {
				System.err.println("Exception at restart " + ioe);
			}
			rt.exit(0);
		}

	}
	
	public boolean updateTrayTitle(String title) {
		if (trayIcon != null) {
			if (title.length() > 0)
				trayIcon.displayMessage(PhotoOrganizer.PROGRAMNAME, title, TrayIcon.MessageType.INFO);
			trayIcon.setToolTip(String.format(PhotoOrganizer.PROGRAMNAME+":%s", title));
			return true;
		}
		return false;
	}
	protected void	restoreIconizedApp() {
		SystemTray.getSystemTray().remove(trayIcon);
		mediachest.setVisible(true);
	}
	
	protected boolean iconize() {
		if (trayIcon == null)
			return false;
		PopupMenu pum = trayIcon.getPopupMenu();
		if (pum.getItemCount() == 0) {
			MenuItem mi;
			pum.add(mi = new MenuItem(Resources.MENU_RESTORE));
			mi.addActionListener(this);
			if (mediaPlayer != null) {
				pum.addSeparator();
				pum.add(mi = new MenuItem(Resources.CMD_RESUME));
				mi.addActionListener(mediaPlayer);
				pum.add(mi = new MenuItem(Resources.CMD_SKIP));
				mi.addActionListener(mediaPlayer);
				pum.add(mi = new MenuItem(Resources.CMD_STOP));
				mi.addActionListener(mediaPlayer);
				trayIcon.addMouseListener(new MouseListener() {

					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() > 1)
							restoreIconizedApp();
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {
					}

					@Override
					public void mouseExited(MouseEvent arg0) {
					}

					@Override
					public void mousePressed(MouseEvent arg0) {
					}

					@Override
					public void mouseReleased(MouseEvent arg0) {
					}});
			}
			pum.addSeparator();
			pum.add(mi = new MenuItem(Resources.MENU_EXIT));
			mi.addActionListener(this);
		}
		try {
			SystemTray.getSystemTray().add(trayIcon);
			return true;
		} catch (AWTException ae) {

		}
		return false;
	}

	class HTMLFilter extends FileFilter {
		final String[] extensions = new String[] { "html", "htm", "htmt" };

		// Accept all directories and (gif || jpg || tiff) files.
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if (i > 0 && i < s.length() - 1) {
				String extension = s.substring(i + 1).toLowerCase();
				for (i = 0; i < extensions.length; i++) {
					if (extensions[i].equals(extension)) {
						return true;
					} else {
						return false;
					}
				}
			}
			return false;
		}

		// The desctiption of this filter
		public String getDescription() {
			return Resources.LABEL_HTML_FILES;
		}
	}

	public MediaPlayerPanel playMedia(MediaFormat media, int introFrames) throws IOException {
		// TODO reconsider the fragment to either use play supported classes list of inquiry inside
		if (media.getFormat(MediaFormat.AUDIO) == null
				&& media instanceof MP4 == false) {
			System.err.printf("Trying a desktop player for unsupported %s%n", media);
			if (Desktop.openFile(media.getFile()))
				return new  MediaPlayerPanel(true, this) {

					@Override
					public synchronized void waitForCompletion() {
					}				
			};
		}
		if (isDebug())
			System.err.printf("Calling Java player type %d format %s%n",
					media.getType(),
					media.getFormat(0xFFFF | MediaFormat.AUDIO));
		if (prefs.getInt(prefs.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.REUSEPLAYER), 1) == 1) {
			JFrame frame = null;
			synchronized (this) {
				if (mediaPlayer == null) {
					frame = new JFrame();
					frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					frame.setContentPane(mediaPlayer = new MediaPlayerPanel(true, this));
					frame.pack();
					frame.setIconImage(getMainIcon());
					frame.addWindowListener(mediaPlayer.getWindowListener());
				} else {
					frame = (JFrame) mediaPlayer.getTopLevelAncestor();
				}
			}
			if (frame != mediachest)
				frame.setVisible(true);
			mediaPlayer.replay(media, introFrames);
			if (component(COMP_REMOTERECEIVER) != null)
				((IrdReceiver) component(COMP_REMOTERECEIVER)).setOnTop(mediaPlayer);
			return mediaPlayer;
		} else {
			if (isDebug())
				System.err.printf("New player%n");
			MediaPlayerPanel result;
			JFrame frame = new JFrame(Resources.TITLE_NOW_PLAYING + media.toString());
			//frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			frame.setContentPane(result = new MediaPlayerPanel(media, frame, introFrames, this));
			frame.pack();
			frame.setVisible(true);
			frame.setIconImage(getMainIcon());
			if (component(COMP_REMOTERECEIVER) != null)
				((IrdReceiver) component(COMP_REMOTERECEIVER)).setOnTop(result);
			return result;
		}
	}

	// for version 1.1.1
	// public void playMediaList(URL[] medias) {
	// }

	public void playMediaList(final PlaybackRequest request) {
		if (isDebug()) {
			System.err.printf("Request palying %s%n", request.playbackList);
			new Exception().printStackTrace();
		}
		new Thread(new Runnable() {
			public void run() {
				request.player = null;
				playMediaList(request.playbackList, request);
			}
		}, "MediaListPlayer").start();
	}

	public void playMediaList(Object[] medias, PlaybackRequest request) {
		if (request.player != null && request.player.isClosed())
			return;
		if (mediaPlayer.activeRequest != null) {
			mediaPlayer.activeRequest.close();
			//mediaPlayer.close();
		}
		mediaPlayer.activeRequest = request;
		for (int k = 0; medias != null && k < medias.length; k++) {
			File file = null;
			MediaFormat media = null;
			if (medias[k] instanceof File)
				file = (File) medias[k];
			else if (medias[k] instanceof MediaFormat)
				media = (MediaFormat) medias[k];
			else
				continue;
			if (isDebug())
				System.err.printf("Playing %d %s of %d%n", k, medias[0],
						medias.length);
			if (file != null && file.isDirectory() && request.recursive) {
				playMediaList(file.listFiles(), request);
			} else {
				if (media == null && file != null)
					media = MediaFormatFactory.createMediaFormat(file, encoding, false);
				if (media == null || !media.isValid())
					continue;
				if (request.matcher == null || request.matcher.match(media))
					try {
						if (isDebug())
							System.err.printf("Playing %s - %d of %d%n", media, k, medias.length);
						//if (request.player == null)
						request.player = playMedia(media, request.introFrames);
						//else
						//request.player.replay(media);
						request.player.waitForCompletion();
						if (isDebug())
							System.err.println("awake Closed " + request.player.isClosed());
						if (request.isClosed())
							break;
						if (isDebug())
							System.err.printf("Pause %d%n", request.pauseBetween);
						if (request.pauseBetween > 999)
							try {
								Thread.sleep(request.pauseBetween);
							} catch (InterruptedException ie) {
							}
					} catch (IOException ioe) {
						System.err.println("Problem " + ioe + " for " + media);
					}
			}
		}
		request.close();
		updateTrayTitle("");
	}

	public static void buildMediaList(List holder, Object[] fs, int[] types, boolean recursive) {
		for (int i = 0; i < fs.length; i++) {
			MediaFormat format = null;
			if (fs[i] instanceof MediaFormat)
				format = (MediaFormat) fs[i];
			else if (fs[i] instanceof File) {
				// TODO: find a work around JDK 1.4 bug, no unicode names in
				// drop list
				// System.err.println(" "+fs[i]+" is
				// "+((File)fs[i]).isDirectory());
				if (((File) fs[i]).isFile())
					format = MediaFormatFactory.createMediaFormat((File) fs[i], encoding, false);
				else if (((File) fs[i]).isDirectory() && (fs.length == 1 || recursive))
					buildMediaList(holder, ((File) fs[i]).listFiles(), types, recursive);
			}
			if (format != null && format.isValid()) {
				if (types != null) {
					int type = format.getType();
					if (type > 0) {
						for (int k = 0; k < types.length; k++) {
							if ((type & types[k]) > 0) {
								holder.add(format);
								break;
							}
						}
					}
				} else
					holder.add(format);
			}
		}
	}

	public static void convertToWav(MediaFormat media, String destName, StatusBar statusBar) throws JavaLayerException {
		convertToWav(media, destName, new Converter(), statusBar);
	}

	// TODO: reconsider each time creation
	public static void convertToWav(MediaFormat media, String dest, Converter converter, StatusBar statusBar)
			throws JavaLayerException, IllegalArgumentException {
		if (media == null || !(media instanceof MP3) || !media.isValid())
			throw new IllegalArgumentException("Media format not specified or invalid.");
		if (statusBar != null)
			converter.convert(media.getFile().getPath(), dest, new ConvertProgressListener(statusBar));
		else
			converter.convert(media.getFile().getPath(), dest);
	}

	static public class ConvertProgressListener implements Converter.ProgressListener {
		StatusBar statusBar;

		public ConvertProgressListener(StatusBar statusBar) {
			this.statusBar = statusBar;
		}

		public void converterUpdate(int updateID, int param1, int param2) {
			// System.err.println("Set progress "+param1+'/'+param2);
			if (UPDATE_FRAME_COUNT == updateID)
				statusBar.setProgress(param1);
			else if (UPDATE_CONVERT_COMPLETE == updateID) {
				statusBar.clearProgress();
				statusBar.flashInfo("Completed");
			}
		}

		public void parsedFrame(int frameNo, Header header) {
			statusBar.displayInfo("P/f: " + frameNo);
			statusBar.tickProgress();
		}

		public void readFrame(int frameNo, Header header) {
			statusBar.displayInfo("R/f: " + frameNo);
			statusBar.tickProgress();
		}

		public void decodedFrame(int frameNo, Header header, Obuffer o) {
			statusBar.displayInfo("D/f: " + frameNo);
			statusBar.tickProgress();
		}

		public boolean converterException(Throwable t) {
			statusBar.flashInfo("Exception " + t, true);
			return false;
		}
	}

	public static String[] objectArrayToStringArray(Object[] _oa) {
		if (_oa == null)
			return null;
		String[] result = new String[_oa.length];
		for (int i = 0; i < _oa.length; i++)
			result[i] = _oa[i].toString();
		return result;
	}
	
	public static Object nullToEmpty(Object s) {
		if (s == null)
			return "";
		return s;
	}
	
	public static boolean hasExtension(File file, String[] extensions) {
		String name = file.getName();
		if (name != null && name.isEmpty() == false) {
			int l = name.length();
			for (int i = 0; i < extensions.length; i++) {
				int el = extensions[i].length();
				if (el < l - 1 && name.regionMatches(true, l - el, extensions[i], 0, el))
					return true;
			}
		}
		return false;
	}

	public static String getFileExtension(File f) {
		return getFileExtension(f.getName());
	}

	public static String getFileExtension(String n) {
		int p = n.lastIndexOf('.');
		if (p > 0)
			return n.substring(p);
		return "";
	}

	public void showHtml(String content, String title) {
		JDialog dialog = new JDialog(mediachest, title, true);
		TwoPanesView contentPane;
		dialog.setContentPane(contentPane = TwoPanesView.createFramed(true, dialog, BTN_MSK_CLOSE, null));
		contentPane.setUpperText(content);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		// dialog.setIconImage(getMainIcon());
		contentPane.setSize(440, 330);
		dialog.setVisible(true);
	}

	// TODO: should be public
	protected String getOwnerInfo() {
		Object n = prefs.getProperty(REGISTER, NAME);
		if (n != null)
			return "\n" + Resources.LABEL_REGISTER_NAME + " " + n;
		return "";
	}

	public static class ExtFileChooser extends JFileChooser {
		String extension, description;

		public ExtFileChooser(boolean writeRequest, String defName, String ext, String extHelp) {
			this(writeRequest, defName, ext, extHelp, null);
		}

		public ExtFileChooser(boolean writeRequest, String defName, String ext, String extHelp, String title) {
			super();
			if (ext == null)
				extension = Resources.EXT_XML;
			else
				extension = ext;
			if (extHelp == null)
				description = Resources.LABEL_XML_LAYOUT_FILES;
			else
				description = extHelp;
			if (System.getProperty(PhotoOrganizer.PROGRAMNAME + IniPrefs.HOMEDIRSUFX) != null) {
				setCurrentDirectory(new File(System.getProperty(PhotoOrganizer.PROGRAMNAME + IniPrefs.HOMEDIRSUFX))
						.getAbsoluteFile());
				rescanCurrentDirectory();
			}
			if (title == null)
				setDialogTitle(writeRequest ? Resources.MENU_SAVE_LAYOUT : Resources.MENU_LOAD_LAYOUT);
			else
				setDialogTitle(title);
			setDialogType(writeRequest ? SAVE_DIALOG : OPEN_DIALOG);
			if (writeRequest)
				setSelectedFile(new File(getCurrentDirectory(), defName + extension));
			setApproveButtonText(writeRequest ? Resources.CMD_SAVE : Resources.CMD_OPEN);
			setApproveButtonToolTipText(writeRequest ? Resources.CMD_SAVE : Resources.CMD_OPEN);
			addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
				public String getDescription() {
					return description;
				}

				public boolean accept(File f) {
					return ExtFileChooser.this.accept(f);
				}
			});
		}

		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
		}

		public File getSelectedFile() {
			File result = super.getSelectedFile();
			if (result != null && !accept(result))
				result = new File(result.getParent(), result.getName() + extension);
			return result;
		}
	}

	protected PageFormat page;

	protected PrinterJob printer;

	private Image mainicon;

	private String transformtargetdir;

	private OptionsFrame options;

	private HTMLFilter filter;

	private UiUpdater uiupdater;

	/*
	 * synchronized(pj) { pj.notify(); } synchronized(pj) {
	 * 
	 * try { pj.wait(10*60*1000); } catch(InterruptedException e) { } }
	 */

	// some methods can be move in base class or a separate util class
	public static byte[] readStreamInBuffer(InputStream in) throws IOException {
		return readStreamInBuffer(in, 8*2048, 0);
	}

	public static byte[] readStreamInBuffer(InputStream in, int bufSize, int maxSize) throws IOException {
		byte[] buf = new byte[bufSize];
		byte[] result = new byte[0];
		try {
			int cl;
			do {
				cl = in.read(buf);
				if (cl < 0)
					break;
				byte[] wa = new byte[result.length + cl];
				System.arraycopy(result, 0, wa, 0, result.length);
				System.arraycopy(buf, 0, wa, result.length, cl);
				result = wa;
			} while (maxSize <= 0 || result.length < maxSize);
		} finally {
			// TODO reconsider that, since stream should be called a party opened the stream
			in.close();
		}
		return result;
	}

	public static class CopierWithProgress extends BatchActionWithProgress.Action<File[]> {
		boolean canceled;

		boolean createDirs;

		public CopierWithProgress() {

		}

		public CopierWithProgress(boolean addDirs) {
			createDirs = addDirs;
		}

		public void setCanceled() {
			canceled = true;
		}

		public void run() {
			int BUF_SIZE = 8192;
			setMax((int) (job[0].length() / BUF_SIZE), job[0].getName());
			byte[] buffer = new byte[BUF_SIZE];
			InputStream is = null;
			OutputStream os = null;
			// TODO: check if target directory of out file available and create
			// if needed
			if (createDirs && job[1].getParentFile().exists() == false)
				if (job[1].getParentFile().mkdirs() == false) {
					System.err.println("Can't create target folder: " + job[1].getParentFile());
					return;
				}
			try {
				int len;
				is = new FileInputStream(job[0]);
				os = new FileOutputStream(job[1]);
				while ((len = is.read(buffer)) > 0) {
					os.write(buffer, 0, len);
					tick();
					if (canceled)
						break;
				}
				is.close();
				os.close();
			} catch (IOException ioe) {
				try {
					if (is != null)
						is.close();
					if (os != null)
						os.close();
				} catch (IOException ioe2) {

				}
				System.err.println("Can't copy file " + job[0] + " to " + job[1] + ", because " + ioe);
			}
		}
	}

	public static String trimLastSlash(String path) {
		if (path == null)
			return null;
		int l = path.length();
		if (l >0 && path.charAt(l-1) == File.separatorChar)
			return path.substring(0, l-2);
		return path;
	}
}
