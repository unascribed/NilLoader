package nilloader.impl.log;

public interface NilLogImpl {

	String getImplementationName();
	
	boolean isTraceEnabled();
	boolean isDebugEnabled();
	boolean isInfoEnabled();
	boolean isWarnEnabled();
	boolean isErrorEnabled();

	void trace(String message, Throwable t);
	void debug(String message, Throwable t);
	void info(String message, Throwable t);
	void warn(String message, Throwable t);
	void error(String message, Throwable t);

	NilLogImpl fork(String name);

}