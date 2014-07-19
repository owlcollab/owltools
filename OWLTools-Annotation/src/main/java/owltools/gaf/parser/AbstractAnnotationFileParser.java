package owltools.gaf.parser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

abstract class AbstractAnnotationFileParser implements Closeable {
	
	private static final Logger LOG = Logger.getLogger(AbstractAnnotationFileParser.class);
	
	private BufferedReader reader;
	
	protected double version;
	
	protected String currentRow;
	
	protected String currentCols[];
	
	protected int lineNumber;
	
	private List<Object> violations;
	
	private final List<ParserListener> parserListeners;
	
	private final List<CommentListener> commentListeners;

	protected final String commentPrefix;
	private final String formatName;
	
	public AbstractAnnotationFileParser(double defaultVersion, String commentPrefix, String formatName) {
		reader = null;
		version = defaultVersion;
		currentRow = null;
		currentCols = null;
		lineNumber = 0;
		violations = null;
		parserListeners = new ArrayList<ParserListener>();
		commentListeners = new ArrayList<CommentListener>();
		this.commentPrefix = commentPrefix;
		this.formatName = formatName;
	}
	//----------------------------
	//
	//----------------------------

	@SuppressWarnings("unused")
	private static enum GpiColumns {
		
		DB_Object_ID(1, "DB_Object_ID", true),
		DB_Object_Symbol(2, "DB_Object_Symbol", true),
		DB_Object_Name(3, "DB_Object_Name", false),
		DB_Object_Synonym(4, "DB_Object_Synonym", false),
		DB_Object_Type(5, "DB_Object_Type", true),
		Taxon(6, "Taxon", true),
		Parent_Object_ID(7, "Parent_Object_ID", false),
		DB_Xref(8, "DB_Xref", false),
		Gene_Product_Properties(9, "Gene_Product_Properties", false);
		
		private final int pos;
		private final String name;
		private final boolean required;
		
		private GpiColumns(int pos, String name, boolean required) {
			this.pos = pos;
			this.name = name;
			this.required = required;
		}
		
		private int index() {
			return pos - 1;
		}
		
		private String getName() {
			return name;
		}
		
		private boolean isRequired() {
			return required;
		}
	}
	
	//----------------------------
	//
	//----------------------------
	
	/**
	 * Try to parse a next line, may skip lines until a line is parsed successfully or the file ends.
	 * 
	 * @return true, if there is a next line.
	 * @throws IOException
	 */
	public boolean next() throws IOException{
		
		while (true) {
			ReadState state = loadNext();
			if (state == ReadState.success) {
				return true;
			}
			else if (state == ReadState.no) {
				return false;
			}
			// default ReadState.next
			// continue loop
		}
	}
	
	enum ReadState {
		success,
		no,
		next
	}
	
	/**
	 * Using a recursive call to check if a next line exists, may lead to an over flow.
	 * Use a while loop with a function call, which has a tri-state return value.
	 * 
	 * @return ReadState
	 * @throws IOException
	 */
	private ReadState loadNext() throws IOException{
		if(reader != null){
			currentRow  = reader.readLine();
			if(currentRow == null){
				return ReadState.no;
			}

			lineNumber++;

			final String trimmedLine = StringUtils.trimToEmpty(currentRow);
			if (trimmedLine.length() == 0) {
				return ReadState.next;
			}
			else if (trimmedLine.startsWith(commentPrefix)) {
				if (isHeaderMetaData(trimmedLine)) {
					handleHeaderMetaData(trimmedLine);
				}
				else {
					fireComment();
				}
				return ReadState.next;
			}
			else{
				fireParsing();
				this.currentCols = StringUtils.splitPreserveAllTokens(this.currentRow, '\t');
				return validateLine(currentCols);
			}
		}
		return ReadState.no;
	}

	/**
	 * @param currentCols
	 * @return readstate
	 */
	protected ReadState validateLine(String[] currentCols) {
		int expectedColumnCount = getExpectedColumnCount();
		if (currentCols.length != expectedColumnCount) {

			final String error = "Got invalid number of columns for row '"+lineNumber+"' (expected "
				+ expectedColumnCount
				+ ", got "
				+ currentCols.length
				+ ") for format "
				+ formatName;

			if (currentCols.length < expectedColumnCount) {
				addViolation(error);
				fireParsingError(error);
				LOG.error(error + "  The row is ignored: " + this.currentRow);
				return ReadState.next;
			}
			else {
				fireParsingWarning(error);
				LOG.warn(error + " : " + this.currentRow);
			}
		}
		return ReadState.success;
	}

	/**
	 * @param line
	 */
	protected void handleHeaderMetaData(final String line) {
		if (isFormatDeclaration(line)) {
			version = parseVersion(line);
		}
	}
	
	protected abstract boolean isHeaderMetaData(String line);
	
	protected abstract int getExpectedColumnCount();
	
	protected abstract boolean isFormatDeclaration(String line);
	
	protected abstract double parseVersion(String line);
	
	private void fireParsing(){
		for(ParserListener listner: parserListeners){
			listner.parsing(this.currentRow, lineNumber);
		}
	}
	
	private void fireComment() {
		if (!commentListeners.isEmpty()) {
			String comment = StringUtils.substringAfter(this.currentRow, commentPrefix);
			for(CommentListener listener: commentListeners) {
				listener.readingComment(comment, this.currentRow, lineNumber);
			}
		}
	}
	
	protected void fireParsingError(String message){
		for(ParserListener listner: parserListeners){
			listner.parserError(message, this.currentRow, lineNumber);
		}
	}
	
	protected void fireParsingWarning(String message){
		for(ParserListener listner: parserListeners){
			if (listner.reportWarnings()) {
				listner.parserWarning(message, this.currentRow, lineNumber);
			}
		}
	}
	
	//----------------------------
	//
	//----------------------------
	
	public void createReader(InputStream inputStream) {
		reader = new BufferedReader(new InputStreamReader(inputStream));
	}
	
	public void setReader(BufferedReader reader) {
		this.reader = reader;
	}
	
	public String getCurrentRow(){
		return this.currentRow;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	//----------------------------
	//
	//----------------------------
	
	public void addParserListener(ParserListener listener){
		if(listener == null)
			return;
		
		if(!parserListeners.contains(listener))
			parserListeners.add(listener);
	}
	
	public void remoteParserListener(ParserListener listener){
		if(listener == null)
			return;
		
		parserListeners.remove(listener);
	}

	public void addCommentListener(CommentListener listener) {
		if (listener != null && !commentListeners.contains(listener)) {
			commentListeners.add(listener);
		}
	}
	
	public void removeCommentListener(CommentListener listener) {
		if (listener != null) {
			commentListeners.remove(listener);
		}
	}

	//----------------------------
	//
	//----------------------------
	
	protected void addViolation(Object violation) {
		if (violations == null) {
			violations = new ArrayList<Object>();
		}
		violations.add(violation);
	}
	
	public List<Object> getViolations(){
		return this.violations;
	}

	@Override
	public void close() throws IOException {
		IOUtils.closeQuietly(reader);
		violations = null;
		parserListeners.clear();
		commentListeners.clear();
	}
}
