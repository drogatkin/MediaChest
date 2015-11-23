package photoorganizer.directory;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses of /proc/mounts.
 */
public class MountInfo {
	/**
	 * The mounted device (can be "none" or any arbitrary string for virtual
	 * file systems).
	 */
	public String device;
	/**
	 * The path where the file system is mounted.
	 */
	public String mountpoint;
	/**
	 * The file system.
	 */
	public String fs;
	/**
	 * The mount options. For most file systems, you can use
	 * {@link parseOptions} for easier access.
	 */
	public String options;
	/**
	 * The dumping frequency for dump(8); see fstab(5).
	 */
	public int fs_freq;
	/**
	 * The order in which file system checks are done at reboot time; see
	 * fstab(5).
	 */
	public int fs_passno;

	/**
	 * Parses /proc/mounts and gets the mounts.
	 * 
	 * @return the mounts
	 */
	public static List<MountInfo> getMounts() {
		try {
			InputStreamReader reader = new InputStreamReader(
					new FileInputStream("/proc/mounts"),
					Charset.defaultCharset());
			BufferedReader bufferedReader = new BufferedReader(reader);

			List<MountInfo> mounts = new ArrayList<MountInfo>();

			try {
				String line;
				do {
					line = bufferedReader.readLine();
					if (line == null)
						break;

					String[] parts = line.split(" ");
					if (parts.length < 6)
						continue;
					MountInfo mount = new MountInfo();
					mount.device = parts[0];
					mount.mountpoint = parts[1];
					mount.fs = parts[2];
					mount.options = parts[3];
					mount.fs_freq = Integer.parseInt(parts[4]);
					mount.fs_passno = Integer.parseInt(parts[5]);
					mounts.add(mount);
				} while (true);

				return mounts;
			} finally {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(
					"Unable to open /proc/mounts to get mountpoint info");
		}
	}

	/**
	 * Parses mount options in the form of "key=value,key=value,key,..."; for
	 * example "rw,mode=0664".
	 * 
	 * @param optionsStr
	 *            mount options
	 * @return Mapping between keys and values (with null values for value-less
	 *         keys, e.g. "rw").
	 */
	public static Map<String, String> parseOptions(String optionsStr) {
		Map<String, String> optionsDict = new HashMap<String, String>();
		String[] options = optionsStr.split(",");
		for (String option : options) {
			String[] optionParts = option.split("=", 2);
			if (optionParts.length == 0)
				continue;
			else if (optionParts.length == 1)
				optionsDict.put(optionParts[0], null);
			else
				optionsDict.put(optionParts[0], optionParts[1]);
		}
		return optionsDict;
	}

	/***
	 * Parses the options field.
	 */
	public Map<String, String> parseOptions() {
		return parseOptions(this.options);
	}
}

