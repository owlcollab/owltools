package owltools.gaf;

public interface GafParserListener {

	public void parsing(String line, int lineNumber);
	public void parserError(String errorMessage, String line, int lineNumber);
	
}
