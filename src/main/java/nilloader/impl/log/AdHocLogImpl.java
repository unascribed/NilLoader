package nilloader.impl.log;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdHocLogImpl implements NilLogImpl {

	private static final boolean DEBUG = Boolean.getBoolean("nil.debug");
	private static final DateFormat fmt = new SimpleDateFormat("HH:mm:ss");
	
	private final PrintStream out = System.out;
	private final String name;
	
	public AdHocLogImpl(String name) {
		this.name = name;
	}
	
	@Override
	public String getImplementationName() {
		return "System.out";
	}
	
	@Override
	public NilLogImpl fork(String name) {
		return new AdHocLogImpl(name);
	}

	private void log(String tag, String message, Throwable t) {
		if (t != null) {
			t.printStackTrace(out);
		}
		out.println(fmt.format(new Date())+" ["+tag+"] ["+name+"] "+message);
	}

	@Override
	public boolean isTraceEnabled() {
		return DEBUG;
	}

	@Override
	public boolean isDebugEnabled() {
		return DEBUG;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public void trace(String message, Throwable t) {
		log("TRACE", message, t);
	}

	@Override
	public void debug(String message, Throwable t) {
		log("DEBUG", message, t);
	}

	@Override
	public void info(String message, Throwable t) {
		log("INFO", message, t);
	}

	@Override
	public void warn(String message, Throwable t) {
		log("WARN", message, t);
	}

	@Override
	public void error(String message, Throwable t) {
		log("ERROR", message, t);
	}

}
