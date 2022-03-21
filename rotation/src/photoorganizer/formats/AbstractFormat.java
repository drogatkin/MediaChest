/* PhotoOrganizer - $RCSfile: AbstractFormat.java,v $ 
 * Copyright (C) 2001 Dmitriy Rogatkin.  All rights reserved.
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
 *  Visit http://mediachest.sourceforge.net to get the latest infromation
 *  about Rogatkin's products.
 *  $Id: AbstractFormat.java,v 1.1 2003/09/05 08:24:36 rogatkin Exp $
 */
package photoorganizer.formats;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.net.URL;
import java.awt.Dimension;
import javax.swing.Icon;

public interface AbstractFormat extends Serializable {
   public abstract AbstractInfo getInfo();
   public abstract boolean isValid();
   public abstract String getType();
   public abstract String getName();
   public abstract File getFile();
   public abstract boolean renameTo(File dest);
   public abstract URL getUrl();
   public abstract Icon getThumbnail(Dimension size);
   /**
    * @deprecated 
    * Use getLength()
    * @see getLength()
    */
   public abstract long getFileSize();
   //public abstract long getLength();
   public abstract InputStream getAsStream() throws IOException;
   public abstract byte[] getThumbnailData(Dimension size);
   public abstract String getThumbnailType();
}
