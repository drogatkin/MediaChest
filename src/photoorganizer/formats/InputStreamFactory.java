package photoorganizer.formats;

import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamFactory {
   public InputStream getInputStream(File file) throws IOException {
	   return new FileInputStream(file);
   }
   
   public DataInput getDataInputm(File file) {
	   return null;
   }
}
