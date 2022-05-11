package nilloader.impl.fixes;

import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("cpw.mods.fml.relauncher.RelaunchClassLoader")
public class RelaunchClassLoaderTransformer extends EarlyMiniTransformer {

	@Patch.Method("<init>([Ljava/net/URL;)V")
	public void patchInitialize(PatchContext ctx) {
		// Avoids creation of ghost NilLoader instances when entrypoints are invoked inside the relaunch class loader
		
		ctx.jumpToLastReturn();
		ctx.add(
			ALOAD(0),
			LDC("nilloader."),
			INVOKESPECIAL("cpw/mods/fml/relauncher/RelaunchClassLoader", "addClassLoaderExclusion", "(Ljava/lang/String;)V")
		);
	}

}
