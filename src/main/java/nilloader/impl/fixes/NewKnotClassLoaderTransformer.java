package nilloader.impl.fixes;

import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")
public class NewKnotClassLoaderTransformer extends GenericClassLoaderTransformer {}
