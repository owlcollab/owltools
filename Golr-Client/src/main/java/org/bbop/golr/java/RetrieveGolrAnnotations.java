package org.bbop.golr.java;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class RetrieveGolrAnnotations {
	
	static boolean JSON_INDENT_FLAG = false;
	static int PAGINATION_CHUNK_SIZE = 100;
	
	private static final Gson GSON = new GsonBuilder().create();
	
	private final String server;

	RetrieveGolrAnnotations(String server) {
		this.server = server;
	}
	
	public GafDocument convert(List<GolrDocument> golrDocuments) {
		Map<String, Bioentity> entities = new HashMap<String, Bioentity>();
		GafDocument document = new GafDocument(null, null);
		for (GolrDocument golrDocument : golrDocuments) {
			String bioentityId = golrDocument.bioentity;
			Bioentity entity = entities.get(bioentityId);
			if (entity == null) {
				entity = new Bioentity();
				entity.setId(bioentityId);
				entity.setFullName(golrDocument.bioentity_label);
				entity.setNcbiTaxonId(golrDocument.taxon);
				entity.setTypeCls(golrDocument.type);
				entity.setDb(golrDocument.source); // TODO check is that the correct mapping
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
		}
		return document;
	}

	public List<GolrDocument> getGolrAnnotationsForGene(String id) throws IOException {
		final String url = createGolrString(id, "bioentity", "annotation", 0, PAGINATION_CHUNK_SIZE);
		final String jsonString = getJsonStringFromUrl(url);
		final GolrResponse response = parseGolrResponse(jsonString);
		final List<GolrDocument> documents = new ArrayList<GolrDocument>(response.numFound);
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
				String urlPagination = createGolrString(id, "bioentity", "annotation", start, PAGINATION_CHUNK_SIZE);
				String jsonStringPagination = getJsonStringFromUrl(urlPagination);
				GolrResponse responsePagination = parseGolrResponse(jsonStringPagination);
				documents.addAll(Arrays.asList(responsePagination.docs));
				start += PAGINATION_CHUNK_SIZE;
			}
		}
		return documents;
	}
	
	
	
	String createGolrString(String id, String field, String category, int start, int pagination) {
		StringBuilder sb = new StringBuilder(server);
		sb.append("/select?defType=edismax&qt=standard&wt=json");
		if (JSON_INDENT_FLAG) {
			sb.append("&indent=on");
		}
		sb.append("&fl=*,score");
		sb.append("&facet=false");
		sb.append("&json.nl=arrarr");
		sb.append("&q=*:*");
		sb.append("&rows=").append(pagination);
		sb.append("&start=").append(start);
		sb.append("&fq=document_category:\"").append(category).append("\"");
		sb.append("&fq=").append(field).append(":\"").append(id).append("\"");
		return sb.toString();
	}
	
	protected String getJsonStringFromUrl(String urlString) throws IOException {
		try {
			URL url = new URL(urlString);
			String content = IOUtils.toString(url.openStream());
			return content;
		} catch (MalformedURLException e) {
			throw new IOException(e);
		}
	}
	
	static class GolrEnvelope {
		GolrResponseHeader responseHeader;
		GolrResponse response;
	}
	
	static class GolrResponseHeader {
		String status;
		String QTime;
		Object params;
	}
	
	static class GolrResponse {
		int numFound;
		int start;
		GolrDocument[] docs;
	}
	
	public static class GolrDocument {
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
		List<String> synonym;
		List<String> evidence_with;
		List<String> reference;
	}

	protected GolrResponse parseGolrResponse(String response) throws IOException {
		try {
			GolrEnvelope envelope = GSON.fromJson(response, GolrEnvelope.class);
			if (envelope == null || envelope.response == null || envelope.responseHeader == null) {
				throw new IOException("Unexpected response content in GOLR response.");
			}
			if ("0".equals(envelope.responseHeader.status) == false) {
				throw new IOException("Unexpected response status in GOLR response header: "+envelope.responseHeader.status);
			}
			return envelope.response;
		} catch (JsonSyntaxException e) {
			throw new IOException("Could not parse JSON response.", e);
		}
	}
	
}
