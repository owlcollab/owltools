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
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.sim2.OwlSim;
import owltools.sim2.OwlSimFactory;
import owltools.sim2.SimpleOwlSimFactory;
import owltools.sim2.preprocessor.ABoxUtils;

/**
 * Simple test for the client server communication for the OWLTools web server.
 */
public class OWLServerTest {
	
	private static final Logger LOG = Logger.getLogger(OWLServerTest.class);

	OWLGraphWrapper g;
	@Test
	public void testServerCommunication() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		// set sim
		OwlSimFactory owlSimFactory = new SimpleOwlSimFactory();
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
			HttpUriRequest httppost = createPostRequest(300);

			// run request
			LOG.info("Executing="+httppost);
			//HttpResponse response = httpclient.execute(httpUriRequest);
			HttpResponse response = httpclient.execute(httppost);
			LOG.info("Executed="+httpclient);
			
			// check response
			HttpEntity entity = response.getEntity();
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				LOG.info("Status="+statusLine.getStatusCode());
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
			.setPath("/owlsim/compareAttributeSets/")
			.setParameter("a", "MP:0000001")
			.setParameter("b", "MP:0000003");
			
		int i=0;
		for (OWLClass c : g.getAllOWLClasses()) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);
			uriBuilder.addParameter("b", id);
			i++;
			if (i >= n)
				break;
		}
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}

	protected HttpUriRequest createPostRequest(int n) throws URISyntaxException, UnsupportedEncodingException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/compareAttributeSets/");
		
		HttpPost httpost = new HttpPost(uriBuilder.build());
	
			//.setParameter("a", "MP:0000001")
			//.setParameter("b", "MP:0000003");
			
		int i=0;
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		for (OWLClass c : g.getAllOWLClasses()) {
			String id = g.getIdentifier(c);
		       
			nvps.add(new BasicNameValuePair("a", id));
			nvps.add(new BasicNameValuePair("b", id));
			i++;
			if (i >= n)
				break;
		}
		httpost.setEntity((HttpEntity) new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

		return httpost;
	}
	/**
	 * @param responseContent
	 */
	protected void handleResponse(String responseContent) {
		if (responseContent.length() > 1000) {
			System.out.println("0.."+responseContent.length() + " => "+responseContent.substring(0, 1000));
		}
		else {
			System.out.println(responseContent);
		}
	}
}
