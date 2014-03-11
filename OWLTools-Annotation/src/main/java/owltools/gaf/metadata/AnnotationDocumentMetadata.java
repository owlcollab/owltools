package owltools.gaf.metadata;

public class AnnotationDocumentMetadata {
	
	public String dbname; // e.g. goa_human, mgi
	public String submissionDate; // MUST be ISO 8601, ie YYYY-MM-DD
	public int annotatedEntityCount; // aka Gene Products Annotated
	public int annotationCount;
	public int annotationCountExcludingIEA;
	public long gafDocumentSizeInBytes;

}
