package owltools.gaf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.log4j.Logger;

import owltools.gaf.test.GafObjectsBuilderTest;

/**
 * The class builds {@link GafDocument} a gaf file.
 * By default it read the whole gaf file and builds the GAFDocument.
 * If a GAF file is large enough, e.g. uniport file, it cannot be loaded as whole into memory,
 * so in such cases the file is needed to be split into small units that only one unit will be loaded into memory. This
 * class has built-in this split functionality. By creating the instance of this class through the constructor with int parameter (split size)
 * enable the split methodology of building the gaf document. See {@link GafObjectsBuilderTest} class for details how to build GafDocument
 * object from this class.
 * @author Shahid Manzoor
 *
 */

public class GafObjectsBuilder {

	private final static Logger LOG = Logger.getLogger(GafObjectsBuilder.class);
	
	private GafDocument gafDocument;
	
	private GAFParser parser;

	
	private String docId;
	
	private String documentPath;

	//this variable is used when a document is splitted
	private int counter;
	
	//this variable is set to true when a document is splitted
	private boolean isSplitted;

	//the number of rows of a gaf file to be used to build a GafDocument
	private int splitSize;
	
	
	public GafObjectsBuilder(){
		gafDocument = new GafDocument();
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
	
	public GafDocument getGafDocument(){
		return gafDocument;
	}
	
/*	public void startDocument(File gafFile) {
		gafDocument.setDocumentPath(gafFile.getAbsolutePath());
		gafDocument.setId(gafFile.getName());
		
	}
*/
	public GAFParser getParser(){
		return parser;
	}
	
	public GafDocument buildDocument(Reader reader, String docId, String path) throws IOException{
	
		this.docId = docId;
		this.documentPath = path;

		parser.parse(reader);
		
		isSplitted = false;
		
		return getNextSplitDocument();
	
	}

	/**
	 * 	
	 * when this variable reaches at splitSize then the algorithm in the getNextSplitDocument method stops calling next method of the GafParser and return the GafDocument object build with the number of rows 
	 * @return
	 * @throws IOException
	 */
	
	
	
	public GafDocument getNextSplitDocument() throws IOException{
		if(parser == null){
			throw new IllegalStateException("the buildDocument method is not called yet.");
		}

		counter = 0;
		gafDocument = new GafDocument(docId, this.documentPath);
		
		while(parser.next()){
			if(splitSize != -1){
			
				if( counter>= this.splitSize){
					isSplitted= true;
					counter = 0;
					break;
				}
				
				counter++;
			}
			Bioentity entity= addBioEntity(parser);
			addGeneAnnotation(parser, entity);
			addWithInfo(parser);
			addCompositeQualifier(parser);
			addExtensionExpression(parser);
		}
		
		
		
		return gafDocument.getBioentities().size()==0?null: gafDocument;

	}
	
	public boolean isDocumentSplitted(){
		return this.isSplitted;
	}
		
	
	public GafDocument buildDocument(File gafFilePath) throws IOException{
		
		FileReader reader = new FileReader(gafFilePath);
		return buildDocument(reader, gafFilePath.getName(), gafFilePath.getCanonicalPath());
		
	}
	
	/**
	 * This method builds Bioentity object from the current position (row) of GafParser
	 * @param parser
	 * @return
	 */
	private Bioentity addBioEntity(GAFParser parser){
		String id = parser.getDb() + ":" + parser.getDbObjectId();
		String symbol = parser.getDbObjectSymbol();
		String fullName = parser.getDbObjectName();
		String typeCls = parser.getDBObjectType();
		String ncbiTaxonId ="";
		String taxons[] = parser.getTaxon().split("\\|");
		taxons = taxons[0].split(":");
		
		if(taxons.length>1){
			ncbiTaxonId = taxons[1];
		}else
			ncbiTaxonId = taxons[0];
		
		String db = parser.getDbObjectSynonym();
		
		Bioentity entity = new Bioentity(id, symbol, fullName, typeCls, "NCBITaxon:" + ncbiTaxonId, db, gafDocument.getId());
		
		gafDocument.addBioentity(entity);
		

		return entity;
	}
	
	
	private void addWithInfo(GAFParser parser){
		if(parser.getWith().length()>0){
			String tokens[] = parser.getWith().split("[\\||,]");
			for(String token: tokens){
				gafDocument.addWithInfo(new WithInfo(parser.getWith(), token));
			}
		}
	}
	
	private void addCompositeQualifier(GAFParser parser){
		if(parser.getQualifier().length()>0){
			String tokens[] = parser.getQualifier().split("[\\||,]");
			for(String token: tokens){
				gafDocument.addCompositeQualifier(new CompositeQualifier(parser.getQualifier(), token));
			}
		}
	}

	private void addExtensionExpression(GAFParser parser){
		if(parser.getAnnotationExtension() != null){
			if(parser.getAnnotationExtension().length()>0){
				String tokens[] = parser.getAnnotationExtension().split("[\\||,]");
				for(String token: tokens){
					
					int index = token.indexOf("(");
					
					if(index>0){
						String relation = token.substring(0, index);
						String cls = token.substring(index+1, token.length()-1);
						gafDocument.addExtensionExpression(new ExtensionExpression(parser.getAnnotationExtension(), relation, cls));
					}
					
				}
			}
			
		}
	}
	
	/**
	 * Build GeneAnnotation object from current position/row of the GafParser.
	 * @param parser
	 * @param entity
	 */
	private void addGeneAnnotation(GAFParser parser, Bioentity entity){
		String compositeQualifier = parser.getQualifier();
	
		
		boolean isContributesTo = compositeQualifier.contains("contributes_to");
		boolean isIntegeralTo = compositeQualifier.contains("integral_to");
		
		String clsId = parser.getGOId();

		String referenceId = parser.getReference();
		
		String evidenceCls = parser.getEvidence();
		String withExpression = parser.getWith();

		String actsOnTaxonId ="";
		
		String taxons[] = parser.getTaxon().split("\\|");
		if(taxons.length>1){
			taxons = taxons[1].split(":");
			actsOnTaxonId = "NCBITaxon:" + taxons[1];
		}
		
		String lastUpdateDate = parser.getDate();
		
		String assignedBy = parser.getAssignedBy();

		String extensionExpression = parser.getAnnotationExtension();
		String geneProductForm = parser.getGeneProjectFormId();

		
		GeneAnnotation ga = new GeneAnnotation(entity.getId(),
				isContributesTo, isIntegeralTo, compositeQualifier, clsId, referenceId, evidenceCls, 
				withExpression, actsOnTaxonId, lastUpdateDate, assignedBy,extensionExpression, geneProductForm, gafDocument.getId());
		ga.setBioentityObject(entity);
		AnnotationSource source = new AnnotationSource(parser.getCurrentRow(), parser.getLineNumber(), gafDocument.getId());
		ga.setSource(source);
		gafDocument.addGeneAnnotation(ga);
		
	}
	
	
}
