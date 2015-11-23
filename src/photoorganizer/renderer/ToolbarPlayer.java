package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mediautil.gen.MediaFormat;
import photoorganizer.Controller;
import photoorganizer.Resources;

public class ToolbarPlayer extends MediaPlayerPanel {

	public ToolbarPlayer(Controller controller) {
		this.controller = controller;
		if (controller != null) {
			controller.add(this, Controller.COMP_EMBEDDED_PLAYER);
			controller.mediaPlayer = this;
		}
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(tf_song_title = new JTextField(""));
		tf_song_title.setEditable(false);
		add(progress = new ProgressCtrl());
		add(resume = new JButton(Controller.getResourceIcon(Resources.IMG_PLAY)));
		resume.addActionListener(this);
		resume.setEnabled(false);
		resume.setToolTipText(Resources.CMD_RESUME);
		resume.setActionCommand(Resources.CMD_RESUME);
		add(skip = new JButton(Controller.getResourceIcon(Resources.IMG_SKIP)));
		skip.setActionCommand(Resources.CMD_SKIP);
		skip.addActionListener(this);
		skip.setEnabled(false);
		skip.setToolTipText(Resources.CMD_SKIP);
		add(stop = new JButton(Controller.getResourceIcon(Resources.IMG_STOP)));
		stop.addActionListener(this);
		stop.setToolTipText(Resources.CMD_STOP);
		stop.setActionCommand(Resources.CMD_STOP);
		JButton b;
		add(b = new JButton(Controller.getResourceIcon(Resources.IMG_ADVANCED)));
		b.setEnabled(false);
		b.setToolTipText(Resources.CMD_ADVANCED);
		b.setActionCommand(Resources.CMD_ADVANCED);
		//add(close = new JButton(Controller.getResourceIcon(Resources.IMG_CLOSE)));
		//close.setToolTipText(Resources.CMD_CLOSE);
		setupProgress();
	}

	@Override
	protected void updateSongInf0(MediaFormat media) {
		Object[] infos = media.getMediaInfo().getFiveMajorAttributes();
		for (int i = 0; i < infos.length; i++)
			infos[i] = Controller.nullToEmpty(infos[i]);
		tf_song_title.setText(String.format("%s %s %s %s %s", infos));
	}

	@Override
	public void bringOnTop() {
		if (f != null)
			f.requestFocus();
	}
	


	@Override
	public void updateTitle(String title) {
		super.updateTitle(title);
		if (f != null)
			f.setTitle(title);
	}

	@Override
	public synchronized WindowListener getWindowListener() {
		
		return new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				f.setVisible(false);
				f.getContentPane().remove(m4Player.getRendererComponent());
				closeMP4();
			}
		};
	}

	@Override
	protected void addVideoScreen(Component videoScreen) {
		if (screenCreated) {
			if (f.isVisible() == false) {
				f.getContentPane().add(videoScreen, BorderLayout.CENTER);
				f.setVisible(true);
			}
			return;
		}
		JPanel videoPanel = new JPanel();
		videoPanel.setLayout(new BorderLayout());
		videoPanel.add(videoScreen, BorderLayout.CENTER);
		videoPanel.validate();
		f = new JFrame();
		f.setContentPane(videoPanel);
		f.pack();
		f.setIconImage(controller.getMainIcon());
		f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		f.addWindowListener(getWindowListener());
		f.setVisible(true);
		f.setSize(360, 280);
		screenCreated = true;
	}

	private JTextField tf_song_title;

	private boolean screenCreated;
	
	private JFrame f;
}
