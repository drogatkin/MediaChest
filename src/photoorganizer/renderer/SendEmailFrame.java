/* MediaChest - SendEmailFrame 
 * Copyright (C) 1999-2000 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: SendEmailFrame.java,v 1.17 2008/03/03 08:37:44 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

import mediautil.gen.MediaFormat;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.HtmlProducer;
import photoorganizer.PhotoOrganizer;
import photoorganizer.Resources;
import photoorganizer.courier.MailCourier;
import addressbook.AddressBookFrame;
import addressbook.util.ActionPerformer;

// Java mail implementation
public class SendEmailFrame extends JFrame implements ActionListener, ActionPerformer {
	static final int MAX_SUBJECT_LENGTH = 100;

	Controller controller;

	AddressBookFrame ab;

	Object[] medias_to_send;

	public SendEmailFrame(Controller controller, Object[] medias) {
		super(Resources.TITLE_SENDTO);
		this.controller = controller;
		setIconImage(controller.getMainIcon());
		medias_to_send = medias;
		// getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new SendEmail(), "Center");
		getContentPane().add(new ActionButtons(), "South");
		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.CMD_ADDRESSBOOK)) {
			ActionPerformer ap = (ActionPerformer) controller.component(AddressBookFrame.COMP_ACTIONPERFORMER);
			if (ap == null || ap != this)
				controller.add(this, AddressBookFrame.COMP_ACTIONPERFORMER);
			ab = (AddressBookFrame) controller.component(AddressBookFrame.COMP_ADDRESSBOOK);
			if (ab == null || !ab.isShowing()) {
				ab = new AddressBookFrame(controller);
				controller.add(ab, AddressBookFrame.COMP_ADDRESSBOOK);
			} else {
				if (ab.getState() == Frame.ICONIFIED)
					ab.setState(Frame.NORMAL);
				ab.requestFocus();
			}
			return;
		} else if (cmd.equals(Resources.CMD_OK)) {
			new Thread(new Runnable() {
				public void run() {
					send();
				}
			}, "E-Mail sender").start();
		}
		controller.add((String) null, AddressBookFrame.COMP_ACTIONPERFORMER);
		dispose();
	}

	public void act(int _action, Object _value) {
		if (_action == ActionPerformer.SENDMAIL) {
			String el = tf_to.getText();
			if (el.length() > 0)
				tf_to.setText(el + ',' + (String) _value);
			else
				tf_to.setText((String) _value);
		}
	}

	void send() {
		Properties props = System.getProperties();
		IniPrefs s = controller.getPrefs();
		String ssl = s.getInt(s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.USESSL), 0) == 1 ? "s"
				: "";
		String host = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.SMTPSERVER);
		if (host == null || host.length() == 0)
			throw new IllegalArgumentException("No SMTP host name");
		if (null != s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.SMTPPORT))
			props.put("mail.smtp" + ssl + ".port", s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.SMTPPORT));
		String passwd = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.SMTPASS);
		if (passwd != null) {
			props.put("mail.smtp" + ssl + ".auth", "true");
			props.put("mail.smtp" + ssl + ".quitwait", "false");
			passwd = controller.crypto.decrypt(passwd);
		}
		try {
			// Get a Session object
			Session session = Session.getDefaultInstance(props, null);
			session.setDebug(true);

			String charset = MiscellaneousOptionsTab.getEncoding(controller);
			String sb = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.EMAILADDR);
			final String fromId = sb == null ? System.getProperty("user.name") + '@' + InetAddress.getLocalHost() : sb;
			// construct the message
			Message msg = new MimeMessage(session) {
				protected void updateHeaders() throws MessagingException {
					super.updateHeaders();
					setHeader("Message-ID", "<" + (System.currentTimeMillis() / 100l) + '.'
							+ (Math.random() * 100000000) + '.' + PhotoOrganizer.PROGRAMNAME + '.' + fromId + '>');
				}
			};
			String person = DataConv.arrayToString(s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.SMTPUSER), ',');
			if (sb != null)
				if (charset != null && charset.length() > 0)
					msg.setFrom(person != null ? new InternetAddress(sb, person, charset) : new InternetAddress(sb));
				else
					msg.setFrom(person != null ? new InternetAddress(sb, person) : new InternetAddress(sb));
			else
				msg.setFrom();
			sb = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.REPLYADDR);
			if (sb != null && sb.length() > 0)
				msg.setReplyTo(InternetAddress.parse(sb));
			sb = tf_to.getText();
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sb, false));
			MimeBodyPart bp;
			Multipart mp = new MimeMultipart("related");
			Multipart mp2 = new MimeMultipart("alternative");
			bp = new MimeBodyPart();
			bp.setContent(mp2);
			mp.addBodyPart(bp);
			sb = ta.getText();
			if (sb.length() > 0) {
				String subject = new StringTokenizer(sb, "\n\r").nextToken();
				if (subject.length() > MAX_SUBJECT_LENGTH)
					subject = subject.substring(0, MAX_SUBJECT_LENGTH - 1);
				bp = new MimeBodyPart();
				if (charset != null && charset.length() > 0) {
					bp.setContent(sb, "text/plain; charset=" + charset);
					((MimeMessage) msg).setSubject(subject, charset); // we
					// know
					// we
					// are
				} else {
					bp.setContent(sb, "text/plain");
					msg.setSubject(subject);
				}
				mp.addBodyPart(bp);
			} else
				msg.setSubject(Resources.TITLE_DEF_EMAILSUBJECT);

			File[] files = new File[medias_to_send.length];
			for (int i = 0; i < medias_to_send.length; i++)
				if (medias_to_send[i] instanceof File)
					files[i] = (File) medias_to_send[i];
				else if (medias_to_send[i] instanceof MediaFormat)
					files[i] = ((MediaFormat) medias_to_send[i]).getFile();
			new HtmlProducer(controller).produce(new MailCourier(controller, mp, mp2), files, sb);

			msg.setHeader("X-Mailer", PhotoOrganizer.PROGRAMNAME + " (" + PhotoOrganizer.HOME_PAGE + ')');
			msg.setHeader("Organization", DataConv.arrayToString(s.getProperty(WebPublishOptionsTab.SECNAME,
					WebPublishOptionsTab.ORGANIZATION), ','));
			if (charset != null && charset.length() > 0)
				((MimeMessage) msg).setDescription(DataConv.arrayToString(s.getProperty(WebPublishOptionsTab.SECNAME,
						WebPublishOptionsTab.ORGANIZATION), ','), charset);
			else
				msg.setDescription(DataConv.arrayToString(s.getProperty(WebPublishOptionsTab.SECNAME,
						WebPublishOptionsTab.ORGANIZATION), ','));
			msg.setSentDate(new Date());
			msg.setContent(mp);

			// send the thing off
			Transport tr = session.getTransport("smtp" + ssl);
			String accnt = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.EMAILADDR);
			tr.connect(host, accnt, passwd);
			tr.sendMessage(msg, msg.getAllRecipients());
			tr.close();
		} catch (IOException ioex) {
			JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_MAIL_SEND + '\n' + ioex,
					Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			ioex.printStackTrace();
		} catch (MessagingException mex) {
			if (mex.getCause() != null)
				System.err.println(mex.getCause().getMessage());
			JOptionPane.showMessageDialog(controller.mediachest, Resources.LABEL_ERR_MAIL_SEND + '\n' + mex,
					Resources.TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
			mex.printStackTrace();
		}
	}

	class SendEmail extends JPanel {
		SendEmail() {
			setLayout(new FixedGridLayout(5, 7, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			setBorder(new BevelBorder(EtchedBorder.RAISED));
			add(new JLabel(Resources.LABEL_SENDTO), "0,0,2");
			add(tf_to = new JTextField(), "0,1,3");
			JButton b;
			add(b = new JButton(Resources.CMD_ADDRESSBOOK), "3,1,2");
			b.addActionListener(SendEmailFrame.this);
			add(new JScrollPane(ta = new /* LocalizedInputTextArea */JTextArea()), "0,2,5,-1");
			// ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
			ta.setFont(new Font("Arial", Font.PLAIN, 16));
		}
	}

	class ActionButtons extends JPanel {
		ActionButtons() {
			setLayout(new FlowLayout(FlowLayout.RIGHT));
			JButton btn;
			add(btn = new JButton(Resources.CMD_OK));
			btn.addActionListener(SendEmailFrame.this);
			add(btn = new JButton(Resources.CMD_CANCEL));
			btn.addActionListener(SendEmailFrame.this);
		}
	}

	JTextField tf_to;

	JTextArea ta;
}
