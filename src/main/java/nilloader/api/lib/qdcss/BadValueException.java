package nilloader.api.lib.qdcss;

public class BadValueException extends QDCSSException {
	public BadValueException() {}
	public BadValueException(String message, Throwable cause) { super(message, cause); }
	public BadValueException(String s) { super(s); }
	public BadValueException(Throwable cause) { super(cause); }
}