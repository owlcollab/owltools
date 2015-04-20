package org.bbop.golr.java;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationExtension.GolrAnnotationExtensionEntry.GolrAnnotationExtensionRelation;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class RetrieveGolrAnnotations extends AbstractRetrieveGolr{
	
	static boolean JSON_INDENT_FLAG = false;
	static int PAGINATION_CHUNK_SIZE = 100;
	
	private static final Gson GSON = new GsonBuilder().create();
	
	/*
	 * This flag indicates that missing c16 data, due to malformed JSON is acceptable. 
	 */
	private final boolean ignoreC16ParseErrors;

	public RetrieveGolrAnnotations(String server) {
		this(server, 3, false);
	}
	
	public RetrieveGolrAnnotations(String server, int retryCount, boolean ignoreC16ParseErrors) {
		super(server, retryCount);
		this.ignoreC16ParseErrors = ignoreC16ParseErrors;
	}
	
	@Override
	protected boolean isIndentJson() {
		return JSON_INDENT_FLAG;
	}

	@Override
	protected List<String> getRelevantFields() {
		return GolrAnnotationDocument.getRelevantFields();
	}

	public GafDocument convert(List<GolrAnnotationDocument> golrAnnotationDocuments) throws IOException {
		Map<String, Bioentity> entities = new HashMap<String, Bioentity>();
		GafDocument document = new GafDocument(null, null);
		convert(golrAnnotationDocuments, entities, document);
		return document;
	}
	
	public void convert(List<GolrAnnotationDocument> golrAnnotationDocuments, Map<String, Bioentity> entities, GafDocument document) throws IOException, JsonSyntaxException {
		for (GolrAnnotationDocument golrDocument : golrAnnotationDocuments) {
			String bioentityId = golrDocument.bioentity;
			Bioentity entity = entities.get(bioentityId);
			if (entity == null) {
				entity = new Bioentity();
				entity.setId(bioentityId);
				entity.setSymbol(golrDocument.bioentity_label);
				entity.setFullName(golrDocument.bioentity_name);
				entity.setNcbiTaxonId(golrDocument.taxon);
				entity.setTypeCls(golrDocument.type);
				entity.setDb(golrDocument.source); // TODO check is that the correct mapping
				entity.setSynonyms(golrDocument.synonym);
				entities.put(bioentityId, entity);
				document.addBioentity(entity);
			}
			GeneAnnotation annotation = new GeneAnnotation();
			annotation.setAspect(golrDocument.aspect);
			annotation.setAssignedBy(golrDocument.assigned_by);
			annotation.setBioentity(bioentityId);
			annotation.setBioentityObject(entity);
			annotation.setCls(golrDocument.annotation_class);
			annotation.setEvidence(golrDocument.evidence_type, null);
			annotation.setGeneProductForm(golrDocument.bioentity_isoform);
			annotation.setLastUpdateDate(golrDocument.date);
			if (golrDocument.reference != null) {
				annotation.addReferenceIds(golrDocument.reference);
			}
			if (golrDocument.evidence_with != null) {
				annotation.setWithInfos(golrDocument.evidence_with);
			}
			handleAnnotationExtension(annotation, golrDocument);
			document.addGeneAnnotation(annotation);
		}
	}
	
	protected void handleAnnotationExtension(GeneAnnotation annotation, GolrAnnotationDocument document) throws JsonSyntaxException {
		if (document.annotation_extension_json != null && 
				document.annotation_extension_json.isEmpty() == false){
			List<List<ExtensionExpression>> expressions = annotation.getExtensionExpressions();
			if (expressions == null) {
				expressions = new ArrayList<List<ExtensionExpression>>(document.annotation_extension_json.size());
			}
			for(String json : document.annotation_extension_json) {
				try {
					GolrAnnotationExtension extension = GSON.fromJson(json, GolrAnnotationExtension.class);
					if (extension != null && extension.relationship != null) {
						// WARNING the Golr c16 model is lossy! There is no distinction between disjunction and conjunction in Golr-c16
						// add all as disjunction
						String relation = extractRelation(extension);
						if (relation != null) {
							ExtensionExpression ee = new ExtensionExpression(relation, extension.relationship.id);
							expressions.add(Collections.singletonList(ee));
						}
					}
				} catch (JsonSyntaxException e) {
					// when the ignore flag is set, the user has decided that incomplete c16 data is better than no data.
					if (ignoreC16ParseErrors == false) {
						throw e;
					}
				}
			}
			annotation.setExtensionExpressions(expressions);
		};
	}
	
	private String extractRelation(GolrAnnotationExtension extension) {
		StringBuilder sb = new StringBuilder();
		for(GolrAnnotationExtensionRelation rel : extension.relationship.relation) {
			if (sb.length() > 0) {
				sb.append(" o ");
			}
			sb.append(rel.id);
		}
		if (sb.length() > 0) {
			return sb.toString();
		}
		return null;
	}
	
	public List<GolrAnnotationDocument> getGolrAnnotationsForGenes(List<String> ids) throws IOException {
		List<String[]> tagvalues = new ArrayList<String[]>();
		String [] tagvalue = new String[ids.size() + 1];
		tagvalue[0] = "bioentity";
		for (int i = 0; i < ids.size(); i++) {
			tagvalue[i+1] = ids.get(i);
		}
		tagvalues.add(tagvalue);
		final List<GolrAnnotationDocument> documents = getGolrAnnotations(tagvalues);
		return documents;
	}

	public List<GolrAnnotationDocument> getGolrAnnotationsForGene(String id) throws IOException {
		List<String[]> tagvalues = new ArrayList<String[]>();
		String [] tagvalue = new String[2];
		tagvalue[0] = "bioentity";
		tagvalue[1] = id;
		tagvalues.add(tagvalue);
		final List<GolrAnnotationDocument> documents = getGolrAnnotations(tagvalues);
		return documents;
	}
	
	public List<GolrAnnotationDocument> getGolrAnnotationsForSynonym(String source, String synonym) throws IOException {
		return getGolrAnnotationsForSynonym(source, Collections.singletonList(synonym));
	}
	
	public List<GolrAnnotationDocument> getGolrAnnotationsForSynonym(String source, List<String> synonyms) throws IOException {
		List<String[]> tagvalues = new ArrayList<String[]>();
		String [] param1 = new String[2];
		param1[0] = "source";
		param1[1] = source;
		tagvalues.add(param1);
		String [] param2 = new String[synonyms.size() + 1];
		param2[0] = "synonym";
		for (int i = 0; i < synonyms.size(); i++) {
			param2[i+1] = synonyms.get(i);
		}
		tagvalues.add(param2);
		
		final List<GolrAnnotationDocument> documents = getGolrAnnotations(tagvalues);

		return documents;
	}
	
	public List<GolrAnnotationDocument> getGolrAnnotations(List<String []> tagvalues) throws IOException {
		JSON_INDENT_FLAG = true;
		final URI uri = createGolrRequest(tagvalues, "annotation", 0, PAGINATION_CHUNK_SIZE);
		final String jsonString = getJsonStringFromUri(uri);
		final GolrResponse<GolrAnnotationDocument> response = parseGolrResponse(jsonString);
		final List<GolrAnnotationDocument> documents = new ArrayList<GolrAnnotationDocument>(response.numFound);
		documents.addAll(Arrays.asList(response.docs));
		if (response.numFound > PAGINATION_CHUNK_SIZE) {
			// fetch remaining documents
			int start = PAGINATION_CHUNK_SIZE;
			int end = response.numFound / PAGINATION_CHUNK_SIZE;
			if (response.numFound % PAGINATION_CHUNK_SIZE != 0) {
				end += 1;
			}
			end = end * PAGINATION_CHUNK_SIZE;
			while (start <= end) {
				URI uriPagination = createGolrRequest(tagvalues, "annotation", start, PAGINATION_CHUNK_SIZE);
				String jsonStringPagination = getJsonStringFromUri(uriPagination);
				GolrResponse<GolrAnnotationDocument> responsePagination = parseGolrResponse(jsonStringPagination);
				documents.addAll(Arrays.asList(responsePagination.docs));
				start += PAGINATION_CHUNK_SIZE;
			}
		}
		return documents;
	}
	
	private static class GolrAnnotationResponse extends GolrEnvelope<GolrAnnotationDocument> {
		// empty
	}
	
	public static class GolrAnnotationDocument {
		String source;
		String bioentity;
		String bioentity_internal_id;
		String bioentity_label;
		String bioentity_name;
		String annotation_class;
		String annotation_class_label;
		String evidence_type;
		String aspect;
		String type;
		String taxon;
		String taxon_label;
		String date;
		String assigned_by;
		String bioentity_isoform;
		String panther_family;
		String panther_family_label;
		List<String> annotation_extension_json;
		List<String> synonym;
		List<String> evidence_with;
		List<String> reference;
		
		static List<String> getRelevantFields() {
			// explicit list of fields, avoid "*" retrieval of unused fields
			return Arrays.asList("source",
					"bioentity",
					"bioentity_internal_id",
					"bioentity_label",
					"bioentity_name",
					"annotation_class",
					"annotation_class_label",
					"evidence_type",
					"aspect",
					"type",
					"taxon",
					"taxon_label",
					"date",
					"assigned_by",
					"bioentity_isoform",
					"panther_family",
					"panther_family_label",
					"annotation_extension_json",
					"synonym",
					"evidence_with",
					"reference");
		}
	}

	public static class GolrAnnotationExtension {
		
		GolrAnnotationExtensionEntry relationship;
		
		public static class GolrAnnotationExtensionEntry {
			List<GolrAnnotationExtensionRelation> relation; // list represents a property chain
			String id;
			String label;
			
			public static class GolrAnnotationExtensionRelation {
				String id;
				String label;
			}
		}
	}

	private GolrResponse<GolrAnnotationDocument> parseGolrResponse(String jsonString) throws IOException {
		return parseGolrResponse(jsonString, GolrAnnotationResponse.class).response;
	}
}
