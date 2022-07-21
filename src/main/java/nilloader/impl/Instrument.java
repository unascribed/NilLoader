package nilloader.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface Instrument extends Library {

	Instrument INSTANCE = Native.load("instrument", Instrument.class);
	
	int Agent_OnAttach(Pointer vm, String options, Pointer reserved);
	
}
