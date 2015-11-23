/* MediaChest  $RCSfile: MediaPlayer.java,v $
 * Copyright (C) 1999-2012 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: MediaPlayer.java,v 1.10 2013/06/17 07:20:09 cvs Exp $
 */
package photoorganizer.media;

import javax.sound.sampled.LineListener;

import mediautil.gen.MediaFormat;

public interface MediaPlayer<MF extends MediaFormat> extends AutoCloseable {
	public enum Status {
		playing, stopped, paused, transtioning, inerror, closed, stopping
	};

	void init(MF mf);

	void setIntro(int sec);

	void start();

	void stop();

	void pause();

	void resume();

	void seek(long pos);

	Status getStatus();

	Throwable getLastError();

	String getLastMessage();

	long getPosition();

	void addListener(LineListener ll);

	void removeListener(LineListener ll);
	
	MF getMedia();

	@Override
	void close();

	void setProgressListener(ProgressListener pl);

	void waitPlayEnds();

	public static interface ProgressListener {

		void setMaximum(int m);

		void setValue(int v);
		
		void finished();
	}
}
