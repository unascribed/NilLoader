package nilloader.impl.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogImpl implements NilLogImpl {

	private final Logger log;
	
	public Slf4jLogImpl(String name) {
		this.log = LoggerFactory.getLogger(name);
	}
	
	@Override
	public NilLogImpl fork(String name) {
		return new Slf4jLogImpl(name);
	}
	
	@Override
	public String getImplementationName() {
		return "SLF4j";
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return log.isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return log.isErrorEnabled();
	}

	@Override
	public void trace(String message, Throwable t) {
		log.trace(message, t);
	}

	@Override
	public void debug(String message, Throwable t) {
		log.debug(message, t);
	}

	@Override
	public void info(String message, Throwable t) {
		log.info(message, t);
	}

	@Override
	public void warn(String message, Throwable t) {
		log.warn(message, t);
	}

	@Override
	public void error(String message, Throwable t) {
		log.error(message, t);
	}

}
