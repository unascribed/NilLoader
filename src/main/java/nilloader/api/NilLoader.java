package nilloader.api;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import nilloader.NilAgent;
import nilloader.impl.Instrument;
import nilloader.impl.JVM;

public class NilLoader {

	/**
	 * Inject the NilLoader agent into the currently running JVM and fire the {@code hijack}
	 * entrypoint.
	 * <p>
	 * <b>In order for this to work, JNA needs to be on the classpath.</b>
	 */
	public static void hijack() {
		if (NilAgent.getInitializations() > 0) {
			// no need to hijack
			return;
		}
		Hijack.perform();
	}

	private static class Hijack {
		public static void perform() {
			try {
				ByteBuffer vmBuf = ByteBuffer.allocateDirect(16); // 16 bytes just in case
				ByteBuffer nVMsBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
				Pointer vmBufPtr = Native.getDirectBufferPointer(vmBuf);
				int err = JVM.INSTANCE.JNI_GetCreatedJavaVMs(vmBufPtr, 1, Native.getDirectBufferPointer(nVMsBuf));
				if (err != 0) throw new RuntimeException("JNI_GetCreatedJavaVMs returned "+err);
				int nVMs = nVMsBuf.getInt();
				if (nVMs != 1) throw new RuntimeException("JNI_GetCreatedJavaVMs didn't return exactly one VM ("+nVMs+")");
				String path = new File(NilAgent.getJarURL(NilLoader.class.getProtectionDomain().getCodeSource().getLocation()).toURI()).getAbsolutePath();
				System.out.println(path);
				err = Instrument.INSTANCE.Agent_OnAttach(vmBufPtr, path, null);
				if (err != 0) throw new RuntimeException("Agent_OnAttach returned "+err);
			} catch (Throwable t) {
				throw new RuntimeException("Cannot hijack", t);
			}
		}
	}
	
}
