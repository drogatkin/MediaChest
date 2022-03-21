/* MediaChest - $RCSfile: PlaybackProperties.java,v $                         
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
 *  $Id: PlaybackProperties.java,v 1.16 2012/10/18 06:58:59 cvs Exp $          
 */

package photoorganizer.renderer;

import static mediautil.gen.MediaInfo.ALBUM;
import static mediautil.gen.MediaInfo.ARTISTWEBPAGE;
import static mediautil.gen.MediaInfo.AUDIOFILEWEBPAGE;
import static mediautil.gen.MediaInfo.AUDIOSOURCEWEBPAGE;
import static mediautil.gen.MediaInfo.BAND;
import static mediautil.gen.MediaInfo.BITRATE;
import static mediautil.gen.MediaInfo.CDIDENTIFIER;
import static mediautil.gen.MediaInfo.COMMENTS;
import static mediautil.gen.MediaInfo.COMPOSER;
import static mediautil.gen.MediaInfo.CONDUCTOR;
import static mediautil.gen.MediaInfo.CONTENTGROUP;
import static mediautil.gen.MediaInfo.COPYRIGHT;
import static mediautil.gen.MediaInfo.COPYRIGHTTEXT;
import static mediautil.gen.MediaInfo.COPYRIGHTWEBPAGE;
import static mediautil.gen.MediaInfo.FILEOWNER;
import static mediautil.gen.MediaInfo.FILETYPE;
import static mediautil.gen.MediaInfo.INITIALKEY;
import static mediautil.gen.MediaInfo.INTERNETRADIOSTATIONNAME;
import static mediautil.gen.MediaInfo.INTERNETRADIOSTATIONOWNER;
import static mediautil.gen.MediaInfo.INTERNETRADIOSTATIONWEBPAGE;
import static mediautil.gen.MediaInfo.ISRC;
import static mediautil.gen.MediaInfo.LANGUAGE;
import static mediautil.gen.MediaInfo.LYRICIST;
import static mediautil.gen.MediaInfo.MEDIATYPE;
import static mediautil.gen.MediaInfo.MODE;
import static mediautil.gen.MediaInfo.ORIGINAL;
import static mediautil.gen.MediaInfo.ORIGINALARTIST;
import static mediautil.gen.MediaInfo.ORIGINALLYRICIST;
import static mediautil.gen.MediaInfo.ORIGINALTITLE;
import static mediautil.gen.MediaInfo.ORIGINALYEAR;
import static mediautil.gen.MediaInfo.OWNERSHIP;
import static mediautil.gen.MediaInfo.PUBLISHER;
import static mediautil.gen.MediaInfo.PUBLISHERSWEBPAGE;
import static mediautil.gen.MediaInfo.RECORDINGDATES;
import static mediautil.gen.MediaInfo.REMIXER;
import static mediautil.gen.MediaInfo.REVERB;
import static mediautil.gen.MediaInfo.SAMPLERATE;
import static mediautil.gen.MediaInfo.SUBTITLE;
import static mediautil.gen.MediaInfo.TITLE;
import static mediautil.gen.MediaInfo.TRACK;
import static mediautil.gen.MediaInfo.UNIQUEFILEIDENTIFIER;
import static mediautil.gen.MediaInfo.USERDEFINEDTEXT;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import mediautil.gen.MediaFormat;

import org.aldan3.app.ui.FixedGridLayout;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.formats.MP3;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.media.ContentMatcher;
import photoorganizer.media.PlaybackRequest;

// TODO: rename something playback filter
public class PlaybackProperties extends JPanel {

	public static final int START_YEAR = 1938;

	public static final String PLAY_ATTRIBUTES[] = { ALBUM, ARTISTWEBPAGE, AUDIOFILEWEBPAGE, AUDIOSOURCEWEBPAGE, BAND,
			CDIDENTIFIER, COMMENTS, COMPOSER, CONDUCTOR, CONTENTGROUP, COPYRIGHTTEXT, COPYRIGHT, COPYRIGHTWEBPAGE,
			FILEOWNER, FILETYPE, INITIALKEY, INTERNETRADIOSTATIONNAME, INTERNETRADIOSTATIONOWNER,
			INTERNETRADIOSTATIONWEBPAGE, ISRC, LANGUAGE, LYRICIST, MEDIATYPE, ORIGINAL, ORIGINALARTIST,
			ORIGINALLYRICIST, ORIGINALTITLE, ORIGINALYEAR, OWNERSHIP, PUBLISHER, PUBLISHERSWEBPAGE, RECORDINGDATES,
			REMIXER, REVERB, SUBTITLE, TITLE, TRACK, UNIQUEFILEIDENTIFIER, USERDEFINEDTEXT, BITRATE, MODE, SAMPLERATE };

	public PlaybackProperties(Controller controller, Window window) {
		this(controller, window, false, null);
	}

	public PlaybackProperties(Controller controller, Window window, boolean saveOption, List playLists) {

		setLayout(new FixedGridLayout(5, playLists != null ? 15 : 14, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		this.controller = controller;
		if (window == null)
			throw new IllegalArgumentException("Window cannot be null.");
		this.window = window;
		add(new JLabel(Resources.LABEL_SCHEMA_NAME), "0,0,2");
		add(cb_schema = new JComboBox(new File(controller.getHomeDirectory()).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(Resources.EXT_PSX) && new File(dir, name).isFile();
			}
		})), "2,0,2");
		cb_schema.setEditable(true);
		int bp = 1;
		if (playLists != null) {
			add(new JLabel("Play List:"), "0,1,1");
			add(new JComboBox(playLists.toArray()), "1,1,2");
			bp++;
		}
		JTabbedPane propPanel = new JTabbedPane(SwingConstants.TOP);
		propPanel.insertTab(Resources.TAB_GENRE, (Icon) null, gsp = new GenreSelectionPanel(), Resources.TTIP_GENRE, 0);
		propPanel.insertTab(Resources.TAB_ARTIST, (Icon) null, asp = new ArtistSelectionPanel(), Resources.TTIP_ARTIST,
				1);
		propPanel.insertTab(Resources.TAB_YEARS, (Icon) null, ysp = new YearSelectionPanel(), Resources.TTIP_YEARS, 2);
		propPanel.insertTab(Resources.TAB_ADDITIONS, (Icon) null, dsp = new AdditionsSelectionPanel(),
				Resources.TTIP_ADDITIONS, 3);
		/*
		 * propPanel.insertTab(Resources.TAB_IPOD, (Icon)null, rctp = new
		 * MagicSelectionPanel(), Resources.TTIP_IPOD, 4);
		 */
		add(propPanel, "0," + bp + ",5,9");
		bp += 10;
		ButtonGroup group = new ButtonGroup();
		add(rb_rand = new JRadioButton(Resources.LABEL_RANDOM), "0," + bp);
		rb_rand.setToolTipText(Resources.LABEL_RANDOM);
		group.add(rb_rand);
		rb_rand.setSelected(true);
		add(rb_shuffl = new JRadioButton(Resources.LABEL_SHUFFLE), "1," + bp);
		rb_shuffl.setToolTipText(Resources.LABEL_SHUFFLE);
		group.add(rb_shuffl);
		add(cb_exclude = new JCheckBox(Resources.LABEL_EXCLUDE), "2," + bp);
		cb_exclude.setToolTipText(Resources.LABEL_EXCLUDE);
		if (playLists == null) {
			// digest mode
			add(cb_intro = new JCheckBox(Resources.LABEL_INTROMODE), "3," + bp + ",2");
			cb_intro.setToolTipText(Resources.LABEL_INTROMODE);
		}
		bp++;
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		JButton b;
		buttons.add(b = new JButton(saveOption ? Resources.CMD_SAVE : Resources.CMD_OK));
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// process
				matcher = new ContentMatcher();
				matcher.setGenres(gsp.getData());
				matcher.setArtists(asp.getData());
				matcher.setYears(ysp.getData());
				matcher.setExcludeMode(cb_exclude.isSelected());
				matcher.setExtraConditions(dsp.getData());
				PlaybackProperties.this.window.dispose();
			}
		});
		buttons.add(b = new JButton(Resources.CMD_CANCEL));
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PlaybackProperties.this.window.dispose();
			}
		});
		buttons.add(new JButton(Resources.CMD_HELP));
		buttons.add(new JButton(Resources.CMD_ADVANCED));
		add(buttons, "0," + bp + ",5,2");
	}

	public ContentMatcher getMatcher() {
		return matcher;
	}

	public void applyTo(PlaybackRequest request) {
		request.matcher = getMatcher();
		if (!cb_intro.isSelected())
			request.introFrames = 0;
		request.shuffled = rb_shuffl.isSelected();

	}

	public void saveCurrentSchema() throws IOException {
		Object item = cb_schema.getSelectedItem();
		if (item != null) {
			String schemaName = item.toString();
			if (schemaName.trim().length() == 0) {
				schemaName = JOptionPane.showInputDialog(this, Resources.LABEL_GET_SCHEMA_NAME, Resources.TITLE_NAME,
						JOptionPane.QUESTION_MESSAGE);
				if (schemaName != null && schemaName.length() > 0) {
					if (schemaName.endsWith(Resources.EXT_PSX) == false)
						schemaName += Resources.EXT_PSX;
					XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(new File(controller
							.getHomeDirectory(), schemaName))));
					e.writeObject(getMatcher());
					e.close();
					// System.err.println("A schema saved to "+new
					// File(controller.getHomeDirectory(), schemaName));
				}
			}
		}
	}

	// TODO: add version getting the list from flat file
	// TODO: add version returning request structure used for matching the
	// selected criterias

	public static void doModal(Controller controller, PlaybackRequest request) {
		JFrame frame = new JFrame(Resources.TITLE_CONTENT_SELECTION_CRITERIA);
		frame.setIconImage(controller.getMainIcon());
		final PlaybackProperties pbps = new PlaybackProperties(controller, frame);
		frame.setContentPane(pbps);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				synchronized (pbps) {
					pbps.notify();
				}
			}
		});
		frame.pack();
		frame.setVisible(true);
		synchronized (pbps) {
			try {
				pbps.wait();
			} catch (InterruptedException ie) {
			}
			pbps.applyTo(request);
			try {
				pbps.saveCurrentSchema();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static PlaybackProperties doPropertiesDialog(Controller controller, List playLists) {
		JDialog d = new JDialog(controller.mediachest, Resources.TITLE_CONTENT_SELECTION_CRITERIA, true);
		final PlaybackProperties pbps = new PlaybackProperties(controller, d, false, playLists);
		d.getContentPane().add(pbps);
		d.pack();
		d.show();

		return pbps;
	}

	public static PlaybackProperties doPropertiesDialog(Controller controller, Frame frame) {
		PlaybackProperties result = null;
		JDialog dialog = new JDialog(frame == null ? controller.mediachest : frame,
				Resources.TITLE_CONTENT_SELECTION_CRITERIA, true);
		dialog.setContentPane(result = new PlaybackProperties(controller, dialog, true, null));
		dialog.pack();
		dialog.show();
		return result;
	}

	public static Object[] doModal(Controller controller, Object[] medias) {
		if (medias == null || medias.length == 0)
			return null;
		// TODO: make desc from frame and override dispose to notify
		JFrame frame = new JFrame(Resources.TITLE_CONTENT_SELECTION_CRITERIA);
		frame.setIconImage(controller.getMainIcon());
		final PlaybackProperties pbps = new PlaybackProperties(controller, frame);
		frame.getContentPane().add(pbps, "Center");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				synchronized (pbps) {
					pbps.notify();
				}
			}
		});
		frame.pack();
		frame.setVisible(true);
		synchronized (pbps) {
			try {
				pbps.wait();
			} catch (InterruptedException ie) {
				return null;
			}
		}
		ContentMatcher matcher = pbps.getMatcher();
		if (matcher == null)
			return null;
		int k = 0;
		for (int i = 0; i < medias.length; i++)
			if (medias[i] instanceof MediaFormat)
				if (matcher.match((MediaFormat) medias[i]))
					medias[k++] = medias[i];
				else
					;
			else if (medias[i] instanceof File)
				if (matcher.match(MediaFormatFactory.createMediaFormat((File) medias[i])))
					medias[k++] = medias[i];
				else
					;
		if (k == 0)
			return null;
		Object[] result = new Object[k];
		System.arraycopy(medias, 0, result, 0, k);
		return result;
	}

	class ListSelectionPanel extends JPanel implements ActionListener, ListSelectionListener {
		ListSelectionPanel() {
			setLayout(new FixedGridLayout(5, 8, Resources.CTRL_VERT_SIZE, Resources.CTRL_VERT_GAP,
					Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
			add(new JScrollPane(resultList = new JList(new DefaultListModel())), "0,1,2,6");
			resultList.addListSelectionListener(this);
			add(btnCopy = new JButton(Resources.CMD_COPY), "2,2");
			btnCopy.addActionListener(this);
			add(btnCopyAll = new JButton(Resources.CMD_COPY_ALL), "2,3");
			btnCopyAll.addActionListener(this);
			add(btnDelete = new JButton(Resources.CMD_DELETE), "2,4");
			btnDelete.addActionListener(this);
			add(btnDeleteAll = new JButton(Resources.CMD_DELETE_ALL), "2,5");
			btnDeleteAll.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			DefaultListModel model = (DefaultListModel) resultList.getModel();
			if (Resources.CMD_COPY.equals(cmd)) {
				addElements(getSelectedItems(), model);
				clearSelection();
			} else if (Resources.CMD_COPY_ALL.equals(cmd)) {
				addElements(getAllItems(), model);
			} else if (Resources.CMD_DELETE.equals(cmd)) {
				removeElements(resultList.getSelectedValues(), model);
			} else if (Resources.CMD_DELETE_ALL.equals(cmd)) {
				model.removeAllElements();
			}
		}

		public void valueChanged(ListSelectionEvent e) {
			updateUiState();
		}

		Object[] getData() {
			DefaultListModel model = (DefaultListModel) resultList.getModel();
			return model.toArray();
		}

		void updateUiState() {
			Object[] si = getSelectedItems();
			btnCopy.setEnabled(si != null && si.length > 0);
			btnCopyAll.setEnabled(getAllItems() != null);
			si = resultList.getSelectedValues();
			btnDelete.setEnabled(si != null && si.length > 0);
			btnDeleteAll.setEnabled(((DefaultListModel) resultList.getModel()).size() > 0);
		}

		void addElements(Object[] elements, DefaultListModel model) {
			for (int l = 0; elements != null && l < elements.length; l++)
				if (!model.contains(elements[l]))
					model.addElement(elements[l]);
		}

		void removeElements(Object[] elements, DefaultListModel model) {
			for (int l = 0; elements != null && l < elements.length; l++)
				model.removeElement(elements[l]);
		}

		Object[] getSelectedItems() {
			return null;
		}

		void clearSelection() {
		}

		Object[] getAllItems() {
			return null;
		}

		JList resultList;

		JButton btnCopy, btnCopyAll, btnDelete, btnDeleteAll;
	}

	class GenreSelectionPanel extends ListSelectionPanel {
		GenreSelectionPanel() {
			String[] genreSorted = (String[]) MP3.GENRES.clone();
			Arrays.sort(genreSorted);
			add(new JScrollPane(l_genres = new JList(genreSorted)), "3,1,2,6");
			l_genres.addListSelectionListener(this);
			updateUiState();
		}

		Object[] getSelectedItems() {
			return l_genres.getSelectedValues();
		}

		void clearSelection() {
			l_genres.removeSelectionInterval(0, MP3.GENRES.length - 1);
		}

		Object[] getAllItems() {
			return MP3.GENRES;
			// Object allEls[] = new Object[l_genres.getModel().getSize()]
			// for (int i=0; i<allEls.length; i++)
			// allEls[i] = l_genres.getModel().getElementAt(i);
			// return allEls;
			// return l_genres.getModel().toArray();
		}

		JList l_genres;
	}

	class ArtistSelectionPanel extends ListSelectionPanel {
		ArtistSelectionPanel() {
			add(tf_artist = new JTextField(), "3,1,2");
			tf_artist.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
				}

				public void removeUpdate(DocumentEvent e) {
					updateUiState();
				}

				public void insertUpdate(DocumentEvent e) {
					updateUiState();
				}
			});
			updateUiState();
		}

		Object[] getSelectedItems() {
			String result = tf_artist.getText().trim();
			if (result.length() == 0)
				return null;
			selectedItems[0] = result;
			return selectedItems;
		}

		void clearSelection() {
			tf_artist.setText("");
		}

		Object[] getAllItems() {
			return null;
		}

		JTextField tf_artist;

		Object[] selectedItems = new Object[1];
	}

	class YearSelectionPanel extends ListSelectionPanel {
		YearSelectionPanel() {
			add(new JScrollPane(l_years = new JList(new DefaultListModel())), "3,1,1,6");
			int currYear = Calendar.getInstance().get(Calendar.YEAR);
			DefaultListModel model = (DefaultListModel) l_years.getModel();
			for (int y = START_YEAR; y <= currYear; y++)
				// model.addElement(String.valueOf(y));
				model.addElement(new Integer(y));
			l_years.addListSelectionListener(this);
			updateUiState();
		}

		Object[] getSelectedItems() {
			return l_years.getSelectedValues();
		}

		void clearSelection() {
			l_years.removeSelectionInterval(0, ((DefaultListModel) l_years.getModel()).size() - 1);
		}

		Object[] getAllItems() {
			return ((DefaultListModel) l_years.getModel()).toArray();
		}

		JList l_years;
	}

	class AdditionsSelectionPanel extends ListSelectionPanel {
		AdditionsSelectionPanel() {
			add(tf_addition = new JTextField(), "3,1,2");
			add(new JScrollPane(l_types = new JList(PLAY_ATTRIBUTES)), "3,2,2,5");
			tf_addition.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
				}

				public void removeUpdate(DocumentEvent e) {
					updateUiState();
				}

				public void insertUpdate(DocumentEvent e) {
					updateUiState();
				}
			});
			updateUiState();
		}

		Object[] getSelectedItems() {
			String result = tf_addition.getText().trim();
			if (result.length() == 0)
				return null;
			Object[] types = l_types.getSelectedValues();
			if (types != null && types.length > 0) {
				result += "{" + types[0];
				for (int t = 1; t < types.length; t++)
					result += "," + types[t];
				result += "}";
			}
			selectedItems[0] = result;
			return selectedItems;
		}

		void clearSelection() {
			l_types.removeSelectionInterval(0, PLAY_ATTRIBUTES.length - 1);
			tf_addition.setText("");
		}

		Object[] getAllItems() {
			return null;
		}

		JList l_types;

		JTextField tf_addition;

		Object[] selectedItems = new Object[1];
	}

	protected Controller controller;

	protected Window window;

	protected ListSelectionPanel gsp, asp, ysp, dsp;

	protected JCheckBox cb_intro, cb_exclude;

	protected JRadioButton rb_rand, rb_shuffl;

	protected ContentMatcher matcher;

	protected JComboBox cb_schema;
	// Object monitor = new Object();
}
