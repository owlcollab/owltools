package owltools.gaf.parser;

public interface CommentListener {
	
	public void readingComment(String comment, String line, int lineNumber);
}