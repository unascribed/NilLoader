package nilloader.api;

import java.util.NoSuchElementException;

import nilloader.NilLoader;

public final class ModRemapper {

	private ModRemapper() {}
	
	/**
	 * Configure the target mappings to use for the currently executing mod. This can be set to the
	 * ID of any target mappings declared in your build.gradle. Check META-INF/nil/mappings.json in
	 * your built JAR to see the available IDs.
	 * <p>
	 * You may pass {@code null} to disable remapping for your mod. Otherwise, the mappings with ID
	 * "default" will be applied.
	 * @param id the mappings to apply
	 * @throws NoSuchElementException if there are no mappings available with the given id
	 */
	public static void setTargetMapping(String id) {
		NilLoader.setTargetMapping(NilLoader.getActiveMod(), id);
	}
	
}
