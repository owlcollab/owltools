package org.bbop.golr.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationExtension.GolrAnnotationExtensionEntry.GolrAnnotationExtensionRelation;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class RetrieveGolrAnnotations {
	
	static boolean JSON_INDENT_FLAG = false;
	static int PAGINATION_CHUNK_SIZE = 100;
	
	private static final Gson GSON = new GsonBuilder().create();
	
	private final String server;
	private int retryCount;
	private final CloseableHttpClient httpclient;
	
	/*
	 * This flag indicates that missing c16 data, due to malformed JSON is acceptable. 
	 */
	private final boolean ignoreC16ParseErrors;

	public RetrieveGolrAnnotations(String server) {
		this(server, 3, false);
	}
	
	public RetrieveGolrAnnotations(String server, int retryCount, boolean ignoreC16ParseErrors) {
		this.server = server;
		this.retryCount = retryCount;
		HttpClientBuilder builder = HttpClientBuilder.create();
		// diss-allow automatic retry, we use a custom wait period
		builder.setRetryHandler(new HttpRequestRetryHandler() {
			
			@Override
			public boolean retryRequest(IOException exception, int executionCount,
					HttpContext context) {
				return false;
			}
		});
		httpclient = builder.build();
		this.ignoreC16ParseErrors = ignoreC16ParseErrors;
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
		List<String[]> tagvalues = new ArrayList<String[]>();
		String [] param1 = new String[2];
		param1[0] = "source";
		param1[1] = source;
		tagvalues.add(param1);
		String [] param2 = new String[2];
		param2[0] = "synonym";
		param2[1] = synonym;
		tagvalues.add(param2);
		
		final List<GolrAnnotationDocument> documents = getGolrAnnotations(tagvalues);

		return documents;
	}
	
	public List<GolrAnnotationDocument> getGolrAnnotations(List<String []> tagvalues) throws IOException {
		JSON_INDENT_FLAG = true;
		final URI uri = createGolrRequest(tagvalues, "annotation", 0, PAGINATION_CHUNK_SIZE);
		final String jsonString = getJsonStringFromUri(uri);
		final GolrResponse response = parseGolrResponse(jsonString);
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
				GolrResponse responsePagination = parseGolrResponse(jsonStringPagination);
				documents.addAll(Arrays.asList(responsePagination.docs));
				start += PAGINATION_CHUNK_SIZE;
			}
		}
		return documents;
	}
	
	URI createGolrRequest(List<String []> tagvalues, String category, int start, int pagination) throws IOException {
		try {
			URIBuilder builder = new URIBuilder(server);
			String currentPath = StringUtils.trimToEmpty(builder.getPath());
			builder.setPath(currentPath+"/select");
			builder.addParameter("defType", "edismax");
			builder.addParameter("qt", "standard");
			builder.addParameter("wt", "json");
			if (JSON_INDENT_FLAG) {
				builder.addParameter("indent","on");
			}
			// explicit list of fields, avoid "*" retrieval of unused fields
			builder.addParameter("fl",StringUtils.join(Arrays.asList(
					"source",
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
					"reference"), 
					','));
			builder.addParameter("facet","false");
			builder.addParameter("json.nl","arrarr");
			builder.addParameter("q","*:*");
			builder.addParameter("rows", Integer.toString(pagination));
			builder.addParameter("start", Integer.toString(start));
			builder.addParameter("fq", "document_category:\""+category+"\"");
			for (String [] tagvalue : tagvalues) {
				if (tagvalue.length == 2) {
					builder.addParameter("fq", tagvalue[0]+":\""+tagvalue[1]+"\"");
				}
				else if (tagvalue.length > 2) {
					// if there is more than one value, assume that this is an OR query
					StringBuilder value = new StringBuilder();
					value.append(tagvalue[0]).append(":(");
					for (int i = 1; i < tagvalue.length; i++) {
						if (i > 1) {
							value.append(" OR ");
						}
						value.append('"').append(tagvalue[i]).append('"');
					}
					value.append(')');
					builder.addParameter("fq", value.toString());
				}
			}
			return builder.build();
		} catch (URISyntaxException e) {
			throw new IOException("Could not build URI for Golr request", e);
		}
	}
	
	protected String getJsonStringFromUri(URI uri) throws IOException {
		logRequest(uri);
		return getJsonStringFromUri(uri, retryCount);
	}
	
	protected String getJsonStringFromUri(URI uri, int retryCount) throws IOException {
		HttpGet get = new HttpGet(uri);
		CloseableHttpResponse response = null;
		String content = null;
		IOException error = null;
		try {
			try {
				response = httpclient.execute(get);
			}
			catch (IOException exception) {
				error = exception;
			}
			if (error == null) {
				final StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() > 200) {
					error = new HttpResponseException(
							statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}
				else {
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						try {
							content = EntityUtils.toString(entity);
						} catch (IOException e) {
							error = e;
						}
					}
					else {
						error = new ClientProtocolException("Response contains no content");
					}
				}
			}
		} finally {
			IOUtils.closeQuietly(response);
		}
		if (error != null) {
			if (retryCount > 0) {
				int remaining = retryCount - 1;
				logRetry(uri, error, remaining);
				defaultRandomWait();
				return getJsonStringFromUri(uri, remaining);
			}
			logRequestError(uri, error);
			throw error;
		}
		if (content == null) {
			error = new ClientProtocolException("Response contains no content");
			logRequestError(uri, error);
			throw error;
		}
		return content;
	}
	
	protected void defaultRandomWait() {
		// wait a random interval between 400 and 1500 ms
		randomWait(400, 1500);
	}

	protected void randomWait(int min, int max) {
		Random random = new Random(System.currentTimeMillis());
		long wait = min + random.nextInt((max - min));
		try {
			Thread.sleep(wait);
		} catch (InterruptedException exception) {
			// ignore
		}
	}

	
	protected void logRequest(URI uri) {
		// do nothing
		// hook to implement logging of requests
	}
	
	protected void logRequestError(URI uri, IOException exception) {
		// do nothing
		// hook to implement logging of request errors
	}
	
	protected void logRetry(URI uri, IOException exception, int remaining) {
		// do nothing
		// hook to implement logging of a retry
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
		GolrAnnotationDocument[] docs;
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
