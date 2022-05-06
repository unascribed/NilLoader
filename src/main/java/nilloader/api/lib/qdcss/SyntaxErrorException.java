package nilloader.api.lib.qdcss;

public class SyntaxErrorException extends QDCSSException {
	public SyntaxErrorException() {}
	public SyntaxErrorException(String message, Throwable cause) { super(message, cause); }
	public SyntaxErrorException(String s) { super(s); }
	public SyntaxErrorException(Throwable cause) { super(cause); }
}