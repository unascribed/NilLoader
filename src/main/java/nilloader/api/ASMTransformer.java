package nilloader.api;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * A {@link ClassTransformer} that wants to operate on an ASM ClassNode object rather than raw
 * bytes. This allows some optimizations to be made.
 */
public interface ASMTransformer extends ClassTransformer {

	@Override @Deprecated
	default byte[] transform(ClassLoader loader, String className, byte[] originalData) {
		if (!canTransform(loader, className)) return originalData;
		ClassReader reader = new ClassReader(originalData);
		ClassNode clazz = new ClassNode();
		reader.accept(clazz, 0);
		
		boolean frames = transform(loader, clazz);
		
		int flags = ClassWriter.COMPUTE_MAXS;
		if (frames) {
			flags |= ClassWriter.COMPUTE_FRAMES;
		}
		ClassWriter writer = new NonLoadingClassWriter(loader, flags);
		clazz.accept(writer);
		return writer.toByteArray();
	}
	
	@Override @Deprecated
	default byte[] transform(String className, byte[] originalData) {
		return transform(ClassLoader.getSystemClassLoader(), className, originalData);
	}
	
	/**
	 * @return {@code true} if frames must be recomputed as control flow was modified
	 */
	boolean transform(ClassLoader loader, ClassNode clazz);
	boolean canTransform(ClassLoader loader, String className);
	
}
