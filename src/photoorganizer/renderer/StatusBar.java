/* MediaChest - $RCSfile: StatusBar.java,v $
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
 *  $Id: StatusBar.java,v 1.8 2007/07/27 02:58:08 rogatkin Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.aldan3.app.ui.FixedGridLayout;

import photoorganizer.Resources;

// TODO: keep copy of information for each requester/tab and redisplay it
// when the requester/tab gains focus
public class StatusBar extends JPanel {

	public StatusBar(photoorganizer.Controller controller) {
		// setBorder(new BevelBorder(BevelBorder.LOWERED));
		setLayout(new BorderLayout());
		add(progress = new JProgressBar(), "West");
		progress.setBorder(BorderFactory.createLoweredBevelBorder());
		progress.setMinimum(0);
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new FixedGridLayout(5, 1, Resources.CTRL_VERT_SIZE, 0, 4, 4));
		infoPanel.add(info = new JLabel(" ", JLabel.CENTER), "0,0,4");
		infoPanel.add(metric = new JLabel(" ", JLabel.RIGHT), "4,0");
		info.setBorder(BorderFactory.createLoweredBevelBorder());
		metric.setBorder(BorderFactory.createLoweredBevelBorder());
		add(infoPanel, "Center");
		// infoPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		label = new SLabel("Powered by Java " + '\u2122'/* PhotoOrganizer.COPYRIGHT */, JLabel.RIGHT);
		// label.
		setCustomStatus(null);
	}

	public void displayInfo(String info) {
		if (info != null)
			this.info.setText(info);
	}

	public void displayMetric(String metric) {
		if (metric != null)
			this.metric.setText(metric);
	}

	public void setCustomStatus(JComponent c) {
		if (c == null) {
			if (custom != label) {
				if (custom != null)
					remove(custom);
				custom = label;
				add(custom, "East");
			}
		} else if (c != custom) {
			if (custom != null)
				remove(custom);
			custom = c;
			add(custom, "East");
		}
	}

	public void clearInfo() {
		info.setText(" ");
	}

	public void clearProgress() {
		progress.setValue(0);
		progress.setIndeterminate(false);
	}

	public void setProgress(int max) {
		if (max < 0)
			progress.setIndeterminate(true);
		else {
			progress.setIndeterminate(false);
			progress.setMaximum(max);
		}
	}

	public void tickProgress() {
		if (!progress.isIndeterminate())
			progress.setValue(progress.getValue() + 1);
	}

	public void flashInfo(String info) {
		flashInfo(info, false);
	}

	public void flashInfo(String info, boolean logCopy) {
		displayInfo(info);
		if (logCopy)
			System.err.println(info);
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(Resources.I_FLASH_DELAY);
				} catch (InterruptedException e) {
				}
				clearInfo();
			}
		}).start();
	}

	protected JProgressBar progress;

	protected JLabel info, label, metric;

	protected JComponent custom;
}
