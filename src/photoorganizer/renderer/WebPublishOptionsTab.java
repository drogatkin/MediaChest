/* PhotoOrganizer - WebPublishOptionsTab 
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  $Id: WebPublishOptionsTab.java,v 1.20 2008/05/24 07:02:42 dmitriy Exp $
 *  Visit http://mediachest.sf.net to get the latest information
 *  about Rogatkin's products.
 */
package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileSystemView;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.HelpProvider;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.courier.HTTPCourier;
import photoorganizer.directory.JDirectoryChooser;
import photoorganizer.ftp.Ftp;
import photoorganizer.ftp.FtpConnectionInfo;
import photoorganizer.ftp.FtpFileSystemView;

// TODO rename to NetPublishing
public class WebPublishOptionsTab extends JPanel implements ActionListener, Persistancable, HelpProvider {
	public final static String SECNAME = "WebPublishOptions";

	public final static String HOSTNAME = "HostName";

	public final static String FTPLOGIN = "FtpLogin";

	public final static String FTPPASSWORD = "FtpPassword";

	public final static String TIMEOUT = "FtpTimeout";

	public final static String PROXYHOST = "ProxyHost";

	public final static String PROXYPORT = "ProxyPort";

	public final static String USEPROXY = "UseProxy";

	public final static String ASKPSWD = "SavePassword";

	public final static String PASSIVEMODE = "PassiveMode";

	public final static String PUBMODE = "PublishingMode";

	public final static String HTMLTEMPL = "HTMLTemplate";
	
	public final static String EMAIL_HTMLTEMPL = "Email"+HTMLTEMPL;
	
	public final static String NNTP_HTMLTEMPL = "NNTP"+HTMLTEMPL;
	
	public final static String XML_HTMLTEMPL = "XML"+HTMLTEMPL;

	public final static String RESIZEIMAGE = "ResizedImage";

	public final static String FTP_WEBROOT = "FtpWebRoot";

	public final static String HTTP_WEBROOT = "HttpWebRoot";

	public final static String LOCAL_WEBROOT = "LocalWebRoot";

	public final static String NNTP_WEBROOT = "NntpWebRoot"; // could be a
																// news group

	public final static String LOCAL_IMAGEPATH = "LocalImagePath";

	public final static String HTTP_IMAGEPATH = "HttpImagePath";

	public final static String FTP_IMAGEPATH = "FtpImagePath";

	public final static String LOCAL_TNWEBPATH = "LocalThumbnailPath";

	public final static String HTTP_TNWEBPATH = "HttpThumbnailPath";

	public final static String FTP_TNWEBPATH = "FtpThumbnailPath";

	public final static String LOCAL_IMAGEURL = "LocalImageURL";

	public final static String FTP_IMAGEURL = "FtpImageURL";

	public final static String HTTP_IMAGEURL = "HttpImageURL";

	public final static String USEURLPATH = "UseURL"; // flag

	public final static String CPYPICS = "CopyPictures";

	public final static String CPYPICSONLY = "CopyWebContent";

	public final static String SMTPSERVER = "SMTPServer";

	public final static String SMTPUSER = "SMTPUser";

	public final static String ORGANIZATION = "Organization";

	public final static String REPLYADDR = "Reply";

	public final static String EMAILADDR = "E-MailAddress";

	public final static String SMTPPORT = "SMTPPort";
	
	public final static String SMTPASS = "SMTPPassword";

	public final static String USESSL = "UseSSL";

	public final static String SRVTIMEOUT = "SMTPTimeout";

	public final static String NNTPSERVER = "NNTPServer";

	public final static String NNTPPORT = "NNTPPort";

	public final static String NNTPUSER = "NNTPUser";

	public final static String NNTPORG = "NNTPOrganization";

	public final static String NNTPREPLYADDR = "NNTPRepAddr";

	public final static String NNTPEMAILADDR = "NNTPEmail";

	public final static String NNTPUSESSL = "NNTPSsl";

	public final static String NNTPSRVTIMEOUT = "NNTPTimeout";

	public final static String NNTPLOGIN = "NNTPLogin";

	public final static String NNTPPASSWD = "NNTPPassword";

	public final static String NNTPSECLOGON = "NNTPSecLogon";

	public final static String NNTPSRVLOGON = "NNTPServLogon";

	public final static String UPL_SERVLET_URL = "UploadServletURL";

	public final static String UPL_DEST_NAME = "UploadDestName";

	public final static String UPL_DATA_NAME = "UploadDataName";

	public final static String HTTPLOGIN = "HttpLogin";

	public final static String HTTPPASSWORD = "HttpPassword";

	public final static String HTTPLOGIN_NAME = "HttpLoginName";

	public final static String HTTPPASSWORD_NAME = "HttpPasswordName";

	public final static String HTTP_AUTH_SHC = "HttpAuthentication";

	public final static String HTTP_MANUAL_MODE = "HttpManualMode";

	public final static String HTTPKEEPPSWD = "HttpKeepPswd";

	public final static String HTTPLOGINURL = "HttpLoginURL";

	public final static String HTTPLOGINMETHOD = "HttpLoginMethod";

	public final static String HTTPSTATICQUERY = "HttpStaticQuery";

	public final static String HTTPALBUMNAME = "HttpAlbumName";

	public final static String HTTPALBUMID = "HttpAlbumId";

	public final static int LOCAL = 0;

	public final static int FTP = 1;

	public final static int HTTP = 2;

	public final static int EMAIL = 3;

	public final static int XML_SVG = 4;

	public final static int NNTP = 5;

	final static int DEFTIMEOUT = 20;

	public WebPublishOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(8, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(new JLabel(Resources.LABEL_HTML_TEMPLATE), "0,0,2");
		add(tf_htmltmpl = new JTextField(), "2,0,4");
		add(bt_brws_htmltmpl = new JButton(Resources.CMD_BROWSE), "6,0,2");
		bt_brws_htmltmpl.addActionListener(this);
		add(new JLabel(Resources.LABEL_WEB_ROOT), "0,1,2");
		add(tf_webroot = new JTextField(), "2,1,4");
		add(bt_brws_wroot = new JButton(Resources.CMD_BROWSE), "6,1,2");
		bt_brws_wroot.addActionListener(this);
		// add(new JLabel(Resources.LABEL_USE), "0,2");
		bt_setup = new JButton(Resources.CMD_SETUP);
		add(bt_setup, "6,2,2");
		bt_setup.addActionListener(this); // set action listener the dialog
											// itself
		publishModeSel = new RadioButtonsGroup(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				// System.err.println("Change event "+e+", index:"+pubType);
				savePublishTypeSpecific(pubType);
				pubType = publishModeSel.getSelectedIndex();
				loadPublishTypeSpecific(pubType);
			}
		});
		JRadioButton rb;
		add(rb = new JRadioButton(Resources.LABEL_USE_LOCAL), "5,2");
		publishModeSel.add(rb, LOCAL);
		add(rb = new JRadioButton(Resources.LABEL_USE_FTP), "1,2");
		publishModeSel.add(rb, FTP);
		add(rb = new JRadioButton(Resources.LABEL_USE_HTTP), "2,2");
		publishModeSel.add(rb, HTTP);
		add(rb = new JRadioButton(Resources.LABEL_USE_SMTP), "3,2");
		publishModeSel.add(rb, EMAIL);
		add(rb = new JRadioButton(Resources.LABEL_USE_NNTP), "0,2");
		publishModeSel.add(rb, NNTP);
		add(rb = new JRadioButton(Resources.LABEL_USE_XML), "4,2");
		publishModeSel.add(rb, XML_SVG);
		add(new JLabel(Resources.LABEL_TN_LOC), "0,3,2");
		add(tf_tnwebpath = new JTextField(), "2,3,4");
		add(new JLabel(Resources.LABEL_ADD_RESIZED), "6,3,0");
		add(new JLabel(Resources.LABEL_PIC_LOC), "0,4,2");
		add(tf_webpath = new JTextField(), "2,4,4");
		add(cx_resize = new JComboBox(Resources.LIST_SIZES), "6,4,0");
		add(cb_notCpyImgs = new JCheckBox(Resources.LABEL_CPY_PIC), "1,5,2");
		add(cb_notCpyThmbHtml = new JCheckBox(Resources.LABEL_NOTCOPY_THUMBS), "3,5,3");
		add(cb_webpic = new JCheckBox(Resources.LABEL_USE_WEB_PIC), "6,5,2");

		add(new JLabel(Resources.LABEL_PIC_URL), "0,6,2");
		add(tf_pic_url = new JTextField(), "2,6,4");
		cb_webpic.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				tf_pic_url.setEnabled(((AbstractButton) e.getSource()).isSelected());
			}
		});
		cb_notCpyImgs.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				boolean selected = ((AbstractButton) e.getSource()).isSelected();
				tf_webpath.setEnabled(!selected);
				if (selected && cb_notCpyThmbHtml.isSelected())
					cb_notCpyThmbHtml.setSelected(false);
			}
		});
		cb_notCpyThmbHtml.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (cb_notCpyImgs.isSelected() && cb_notCpyThmbHtml.isSelected())
					cb_notCpyImgs.setSelected(false);
			}
		});
	}

	public String getHelp() {
		return "No help";
	}

	public void actionPerformed(ActionEvent a) {
		if (a.getSource() == bt_brws_wroot) {
			FileSystemView fv = null;
			if (publishModeSel.getSelectedIndex() == FTP) {
				controller.setWaitCursor(getTopLevelAncestor()/* specific case */, true);
				Ftp ftp = new Ftp(getConnectionInfo(controller.getPrefs()), false);
				if (!ftp.isConnected) {
					controller.setWaitCursor(getTopLevelAncestor(), false);
					JOptionPane.showMessageDialog(this, Resources.LABEL_ERR_FTP_CONNECT, Resources.TITLE_ERROR,
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					ftp.login();
					fv = new FtpFileSystemView(ftp);
				} catch (IOException e) {
					controller.setWaitCursor(getTopLevelAncestor(), false);
					JOptionPane.showMessageDialog(this, Resources.LABEL_ERR_FTP_LOGIN + e, Resources.TITLE_ERROR,
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (publishModeSel.getSelectedIndex() == LOCAL) {
				JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), tf_webroot.getText(), fv);
				controller.setWaitCursor(getTopLevelAncestor(), false);
				if (dc.getDirectory() != null)
					tf_webroot.setText(dc.getDirectory());
			}
		} else if (a.getSource() == bt_brws_htmltmpl) {
			String d = tf_htmltmpl.getText();
			if (d.length() == 0)
				d = controller.getPrefs().getHomeDirectory();
			JFileChooser fc = new JFileChooser(d);
			fc.addChoosableFileFilter(controller.getHtmlFilter());
			fc.setFileFilter(controller.getHtmlFilter());
			fc.setDialogTitle(Resources.TITLE_HTML_TEMPL);
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
				tf_htmltmpl.setText(fc.getSelectedFile().getAbsolutePath());
		} else if (a.getActionCommand().equals(Resources.CMD_SETUP)) {
			switch (publishModeSel.getSelectedIndex()) {
			case FTP:
				if (ftpdialog == null)
					ftpdialog = new FtpDialog();
				ftpdialog.setVisible(true);
				break;
			case HTTP: // http
				new HttpSettingsDialog().setVisible(true);
				break;
			case EMAIL:
				new MailSettingsDialog().setVisible(true);
				break;
			case XML_SVG: // SVG
				break;
			case NNTP:
				new NewsSettingsDialog().setVisible(true);
			}
		}
	}

	protected void savePublishTypeSpecific(int type) {
		IniPrefs s = controller.getPrefs();
		// System.err.println("Save index:"+type);
		String medPath = tf_webpath.getText(), rtPath = tf_webroot.getText(), medUrl = tf_pic_url.getText(), tnPath = tf_tnwebpath
				.getText(), templ=tf_htmltmpl.getText();
		boolean templStored = false;
		switch (type) {
		case FTP:
			s.setProperty(SECNAME, FTP_IMAGEPATH, medPath);
			s.setProperty(SECNAME, FTP_TNWEBPATH, tnPath);
			s.setProperty(SECNAME, FTP_WEBROOT, rtPath);
			s.setProperty(SECNAME, FTP_IMAGEURL, medUrl);
			break;
		case HTTP: // http
			s.setProperty(SECNAME, HTTP_IMAGEPATH, medPath);
			s.setProperty(SECNAME, HTTP_TNWEBPATH, tnPath);
			s.setProperty(SECNAME, HTTP_WEBROOT, rtPath);
			s.setProperty(SECNAME, HTTP_IMAGEURL, medUrl);
			break;
		case LOCAL:
			s.setProperty(SECNAME, LOCAL_IMAGEPATH, medPath);
			s.setProperty(SECNAME, LOCAL_TNWEBPATH, tnPath);
			s.setProperty(SECNAME, LOCAL_WEBROOT, rtPath);
			s.setProperty(SECNAME, LOCAL_IMAGEURL, medUrl);
			break;
		case EMAIL:
			s.setProperty(SECNAME, EMAIL_HTMLTEMPL, templ);
			templStored = true;
			break;
		case NNTP:
			s.setProperty(SECNAME, NNTP_HTMLTEMPL, templ);
			templStored = true;
			break;
		case XML_SVG:
			s.setProperty(SECNAME, XML_HTMLTEMPL, templ);
			templStored = true;
			break;
		}
		if (templStored == false)
			s.setProperty(SECNAME, HTMLTEMPL, templ);
	}

	protected void loadPublishTypeSpecific(int type) { // publishModeSel.getSelectedIndex()
		IniPrefs s = controller.getPrefs();
		String medPath = null, tnPath = null, rtPath = null, medUrl = null, templ=(String) s.getProperty(SECNAME, HTMLTEMPL);
		boolean en = true, enu = true, ens = true;
		switch (type) {
		case FTP:
			medPath = (String) s.getProperty(SECNAME, FTP_IMAGEPATH);
			tnPath = (String) s.getProperty(SECNAME, FTP_TNWEBPATH);
			rtPath = (String) s.getProperty(SECNAME, FTP_WEBROOT);
			medUrl = (String) s.getProperty(SECNAME, FTP_IMAGEURL);
			break;
		case HTTP: // http
			medPath = (String) s.getProperty(SECNAME, HTTP_IMAGEPATH);
			tnPath = (String) s.getProperty(SECNAME, HTTP_TNWEBPATH);
			rtPath = (String) s.getProperty(SECNAME, HTTP_WEBROOT);
			medUrl = (String) s.getProperty(SECNAME, HTTP_IMAGEURL);
			break;
		case XML_SVG: // SVG
			ens = false;
			templ = (String)s.getProperty(SECNAME, XML_HTMLTEMPL);
		case NNTP:
			templ = (String)s.getProperty(SECNAME, NNTP_HTMLTEMPL);
			enu = false;
		case EMAIL:
			templ = (String)s.getProperty(SECNAME, EMAIL_HTMLTEMPL);
			en = false;
			break;
		case LOCAL:
			ens = false;
			medPath = (String) s.getProperty(SECNAME, LOCAL_IMAGEPATH);
			tnPath = (String) s.getProperty(SECNAME, LOCAL_TNWEBPATH);
			rtPath = (String) s.getProperty(SECNAME, LOCAL_WEBROOT);
			medUrl = (String) s.getProperty(SECNAME, LOCAL_IMAGEURL);
		}
		// System.err.println("Load index:"+type+", path:"+rtPath);
		if (medPath != null)
			tf_webpath.setText(medPath);
		else
			tf_webpath.setText("");
		tf_webpath.setEnabled(en);
		if (rtPath != null)
			tf_webroot.setText(rtPath);
		else
			tf_webroot.setText("");
		tf_webroot.setEnabled(en);
		if (medUrl != null)
			tf_pic_url.setText(medUrl);
		else
			tf_pic_url.setText("");
		tf_pic_url.setEnabled(enu);
		if (tnPath != null)
			tf_tnwebpath.setText(tnPath);
		else
			tf_tnwebpath.setText("");
		if (templ != null)
			tf_htmltmpl.setText(templ);
		else
			tf_htmltmpl.setText("");
		tf_tnwebpath.setEnabled(en);
		bt_setup.setEnabled(ens);
		bt_brws_wroot.setEnabled(en);
	}

	public static FtpConnectionInfo getConnectionInfo(IniPrefs s) {
		connectioninfo.startDirectory = (String) Controller.nullToEmpty(s.getProperty(SECNAME, FTP_WEBROOT));
		connectioninfo.host = (String) Controller.nullToEmpty(s.getProperty(SECNAME, HOSTNAME));
		connectioninfo.user = (String) Controller.nullToEmpty(s.getProperty(SECNAME, FTPLOGIN));
		String ps = (String) s.getProperty(SECNAME, FTPPASSWORD);
		if (ps != null) {
			try {
				connectioninfo.setPassword(DataConv.encryptXor(new String(DataConv.hexToBytes(ps),
						Controller.ISO_8859_1)));
			} catch (UnsupportedEncodingException uee) {
			}
		}
		connectioninfo.proxyHost = (String) s.getProperty(SECNAME, PROXYHOST);
		try {
			connectioninfo.proxyPort = ((Integer) s.getProperty(SECNAME, PROXYPORT)).intValue();
		} catch (Throwable e) {
			connectioninfo.proxyPort = 27;
		}
		try {
			connectioninfo.timeout = ((Integer) s.getProperty(SECNAME, TIMEOUT)).intValue();
		} catch (Throwable e) {
			connectioninfo.timeout = DEFTIMEOUT * 1000;
		}
		try {
			connectioninfo.useProxy = ((Integer) s.getProperty(SECNAME, USEPROXY)).intValue() == 1;
		} catch (Throwable e) {
			connectioninfo.useProxy = false;
		}
		try {
			connectioninfo.active = ((Integer) s.getProperty(SECNAME, PASSIVEMODE)).intValue() == 0;
		} catch (Throwable e) {
			connectioninfo.active = true;
		}
		try {
			connectioninfo.askPassword = ((Integer) s.getProperty(SECNAME, ASKPSWD)).intValue() == 1;
		} catch (Throwable e) {
			connectioninfo.askPassword = true;
		}
		return connectioninfo;
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		Integer i = (Integer) s.getProperty(SECNAME, PUBMODE);
		if (i != null)
			pubType = i.intValue();
		else
			pubType = 0;
		loadPublishTypeSpecific(pubType);
		publishModeSel.setSelectedIndex(pubType);
		getConnectionInfo(s);
		// if (connectioninfo.startDirectory != null)
		// tf_webroot.setText(connectioninfo.startDirectory);
		cb_webpic.setSelected(s.getInt(s.getProperty(SECNAME, USEURLPATH), 0) == 1);
		boolean picsonly = s.getInt(s.getProperty(SECNAME, CPYPICSONLY), 1) == 0;
		cb_notCpyThmbHtml.setSelected(picsonly);
		cb_notCpyImgs.setSelected(s.getInt(s.getProperty(SECNAME, CPYPICS), 1) == 0 && picsonly);
		cx_resize.setSelectedIndex(s.getInt(s.getProperty(SECNAME, RESIZEIMAGE), 0));
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		pubType = publishModeSel.getSelectedIndex();
		if (pubType < 0)
			pubType = 0;
		s.setProperty(SECNAME, PUBMODE, new Integer(pubType));
		savePublishTypeSpecific(pubType);		
		s.setProperty(SECNAME, USEURLPATH, cb_webpic.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, CPYPICS, cb_notCpyImgs.isSelected() ? Resources.I_NO : Resources.I_YES);
		s.setProperty(SECNAME, CPYPICSONLY, cb_notCpyThmbHtml.isSelected() ? Resources.I_NO : Resources.I_YES);
		s.setProperty(SECNAME, RESIZEIMAGE, new Integer(cx_resize.getSelectedIndex()));
		getConnectionInfo(s);
	}

	Controller controller;

	int pubType;

	JTextField tf_webroot, tf_htmltmpl, tf_webpath, tf_tnwebpath, tf_pic_url;

	JCheckBox cb_webpic, cb_notCpyImgs, cb_notCpyThmbHtml;

	JButton bt_brws_htmltmpl, bt_brws_wroot, bt_setup;

	JComboBox cx_resize;

	RadioButtonsGroup publishModeSel;

	static FtpConnectionInfo connectioninfo = new FtpConnectionInfo();

	FtpDialog ftpdialog;

	abstract class PersistantDialog extends JDialog implements Persistancable, ActionListener {

		PersistantDialog(JFrame owner, String title, boolean modal) {
			super(owner, title, modal);
		}

		protected void processWindowEvent(WindowEvent e) {
			if (e.getID() == WindowEvent.WINDOW_CLOSING)
				save();
			super.processWindowEvent(e);
		}

		public void actionPerformed(ActionEvent a) {
			if (a.getActionCommand().equals(Resources.CMD_RESET)) {
				load();
			}
		}
	}

	class FtpDialog extends PersistantDialog {
		FtpDialog() {
			super((JFrame) WebPublishOptionsTab.this.getTopLevelAncestor(), Resources.TITLE_FTP, true);
			java.awt.Container c = getContentPane();
			c.setLayout(new FixedGridLayout(2, 15, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP, 7));
			c.add(new JLabel(Resources.LABEL_FTP_HOST), "0,0,2");
			c.add(tf_hostname = new JTextField(), "0,1,2");

			c.add(new JLabel(Resources.LABEL_LOGIN), "0,2,2");
			c.add(tf_user = new JTextField(), "0,3,2");
			c.add(new JLabel(Resources.LABEL_PASSWORD), "0,4,1");
			c.add(tf_pswd = new JPasswordField(), "0,5,1");
			c.add(cb_keeppswd = new JCheckBox(Resources.LABEL_SAVE_PSWD), "1,5,1");
			c.add(cb_passivemode = new JCheckBox(Resources.LABEL_PASSIVE_MODE), "0,6,1");
			c.add(new JLabel(Resources.LABEL_CONN_TIMEOUT), "0,7,2");
			c.add(sl_conntimeout = new JSlider(0, 120, 20), "0,9,1,0");
			c.add(l_timeout = new JLabel(Resources.LABEL_SEC), "1,9,1");
			c.add(cb_useproxy = new JCheckBox(Resources.LABEL_USE_PROXY), "0,10,1");
			c.add(new JLabel(Resources.LABEL_HOST), "0,11,1");
			c.add(tf_proxyhost = new JTextField(), "0,12,2");
			c.add(new JLabel(Resources.LABEL_PORT), "0,13,1");
			c.add(tf_proxyport = new JTextField(), "0,14,1");
			tf_proxyport.setHorizontalAlignment(JTextField.RIGHT);
			JButton b;
			c.add(b = new JButton(Resources.CMD_RESET), "1,14,1");
			b.addActionListener(this);
			sl_conntimeout.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (e.getSource() == sl_conntimeout)
						l_timeout.setText("" + sl_conntimeout.getValue() + " " + Resources.LABEL_SEC);
				}
			});
			cb_useproxy.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					boolean enabled = ((AbstractButton) e.getSource()).isSelected();
					tf_proxyhost.setEnabled(enabled);
					tf_proxyport.setEnabled(enabled);
				}
			});
			cb_keeppswd.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					boolean enabled = ((AbstractButton) e.getSource()).isSelected();
					if (!enabled)
						tf_pswd.setText("");
					tf_pswd.setEnabled(enabled);
				}
			});
			pack();
			load();
		}

		public void save() {
			IniPrefs s = controller.getPrefs();
			s.setProperty(SECNAME, HOSTNAME, tf_hostname.getText());
			s.setProperty(SECNAME, FTPLOGIN, tf_user.getText());
			s.setProperty(SECNAME, PROXYHOST, tf_proxyhost.getText());
			Integer i;
			try {
				i = new Integer(tf_proxyport.getText());
			} catch (NumberFormatException e) {
				i = Resources.I_NO;
			}
			s.setProperty(SECNAME, PROXYPORT, i);
			// DEFTIMEOUT
			s.setProperty(SECNAME, TIMEOUT, new Integer(sl_conntimeout.getValue() * 1000));
			s.setProperty(SECNAME, USEPROXY, cb_useproxy.isSelected() ? Resources.I_YES : Resources.I_NO);
			s.setProperty(SECNAME, ASKPSWD, cb_keeppswd.isSelected() ? Resources.I_NO : Resources.I_YES);
			s.setProperty(SECNAME, PASSIVEMODE, cb_passivemode.isSelected() ? Resources.I_YES : Resources.I_NO);
			if (cb_keeppswd.isSelected()) {
				try {
					s.setProperty(SECNAME, FTPPASSWORD, DataConv.bytesToHex(DataConv.encryptXor(
							new String(tf_pswd.getPassword())).getBytes(Controller.ISO_8859_1)));
				} catch (UnsupportedEncodingException uee) {
				}
			} else
				s.setProperty(SECNAME, FTPPASSWORD, "");
		}

		public void load() {
			if (connectioninfo.host != null)
				tf_hostname.setText(connectioninfo.host);
			if (connectioninfo.user != null)
				tf_user.setText(connectioninfo.user);
			if (!connectioninfo.askPassword && connectioninfo.getPassword() != null)
				tf_pswd.setText(connectioninfo.getPassword());
			tf_proxyport.setText("" + connectioninfo.proxyPort);
			sl_conntimeout.setValue(connectioninfo.timeout / 1000);
			l_timeout.setText("" + sl_conntimeout.getValue() + " " + Resources.LABEL_SEC);
			if (connectioninfo.proxyHost != null)
				tf_proxyhost.setText(connectioninfo.proxyHost);
			cb_useproxy.setSelected(connectioninfo.useProxy);
			cb_keeppswd.setSelected(!connectioninfo.askPassword);
			cb_passivemode.setSelected(!connectioninfo.active);
		}

		JSlider sl_conntimeout;

		JPasswordField tf_pswd;

		JTextField tf_proxyhost, tf_proxyport, tf_hostname, tf_user;

		JCheckBox cb_useproxy, cb_keeppswd, cb_passivemode;

		JLabel l_timeout;
	}

	class MailSettingsDialog extends PersistantDialog {
		MailSettingsDialog() {
			super((JFrame) WebPublishOptionsTab.this.getTopLevelAncestor(), Resources.TITLE_EMAILS, true);
			java.awt.Container c = getContentPane();
			c.setLayout(new FixedGridLayout(3, 9, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP, 6));
			c.add(new JLabel(Resources.LABEL_NAME), "0,0,1");
			c.add(tf_name = new JTextField(), "1,0,2");
			c.add(new JLabel(Resources.LABEL_ORG), "0,1,1");
			c.add(tf_organization = new JTextField(), "1,1,2");
			c.add(new JLabel(Resources.LABEL_EMAILA), "0,2,1");
			c.add(tf_email = new JTextField(), "1,2,2");
			c.add(new JLabel(Resources.LABEL_REPLYA), "0,3,1");
			c.add(tf_reply = new JTextField(), "1,3,2");
			c.add(new JLabel(Resources.LABEL_SMTPSRV), "0,4,1");
			c.add(tf_server = new JTextField(), "1,4,2");
			c.add(new JLabel(Resources.LABEL_SMTPPRT), "0,5,1");
			c.add(tf_port = new JTextField("25"), "1,5,1");
			tf_port.setHorizontalAlignment(JTextField.RIGHT);
			c.add(cb_ssl = new JCheckBox(Resources.LABEL_SSL), "2,5,1");
			
			c.add(new JLabel(Resources.LABEL_PASSWORD), "0,6,1");
			c.add(pf_pass = new JPasswordField(""), "1,6,1");
			
			c.add(new JLabel(Resources.LABEL_SRVTIMEOUT), "0,7,1");
			JButton b;
			c.add(b = new JButton(Resources.CMD_RESET), "2,7,1");
			b.addActionListener(this);
			c.add(sl_timeout = new JSlider(0, 120, 20), "0,8,2,0"); // JSlider.HORIZONTAL
			sl_timeout.setMajorTickSpacing(30);
			sl_timeout.setMinorTickSpacing(5);
			sl_timeout.setSnapToTicks(true);
			/*
			 * sl_timeout.setLabelTable(sl_timeout.createStandardLabels(30));
			 * sl_timeout.setPaintLabels(true); sl_timeout.setPaintTicks(true);
			 * sl_timeout.setPaintTrack(true);
			 */
			c.add(l_timeout = new JLabel(Resources.LABEL_SEC), "2,7,1");
			sl_timeout.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (e.getSource() == sl_timeout)
						l_timeout.setText("" + sl_timeout.getValue() + " " + Resources.LABEL_SEC);
				}
			});
			pack();
			load();
		}

		public void save() {
			IniPrefs s = controller.getPrefs();
			s.setProperty(SECNAME, SMTPSERVER, tf_server.getText().trim());
			s.setProperty(SECNAME, SMTPUSER, tf_name.getText());
			s.setProperty(SECNAME, ORGANIZATION, tf_organization.getText());
			s.setProperty(SECNAME, REPLYADDR, tf_reply.getText());
			s.setProperty(SECNAME, EMAILADDR, tf_email.getText());
			s.setProperty(SECNAME, SMTPASS, controller.crypto.encrypt(new String(pf_pass.getPassword())));
			try {
				s.setProperty(SECNAME, SMTPPORT, new Integer(tf_port.getText()));
			} catch (NumberFormatException e) {
			}
			s.setProperty(SECNAME, USESSL, cb_ssl.isSelected() ? Resources.I_YES : Resources.I_NO);
			s.setProperty(SECNAME, SRVTIMEOUT, new Integer(sl_timeout.getValue()));
		}

		public void load() {
			IniPrefs s = controller.getPrefs();
			String ps;
			cb_ssl.setSelected(s.getInt(s.getProperty(SECNAME, USESSL), 0) == 1);
			sl_timeout.setValue(s.getInt(s.getProperty(SECNAME, SRVTIMEOUT), 20));
			l_timeout.setText("" + sl_timeout.getValue() + " " + Resources.LABEL_SEC);
			ps = (String) s.getProperty(SECNAME, SMTPSERVER);
			if (ps != null)
				tf_server.setText(ps);
			ps = DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, SMTPUSER)), ',');
			if (ps != null)
				tf_name.setText(ps);
			ps = DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, ORGANIZATION)), ',');
			if (ps != null)
				tf_organization.setText(ps);
			ps = (String) s.getProperty(SECNAME, REPLYADDR);
			if (ps != null)
				tf_reply.setText(ps);
			ps = (String) s.getProperty(SECNAME, EMAILADDR);
			if (ps != null)
				tf_email.setText(ps);
			tf_port.setText("" + s.getInt(s.getProperty(SECNAME, SMTPPORT), 25));
			ps = (String) s.getProperty(SECNAME, SMTPASS);
			if (ps != null)
				pf_pass.setText(controller.crypto.decrypt(ps));
		}

		JTextField tf_name, tf_organization, tf_email, tf_reply, tf_server, tf_port;
		
		JPasswordField pf_pass; 

		JCheckBox cb_ssl;

		JSlider sl_timeout;

		JLabel l_timeout;
	}

	class HttpSettingsDialog extends PersistantDialog {
		HttpSettingsDialog() {
			super((JFrame) WebPublishOptionsTab.this.getTopLevelAncestor(), Resources.TITLE_HTTP, true);
			java.awt.Container c = getContentPane();
			c.setLayout(new FixedGridLayout(2, 18, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP, 8));
			c.add(new JLabel(Resources.LABEL_UPLOAD_SERVLET_URL), "0,0,2");
			c.add(tf_servletUrl = new JTextField(), "0,1,2");
			c.add(new JLabel(Resources.LABEL_UPLOAD_DST_NAME), "0,2,1");
			c.add(tf_inputDst = new JTextField(), "1,2,1");
			c.add(new JLabel(Resources.LABEL_UPLOAD_DATA_NAME), "0,3,1");
			c.add(tf_inputData = new JTextField(), "1,3,1");
			c.add(new JLabel(Resources.LABEL_HTTP_AUTH), "0,4,1");
			c.add(cb_auths = new JComboBox(Resources.HTTP_AUTHS), "1,4,1");			
			c.add(new JLabel(Resources.LABEL_HTTP_LOGIN_URL), "0,5,1");
			c.add(tf_login_url = new JTextField(), "0,6,2");
			c.add(new JLabel(Resources.LABEL_LOGIN_NAME), "0,7,1");
			c.add(tf_loginName = new JTextField(), "1,7,1");
			c.add(new JLabel(Resources.LABEL_PASSWORD_NAME), "0,8,1");
			c.add(tf_passwordName = new JTextField(), "1,8,1");
			c.add(new JLabel(Resources.LABEL_HTTP_LOGIN_METHOD), "0,9,1");
			c.add(cb_loginMethod = new JComboBox(Resources.HTTP_LOGIN_METHODS), "1,9,1");
			c.add(new JLabel(Resources.LABEL_LOGIN), "0,10,1");
			c.add(tf_user = new JTextField(), "1,10,1");
			c.add(new JLabel(Resources.LABEL_PASSWORD), "0,11,1");
			c.add(tf_password = new JPasswordField(), "1,11,1");
			// for album
			c.add(new JLabel(Resources.LABEL_ALBUMNAME), "0,12,1");
			c.add(tf_albumName = new JTextField(), "1,12,1");
			c.add(new JLabel(Resources.LABEL_ALBUM_ID), "0,13,1");
			c.add(tf_albumId = new JTextField(), "1,13,1");

			c.add(new JLabel(Resources.LABEL_STATIC_QUERY), "0,14,2");
			c.add(tf_staticQuery = new JTextField(), "0,15,2");
			cb_auths.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					updateUI();
				}
			});

			c.add(cb_traceUpl = new JCheckBox(Resources.LABEL_TRACE_UPLOAD), "1,16,2");

			JButton b;
			c.add(b = new JButton(Resources.CMD_RESET), "1,17,1");
			b.addActionListener(this);
			pack();
			load();
		}

		public void save() {
			IniPrefs s = controller.getPrefs();
			s.setProperty(SECNAME, UPL_SERVLET_URL, tf_servletUrl.getText().trim());
			s.setProperty(SECNAME, UPL_DEST_NAME, tf_inputDst.getText());
			s.setProperty(SECNAME, UPL_DATA_NAME, tf_inputData.getText());
			s.setProperty(SECNAME, HTTPPASSWORD_NAME, tf_passwordName.getText());
			s.setProperty(SECNAME, HTTPSTATICQUERY, tf_staticQuery.getText());
			s.setProperty(SECNAME, HTTPLOGIN_NAME, tf_loginName.getText());
			s.setProperty(SECNAME, HTTPLOGIN, tf_user.getText());
			try {
				s.setProperty(SECNAME, HTTPPASSWORD, DataConv.bytesToHex(DataConv.encryptXor(
						new String(tf_password.getPassword())).getBytes(Controller.ISO_8859_1)));
			} catch (UnsupportedEncodingException uee) {
			}
			s.setProperty(SECNAME, HTTP_AUTH_SHC, new Integer(cb_auths.getSelectedIndex()));
			s.setProperty(SECNAME, HTTP_MANUAL_MODE, cb_traceUpl.isSelected() ? Resources.I_YES : Resources.I_NO);
			s.setProperty(SECNAME, HTTPLOGINURL, tf_login_url.getText());
			s.setProperty(SECNAME, HTTPLOGINMETHOD, new Integer(cb_loginMethod.getSelectedIndex()));
			s.setProperty(SECNAME, HTTPALBUMNAME, tf_albumName.getText());
			s.setProperty(SECNAME, HTTPALBUMID, tf_albumId.getText());
		}

		public void load() {
			IniPrefs s = controller.getPrefs();
			cb_auths.setSelectedIndex(IniPrefs.getInt(s.getProperty(SECNAME, HTTP_AUTH_SHC), 0));
			updateUI();
			tf_servletUrl.setText(DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, UPL_SERVLET_URL)), ','));
			tf_inputDst.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, UPL_DEST_NAME)));
			tf_inputData.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, UPL_DATA_NAME)));
			tf_loginName.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPLOGIN_NAME)));
			tf_passwordName.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPPASSWORD_NAME)));
			tf_staticQuery.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPSTATICQUERY)));
			tf_user.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPLOGIN)));
			tf_albumName.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPALBUMNAME)));
			tf_albumId.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPALBUMID)));
			String sp = (String) s.getProperty(SECNAME, HTTPPASSWORD);
			if (sp != null) {
				try {
					tf_password
							.setText(DataConv.encryptXor(new String(DataConv.hexToBytes(sp), Controller.ISO_8859_1)));
				} catch (UnsupportedEncodingException uee) {
				}
			}
			cb_traceUpl.setSelected(s.getInt(s.getProperty(SECNAME, HTTP_MANUAL_MODE), 0) == 1);
			tf_login_url.setText((String) Controller.nullToEmpty(s.getProperty(SECNAME, HTTPLOGINURL)));
			cb_loginMethod.setSelectedIndex(IniPrefs.getInt(s.getProperty(SECNAME, HTTPLOGINMETHOD), 0));
		}

		private void updateUI() {
			int authmode = cb_auths.getSelectedIndex();
			boolean enabled = authmode > 0;
			tf_user.setEnabled(enabled);
			tf_password.setEnabled(enabled);
			cb_loginMethod.setEnabled(enabled);
			tf_login_url.setEnabled(enabled);
			tf_loginName.setEnabled(enabled);
			tf_passwordName.setEnabled(enabled);
			tf_staticQuery.setEnabled(enabled);
			if (authmode == HTTPCourier.AUTH_GOOGLE_AUTH) {
				tf_login_url.setText("https://www.google.com/accounts/ClientLogin");
				cb_loginMethod.setSelectedIndex(HTTPCourier.METHOD_POST);
				tf_staticQuery.setText("accountType=GOOGLE&source=rogatkin-MediaChest-1.6&service=lh2");
				tf_loginName.setText("Email");
				tf_passwordName.setText("Passwd");
				tf_servletUrl.setText("http://picasaweb.google.com/data/feed/api/user/%s/albumid/%s");
				tf_inputDst.setEnabled(false);
				tf_inputData.setEnabled(false);
				tf_albumId.setEnabled(enabled);
				tf_albumName.setEnabled(enabled);
				tf_albumName.setText("http://picasaweb.google.com/data/feed/api/user/%s");
			} else {
				tf_inputDst.setEnabled(enabled);
				tf_inputData.setEnabled(enabled);
				//tf_login_url.setText("");
			}
		}

		JTextField tf_servletUrl, tf_inputDst, tf_inputData, tf_user, tf_login_url, tf_loginName, tf_passwordName,
				tf_staticQuery, tf_albumName, tf_albumId;

		JPasswordField tf_password;

		JComboBox cb_auths, cb_loginMethod;

		JCheckBox cb_traceUpl;
	}

	class NewsSettingsDialog extends PersistantDialog {
		NewsSettingsDialog() {
			super((JFrame) WebPublishOptionsTab.this.getTopLevelAncestor(), Resources.TITLE_NEWS, true);
			java.awt.Container c = getContentPane();
			c.setLayout(new FixedGridLayout(3, 12, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP, 6));
			c.add(new JLabel(Resources.LABEL_NAME), "0,0,1");
			c.add(tf_name = new JTextField(), "1,0,2");
			c.add(new JLabel(Resources.LABEL_ORG), "0,1,1");
			c.add(tf_organization = new JTextField(), "1,1,2");
			c.add(new JLabel(Resources.LABEL_EMAILA), "0,2,1");
			c.add(tf_email = new JTextField(), "1,2,2");
			c.add(new JLabel(Resources.LABEL_REPLYA), "0,3,1");
			c.add(tf_reply = new JTextField(), "1,3,2");
			c.add(new JLabel(Resources.LABEL_NEWSSRV), "0,4,1");
			c.add(tf_server = new JTextField(), "1,4,2");
			c.add(new JLabel(Resources.LABEL_NEWSPORT), "0,5,1");
			c.add(tf_port = new JTextField("119"), "1,5,1");
			tf_port.setHorizontalAlignment(JTextField.RIGHT);
			c.add(cb_log_req = new JCheckBox(Resources.LABEL_NNTP_LOGIN_REQ), "0,6,2");
			c.add(new JLabel(Resources.LABEL_LOGIN), "0,7,1");
			c.add(tf_login = new JTextField(), "1,7,1");
			c.add(new JLabel(Resources.LABEL_PASSWORD), "0,8,1");
			c.add(tf_password = new JPasswordField(), "1,8,1");
			c.add(cb_sec_aut = new JCheckBox(Resources.LABEL_SECURE_LOGIN), "0,9,2");
			c.add(cb_ssl = new JCheckBox(Resources.LABEL_SSL), "2,9,1");
			c.add(new JLabel(Resources.LABEL_SRVTIMEOUT), "0,10,1");
			JButton b;
			c.add(b = new JButton(Resources.CMD_RESET), "2,10,1");
			b.addActionListener(this);
			c.add(sl_timeout = new JSlider(0, 120, 20), "0,11,2,0"); // JSlider.HORIZONTAL
			sl_timeout.setMajorTickSpacing(30);
			sl_timeout.setMinorTickSpacing(5);
			sl_timeout.setSnapToTicks(true);
			c.add(l_timeout = new JLabel(Resources.LABEL_SEC), "2,11,1");
			sl_timeout.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (e.getSource() == sl_timeout)
						l_timeout.setText("" + sl_timeout.getValue() + " " + Resources.LABEL_SEC);
				}
			});
			cb_log_req.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (e.getSource() == cb_log_req) {
						tf_login.setEnabled(cb_log_req.isSelected());
						tf_password.setEnabled(cb_log_req.isSelected());
					}
				}
			});
			pack();
			load();
		}

		public void save() {
			IniPrefs s = controller.getPrefs();
			s.setProperty(SECNAME, NNTPSERVER, tf_server.getText().trim());
			s.setProperty(SECNAME, NNTPUSER, tf_name.getText());
			s.setProperty(SECNAME, NNTPORG, tf_organization.getText());
			s.setProperty(SECNAME, NNTPREPLYADDR, tf_reply.getText());
			s.setProperty(SECNAME, NNTPEMAILADDR, tf_email.getText());
			s.setProperty(SECNAME, NNTPLOGIN, tf_login.getText());
			try {
				s.setProperty(SECNAME, NNTPPORT, new Integer(tf_port.getText()));
			} catch (NumberFormatException e) {
			}
			try {
				s.setProperty(SECNAME, NNTPPASSWD, DataConv.bytesToHex(DataConv.encryptXor(
						new String(tf_password.getPassword())).getBytes(Controller.ISO_8859_1)));
			} catch (UnsupportedEncodingException uee) {
			}
			s.setProperty(SECNAME, NNTPUSESSL, cb_ssl.isSelected() ? Resources.I_YES : Resources.I_NO);
			s.setProperty(SECNAME, NNTPSRVTIMEOUT, new Integer(sl_timeout.getValue()));
			s.setProperty(SECNAME, NNTPSECLOGON, cb_sec_aut.isSelected() ? Resources.I_YES : Resources.I_NO);
			s.setProperty(SECNAME, NNTPSRVLOGON, cb_log_req.isSelected() ? Resources.I_YES : Resources.I_NO);
		}

		public void load() {
			IniPrefs s = controller.getPrefs();
			String ps;
			cb_ssl.setSelected(s.getInt(s.getProperty(SECNAME, NNTPUSESSL), 0) == 1);
			sl_timeout.setValue(s.getInt(s.getProperty(SECNAME, NNTPSRVTIMEOUT), 25));
			l_timeout.setText("" + sl_timeout.getValue() + " " + Resources.LABEL_SEC);
			ps = (String) s.getProperty(SECNAME, NNTPSERVER);
			if (ps != null)
				tf_server.setText(ps);
			ps = DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, NNTPUSER)), ',');
			if (ps != null)
				tf_name.setText(ps);
			ps = DataConv.arrayToString(Controller.nullToEmpty(s.getProperty(SECNAME, NNTPORG)), ',');
			if (ps != null)
				tf_organization.setText(ps);
			ps = (String) s.getProperty(SECNAME, NNTPREPLYADDR);
			if (ps != null)
				tf_reply.setText(ps);
			ps = (String) s.getProperty(SECNAME, NNTPEMAILADDR);
			if (ps != null)
				tf_email.setText(ps);
			tf_port.setText("" + s.getInt(s.getProperty(SECNAME, NNTPPORT), 119));
			String sp = (String) s.getProperty(SECNAME, NNTPPASSWD);
			if (sp != null) {
				try {
					tf_password
							.setText(DataConv.encryptXor(new String(DataConv.hexToBytes(sp), Controller.ISO_8859_1)));
				} catch (UnsupportedEncodingException uee) {
				}
			}
			ps = (String) s.getProperty(SECNAME, NNTPLOGIN);
			if (ps != null)
				tf_login.setText(ps);
			cb_sec_aut.setSelected(s.getInt(s.getProperty(SECNAME, NNTPSECLOGON), 0) == 1);
			cb_log_req.setSelected(s.getInt(s.getProperty(SECNAME, NNTPSRVLOGON), 0) == 1);
			updateUI();
		}

		private void updateUI() {
			boolean enabled = cb_log_req.isSelected();
			tf_login.setEnabled(enabled);
			tf_password.setEnabled(enabled);
		}

		JTextField tf_name, tf_organization, tf_email, tf_reply, tf_server, tf_port, tf_login;

		JPasswordField tf_password;

		JCheckBox cb_ssl, cb_sec_aut, cb_log_req;

		JSlider sl_timeout;

		JLabel l_timeout;
	}
}