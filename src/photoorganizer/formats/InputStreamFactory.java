package photoorganizer.formats;

import java.io.DataInput;
import net.didion.loopy.AccessStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.justcodecs.dsd.DSDStream;
import org.justcodecs.dsd.Utils;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamFactory {
   public InputStream getInputStream(File file) throws IOException {
	   return new FileInputStream(file);
   }
   
   public DataInput getDataInput(File file) throws IOException {
	   return null;
   }
   
   public AccessStream getRandomAccessStream(File file)throws IOException {
	   return new AccessRandomAccessFile(file, "r");
   }
   
   public DSDStream getDSDStream(File file) throws IOException {
	   return new Utils.RandomDSDStream(file);
   }
   
   public davaguine.jmac.tools.File createApeFile(File file) throws IOException {
	   return davaguine.jmac.tools.File.createFile(file.getPath(), "r") ;
   }
   
   public FileChannel getInputChannel(File file) throws IOException {
	   return new FileInputStream(file).getChannel();
   }
   
   public static class AccessRandomAccessFile extends RandomAccessFile implements AccessStream {
	   public AccessRandomAccessFile(File file, String mode) throws IOException {
		   super(file, mode);
	   }
   }
}
