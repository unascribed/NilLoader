package nilloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.cadixdev.bombe.asm.analysis.ClassProviderInheritanceProvider;
import org.cadixdev.bombe.asm.jar.ClassLoaderClassProvider;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.asm.LorenzRemapper;
import org.cadixdev.lorenz.io.srg.tsrg.TSrgReader;
import org.cadixdev.lorenz.model.ClassMapping;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import nilloader.api.ClassTransformer;
import nilloader.api.NilMetadata;
import nilloader.api.lib.mini.MiniTransformer;
import nilloader.api.lib.qdcss.QDCSS;
import nilloader.impl.fixes.NewKnotClassLoaderTransformer;
import nilloader.impl.fixes.OldKnotClassLoaderTransformer;
import nilloader.impl.fixes.RelaunchClassLoaderTransformer;

public class NilLoader {

	private static final boolean DEBUG_DUMP = Boolean.getBoolean("nil.debug.dump");
	private static final boolean DEBUG_DUMP_MODREMAPPED = Boolean.getBoolean("nil.debug.dump.modRemapped");
	private static final boolean DEBUG_DECOMPILE = Boolean.getBoolean("nil.debug.decompile");
	private static final boolean DEBUG_DECOMPILE_MODREMAPPED = Boolean.getBoolean("nil.debug.decompile.modRemapped");
	private static final boolean DEBUG_DUMP_ALL = Boolean.getBoolean("nil.debug.dump.all");
	private static final boolean DEBUG_FLIP_DIR_LAYOUT = Boolean.getBoolean("nil.debug.dump.flipDirLayout") || Boolean.getBoolean("nil.debug.decompile.flipDirLayout");
	private static final boolean DEBUG_CLASSLOADING = Boolean.getBoolean("nil.debug.classLoading");
	private static final String DEBUG_MAPPINGS_PATH = System.getProperty("nil.debug.mappings");
	
	private static Executor decompilerThread;
	
	private static MappingSet debugMappings = null;
	
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
	private static final Set<File> additionalSearchPath = new LinkedHashSet<>();
	private static final Set<File> additionalClassPath = new LinkedHashSet<>();
	private static final Map<File, String> classSources = new LinkedHashMap<>();
	private static final Map<URL, String> classSourceURLs = new LinkedHashMap<>();
	
	private static final Map<String, Map<String, WidenSet>> modWidens = new HashMap<>();
	private static final Map<String, Map<String, MappingSet>> modMappings = new HashMap<>();
	private static final Map<String, String> activeModMappings = new HashMap<>();
	
	private static WidenSet finalWidens;
	private static final Set<String> widenSubjects = new HashSet<>();
	
	private static ClassFileTransformer loadTracker;
	
	private static Set<String> loadedClasses = new HashSet<>();
	
	private static Instrumentation instrumentation;
	
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
		instrumentation = ins;
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
		
		// TODO it'd be nice to do this in a more generic way instead of needing loader-specific hacks
		builtInTransformers.add(new RelaunchClassLoaderTransformer());
		builtInTransformers.add(new NewKnotClassLoaderTransformer());
		builtInTransformers.add(new OldKnotClassLoaderTransformer());
		
		ins.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
			if (classBeingRedefined != null || className == null) return classfileBuffer;
			return NilLoader.transform(loader, className, classfileBuffer);
		});
		for (Runnable r : NilLogManager.initLogs) {
			r.run();
		}
		NilLogManager.initLogs.clear();
		if (DEBUG_DECOMPILE || DEBUG_DECOMPILE_MODREMAPPED) {
			Decompiler.initialize();
			decompilerThread = Executors.newSingleThreadExecutor(r -> new Thread(r, "NilLoader decompile thread"));
		}
		if (DEBUG_MAPPINGS_PATH != null) {
			try (InputStreamReader r = new InputStreamReader(new FileInputStream(new File(DEBUG_MAPPINGS_PATH)), StandardCharsets.UTF_8)) {
				debugMappings = new TSrgReader(r).read();
			} catch (IOException e) {
				NilLoaderLog.log.error("Failed to load debug mappings", e);
			}
		}
		try {
			discover("nilloader", NilLoader.class.getClassLoader(), new File(NilLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
		NilLoaderLog.log.info("NilLoader v{} initialized, logging via {}", mods.get("nilloader").version, NilLoaderLog.log.getImplementationName());
		File ourFile = null;
		try {
			ourFile = new File(NilLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			additionalClassPath.add(ourFile);
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
		String additional = System.getProperty("nil.discoverPath");
		if (additional != null) {
			for (String path : additional.split(File.pathSeparator)) {
				discoverDirectory(new File(path), "jar");
			}
		}
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
		StringBuilder javaClassPathAddn = new StringBuilder();
		for (Map.Entry<File, String> en : classSources.entrySet()) {
			File f = en.getKey();
			try {
				classSourceURLs.put(f.toURI().toURL(), en.getValue());
			} catch (IOException e) {
				NilLoaderLog.log.error("Failed to add {} to class map", f, e);
			}
		}
		for (File f : additionalSearchPath) {
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
		for (File f : additionalClassPath) {
			javaClassPathAddn.append(File.pathSeparator).append(f.getPath());
		}
		System.setProperty("java.class.path", System.getProperty("java.class.path")+javaClassPathAddn);
		ins.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
			if (classBeingRedefined != null || className == null) return classfileBuffer;
			if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
				String definer = getDefiningMod(protectionDomain.getCodeSource().getLocation());
				if (definer != null && isFrozen()) {
					MappingSet mappings = getActiveMappings(definer);
					if (mappings != null) {
						NilLoaderLog.log.debug("Remapping mod class {} via mapping set {}", className, NilLoader.getActiveMappingId(definer));
						classfileBuffer = remap(loader, classfileBuffer, mappings);
						if (DEBUG_DUMP_MODREMAPPED) {
							writeDump(className, classfileBuffer, "modRemapped", "class");
						}
						if (DEBUG_DECOMPILE_MODREMAPPED) {
							byte[] finalBys = classfileBuffer.clone();
							decompilerThread.execute(() -> {
								writeDump(className, Decompiler.decompile(className, finalBys).getBytes(StandardCharsets.UTF_8), "modRemapped", "java");
							});
						}
					}
				}
			}
			return classfileBuffer;
		});
		fireEntrypoint("premain");
		NilLoaderLog.log.debug("{} class transformer{} registered", transformers.size(), transformers.size() == 1 ? "" : "s");
		frozen = true;
		// clean up stuff we won't be using anymore
		instrumentation = null;
		ins.removeTransformer(loadTracker);
		for (Map.Entry<String, Map<String, MappingSet>> en : modMappings.entrySet()) {
			String id = getActiveMappingId(en.getKey());
			MappingSet ms = en.getValue().get(id);
			if (ms != null) {
				en.setValue(Collections.singletonMap(id, ms));
			} else {
				en.setValue(Collections.emptyMap());
			}
		}
		// bake the widens
		finalWidens = new WidenSet();
		for (Map.Entry<String, Map<String, WidenSet>> en : modWidens.entrySet()) {
			String id = getActiveMappingId(en.getKey());
			WidenSet val = en.getValue().get(id);
			if (val != null) {
				finalWidens.widenClasses.addAll(val.widenClasses);
				for (Map.Entry<String, Set<MethodSignature>> men : val.widenMethods.entrySet()) {
					if (!finalWidens.widenMethods.containsKey(men.getKey())) {
						finalWidens.widenMethods.put(men.getKey(), new HashSet<>());
					}
					finalWidens.widenMethods.get(men.getKey()).addAll(men.getValue());
				}
				for (Map.Entry<String, Set<FieldSignature>> fen : val.widenFields.entrySet()) {
					if (!finalWidens.widenFields.containsKey(fen.getKey())) {
						finalWidens.widenFields.put(fen.getKey(), new HashSet<>());
					}
					finalWidens.widenFields.get(fen.getKey()).addAll(fen.getValue());
				}
			}
		}
		checkWidenLoad(finalWidens.widenClasses);
		checkWidenLoad(finalWidens.widenFields.keySet());
		checkWidenLoad(finalWidens.widenMethods.keySet());
		modWidens.clear();
		loadedClasses = null;
	}

	private static void checkWidenLoad(Set<String> classes) {
		widenSubjects.addAll(classes);
		for (String s : classes) {
			if (loadedClasses.contains(s)) {
				NilLoaderLog.log.warn("Can't widen access of {} as it was loaded prematurely! Expect fireworks!", s);
			}
		}
	}

	private static byte[] remap(ClassLoader loader, byte[] clazz, MappingSet mappings) {
		ClassProviderInheritanceProvider cpip = new ClassProviderInheritanceProvider(Opcodes.ASM9, new ClassLoaderClassProvider(loader == null ? ClassLoader.getSystemClassLoader() : loader));
		LorenzRemapper lr = new LorenzRemapper(mappings, cpip);
		ClassReader reader = new ClassReader(clazz);
		ClassWriter writer = new ClassWriter(reader, 0);
		ClassRemapper cr = new ClassRemapper(writer, lr);
		reader.accept(cr, 0);
		return writer.toByteArray();
	}

	private static void discoverDirectory(File dir, String... extensions) {
		NilLoaderLog.log.debug("Searching for nilmods in {}", dir.getPath());
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
	
	private static boolean discover(File file, boolean addToSearchPath) {
		List<NilMetadata> found = new ArrayList<>();
		Map<String, MappingSet> mappings = new HashMap<>();
		Map<String, WidenSet> widens = new HashMap<>();
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
									JsonObject widen = mobj.getObject("widen");
									if (widen != null) {
										WidenSet ws = new WidenSet();
										JsonArray wclasses = widen.getArray("classes");
										if (wclasses != null) {
											for (Object clazz : wclasses) {
												if (clazz instanceof String) ws.widenClasses.add(clazz.toString());
											}
										}
										JsonArray wmethods = widen.getArray("methods");
										if (wmethods != null) {
											for (Object o : wmethods) {
												if (o instanceof JsonObject) {
													JsonObject m = (JsonObject)o;
													String owner = m.getString("owner");
													if (!ws.widenMethods.containsKey(owner)) {
														ws.widenMethods.put(owner, new HashSet<>());
													}
													ws.widenMethods.get(owner).add(MethodSignature.of(m.getString("sig")));
												}
											}
										}
										JsonArray wfields = widen.getArray("fields");
										if (wfields != null) {
											for (Object o : wfields) {
												if (o instanceof JsonObject) {
													JsonObject m = (JsonObject)o;
													String owner = m.getString("owner");
													if (!ws.widenFields.containsKey(owner)) {
														ws.widenFields.put(owner, new HashSet<>());
													}
													ws.widenFields.get(owner).add(parseFieldSignature(m.getString("sig")));
												}
											}
										}
										widens.put(mapping.getKey(), ws);
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
			if (addToSearchPath) {
				additionalSearchPath.add(file);
			}
			additionalClassPath.add(file);
			for (NilMetadata meta : found) {
				classSources.put(file, meta.id);
				modMappings.put(meta.id, mappings);
				modWidens.put(meta.id, widens);
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
				} catch (ClassNotFoundException e) {
					NilLoaderLog.log.error("Failed to invoke entrypoint {} for nilmod {} as the class {} does not exist", entrypoint, l.id, l.className);
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
	
	private static int makePublic(int access) {
		return (access & ~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}
	
	public static byte[] transform(ClassLoader loader, String className, byte[] classBytes) {
		byte[] orig = DEBUG_DUMP || DEBUG_DECOMPILE ? classBytes : null;
		try {
			boolean changed = false;
			boolean failed = false;
			for (ClassTransformer ct : builtInTransformers) {
				try {
					if ((classBytes = ct.transform(loader, className, classBytes)) != orig) {
						changed = true;
					}
				} catch (Throwable t) {
					NilLoaderLog.log.error("Failed to transform {} via built-in transformer {}", className, ct.getClass().getName(), t);
					failed = true;
				}
			}
			for (ClassTransformer ct : transformers) {
				try {
					if ((classBytes = ct.transform(loader, className, classBytes)) != orig) {
						changed = true;
					}
				} catch (Throwable t) {
					NilLoaderLog.log.error("Failed to transform {} via mod transformer {}", className, ct.getClass().getName(), t);
					failed = true;
				}
			}
			if (widenSubjects.contains(className)) {
				NilLoaderLog.log.debug("Applying widening to {}", className);
				Set<FieldSignature> fields = finalWidens.widenFields.getOrDefault(className, Collections.emptySet());
				Set<MethodSignature> methods = finalWidens.widenMethods.getOrDefault(className, Collections.emptySet());
				ClassReader cr = new ClassReader(classBytes);
				ClassWriter cw = new ClassWriter(cr, 0) {
					@Override
					protected ClassLoader getClassLoader() {
						return loader;
					}
				};
				cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
					@Override
					public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
						if (finalWidens.widenClasses.contains(name)) {
							NilLoaderLog.log.debug("Making class {} public", name);
							access = makePublic(access);
						}
						super.visit(version, access, name, signature, superName, interfaces);
					}
					
					@Override
					public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
						if (methods.contains(MethodSignature.of(name, descriptor))) {
							NilLoaderLog.log.debug("Making method {}.{}{} public", className, name, descriptor);
							access = makePublic(access);
						}
						return super.visitMethod(access, name, descriptor, signature, exceptions);
					}
					
					@Override
					public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
						if (fields.contains(FieldSignature.of(name, descriptor))) {
							NilLoaderLog.log.debug("Making field {}.{}:{} public", className, name, descriptor);
							access = makePublic(access);
						}
						return super.visitField(access, name, descriptor, signature, value);
					}
					
				}, 0);
				changed = true;
				classBytes = cw.toByteArray();
			}
			if (changed) {
				String dumpName = className;
				byte[] before = orig;
				byte[] after = classBytes;
				if (debugMappings != null) {
					try {
						before = remap(loader, before, debugMappings);
						after = remap(loader, after, debugMappings);
						dumpName = debugMappings.computeClassMapping(dumpName).map(ClassMapping::getFullDeobfuscatedName).orElse(className).replace('/', '.');
					} catch (Throwable t) {
						NilLoaderLog.log.error("Failed to remap {}", className, t);
					}
				}
				if (DEBUG_DUMP) {
					writeDump(dumpName, before, "before", "class");
					writeDump(dumpName, after, "after", "class");
				}
				if (DEBUG_DECOMPILE) {
					String fdumpName = dumpName;
					byte[] fbefore = before.clone();
					byte[] fafter = after.clone();
					decompilerThread.execute(() -> {
						writeDump(fdumpName, Decompiler.decompile(fdumpName, fbefore).getBytes(StandardCharsets.UTF_8), "before", "java");
						writeDump(fdumpName, Decompiler.decompile(fdumpName, fafter).getBytes(StandardCharsets.UTF_8), "after", "java");
					});
				}
			} else if (DEBUG_DUMP_ALL || failed) {
				String dumpName = className;
				byte[] bys = classBytes;
				if (debugMappings != null && !className.startsWith("java/") && !className.startsWith("sun/") && !className.startsWith("javax/")) {
					try {
						bys = remap(loader, bys, debugMappings);
						dumpName = debugMappings.computeClassMapping(dumpName).map(ClassMapping::getFullDeobfuscatedName).orElse(className).replace('/', '.');
					} catch (Throwable t) {
						NilLoaderLog.log.error("Failed to remap {}", className, t);
					}
				}
				writeDump(dumpName, bys, failed ? "before" : "unchanged", "class");
			}
			return classBytes;
		} catch (Throwable t) {
			if (DEBUG_DUMP) writeDump(className, orig, "before", "class");
			NilLoaderLog.log.error("Error while transforming {}", className, t);
			return classBytes;
		}
	}
	
	static void writeDump(String className, byte[] classBytes, String what, String ext) {
		String classNameDots = className.replace('/', '.');
		File dir = new File(".nil/debug-out", DEBUG_FLIP_DIR_LAYOUT ? what : classNameDots);
		dir.mkdirs();
		File f = new File(dir, (DEBUG_FLIP_DIR_LAYOUT ? classNameDots : what)+"."+ext);
		try {
			FileOutputStream fos = new FileOutputStream(f);
			try {
				fos.write(classBytes);
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			NilLoaderLog.log.debug("Failed to write {} {} to {}", what, ext, f, e);
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
		if (raw == null) {
			if ("jar".equals(codeSource.getProtocol())) {
				String str = codeSource.toString().substring(4);
				int bang = str.indexOf('!');
				if (bang != -1) str = str.substring(0, bang);
				try {
					return getDefiningMod(new URL(str));
				} catch (MalformedURLException e) {
					return null;
				}
			} else if ("union".equals(codeSource.getProtocol())) {
				// some ModLauncher nonsense
				String str = codeSource.toString().substring(6);
				int bullshit = str.indexOf("%23");
				if (bullshit != -1) str = str.substring(0, bullshit);
				try {
					return getDefiningMod(new URL("file:"+str));
				} catch (MalformedURLException e) {
					return null;
				}
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
	
	static void injectToSearchPath(JarFile file) {
		instrumentation.appendToSystemClassLoaderSearch(file);
	}
	
}
