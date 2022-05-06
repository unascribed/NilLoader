package nilloader.impl.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4j2LogImpl implements NilLogImpl {

	private final Logger log;
	
	public Log4j2LogImpl(String name) {
		this.log = LogManager.getLogger(name);
	}
	
	@Override
	public NilLogImpl fork(String name) {
		return new Log4j2LogImpl(name);
	}
	
	@Override
	public String getImplementationName() {
		return "Log4j 2";
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
