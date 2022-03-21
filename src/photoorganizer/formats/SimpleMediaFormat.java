/* MediaChest - $RCSfile: SimpleMediaFormat.java,v $                          
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
 *  $Id: SimpleMediaFormat.java,v 1.36 2014/10/24 07:27:23 cvs Exp $           
 */
package photoorganizer.formats;

import java.awt.Dimension;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import mediautil.gen.MediaFormat;
import mediautil.image.ImageUtil;

import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.Resources;
import photoorganizer.media.MediaPlayer;

public class SimpleMediaFormat<MI extends SimpleMediaInfo> implements MediaFormat {
	MI info;
	String encoding;

	static protected byte[] defaultIconData;

	private static final boolean __debug = true;

	protected SimpleMediaFormat(File file, String enc) {
		try {
			if (Controller.hasExtension(file, getExtensions()) == false) {
				info = null;
				return;
				// throw new Exception("Wrong extension for MP4 for file
				// "+file);
			}
			encoding = enc;
			if (encoding == null)
				new Exception("Encoding null").printStackTrace();
			info = createMediaInfo(file);
		} catch (Exception e) {
			info = null;
			if (__debug)
				System.err.println(e.toString() + " in probe for WAV in " + file);
		}
	}

	String[] getExtensions() {
		return null;
	}

	MI createMediaInfo(File file) {
		return null;
	}

	String getDefaultTNname() {
		return Resources.IMG_MP3ICON;
	}

	@Override
	public InputStream getAsStream() throws IOException {
		if (info != null)
			if (!Controller.isJdk1_4())
				return getUrl().openStream(); // a caller can use new
			// BufferedInputStream()
			else
				return new FileInputStream(info.file);
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public File getFile() {
		return info.file;
	}

	@Override
	public long getFileSize() {
		return info.file.length();
	}

	@Override
	public String getFormat(int media) {
		return getDescription();
	}

	@Override
	public String toString() {
		if (info == null)
			return super.toString();
		else
			return Arrays.toString(info.getFiveMajorAttributes());
	}

	@Override
	public MI getMediaInfo() {
		return info;
	}

	@Override
	public String getName() {
		if (isValid())
			return info.getName();
		return null;
	}

	@Override
	public Icon getThumbnail(Dimension size) {
		// get it first from info
		byte[] picData = getThumbnailData(null);
		if (picData != null) {
			ImageIcon result = new ImageIcon(picData);
			if (size != null) {
				// System.err.printf("Scaling %s%n", size);
				result.setImage(ImageUtil.getScaled(result.getImage(), size, Image.SCALE_FAST, null));
			}
			return result;
		}
		return null;
	}

	@Override
	public byte[] getThumbnailData(Dimension size) {
		return null;
	}

	@Override
	public String getThumbnailType() {
		return Resources.EXT_JPEG;
	}

	@Override
	public int getType() {
		return MediaFormat.AUDIO;
	}

	@Override
	public URL getUrl() {
		return info.toURL();
	}

	@Override
	public boolean isValid() {
		return info != null;
	}

	@Override
	public boolean renameTo(File newFile) {
		if (info.file.renameTo(newFile))
			try {
				info = createMediaInfo(newFile);
				return true;
			} catch (Exception e) {
			}
		else {
			// TODO provide copy or move in nio.file
		}
		return false;
	}

	public static byte[] getResource(String name) {
		byte[] result = null;
		try (BufferedInputStream bis = new BufferedInputStream( Controller.class.getClassLoader()
					.getResourceAsStream("resource/image/" + name)); ByteArrayOutputStream bos = new ByteArrayOutputStream(8 * 1024)){
			Stream.copyStream(bis, bos);
			result = bos.toByteArray();
		} catch (Exception e) { // io or null ptr
			e.printStackTrace();
			result = new byte[0];
		}
		return result;
	}

	public MediaPlayer getPlayer() {
		return null;
	}

	public static abstract class SimpleMediaPlayer<MF extends MediaFormat> implements MediaPlayer<MF>, Runnable {
		Status status;

		Throwable error;

		String statusMessage;

		int introSecs;

		int positionSecs;

		MF mediaFormat;
		ProgressListener progress;
		SourceDataLine line;
		DataLine.Info info;
		AudioFormat fmt;
		ArrayList<LineListener> listeners;
		InputStream inputStream;

		//boolean terminated;
		private volatile Object latch;

		static Thread playThread;

		@Override
		public synchronized void init(MF mf) {
			mediaFormat = mf;
			listeners = new ArrayList<LineListener>();
		}

		@Override
		public void start() {
			if (playThread != null && playThread.isAlive()) {
				//new Exception("Start play").printStackTrace();
				throw new IllegalStateException("Attempt to start already started player "+playThread+playThread.getState()+Arrays.toString(playThread.getStackTrace()));
				//playThread.interrupt();
			}

			playThread = new Thread(this, getPlayerName());
			playThread.start();
		}

		@Override
		public void stop() {
			pause();
			//System.err.println("Status:"+status);
			if (Status.playing.equals(status) ||  Status.paused.equals(status))
				status = Status.stopping;
			if (playThread != null && playThread.isAlive()) {
				playThread.interrupt();
				//new Exception("INTERRUPT").printStackTrace();
			}
		}

		@Override
		public synchronized void pause() {
			if (latch == null)
				latch = new Object();
		}

		@Override
		public void resume() {
			if (latch != null)
				synchronized (latch) {
					latch.notify();
				}
		}

		@Override
		public void seek(long pos) {

		}

		@Override
		public Status getStatus() {
			return status;
		}
		
		@Override
		public MF getMedia() {
			return mediaFormat;
		}

		@Override
		public long getPosition() {
			// TODO think to take dynamically from line?
			return positionSecs;
		}

		@Override
		public void close() {
			stop();
		}

		@Override
		public final void run() {
			updateProgress(0);
			try {
				if (Status.inerror.equals(status) == false) {
					status = Status.playing; //Status.transtioning;
					playLoop();
				}
			} finally {
				if (Status.inerror.equals(status) == false)
					status = Status.stopped;
				else {
					System.err.printf("Playback problem:%s (%s)%n", statusMessage, error);
					if (error != null)
						error.printStackTrace();
				}
				freeResources();
			}
		}

		@Override
		public void waitPlayEnds() {
			if (playThread != null)
				try {
					playThread.join();
				} catch (InterruptedException e) {

				}
		}

		abstract void playLoop();

		protected void freeResources() {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
				inputStream = null;
			}
			if (line != null) {
				if (listeners != null)
					for (LineListener ll : listeners)
						line.removeLineListener(ll);
				if (line.isRunning())
					line.stop();
				if (line.isOpen())
					try {
						line.close();
					} catch (Exception e) {
						System.err.printf("Line closing exception: %s%n", e);
					}
				line = null;
			}
			if (progress != null)
				progress.finished();
		}

		String getPlayerName() {
			return "Simple media player of " + mediaFormat.getName();
		}

		void updateProgress(int pp) {
			if (progress != null)
				progress.setValue(pp);
		}

		void reportError(String mess, Throwable ex) {
			status = Status.inerror;
			statusMessage = mess;
			error = ex;
		}

		boolean checkPause() {
			updateProgress();
			if (latch != null)
				try {
					if (Status.stopping.equals(status)) {
						if (line != null && line.isActive())
							line.flush();
						return false;
					}
					status = Status.paused;
					synchronized (latch) {
						try {
							latch.wait();
						} catch (InterruptedException e) {
							if (line != null)
								line.flush();
							return false;
						}
					}
				} finally {
					latch = null;
				}
			return true;
		}
		
		protected void updateProgress() {
			// update progress
			if (line != null) {
				positionSecs = (int)(line.getMicrosecondPosition() / 1000000);
				if(progress != null)
					progress.setValue(positionSecs);
			}	
		}

		@Override
		public void addListener(LineListener ll) {
			listeners.add(ll);
		}

		@Override
		public void removeListener(LineListener ll) {
			listeners.remove(ll);
			if (line != null)
				line.removeLineListener(ll);
		}

		@Override
		public void setIntro(int sec) {
			introSecs = sec;
		}

		@Override
		public void setProgressListener(ProgressListener pl) {
			progress = pl;

		}

		@Override
		public Throwable getLastError() {
			if (Status.inerror.equals(status))
				return error;
			throw new IllegalStateException();
		}

		@Override
		public String getLastMessage() {
			return statusMessage;
		}

		SourceDataLine getMixerLine(String mixerName) throws LineUnavailableException {
			Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (Mixer.Info mi : mixers) {
				System.err.printf("Mixer %s - %s%n", mi.getDescription(), mi.getName());
				if (mi.getName().indexOf(mixerName)>= 0) {
					for(Line.Info inf : AudioSystem .getMixer(mi).getSourceLineInfo() ) {
						System.err.printf("Line: %s - %s%n", inf.getClass(), inf.getLineClass());
						Class mc = inf.getClass();
						try {
							Field f = mc.getDeclaredField("hardwareFormats");
							f.setAccessible(true);
							System.err.printf(" Formats: %s%n", Arrays.toString((Object[])f.get(inf)));
						} catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
								
					}
					return (SourceDataLine) AudioSystem .getMixer(mi).getLine(info);
				}	
			}
			throw new IllegalArgumentException("No mixer "+mixerName+" found.");
		}		
		
	}
}
