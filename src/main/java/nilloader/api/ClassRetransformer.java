package nilloader.api;

import java.util.Set;

public interface ClassRetransformer extends ClassTransformer {
	
	Set<String> getTargets();
	
}
