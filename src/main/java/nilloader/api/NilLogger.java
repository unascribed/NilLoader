package nilloader.api;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nilloader.NilLogManager;
import nilloader.impl.log.NilLogImpl;

public class NilLogger {
	
	private static final Pattern BRACES_PATTERN = Pattern.compile("{}", Pattern.LITERAL);

	private final NilLogImpl impl;
	
	public NilLogger(NilLogImpl impl) {
		this.impl = impl;
	}
	
	public static NilLogger get(String name) {
		return NilLogManager.getLogger(name);
	}
	
	private static void log(BiConsumer<String, Throwable> impl, String message, Object... params) {
		StringBuffer buf = new StringBuffer();
		Matcher m = BRACES_PATTERN.matcher(message);
		Throwable t = null;
		if (params != null && params.length > 0 && params[params.length-1] instanceof Throwable) {
			t = (Throwable)params[params.length-1];
		}
		int i = 0;
		while (m.find()) {
			m.appendReplacement(buf, params != null && i < params.length ? String.valueOf(params[i]).replace("\\", "\\\\").replace("$", "\\$") : "{}");
			i++;
		}
		m.appendTail(buf);
		impl.accept(buf.toString(), t);
	}
	
	public String getImplementationName() {
		return impl.getImplementationName();
	}

	public boolean isTraceEnabled() {
		return impl.isTraceEnabled();
	}

	public boolean isDebugEnabled() {
		return impl.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return impl.isInfoEnabled();
	}

	public boolean isWarnEnabled() {
		return impl.isWarnEnabled();
	}

	public boolean isErrorEnabled() {
		return impl.isErrorEnabled();
	}

	public void trace(String message) {
		if (isTraceEnabled()) impl.trace(message, null);
	}

	public void trace(String message, Object... params) {
		if (isTraceEnabled()) log(impl::trace, message, params);
	}

	public void trace(String message, Object p0) {
		if (isTraceEnabled()) log(impl::trace, message, p0);
	}

	public void trace(String message, Object p0, Object p1) {
		if (isTraceEnabled()) log(impl::trace, message, p0, p1);
	}

	public void trace(String message, Object p0, Object p1, Object p2) {
		if (isTraceEnabled()) log(impl::trace, message, p0, p1, p2);
	}

	public void debug(String message) {
		if (isDebugEnabled()) impl.debug(message, null);
	}

	public void debug(String message, Object... params) {
		if (isDebugEnabled()) log(impl::debug, message, params);
	}

	public void debug(String message, Object p0) {
		if (isDebugEnabled()) log(impl::debug, message, p0);
	}

	public void debug(String message, Object p0, Object p1) {
		if (isDebugEnabled()) log(impl::debug, message, p0, p1);
	}

	public void debug(String message, Object p0, Object p1, Object p2) {
		if (isDebugEnabled()) log(impl::debug, message, p0, p1, p2);
	}

	public void info(String message) {
		if (isInfoEnabled()) impl.info(message, null);
	}

	public void info(String message, Object... params) {
		if (isInfoEnabled()) log(impl::info, message, params);
	}

	public void info(String message, Object p0) {
		if (isInfoEnabled()) log(impl::info, message, p0);
	}

	public void info(String message, Object p0, Object p1) {
		if (isInfoEnabled()) log(impl::info, message, p0, p1);
	}

	public void info(String message, Object p0, Object p1, Object p2) {
		if (isInfoEnabled()) log(impl::info, message, p0, p1, p2);
	}

	public void warn(String message) {
		if (isWarnEnabled()) impl.warn(message, null);
	}

	public void warn(String message, Object... params) {
		if (isWarnEnabled()) log(impl::warn, message, params);
	}

	public void warn(String message, Object p0) {
		if (isWarnEnabled()) log(impl::warn, message, p0);
	}

	public void warn(String message, Object p0, Object p1) {
		if (isWarnEnabled()) log(impl::warn, message, p0, p1);
	}

	public void warn(String message, Object p0, Object p1, Object p2) {
		if (isWarnEnabled()) log(impl::warn, message, p0, p1, p2);
	}

	public void error(String message) {
		if (isErrorEnabled()) impl.error(message, null);
	}

	public void error(String message, Object... params) {
		if (isErrorEnabled()) log(impl::error, message, params);
	}

	public void error(String message, Object p0) {
		if (isErrorEnabled()) log(impl::error, message, p0);
	}

	public void error(String message, Object p0, Object p1) {
		if (isErrorEnabled()) log(impl::error, message, p0, p1);
	}

	public void error(String message, Object p0, Object p1, Object p2) {
		if (isErrorEnabled()) log(impl::error, message, p0, p1, p2);
	}

}
