/* PhotoOrganizer - $RCSfile: RemoteOptionsTab.java,v $
 * Copyright (C) 2001-2004 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: RemoteOptionsTab.java,v 1.7 2012/10/18 06:58:59 cvs Exp $
 */
package photoorganizer.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.aldan3.app.ui.FixedGridLayout;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.IrdControllable;
import photoorganizer.Persistancable;
import photoorganizer.Resources;
import photoorganizer.ird.IrdReceiver;

// Currently the implementation supports only one remote
// TODO: extend to support multiple remotes
public class RemoteOptionsTab extends JPanel implements Persistancable
/*
 * , ChangeListener
 */{
	public final static String SECNAME = "RemoteOptions";

	public final static String NAME = "Name";

	// Name=Default,Panasonic,JVC
	// Controllable=MediaPlayer,MediaChest
	// KeyCode.Default.MediaChest.Browse=00ff58a70000
	// KeyCode.Default.MediaChest.Selection=00ff58a90000

	public final static String CONTROLLABLE = "Controllable";

	public final static String KEYCODE = "KeyCode";

	public final static String DEFAULT_REMOTE = "Default";

	static final int MAX_LEARN_TIME = 1000 * 60;

	static final int PATH_COUNT = 3;

	public RemoteOptionsTab(Controller controller) {
		this.controller = controller;
		remoteReceiver = (IrdReceiver) controller.component(Controller.COMP_REMOTERECEIVER);
		assert remoteReceiver != null;
		setLayout(new FixedGridLayout(7, Resources.CTRL_VERT_PREF_SIZE, Resources.CTRL_VERT_SIZE,
				Resources.CTRL_VERT_GAP, Resources.CTRL_HORIS_INSET, Resources.CTRL_HORIZ_GAP));

		add(tf_remoteName = new JTextField(DEFAULT_REMOTE), "3,0,2"); // the
		// order
		// important
		tf_remoteName.setEnabled(false);
		add(new JScrollPane(remoteKeys = new JTree(new RemoteKeysModel())), "0,0,3,7");
		// remoteKeys.setDragEnabled(true);

		add(b_add = new JButton(Resources.CMD_ADD), "5,0");
		b_add.setEnabled(false);
		add(cb_remoteNames = new JComboBox(new String[] { DEFAULT_REMOTE }), "3,1,3");
		cb_remoteNames.setEnabled(false);
		add(b_learn = new JButton(Resources.CMD_LEARN), "4,3");
		b_learn.setEnabled(false);
		b_learn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				learn();
			}
		});
		remoteKeys.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				// race condition

				b_learn.setEnabled(e.getPath() != null && e.getPath().getPathCount() == PATH_COUNT);
			}
		});

	}

	protected void learn() {
		// check selection
		TreePath sp = remoteKeys.getSelectionPath();
		assert sp != null; // change handler insure that
		final Object[] pc = sp.getPath();
		assert pc.length == PATH_COUNT;
		System.err.println("Programming remote " + pc[0] + " for component " + pc[1] + " for key " + pc[2]);
		final Object[] monitor = new String[1];
		JOptionPane learnPane = new JOptionPane("Point remote you are learning to " + "and press a desired button.\n"
				+ "Press Cancel to cancel learning.", JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
				new String[] { Resources.CMD_CANCEL });
		final JDialog dialog = learnPane.createDialog(this, "Learning");

		Thread learnThread = new Thread(new Runnable() {
			public void run() {
				String learnCode = remoteReceiver.learn(monitor);
				Map componentsMap = remoteReceiver.getComponentsMap();
				Map cm = (Map) componentsMap.get(pc[1].toString());
				if (cm == null) {
					cm = new HashMap();
					componentsMap.put(pc[1].toString(), cm);
				}
				if (cm.containsValue(pc[2].toString())) {
					Iterator i = cm.entrySet().iterator();
					ArrayList removedEntries = new ArrayList();
					while (i.hasNext()) {
						Map.Entry e = (Map.Entry) i.next();
						if (pc[2].toString().equals(e.getValue()))
							removedEntries.add(e.getKey());
					}
					i = removedEntries.iterator();
					while (i.hasNext())
						cm.remove(i.next());
				}

				cm.put(learnCode, pc[2].toString());
				System.err.println("Learned " + pc[1] + " to " + learnCode + " for " + pc[2]);
				dialog.dispose();
			}
		}, "LearnDialogThread");
		learnThread.start();
		dialog.show();
		synchronized (monitor) {
			monitor.notify();
		}
		/*
		 * try { learnThread.join(MAX_LEARN_TIME); } catch(InterruptedException
		 * ie) { } dialog.dispose();
		 */
	}

	public static Map loadComponentsMap(IniPrefs ser, IrdReceiver remoteReceiver) {
		Map result = new HashMap();
		Object o = ser.getProperty(SECNAME, CONTROLLABLE);
		Object[] components = null;
		if (o == null)
			return result;
		if (o instanceof Object[])
			components = (Object[]) o;
		else if (o instanceof String)
			components = new Object[] { o };

		if (components != null) {
			Iterator i = remoteReceiver.getRegisteredList().iterator();
			while (i.hasNext()) {
				IrdControllable c = (IrdControllable) i.next();
				Map cm = new HashMap();
				String componentName = c.getName();
				System.err.println("Load component " + componentName);
				result.put(componentName, cm);
				Iterator i2 = c.getKeyMnemonics();
				while (i2.hasNext()) {
					String cmdKey = (String) i2.next();
					String cmd = (String) ser.getProperty(SECNAME, KEYCODE + '.' + DEFAULT_REMOTE + '.' + componentName
							+ '.' + cmdKey);
					if (cmd != null)
						cm.put(cmd, cmdKey);
				}
			}
		} else
			System.err.println("Components of " + o.getClass().getName());
		return result;
	}

	public void load() {

		// IniPrefs s = controller.getPrefs();
		// cb_reuse_player.setSelected(s.getInt(s.getProperty(SECNAME,
		// REUSEPLAYER), 0) == 0);
	}

	public void save() {
		IniPrefs s = controller.getPrefs();
		s.setProperty(SECNAME, NAME, DEFAULT_REMOTE); // will be array prop
		Map componentsMap = remoteReceiver.getComponentsMap();
		assert componentsMap != null;
		ArrayList components = new ArrayList();
		Iterator i = componentsMap.keySet().iterator();
		while (i.hasNext()) {
			String compName = (String) i.next();
			components.add(compName);
			Map cm = (Map) componentsMap.get(compName);
			if (cm == null)
				continue;
			Iterator i2 = cm.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry e = (Map.Entry) i2.next();
				s
						.setProperty(SECNAME, KEYCODE + '.' + DEFAULT_REMOTE + '.' + compName + '.' + e.getValue(), e
								.getKey());
			}
		}
		s.setProperty(SECNAME, CONTROLLABLE, components.toArray());
	}

	public String toString() {
		return tf_remoteName.getText();
	}

	class RemoteKeysModel implements TreeModel {
		EventListenerList listenerList = new EventListenerList();

		public Object getRoot() {
			return RemoteOptionsTab.this;
		}

		public Object getChild(Object parent, int index) {
			if (parent == getRoot()) {
				return remoteReceiver.getRegisteredList().get(index);
			} else if (parent instanceof IrdControllable) {
				int i = 0;
				Object c;
				for (Iterator it = ((IrdControllable) parent).getKeyMnemonics(); it.hasNext(); i++) {
					c = it.next();
					if (index == i)
						return c;
				}
			}
			return null;
		}

		public int getChildCount(Object parent) {
			if (parent == getRoot()) {
				return remoteReceiver.getRegisteredList().size();
			} else if (parent instanceof IrdControllable) {
				int i = 0;
				for (Iterator it = ((IrdControllable) parent).getKeyMnemonics(); it.hasNext(); i++)
					it.next();
				return i;
			}
			return 0;
		}

		public boolean isLeaf(Object node) {
			return node instanceof String;
		}

		public void valueForPathChanged(TreePath path, Object newValue) {
			// update icon for learned
		}

		public int getIndexOfChild(Object parent, Object child) {
			if (parent == getRoot() && child != null) {
				return remoteReceiver.getRegisteredList().indexOf(child);
			} else if (parent != null && parent instanceof IrdControllable) {
				int i = 0;
				for (Iterator it = ((IrdControllable) parent).getKeyMnemonics(); it.hasNext(); i++)
					if (it.next().equals(child))
						return i;
			}
			return -1;
		}

		public void addTreeModelListener(TreeModelListener l) {
			listenerList.add(TreeModelListener.class, l);
		}

		public void removeTreeModelListener(TreeModelListener l) {
			listenerList.remove(TreeModelListener.class, l);
		}
	}

	protected Controller controller;

	protected JTree remoteKeys;

	protected JTextField tf_remoteName;

	protected JComboBox cb_remoteNames;

	protected JButton b_learn, b_add;

	protected IrdReceiver remoteReceiver;
}
