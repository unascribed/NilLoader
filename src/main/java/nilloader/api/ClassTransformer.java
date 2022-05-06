package nilloader.api;

public interface ClassTransformer {

	byte[] transform(String className, byte[] originalData);
	
}
