package nilloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import nilloader.api.NilLogger;
import nilloader.impl.log.AdHocLogImpl;
import nilloader.impl.log.CommonsLogImpl;
import nilloader.impl.log.JULLogImpl;
import nilloader.impl.log.Log4j1LogImpl;
import nilloader.impl.log.Log4j2LogImpl;
import nilloader.impl.log.NilLogImpl;
import nilloader.impl.log.Slf4jLogImpl;

/**
 * Entrypoint to NilLoader's logging abstraction, which will try to detect and delegate to the best
 * available logging API in the current environment.
 */
public class NilLogManager {
	private static final NilLogImpl IMPL;
	
	static List<Runnable> initLogs = new ArrayList<>();
	
	static {
		/*
		 * The order here is pretty specific. Log4j started being used by vanilla Minecraft in 1.7,
		 * and was used from then to 1.18.2. 1.18.2 redirects Log4j to SLF4j, so its native API is
		 * technically SLF4j, and we'd like to prefer it there, but Nil does not have that level of
		 * information available. However, we cannot prefer SLF4j, as a variety of broken mods on
		 * earlier versions accidentally pull in SLF4j with other libraries they use (e.g. ARRP on
		 * Fabric 1.16) and then don't hook it up; attempting to log over SLF4j in this case will
		 * simply print an error.
		 * 
		 * Prior to 1.7, Minecraft simply does not have a logging system; it just spits things out
		 * to STDOUT. However, Minecraft Forge uses the java.util.logging system (as does LiteLoader,
		 * but it does not play nice with Forge's logger.) We don't want to wind up using JUL if no
		 * one will configure it, as its default output is very ugly (especially interspersed with
		 * other nicer log formats)
		 * 
		 * Due to how JUL is configured, though, a logger that is set up before whoever configures
		 * it (such as us, we're an agent, we load as early as possible) will not adopt the new
		 * appearance. It's very unlikely anyone will want to patch FML's logger system, so we
		 * can poke FML's logger and only then use JUL. The relauncher is only present on old Forge
		 * versions, and it's safe for Forge to later reinitialize it when it loads.
		 * 
		 * For non-Minecraft uses (should those ever occur), we additionally support Log4j 1 and
		 * Apache Commons Logging.
		 * 
		 * If all of that fails, we simply fall back to an "ad-hoc" stdout logger rather than trying
		 * to wake the JUL beast. This is also a nicer default for non-Minecraft usages.
		 */
		if (Boolean.getBoolean("nil.alwaysUseAdHocLogger")) {
			IMPL = new AdHocLogImpl("NilLoader");
		} else {
			NilLogImpl implTmp = null;
			if (classDefined("org.apache.logging.log4j.Logger")) {
				initLogs.add(() -> NilLoader.log.debug("Discovered Log4j 2"));
				implTmp = new Log4j2LogImpl("NilLoader");
			} else if (classDefined("org.slf4j.Logger")) {
				initLogs.add(() -> NilLoader.log.debug("Discovered SLF4j"));
				implTmp = new Slf4jLogImpl("NilLoader");
			} else if (classDefined("cpw.mods.fml.relauncher.FMLRelaunchLog")) {
				initLogs.add(() -> NilLoader.log.debug("Discovered FML Relauncher, attempting to initialize..."));
				try {
					Class<?> log = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog");
					Method configure = log.getDeclaredMethod("configureLogging");
					configure.setAccessible(true);
					try {
						configure.invoke(null);
					} catch (InvocationTargetException e) {
						if (e.getCause() instanceof NullPointerException) {
							/*
							 * FML's logger initializer NPEs if the relauncher hasn't started yet, as it
							 * tries to determine the Minecraft home with a field that's still null.
							 * It's safe to just swallow the NPE and let Forge fix it later, as the NPE
							 * causes the "configured" field to never be set to true, resulting in FML
							 * reinitializing the logger when it tries to use it. configureLogging is
							 * idempotent, so this doesn't break anything - it simply properly
							 * configures the file logger, which is what we wanted anyway.
							 */
						} else {
							throw e.getCause();
						}
					}
					initLogs.add(() -> NilLoader.log.debug("Relauncher log initialized successfully"));
					implTmp = new JULLogImpl("Legacy FML", java.util.logging.Logger.getLogger("ForgeModLoader"), "NilLoader");
				} catch (Throwable t) {
					initLogs.add(() -> NilLoader.log.debug("Unexpected exception when trying to initialize FMLRelaunchLog", t));
				}
			} else if (classDefined("org.apache.log4j.Logger")) {
				initLogs.add(() -> NilLoader.log.debug("Discovered Log4j 1 / Reload4j"));
				implTmp = new Log4j1LogImpl("NilLoader");
			} else if (classDefined("org.apache.commons.logging.Log")) {
				initLogs.add(() -> NilLoader.log.debug("Discovered Commons Logging"));
				implTmp = new CommonsLogImpl("NilLoader");
			}
			if (implTmp == null) {
				initLogs.add(() -> NilLoader.log.debug("Failed to discover other loggers, using ad-hoc"));
				implTmp = new AdHocLogImpl("NilLoader");
			}
			IMPL = implTmp;
		}
	}
	
	private static boolean classDefined(String name) {
		try {
			Class.forName(name, false, ClassLoader.getSystemClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static NilLogger getLogger(String name) {
		return new NilLogger(IMPL.fork(name));
	}
	
}
