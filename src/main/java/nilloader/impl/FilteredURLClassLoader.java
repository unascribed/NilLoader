package nilloader.impl;

import java.net.URL;
import java.net.URLClassLoader;

public class FilteredURLClassLoader extends URLClassLoader {

	static {
		registerAsParallelCapable();
	}
	
	private final String[] filters;
	
	public FilteredURLClassLoader(URL[] urls, String[] filters, ClassLoader parent) {
		super(urls, parent);
		this.filters = filters;
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		for (String s : filters) {
			if (name.startsWith(s)) {
				throw new ClassNotFoundException();
			}
		}
		return super.findClass(name);
	}
	
}
