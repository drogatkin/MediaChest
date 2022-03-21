/* MediaChest - MediaInfoInputPanel 
 * Copyright (C) 1999-20078 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: MediaInfoInputPanel.java,v 1.65 2008/02/17 04:10:22 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Descriptor;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.ipod.ArtworkDB;
import photoorganizer.ipod.BaseItem;
import photoorganizer.ipod.ITunesDB;
import photoorganizer.ipod.PlayItem;
import photoorganizer.ipod.PlayList;

/**
 * this panel used for select value for some info field and info field itself
 */
public class MediaInfoInputPanel extends JPanel {
	// TODO: Move in resource bundle
	public static final String METAINFO_EDIT_HELP = "metainfo_edit";

	protected Controller controller;

	protected int current;

	protected PlayList playList;

	protected int[] selections;
	
	protected int firstTrack;

	protected Descriptor returnValue;

	protected static int[] PLAYLIST_MAP;	

	public MediaInfoInputPanel(Controller controller) {
		this(controller, null, null);
	}

	public MediaInfoInputPanel(Controller controller, PlayList playList, int[] selections) {
		this.controller = controller;
		this.playList = playList;
		this.selections = selections;
		setLayout(new BorderLayout());
		int buttonsMask = Controller.BTN_MSK_OK;
		if (selections != null)
			if (selections.length == 1) {
				buttonsMask = Controller.BTN_MSK_NEXT + Controller.BTN_MSK_FINISH;
				current = selections[0];
			} else { // multi update
				current = selections[0];
			}
		display(true);
		add(Controller.createButtonPanel(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				String cmd = a.getActionCommand();
				if (Resources.CMD_OK.equals(cmd)) {
					if (cb_sel != null) {
						int sel = cb_sel.getSelectedIndex();
						if (sel > 0) {
							MediaInfoInputPanel.this.returnValue = new Descriptor(
									((JTextField) tf_values[0]).getText(), PLAYLIST_MAP[sel]);
						}
					} else {
						assert MediaInfoInputPanel.this.selections != null;
						if (cb_update_id3.isSelected()) {
							new SwingWorker() {

								@Override
								protected Object doInBackground() throws Exception {
									// TODO song info can be updated instantly, however ID3 tag update can be 
									// queued
									for (int i = 0; i < MediaInfoInputPanel.this.selections.length; i++) {
										update(MediaInfoInputPanel.this.selections[i]);
									}
									return null;
								}
								
							}.execute();
						} else {
							for (int i = 0; i < MediaInfoInputPanel.this.selections.length; i++) {
								update(MediaInfoInputPanel.this.selections[i]);
							}
						}
						MediaInfoInputPanel.this.returnValue = new Descriptor(null, 0);
					}
				} else if (Resources.CMD_NEXT.equals(cmd)) {
					update(current++);
					if (current > MediaInfoInputPanel.this.playList.size() - 1)
						current = 0;
					display(false); // only update
					return;
				} else if (Resources.CMD_FINISH.equals(cmd)) {
					update(current++);
				} else if (Resources.CMD_CANCEL.equals(cmd)) {
					MediaInfoInputPanel.this.returnValue = null;
				} else if (Resources.CMD_HELP.equals(cmd)) {
					MediaInfoInputPanel.this.controller.showHelp(MediaInfoInputPanel.this, METAINFO_EDIT_HELP);
					return;
				}
				((Window) getTopLevelAncestor()).dispose();
			}
		}, Controller.BTN_MSK_CANCEL + Controller.BTN_MSK_HELP + buttonsMask, FlowLayout.RIGHT), BorderLayout.SOUTH);
	}

	protected void update(int current) {
		PlayItem pi = current < 0 || playList == null ? null : (PlayItem) playList.get(current);
		if (pi == null)
			return;
		int so = FIELD_DESCRIPTORS.length > tf_values.length ? 1 : 0;
		for (int i = 0; i < tf_values.length; i++) {
			switch (FIELD_DESCRIPTORS[i + so].selector) {
			case PlayItem.GENRE:
				String newValue = (String) getSelectedValue(tf_values[i]);
				if (newValue.length() > 0 && isChangeRequested(tf_values[i]))
					pi.set(FIELD_DESCRIPTORS[i + so].selector, newValue);
				break;
			case PlayItem.RATING:
				if (isChangeRequested(tf_values[i]) /*&&getSelectedIndex(tf_values[i]) > 0*/)
					pi.set(FIELD_DESCRIPTORS[i + so].selector, getSelectedIndex(tf_values[i]));
				break;
			case PlayItem.COMPILATION:
				int si = ((JComboBox) tf_values[i]).getSelectedIndex();
				if (si == 1)
					pi.set(PlayItem.COMPILATION, true);
				else if (si == 2)
					pi.set(PlayItem.COMPILATION, false);
				break;
			case PlayItem.EQ_SETTING:
				if (isChangeRequested(tf_values[i]))
					pi.set(PlayItem.EQ_SETTING, (String) ((Descriptor) getSelectedValue(tf_values[i])).value);
				break;
			case PlayItem.VOLUME:
				if (isChangeRequested(tf_values[i]))
					pi.set(PlayItem.VOLUME, getValue(tf_values[i]));
				break;
			case PlayItem.LENGTH:
				try {
					pi.set(PlayItem.LENGTH, (int) MP3.parseTime(getText(tf_values[i])));
				} catch (ParseException pe) {
					System.err.println("Can't set length " + pe);
				}
				break;
			case PlayItem.START:
				try {
					pi.set(PlayItem.START, (int) MP3.parseTime(((JTextField) tf_values[i]).getText()));
				} catch (ParseException pe) {
				}
				break;
			case PlayItem.STOP:
				try {
					int endTime = (int) MP3.parseTime(((JTextField) tf_values[i]).getText());
					Object len = pi.get(PlayItem.LENGTH);
					if (endTime > 0 && len != null && len instanceof Integer && ((Integer) len).intValue() != endTime)
						pi.set(PlayItem.STOP, (int) MP3.parseTime(((JTextField) tf_values[i]).getText()));
				} catch (ParseException pe) {
				}
				break;
			case PlayItem.ARTWORK:				
				if (cb_artwork_id3.isSelected()) {
					MediaFormat mf = pi.getAttachedFormat();
					if (mf == null)
						mf = MediaFormatFactory.createMediaFormat(pi.getFile(IpodOptionsTab.getDevice(controller)));
					if (mf != null && mf.getMediaInfo().getAttribute(MediaInfo.PICTURE) != null) {
						pi.set(PlayItem.ARTWORK, (String)null);
						pi.setImage(new ArtworkDB.ImageItem());
					}
				} else {
					newValue = ((JTextField) tf_values[i]).getText();
					if (newValue.trim().length() > 0) {
						pi.set(PlayItem.ARTWORK, newValue);
						ArtworkDB.ImageItem ii = pi.getImage();
						if (ii == null)
							pi.setImage(ii = new ArtworkDB.ImageItem());
						else
							ii.resetState(BaseItem.STATE_COPIED + BaseItem.STATE_METASYNCED); 
					}
				}
				break;
			default:
				newValue = getText(tf_values[i]);
				if (isChangeRequested(tf_values[i]))
					pi.set(FIELD_DESCRIPTORS[i + so].selector, newValue);
			}
		}
		for (int i = 0; i < tf_sortvalues.length; i++) {
			if (isChangeRequested(tf_sortvalues[i]))
				pi.set(SORT_FIELD_DESCRIPTORS[i].selector, getText(tf_sortvalues[i]));
		}
		for (int i = 0; i < tf_optvalues.length; i++) {
			if (isChangeRequested(tf_optvalues[i]))
				pi.set(OPT_FIELD_DESCRIPTORS[i].selector, getCheck(tf_optvalues[i]));
		}
		// update video
		if (tf_videovalues != null && pi.get(PlayItem.VIDEO_KIND) != null) {
			if (isChangeRequested(tf_videovalues[0])) {
				pi.set(PlayItem.VIDEO_KIND, (String) getSelectedValue(tf_videovalues[0]));
			}
			if (isChangeRequested(tf_videovalues[1])) {
				pi.set(PlayItem.SHOW, getText(tf_videovalues[1]));
			}
			if (isChangeRequested(tf_videovalues[2])) {
				try {
					pi.set(PlayItem.SEASON_NUM, Integer.parseInt(getText(tf_videovalues[2])));
				} catch (Exception ex) {
				}
			}
			if (isChangeRequested(tf_videovalues[3])) {
				pi.set(PlayItem.EPIZODE, getText(tf_videovalues[3]));
			}
			if (isChangeRequested(tf_videovalues[4])) {
				try {
					pi.set(PlayItem.EPIZODE_NUM, Integer.parseInt(getText(tf_videovalues[4])));
				} catch (Exception ex) {
				}
			}
		}
		pi.resetState(BaseItem.STATE_METASYNCED);
		if (cb_update_id3.isSelected())
			pi.syncTag(IpodOptionsTab.getDevice(controller));
		String fileName = tf_filename==null?"":tf_filename.getText();
		if (fileName.length() >0) {
			// TODO if sync is missed then this info will be lost and file disappear, as a cure, add this in 
			// a pending for rename list and do it at sync
			File curFile = pi.getFile(IpodOptionsTab.getDevice(controller));
			if (curFile.getName().equals(fileName) == false) {
				// rename
				String ext = pi.getAttachedFormat() != null ? pi.getAttachedFormat().getFormat(0) : null;
				if (ext == null)
					ext = MP3.TYPE;
				fileName = ITunesDB.makeValidFileName(fileName, 0, ext);
				if (curFile.renameTo( new File(curFile.getParent(), fileName)) || curFile.exists() == false ){
					if (pi.isState(PlayItem.STATE_COPIED)) {
						String oldName = (String)pi.get(PlayItem.FILENAME);
						System.err.printf("FIle name from pi %s%n", oldName);
						int p = oldName.lastIndexOf(':');
						if (p >= 0) 
							pi.set(PlayItem.FILENAME, oldName.substring(0, p+1)+fileName);
						else
							pi.set(PlayItem.FILENAME, fileName);
					} else
						pi.set(PlayItem.FILENAME, new File(curFile.getParent(), fileName).getPath());
				} else
					System.err.printf("Couldn't rename %s to %s%n", curFile, new File(curFile.getParent(), fileName));
			}
		}
	}

	protected int getNumFields() {
		if (selections != null) {
			if (selections.length > 1)
				return FIELD_DESCRIPTORS.length - 5;
			else if (selections.length == 1)
				return FIELD_DESCRIPTORS.length;
		}
		return 1;
	}

	protected int getNumSortFields() {
		return SORT_FIELD_DESCRIPTORS.length;
	}

	protected int getNumOptFields() {
		return OPT_FIELD_DESCRIPTORS.length;
	}

	protected class OptPanel extends JPanel {
		OptPanel() {
			setLayout(new FixedGridLayout(4, 1 + getNumOptFields(), Resources.CTRL_VERT_SIZE,
					Resources.CTRL_VERT_GAP + 6, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			tf_optvalues = new JComponent[getNumOptFields()];
			int line = 0;
			int so = selections == null || selections.length == 1 ? 0 : 1;
			for (; line < getNumOptFields(); line++) {
				add(tf_optvalues[line] = so == 0 ? new JCheckBox(OPT_FIELD_DESCRIPTORS[line].name)
						: new CheckedInfoElement(new JCheckBox(OPT_FIELD_DESCRIPTORS[line].name)), "0," + line + ",3");
			}
			
			if (so == 0) {
				line++;
				add(tf_filename = new JTextField(), "0," + line + ",3");
			}
		}
	}

	protected class SortPanel extends JPanel {
		SortPanel() {
			setLayout(new FixedGridLayout(4, 1 + getNumSortFields(), Resources.CTRL_VERT_SIZE,
					Resources.CTRL_VERT_GAP + 6, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			tf_sortvalues = new JComponent[getNumSortFields()];
			int line = 0;
			int so = selections == null || selections.length == 1 ? 0 : 1;
			for (; line < getNumSortFields(); line++) {
				add(new SLabel(SORT_FIELD_DESCRIPTORS[line].name), "0," + line + ",0");
				add(tf_sortvalues[line] = so == 0 ? new JTextField() : new CheckedInfoElement(new JTextField()), "1,"
						+ line + ",3");
			}
		}
	}

	protected class VideoPanel extends JPanel {
		VideoPanel() {
			setLayout(new FixedGridLayout(4, 6, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP + 4,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			tf_videovalues = new JComponent[5/*getNumVideoOptions()*/];
			add(new SLabel(Resources.LABEL_VIDEO_KIND), "0,1,0");
			int so = selections == null || selections.length == 1 ? 0 : 1;
			int vo = 0;
			add(tf_videovalues[vo++] = so == 0 ? new JComboBox(new String[] { Resources.HDR_MOVIES,
					Resources.HDR_MUSICVIDEO, Resources.HDR_TVSHOW }) : new CheckedInfoElement(new JComboBox(
					new String[] { Resources.HDR_MOVIES, Resources.HDR_MUSICVIDEO, Resources.HDR_TVSHOW })), "1,1,0");
			add(new SLabel(Resources.LABEL_SHOW), "0,2,0");
			add(tf_videovalues[vo++] = so == 0 ? new JTextField() : new CheckedInfoElement(new JTextField()), "1,2,2");
			add(new SLabel(Resources.LABEL_SEASON_N), "0,3,0");
			JTextField tf = new JTextField();
			tf.setHorizontalAlignment(JTextField.RIGHT);
			add(tf_videovalues[vo++] = so == 0 ? tf : new CheckedInfoElement(tf), "1,3,1");
			add(new SLabel(Resources.LABEL_EPIZODE_ID), "0,4,0");
			add(tf_videovalues[vo++] = so == 0 ? new JTextField() : new CheckedInfoElement(new JTextField()), "1,4,2");
			add(new SLabel(Resources.LABEL_EPIZODE_N), "0,5,0");
			tf = new JTextField();
			tf.setHorizontalAlignment(JTextField.RIGHT);
			add(tf_videovalues[vo++] = so == 0 ? tf : new CheckedInfoElement(tf), "1,5,1");
		}
	}

	protected void display(boolean create) {
		int so = selections.length == 1 ? 0 : 1;
		boolean video = true;
		for (int i : selections) {
			if (playList.get(i).get(PlayItem.VIDEO_KIND) == null) {
				video = false;
				break;
			}
		}
		if (create) {
			JTabbedPane tp = new JTabbedPane();
			add(tp, BorderLayout.CENTER);
			JPanel infop = new JPanel();
			infop.setLayout(new FixedGridLayout(4, 3 + getNumFields(), Resources.CTRL_VERT_SIZE,
					Resources.CTRL_VERT_GAP + 6, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			// TODO better way to get all layout stuff in JPanel extension itself
			tp.addTab(Resources.LABEL_INFO, infop);
			tp.addTab(Resources.LABEL_SORT, new SortPanel());
			if (video)
				tp.addTab(Resources.LABEL_VIDEO, new VideoPanel());
			tp.addTab(Resources.LABEL_OPT, new OptPanel());
			int line = 0;
			tf_values = new JComponent[getNumFields()];
			if (selections == null || selections.length == 0) {
				infop.add(tf_values[0] = new JTextField(), "0," + line + ",3");
				line++;
				infop.add(cb_sel = new JComboBox(IpodOptionsTab.DIR_INFO_MAP), "0," + line + ",3");
				line++;
			} else {
				for (int i = 0; i < tf_values.length; i++) {
					infop.add(new SLabel(FIELD_DESCRIPTORS[i + so].name), "0," + line + ",0");
					switch (FIELD_DESCRIPTORS[i + so].selector) {
					case PlayItem.GENRE:
						JComboBox cgen = new JComboBox(Controller.getSortedGenres());
						cgen.setEditable(true);
						infop.add(tf_values[i] = so == 0 ? cgen : new CheckedInfoElement(cgen), "1," + line + ",3");
						break;
					case PlayItem.RATING:
						infop.add(tf_values[i] = so == 0 ? new JComboBox(PlayItem.getRatingArray())
								: new CheckedInfoElement(new JComboBox(PlayItem.getRatingArray())), "1," + line + ",0");
						break;
					case PlayItem.COMPILATION:
						infop.add(tf_values[i] = new JComboBox(new String[] { Resources.LIST_NOTSET,
								Resources.LIST_YES, Resources.LIST_NO }), "1," + line + ",0");
						break;
					case PlayItem.DISK:
						infop.add(tf_values[i] = so == 0 ? createTF(JTextField.RIGHT) : new CheckedInfoElement(createTF(JTextField.RIGHT)),
								"1," + line + ",1");
						i++;
						infop.add(new SLabel(Resources.LABEL_SETOF), "2," + line + ",1");
						assert FIELD_DESCRIPTORS[i + so].selector == PlayItem.NUM_DISKS;
						infop.add(tf_values[i] = so == 0 ? createTF(JTextField.RIGHT) : new CheckedInfoElement(createTF(JTextField.RIGHT)),
								"3," + line + ",1");
						break;
					case PlayItem.START:
						infop.add(tf_values[i] = new JTextField(), "1," + line + ",1");
						i++;
						infop.add(new SLabel(Resources.LABEL_STOPTIME), "2," + line + ",1");
						// add(new SLabel(descriptors[i].name), "0,"+line+",0");
						assert FIELD_DESCRIPTORS[i + so].selector == PlayItem.STOP;
						infop.add(tf_values[i] = new JTextField(), "3," + line + ",1");
						break;
					case PlayItem.PLAYED_TIMES:
						infop.add(tf_values[i] = so == 0 ? createTF(JTextField.RIGHT) : new CheckedInfoElement(createTF(JTextField.RIGHT)),
								"1," + line + ",1");
						i++;
						infop.add(new SLabel(FIELD_DESCRIPTORS[i + so].name), "2," + line + ",1");
						infop.add(tf_values[i] = so == 0 ? createTF(JTextField.RIGHT) : new CheckedInfoElement(createTF(JTextField.RIGHT)),
								"3," + line + ",1");
						break;
					case PlayItem.EQ_SETTING:
						infop.add(tf_values[i] = so == 0 ? new JComboBox(PlayItem.EQUALISATIONS)
								: new CheckedInfoElement(new JComboBox(PlayItem.EQUALISATIONS)), "1," + line + ",0");
						break;
					case PlayItem.VOLUME:
						JSlider vs = new JSlider(-255, 255, 0);
						infop.add(tf_values[i] = so == 0 ? vs : new CheckedInfoElement(vs), "1," + line + ",0,2");
						Hashtable labelTable = new Hashtable();
						labelTable.put(new Integer(-255), new SLabel("-100%"));
						labelTable.put(new Integer(0), new SLabel("None"));
						labelTable.put(new Integer(255), new SLabel("100%"));
						vs.setLabelTable(labelTable);
						vs.setPaintLabels(true);
						line++;
						break;
					case PlayItem.ORDER:
						infop.add(tf_values[i] = createTF(JTextField.RIGHT), "1," + line + ",1");
						infop.add(cb_autoincr = new JCheckBox(Resources.LABEL_AUTOINCR_TRACK), "2,"+ line + ",2");
						final int pos = i;
						cb_autoincr.addChangeListener(new ChangeListener() {

							public void stateChanged(ChangeEvent arg0) {
								if (((JCheckBox)arg0.getSource()).isSelected()) {
									String ft = getText(tf_values[pos]);
									try {
										firstTrack = Integer.parseInt(ft);
									} catch (NumberFormatException e) {
										firstTrack = 0;
									}
									if (firstTrack == 0)
										firstTrack = current+1;
									setText(tf_values[pos], ""+firstTrack);
								}
							}});
						break;
					case PlayItem.ARTWORK:
						infop.add(tf_values[i] = new JTextField(), "1," + line + ",2");
						JButton b;
						infop.add(b = new JButton(Resources.CMD_BROWSE), "3," + line + ",0");
						final JTextField text = (JTextField) tf_values[i];
						b.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent a) {
								JFileChooser chooser = new JFileChooser();
								chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
									public String getDescription() {
										return Resources.LABEL_IMAGE_FILES;
									}

									public boolean accept(File f) {
										String name = f.getName().toLowerCase();
										return f.isDirectory() || name.endsWith(Resources.EXT_JPG)
												|| name.endsWith(Resources.EXT_GIF)
												|| name.endsWith(Resources.EXT_JPEG)
												|| name.endsWith(Resources.EXT_PNG);
									}
								});
								int returnVal = chooser.showOpenDialog(MediaInfoInputPanel.this);
								if (returnVal == JFileChooser.APPROVE_OPTION) {
									text.setText(chooser.getSelectedFile().getPath());
								}
							}
						});
						break;
					default:
						infop.add(tf_values[i] = so == 0 ? new JTextField() : new CheckedInfoElement(new JTextField()),
								"1," + line + ",3");
						// add(new JCheckBox("All"), "3,"+line+",0");
					}
					line++;
				}
				// TODO: autoincrement track, and empty blank fields
				// line++;
				//
				infop.add(l_added_mod = new SLabel(""), "0," + line + ",0");
				line++;
				infop.add(l_play_cnt = new SLabel(""), "0," + line + ",0");
				line++;
				infop.add(cb_update_id3 = new JCheckBox(Resources.LABEL_UPDATEID3), "0," + line + ",0");
				cb_update_id3.setSelected(IniPrefs.getInt(controller.getPrefs().getProperty(IpodOptionsTab.SECNAME,
						IpodOptionsTab.SYNC_ID3), 0) == 1);
				infop.add(b_artwork = new JButton(Resources.LABEL_ARTWORK), "2,," + line + ",0,0");
				infop.add(cb_artwork_id3 = new JCheckBox(Resources.LABEL_ARTWORK_FROM_ID3), "3,," + line + ",0,0");
				b_artwork.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						PlayItem pi = current < 0 || playList == null ? null : (PlayItem) playList.get(current);
						if (pi != null) {
							ImageIcon ico = ArtworkDB.getImage(true, pi, IpodOptionsTab.getDevice(controller));
							if (ico != null) {
								final Component c = (Component) e.getSource();
								c.setEnabled(false);
								final JWindow w = new JWindow(SwingUtilities
										.getWindowAncestor(MediaInfoInputPanel.this));
								w.getContentPane().add(new JLabel(ico));
								w.addMouseListener(new MouseAdapter() {
									public void mouseClicked(MouseEvent e) {
										w.dispose();
										c.setEnabled(true);
									}
								});
								w.pack();
								w.setSize(pi.THUMBNAIL_SIZE);
								Point p = ((Component) e.getSource()).getLocation();
								p.y -= pi.THUMBNAIL_SIZE.height;
								SwingUtilities.convertPointToScreen(p, MediaInfoInputPanel.this);
								w.setLocation(p);
								w.setVisible(true);
							}
						}
					}
				});
				line++;
			}
		}
		// fill values
		if (selections != null && selections.length > 0) {
			PlayItem pi = current < 0 || playList == null ? null : (PlayItem) playList.get(current);
			if (pi != null && selections.length == 1) {
				l_added_mod.setText(MessageFormat.format(Resources.MSG_ADDED_CREATED_DATE, new Object[] {
						pi.get(PlayItem.CREATE_TIME), pi.get(PlayItem.MODIFIED_TIME) }));
				if (pi.get(PlayItem.LAST_TIME) != null)
					l_play_cnt.setText(MessageFormat.format(Resources.MSG_LAST_PLAYED_COUNT, new Object[] {
							pi.get(PlayItem.LAST_TIME), pi.get(pi.PLAYED_TIMES) }));
				else
					l_play_cnt.setText("");
			}
			b_artwork.setEnabled(pi != null && pi.getImage() != null);
			for (int i = 0; i < tf_values.length; i++) {
				Object value = pi == null ? null : pi.get(FIELD_DESCRIPTORS[i + so].selector);
				switch (FIELD_DESCRIPTORS[i + so].selector) {
				case PlayItem.GENRE:
					setSelectedValue(tf_values[i], value, MP3.GENRE_OTHER);
					break;
				case PlayItem.RATING:
					setSelectedValue(tf_values[i], null, value == null ? 0 : (Integer) value);
					break;
				case PlayItem.COMPILATION:
					if (value != null && value instanceof Boolean && so < 1)
						((JComboBox) tf_values[i]).setSelectedIndex(((Boolean) value).booleanValue() ? 1 : 2);
					else
						((JComboBox) tf_values[i]).setSelectedIndex(0);
					break;
				case PlayItem.DISK:
					setText(tf_values[i], value == null ? "" : value.toString());
					i++;
					assert FIELD_DESCRIPTORS[i + so].selector == PlayItem.NUM_DISKS;
					value = pi.get(FIELD_DESCRIPTORS[i + so].selector);
					setText(tf_values[i], value == null ? "" : value.toString());
					break;
				case PlayItem.START:
					if (value != null && value instanceof Integer)
						((JTextField) tf_values[i]).setText(MP3.formatTime(((Integer) value).intValue()));
					else
						((JTextField) tf_values[i]).setText("");
					i++;
					// add(new SLabel(descriptors[i].name), "0,"+line+",0");
					assert FIELD_DESCRIPTORS[i + so].selector == PlayItem.STOP;
					value = pi.get(FIELD_DESCRIPTORS[i + so].selector);
					if (value != null && value instanceof Integer) {
						if (((Integer) value).intValue() <= 0)
							value = pi.get(PlayItem.LENGTH);
						((JTextField) tf_values[i]).setText(MP3.formatTime(((Integer) value).intValue()));
					} else
						((JTextField) tf_values[i]).setText("");
					break;
				case PlayItem.EQ_SETTING:
					try {
						setSelectedValue(tf_values[i], value == null ? null : PlayItem.EQUALISATIONS[Integer
								.parseInt(((String) value).substring(3, 6)) - 99], 0);
					} catch (Exception e) {
						setSelectedValue(tf_values[i], null, 0);
					}
					break;
				case PlayItem.VOLUME:
					setValue(tf_values[i], value, 0);
					break;
				case PlayItem.ORDER:
					if (cb_autoincr.isSelected()) {
						setText(tf_values[i], ""+(firstTrack+current));
						break;
					} // let it go through
				default:
					if (value != null)
						if (PlayItem.LENGTH == FIELD_DESCRIPTORS[i + so].selector) {
							if (value instanceof Integer)
								setText(tf_values[i], MP3.formatTime(((Integer) value).intValue()));
							else
								setText(tf_values[i], "");
							tf_values[i].setEnabled(false);
						} else
							setText(tf_values[i], value.toString());

					else
						setText(tf_values[i], "");
				}
			}
			for (int i = 0; i < tf_sortvalues.length; i++) {
				Object value = pi == null ? null : pi.get(SORT_FIELD_DESCRIPTORS[i].selector);
				setText(tf_sortvalues[i], value == null ? "" : value.toString());
			}
			for (int i = 0; i < tf_optvalues.length; i++) {
				Object value = pi == null ? null : pi.get(OPT_FIELD_DESCRIPTORS[i].selector);
				setCheck(tf_optvalues[i], value == null ? false : (Boolean) value);
			}
			if (tf_filename != null && pi != null)
				tf_filename.setText(pi.getFile(IpodOptionsTab.getDevice(controller)).getName());

			// fill video

			Object value = pi == null ? null : pi.get(PlayItem.VIDEO_KIND);
			if (value != null) {
				setSelectedValue(tf_videovalues[0], value, -1);
				value = pi == null ? null : pi.get(PlayItem.SHOW);
				setText(tf_videovalues[1], (String) (value == null ? "" : value));
				value = pi == null ? null : pi.get(PlayItem.SEASON_NUM);
				setText(tf_videovalues[2], (String) (value == null ? "" : value.toString()));
				value = pi == null ? null : pi.get(PlayItem.EPIZODE);
				setText(tf_videovalues[3], (String) (value == null ? "" : value));
				value = pi == null ? null : pi.get(PlayItem.EPIZODE_NUM);
				setText(tf_videovalues[4], (String) (value == null ? "" : value.toString()));
			} else {
				// PlayItem.GAPLESS_ALBUM
				// hack soultion, find out how to make it robust
				tf_optvalues[2].setEnabled(false);
			}
		}
	}

	protected static JTextField createTF(int align) {
		JTextField result = new JTextField();
		result.setHorizontalAlignment(align);
		return result;
	}
	
	protected static void setText(JComponent comp, String text) {
		if (comp instanceof CheckedInfoElement)
			((JTextField) ((CheckedInfoElement) comp).getMain()).setText(text);
		else
			((JTextField) comp).setText(text);
	}

	protected static String getText(JComponent comp) {
		return comp instanceof CheckedInfoElement ? ((JTextField) ((CheckedInfoElement) comp).getMain()).getText()
				: ((JTextField) comp).getText();
	}

	protected static Object getSelectedValue(JComponent comp) {
		return comp instanceof CheckedInfoElement ? ((JComboBox) ((CheckedInfoElement) comp).getMain())
				.getSelectedItem() : ((JComboBox) comp).getSelectedItem();
	}

	protected static void setSelectedValue(JComponent comp, Object value, int defa) {
		if (value == null) {
			if (comp instanceof CheckedInfoElement)
				((JComboBox) ((CheckedInfoElement) comp).getMain()).setSelectedIndex(defa);
			else
				((JComboBox) comp).setSelectedIndex(defa);
			return;
		}

		if (comp instanceof CheckedInfoElement)
			((JComboBox) ((CheckedInfoElement) comp).getMain()).setSelectedItem(value);
		else
			((JComboBox) comp).setSelectedItem(value);
	}

	protected static int getSelectedIndex(JComponent comp) {
		return comp instanceof CheckedInfoElement ? ((JComboBox) ((CheckedInfoElement) comp).getMain())
				.getSelectedIndex() : ((JComboBox) comp).getSelectedIndex();
	}

	protected static void setCheck(JComponent comp, boolean check) {
		if (comp instanceof CheckedInfoElement)
			((JCheckBox) ((CheckedInfoElement) comp).getMain()).setSelected(check);
		else
			((JCheckBox) comp).setSelected(check);
	}

	protected static boolean getCheck(JComponent comp) {
		return comp instanceof CheckedInfoElement ? ((JCheckBox) ((CheckedInfoElement) comp).getMain()).isSelected()
				: ((JCheckBox) comp).isSelected();
	}

	protected static int getValue(JComponent comp) {
		return comp instanceof JSlider ? ((JSlider) comp).getValue()
				: ((JSlider) ((CheckedInfoElement) comp).getMain()).getValue();
	}

	protected static void setValue(JComponent comp, Object value, int defVal) {
		JSlider s = comp instanceof JSlider ? (JSlider) comp : (JSlider) ((CheckedInfoElement) comp).getMain();

		if (value != null && value instanceof Integer)
			s.setValue(((Integer) value).intValue());
		else
			s.setValue(defVal);
	}

	protected boolean isChangeRequested(JComponent comp) {
		if (comp instanceof CheckedInfoElement)
			return ((CheckedInfoElement) comp).needChange();
		if (comp instanceof JTextField)
			return getText(comp).trim().length() > 0;
		return true;
	}

	public static Descriptor doMediaInfoInput(Controller controller) {
		return doMediaInfoInput(controller, null, null, null);
	}

	protected final static Descriptor[] FIELD_DESCRIPTORS = {
			// group and individuals attributes
			new Descriptor(Resources.LIST_TITLE, PlayItem.TITLE),
			new Descriptor(Resources.LIST_ALBUM, PlayItem.ALBUM),
			new Descriptor(Resources.LIST_ARTIST, PlayItem.ARTIST),
			new Descriptor(Resources.LIST_ALBUMARTIST, PlayItem.ALBUM_ARTIST),
			new Descriptor(Resources.LIST_COMPOSER, PlayItem.COMPOSER),
			new Descriptor(Resources.LIST_GROUPING, PlayItem.GROUPING),
			new Descriptor(Resources.LIST_GENRE, PlayItem.GENRE),
			new Descriptor(Resources.LIST_COMMENT, PlayItem.COMMENT),
			new Descriptor(Resources.LIST_YEAR, PlayItem.YEAR),
			new Descriptor(Resources.LIST_RATING, PlayItem.RATING),
			new Descriptor(Resources.LIST_BPM, PlayItem.BPM),
			new Descriptor(Resources.LIST_PLAYCOUNT, PlayItem.PLAYED_TIMES),
			new Descriptor(Resources.LIST_SKIPCOUNT, PlayItem.SKIPPED_TIMES),
			new Descriptor(Resources.LIST_COMPILATION, PlayItem.COMPILATION),
			new Descriptor(Resources.LIST_DISK, PlayItem.DISK),
			new Descriptor(Resources.LABEL_SETOF, PlayItem.NUM_DISKS),
			new Descriptor(Resources.LIST_EQUALISATION, PlayItem.EQ_SETTING),
			new Descriptor(Resources.LIST_RELATIVEVOLUMENADJUSTMENT, PlayItem.VOLUME),
			new Descriptor(Resources.LABEL_ARTWORK, PlayItem.ARTWORK),
			// individual attrs
			new Descriptor(Resources.LIST_START_TIME, PlayItem.START),
			new Descriptor(Resources.LABEL_STOPTIME, PlayItem.STOP),
			new Descriptor(Resources.LIST_TRACK, PlayItem.ORDER),
			new Descriptor(Resources.LIST_LENGTH, PlayItem.LENGTH) };

	protected final static Descriptor[] SORT_FIELD_DESCRIPTORS = {
			new Descriptor(Resources.LIST_SORTTITLE, PlayItem.SORT_TITLE),
			new Descriptor(Resources.LIST_SORTALBUM, PlayItem.SORT_ALBUM),
			new Descriptor(Resources.LIST_SORTALBUMARTIST, PlayItem.SORT_ALBUM_ARTIST),
			new Descriptor(Resources.LIST_SORTARTIST, PlayItem.SORT_ARTIST),
			new Descriptor(Resources.LIST_SORTCOMPOSER, PlayItem.SORT_COMPOSER),
			new Descriptor(Resources.LIST_SORTSHOW, PlayItem.SORT_SHOW_TYPE),
			new Descriptor(Resources.LIST_KEYWORDS, PlayItem.KEYWORDS) };

	protected final static Descriptor[] OPT_FIELD_DESCRIPTORS = {
			new Descriptor(Resources.LIST_SKIPSHUFFL, PlayItem.SKIP_SHUFFLING),
			new Descriptor(Resources.LIST_REMEMBERPLBK, PlayItem.REMEMBER_POS),
			new Descriptor(Resources.LIST_PARTGAPLESS, PlayItem.GAPLESS_ALBUM)			
	};

	public static Descriptor doMediaInfoInput(Controller controller, PlayList playList, int[] selections, String title) {
		final MediaInfoInputPanel result = new MediaInfoInputPanel(controller, playList, selections);
		JDialog d = new JDialog(controller.mediachest, title != null ? title : Resources.TITLE_MAKEITALL, true);
		d.getContentPane().add(result);
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				result.returnValue = null;
			}
		});
		d.pack();
		d.setVisible(true);
		return result.returnValue;
	}

	static {
		PLAYLIST_MAP = new int[IpodOptionsTab.DIR_INFO_MAP.length];
		PLAYLIST_MAP[IpodOptionsTab.TITLE + 1] = PlayItem.TITLE;
		PLAYLIST_MAP[IpodOptionsTab.ALBUM + 1] = PlayItem.ALBUM;
		PLAYLIST_MAP[IpodOptionsTab.ARTIST + 1] = PlayItem.ARTIST;
		PLAYLIST_MAP[IpodOptionsTab.COMPOSER + 1] = PlayItem.COMPOSER;
		PLAYLIST_MAP[IpodOptionsTab.GENRE + 1] = PlayItem.GENRE;
		PLAYLIST_MAP[IpodOptionsTab.YEAR + 1] = PlayItem.YEAR;
		PLAYLIST_MAP[IpodOptionsTab.TRACK + 1] = PlayItem.ORDER;
	}

	protected JComponent[] tf_values, tf_sortvalues, tf_optvalues, tf_videovalues;

	protected JComboBox cb_sel;

	protected JLabel l_play_cnt, l_added_mod;

	protected JButton b_artwork;

	protected JCheckBox cb_update_id3, cb_autoincr, cb_artwork_id3;
	
	protected JTextField tf_filename;


	protected static class CheckedInfoElement extends JPanel {
		JComponent main;

		CheckedInfoElement(JComponent c) {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(new JCheckBox());
			add(main = c);
			if (main instanceof JComboBox) {
				((JComboBox) main).addPopupMenuListener(new PopupMenuListener() {

					public void popupMenuCanceled(PopupMenuEvent arg0) {
					}

					public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
						((JCheckBox) getComponent(0)).setSelected(true);

					}

					public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
					}
				});
				if (((JComboBox) main).isEditable()) {
					//					System.err.println("Editor "+((JComboBox) main).getEditor().getClass().getName()+
					//				" editor comp: "+((JComboBox) main).getEditor().getEditorComponent().getClass().getName());
					((JComboBox) main).getEditor().getEditorComponent().addKeyListener(new KeyListener() {

						public void keyPressed(KeyEvent arg0) {
						}

						public void keyReleased(KeyEvent arg0) {
							((JCheckBox) getComponent(0)).setSelected(true);
						}

						public void keyTyped(KeyEvent arg0) {
						}
					});
				}
			} else {
				main.addKeyListener(new KeyListener() {

					public void keyPressed(KeyEvent arg0) {
					}

					public void keyReleased(KeyEvent arg0) {
						((JCheckBox) getComponent(0)).setSelected(true);
					}

					public void keyTyped(KeyEvent arg0) {
					}
				});
				if (main instanceof JCheckBox) {
					((JCheckBox) main).addActionListener (new ActionListener () {
						public void actionPerformed(ActionEvent arg0) {
							((JCheckBox) getComponent(0)).setSelected(true);
						}
					});
				}
			}
		}

		JComponent getMain() {
			return main;
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = main.getPreferredSize();
			return new Dimension(d.width + 24, d.height + 2);
		}

		boolean needChange() {
			return ((JCheckBox) getComponent(0)).isSelected();
		}
	}
}