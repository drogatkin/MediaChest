/* MediaChest - BaseItem.java
 * Copyright (C) 1999-2005 Dmitriy Rogatkin.  All rights reserved.
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
 *
 *  $Id: BaseItem.java,v 1.7 2007/12/29 09:29:54 dmitriy Exp $
 * Created on Mar 31, 2005
 */

package photoorganizer.ipod;

import java.awt.Dimension;
import java.io.Serializable;

/**
 * @author dmitriy
 * 
 * 
 */
public abstract class BaseItem implements Serializable {
	public static final boolean IPOD_PHOTO = false; // change to true for iPod photo / TODO make it configurable
	
	public static final int STATE_DELETED = 8;

	public static final int STATE_COPIED = 1;

	public static final int STATE_METASYNCED = 2;
		

	public final static Dimension NOWPLAY_SIZE  = IPOD_PHOTO?new Dimension(140, 140):new Dimension(200, 200);

	public final static Dimension THUMBNAIL_SIZE = IPOD_PHOTO?new Dimension(56, 56):new Dimension(100, 100);

	public final static int THUMBNAIL_FILE_LENGTH = THUMBNAIL_SIZE.width * THUMBNAIL_SIZE.height * 2; // short
																										// size

	public final static int NOWPLAY_FILE_LENGTH = NOWPLAY_SIZE.width * NOWPLAY_SIZE.height * 2;

	protected final static int[] IMG_FILE_LENGTHS = { THUMBNAIL_FILE_LENGTH, NOWPLAY_FILE_LENGTH };

	protected final static Dimension[] IMG_DIMS = { THUMBNAIL_SIZE, NOWPLAY_SIZE };

	int id;

	protected int state;

	// abstract String getTypeName(int type);

	abstract int getId();

	abstract Object get(int index);

	abstract void set(int index, int value);

	abstract void set(int index, String value);

	public void setState(int state) {
		this.state |= state;
	}

	public void resetState(int state) {
		//if ((state & STATE_METASYNCED) == STATE_METASYNCED)
		//	new Exception("Reset STATE_METASYNCED").printStackTrace();
		this.state &= ~state;
	}

	public int getState() {
		return state;
	}

	public boolean isState(int mask) {
		return (state & mask) == mask;
	}

}
