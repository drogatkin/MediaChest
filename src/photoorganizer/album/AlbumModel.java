/* AlbumModel.java 
 * Copyright (C) 1999 Dmitriy Rogatkin.  All rights reserved.
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
 */
package photoorganizer.album;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import photoorganizer.Resources;

public class AlbumModel implements TreeModel {
	public Access access;

	EventListenerList listenerList = new EventListenerList();

	public AlbumModel(Access access) {
		this.access = access;
	}

	public Object getChild(Object parent, int index) {
		if (parent instanceof AlbumNode)
			return ((AlbumNode) parent).getChild(index);
		return new AlbumNode(-1);
	}

	public Object getRoot() {
		return new AlbumNode(0);
	}

	public int getChildCount(Object parent) {
		if (parent instanceof AlbumNode)
			return ((AlbumNode) parent).getChildCount();
		return 0;
	}

	public boolean isLeaf(Object node) {
		if (node instanceof AlbumNode)
			return ((AlbumNode) node).getChildCount() == 0;
		return true;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		if (access.renameAlbumTo(access.getAlbumId(path), (String) newValue)) {
			fireTreeNodesChanged(new Object[] { path });
			try {
				((AlbumNode) path.getLastPathComponent()).update();
			} catch (ClassCastException e) {
			}
		}
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof AlbumNode)
			return ((AlbumNode) parent).getIndexOfChild(child);
		return -1;
	}

	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeModelListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TreeModelEvent(source, path, childIndices, children);
				((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
			}
		}
	}

	public void fireTreeNodesChanged(Object[] path) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeModelListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TreeModelEvent(this, path, null, null);
				((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
			}
		}
	}

	class AlbumNode {
		public AlbumNode(int group_id) {
			this.group_id = group_id;
			update();
		}

		public void update() {
			if (group_id == 0)
				name = Resources.LABEL_ALBUMROOT;
			else
				name = access.getNameOfAlbum(group_id);
		}

		public int getChildCount() {
			return access.getAlbumsCount(group_id);
		}

		public Object getChild(int index) {
			child_ids = access.getAlbumsId(group_id);
			try {
				return new AlbumNode(child_ids[index]);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("Attempt to reach non existing album " + e);
			}
			return null;
		}

		public String toString() {
			return name;
		}

		public int getIndexOfChild(Object child) {
			// if (child instanceof AlbumNode) trusted
			child_ids = access.getAlbumsId(group_id);
			for (int i = 0; i < child_ids.length; i++)
				if (((AlbumNode) child).group_id == child_ids[i])
					return i;
			return -1;
		}

		String name;

		int group_id;

		int[] child_ids;
	}
}