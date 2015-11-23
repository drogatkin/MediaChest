import java.applet.Applet;
import java.awt.BorderLayout;

public class checkvmver extends Applet {
	boolean compatible;

	String vmversion;

	public void init() {
		vmversion = System.getProperty("java.version");
		super.init();
	}

	public void start() {
	}

	public String getVMVersion() {
		return vmversion;
	}
}
