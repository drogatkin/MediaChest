/* MediaChest - interface Courier
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
 *  $Id: Courier.java,v 1.8 2012/08/05 06:27:48 dmitriy Exp $
 */

package photoorganizer;

import java.io.IOException;

import mediautil.gen.MediaFormat;

public interface Courier {
    // all paths are related to web root path;
    // this convention can be revised in future
	/** Inits courier and prepares it for work doing possible authentications
	 * 
	 */
    public void init() throws IOException;
    /** Tells that all used resources can be freed
     * 
     */
    public void done();
    /** This method used for uploading thumbnail directory page
     * 
     * @param buf
     * @param destPath
     * @param contentType
     * @param encoding
     * @throws IOException
     */
    public void deliver(StringBuffer buf, String destPath, String contentType, String encoding) throws IOException;
    /** This method is used for upload full size image
     * 
     * @param srcPath
     * @param destPath
     * @throws IOException
     */
    public void deliver(String srcPath, String destPath) throws IOException;
    /** This method is used for upload thumbnail
     * 
     * @param format
     * @param destPath
     * @param mask
     * @return
     * @throws IOException
     */
    public String deliver(MediaFormat format, String destPath, String mask) throws IOException;
    public void checkForDestPath(String path) throws IOException;
    /** Returns true for local publishing
     * 
     * @return
     */ 
    public boolean isLocal();
	public boolean isContentIncluded();
	public String getRootPathProperty();
	public String getMediaPathProperty();
	public String getThumbnailsPathProperty();
	public String getMediaUrlProperty();
	public String getTemplatePropertyName();
}