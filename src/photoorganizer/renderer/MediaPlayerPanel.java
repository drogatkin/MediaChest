/* PhotoOrganizer - $RCSfile: MediaPlayerPanel.java,v $                                                         
 * Copyright (C) 2001-2013 Dmitriy Rogatkin.  All rights reserved.                   
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
 *  $Id: MediaPlayerPanel.java,v 1.48 2013/03/09 07:56:42 cvs Exp $                       
 */

package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import photoorganizer.Controller;
import photoorganizer.IrdControllable;
import photoorganizer.Resources;
import photoorganizer.formats.MP4;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.media.MediaPlayer;
import photoorganizer.media.MediaPlayer.ProgressListener;
import photoorganizer.media.MediaPlayer.Status;
import photoorganizer.media.PlaybackRequest;
import player.PlayerAdapter;
import player.PlayerControl;
import player.PlayerFactory;

public class MediaPlayerPanel extends JPanel implements ActionListener, IrdControllable {

	static final int MS_IN_FRAME = 26;

	// TODO: consider reading from configuration
	protected static final MessageFormat INFO_FMT = new MessageFormat(Resources.FMT_PLAYING_INFO);

	protected Controller controller;

	PlayerControl m4Player;

	MediaPlayer mediaPlayer;
	
	Object playerControl = new Object();

	// for remote options
	protected MediaPlayerPanel() {

	}

	public MediaPlayerPanel(boolean listPlayer, Controller controller) {
		this.controller = controller;
		setLayout(new BorderLayout());
		// normal panel
		JPanel normalPanel = new JPanel();
		normalPanel.setLayout(new BorderLayout());
		JScrollPane sp;
		normalPanel.add(sp = new JScrollPane(tp_info = new JEditorPane()), BorderLayout.NORTH);
		sp.setPreferredSize(new Dimension(200, 60));
		tp_info.setContentType("text/html; charset=UTF-8"); // Resources.CT_TEXT_HTML
		tp_info.setEditable(false);
		MiscellaneousOptionsTab.applyFontSettings(tp_info, controller);
		normalPanel.add(progress = new ProgressCtrl(), BorderLayout.CENTER);
		remoteCommandMap = new HashMap(4);
		add(normalPanel, BorderLayout.NORTH);
		// advanced panel
		advancedPanel = new JPanel();
		advancedPanel.setLayout(new FlowLayout());
		advancedPanel.add(new JButton(Resources.CMD_MARK));
		advancedPanel.add(new JButton(Resources.CMD_TO_SEL));
		advancedPanel.add(new JButton(Resources.CMD_TO_ALB));
		// buttons panel
		// TODO make buttons icons
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(resume = new JButton(Resources.CMD_RESUME));
		resume.addActionListener(this);
		resume.setEnabled(false);
		remoteCommandMap.put(Resources.CMD_RESUME, resume);
		if (listPlayer) {
			buttons.add(skip = new JButton(Resources.CMD_SKIP));
			skip.addActionListener(this);
			skip.setEnabled(false);
			remoteCommandMap.put(Resources.CMD_SKIP, skip);
		} else
			remoteCommandMap.put(Resources.CMD_SKIP, null);
		buttons.add(stop = new JButton(Resources.CMD_STOP));
		stop.addActionListener(this);
		stop.setEnabled(false);
		remoteCommandMap.put(Resources.CMD_STOP, stop);
		buttons.add(close = new JButton(Resources.CMD_CLOSE));
		close.addActionListener(this);
		remoteCommandMap.put(Resources.CMD_CLOSE, close);
		buttons.add(advanced = new JButton(Resources.CMD_ADVANCED));
		add(buttons, BorderLayout.CENTER);
		advanced.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO change visibility attr
				if (getComponentCount() > 2) {
					remove(advancedPanel);
					advanced.setText(Resources.CMD_ADVANCED);
				} else {
					add(advancedPanel, BorderLayout.SOUTH);
					advanced.setText(Resources.LABEL_NORMAL);
				}
				pack();
			}
		});
		setupProgress();
	}

	protected void setupProgress() {
		progress.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (source.getValueIsAdjusting()) {
					int fps = (int) source.getValue();
					stop();
					if (m4Player != null && m4Player.getState() == PlayerControl.PLAYING) {
						try {
							m4Player.seek(fps * 1000);
						} catch (IllegalStateException e1) {
							if (__debug)
								e1.printStackTrace();
						} catch (IOException e1) {
							if (__debug)
								e1.printStackTrace();
						}
					} else if (mediaPlayer != null){
						//synchronized(MediaPlayerPanel.this) {
						mediaPlayer.setIntro(fps /** 1000 / MS_IN_FRAME*/);
						//}
					}
					resume();
				}
			}
		});
	}

	protected void addVideoScreen(Component videoScreen) {

	}

	public MediaPlayerPanel(MediaFormat media, Window window, int introFrames, Controller controller)
			throws IOException {
		this(false, controller);
		this.window = window;
		if (window != null)
			window.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					//System.err.println("Closing win called");
					//MediaPlayerPanel.this.window.dispose();
					close();
				}

				public void windowClosed(WindowEvent e) {
					//System.err.println("Closed win called");
					//close();
				}
			});
		replay(media, introFrames);
		autoClose = true;
	}

	public static MediaPlayerPanel getIrdControllable(Controller controller) {
		if (controller.mediaPlayer == null)
			controller.mediaPlayer = new MediaPlayerPanel();
		return controller.mediaPlayer;
	}

	public void waitForCompletion() {
		if (m4Player != null && m4Player.getState() == PlayerControl.PLAYING)
			synchronized (playerControl) {
				try {
					if (__debug)
						System.err.printf("Normal wait %s%n", m4Player);
					playerControl.wait();
				} catch (InterruptedException ie) {
				}
			}
		else if (mediaPlayer != null) {
			if (__debug)
				System.err.printf("Wait for media player..%n");
			mediaPlayer.waitPlayEnds();
			if (Status.inerror.equals(mediaPlayer.getStatus())) {
				StatusBar statusbar = (StatusBar) controller.component(Controller.COMP_STATUSBAR);
				statusbar.flashInfo(mediaPlayer.getLastMessage());
			}
			if (__debug)
				System.err.printf("Done for media player..%n");
		}
	}

	public void replay(MediaFormat media) throws IOException {
		replay(media, 0);
	}

	public synchronized WindowListener getWindowListener() {
		if (windowListener == null)
			windowListener = new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					close();
				}
			};
		return windowListener;
	}

	public void replay(MediaFormat media, int intro) throws IOException {
		stop();
		//new Exception().printStackTrace();
		// TODO introduce play interface in media itself so only call it here
		if (mediaPlayer != null)
			mediaPlayer.close();
		mediaPlayer = MediaFormatFactory.getPlayer(media);
		if (mediaPlayer == null)
			if (media instanceof MP4)
				playMP4(media);
			else
				throw new IOException("Not supported format " + media);
		else {
			mediaPlayer.setIntro(intro);
			mediaPlayer.setProgressListener(progress);
			playProlog(media);
			mediaPlayer.start();
			stop.setEnabled(true);
			if (skip != null)
				skip.setEnabled(true);
			resume.setEnabled(false);
		}
		closed = false;		
	}

	protected void playProlog(MediaFormat media) {
		updateTitle(Resources.TITLE_NOW_PLAYING + media.toString());
		updateSongInf0(media);
		progress.setMaximum((int) media.getMediaInfo().getLongAttribute(MediaInfo.LENGTH));
	}

	protected void playMP4(MediaFormat media) {
		if (m4Player == null) {
			m4Player = PlayerFactory.createLightweightMPEG4Player();
			m4Player.addListener(new PlayerAdapter() {

				@Override
				public void changedState(final int state) {
					try {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {

								switch (state) {
								case PlayerControl.CLOSED:
									if (close != null)
										close.setEnabled(false);
									stop.setEnabled(false);
									if (skip != null)
										skip.setEnabled(false);
									break;

								case PlayerControl.STOPPED:
									if (__debug)
										System.err.printf("Stopped%n");
									if (close != null)
										close.setEnabled(true);
									if (skip != null)
										skip.setEnabled(true);
									resume.setEnabled(true);
									stop.setEnabled(false);
									synchronized (MediaPlayerPanel.this.playerControl) {
										MediaPlayerPanel.this.playerControl.notify();
									}
									// note that content of a play is available to explore
									break;

								case PlayerControl.PLAYING:
									if (close != null)
										close.setEnabled(true);
									stop.setEnabled(true);
									if (skip != null)
										skip.setEnabled(true);
									resume.setEnabled(false);
									break;

								case PlayerControl.PAUSED:
									if (close != null)
										close.setEnabled(true);
									resume.setEnabled(true);
									if (skip != null)
										skip.setEnabled(true);
									stop.setEnabled(false);
									break;
								}
							}

						});
					} catch (Exception ignored) {
						if (__debug)
							ignored.printStackTrace();
					}
				}

				@Override
				public void playerTime(long arg0) {
					//System.err.printf("Time:, arg1)
					progress.setValue((int) arg0 / 1000);
				}

			});
		}

		m4Player.setPlayerEndAction(PlayerControl.END_ACTION_STOP);
		//		resume.setEnabled(false);

		try {
			m4Player.stopUrl();
			if ((media.getType() & MediaFormat.VIDEO) != 0) {
				addVideoScreen(m4Player.getRendererComponent());
				m4Player.setScaling(true);
			}

			m4Player.open(media.getFile().getPath());
			playProlog(media);
			m4Player.start();
			//m4Player.setSize(800, 600);
			if (__debug)
				System.err.printf("Playback %s started %n", media);
		} catch (Exception e) {
			System.err.printf("An exception in playing: %s%n", e);
			if (__debug)
				e.printStackTrace();
			m4Player.stopUrl();
		}
	}

	
	public boolean isClosed() {
		return closed;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		//System.err.printf("Command %s%n", cmd);
		if (cmd == Resources.CMD_RESUME)
			resume();
		else if (cmd == Resources.CMD_STOP) {
			stop();
		} else if (cmd == Resources.CMD_CLOSE) {
			close();
			updateTitle(Resources.TITLE_CLOSED);
			tp_info.setText("");
			synchronized (playerControl) {
				playerControl.notify();
			}
		} else if (cmd == Resources.CMD_SKIP) {
			if (m4Player != null && m4Player.getState() == PlayerControl.PLAYING) {
				try {
					m4Player.stop();
				} catch (Exception ex) {
					if (__debug)
						ex.printStackTrace();
				}
				synchronized (playerControl) {
					playerControl.notify();
				}
			} else {				
					mediaPlayer.stop();
				if (__debug)
					System.err.printf("Skip %b%n", closed);
			}
		}
	}

	public void stop() {
		boolean stopApplied = false;
		if (m4Player != null && m4Player.getState() == PlayerControl.PLAYING) {
			try {
				m4Player.pause();
				stopApplied = true;
			} catch (Exception e) {
				if (__debug)
					e.printStackTrace();
			}
		} else if (mediaPlayer != null) {
			mediaPlayer.pause();
			stopApplied = true;
		}
		if (stopApplied) {
			stop.setEnabled(false);
			if (skip != null)
				skip.setEnabled(false);
			resume.setEnabled(true);
		}
	}

	public void resume() {
		boolean resumeApplied = false;
		if (m4Player != null && m4Player.getState() == PlayerControl.PAUSED) {
			try {
				m4Player.resume();
				bringOnTop();
			} catch (IllegalStateException e) {
				if (__debug)
					e.printStackTrace();
			} catch (IOException e) {
				if (__debug)
					e.printStackTrace();
			}
			resumeApplied = true;
		} else if (mediaPlayer != null) {
			mediaPlayer.resume();
			resumeApplied = true;
		}

		if (resumeApplied) {
			stop.setEnabled(true);
			if (close != null)
				close.setEnabled(true);
			if (skip != null)
				skip.setEnabled(true);
			resume.setEnabled(false);
		}
	}

	synchronized protected void closeMP4() {
		if (m4Player != null) {
			m4Player.stopUrl();
			try {
				m4Player.close();
			} catch (IOException e) {
				if (__debug)
					e.printStackTrace();
			}
			progress.setValue(0);
		}
	}

	public void close() {		
		if (mediaPlayer != null) {
			mediaPlayer.close();
		} else
			closeMP4();
		closed = true;
		stop.setEnabled(false);
		if (skip != null)
			skip.setEnabled(false);
		resume.setEnabled(false);
		if (close != null)
			close.setEnabled(false);
		if (window != null) {
			window.dispose();
			window = null;
		}
		synchronized (playerControl) {
			playerControl.notify();
		}
	}

	// remote controllable
	public String getName() {
		return Resources.COMP_MEDIA_PAYER;
	}

	public String toString() {
		return getName();
	}

	public Iterator getKeyMnemonics() {
		return remoteCommandMap.keySet().iterator();
	}

	public boolean doAction(String keyCode) {
		//System.err.println("MP:Doing action " + keyCode);
		if (remoteCommandMap.get(keyCode) == null)
			return false;
		actionPerformed(new ActionEvent(remoteCommandMap.get(keyCode), keyCode.hashCode(), keyCode));
		return true;
	}

	public void bringOnTop() {
		requestFocus();
	}

	protected void finalize() throws Throwable {
		if (!closed)
			close();
		super.finalize();
	}

	public void setWindow(Window w) {
		if (window == null)
			window = w;
	}

	public void updateTitle(String title) {
		Object f = getTopLevelAncestor();
		if (f != null && f instanceof Frame)
			((Frame) f).setTitle(title);
		//controller.updateCaption(title);
		controller.updateTrayTitle(title);
	}

	protected void updateSongInf0(MediaFormat media) {
		Object[] attrs = new Object[7];
		System.arraycopy(media.getMediaInfo().getFiveMajorAttributes(), 0, attrs, 0, 5);
		Font font = tp_info.getFont();
		attrs[5] = font.getFamily();
		attrs[6] = font.getSize() < 14 ? "2" : "4";
		tp_info.setText(INFO_FMT.format(attrs));
	}

	protected void pack() {
		Object f = getTopLevelAncestor();
		if (f != null && f instanceof Frame)
			((Frame) f).pack();
		else
			doLayout();
	}
	
	static class ProgressCtrl extends JSlider implements ProgressListener {

		@Override
		public void finished() {
			setValue(0);
		}
		
	}

	/**
	 * Has the player been closed?
	 */
	private volatile boolean closed = false;

	protected boolean autoClose;

	protected ProgressCtrl progress;
	
	public PlaybackRequest activeRequest;

	protected JButton stop, resume, close, skip, advanced;

	protected JEditorPane tp_info;

	protected Window window;

	protected WindowListener windowListener;

	protected JPanel advancedPanel;

	protected Map remoteCommandMap;

	protected int mark;

	private final static boolean __debug = false;
}
