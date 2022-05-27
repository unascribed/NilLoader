package nilloader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;

class WidenSet {

	public final Set<String> widenClasses = new HashSet<>();
	public final Map<String, Set<MethodSignature>> widenMethods = new HashMap<>();
	public final Map<String, Set<FieldSignature>> widenFields = new HashMap<>();
	
}
