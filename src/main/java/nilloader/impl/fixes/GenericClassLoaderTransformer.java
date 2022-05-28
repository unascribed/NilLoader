package nilloader.impl.fixes;

import org.objectweb.asm.tree.LabelNode;

import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

public abstract class GenericClassLoaderTransformer extends EarlyMiniTransformer {

	@Patch.Method("loadClass(Ljava/lang/String;Z)Ljava/lang/Class;")
	@Patch.Method.AffectsControlFlow
	public void patchLoadClass(PatchContext ctx) {
		ctx.jumpToStart();
		LabelNode Lcontinue = new LabelNode();
		ctx.add(
			ALOAD(1),
			LDC("nilloader."),
			INVOKEVIRTUAL("java/lang/String", "startsWith", "(Ljava/lang/String;)Z"),
			IFEQ(Lcontinue),
			ALOAD(1),
			ICONST_0(),
			INVOKESTATIC("java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;"),
			INVOKESTATIC("java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"),
			ARETURN(),
			Lcontinue
		);
	}
	
}
