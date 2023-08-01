/* PhotoOrganizer - $RCSfile: MediaFormatFactory.java,v $ 
 * Copyright (C) 2001-2005 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: MediaFormatFactory.java,v 1.25 2014/05/21 02:57:56 cvs Exp $
 */

package photoorganizer.formats;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import photoorganizer.media.MediaPlayer;
import mediautil.gen.MediaFormat;
import mediautil.image.jpeg.BasicJpeg;

public class MediaFormatFactory { 
	public static MediaFormat createMediaFormat(File file) {
		//new Exception("called w/o enc").printStackTrace();
		return createMediaFormat(file, null, false);
	}
	
	static final String[] FORMATS = {"MP3", "MP4", "WMA", "FLAC", "OGG", "APE", "WavPack", "M4A", "CUE", "DSD" };
	
	private static InputStreamFactory streamFactory = new InputStreamFactory();;

	public static MediaFormat createMediaFormat(File file, String encoding, boolean skipImage) {
		// TODO: some optimization based on extension and first check can be done here
		// TODO: possible creation from URL location
		// TODO formats handler should be read from config
		MediaFormat result = null;
		for (String mfmt : FORMATS) {
			try {
				result = (MediaFormat) Class.forName("photoorganizer.formats." + mfmt)
						.getConstructor(File.class, String.class).newInstance(file, encoding);
				if (result.isValid())
					break;
				result = null;
			} catch (Exception e) {
				System.err.printf("A problem in instatiation %s%n", e); e.printStackTrace();
			}
		}
		if (result == null && skipImage == false) {
			result = new BasicJpeg(file, encoding);
			if (result.isValid() == false)
				result = null;
		}
		return result;
	}
	
	public static MediaPlayer getPlayer(MediaFormat mf) {
		if (mf instanceof SimpleMediaFormat)
			return ((SimpleMediaFormat)mf).getPlayer();
                if (mf == null)
                    return null;
		try {
			return (MediaPlayer) mf.getClass().getMethod("getPlayer").invoke(mf);
		} catch(InvocationTargetException ite) {
			System.err.printf("Exception at instantiation of a player: %s%n", ite.getTargetException());
		} catch (Exception e) {
			System.err.printf("Exception at resolving player: %s for %s%n", e, mf);
		} 
		return null;
	}
	
	public static InputStreamFactory getInputStreamFactory() {
		return streamFactory;
	}
	
	public static synchronized void setInputStreamFactory(InputStreamFactory factory) {
		streamFactory = factory;
	}
}
