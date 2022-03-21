/* PhotoOrganizer - BatchActionWithProgress.java
 * Copyright (C) 1999-2004 Dmitriy Rogatkin.  All rights reserved.                         
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
 *  $Id: BatchActionWithProgress.java,v 1.8 2008/03/16 09:10:23 dmitriy Exp $                
 *  Created on 17.10.2004
 */
package photoorganizer.renderer;

import static photoorganizer.Resources.CMD_CANCEL;
import static photoorganizer.Resources.CTRL_HORIS_INSET;
import static photoorganizer.Resources.CTRL_HORIZ_GAP;
import static photoorganizer.Resources.CTRL_VERT_GAP;
import static photoorganizer.Resources.CTRL_VERT_SIZE;
import static photoorganizer.Resources.LABEL_OPERATION;
import static photoorganizer.Resources.LABEL_TOTAL;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.aldan3.app.ui.FixedGridLayout;

import photoorganizer.Resources;

/**
 * @author Dmitriy
 * 
 */
public abstract class BatchActionWithProgress<T> extends JPanel {
	// TODO consider using SwingWorker
	// TODO consider Visitor pattern
	public static abstract class Action<T> implements Runnable {
		protected T job;

		JProgressBar pb;

		JLabel l;

		public abstract void setCanceled();

		public void setJob(T job) {
			this.job = job;
		}

		protected void setMax(int range, Object display) {
			if (pb != null)
				pb.setMaximum(range);
			if (l != null)
				if (display != null)
					l.setText(display.toString());
				else
					l.setText("");
		}

		protected void tick() {
			pb.setValue(pb.getValue() + 1);
		}

		protected void rollback() {

		}
	}

	public static void doLongTimeOperation(Frame frame, List items, Action action) {
		final JDialog dialog = new JDialog(frame);
		dialog.setContentPane(new BatchActionWithProgress(items, action) {		
			void finish() {
				btn.setAction(new AbstractAction(Resources.CMD_CLOSE) {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dialog.dispose();
					}
					
				});
			}
		});
		dialog.setTitle(Resources.TIILE_TRANSFER_PROGRESS);
		dialog.setDefaultCloseOperation(dialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);
	}

	public BatchActionWithProgress(List<T> items, Action<T> action) {
		setLayout(new FixedGridLayout(5, 5, CTRL_VERT_SIZE, CTRL_VERT_GAP + 5, CTRL_HORIS_INSET, CTRL_HORIZ_GAP));
		add(new JLabel(LABEL_TOTAL), "1,0,0");
		add(pb_total = new JProgressBar(), "0,1,5");
		add(new JLabel(LABEL_OPERATION), "0,2,0");
		add(l_name = new JLabel(), "2,2,0");
		add(pb_current = new JProgressBar(), "0,3,5");
		add(btn = new JButton(CMD_CANCEL), "2,4,0");
		btn.setAction(new AbstractAction(CMD_CANCEL) {
			public void actionPerformed(ActionEvent e) {
					getAction().setCanceled();
					canceled = true;
			}
		});
		setSize(360, 110);
		process(items, action);
	}

	abstract void finish();

	public void process(final List<T> items, final Action<T> action) {
		if (items == null || items.size() == 0)
			return;
		pb_total.setMaximum(items.size());
		action.pb = pb_current;
		action.l = l_name;
		currentAction = action;
		new Thread(new Runnable() {
			public void run() {
				T lastJob = null;
				for (T job : items) {
					if (canceled)
						break;
					pb_current.setValue(0);
					action.setJob(job);
					action.run();
					pb_total.setValue(pb_total.getValue() + 1);
					lastJob = job;
				}
				if (canceled && lastJob != null) {
					for (T job : items) {
						if (job == lastJob)
							break;
						action.setJob(job);
						action.rollback();
					}
				}
				finish();
			}
		}, "long time").start();

	}

	protected Action<T> getAction() {
		return currentAction;
	}

	protected Action<T> currentAction;

	protected boolean canceled;

	protected JProgressBar pb_total, pb_current;

	protected JLabel l_name;
	
	protected JButton btn;
}