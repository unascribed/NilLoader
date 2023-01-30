package nilloader;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Some state stuff split out of NilAgent to avoid duplicate class loading with hijacks
 */
public class NilAgentPart {

	static int initializations = 0;

	public static URL getJarURL(URL codeSource) {
		if (codeSource == null) return null;
		if ("jar".equals(codeSource.getProtocol())) {
			String str = codeSource.toString().substring(4);
			int bang = str.indexOf('!');
			if (bang != -1) str = str.substring(0, bang);
			try {
				return new URL(str);
			} catch (MalformedURLException e) {
				return null;
			}
		} else if ("union".equals(codeSource.getProtocol())) {
			// some ModLauncher nonsense
			String str = codeSource.toString().substring(6);
			int bullshit = str.indexOf("%23");
			if (bullshit != -1) str = str.substring(0, bullshit);
			try {
				return new URL("file:"+str);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return codeSource;
	}

	public static int getInitializations() {
		return initializations;
	}

}
