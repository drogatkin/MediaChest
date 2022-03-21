/* FtpConnectionInfo.java
 * Copyright (C) 1999 Dmitry Rogatkin.  All rights reserved.
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
package photoorganizer.ftp;

import javax.swing.JOptionPane; // shouldn't be here
import javax.swing.JPasswordField;
import photoorganizer.Resources;

public class FtpConnectionInfo {
    public String host;
    public String user;
    private String password;
    public boolean useProxy;
    public boolean askPassword;
    public boolean active;
    public String proxyHost;
    public int    proxyPort;
    public String startDirectory;
    public int    timeout;
    // for good design should be abstract for particular GUI l&F impl
    public String getPassword() {
        if (askPassword) {
            JPasswordField pf;
            JOptionPane.showConfirmDialog(null, pf = new JPasswordField(),
                Resources.LABEL_PASSWORD+':'+user,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            return new String(pf.getPassword());
        } else 
            return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}