package nilloader.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public interface JVM extends Library {

	JVM INSTANCE = Native.load(JVM.class);
	
	@FieldOrder({"reserved0", "reserved1", "reserved2", "reserved3"})
	public class JavaVM extends Structure implements Structure.ByReference {
		public JavaVM() {}
		public JavaVM(Pointer ptr) { super(ptr); }
		
		public Pointer reserved0, reserved1, reserved2, reserved3;
	}
	
	int JNI_GetCreatedJavaVMs(Pointer bufPtr, int bufLen, Pointer nVMs);
	
}
