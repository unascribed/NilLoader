package nilloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import nilloader.api.NilLogger;

class Decompiler {

	private interface QuiltflowerAccess {
		String decompile(String name, byte[] clazz);
	}
	
	private static final class QuiltflowerAccessImpl implements QuiltflowerAccess {

		private final NilLogger log = NilLogger.get("Quiltflower");
		
		@Override
		public String decompile(String name, byte[] clazz) {
			Map<String, Object> options = new HashMap<>();
			options.put(IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH, true);
			options.put(IFernflowerPreferences.INCLUDE_JAVA_RUNTIME, true);
			options.put(IFernflowerPreferences.THREADS, 4);
			options.put(IFernflowerPreferences.INDENT_STRING, "\t");
			options.put(IFernflowerPreferences.FINALLY_DEINLINE, true);
			options.put(IFernflowerPreferences.USE_METHOD_PARAMETERS, true);
			options.put(IFernflowerPreferences.USE_DEBUG_VAR_NAMES, true);
			options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, true);
			String[] contentArr = new String[] { "// decompilation failed" };
			Fernflower ff = new Fernflower(new IBytecodeProvider() {
				
				@Override
				public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
					if (externalPath != null && externalPath.endsWith(".nil/bad-fernflower-workaround/this-file-does-not-exist.class")) {
						return clazz;
					}
					return new byte[0];
				}
			}, new IResultSaver() {
				@Override public void saveFolder(String path) {}
				@Override public void saveDirEntry(String path, String archiveName, String entryName) {}
				@Override public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
					contentArr[0] = content;
				}
				@Override public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
					contentArr[0] = content;
				}
				@Override public void createArchive(String path, String archiveName, Manifest manifest) {}
				@Override public void copyFile(String source, String path, String entryName) {}
				@Override public void copyEntry(String source, String path, String archiveName, String entry) {}
				@Override public void closeArchive(String path, String archiveName) {}
			}, options, new IFernflowerLogger() {
				
				@Override
				public void writeMessage(String message, Severity severity, Throwable t) {
					if (severity == Severity.WARN) {
						log.warn(message, t);
					} else if (severity == Severity.ERROR) {
						log.error(message, t);
					}
				}
				
				@Override
				public void writeMessage(String message, Severity severity) {
					if (severity == Severity.WARN) {
						log.warn(message);
					} else if (severity == Severity.ERROR) {
						log.error(message);
					}
				}
				
			});
			try {
				ff.addSource(new File(".nil/bad-fernflower-workaround/this-file-does-not-exist.class"));
				try {
					ff.decompileContext();
				} finally {
					ff.clearContext();
				}
				return contentArr[0];
			} catch (Throwable e) {
				log.error("Failed to decompile", e);
				return "// decompilation failed";
			}
		}
		
	}
	
	private static boolean failure = false;
	private static QuiltflowerAccess access = null;
	
	static void initialize() {
		try {
			File dotNil = new File(".nil");
			File quiltflower = new File(dotNil, "quiltflower.jar");
			if (!quiltflower.exists()) {
				NilLoaderLog.log.info("Downloading Quiltflower...");
				try {
					URL u = new URL("https://maven.quiltmc.org/repository/release/org/quiltmc/quiltflower/1.8.1/quiltflower-1.8.1.jar");
					String expectedHashB64 = "79hnFEabe9+TEz1KefC7gcugcmuCcz3N1Ok/80kolcs=";
					byte[] expectedHash = Base64.getDecoder().decode(expectedHashB64);
					int expectedSize = 874457;
					dotNil.mkdirs();
					File tmp = File.createTempFile("quiltflower", ".jar.part", dotNil);
					byte[] buf = new byte[4096];
					MessageDigest digest = MessageDigest.getInstance("SHA-256");
					int realSize = 0;
					try (InputStream in = u.openStream(); OutputStream out = new FileOutputStream(tmp)) {
						while (true) {
							int read = in.read(buf);
							if (read == -1) break;
							realSize += read;
							if (realSize > expectedSize) throw new IOException("File is longer than expected length "+expectedSize);
							out.write(buf, 0, read);
							digest.update(buf, 0, read);
						}
					}
					byte[] realHash = digest.digest();
					if (!Arrays.equals(expectedHash, realHash)) {
						throw new IOException("File hash is incorrect (got "+Base64.getEncoder().encodeToString(realHash)+", but expected "+expectedHashB64+")");
					}
					tmp.renameTo(quiltflower);
				} catch (IOException | NoSuchAlgorithmException e) {
					NilLoaderLog.log.error("Failed to download Quiltflower", e);
					failure = true;
					return;
				}
			}
			NilLoader.injectToSearchPath(new JarFile(quiltflower));
			access = new QuiltflowerAccessImpl();
		} catch (Throwable t) {
			NilLoaderLog.log.error("Failed to load Quiltflower", t);
			failure = true;
			return;
		}
	}
	
	static String decompile(String name, byte[] bys) {
		try {
			if (failure) return "// decompiler failed to load";
			return access.decompile(name, bys);
		} catch (Throwable t) {
			NilLoaderLog.log.error("Failed to decompile {}", name, t);
			return "// decompilation failed";
		}
	}

}
