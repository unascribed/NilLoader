package nilloader.impl.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JULLogImpl implements NilLogImpl {

	private final String implName;
	private final Logger log;
	
	public JULLogImpl(String implName, Logger parent, String name) {
		this.implName = implName;
		log = Logger.getLogger(name);
		log.setParent(parent);
	}
	
	@Override
	public NilLogImpl fork(String name) {
		return new JULLogImpl(implName, log.getParent(), name);
	}

	private void log(Level lvl, String message, Throwable t) {
		if (t == null) {
			log.log(lvl, message);
		} else {
			log.log(lvl, message, t);
		}
	}

	@Override
	public String getImplementationName() {
		return implName;
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isLoggable(Level.FINER);
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isLoggable(Level.FINE);
	}

	@Override
	public boolean isInfoEnabled() {
		return log.isLoggable(Level.INFO);
	}

	@Override
	public boolean isWarnEnabled() {
		return log.isLoggable(Level.WARNING);
	}

	@Override
	public boolean isErrorEnabled() {
		return log.isLoggable(Level.SEVERE);
	}

	@Override
	public void trace(String message, Throwable t) {
		log(Level.FINER, message, t);
	}

	@Override
	public void debug(String message, Throwable t) {
		log(Level.FINE, message, t);
	}

	@Override
	public void info(String message, Throwable t) {
		log(Level.INFO, message, t);
	}

	@Override
	public void warn(String message, Throwable t) {
		log(Level.WARNING, message, t);
	}

	@Override
	public void error(String message, Throwable t) {
		log(Level.SEVERE, message, t);
	}

}
