package owltools.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.junit.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.sim2.FastOwlSimFactory;
import owltools.sim2.OwlSim;
import owltools.sim2.OwlSimFactory;
import owltools.sim2.preprocessor.ABoxUtils;

/**
 * Tests for the client server communication for the OWLTools web server,
 * specifically for {@link searchByAttributeSetCommand} and {@link getAttributeInformationProfileCommand}
 * and {@link getAnnotationSufficiencyScoreCommand}
 */
public class OWLServerSimSearchTest {

	private Logger LOG = Logger.getLogger(OWLServerSimSearchTest.class);
	
	OWLGraphWrapper g;
	
	
	@Test
	public void testServerCompareByAttributeSetsCommand() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		final HttpUriRequest httppost = createCompareByAttributeSetsRequest(4,6);
		runServerCommunication(httppost,sos);

	}
	
	@Test
	public void testServerSearchByAttributeSetCommand() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		final HttpUriRequest httppost = createSearchByAttributeSetRequest(5);
		runServerCommunication(httppost,sos);

	}
	
	@Test
	public void testServerSearchByAttributeSetCommandBad() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		final HttpUriRequest httppost = createBogusRequest(5);

		runServerCommunication(httppost,sos);
	}
	
	@Test
	public void testServerGetAnnotationSufficiencyScoreCommand() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		HttpUriRequest httppost = createGoodAnnotSufRequest(5);

		runServerCommunication(httppost,sos);

	}
	
	@Test
	public void testServerGetAnnotationSufficiencyScoreCommandBadPartial() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		HttpUriRequest httppost = createPartiallyBogusAnnotSufRequest(5);

		runServerCommunication(httppost,sos);

	}
	
	@Test
	public void testServerGetAnnotationSufficiencyScoreCommandBad() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);
		// set sim
		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		HttpUriRequest httppost = createBogusAnnotSufRequest(5);

		runServerCommunication(httppost,sos);

	}
	
	@Test
	public void testServerGetAnnotationProfileWithSubgraphsCommand() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		HttpUriRequest httppost = createGoodAnnotProfileWithSubgraphsRequest(5);

		runServerCommunication(httppost,sos);

	}

	@Test
	public void testServerGetAnnotationProfileNoSubgraphsCommand() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		HttpUriRequest httppost = createGoodAnnotProfileRequest(5);

		runServerCommunication(httppost,sos);

	}

	
	@Test
	public void testServerGetAnnotationProfileCommandBad() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		
		ABoxUtils.createRandomClassAssertions(g.getSourceOntology(), 200, 20);

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
		
		HttpUriRequest httppost = createBadAnnotProfileWithSubgraphsRequest(5);

		runServerCommunication(httppost,sos);

	}
	
	
	@Test
	public void testServerGetCoAnnotationSuggestions() throws Exception {
		g = loadOntology("../OWLTools-Sim/src/test/resources/sim/mp-subset-1.obo");
		String file="../OWLTools-Sim/src/test/resources/sim/mgi-gene2mp-subset-1.tbl";
//		g = loadOntology("/Users/Nicole/work/MONARCH/phenotype-ontologies/src/ontology/hp.obo");
//		String file="/Users/Nicole/work/MONARCH/phenotype-ontologies/data/Homo_sapiens/Hs-disease-to-phenotype-O.txt";

		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse(file);			

		OwlSimFactory owlSimFactory = new FastOwlSimFactory();
		OwlSim sos = owlSimFactory.createOwlSim(g.getSourceOntology());

		sos.createElementAttributeMapFromOntology();
//		sos.populateFullCoannotationMatrix();
		LOG.info("Finished populating the big matrix");
		
		HttpUriRequest httppost = createGoodCoAnnotationRequest(1);

		runServerCommunication(httppost,sos);

	}
	
	
	/**
	 * @param httppost A well-formed {@link HttpUriRequest}
	 * @param owlsim An initialized {@link OWLSim} instance.  It is expected that the {@link OWLGraphWrapper} is already loaded with an ontology.
	 * @throws Exception
	 */
	protected void runServerCommunication(HttpUriRequest httppost, OwlSim owlsim) throws Exception {
		// create server
		Server server = new Server(9031);
		server.setHandler(new OWLServer(g, owlsim));
		try {
			server.start();

			// create a client
			HttpClient httpclient = new DefaultHttpClient();

			// run request
			LOG.info("Executing="+httppost);
			HttpResponse response = httpclient.execute(httppost);
			LOG.info("Executed="+httpclient);
			
			// check response
			HttpEntity entity = response.getEntity();
			StatusLine statusLine = response.getStatusLine();
			LOG.info("Status="+statusLine.getStatusCode());
			if (statusLine.getStatusCode() == 200) {
				String responseContent = EntityUtils.toString(entity);
				handleResponse(responseContent);
			}
			else {
				EntityUtils.consumeQuietly(entity);
			}			
		}
		finally {
			// clean up
			server.stop();
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

	protected HttpUriRequest createSearchByAttributeSetRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/searchByAttributeSet/");
			
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			//get at least one ancestor of each class
			i++;
			if (i >= n)
				break;
		}
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	
	protected HttpUriRequest createCompareByAttributeSetsRequest(int nA, int nB) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/compareAttributeSets/");
			
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			//get at least one ancestor of each class
			i++;
			if (i >= nA)
				break;
		}
		
		Collections.shuffle(allClasses);
		i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("b", id);

			//get at least one ancestor of each class
			i++;
			if (i >= nB)
				break;
		}

		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
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
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
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
	
	

	
	protected HttpUriRequest createGoodAnnotSufRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAnnotationSufficiencyScore/");
			
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			i++;
			if (i >= n)
				break;
		}
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
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
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
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
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	protected HttpUriRequest createGoodAnnotProfileRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAttributeInformationProfile/");
			
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			i++;
			if (i >= n)
				break;
		}
		
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	
	protected HttpUriRequest createGoodAnnotProfileWithSubgraphsRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAttributeInformationProfile/");
			
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			//get at least one ancestor of each class
			Set<OWLObject> ancestors = g.getAncestors(c);
			if (!ancestors.isEmpty()) {
				OWLClass r = (OWLClass) ancestors.iterator().next();
				uriBuilder.addParameter("r", g.getIdentifier(r));
				i++;
				if (i >= n)
					break;
			}
		}

		
		Random rand = new Random();
		int r = 0;
		//get some random classes
		for (i=0; i< n ; i++) {
			r = rand.nextInt(allClasses.size());
			OWLClass c = allClasses.get(r);
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("r", id);
			i++;
			if (i >= n)
				break;
		}
		
		
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	protected HttpUriRequest createBadAnnotProfileWithSubgraphsRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
			.setPath("/owlsim/getAttributeInformationProfile/");
			
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllRealOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;

		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			//get at least one ancestor of each class
			OWLClass r = (OWLClass) g.getAncestors(c).iterator().next();
			uriBuilder.addParameter("r", g.getIdentifier(r));
			i++;
			if (i >= n)
				break;
		}

		uriBuilder.addParameter("a", "BOGUS:1234567");
		uriBuilder.addParameter("r", "BOGUS:0000003");

		
		Random rand = new Random();
		int r = 0;
		//get some random classes
		for (i=0; i< n ; i++) {
			r = rand.nextInt(allClasses.size());
			OWLClass c = allClasses.get(r);
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("r", id);
			i++;
			if (i >= n)
				break;
		}
		
		
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}
	
	protected HttpUriRequest createGoodCoAnnotationRequest(int n) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("localhost").setPort(9031)
//			.setPath("/owlsim/getCoAnnotationListForAttribute/");
			.setPath("/owlsim/getCoAnnotatedClasses/");
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		allClasses.addAll(g.getAllOWLClasses());
		Collections.shuffle(allClasses);
		int i=0;
/*
		for (OWLClass c : allClasses) {
			String id = g.getIdentifier(c);
			uriBuilder.addParameter("a", id);

			i++;
			if (i >= n)
				break;
		} */
//		uriBuilder.addParameter("a","HP:0001252");
//		uriBuilder.addParameter("a","HP:0001250");
//		uriBuilder.addParameter("a","HP:0000252");
		uriBuilder.addParameter("a","MP:0002082");
		uriBuilder.addParameter("limit","5");
		URI uri = uriBuilder.build();
		LOG.info("Getting URL="+uri);
		HttpUriRequest httpUriRequest = new HttpGet(uri);
		LOG.info("Got URL="+uri);
		return httpUriRequest;
	}
	
}
