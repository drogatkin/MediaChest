/* MediaChest - CUE
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
 *  $Id: CUE.java,v 1.6 2013/05/04 03:17:37 cvs Exp $
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.sound.sampled.LineListener;

import jwbroek.cuelib.CueSheet;
import jwbroek.cuelib.FileData;
import mediautil.gen.MediaFormat;
import photoorganizer.Resources;
import photoorganizer.media.MediaPlayer;

public class CUE extends SimpleMediaFormat<CUE.CueInfo> {
	public static final String CUE = "CUE";
	public static final String[] EXTENSIONS = { CUE };

	private static final boolean __debug = true;
	static byte[] defaultIconData;
	
	public CUE(File file, String enc) {
		super(file, enc);
	}

	@Override
	String[] getExtensions() {
		return EXTENSIONS;
	}

	@Override
	public String getDescription() {
		return CUE;
	}

	@Override
	CueInfo createMediaInfo(File file) {
		return new CueInfo(file, encoding);
	}
	
	@Override
	public boolean isValid() {
		if (info != null && info.cueSheet != null) {
			List<FileData> fds = info.cueSheet.getFileData();
			if (fds.size() == 1)
			  return new File(info.file.getParentFile(), fds.get(0).getFile()).exists();
		}
		return false;
	}

	@Override
	public MediaPlayer getPlayer() {
		List<FileData> fds = info.cueSheet.getFileData();
		
		if (fds.size() == 1)
			return MediaFormatFactory.getPlayer(MediaFormatFactory.createMediaFormat(new File(info.file.getParentFile(), fds.get(0).getFile()), encoding, true));
		CuePlayer result = new CuePlayer();
		result.init(this);
		return result;
	}

	@Override
	public byte[] getThumbnailData(Dimension size) {
		synchronized (CUE.class) {
			if (defaultIconData == null) {
				defaultIconData = SimpleMediaFormat.getResource(Resources.IMG_CUEICON);
			}
		}
		return defaultIconData;
	}
	class CueInfo extends SimpleMediaInfo {

		protected CueInfo(File f, String e) {
			super(f, e);
		}

		@Override
		boolean cueAware() {
			return true;
		}

		@Override
		protected InputStream getCueStream() throws IOException {
			return new FileInputStream(file);
		}
		
	}
	
	static class CuePlayer implements MediaPlayer {
		MediaPlayer currentPlayer;
		int cuePos;
		CueSheet cueSheet;
		
		@Override
		public void init(MediaFormat mf) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setIntro(int sec) {
			if (currentPlayer != null)
				currentPlayer.setIntro(sec);
		}

		@Override
		public void start() {
			if (currentPlayer != null)
				currentPlayer.start();
		}

		@Override
		public void stop() {
			if (currentPlayer != null)
				currentPlayer.stop();
		}

		@Override
		public void pause() {
			if (currentPlayer != null)
				currentPlayer.stop();
		}

		@Override
		public void resume() {
			if (currentPlayer != null)
				currentPlayer.resume();
			
		}

		@Override
		public void seek(long pos) {
			if (currentPlayer != null)
				currentPlayer.seek(pos);
		}

		@Override
		public Status getStatus() {
			if (currentPlayer != null)
				return currentPlayer.getStatus();
			return null;
		}

		@Override
		public Throwable getLastError() {
			if (currentPlayer != null)
				return currentPlayer.getLastError();
			return null;
		}

		@Override
		public String getLastMessage() {
			if (currentPlayer != null)
				return currentPlayer.getLastMessage();
			return null;
		}

		@Override
		public long getPosition() {
			if (currentPlayer != null)
				return currentPlayer.getPosition();
			return 0;
		}

		@Override
		public void addListener(LineListener ll) {

		}

		@Override
		public void removeListener(LineListener ll) {

		}

		@Override
		public void close() {
			if (currentPlayer != null)
				currentPlayer.close();
		}

		@Override
		public void setProgressListener(ProgressListener pl) {
			if (currentPlayer != null)
				currentPlayer.setProgressListener(pl);
		}

		@Override
		public void waitPlayEnds() {
			if (currentPlayer != null)
				currentPlayer.waitPlayEnds();
		}

		@Override
		public MediaFormat getMedia() {
			return currentPlayer.getMedia();
		}
		
	}

}
