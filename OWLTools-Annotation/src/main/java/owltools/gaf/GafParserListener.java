package owltools.gaf;

public interface GafParserListener {

	/**
	 * The current line is parsed. May trigger calls of 
	 * {@link #parserError(String, String, int)} and 
	 * {@link #parserWarning(String, String, int)}.
	 * 
	 * @param line
	 * @param lineNumber
	 */
	public void parsing(String line, int lineNumber);
	
	/**
	 * Report a parser error. The parser will skip this line.
	 * 
	 * @param errorMessage
	 * @param line
	 * @param lineNumber
	 */
	public void parserError(String errorMessage, String line, int lineNumber);
	
	/**
	 * Report a parser warning.
	 * 
	 * @param message
	 * @param line
	 * @param lineNumber
	 * 
	 * @see #reportWarnings()
	 */
	public void parserWarning(String message, String line, int lineNumber);
	
	/**
	 * @return true, if warning should be reported to this listener.
	 * 
	 * @see #parserWarning(String, String, int)
	 */
	public boolean reportWarnings();
}
