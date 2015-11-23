/* PhotoOrganizer - $RCSfile: Id3TagEditor.java,v $                           
 * Copyright (C) 2001-2003 Dmitriy Rogatkin.  All rights reserved.                    
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
 *  $Id: Id3TagEditor.java,v 1.32 2012/10/18 06:59:00 cvs Exp $            
 */

package photoorganizer.renderer;

import static mediautil.gen.MediaInfo.ALBUM;
import static mediautil.gen.MediaInfo.ARTIST;
import static mediautil.gen.MediaInfo.ARTISTWEBPAGE;
import static mediautil.gen.MediaInfo.AUDIOFILEWEBPAGE;
import static mediautil.gen.MediaInfo.AUDIOSOURCEWEBPAGE;
import static mediautil.gen.MediaInfo.BAND;
import static mediautil.gen.MediaInfo.BPM;
import static mediautil.gen.MediaInfo.COMMENTS;
import static mediautil.gen.MediaInfo.COMPILATION;
import static mediautil.gen.MediaInfo.COMPOSER;
import static mediautil.gen.MediaInfo.CONDUCTOR;
import static mediautil.gen.MediaInfo.CONTENTGROUP;
import static mediautil.gen.MediaInfo.FILEOWNER;
import static mediautil.gen.MediaInfo.GENRE;
import static mediautil.gen.MediaInfo.LYRICIST;
import static mediautil.gen.MediaInfo.PARTOFSET;
import static mediautil.gen.MediaInfo.PICTURE;
import static mediautil.gen.MediaInfo.REMIXER;
import static mediautil.gen.MediaInfo.SUBTITLE;
import static mediautil.gen.MediaInfo.TITLE;
import static mediautil.gen.MediaInfo.TRACK;
import static mediautil.gen.MediaInfo.YEAR;

import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import de.vdheide.mp3.MP3File;
import de.vdheide.mp3.TagContent;

public final class Id3TagEditor extends JPanel implements ActionListener {
	static final String[] TAG_FIELDS = { ARTIST, ALBUM, TITLE, COMMENTS, COMPOSER, CONDUCTOR, LYRICIST, CONTENTGROUP,
			FILEOWNER, ARTISTWEBPAGE, AUDIOFILEWEBPAGE, AUDIOSOURCEWEBPAGE, BAND, REMIXER, SUBTITLE, TRACK, YEAR, BPM,
			PARTOFSET };

	static final String[] GROUP_TAG_FIELDS = { ARTIST, ALBUM, COMMENTS, COMPOSER, CONDUCTOR, LYRICIST, CONTENTGROUP,
			FILEOWNER, ARTISTWEBPAGE, AUDIOFILEWEBPAGE, AUDIOSOURCEWEBPAGE, BAND, REMIXER, YEAR, BPM, PARTOFSET

	};

	// genre, compilation, and art work added separately

	static final long MAX_MEMORY_FOR_FILE = 1024 * 1024 * 2;

	public Id3TagEditor(Controller controller, MediaFormat[] formats) {
		this.controller = controller;
		this.formats = formats;
		MediaInfo info = formats[0].getMediaInfo(); // TODO: check for MP3 and
													// rise exception
		setLayout(new FixedGridLayout(5, 8 + (formats.length > 1 ? GROUP_TAG_FIELDS.length : TAG_FIELDS.length),
				Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP + 5, Resources.CTRL_HORIS_INSET,
				Resources.CTRL_HORIZ_GAP));
		int offset = 0;
		String[] tag_field_names = formats.length > 1 ? GROUP_TAG_FIELDS : TAG_FIELDS;
		if (formats.length == 1)
			add(new SLabel(formats[0].getName()), "0,0,0");
		else {
			add(new SLabel(Resources.LABEL_FILE_RULE), "0," + offset + ",0");
			add(tf_namePattern = new JTextField(), "1," + offset + ",3");
			String pattern = (String) controller.getPrefs().getProperty(AppearanceOptionsTab.SECNAME,
					AppearanceOptionsTab.FILE_TAG_MAP);
			if (pattern != null)
				tf_namePattern.setText(pattern);
			add(new PopupCombo(Resources.MASKS[1], Resources.LABEL_INS_R, this, "4," + offset + ",1", tf_namePattern),
					"4," + offset + ",0");
			offset++;
			tf_namePattern.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
					tf_namePattern.setToolTipText(IpodOptionsTab.getHelpFileRule(tf_namePattern.getText()));
				}

				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
		}

		offset++;
		tf_tagFields = new JTextField[tag_field_names.length];
		for (int i = 0; i < tag_field_names.length; i++, offset++) {
			add(new SLabel(tag_field_names[i]), "0," + offset);
			add(tf_tagFields[i] = new JTextField(), "1," + offset + ",4");
			Object value = null;
			try {
				value = info.getAttribute(tag_field_names[i]);
			} catch (IllegalArgumentException iae) {
			}
			if (value != null)
				tf_tagFields[i].setText(value.toString());
		}
		add(new SLabel(GENRE), "0," + offset);
		add(cb_genre = new JComboBox(Controller.getSortedGenres()), "1," + offset + ",2");
		cb_genre.setEditable(true);
		try {
			cb_genre.setSelectedItem(MP3.findGenre(info));
		} catch (Exception e) {
			cb_genre.setSelectedItem(MP3.GENRES[12]);
		}
		offset++;
		add(new SLabel(COMPILATION), "0," + offset);
		add(cb_cmp = new JComboBox(new String[] { Resources.LIST_NOTSET, Resources.LIST_YES, Resources.LIST_NO }), "1,"
				+ offset + ",2");
		try {
			cb_cmp.setSelectedIndex(info.getBoolAttribute(COMPILATION) ? 1 : 2);
		} catch (Exception e) {
			cb_cmp.setSelectedIndex(0);
		}
		offset++;
		// PICTURE
		add(new SLabel(PICTURE), "0," + offset);
		add(tf_picture = new JTextField(), "1," + offset + ",3");
		try {
			tf_picture.setText(info.getAttribute(PICTURE).toString());
		} catch (Exception e) {
			// e.printStackTrace();
			System.err.println(PICTURE + " has not been set, " + e);
		}

		JButton btn;
		add(btn = new JButton(Resources.CMD_BROWSE), "4," + offset + ",0");
		// TODO: add image type
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				JFileChooser chooser = new JFileChooser();
				chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
					public String getDescription() {
						return Resources.LABEL_IMAGE_FILES;
					}

					public boolean accept(File f) {
						String name = f.getName().toLowerCase();
						return f.isDirectory() || name.endsWith(Resources.EXT_JPG) || name.endsWith(Resources.EXT_GIF)
								|| name.endsWith(Resources.EXT_JPEG) || name.endsWith(Resources.EXT_PNG);
					}
				});
				int returnVal = chooser.showOpenDialog(Id3TagEditor.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					tf_picture.setText(chooser.getSelectedFile().getPath());
					cb_id3v2.setSelected(true);
				}
			}
		});
		offset++;
		add(cb_id3v2 = new JCheckBox(Resources.LABEL_ID3V2TAG), "0," + offset + ",2");
		// TODO: add check listener to disable/enable picture field
		cb_id3v2.setBorderPaintedFlat(true);
		if (info instanceof MP3File)
			cb_id3v2.setSelected(((MP3File) info).isID3v2());
		add(cb_use_unicode = new JCheckBox(Resources.LABEL_ID3UNICOD), "2," + offset + ",2");
		cb_use_unicode.setSelected(((MP3File) info).isID3v2());
		offset++;
		add(controller.createButtonPanel(this, controller.BTN_MSK_OK + controller.BTN_MSK_CANCEL
				+ controller.BTN_MSK_HELP, FlowLayout.RIGHT), "1," + offset + ",3,2");
	}

	public void actionPerformed(ActionEvent e) {
		if (tf_namePattern != null)
			controller.getPrefs().setProperty(AppearanceOptionsTab.SECNAME, AppearanceOptionsTab.FILE_TAG_MAP,
					tf_namePattern.getText());
		if (e.getActionCommand().equals(Resources.CMD_OK)) {
			numUpdates = 0;
			controller.setWaitCursor(this, true);
			String[] tag_field_names = formats.length > 1 ? GROUP_TAG_FIELDS : TAG_FIELDS;
			for (int k = 0; k < formats.length; k++) {
				MediaInfo info = formats[k].getMediaInfo();
				if (tf_namePattern != null) {
					String pattern = tf_namePattern.getText();
					String[] fields = IpodOptionsTab.parseFileName(formats[k].getName(), pattern, null, controller);
					setAttribute(info, TITLE, fields[IpodOptionsTab.TITLE]);
					setAttribute(info, ARTIST, fields[IpodOptionsTab.ARTIST]);
					setAttribute(info, ALBUM, fields[IpodOptionsTab.ALBUM]);
					setAttribute(info, COMPOSER, fields[IpodOptionsTab.COMPOSER]);
					setAttribute(info, YEAR, fields[IpodOptionsTab.YEAR]);
					setAttribute(info, TRACK, fields[IpodOptionsTab.TRACK]);
					// setAttribute(info, TITLE, fields[IpodOptionsTab.TITLE]);
				}
				for (int i = 0; i < tag_field_names.length; i++) {
					try {
						String s = tf_tagFields[i].getText();
						if (s.length() > 0)
							if (COMMENTS.equals(tag_field_names[i])) {
								// TODO: make it more smarter for v2
								// adding description and type
								TagContent comment = new TagContent();
								comment.setContent(s);
								comment.setType("Eng");
								comment.setDescription(tag_field_names[i]);
								info.setAttribute(tag_field_names[i], comment);
							} else
								info.setAttribute(tag_field_names[i], s);
					} catch (Exception ex) {
						System.err.println("Attribute " + tag_field_names[i] + " could not be set, because " + ex);
					}
				}
				boolean id3v2 = cb_id3v2.isSelected();
				((MP3File) info).setWriteID3v2(id3v2);
				((MP3File) info).setTagsEncoding(cb_use_unicode.isSelected() ? MP3File.UNICODE_ENCODING : Controller
						.getEncoding());
				// set genre
				if (id3v2) {
					try {
						info.setAttribute(GENRE, cb_genre.getSelectedItem());
					} catch (Exception ex) {
						System.err.println("Attribute " + GENRE + " could not be set, because " + ex);
					}
					String imgName = tf_picture.getText();
					if (imgName.length() > 0) {
						// try as file
						byte[] image = null;
						File file = new File(imgName);
						ByteArrayOutputStream bos = null;
						if (file.exists()) {
							if (file.length() < MAX_MEMORY_FOR_FILE) {
								try {
									bos = new ByteArrayOutputStream((int) file.length());
									Stream.copyFile(file, bos);
									image = bos.toByteArray();
								} catch (IOException ioe) {
									System.err.println("Error in copy from file " + imgName + " " + ioe);
								} finally {
									try {
										bos.close();
									} catch (Exception e2) {
									}
									bos = null;
								}
							}
						} else {
							InputStream is = null;
							try {
								Stream.copyStream(is = new URL(imgName).openStream(),
										bos = new ByteArrayOutputStream(), MAX_MEMORY_FOR_FILE);
								image = bos.toByteArray();
							} catch (Exception e1) {
								System.err.println("Error in copy from URL " + imgName + " " + e);
							} finally {
								try {
									is.close();
								} catch (Exception e2) {
								}
								try {
									bos.close();
								} catch (Exception e2) {
								}
								bos = null;
							}
						}
						if (image != null) {
							TagContent picture = new TagContent();
							String mimeType = "image/jpeg";
							int ep = imgName.lastIndexOf('.');
							if (ep > 0)
								mimeType = "image/" + imgName.substring(ep + 1);
							picture.setContent(image);
							// TODO: consider to store actual location as
							// description
							// then show it in edit field
							picture.setDescription(imgName);
							picture.setSubtype(new byte[] { MP3.IMAGE_TYPE_OTHER });
							picture.setType(mimeType);
							((MP3File) info).setUseCompression(false);
							info.setAttribute(PICTURE, picture);
						}
					}
					if (cb_cmp.getSelectedIndex() > 0)
						info.setAttribute(COMPILATION, cb_cmp.getSelectedIndex() == 1 ? "1" : "0");
				} else
					info.setAttribute(GENRE, "(" + cb_genre.getSelectedIndex() + ')');
				try {
					((MP3File) info).update();
				} catch (Exception ex) {
					// TODO: report to user
					ex.printStackTrace();
				}
				numUpdates++;
			}
			controller.setWaitCursor(this, false);
		} else if (Resources.CMD_HELP.equals(e.getActionCommand())) {
			controller.showHelp(Id3TagEditor.this, MediaInfoInputPanel.METAINFO_EDIT_HELP);
			return;
		}
		((Window) getTopLevelAncestor()).dispose();
	}

	protected void setAttribute(MediaInfo info, String attribute, String value) {
		if (value != null)
			try {
				info.setAttribute(attribute, value);
			} catch (Exception ex) {
				System.err.println("Attribute " + attribute + " could not be set to " + value + ", because " + ex);
			}
	}

	public static int editTag(Controller controller, MediaFormat format) {
		return editTag(controller, new MediaFormat[] { format });
	}

	synchronized public static int editTag(Controller controller, MediaFormat[] formats) {
		if (isAnyMP3(formats) == false)
			return 0;
		Id3TagEditor editor = new Id3TagEditor(controller, formats);
		JDialog dialog = new JDialog(controller.mediachest, Resources.TITLE_ID3EDITOR, true);
		dialog.setContentPane(editor);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new Connector(editor, dialog));
		dialog.pack();
		dialog.setVisible(true);
		return editor.numUpdates;
	}

	public static boolean isAnyMP3(MediaFormat[] formats) {
		if (formats == null)
			return false;
		for (int i = 0; i < formats.length; i++) {
			if (formats[i].getType() == MediaFormat.AUDIO &&  
					MP3.MP3.equals(formats[i].getFormat(MediaFormat.AUDIO)))
				return true;
		}
		return false;
	}

	protected Controller controller;

	protected MediaFormat[] formats;

	protected JTextField[] tf_tagFields;

	protected JTextField tf_picture, tf_namePattern;

	protected JComboBox cb_genre, cb_cmp;

	protected JCheckBox cb_id3v2, cb_use_unicode;

	protected int numUpdates;
}

final class Connector extends WindowAdapter {
	private Window window;

	Connector(Id3TagEditor editor, Window window) {
		this.window = window;

	}

	public void windowClosing(WindowEvent e) {
		window.dispose();
	}
}
