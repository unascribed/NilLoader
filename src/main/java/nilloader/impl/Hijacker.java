package nilloader.impl;

public class Hijacker {

	public static void main(String[] args) {
		try {
			Class<?> clazz = Class.forName("com.sun.tools.attach.VirtualMachine");
			Object vm = clazz.getMethod("attach", String.class).invoke(null, args[0]);
			clazz.getMethod("loadAgent", String.class).invoke(vm, args[1]);
			clazz.getMethod("detach").invoke(vm);
		} catch (Throwable t) {
			System.err.println("Failed to hijack");
			t.printStackTrace();
			System.exit(1);
		}
	}

}
