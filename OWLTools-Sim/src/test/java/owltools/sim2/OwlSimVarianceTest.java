package owltools.sim2;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.sim2.kb.OWLSimReferenceBasedStatistics;
import owltools.sim2.kb.PValue;

public class OwlSimVarianceTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(OwlSimVarianceTest.class);

	@Test
	public void testSingleVar() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResource("sim/mp-subset-1.obo").getAbsolutePath());
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		LOG.info("Initialize OwlSim ..");

		OWLReasoner reasoner = new ReasonerFactory().createReasoner(sourceOntol);
		reasoner.flush();
		
		try {
			owlsim = owlSimFactory.createOwlSim(sourceOntol);
			owlsim.createElementAttributeMapFromOntology();
			owlsim.computeSystemStats();
		} catch (UnknownOWLClassException e) {
			e.printStackTrace();
		} finally {
			reasoner.dispose();
		}
		
		// Source sourceOntol and g are used as background knowledge ... 
		OWLSimReferenceBasedStatistics refBasedStats = new OWLSimReferenceBasedStatistics(owlsim, sourceOntol, g);
		
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/MGI_101761");
		
		String[] testClasses = new String[] {"MP:0002758", "MP:0002772", "MP:0005448", "MP:0003660"};
		Set<OWLClass> testClassesSet = new HashSet<OWLClass>();
		for (String testClass : testClasses) {
			testClassesSet.add(this.getOBOClass(testClass));
		}

		double varValue = refBasedStats.getVariance(testClassesSet, iri);
		LOG.info(varValue);

		testClasses = new String[] {"MP:0000889", "MP:0002739"};
		testClassesSet = new HashSet<OWLClass>();
		for (String testClass : testClasses) {
			testClassesSet.add(this.getOBOClass(testClass));
		}

		varValue = refBasedStats.getVariance(testClassesSet, iri);
		LOG.info(varValue);

	}

	@Test
	public void testPValue() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResource("sim/mp-subset-1.obo").getAbsolutePath());
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		LOG.info("Initialize OwlSim ..");

		OWLReasoner reasoner = new ReasonerFactory().createReasoner(sourceOntol);
		reasoner.flush();
		
		try {
			owlsim = owlSimFactory.createOwlSim(sourceOntol);
			owlsim.createElementAttributeMapFromOntology();
			owlsim.computeSystemStats();
		} catch (UnknownOWLClassException e) {
			e.printStackTrace();
		} finally {
			reasoner.dispose();
		}

		// Source sourceOntol and g are used as background knowledge ... 
		OWLSimReferenceBasedStatistics refBasedStats = new OWLSimReferenceBasedStatistics(owlsim, sourceOntol, g);
		
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/MGI_101761");
		
		String[] testClasses = new String[] {"MP:0002758", "MP:0002772", "MP:0005448", "MP:0003660"};
		Set<OWLClass> testClassesSet = new HashSet<OWLClass>();
		for (String testClass : testClasses) {
			testClassesSet.add(this.getOBOClass(testClass));
		}

		PValue pValue = refBasedStats.getPValue(testClassesSet, iri);
		LOG.info(pValue.getSimplePValue());

	}
}
