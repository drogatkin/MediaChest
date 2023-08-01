package photoorganizer.formats;

import java.io.DataInput;
import net.didion.loopy.AccessStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamFactory {
   public InputStream getInputStream(File file) throws IOException {
	   return new FileInputStream(file);
   }
   
   public DataInput getDataInputm(File file) throws IOException {
	   return null;
   }
   
   public AccessStream getRandomAccessStream(File file)throws IOException {
	   return new AccessRandomAccessFile(file, "r");
   }
   
   public static class AccessRandomAccessFile extends RandomAccessFile implements AccessStream {
	   public AccessRandomAccessFile(File file, String mode) throws IOException {
		   super(file, mode);
	   }
   }
}
