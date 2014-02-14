package owltools.web;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.sim2.FastOwlSimFactory;
import owltools.sim2.OwlSim;
import owltools.sim2.OwlSimFactory;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.SimpleOwlSimFactory;
import owltools.sim2.preprocessor.ABoxUtils;
import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;

/**
 * Simple test for the client server communication for the OWLTools web server.
 */
public class OWLServerSimSearchTest {

	OWLGraphWrapper g;
	@Test
	public void testServerCommunication() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);
		// set sim
		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());
//		SimPreProcessor pproc = new NullSimPreProcessor();
//		pproc.setInputOntology(g.getSourceOntology());
//		pproc.setOutputOntology(g.getSourceOntology());
//		if (sos instanceof SimpleOwlSim)
//			((SimpleOwlSim) sos).setSimPreProcessor(pproc);
		sos.createElementAttributeMapFromOntology();
		// TODO	attributeAllByAllOld(opts);
		
		// create server
		Server server = new Server(9031);
		server.setHandler(new OWLServer(g, sos));
		try {
			server.start();

			// create a client
			HttpClient httpclient = new DefaultHttpClient();

			// prepare a request
			//final HttpUriRequest httpUriRequest = createRequest(200);
			HttpUriRequest httppost = createRequest(5);

			// run request
			Log.info("Executing="+httppost);
			//HttpResponse response = httpclient.execute(httpUriRequest);
			HttpResponse response = httpclient.execute(httppost);
			Log.info("Executed="+httpclient);
			
			// check response
			HttpEntity entity = response.getEntity();
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				Log.info("Status="+statusLine.getStatusCode());
				EntityUtils.consumeQuietly(entity);
			}

			// prepare a request
			//final HttpUriRequest httpUriRequest = createRequest(200);
			httppost = createBogusRequest(5);

			// run request
			Log.info("Executing="+httppost);
			//HttpResponse response = httpclient.execute(httpUriRequest);
			response = httpclient.execute(httppost);
			Log.info("Executed="+httpclient);
			
			// check response
			entity = response.getEntity();
			statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				Log.info("Status="+statusLine.getStatusCode());
				EntityUtils.consumeQuietly(entity);
			}

			
			
		}
		finally {
			// clean up
			server.stop();

//			if (pproc != null) {
//				pproc.dispose();
//			}
		}
	}
	

	protected OWLGraphWrapper loadOntology(String location) throws Exception {
		// load server ontology
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(location);
		
		// prepare ontology
		ABoxUtils.makeDefaultIndividuals(g.getSourceOntology());
		return g;
	}

	protected HttpUriRequest createRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/searchByAttributeSet/");
			
		int i=0;
		for (OWLClass c : g.getAllOWLClasses()) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);
			i++;
			if (i >= n)
				break;
		}
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		Log.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		Log.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	protected HttpUriRequest createBogusRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/searchByAttributeSet/");
			
		uriBuilder.addParameter("a", "BOGUS:1234567");
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		Log.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		Log.info("Got URL="+uri);
		return httpUriRequest;
	}


	/**
	 * @param responseContent
	 */
	protected void handleResponse(String responseContent) {
		if (responseContent.length() > 10000) {
			System.out.println("0.."+responseContent.length() + " => "+responseContent.substring(0, 10000));
		}
		else {
			System.out.println(responseContent);
		}
	}
	
	
	@Test
	public void testGetAnnotationSufficiencyScore() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);
		// set sim
		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());
//		SimPreProcessor pproc = new NullSimPreProcessor();
//		pproc.setInputOntology(g.getSourceOntology());
//		pproc.setOutputOntology(g.getSourceOntology());
//		if (sos instanceof SimpleOwlSim)
//			((SimpleOwlSim) sos).setSimPreProcessor(pproc);
		sos.createElementAttributeMapFromOntology();
		// TODO	attributeAllByAllOld(opts);
		
		// create server
		Server server = new Server(9031);
		server.setHandler(new OWLServer(g, sos));
		try {
			server.start();

			// create a client
			HttpClient httpclient = new DefaultHttpClient();

			// prepare a request
			//final HttpUriRequest httpUriRequest = createRequest(200);
			HttpUriRequest httppost = createGoodAnnotSufRequest(5);

			// run request
			Log.info("Executing="+httppost);
			//HttpResponse response = httpclient.execute(httpUriRequest);
			HttpResponse response = httpclient.execute(httppost);
			Log.info("Executed="+httpclient);
			
			// check response
			HttpEntity entity = response.getEntity();
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				Log.info("Status="+statusLine.getStatusCode());
				EntityUtils.consumeQuietly(entity);
			}

			// prepare a request
			//final HttpUriRequest httpUriRequest = createRequest(200);
			httppost = createBogusAnnotSufRequest(5);

			// run request
			Log.info("Executing="+httppost);
			//HttpResponse response = httpclient.execute(httpUriRequest);
			response = httpclient.execute(httppost);
			Log.info("Executed="+httpclient);
			
			// check response
			entity = response.getEntity();
			statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				Log.info("Status="+statusLine.getStatusCode());
				EntityUtils.consumeQuietly(entity);
			}
			
			httppost = createPartiallyBogusAnnotSufRequest(5);

			// run request
			Log.info("Executing="+httppost);
			//HttpResponse response = httpclient.execute(httpUriRequest);
			response = httpclient.execute(httppost);
			Log.info("Executed="+httpclient);
			
			// check response
			entity = response.getEntity();
			statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				Log.info("Status="+statusLine.getStatusCode());
				EntityUtils.consumeQuietly(entity);
			}

			
			
		}
		finally {
			// clean up
			server.stop();

//			if (pproc != null) {
//				pproc.dispose();
//			}
		}
	}
	
	protected HttpUriRequest createGoodAnnotSufRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAnnotationSufficiencyScore/");
			
		int i=0;
		for (OWLClass c : g.getAllOWLClasses()) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);
			i++;
			if (i >= n)
				break;
		}
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		Log.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		Log.info("Got URL="+uri);
		return httpUriRequest;
	}

	protected HttpUriRequest createBogusAnnotSufRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAnnotationSufficiencyScore/");
			
		uriBuilder.addParameter("a", "BOGUS:1234567");
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		Log.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		Log.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	protected HttpUriRequest createPartiallyBogusAnnotSufRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAnnotationSufficiencyScore/");
		int i=0;
		for (OWLClass c : g.getAllOWLClasses()) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);
			uriBuilder.addParameter("a", "BOGUS:000000"+i);
			i++;
			if (i >= n)
				break;
		}
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		Log.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		Log.info("Got URL="+uri);
		return httpUriRequest;
	}
}
