package nilloader.api;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nilloader.api.lib.qdcss.QDCSS;

public class NilMetadata {

	/**
	 * The ID of this nilmod. By convention, should be lowercase alphanumeric only.
	 */
	public final String id;
	/**
	 * The user-readable name of this nilmod.
	 */
	public final String name;
	/**
	 * The user-readable description of this nilmod.
	 */
	public final String description;
	/**
	 * A user-readable description of the authors of this nilmod.
	 */
	public final String authors;
	/**
	 * The user-readable version of this nilmod. NilLoader does not attempt to parse or compare
	 * versions.
	 */
	public final String version;
	/**
	 * A mapping of entrypoint IDs to class names that implement Runnable to invoke when the
	 * entrypoint is reached.
	 */
	public final Map<String, String> entrypoints;
	/**
	 * The file this nilmod was loaded from. Generally points to a JAR, but you should be prepared
	 * for it to point to a directory.
	 */
	public final File source;
	
	
	public NilMetadata(String id, String name, String description,
			String authors, String version, Map<String, String> entrypoints, File source) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.authors = authors;
		this.version = version;
		this.entrypoints = entrypoints;
		this.source = source;
	}


	public static NilMetadata from(String id, QDCSS css, File source) {
		Map<String, String> entrypoints = new HashMap<>();
		for (Map.Entry<String, String> en : css.flatten().entrySet()) {
			if (en.getKey().startsWith("entrypoints.")) {
				entrypoints.put(en.getKey().substring(12), en.getValue());
			}
		}
		return new NilMetadata(
				id,
				css.get("@nilmod.name").orElse(id),
				css.get("@nilmod.description").orElse("No description provided"),
				css.get("@nilmod.authors").orElse("No authorship provided"),
				css.get("@nilmod.version").orElse("?"),
				Collections.unmodifiableMap(entrypoints),
				source
		);
	}
	
}
