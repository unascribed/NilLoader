package nilloader.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class NonLoadingClassWriter extends ClassWriter {

	private final Map<String, ClassNode> nodeCache = new HashMap<>();
	
	private final ClassLoader loader;
	
	public NonLoadingClassWriter(ClassLoader loader, int flags) {
		super(flags);
		this.loader = loader;
	}
	
	@Override
	protected ClassLoader getClassLoader() {
		return loader;
	}
	
	@Override
	protected String getCommonSuperClass(String a, String b) {
		if ("java/lang/Object".equals(a) || "java/lang/Object".equals(b)) return "java/lang/Object";
		ClassNode an = getClassNode(a);
		if ((an.access & Opcodes.ACC_INTERFACE) != 0 || an.superName == null) return "java/lang/Object";
		ClassNode bn = getClassNode(b);
		if ((bn.access & Opcodes.ACC_INTERFACE) != 0 || bn.superName == null) return "java/lang/Object";
		if (an.superName.equals(bn.superName)) return an.superName;
		if (an.superName.equals(b)) return b;
		if (bn.superName.equals(a)) return a;
		List<String> aSupers = walkSupers(an);
		List<String> bSupers = walkSupers(bn);
		for (String sup : aSupers) {
			if (bSupers.contains(sup)) return sup;
		}
		return "java/lang/Object";
	}

	private List<String> walkSupers(ClassNode node) {
		List<String> out = new ArrayList<>();
		while (node.superName != null) {
			out.add(node.superName);
			node = getClassNode(node.superName);
		}
		return out;
	}

	private ClassNode getClassNode(String type) {
		ClassNode node = nodeCache.get(type);
		if (node == null) {
			URL url = loader.getResource(type+".class");
			if (url == null) throw new TypeNotPresentException(type, null);
			ClassReader reader;
			try (InputStream in = url.openStream()) {
				reader = new ClassReader(in);
			} catch (IOException e) {
				throw new TypeNotPresentException(type, e);
			}
			node = new ClassNode();
			reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
			nodeCache.put(type, node);
		}
		return node;
	}

}
