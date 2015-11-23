/* MediaChest - $RCSfile: MediaOptionsTab.java,v $                                                  
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
 *  $Id: MediaOptionsTab.java,v 1.16 2012/10/18 06:58:59 cvs Exp $                      
 */

package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.directory.JDirectoryChooser;

public class MediaOptionsTab extends JPanel implements Persistancable, ChangeListener {
	public final static String SECNAME = "MediaOptions";

	public final static String REUSEPLAYER = "ReusePlayer";

	public final static String SONGS_PAUSE = "PauseDuration";

	public final static String PLAYLIST_LIMIT = "ListLimitValue";

	public final static String PLAYLIST_TYPE = "ListLimitType";

	public final static String RECURSIVE_PLAYBACK = "RecursivePlayback";

	public final static String REQUEST_COPYFILTER = "RequestCopyFilter";

	public final static String REQUEST_PLAYMODE = "RequestPlayMode";

	public final static String PLAYMODE_SCHEMA = "PlayModeSchema";

	public final static String INTRO_FRAMES = "IntorductoryFramesNumber";

	public final static String RIPPER_FOLDER = "RipperFolder";

	protected final static int MP3_MEDIA = 0;

	protected final static int AUDIO_MEDIA = 1;

	protected final static int SIZE_LOW = 0;

	protected final static int SIZE_HIGH = 1;

	protected final static int SIZE_CUSTOM = 2;

	public MediaOptionsTab(Controller controller) {
		this.controller = controller;
		setLayout(new FixedGridLayout(7, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));
		add(cb_filterOnCopy = new JCheckBox(Resources.LABEL_FILTERONCOPY), "0,0,0");
		add(new SLabel(Resources.LABEL_PAUSE_DURATION), "0,1,2");
		add(sp_pauseDuration = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1)), "2,1");// **

		add(new SLabel(Resources.LABEL_INTRO_FRAMES), "3,1,2");
		add(tf_numberFrames = new JTextField(), "5,1");

		((JTextField) ((JSpinner.NumberEditor) sp_pauseDuration.getEditor()).getTextField())
				.setHorizontalAlignment(JTextField.RIGHT);
		destMediaTypeSel = new RadioButtonsGroup(this);
		JRadioButton rb;
		add(rb = new JRadioButton(Resources.LABEL_MP3_DISK), "0,2,0");
		destMediaTypeSel.add(rb, MP3_MEDIA);
		add(rb = new JRadioButton(Resources.LABEL_AUDIO_CD), "0,3,0");
		destMediaTypeSel.add(rb, AUDIO_MEDIA);
		mediaSize = new RadioButtonsGroup();
		add(rb = new JRadioButton(), "2,2");
		mediaSize.add(rb, SIZE_LOW);
		add(rb = new JRadioButton(), "2,3");
		mediaSize.add(rb, SIZE_HIGH);
		add(rb = new JRadioButton(Resources.LABEL_SIZE_CUSTOM), "2,4,0");
		mediaSize.add(rb, SIZE_CUSTOM);
		add(tf_customSize = new JTextField(), "1,4");
		tf_customSize.setHorizontalAlignment(JTextField.RIGHT);
		add(cb_reuse_player = new JCheckBox(Resources.LABEL_REUSE_MPLAYER), "4,2,0");
		add(cb_recursive = new JCheckBox(Resources.LABEL_RECURSIVE_PLAYBACK), "4,3,0");
		add(cb_request_playmode = new JCheckBox(Resources.LABEL_REQUEST_PLAYMODE), "4,4,0");

		add(cb_playbackSchema = new JComboBox(), "2,0,2");
		cb_playbackSchema.setEditable(true);
		JButton bt;
		add(bt = new JButton(Resources.CMD_EDIT), "4,0");
		bt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					PlaybackProperties.doPropertiesDialog(MediaOptionsTab.this.controller,
							(JFrame) getTopLevelAncestor()).saveCurrentSchema();
				} catch (Exception ex2) {
					ex2.printStackTrace();
					// report to status bar
				}
			}
		});
		tf_numberFrames.setHorizontalAlignment(JTextField.RIGHT);
		add(new SLabel(Resources.LABEL_RIPPER_FOLDER), "0,6");
		add(tf_ripperLocation = new JTextField(), "1,6,4");
		add(bt = new JButton(Resources.CMD_BROWSE), "5,6");
		bt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JDirectoryChooser dc = new JDirectoryChooser(new JFrame(), tf_ripperLocation.getText(), null);
				if (dc.getDirectory() != null)
					tf_ripperLocation.setText(dc.getDirectory());
			}
		});
	}

	public void stateChanged(ChangeEvent e) {
		if (destMediaTypeSel.getSelectedIndex() == MP3_MEDIA) {
			mediaSize.get(SIZE_LOW).setText(Resources.LABEL_SIZE_650);
			mediaSize.get(SIZE_HIGH).setText(Resources.LABEL_SIZE_700);
		} else {
			mediaSize.get(SIZE_LOW).setText(Resources.LABEL_PLAY_74);
			mediaSize.get(SIZE_HIGH).setText(Resources.LABEL_PLAY_80);
		}
	}

	public void load() {
		IniPrefs s = controller.getPrefs();
		cb_reuse_player.setSelected(s.getInt(s.getProperty(SECNAME, REUSEPLAYER), 0) == 0);
		Integer i = (Integer) s.getProperty(SECNAME, PLAYLIST_TYPE);
		destMediaTypeSel.setSelectedIndex(i != null ? i.intValue() : AUDIO_MEDIA);
		stateChanged(null);
		i = (Integer) s.getProperty(SECNAME, PLAYLIST_LIMIT);
		if (i != null && i.intValue() <= SIZE_HIGH && i.intValue() >= SIZE_LOW)
			mediaSize.setSelectedIndex(i.intValue());
		else {
			mediaSize.setSelectedIndex(SIZE_CUSTOM);
			tf_customSize.setText(i == null ? "0" : i.toString());
		}
		i = (Integer) s.getProperty(SECNAME, SONGS_PAUSE);
		sp_pauseDuration.setValue(i == null ? Resources.I_NO : i);
		i = (Integer) s.getProperty(SECNAME, INTRO_FRAMES);
		tf_numberFrames.setText(i == null ? "0" : i.toString());
		cb_recursive.setSelected(s.getInt(s.getProperty(SECNAME, RECURSIVE_PLAYBACK), 0) == 1);
		cb_request_playmode.setSelected(s.getInt(s.getProperty(SECNAME, REQUEST_PLAYMODE), 0) == 1);
		cb_filterOnCopy.setSelected(s.getInt(s.getProperty(SECNAME, REQUEST_COPYFILTER), 0) == 1);
		String l = (String) s.getProperty(SECNAME, RIPPER_FOLDER);
		if (l != null)
			tf_ripperLocation.setText(l);
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		s.setProperty(SECNAME, REUSEPLAYER, cb_reuse_player.isSelected() ? Resources.I_NO : Resources.I_YES);
		s.setProperty(SECNAME, PLAYLIST_TYPE, new Integer(destMediaTypeSel.getSelectedIndex()));
		if (destMediaTypeSel.getSelectedIndex() == SIZE_CUSTOM)
			try {
				s.setProperty(SECNAME, PLAYLIST_LIMIT, new Integer(tf_customSize.getText()));
			} catch (Exception e) {
				s.setProperty(SECNAME, PLAYLIST_LIMIT, new Integer(100));
			}
		else
			s.setProperty(SECNAME, PLAYLIST_LIMIT, new Integer(mediaSize.getSelectedIndex()));
		try {
			s.setProperty(SECNAME, SONGS_PAUSE, sp_pauseDuration.getValue());
		} catch (Exception e) {
		}
		// TODO: reconsider basic set og methods adding specific short cuts
		try {
			s.setProperty(SECNAME, INTRO_FRAMES, new Integer(tf_numberFrames.getText()));
		} catch (Exception e) {
		}
		s.setProperty(SECNAME, RECURSIVE_PLAYBACK, cb_recursive.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, REQUEST_PLAYMODE, cb_request_playmode.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, REQUEST_COPYFILTER, cb_filterOnCopy.isSelected() ? Resources.I_YES : Resources.I_NO);
		s.setProperty(SECNAME, RIPPER_FOLDER, tf_ripperLocation.getText());
	}

	protected Controller controller;

	protected JCheckBox cb_reuse_player, cb_recursive, cb_request_playmode, cb_filterOnCopy;

	protected JTextField tf_customSize, tf_numberFrames, tf_ripperLocation;

	protected RadioButtonsGroup destMediaTypeSel, mediaSize;

	protected JComboBox cb_playbackSchema;

	protected JSpinner sp_pauseDuration;
}
