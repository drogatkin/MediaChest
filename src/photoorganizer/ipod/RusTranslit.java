//  $Id: RusTranslit.java,v 1.2 2003/06/12 02:20:05 rogatkin Exp $
// No copyrights, use everyone as want and where want.
package photoorganizer.ipod;

public class RusTranslit implements Transliteration {
	static final String[] RUS_TRANS_TBL = {
		"A", "B", "V", "G", "D" , "E", "ZH", "Z", "I", "JI", "K", "L", "M", "N", "O", "P",
		"R" , "S", "T", "U", "F", "KH", "TS", "CH", "SH", "SHCH", "'", "Y", "^", "E", "YU", "YA", 
		"a", "b", "v", "g", "d" , "e", "zh", "z", "i", "ji", "k", "l", "m", "n", "o", "p",
		"r" , "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "'", "y", "^", "e", "yu", "ya"
        };
	
	public String translite(String s) {
		if (s == null)
			return null;
		StringBuffer result = new StringBuffer(s.length()+10); // should be *1.2
		char []ca = s.toCharArray();
		for (int i=0; i<ca.length; i++) {
			if (ca[i] <= '\u044f' && ca[i] >= '\u0410')
			result.append(RUS_TRANS_TBL[ca[i] - '\u0410']);
			else if (ca[i] == '\u0401')
				result.append("YO");
			else if (ca[i] == '\u0451')
				result.append("yo");
			else
				result.append(ca[i]);
		}
		return result.toString();
	}
	
	public static void main(String[] args) {
	}
}