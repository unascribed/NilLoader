package nilloader.impl.fixes;

import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("net.fabricmc.loader.launch.knot.KnotClassLoader")
public class OldKnotClassLoaderTransformer extends GenericClassLoaderTransformer {}
