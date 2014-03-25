package owltools.gaf.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import owltools.gaf.Bioentity;
import owltools.gaf.BioentityDocument;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.SimpleEcoMapper;

public class GpadGpiObjectsBuilder {
	
	private static final Logger logger = Logger.getLogger(GpadGpiObjectsBuilder.class);

	// list of filters
	private List<LineFilter<GpadParser>> gpadFilters = null;
	private List<LineFilter<GpiParser>> gpiFilters = null;
	private AspectProvider aspectProvider = null;
	private final SimpleEcoMapper ecoMapper;
	
	private boolean gpadIncludeUnknowBioentities = false;
	
	public GpadGpiObjectsBuilder(SimpleEcoMapper ecoMapper) {
		this.ecoMapper = ecoMapper;
	}

	public void addGpadFilter(LineFilter<GpadParser> filter) {
		if (gpadFilters == null) {
			gpadFilters = new ArrayList<LineFilter<GpadParser>>();
		}
		gpadFilters.add(filter);
	}
	
	public void addGpiFilter(LineFilter<GpiParser> filter) {
		if (gpiFilters == null) {
			gpiFilters = new ArrayList<LineFilter<GpiParser>>();
		}
		gpiFilters.add(filter);
	}
	
	public boolean isGpadIncludeUnknowBioentities() {
		return gpadIncludeUnknowBioentities;
	}

	public void setGpadIncludeUnknowBioentities(boolean gpadIncludeUnknowBioentities) {
		this.gpadIncludeUnknowBioentities = gpadIncludeUnknowBioentities;
	}

	public Pair<BioentityDocument, GafDocument> loadGpadGpi(File gpad, File gpi) throws IOException {
		// 1. load GPI
		BioentityDocument entities = new BioentityDocument(gpi.getName(), gpi.getCanonicalPath());
		Map<String,Bioentity> mappings = loadGPI(getInputStream(gpi), entities);
		
		// create annotation document with bioentities
		GafDocument document = new GafDocument(gpad.getName(), gpad.getCanonicalPath(), mappings);
		
		// 2. load GPAD
		loadGPAD(getInputStream(gpad), document);
		
		return Pair.of(entities, document);
	}
	
	public void setAspectProvider(AspectProvider aspectProvider) {
		this.aspectProvider = aspectProvider;
	}
	
	public static interface AspectProvider {
		
		public String getAspect(String cls);
	}
	
	private InputStream getInputStream(File file) throws IOException {
		InputStream inputStream = new FileInputStream(file);
		String fileName = file.getName().toLowerCase();
		if (fileName.endsWith(".gz")) {
			inputStream = new GZIPInputStream(inputStream);
		}
		return inputStream;
	}
	
	private Map<String, Bioentity> loadGPI(InputStream inputStream, final BioentityDocument document) throws IOException {
		GpiParser parser = null;
		try {
			parser = new GpiParser();
			parser.addCommentListener(new CommentListener() {
				
				@Override
				public void readingComment(String comment, String line, int lineNumber) {
					document.addComment(comment);
				}
			});
			parser.createReader(inputStream);
			return loadBioentities(parser, document);
		}
		finally {
			IOUtils.closeQuietly(parser);
		}
	}
	
	private Map<String, Bioentity> loadBioentities(GpiParser parser, BioentityDocument document) throws IOException {
		Map<String, Bioentity> entities = new HashMap<String, Bioentity>();
		
		while(parser.next()) {
			// by default load everything
			boolean load = true;
			if (gpiFilters != null) {
				// check each filter
				for (LineFilter<GpiParser> filter : gpiFilters) {
					boolean accept = filter.accept(parser.getCurrentRow(), parser.getLineNumber(), parser);
					if (accept == false) {
						load = false;
						break;
					}
				}
			}
			if (load) {
				String namespace = parser.getNamespace();
				if (namespace != null) {
					Bioentity bioentity = parseBioentity(parser);
					entities.put(bioentity.getId(), bioentity);
					document.addBioentity(bioentity);
				}
			}
		}
		return entities;
	}
	
	private void loadGPAD(InputStream inputStream, GafDocument document) throws IOException {
		GpadParser parser = null;
		try {
			parser = new GpadParser();
			parser.createReader(inputStream);
			loadGeneAnnotations(parser, document);
		}
		finally {
			IOUtils.closeQuietly(parser);
		}
	}
	
	private void loadGeneAnnotations(GpadParser parser, final GafDocument document) throws IOException {
		parser.addCommentListener(new CommentListener() {
			
			@Override
			public void readingComment(String comment, String line, int lineNumber) {
				document.addComment(comment);
			}
		});
		while(parser.next()) {
			// by default load everything
			boolean load = true;
			if (gpiFilters != null) {
				// check each filter
				for (LineFilter<GpadParser> filter : gpadFilters) {
					boolean accept = filter.accept(parser.getCurrentRow(), parser.getLineNumber(), parser);
					if (accept == false) {
						load = false;
						break;
					}
				}
			}
			if (load) {
				GeneAnnotation annotation = parseAnnotation(parser, document, aspectProvider, ecoMapper);
				if (annotation != null) {
					document.addGeneAnnotation(annotation);
				}
			}
		}
	}

	private Bioentity parseBioentity(GpiParser parser) {
		String db = parser.getNamespace();
		String bioentityId = db + ":" + parser.getDB_Object_ID();
		Bioentity entity = new Bioentity(bioentityId,
				parser.getDB_Object_Symbol(), parser.getDB_Object_Name(),
				parser.getDB_Object_Type(),
				BuilderTools.handleTaxonPrefix(parser.getTaxon()), db);

		BuilderTools.addSynonyms(parser.getDB_Object_Synonym(), entity);
		entity.setParentObjectId(parser.getParent_Object_ID());
		BuilderTools.addXrefs(parser.getDB_Xref(), entity);
		BuilderTools.addProperties(parser.getGene_Product_Properties(), entity);
		return entity;
	}
	
	protected void reportUnknowBioentityId(String db, String objectId, String fullId) {
		logger.warn("No Bioentity found for id: "+fullId);
	}
	
	private GeneAnnotation parseAnnotation(GpadParser parser, GafDocument document, AspectProvider aspectProvider, SimpleEcoMapper mapper) {
		GeneAnnotation ga = new GeneAnnotation();
		String cls = parser.getGO_ID();
		
		// col 1-2
		String bioentityId = parser.getDB() + ":" + parser.getDB_Object_ID();
		Bioentity entity = document.getBioentity(bioentityId);
		if (entity == null) {
			reportUnknowBioentityId(parser.getDB(), parser.getDB_Object_ID(), bioentityId);
			if (gpadIncludeUnknowBioentities == false) {
				return null;
			}
			ga.setBioentity(bioentityId);
		}
		else {
			ga.setBioentity(entity.getId());
			ga.setBioentityObject(entity);
		}

		// col 3
		final String qualifierString = parser.getQualifier();
		List<String> allQualifiers = BuilderTools.parseCompositeQualifier(qualifierString);
		String relation = null;
		for (String qualifier : allQualifiers) {
			if (qualifier.equals("NOT")) {
				// modifier
				ga.setIsNegated(true);
			}
			else if (qualifier.equals("contributes_to")) {
				// relationship
				ga.setIsContributesTo(true);
				relation = qualifier;
			}
			else if (qualifier.equals("integral_to")) {
				// modifier
				ga.setIsIntegralTo(true);
			}
			relation = qualifier;
		}
		
		String aspect = "";
		if (aspectProvider != null) {
			aspect = aspectProvider.getAspect(cls);
			if (aspect != null && relation == null) {
				if (aspect.equals("F")) {
					relation = "enables";
				} else if (aspect.equals("P")) {
					relation = "involved_in";
				} else if (aspect.equals("C")) {
					relation = "part_of";
				}
			}
		}
		ga.setRelation(relation);
		ga.setAspect(aspect);
		
		// col 4
		ga.setCls(cls);
		
		// col 5
		BuilderTools.addXrefs(parser.getDB_Reference(), ga);
		
		// col 6
		addEvidenceCode(parser.getEvidence_Code(), ga, mapper);
		
		// col 7 with
		ga.setWithInfos(BuilderTools.parseWithInfo(parser.getWith()));
		
		// col8
		ga.setActsOnTaxonId(BuilderTools.parseTaxonRelationshipPair(parser.getInteracting_Taxon_ID()));
		
		// col 9
		ga.setLastUpdateDate(parser.getDate());
		
		// col 10
		ga.setAssignedBy(parser.getAssigned_by());
		
		// col 11
		String extensionExpression = parser.getAnnotation_Extension();
		List<List<ExtensionExpression>> extensionExpressionList = BuilderTools.parseExtensionExpression(extensionExpression);
		ga.setExtensionExpressions(extensionExpressionList);
		
		// col 12
		BuilderTools.addProperties(parser.getAnnotation_Properties(), ga);
		
		return ga;
	}
	
	private void addEvidenceCode(String eco, GeneAnnotation ga, SimpleEcoMapper mapper) {
		Pair<String,String> pair = mapper.getGoCode(eco);
		if (pair != null) {
			String goCode = pair.getLeft();
			if (goCode != null) {
				ga.setEvidence(goCode, eco);
			}
		}
	}
}
