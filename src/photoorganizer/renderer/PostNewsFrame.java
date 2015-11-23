/* MediaChest - $RCSfile: PostNewsFrame.java,v $ 
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * $Id: PostNewsFrame.java,v 1.19 2008/03/02 04:42:54 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.ImageUtil;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.DimensionS;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.nntp.Article;
import photoorganizer.nntp.NntpClient;
import addressbook.AddressBookFrame;
import addressbook.util.ActionPerformer;

// TODO: inherit from SendEmailFrame
public class PostNewsFrame extends JFrame implements ActionListener, ActionPerformer {
	static final int THEME_IMAGE_OCUP = 25; // in %

	Controller controller;

	Object[] mediasToPost;

	public final static DimensionS[] INDEX_SIZES = Resources.LIST_SIZES;

	public PostNewsFrame(Controller controller, Object[] medias) {
		super(Resources.TITLE_POSTTO);
		this.controller = controller;
		setIconImage(controller.getMainIcon());
		mediasToPost = medias;
		getContentPane().add(new PostArticles(), "Center");
		getContentPane()
				.add(
						controller.createButtonPanel(this, Controller.BTN_MSK_OK + Controller.BTN_MSK_CANCEL,
								FlowLayout.RIGHT), "South");
		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (cmd.equals(Resources.CMD_ADDRESSBOOK)) {
			ActionPerformer ap = (ActionPerformer) controller.component(AddressBookFrame.COMP_ACTIONPERFORMER);
			if (ap == null)
				controller.add(this, AddressBookFrame.COMP_ACTIONPERFORMER);
			AddressBookFrame ab = (AddressBookFrame) controller.component(AddressBookFrame.COMP_ADDRESSBOOK);
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
					// register itself
					controller.registerBackgroundWork(this, true);
					try {
						post();
					} finally {
						// unregister
						controller.registerBackgroundWork(this, false);
					}
				}
			}, "News poster").start();
		}
		controller.add((String) null, AddressBookFrame.COMP_ACTIONPERFORMER);
		dispose();
	}

	public void act(int _action, Object _value) {
		if (_action == ActionPerformer.SHOWBOOKMARK) {
			String el = tf_group.getText();
			if (el.length() > 0)
				tf_group.setText(el + ',' + (String) _value);
			else
				tf_group.setText((String) _value);
		}
	}

	// TODO: should be synchronized?
	protected void post() {
		IniPrefs s = controller.getPrefs();
		StatusBar statusBar = ((StatusBar) controller.component(Controller.COMP_STATUSBAR));
		String host = (String) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPSERVER);
		if (host == null)
			return; // silently, TODO: raise an exception
		int port = 119;
		try {
			port = ((Integer) s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPPORT)).intValue();
		} catch (Exception e) {
			System.err.println("Can get port (" + e + ") default one will be used.");
		}
		NntpClient client = null;
		try {
			client = new NntpClient(host, port);
			if (s.getInt(s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPSRVLOGON), 0) == 1)
				client.setCredentials((String) s.getProperty(WebPublishOptionsTab.SECNAME,
						WebPublishOptionsTab.NNTPLOGIN), DataConv.encryptXor(new String(DataConv.hexToBytes((String) s
						.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPPASSWD)),
						Controller.ISO_8859_1)));
			Article a = new Article(MiscellaneousOptionsTab.getEncoding(controller));
			a.setHeader(Article.FROM, "\"" + s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPUSER)
					+ "\" <" + s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPEMAILADDR) + '>');
			a.setHeader(Article.REPLY_TO, "\""
					+ s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPUSER) + "\" <"
					+ s.getProperty(WebPublishOptionsTab.SECNAME, WebPublishOptionsTab.NNTPREPLYADDR) + '>');
			a.setHeader(Article.NEWSGROUPS, tf_group.getText());

			a
					.setHeader(Article.ORGANIZATION, s.getProperty(WebPublishOptionsTab.SECNAME,
							WebPublishOptionsTab.NNTPORG));
			// a.setBody(ta.getText());
			statusBar.displayInfo(Resources.INFO_WEBPUBLISHING);
			if (cb_postTypes.getSelectedIndex() == Resources.FLOOD_POST) {
				// index only is worth for flood post

				Iterator writers = ImageIO.getImageWritersByFormatName(BasicJpeg.JPEG);
				int styleIndex = cb_indexStyle.getSelectedIndex();
				if (styleIndex > Resources.NONE_STYLE_INDEX_INDEX && writers != null && writers.hasNext()) {
					Dimension thumbSize = ThumbnailsOptionsTab.getThumbnailSize(s);
					ImageWriter wr = (ImageWriter) writers.next();
					int d = cb_indexSize.getSelectedIndex();
					// TODO:
					// interesting
					// to try
					// display
					// Dimension
					// with patched
					// to String
					if (d < 0)
						d = 0;
					int hg = 8;
					int vg = 6;
					int fh = 10;
					BufferedImage ii = null;
					Graphics2D g = null;
					Font font = new Font("Courier", Font.PLAIN, 10);
					// setBackground(Color color) ;
					boolean nextIndex = true;
					int cc = 0, cr = 0;
					int in = 0;
					int leftMargin = 0;
					// TODO: generating indexes in post loop can give
					// performance improvement
					for (int i = 0; i < mediasToPost.length; i++) {
						if (nextIndex) {
							nextIndex = false;
							ii = new BufferedImage(INDEX_SIZES[d].width, INDEX_SIZES[d].height,
									BufferedImage.TYPE_INT_RGB);
							g = ii.createGraphics();
							g.setFont(font);
							g.setColor(Color.WHITE);
							fh = g.getFontMetrics().getHeight();
							// do special
							int themeIndex = cb_themeName.getSelectedIndex();
							if (mediasToPost[themeIndex] != null
									&& mediasToPost[themeIndex] instanceof MediaFormat == false)
								try {
									mediasToPost[themeIndex] = MediaFormatFactory
											.createMediaFormat((File) mediasToPost[themeIndex]);
								} catch (ClassCastException cce) {
								}
							if (styleIndex == Resources.LEFT_STYLE_INDEX_INDEX) {
								// 25% TODO: make configurable
								leftMargin = INDEX_SIZES[d].width * THEME_IMAGE_OCUP / 100;
								if (mediasToPost[themeIndex] != null && mediasToPost[themeIndex] instanceof BasicJpeg) {
									BufferedImage oi = ((BasicJpeg) mediasToPost[themeIndex]).getBufferedImage();
									Dimension imageSize = ImageUtil.getScaledSize(oi, new Dimension(leftMargin,
											INDEX_SIZES[d].height), null);
									leftMargin += hg;
									if (g.drawImage(oi, 0, (INDEX_SIZES[d].height - imageSize.height) / 2,
											imageSize.width, imageSize.height, null) == false)
										System.err.println("Not finished drawing");
								}
							} else if (styleIndex == Resources.BACKGROUND_STYLE_INDEX_INDEX) {
								if (mediasToPost[themeIndex] != null && mediasToPost[themeIndex] instanceof BasicJpeg) {
									BufferedImage oi = ((BasicJpeg) mediasToPost[themeIndex]).getBufferedImage();
									Dimension imageSize = ImageUtil.getScaledSize(oi, INDEX_SIZES[d], null);
									// TODO: consider using tiled if big size
									// difference
									if (g.drawImage(oi, (INDEX_SIZES[d].width - imageSize.width) / 2,
											(INDEX_SIZES[d].height - imageSize.height) / 2, imageSize.width,
											imageSize.height, null) == false)
										System.err.println("Not finished drawing");
								}
								hg = 26;
							}
						}
						if (cc * (thumbSize.width + hg) + thumbSize.width + leftMargin > INDEX_SIZES[d].width) {
							cc = 0;
							cr++;
						}
						if (cr * (thumbSize.height + vg + fh) + thumbSize.height + fh > INDEX_SIZES[d].height) {
							cr = 0;
							nextIndex = true;
							if (postIndexImage(client, a, in++, cb_title.getSelectedItem().toString(), ta.getText(),
									wr, ii) == false) {
								postNotAllowed(client.getResponseStrings().toString());
								break;
							}
						}
						// consider using original thumbnail and do not scale
						// Icon icn =
						// ((BasicJpeg)formatsTopost[i]).getThumbnail(null);
						// if (icn != null)
						// use it directly ico.paintIcon(this, g, ...);
						// small code duplication
						if (mediasToPost[i] != null && mediasToPost[i] instanceof MediaFormat == false)
							try {
								mediasToPost[i] = MediaFormatFactory.createMediaFormat((File) mediasToPost[i]);
							} catch (ClassCastException cce) {
							}
						if (mediasToPost[i] != null && mediasToPost[i] instanceof BasicJpeg) {
							BufferedImage oi = ((BasicJpeg) mediasToPost[i]).getBufferedImage();
							Dimension imageSize = ImageUtil.getScaledSize(oi, AbstractImageInfo.DEFAULT_THUMB_SIZE,
									null);
							if (g.drawImage(oi, leftMargin + cc * (thumbSize.width + hg)
									+ (thumbSize.width - imageSize.width) / 2, cr * (thumbSize.height + vg + fh)
									+ (thumbSize.height - imageSize.height) / 2, imageSize.width, imageSize.height,
									null) == false)
								System.err.println("Not finished drawing");
						}
						String name = Resources.N_A;
						if (mediasToPost[i] != null)
							if (mediasToPost[i] instanceof MediaFormat)
								name = ((MediaFormat) mediasToPost[i]).getName();
							else if (mediasToPost[i] instanceof File)
								name = ((File) mediasToPost[i]).getName();
						g.drawString(name, leftMargin + cc * (thumbSize.width + hg), cr * (thumbSize.height + vg + fh)
								+ thumbSize.height + fh);

						cc++;
					}
					if (nextIndex == false) {
						postIndexImage(client, a, in, cb_title.getSelectedItem().toString(), ta.getText(), wr, ii);
					}
				}
				statusBar.setProgress(mediasToPost.length);
				for (int i = 0; i < mediasToPost.length; i++) {
					// duplication again, but can be OK, instead of doing that
					// one time at start
					if (mediasToPost[i] != null && mediasToPost[i] instanceof MediaFormat == false)
						try {
							mediasToPost[i] = MediaFormatFactory.createMediaFormat((File) mediasToPost[i]);
						} catch (ClassCastException cce) {
						}
					a.setHeader(Article.SUBJECT, new FileNameFormat(cb_title.getSelectedItem().toString())
							.format(mediasToPost[i]));
					a.setBody(new FileNameFormat(ta.getText()).format(mediasToPost[i]));
					a.setStandardHeaders();
					a.resetParts();
					PrintStream ps = client.startPost();
					if (ps != null && mediasToPost[i] != null && mediasToPost[i] instanceof MediaFormat) {
						InputStream is = ((MediaFormat) mediasToPost[i]).getAsStream();
						try {
							a.addPart(((MediaFormat) mediasToPost[i]).getName() + '.'
									+ ((MediaFormat) mediasToPost[i]).getType(), is);
							a.getArticle(ps);
						} finally {
							is.close();
						}
						if (client.finishPost() == false) {
							statusBar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING);
							System.err.println("Post finished with errors " + client.getResponseStrings());
						}
						statusBar.tickProgress();
					} else {
						postNotAllowed(client.getResponseStrings().toString());
						break;
					}
				}
			} else {
				a.setHeader(Article.SUBJECT, cb_title.getSelectedItem().toString());
				a.setStandardHeaders();
				a.setBody(ta.getText());
				PrintStream ps = client.startPost();
				if (ps != null) {
					statusBar.setProgress(mediasToPost.length);
					for (int i = 0; i < mediasToPost.length; i++)
						if (mediasToPost[i] instanceof File)
							a.addPart((File) mediasToPost[i]);
						else if (mediasToPost[i] instanceof MediaFormat)
							a.addPart(((MediaFormat) mediasToPost[i]).getFile());
					a.getArticle(ps);
					if (client.finishPost() == false) {
						statusBar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING);
						System.err.println("Post finished with errors " + client.getResponseStrings());
					}
					statusBar.tickProgress();
				} else
					postNotAllowed(client.getResponseStrings().toString());
			}
			statusBar.clearInfo();
			statusBar.clearProgress();
		} catch (Exception e) {
			// error in status bar
			statusBar.flashInfo(Resources.INFO_ERR_WEBPUBLISHING + " (" + e);
			e.printStackTrace();
		} finally {
			if (client != null)
				client.close();
		}
	}

	protected boolean postIndexImage(NntpClient nntpClient, Article a, int indexNumber, String title, String body,
			ImageWriter imageWriter, BufferedImage bImage) throws IOException {
		a.setHeader(Article.SUBJECT, Resources.LABEL_INDEX + ' ' + (indexNumber++) + ' '
				+ new FileNameFormat(title).format(null)); // to remove meta
															// tags
		a.setStandardHeaders();
		a.setBody(new FileNameFormat(body).format(null));
		a.resetParts();
		PrintStream ps = nntpClient.startPost();
		if (ps != null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 256);
			try {
				imageWriter.setOutput(new MemoryCacheImageOutputStream(bos));
				imageWriter.write(bImage);
				a.addPart(Resources.LABEL_INDEX + indexNumber + '.' + BasicJpeg.JPEG, bos.toByteArray());
				a.getArticle(ps);
			} finally {
				bos.close();
			}
			if (nntpClient.finishPost() == false) {
				System.err.println("Post finished with errors " + nntpClient.getResponseStrings());
				return false;
			}
		} else
			return false;
		return true;
	}

	protected void postNotAllowed(String details) {
		JOptionPane.showMessageDialog(this, "Post not allowed, the sequence will be terminated\n" + details);
	}

	class PostArticles extends JPanel {
		PostArticles() {
			setLayout(new FixedGridLayout(6, 8, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			setBorder(new BevelBorder(EtchedBorder.RAISED));
			add(new JLabel(Resources.LABEL_POSTTO), "0,0,2");
			add(tf_group = new JTextField(), "0,1,4");
			JButton b;
			add(b = new JButton(Resources.CMD_ADDRESSBOOK), "4,1,2");
			// combobox post type all in one
			add(new JLabel(Resources.LABEL_POST_TYPE), "0,2");
			add(cb_postTypes = new JComboBox(Resources.POST_TYPES), "1,2,2");
			cb_postTypes.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					updateUiState();
				}
			});
			// combobox add index and index style
			add(new JLabel(Resources.LABEL_POST_INDEX), "3,2");
			add(cb_indexStyle = new JComboBox(Resources.INDEX_STYLES), "4,2,2");
			cb_indexStyle.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					updateUiIndexState();
				}
			});
			// combobox index theme
			add(new JLabel(Resources.LABEL_THEME_NAME), "0,3");
			add(cb_themeName = new JComboBox(mediasToPost), "1,3,2");
			// combobox index size
			add(new JLabel(Resources.LABEL_INDEX_SIZE), "3,3");
			add(cb_indexSize = new JComboBox(INDEX_SIZES), "4,3,2");
			// checkbox add signature
			add(new JCheckBox(Resources.LABEL_ADD_SIGN), "0,4");
			// edit box post title
			add(new JLabel(Resources.LABEL_POST_TITLE), "1,4");
			add(cb_title = new JComboBox(RenameOptionsTab.getRenameMask(controller.getPrefs())), "2,4,3");
			cb_title.setEditable(true);
			b.addActionListener(PostNewsFrame.this);
			add(new JScrollPane(ta = new JTextArea()), "0,5,6,-1");
			// ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
			updateUiState();
		}

		protected void updateUiState() {
			boolean e = cb_postTypes.getSelectedIndex() == Resources.FLOOD_POST;
			cb_indexStyle.setEnabled(e);
			updateUiIndexState();
		}

		protected void updateUiIndexState() {
			boolean e = cb_indexStyle.getSelectedIndex() > Resources.NONE_STYLE_INDEX_INDEX;
			cb_themeName.setEnabled(e);
			cb_indexSize.setEnabled(e);
		}
	}

	JTextField tf_group;

	JTextArea ta;

	JComboBox cb_title, cb_postTypes, cb_indexSize, cb_indexStyle, cb_themeName;
}
