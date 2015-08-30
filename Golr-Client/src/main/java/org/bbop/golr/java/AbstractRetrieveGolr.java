package org.bbop.golr.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public abstract class AbstractRetrieveGolr {
	
	protected static final Gson GSON = new GsonBuilder().create();
	
	private final String server;
	private int retryCount;
	
	public AbstractRetrieveGolr(String server) {
		this(server, 3);
	}
	
	public AbstractRetrieveGolr(String server, int retryCount) {
		this.server = server;
		this.retryCount = retryCount;
	}
	
	protected abstract boolean isIndentJson();
	
	protected abstract List<String> getRelevantFields();
	
	URI createGolrRequest(List<String []> tagvalues, String category, int start, int pagination) throws IOException {
		try {
			URIBuilder builder = new URIBuilder(server);
			String currentPath = StringUtils.trimToEmpty(builder.getPath());
			builder.setPath(currentPath+"/select");
			builder.addParameter("defType", "edismax");
			builder.addParameter("qt", "standard");
			builder.addParameter("wt", "json");
			if (isIndentJson()) {
				builder.addParameter("indent","on");
			}
			builder.addParameter("fl",StringUtils.join(getRelevantFields(), ','));
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
		final URL url = uri.toURL();
		final HttpURLConnection connection;
		InputStream response = null;
		// setup and open (actual connection)
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(true); // warning does not follow redirects from http to https
			response = connection.getInputStream(); // opens the connection to the server
		}
		catch (IOException e) {
			IOUtils.closeQuietly(response);
			return retryRequest(uri, e, retryCount);
		}
		// check status code
		final int status;
		try {
			status = connection.getResponseCode();
		} catch (IOException e) {
			IOUtils.closeQuietly(response);
			return retryRequest(uri, e, retryCount);
		}
		// handle unexpected status code
		if (status != 200) {
			// try to check error stream
			String errorMsg = getErrorMsg(connection);
			
			// construct message for exception
			StringBuilder sb = new StringBuilder("Unexpected HTTP status code: "+status);
			
			if (errorMsg != null) {
				sb.append(" Details: ");
				sb.append(errorMsg);
			}
			IOException e = new IOException(sb.toString());
			return retryRequest(uri, e, retryCount);
		}
		
		// try to detect charset
		String contentType = connection.getHeaderField("Content-Type");
		String charset = null;

		if (contentType != null) {
			for (String param : contentType.replace(" ", "").split(";")) {
				if (param.startsWith("charset=")) {
					charset = param.split("=", 2)[1];
					break;
				}
			}
		}

		// get string response from stream
		String json;
		try {
			if (charset != null) {
				json = IOUtils.toString(response, charset);
			}
			else {
				json = IOUtils.toString(response);
			}
		} catch (IOException e) {
			return retryRequest(uri, e, retryCount);
		}
		finally {
			IOUtils.closeQuietly(response);
		}
		return json;
	}

	protected String retryRequest(URI uri, IOException e, int retryCount) throws IOException {
		if (retryCount > 0) {
			int remaining = retryCount - 1;
			defaultRandomWait();
			logRetry(uri, e, remaining);
			return getJsonStringFromUri(uri, remaining);
		}
		logRequestError(uri, e);
		throw e;
	}
	
	private static String getErrorMsg(HttpURLConnection connection) {
		String errorMsg = null;
		InputStream errorStream = null;
		try {
			errorStream = connection.getErrorStream();
			if (errorStream != null) {
				errorMsg =IOUtils.toString(errorStream);
			}
			errorMsg = StringUtils.trimToNull(errorMsg);
		}
		catch (IOException e) {
			// ignore errors, while trying to retrieve the error message
		}
		finally {
			IOUtils.closeQuietly(errorStream);
		}
		return errorMsg;
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
	
	
	protected <T extends GolrEnvelope<?>> T parseGolrResponse(String response, Class<T> clazz) throws IOException {
		try {
			T envelope = GSON.fromJson(response, clazz);
			if (envelope == null || envelope.response == null || envelope.responseHeader == null) {
				throw new IOException("Unexpected response content in GOLR response.");
			}
			if ("0".equals(envelope.responseHeader.status) == false) {
				throw new IOException("Unexpected response status in GOLR response header: "+envelope.responseHeader.status);
			}
			return envelope;
		} catch (JsonSyntaxException e) {
			throw new IOException("Could not parse JSON response.", e);
		}
	}
	
	static class GolrEnvelope<T> {
		GolrResponseHeader responseHeader;
		GolrResponse<T> response;
	}
	
	static class GolrResponseHeader {
		String status;
		String QTime;
		Object params;
	}
	
	static class GolrResponse<T> {
		int numFound;
		int start;
		T[] docs;
	}
	
}
