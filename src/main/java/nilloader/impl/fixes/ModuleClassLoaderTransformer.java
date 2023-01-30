package nilloader.impl.fixes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.cadixdev.bombe.util.ByteStreams;
import org.objectweb.asm.tree.LabelNode;

import nilloader.api.NilModList;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("cpw.mods.cl.ModuleClassLoader")
public class ModuleClassLoaderTransformer extends EarlyMiniTransformer {

	@Patch.Method("getClassBytes(Ljava/lang/module/ModuleReader;Ljava/lang/module/ModuleReference;Ljava/lang/String;)[B")
	@Patch.Method.AffectsControlFlow
	public void patchGetClassBytes(PatchContext ctx) {
		ctx.jumpToStart();
		LabelNode Lcontinue = new LabelNode();
		ctx.add(
			ALOAD(0),
			ALOAD(3),
			INVOKESTATIC("nilloader/impl/fixes/ModuleClassLoaderTransformer$Hooks", "synthesizeClass", "(Ljava/lang/Object;Ljava/lang/String;)[B"),
			DUP(),
			IFNULL(Lcontinue),
			ARETURN(),
			Lcontinue,
			POP()
		);
	}
	
	public static class Hooks {
		
		private static final List<ZipFile> nilmods = NilModList.getAll().stream().map(m -> {
			try {
				return new ZipFile(m.source);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).collect(Collectors.toList());
		
		public static byte[] synthesizeClass(Object loader, String name) {
			if (loader.getClass().getName().equals("cpw.mods.modlauncher.TransformingClassLoader")) {
				// game loader - we don't care about bootstrap/etc
				String path = name.replace('.', '/')+".class";
				for (ZipFile zf : nilmods) {
					ZipEntry en = zf.getEntry(path);
					if (en != null) {
						System.out.println("I found "+path+" in "+zf.getName());
						try (InputStream is = zf.getInputStream(en)) {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ByteStreams.copy(is, baos);
							return baos.toByteArray();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				}
			}
			return null;
		}
		
	}
	
}
