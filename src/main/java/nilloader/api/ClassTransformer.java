package nilloader.api;

import nilloader.NilLoader;

public interface ClassTransformer {

	static void register(ClassTransformer transformer) {
		NilLoader.registerTransformer(transformer);
	}
	
	byte[] transform(String className, byte[] originalData);
	
}
