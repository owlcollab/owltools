package owltools.gaf.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import owltools.gaf.AnnotationSource;
import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilderTest;
import owltools.gaf.GeneAnnotation;


/**
 * The class builds {@link GafDocument} a gaf file. By default it read the whole
 * gaf file and builds the GAFDocument.<br>
 * <br>
 * 
 * If a GAF file is large enough, e.g. uniprot file, it cannot be loaded as
 * whole into memory. In such cases the file can to be split into smaller units,
 * such that only one unit is loaded into memory.<br>
 * 
 * This class has built-in this split functionality. By creating the instance of
 * this class through the constructor with int parameter (split size) enable the
 * split methodology of building the gaf document. <br>
 * <br>
 * 
 * See {@link GafObjectsBuilderTest} class for details how to build GafDocument
 * object from this class.
 * 
 * @author Shahid Manzoor
 * 
 */
public class GafObjectsBuilder {

	private final static Logger LOG = Logger.getLogger(GafObjectsBuilder.class);
	
	private final GAFParser parser;

	private String docId;
	
	private String documentPath;

	// list of filters
	private List<LineFilter<GAFParser>> filters = null;
	
	//this variable is used when a document is splitted
	private int counter;
	
	//this variable is set to true when a document is splitted
	private boolean isSplitted;

	//the number of rows of a gaf file to be used to build a GafDocument
	private int splitSize;
	
	
	public GafObjectsBuilder(){
		parser = new GAFParser();
		splitSize = -1;
	}
	
	public GafObjectsBuilder(int splitSize){
		this();
		this.splitSize = splitSize;
	}
	
	public int getSplitSize(){
		return this.splitSize;
	}
	
	public GAFParser getParser(){
		return parser;
	}
	
	/**
	 * Add a filter to object builder. Multiple filters are executed in the
	 * order of insertion. Any rejection preempts the execution of the remaining
	 * filters.
	 * 
	 * @param filter
	 */
	public void addFilter(LineFilter<GAFParser> filter) {
		if (filters == null) {
			filters = new ArrayList<LineFilter<GAFParser>>();
		}
		filters.add(filter);
	}
	
	public GafDocument buildDocument(Reader reader, String docId, String path) throws IOException{
		
		this.docId = docId;
		this.documentPath = path;

		parser.parse(reader);
		
		isSplitted = false;
		
		return getNextSplitDocument();
	
	}

	public GafDocument buildDocument(String fileName, String docId, String path) throws IOException, URISyntaxException{
		
		this.docId = docId;
		this.documentPath = path;

		LOG.info("Building document: [" + fileName + "] @ [" + path + "]");
		
		parser.parse(fileName);
		
		isSplitted = false;
		
		return getNextSplitDocument();
	
	}

	/**
	 * When this variable reaches the splitSize count, the algorithm in the
	 * {@link #getNextSplitDocument()} method stops calling next method of the GAFParser
	 * and returns the {@link GafDocument} object build with the number of rows
	 * 
	 * @return gafDocument
	 * @throws IOException
	 */
	public GafDocument getNextSplitDocument() throws IOException{
		if(parser == null){
			throw new IllegalStateException("the buildDocument method is not called yet.");
		}

		counter = 0;
		GafDocument gafDocument = new GafDocument(docId, this.documentPath);
		
		while(parser.next()){
			if(splitSize != -1){
			
				if( counter>= this.splitSize){
					isSplitted= true;
					counter = 0;
					break;
				}
				
				counter++;
			}
			// by default load everything
			boolean load = true;
			if (filters != null) {
				// check each filter
				for (LineFilter<GAFParser> filter : filters) {
					boolean accept = filter.accept(parser.getCurrentRow(), parser.getLineNumber(), parser);
					if (accept == false) {
						load = false;
						break;
					}
				}
			}
			if (load) {
				Bioentity entity= parseBioEntity(parser);
				entity = gafDocument.addBioentity(entity);
				GeneAnnotation annotation = parseGeneAnnotation(parser, entity, docId);
				gafDocument.addGeneAnnotation(annotation);
			}
		}
		return gafDocument;

	}
	
	public boolean isDocumentSplitted(){
		return this.isSplitted;
	}
		
	
	public GafDocument buildDocument(File gafFilePath) throws IOException{
		
		FileReader reader = new FileReader(gafFilePath);
		return buildDocument(reader, gafFilePath.getName(), gafFilePath.getCanonicalPath());
		
	}
	
	public GafDocument buildDocument(String gafFile) throws IOException, URISyntaxException{
		File gafFilePath = new File(gafFile);
		return buildDocument(gafFile, gafFilePath.getName(), gafFilePath.getCanonicalPath());
		
	}

	public synchronized void dispose() {
		if (parser != null) {
			parser.dispose();
		}
		if (filters != null) {
			filters.clear();
			filters = null;
		}
	}
	
	/**
	 * This method builds {@link Bioentity} object from the current position (row) of GAFParser
	 * @param parser
	 * @return bioentity, never null
	 */
	static Bioentity parseBioEntity(GAFParser parser){
		String id = parser.getDb() + ":" + parser.getDbObjectId();
		String symbol = parser.getDbObjectSymbol();
		String fullName = parser.getDbObjectName();
		String typeCls = parser.getDBObjectType();
		String ncbiTaxonId ="";
		String taxons[] = StringUtils.split(parser.getTaxon(), '|');
		if (taxons.length > 0) {
			ncbiTaxonId = BuilderTools.handleTaxonPrefix(taxons[0]);
		}
		
		String db = parser.getDb();
		
		// "NCBITaxon:"
		Bioentity entity = new Bioentity(id, symbol, fullName, typeCls, ncbiTaxonId, db);
		
		// Handle parsing out the synonyms separately.
		BuilderTools.addSynonyms(parser.getDbObjectSynonym(), entity);
		return entity;
	}
	
	/**
	 * Build GeneAnnotation object from current position/row of the GAFParser.
	 * 
	 * @param parser
	 * @param entity
	 * @param documentId
	 * @return gene annotation
	 */
	private static GeneAnnotation parseGeneAnnotation(GAFParser parser, Bioentity entity, String documentId){
		final GeneAnnotation ga = new GeneAnnotation();
		ga.setCls(parser.getGOId());
		BuilderTools.addXrefs(parser.getReference(), ga);
		ga.setBioentity(entity.getId());
		ga.setBioentityObject(entity);
		ga.setEvidenceCls(parser.getEvidence());
		ga.setLastUpdateDate(parser.getDate());
		ga.setAssignedBy(parser.getAssignedBy());
		ga.setGeneProductForm(parser.getGeneProjectFormId());
		
		// handle composite qualifiers
		final String qualifierString = parser.getQualifier();
		ga.setIsContributesTo(qualifierString.contains("contributes_to"));
		ga.setIsIntegralTo(qualifierString.contains("integral_to"));
		ga.setIsNegated(qualifierString.contains("NOT"));
		
		List<String> qualifiers = BuilderTools.parseCompositeQualifier(qualifierString);
		ga.setCompositeQualifiers(qualifierString, qualifiers);

		// handle relation and aspect
		String relation = null;
		final String aspect = parser.getAspect();
		if (aspect.equals("F"))
			relation = "enables";
		else if (aspect.equals("P"))
			relation = "involved_in";
		else if (aspect.equals("C"))
			relation = "part_of";
		else
			relation = aspect;
		
		if (qualifierString.contains("contributes_to"))
			relation = "contributes_to";
		if (qualifierString.contains("colocalizes_with"))
			relation = "colocalizes_with";
				
		ga.setAspect(aspect);
		ga.setRelation(relation);
		
		// handle with
		final String withExpression = parser.getWith();
		final Collection<String> withInfos = BuilderTools.parseWithInfo(withExpression);
		ga.setWithInfos(withExpression, withInfos);

		// handle acts on taxon
		String taxons[] = StringUtils.split(parser.getTaxon(), '|');
		if(taxons.length > 1) {
			ga.setActsOnTaxonId(BuilderTools.handleTaxonPrefix(taxons[1]));
		}
		
		// handle extension expression
		String extensionExpression = parser.getAnnotationExtension();
		List<List<ExtensionExpression>> extensionExpressionList = BuilderTools.parseExtensionExpression(extensionExpression);
		ga.setExtensionExpressions(extensionExpressionList);
		
		// set source
		AnnotationSource source = new AnnotationSource(parser.getCurrentRow(), parser.getLineNumber(), documentId);
		ga.setSource(source);
		
		return ga;
	}
	
	
}
