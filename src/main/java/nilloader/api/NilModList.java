package nilloader.api;

import java.util.List;
import java.util.Optional;

import nilloader.NilLoader;

public final class NilModList {

	private NilModList() {}
	
	/**
	 * @return {@code true} if and only if a nilmod is loaded with the given id
	 */
	public static boolean isLoaded(String id) {
		return NilLoader.isModLoaded(id);
	}
	
	/**
	 * @return the metadata for the nilmod with the given id, if present
	 */
	public static Optional<NilMetadata> getById(String id) {
		return NilLoader.getModById(id);
	}
	
	/**
	 * @return a list of all loaded nilmods, in the order they were loaded
	 */
	public static List<NilMetadata> getAll() {
		return NilLoader.getMods();
	}
	
}
