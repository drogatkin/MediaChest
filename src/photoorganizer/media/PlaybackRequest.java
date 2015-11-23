/* PhotoOrganizer - $RCSfile: PlaybackRequest.java,v $                           
 * Copyright (C) 2001-2003 Dmitriy Rogatkin.  All rights reserved.                    
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
 *  $Id: PlaybackRequest.java,v 1.20 2013/03/01 00:02:51 cvs Exp $            
 */

package photoorganizer.media;

import java.io.File;
import java.io.IOException;

import javax.swing.ListModel;

import mediautil.gen.MediaFormat;

import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.formats.MediaFormatFactory;
import photoorganizer.formats.Thumbnail;
import photoorganizer.ipod.PlayItem;
import photoorganizer.renderer.IpodOptionsTab;
import photoorganizer.renderer.MediaOptionsTab;
import photoorganizer.renderer.MediaPlayerPanel;
import photoorganizer.renderer.PlaybackProperties;

// MediaController.java
public class PlaybackRequest {
	public Object[] playbackList;

	public boolean recursive;

	public boolean requestProperties;

	public boolean requestOnCopy;

	public boolean shuffled;

	public MediaPlayerPanel player;

	public int pauseBetween;

	public int introFrames;

	public ContentMatcher matcher;
	
	private boolean closed;

	// used to keep position on playing list which can expand or shrink
	public ListModel playListModel;

	public PlaybackRequest() {
	}

	public PlaybackRequest(Object[] list, boolean recursive) {
		this(list, null, recursive);
	}

	public PlaybackRequest(Object[] list) {
		this(list, null);
	}

	public PlaybackRequest(Object[] list, IniPrefs serializer, boolean recursive) {
		this(list, serializer);
		this.recursive = recursive;
	}

	public PlaybackRequest(Object[] list, IniPrefs serializer) {
		this(serializer);
		playbackList = list;
	}

	public PlaybackRequest(ListModel list, IniPrefs serializer) {
		this(serializer);
		playListModel = list;
	}

	public PlaybackRequest(IniPrefs serializer) {
		if (serializer != null)
			initAttributes(serializer);
	}

	protected void initAttributes(IniPrefs serializer) {
		setRecursive(serializer);
		setIntoFrames(serializer);
		setAskProperties(serializer);
		setAskOnCopy(serializer);
		setPauseBetween(serializer);
	}

	protected void setRecursive(IniPrefs ser) {
		recursive = IniPrefs.getInt(ser.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.RECURSIVE_PLAYBACK), 0) == 1;
	}

	protected void setIntoFrames(IniPrefs ser) {
		introFrames = IniPrefs.getInt(ser.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.INTRO_FRAMES), 0);
	}

	protected void setAskProperties(IniPrefs ser) {
		requestProperties = ser.getInt(ser.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.REQUEST_PLAYMODE), 0) == 1;
	}

	protected void setAskOnCopy(IniPrefs ser) {
		requestOnCopy = ser.getInt(ser.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.REQUEST_COPYFILTER), 0) == 1;
	}

	protected void setPauseBetween(IniPrefs ser) {
		pauseBetween = ser.getInt(ser.getProperty(MediaOptionsTab.SECNAME, MediaOptionsTab.SONGS_PAUSE), 0) * 1000;
	}

	public void playList(final Controller controller) {
		initAttributes(controller.getPrefs());
		if (requestProperties
				&& (playbackList.length > 1 || (playbackList.length == 1 && playbackList[0] instanceof File && ((File) playbackList[0])
						.isDirectory()))) {
			new Thread(new Runnable() {
				public void run() {
					PlaybackProperties.doModal(controller, PlaybackRequest.this);
					if (matcher != null)
						controller.playMediaList(PlaybackRequest.this);
				}
			}, "PlayList").start();
		} else {
			introFrames = 0;
			controller.playMediaList(this);
		}
	}

	public Object[] buildList(Controller controller) {
		initAttributes(controller.getPrefs());
		if (requestProperties
				&& (playbackList.length > 1 || (playbackList.length == 1 && playbackList[0] instanceof File && ((File) playbackList[0])
						.isDirectory()))) {
			PlaybackProperties pbProps = PlaybackProperties.doPropertiesDialog(controller, (java.awt.Frame) null);
		}
		return playbackList;
	}

	// TODO: add call back for update selection to currently played
	public void playList(final int pos, final Controller controller) {
		if (playListModel == null)
			return;
		// TODO use playback from Controller
		new Thread(new Runnable() {
			public void run() {
				for (int k = pos; k < playListModel.getSize(); k++) {
					// TODO: use common interface to avoid type check
					Object playObject = playListModel.getElementAt(k);
					MediaFormat af;
					if (playObject instanceof Thumbnail)
						af = (MediaFormat) ((Thumbnail) playObject).getFormat();
					else if (playObject instanceof PlayItem) {
						if (((PlayItem) playObject).getAttachedFormat() == null) {
							IniPrefs s = controller.getPrefs();
							String dev = IpodOptionsTab.getDevice(controller);
							af = MediaFormatFactory.createMediaFormat(((PlayItem) playObject).getFile(dev));
							if (af == null)
								System.err.println("File " + ((PlayItem) playObject).getFile(dev)
										+ " not found or unknown format.");
						} else
							af = ((PlayItem) playObject).getAttachedFormat();
					} else
						continue;
					if (af == null)
						continue;
					try {
						player = controller.playMedia(af, introFrames);
						player.waitForCompletion();
						controller.updateTrayTitle("");
						if (player.isClosed())
							break;
						if (pauseBetween > 999)
							try {
								Thread.sleep(pauseBetween);
							} catch (InterruptedException ie) {
							}
					} catch (IOException ioe) {
						System.err.println("Problem " + ioe + " for " + af);
					}
				}
			}
		}, "Model Play List").start();
	}
	
	public void close() {
		closed = true;
		playListModel = null;
	}
	
	public boolean isClosed() {
		return closed;
	}
}
