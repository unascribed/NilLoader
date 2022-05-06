package nilloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import nilloader.api.ClassTransformer;
import nilloader.api.NilLogger;
import nilloader.api.NilMetadata;
import nilloader.api.lib.qdcss.QDCSS;
import nilloader.impl.FilteredURLClassLoader;

public class NilLoader {

	public static final NilLogger log = NilLogManager.getLogger("NilLoader");
	
	private static final boolean DUMP = Boolean.getBoolean("nil.debug.dump");
	
	private static final class EntrypointListener {
		public final String id;
		public final String className;
		boolean fired;
		
		public EntrypointListener(String id, String className) {
			this.id = id;
			this.className = className;
		}
	}
	
	private static final Map<String, NilMetadata> mods = new LinkedHashMap<>();
	private static final Map<String, List<EntrypointListener>> entrypointListeners = new HashMap<>();
	private static final List<ClassTransformer> transformers = new ArrayList<>();
	private static final List<URL> classSources = new ArrayList<>();
	
	private static URLClassLoader classLoader;
	
	public static void premain(String arg, Instrumentation ins) {
		for (Runnable r : NilLogManager.initLogs) {
			r.run();
		}
		NilLogManager.initLogs.clear();
		discover("nilloader", NilLoader.class.getClassLoader());
		log.info("NilLoader v{} initialized, logging via {}", mods.get("nilloader").version, log.getImplementationName());
		discoverDirectory(new File("mods"), "jar", "nilmod");
		discoverDirectory(new File("nilmods"), "jar");
		for (NilMetadata meta : mods.values()) {
			for (Map.Entry<String, String> en : meta.entrypoints.entrySet()) {
				if (!entrypointListeners.containsKey(en.getKey())) {
					entrypointListeners.put(en.getKey(), new ArrayList<>());
				}
				entrypointListeners.get(en.getKey()).add(new EntrypointListener(meta.id, en.getValue()));
			}
		}
		StringBuilder discoveries = new StringBuilder();
		for (NilMetadata meta : mods.values()) {
			discoveries.append("\n - ");
			discoveries.append(meta.name);
			discoveries.append(" (");
			discoveries.append(meta.id);
			discoveries.append(") v");
			discoveries.append(meta.version);
		}
		log.info("Discovered {} nilmod{}:"+discoveries, mods.size(), mods.size() == 1 ? "" : "s");
		// we filter nilloader out of the class loader as nilloader is designed to be shadowed, but
		// we don't want to have multiple versions of nilloader on the classpath
		classLoader = new FilteredURLClassLoader(classSources.toArray(new URL[0]), new String[] { "nilloader.", "nilloader/" }, NilLoader.class.getClassLoader());
		ins.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> NilLoader.transform(className, classfileBuffer));
		fireEntrypoint("premain");
	}
	
	public static URLClassLoader getClassLoader() {
		return classLoader;
	}
	
	private static void discoverDirectory(File dir, String... extensions) {
		log.debug("Searching for nilmods in ./{}", dir.getName());
		String[] trailers = new String[extensions.length];
		for (int i = 0; i < extensions.length; i++) {
			trailers[i] = "."+extensions[i];
		}
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			boolean match = false;
			for (String t : trailers) {
				if (f.getName().endsWith(t)) {
					match = true;
					break;
				}
			}
			if (match) {
				discover(f);
			}
		}
	}
	
	private static void discover(File file) {
		List<NilMetadata> found = new ArrayList<>();
		try (JarFile jar = new JarFile(file)) {
			Enumeration<JarEntry> iter = jar.entries();
			while (iter.hasMoreElements()) {
				JarEntry en = iter.nextElement();
				String name = en.getName();
				if (name.endsWith(".nilmod.css") && !name.contains("/")) {
					String id = name.substring(0, name.length()-11);
					log.debug("Discovered nilmod {} in {}", id, file);
					try (InputStream is = jar.getInputStream(en)) {
						QDCSS metaCss = QDCSS.load(file.getName()+"/"+en.getName(), is);
						NilMetadata meta = NilMetadata.from(id, metaCss);
						found.add(meta);
					}
				}
			}
		} catch (IOException e) {
			log.warn("Failed to discover nilmods in {}", file, e);
		}
		if (!found.isEmpty()) {
			try {
				classSources.add(file.toURI().toURL());
				for (NilMetadata meta : found) install(meta);
			} catch (MalformedURLException e) {
				log.warn("Failed to add {} to classpath", file, e);
			}
		}
	}

	private static void discover(String id, ClassLoader classLoader) {
		try {
			log.debug("Attempting to discover nilmod with ID {} from the classpath", id);
			QDCSS metaCss = QDCSS.load(classLoader.getResource(id+".nilmod.css"));
			NilMetadata meta = NilMetadata.from(id, metaCss);
			install(meta);
		} catch (IOException e) {
			log.error("Failed to discover nilmod with ID {} from classpath", id, e);
		}
	}
	
	public static void fireEntrypoint(String entrypoint) {
		List<EntrypointListener> listeners = entrypointListeners.get(entrypoint);
		if (listeners == null || listeners.isEmpty()) {
			log.info("Reached entrypoint {}", entrypoint);
		} else {
			log.info("Reached entrypoint {}, informing {} listener{}", entrypoint, listeners.size(), listeners.size() == 1 ? "" : "s");
			for (EntrypointListener l : listeners) {
				if (l.fired) continue;
				l.fired = true;
				try {
					log.debug("Notifying {} of entrypoint {}", l.id, entrypoint);
					Class<?> clazz = Class.forName(l.className, true, classLoader);
					Object o = clazz.newInstance();
					if (o instanceof Runnable) {
						((Runnable)o).run();
					} else {
						log.error("Failed to invoke entrypoint {} for nilmod {}: Listener class {} is not an instance of Runnable", entrypoint, l.id, l.className);
					}
				} catch (Throwable t) {
					log.error("Failed to invoke entrypoint {} for nilmod {}", entrypoint, l.id, t);
				}
			}
		}
	}

	private static void install(NilMetadata meta) {
		log.debug("Installing discovered mod {} (ID {}) v{}", meta.name, meta.id, meta.version);
		mods.put(meta.id, meta);
	}

	public static byte[] transform(String className, byte[] classBytes) {
		byte[] orig = DUMP ? classBytes : null;
		try {
			boolean changed = false;
			for (ClassTransformer ct : transformers) {
				if (ct.transform(className, classBytes) != classBytes) {
					changed = true;
				}
			}
			if (changed && DUMP) {
				writeDump(className, orig, "before");
				writeDump(className, classBytes, "after");
			}
			return classBytes;
		} catch (RuntimeException t) {
			if (DUMP) writeDump(className, orig, "before");
			throw t;
		} catch (Error e) {
			if (DUMP) writeDump(className, orig, "before");
			throw e;
		}
	}
	
	private static void writeDump(String className, byte[] classBytes, String what) {
		File dir = new File(".nil/debug-out", className.replace('/', '.'));
		dir.mkdirs();
		File f = new File(dir, what+".class");
		try {
			FileOutputStream fos = new FileOutputStream(f);
			try {
				fos.write(classBytes);
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			log.debug("Failed to write before class to {}", f, e);
		}
	}
	
}
