/* PhotoOrganizer - FtpCommandReply
 *
 * AUTHOR: dragones
 * Revised by Dmitry Rogatkin
 */
package photoorganizer.ftp;

import java.util.Vector;
import java.net.*;

public class FtpCommandReply
{
    public String commandString;
    public int replyCode;
    public Vector replyMessage;
    public Vector replyData;
    public Socket socket;
    public ServerSocket serverSocket;
    
    public FtpCommandReply (String commandString,
        int replyCode, 
        Vector replyMessage, 
        Vector replyData)
    {
        this.commandString = commandString;
        this.replyCode = replyCode;
        this.replyMessage = replyMessage;
        this.replyData = replyData;
    }
    
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Command: ");
        if (commandString != null)
            result.append(commandString);
        result.append('\n');
        result.append("Reply code: ");
        result.append(replyCode);
        result.append('\n');
        result.append("Messages: ");
        if (replyMessage != null) {
            for (int i = 0; i<replyMessage.size(); i++) {
                result.append(""+replyMessage.elementAt(i));
                result.append('\n');
            }
        } else
            result.append('\n');
        result.append("Data: ");
        if (replyData != null) {
            for (int i = 0; i<replyData.size(); i++) {
                result.append(""+replyData.elementAt(i));
                result.append('\n');
            }
        }
        return result.toString();
    }
}
