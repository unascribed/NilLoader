package nilloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.cadixdev.bombe.asm.analysis.ClassProviderInheritanceProvider;
import org.cadixdev.bombe.asm.jar.ClassLoaderClassProvider;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.asm.LorenzRemapper;
import org.cadixdev.lorenz.model.ClassMapping;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import nilloader.api.ClassTransformer;
import nilloader.api.NilMetadata;
import nilloader.api.lib.mini.MiniTransformer;
import nilloader.api.lib.qdcss.QDCSS;
import nilloader.impl.fixes.RelaunchClassLoaderTransformer;

public class NilLoader {

	private static final boolean DUMP = Boolean.getBoolean("nil.debug.dump");
	private static final boolean DEBUG_CLASSLOADING = Boolean.getBoolean("nil.debug.classLoading");
	
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
	private static final List<ClassTransformer> builtInTransformers = new ArrayList<>();
	private static final List<ClassTransformer> transformers = new ArrayList<>();
	private static final List<File> additionalClassPath = new ArrayList<>();
	private static final Map<File, String> classSources = new LinkedHashMap<>();
	private static final Map<URL, String> classSourceURLs = new LinkedHashMap<>();
	
	private static final Map<String, Map<String, MappingSet>> modMappings = new HashMap<>();
	private static final Map<String, String> activeModMappings = new HashMap<>();
	
	private static ClassFileTransformer loadTracker;
	
	private static Set<String> loadedClasses = new HashSet<>();
	
	private static String activeMod = null;
	private static int initializations = 0;
	private static int nilAgents = 1;
	
	private static boolean frozen = false;
	
	public static void premain(String arg, Instrumentation ins) {
		initializations++;
		if (initializations > 1) {
			NilLoaderLog.log.debug("Initializing for the {} time...", nth(initializations));
			// Multiple nilmods are being added as Java agents; don't do a full reinit
			// We need to delay performing final initialization until the last agent initializes to
			// ensure all the agent jars are on the classpath
			if (initializations == nilAgents) {
				completePremain(ins);
			}
			return;
		}
		if (DEBUG_CLASSLOADING) {
			PrintStream err = System.err;
			ins.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
				err.printf("%s class %s via %s from %s%n", classBeingRedefined == null ? "Loading" : "Redefining", className, loader,
						(protectionDomain != null && protectionDomain.getCodeSource() != null) ? protectionDomain.getCodeSource().getLocation() : "[unknown]");
				return classfileBuffer;
			}, ins.isRetransformClassesSupported());
		}
		loadTracker = (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
			if (loadedClasses != null) loadedClasses.add(className);
			return classfileBuffer;
		};
		ins.addTransformer(loadTracker);
		builtInTransformers.add(new RelaunchClassLoaderTransformer());
		ins.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
			if (classBeingRedefined != null || className == null) return classfileBuffer;
			return NilLoader.transform(builtInTransformers, className, classfileBuffer);
		});
		for (Runnable r : NilLogManager.initLogs) {
			r.run();
		}
		NilLogManager.initLogs.clear();
		try {
			discover("nilloader", NilLoader.class.getClassLoader(), new File(NilLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
		NilLoaderLog.log.info("NilLoader v{} initialized, logging via {}", mods.get("nilloader").version, NilLoaderLog.log.getImplementationName());
		File ourFile = null;
		try {
			ourFile = new File(NilLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			discover(ourFile, false);
		} catch (URISyntaxException | IllegalArgumentException e) {
			NilLoaderLog.log.debug("Failed to discover additional nilmods in our jar", e);
		}
		for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			if (jvmArg.startsWith("-javaagent:")) {
				int equals = jvmArg.indexOf('=');
				String file;
				if (equals != -1) {
					file = jvmArg.substring(11, equals);
				} else {
					file = jvmArg.substring(11);
				}
				File fileObj = new File(file);
				if (ourFile != null && fileObj.getAbsoluteFile().equals(ourFile.getAbsoluteFile())) continue;
				if (fileObj.exists()) {
					if (discover(fileObj, false)) {
						nilAgents++;
					}
				}
			}
		}
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
		if (nilAgents == 1) {
			completePremain(ins);
		} else {
			boolean singular = nilAgents == 2;
			NilLoaderLog.log.debug("Discovered {} other NilLoader agent{}, waiting for {} to initialize before finishing initialization...",
					nilAgents-1, singular ? "" : "s", singular ? "it" : "those");
		}
	}
	
	private static String nth(int n) {
		String prefix = "";
		if (n%100 >= 20) {
			prefix = ""+(n/10);
			n = n%10;
		}
		switch (n) {
			case  1: return prefix+"1st";
			case  2: return prefix+"2nd";
			case  3: return prefix+"3rd";
			default: return prefix+n+"th";
		}
	}

	private static void completePremain(Instrumentation ins) {
		StringBuilder discoveries = new StringBuilder();
		for (NilMetadata meta : mods.values()) {
			discoveries.append("\n\t- ");
			discoveries.append(meta.name);
			discoveries.append(" (");
			discoveries.append(meta.id);
			discoveries.append(") v");
			discoveries.append(meta.version);
		}
		NilLoaderLog.log.info("Discovered {} nilmod{}:{}", mods.size(), mods.size() == 1 ? "" : "s", discoveries);
		for (Map.Entry<File, String> en : classSources.entrySet()) {
			File f = en.getKey();
			try {
				classSourceURLs.put(f.toURI().toURL(), en.getValue());
			} catch (IOException e) {
				NilLoaderLog.log.error("Failed to add {} to class map", f, e);
			}
		}
		for (File f : additionalClassPath) {
			try {
				// This originally passed in a JarFile subclass that filtered its entries, but the
				// JVM doesn't actually call any of the methods on JarFile. We can likely ignore
				// the filtering as these get added with absolute minimum priority to the classpath.
				// TODO: Is there any other way we can filter nilloader/* out of nilmod files?
				ins.appendToSystemClassLoaderSearch(new JarFile(f));
			} catch (IOException e) {
				NilLoaderLog.log.error("Failed to add {} to classpath", f, e);
			}
		}
		ins.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
			if (classBeingRedefined != null || className == null) return classfileBuffer;
			if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
				String definer = getDefiningMod(protectionDomain.getCodeSource().getLocation());
				if (definer != null && isFrozen()) {
					MappingSet mappings = getActiveMappings(definer);
					if (mappings != null) {
						NilLoaderLog.log.debug("Remapping mod class {} via mapping set {}", className, NilLoader.getActiveMappingId(definer));
						ClassProviderInheritanceProvider cpip = new ClassProviderInheritanceProvider(Opcodes.ASM9, new ClassLoaderClassProvider(loader));
						LorenzRemapper lr = new LorenzRemapper(mappings, cpip);
						ClassReader reader = new ClassReader(classfileBuffer);
						ClassWriter writer = new ClassWriter(reader, 0);
						ClassRemapper cr = new ClassRemapper(writer, lr);
						reader.accept(cr, 0);
						classfileBuffer = writer.toByteArray();
					}
				}
			}
			return NilLoader.transform(transformers, className, classfileBuffer);
		});
		fireEntrypoint("premain");
		NilLoaderLog.log.debug("{} class transformer{} registered", transformers.size(), transformers.size() == 1 ? "" : "s");
		frozen = true;
		// clean up stuff we won't be using anymore
		ins.removeTransformer(loadTracker);
		loadedClasses = null;
		for (Map.Entry<String, Map<String, MappingSet>> en : modMappings.entrySet()) {
			String id = getActiveMappingId(en.getKey());
			en.setValue(Collections.singletonMap(id, en.getValue().get(id)));
		}
	}

	private static void discoverDirectory(File dir, String... extensions) {
		NilLoaderLog.log.debug("Searching for nilmods in ./{}", dir.getName());
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
				discover(f, true);
			}
		}
	}
	
	private static boolean discover(File file, boolean addToClassPath) {
		List<NilMetadata> found = new ArrayList<>();
		Map<String, MappingSet> mappings = new HashMap<>();
		try (JarFile jar = new JarFile(file)) {
			Enumeration<JarEntry> iter = jar.entries();
			while (iter.hasMoreElements()) {
				JarEntry en = iter.nextElement();
				String name = en.getName();
				if (name.endsWith(".nilmod.css") && !name.contains("/") && !name.equals("nilloader.nilmod.css")) {
					String id = name.substring(0, name.length()-11);
					NilLoaderLog.log.debug("Discovered nilmod {} in {}", id, file);
					try (InputStream is = jar.getInputStream(en)) {
						QDCSS metaCss = QDCSS.load(file.getName()+"/"+en.getName(), is);
						NilMetadata meta = NilMetadata.from(id, metaCss, file);
						found.add(meta);
					}
				}
				if (name.equals("META-INF/nil/mappings.json")) {
					try (InputStream in = jar.getInputStream(en)) {
						try {
							JsonObject obj = JsonParser.object().from(in);
							for (Map.Entry<String, Object> mapping : obj.entrySet()) {
								if (mapping.getValue() instanceof JsonObject) {
									MappingSet ms = MappingSet.create();
									JsonObject mobj = (JsonObject)mapping.getValue();
									JsonArray classes = mobj.getArray("classes");
									if (classes != null) {
										for (Object clazzo : classes) {
											if (clazzo instanceof JsonObject) {
												parseClassMapping(ms::getOrCreateClassMapping, (JsonObject)clazzo);
											}
										}
									}
									mappings.put(mapping.getKey(), ms);
								}
							}
						} catch (Exception e) {
							NilLoaderLog.log.warn("Failed to parse mappings in {}", file, e);
						}
					}
				}
			}
		} catch (IOException e) {
			NilLoaderLog.log.warn("Failed to discover nilmods in {}", file, e);
		}
		if (!found.isEmpty()) {
			if (addToClassPath) {
				additionalClassPath.add(file);
			}
			for (NilMetadata meta : found) {
				classSources.put(file, meta.id);
				modMappings.put(meta.id, mappings);
				install(meta);
			}
			return true;
		}
		return false;
	}

	private static void parseClassMapping(Function<String, ClassMapping<?, ?>> mappingCreator, JsonObject clazz) {
		ClassMapping<?, ?> cm = mappingCreator.apply(clazz.getString("from"));
		cm.setDeobfuscatedName(clazz.getString("to"));
		JsonObject methods = clazz.getObject("methods");
		if (methods != null) {
			for (Map.Entry<String, Object> method : methods.entrySet()) {
				cm.getOrCreateMethodMapping(MethodSignature.of(method.getKey()))
					.setDeobfuscatedName(MethodSignature.of(method.getValue().toString()).getName());
			}
		}
		JsonObject fields = clazz.getObject("fields");
		if (fields != null) {
			for (Map.Entry<String, Object> field : fields.entrySet()) {
				FieldSignature from = parseFieldSignature(field.getKey());
				FieldSignature to = parseFieldSignature(field.getValue().toString());
				
				cm.getOrCreateFieldMapping(from)
					.setDeobfuscatedName(to.getName());
			}
		}
		JsonArray innerClasses = clazz.getArray("inner-classes");
		if (innerClasses != null) {
			for (Object innerClass : innerClasses) {
				if (innerClass instanceof JsonObject) {
					parseClassMapping(cm::getOrCreateInnerClassMapping, (JsonObject)innerClass);
				}
			}
		}
	}

	private static FieldSignature parseFieldSignature(String val) {
		int colon = val.indexOf(':');
		if (colon >= 0) {
			return FieldSignature.of(val.substring(0, colon), val.substring(colon+1));
		} else {
			return new FieldSignature(val);
		}
	}

	private static void discover(String id, ClassLoader classLoader, File src) {
		try {
			NilLoaderLog.log.debug("Attempting to discover nilmod with ID {} from the classpath", id);
			QDCSS metaCss = QDCSS.load(classLoader.getResource(id+".nilmod.css"));
			NilMetadata meta = NilMetadata.from(id, metaCss, src);
			install(meta);
		} catch (IOException e) {
			NilLoaderLog.log.error("Failed to discover nilmod with ID {} from classpath", id, e);
		}
	}
	
	public static void fireEntrypoint(String entrypoint) {
		if (initializations == 0) {
			NilLoaderLog.log.error("fireEntrypoint called on an uninitialized NilLoader; classloading shenanigans? I was loaded by {}", NilLoader.class.getClassLoader());
			return;
		}
		List<EntrypointListener> listeners = entrypointListeners.get(entrypoint);
		if (listeners == null || listeners.isEmpty()) {
			NilLoaderLog.log.info("Reached entrypoint {}", entrypoint);
		} else {
			NilLoaderLog.log.info("Reached entrypoint {}, informing {} listener{}", entrypoint, listeners.size(), listeners.size() == 1 ? "" : "s");
			for (EntrypointListener l : listeners) {
				if (l.fired) continue;
				l.fired = true;
				String oldActiveMod = activeMod; // in case of recursive entrypoints
				try {
					activeMod = l.id;
					NilLoaderLog.log.debug("Notifying {} of entrypoint {}", l.id, entrypoint);
					Class<?> clazz = Class.forName(l.className);
					Object o = clazz.newInstance();
					if (o instanceof Runnable) {
						((Runnable)o).run();
					} else {
						NilLoaderLog.log.error("Failed to invoke entrypoint {} for nilmod {}: Listener class {} is not an instance of Runnable", entrypoint, l.id, l.className);
					}
				} catch (Throwable t) {
					NilLoaderLog.log.error("Failed to invoke entrypoint {} for nilmod {}", entrypoint, l.id, t);
				} finally {
					activeMod = oldActiveMod;
				}
			}
		}
	}

	private static void install(NilMetadata meta) {
		if (mods.containsKey(meta.id)) {
			NilLoaderLog.log.warn("Loading nilmod with ID {} a second time!", meta.id);
		}
		NilLoaderLog.log.debug("Installing discovered mod {} (ID {}) v{} from {}", meta.name, meta.id, meta.version, meta.source);
		mods.put(meta.id, meta);
	}

	public static byte[] transform(List<ClassTransformer> transformers, String className, byte[] classBytes) {
		byte[] orig = DUMP ? classBytes : null;
		try {
			boolean changed = false;
			for (ClassTransformer ct : transformers) {
				try {
					if ((classBytes = ct.transform(className, classBytes)) != orig) {
						changed = true;
					}
				} catch (Throwable t) {
					NilLoaderLog.log.error("Failed to transform {} via {}", className, ct.getClass().getName(), t);
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
			NilLoaderLog.log.debug("Failed to write before class to {}", f, e);
		}
	}
	
	/**
	 * Only works during entrypoint execution.
	 * @return the id of the currently active nilmod, or null
	 */
	public static String getActiveMod() {
		return activeMod;
	}
	
	public static boolean isFrozen() {
		return frozen;
	}
	
	public static String getActiveMappingId(String mod) {
		return activeModMappings.getOrDefault(mod, "default");
	}
	
	public static MappingSet getActiveMappings(String mod) {
		return modMappings.getOrDefault(mod, Collections.emptyMap()).getOrDefault(getActiveMappingId(mod), null);
	}
	
	public static String getDefiningMod(URL codeSource) {
		String raw = classSourceURLs.get(codeSource);
		if (raw == null && "jar".equals(codeSource.getProtocol())) {
			String str = codeSource.toString().substring(4);
			int bang = str.indexOf('!');
			if (bang != -1) str = str.substring(0, bang);
			try {
				return getDefiningMod(new URL(str));
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return raw;
	}

	public static void registerTransformer(ClassTransformer transformer) {
		if (frozen) throw new IllegalStateException("Transformers must be registered during the premain entrypoint (or earlier)");
		if (transformer instanceof MiniTransformer) {
			MiniTransformer mini = (MiniTransformer)transformer;
			if (loadedClasses.contains(mini.getClassTargetName())) {
				throw new IllegalStateException("Cannot register transformer for already loaded class: "+mini.getClassTargetName());
			}
		}
		transformers.add(transformer);
	}

	public static void setTargetMapping(String mod, String id) {
		if (mod == null) throw new IllegalStateException("Can only call this method during an entrypoint");
		if (frozen) throw new IllegalStateException("Mappings must be set during the premain entrypoint");
		activeModMappings.put(mod, id);
	}

	public static Optional<NilMetadata> getModById(String id) {
		return Optional.ofNullable(mods.get(id));
	}
	
	public static List<NilMetadata> getMods() {
		return new ArrayList<>(mods.values());
	}

	public static boolean isModLoaded(String id) {
		return mods.containsKey(id);
	}
	
}
