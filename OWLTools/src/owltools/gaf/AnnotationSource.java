package owltools.gaf;

/**
 * 
 * @author Shahid Manzoor
 *
 */
public class AnnotationSource {

	private String row;
	private int lineNumber;
	private String fileName;

	
	
	public AnnotationSource(String row, int lineNumber, String fileName) {
		super();
		this.row = row;
		this.lineNumber = lineNumber;
		this.fileName = fileName;
	}
	
	
	public String getRow() {
		return row;
	}
	public int getLineNumber() {
		return lineNumber;
	}
	public String getFileName() {
		return fileName;
	}
	
	
	
}
