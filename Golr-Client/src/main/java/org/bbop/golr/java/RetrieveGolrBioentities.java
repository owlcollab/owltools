package org.bbop.golr.java;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RetrieveGolrBioentities extends AbstractRetrieveGolr {

	static int PAGINATION_CHUNK_SIZE = 100;
	
	private final List<String> relevantFields;
	
	public RetrieveGolrBioentities(String server, int retryCount) {
		super(server, retryCount);
		relevantFields = GolrBioentityDocument.getRelevantFields();
	}

	@Override
	protected boolean isIndentJson() {
		return true;
	}

	@Override
	protected List<String> getRelevantFields() {
		return relevantFields;
	}

	public List<GolrBioentityDocument> getGolrBioentites(String id) throws IOException {
		List<String[]> tagvalues = new ArrayList<String[]>();
		String [] tagvalue = new String[2];
		tagvalue[0] = "bioentity";
		tagvalue[1] = id;
		tagvalues.add(tagvalue);
		final List<GolrBioentityDocument> documents = getGolrBioentities(tagvalues);
		return documents;
	}
	
	public List<GolrBioentityDocument> getGolrBioentities(List<String []> tagvalues) throws IOException {
		final URI uri = createGolrRequest(tagvalues, "bioentity", 0, PAGINATION_CHUNK_SIZE);
		final String jsonString = getJsonStringFromUri(uri);
		final GolrResponse<GolrBioentityDocument> response = parseGolrResponse(jsonString);
		final List<GolrBioentityDocument> documents = new ArrayList<GolrBioentityDocument>(response.numFound);
		documents.addAll(Arrays.<GolrBioentityDocument>asList(response.docs));
		if (response.numFound > PAGINATION_CHUNK_SIZE) {
			// fetch remaining documents
			int start = PAGINATION_CHUNK_SIZE;
			int end = response.numFound / PAGINATION_CHUNK_SIZE;
			if (response.numFound % PAGINATION_CHUNK_SIZE != 0) {
				end += 1;
			}
			end = end * PAGINATION_CHUNK_SIZE;
			while (start <= end) {
				URI uriPagination = createGolrRequest(tagvalues, "bioentity", start, PAGINATION_CHUNK_SIZE);
				String jsonStringPagination = getJsonStringFromUri(uriPagination);
				GolrResponse<GolrBioentityDocument> responsePagination = parseGolrResponse(jsonStringPagination);
				documents.addAll(Arrays.asList(responsePagination.docs));
				start += PAGINATION_CHUNK_SIZE;
			}
		}
		return documents;
	}
	
	private static class GolrBioentityResponse extends GolrEnvelope<GolrBioentityDocument> {
		// empty
	}
	
	public static class GolrBioentityDocument {
		
		String document_category;
		String id;
		String bioentity;
		String bioentity_label;
		String bioentity_name;
		String source;
		String type;
		String taxon;
		String taxon_label;
		List<String> synonym;
		
		static List<String> getRelevantFields() {
			// explicit list of fields, avoid "*" retrieval of unused fields
			return Arrays.asList("document_category",
					"id",
					"bioentity",
					"bioentity_label",
					"bioentity_name",
					"source",
					"type",
					"taxon",
					"taxon_label",
					"synonym");
		}
	}
	
	private GolrResponse<GolrBioentityDocument> parseGolrResponse(String jsonString) throws IOException {
		return parseGolrResponse(jsonString, GolrBioentityResponse.class).response;
	}
}
