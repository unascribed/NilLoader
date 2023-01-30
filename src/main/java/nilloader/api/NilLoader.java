package nilloader.api;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.Locale;

import nilloader.NilAgentPart;

public class NilLoader {

	/**
	 * Inject the NilLoader agent into the currently running JVM and fire the {@code hijack}
	 * entrypoint. Requires a JDK.
	 */
	public static void hijack() {
		if (NilAgentPart.getInitializations() > 0) {
			// no need to hijack
			return;
		}
		try {
			// bad
			
			String vmName = ManagementFactory.getRuntimeMXBean().getName();
			String pid = vmName.substring(0, vmName.indexOf('@'));
			
			String suffix = "";
			if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
				suffix = "w.exe";
			}
			String tools = System.getProperty("java.home")+"/../lib/tools.jar";
			String classpathSuffix = "";
			if (new File(tools).exists()) {
				// JDK 8, need to add tools.jar to classpath instead of it being a jmod that just exists
				classpathSuffix = File.pathSeparator+tools;
			}
			String ourPath = new File(NilAgentPart.getJarURL(NilLoader.class.getProtectionDomain().getCodeSource().getLocation()).toURI()).getAbsolutePath();
			Process p = new ProcessBuilder(
						System.getProperty("java.home")+File.separator+"bin"+File.separator+"java"+suffix,
						"-cp", ourPath+classpathSuffix,
						"nilloader.impl.Hijacker",
						pid, ourPath)
					.inheritIO()
					.start();
			p.getOutputStream().close();
			while (p.isAlive()) {
				try {
					p.waitFor();
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
}
