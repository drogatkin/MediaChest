/* MediaChest - UiUpdater 
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  $Id: UiUpdater.java,v 1.22 2008/08/28 21:22:21 dmitriy Exp $
 */
package photoorganizer;

import java.awt.Component;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

import javax.swing.Action;

public class UiUpdater extends TimerTask {
	// TODO: convert to enum
	public final static int FILE_SELECTED = 1;

	public final static int SELECTION_SELECTED = 2;

	public final static int DIRECTORY_SELECTED = 3;

	public final static int ALBUM_SELECTED = 3;

	public final static int MEDIA_FILE_SELECTED = 4;

	public final static int GRAPHICS_FILE_SELECTED = 5;

	public final static int IS_SELECTION = 6;

	public final static int RIPPER_NOT_EMPTY = 7;

	public final static int IPOD_CONNECTED = 8;

	public final static int PLAYLIST_SELECTED = 9;

	public final static int PLAYLIST_EXISTS = 10;

	public final static int IPODVIEW_SELECTED = 11;

	public final static int UNDO = 12;

	public final static int IPOD_NOTBUSY = 13;

	public final static int PLAYLIST_MAGIC = 14;

	public final static int SRC_PHOTO_AVAL = 15;

	static int[] EVENTS = { FILE_SELECTED, SELECTION_SELECTED, DIRECTORY_SELECTED, ALBUM_SELECTED, MEDIA_FILE_SELECTED,
			GRAPHICS_FILE_SELECTED, IS_SELECTION, RIPPER_NOT_EMPTY, IPOD_CONNECTED, PLAYLIST_SELECTED, PLAYLIST_EXISTS,
			IPODVIEW_SELECTED, UNDO, IPOD_NOTBUSY, PLAYLIST_MAGIC, SRC_PHOTO_AVAL };

	static final Integer NOT_EMPTY = new Integer(1);

	public static interface StateChecker {
		boolean isEnabled();

		void setEnabled(boolean b);
	}

	public UiUpdater() {
		lines = new Hashtable(EVENTS.length);
		states = new Hashtable(EVENTS.length);
		for (int j = 0; j < EVENTS.length; j++) {
			Integer i;
			lines.put(i = new Integer(EVENTS[j]), new WeakHashMap());
			states.put(i, Boolean.FALSE);
		}
		timer = new Timer("UI udpater timer", true);
		timer.scheduleAtFixedRate(this, new Date(), 1500);
	}

	/**
	 * Registers timer based UI state checker
	 * 
	 * @param ui_event
	 *            event index
	 * @param sc
	 *            checker class
	 * @exception IndexOutOfBoundaries
	 *                This method keeps only one checker per an event so all
	 *                previously set checkers are discarded
	 */
	public void registerScheduledUpdater(int ui_event, StateChecker sc) {
		checkers[ui_event - 1] = sc;
	}

	public void addForNotification(Object component, int ui_event) {
		Map m = (Map) lines.get(new Integer(ui_event));
		if (m == null) {
			System.err.println("Unknown event:" + ui_event + ", skipped");
			return;
		}
		if (m.get(component) != null)
			return;
		m.put(component, NOT_EMPTY);
		// look for this component already is here
		if (component instanceof Component)
			((Component) component).setEnabled(isEnabled(component, ui_event));
		else if (component instanceof Action)
			((Action) component).setEnabled(isEnabled(component, ui_event));
		else
			System.err.println("Unknown element " + component);
	}

	public synchronized void notify(boolean newstate, int ui_event) {
		Integer ie = new Integer(ui_event);
		Map m = (Map) lines.get(ie);
		states.put(ie, newstate ? Boolean.TRUE : Boolean.FALSE);
		if (m == null)
			return;
		Iterator i = m.keySet().iterator();
		while (i.hasNext()) {
			Object element = i.next();
			if (newstate) { // can be enabled only if other states are true
				for (int si = 0; si < EVENTS.length; si++) {
					if (ui_event != EVENTS[si]) {
						Integer cie = new Integer(EVENTS[si]);
						if (states.get(cie) == Boolean.FALSE) {
							Map m2 = (Map) lines.get(cie);
							if (m2 != m && m2.containsKey(element)) {
								newstate = false;
								break;
							}
						}
					}
				}
			}
			if (element instanceof Component)
				((Component) element).setEnabled(newstate);
			else if (element instanceof Action)
				((Action) element).setEnabled(newstate);
			else
				System.err.println("Unknown element " + element);
		}
		StateChecker sc = checkers[ui_event - 1];
		if (sc != null)
			sc.setEnabled(newstate);
	}

	public boolean isEnabled(int ui_event) {
		Boolean result = (Boolean) states.get(new Integer(ui_event));
		if (result != null)
			return result.booleanValue();
		return false;
	}

	public void run() {
		for (int i = 0; i < checkers.length; i++) {
			StateChecker sc = checkers[i]; // to avoid synchronized
			if (sc != null)
				try {
					notify(sc.isEnabled(), i + 1);
				}catch(Throwable t) {
					if (t instanceof ThreadDeath)
						throw (ThreadDeath)t;
					System.err.print(t);
				}
		}
	}

	protected boolean isEnabled(Object component, int ui_event) {
		Boolean result = (Boolean) states.get(new Integer(ui_event));
		for (int si = 0; si < EVENTS.length; si++) {
			if (ui_event != EVENTS[si]) {
				Integer cie = new Integer(EVENTS[si]);
				if (states.get(cie) == Boolean.FALSE) {
					Map m2 = (Map) lines.get(cie);
					if (m2.containsKey(component)) {
						return false;
					}
				}
			}
		}
		if (result != null)
			return result.booleanValue();
		return false;
	}

	public void reset() {
		Enumeration e = lines.elements();
		while (e.hasMoreElements()) {
			Map m = (Map) e.nextElement();
			Iterator i = m.keySet().iterator();
			while (i.hasNext()) {
				Object element = i.next();
				if (element instanceof Component)
					((Component) element).setEnabled(false);
				else if (element instanceof Action)
					((Action) element).setEnabled(false);
				else
					System.err.println("Unknown element " + element);
			}
		}
		for (int j = 0; j < EVENTS.length; j++)
			states.put(new Integer(EVENTS[j]), Boolean.FALSE);
	}

	private Hashtable lines, states;

	private StateChecker[] checkers = new StateChecker[EVENTS.length];

	private Timer timer;
}