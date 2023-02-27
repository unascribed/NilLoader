package nilloader.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
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

	private boolean isImplementingInterface(ClassNode clazz, String interfaceName) {
		if (clazz == null || clazz.name.equals("java/lang/Object")) {
			return false;
		}
		for (String interfaces : clazz.interfaces) {
			if (interfaces.equals(interfaceName)) {
				return true;
			} else {
				if (isImplementingInterface(getClassNode(interfaces), interfaceName)) {
					return true;
				}
			}
		}
		if ((clazz.access & Opcodes.ACC_INTERFACE) != 0) {
			return false;
		}
		return isImplementingInterface(getClassNode(clazz.superName), interfaceName);
	}

	private boolean canAssign(ClassNode superType, ClassNode subType) {
		if ((superType.access & Opcodes.ACC_INTERFACE) != 0) {
			return isImplementingInterface(subType, superType.name);
		} else {
			while (subType != null) {
				if (superType.name.equals(subType.name) || superType.name.equals(subType.superName)) {
					return true;
				}
				if (subType.name.equals("java/lang/Object")) {
					return false;
				}
				subType = getClassNode(subType.superName);
			}
		}
		return false;
	}

	@Override
	protected String getCommonSuperClass(String a, String b) {
		if ("java/lang/Object".equals(a) || "java/lang/Object".equals(b)) return "java/lang/Object";
		ClassNode class1 = getClassNode(a);
		ClassNode class2 = getClassNode(b);
		if (class1 == null || class2 == null) {
			return "java/lang/Object";
		}
		if (canAssign(class1, class2)) {
			return class1.name;
		}
		if (canAssign(class2, class1)) {
			return class2.name;
		}
		if (((class1.access | class2.access) & Opcodes.ACC_INTERFACE) != 0) {
			return "java/lang/Object";
		}
		return getCommonSuperClass(a, class2.superName);
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
