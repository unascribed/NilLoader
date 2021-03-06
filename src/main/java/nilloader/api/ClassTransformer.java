package nilloader.api;

import nilloader.NilAgent;
import nilloader.api.lib.mini.MiniTransformer;

public interface ClassTransformer {

	/**
	 * Register a class transformer to participate in class loading. Every class loaded by the JVM
	 * (yes, even that one) will be passed into the transformer to give an opportunity to make
	 * changes to it.
	 * @see MiniTransformer
	 */
	static void register(ClassTransformer transformer) {
		NilAgent.registerTransformer(transformer);
	}
	
	default byte[] transform(ClassLoader loader, String className, byte[] originalData) {
		return transform(className, originalData);
	}
	
	byte[] transform(String className, byte[] originalData);
	
}
