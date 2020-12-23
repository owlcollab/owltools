package owltools.sim2;

import java.util.List;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim2.FastOwlSim.ClassCount;

/**
 * This is the main test class for term suggestions based on co-annotation 
 * frequency using the TF-IDF methods
 * 
 * @author nlw
 *
 */
public class CoannotatedTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(CoannotatedTest.class);


	/**
	 * Test fetching commonly co-annotated classes in a set of individuals 
	 * vs the background (entire) corpus using the TF-IDF method.
	 * @throws Exception
	 * @throws MathException
	 */
	@Test
	public void coAnnotatedTest() throws Exception, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ReasonerFactory().createReasoner(sourceOntol);
		try {

			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();

			reasoner.flush();
			owlsim.populateFullCoannotationMatrix();
			Set<OWLNamedIndividual> inds = sourceOntol.getIndividualsInSignature();
			for (OWLNamedIndividual i : inds) {
				//get a random set of other individuals to do the subset
				List<ClassCount> coaClasses = owlsim.getCoAnnotatedClassesForIndividual(i);
				LOG.info("Found "+coaClasses.size()+" coannotated classes for "+g.getIdentifier(i));
				for (ClassCount cc : coaClasses) {
					LOG.info(owlpp.render(cc.c)+cc.score);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
		

	}
	
	/**
	 * Test fetching commonly co-annotated classes in the entire corpus, which
	 * uses the TF-IDF method.
	 * @throws Exception
	 */
	@Test
	public void testGetCoannotatedClassesForAttribute() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ReasonerFactory().createReasoner(sourceOntol);
		try {

			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();

			reasoner.flush();
			owlsim.populateFullCoannotationMatrix();
			Set<OWLNamedIndividual> inds = sourceOntol.getIndividualsInSignature();
			Set<OWLClass> cs = owlsim.getAllAttributeClasses();
			LOG.info("Dumping coannotations and scores for all annotated classes");
			for (OWLClass c : cs) {
				List<ClassCount> lcc = owlsim.getCoannotatedClassesForAttribute(c,inds.size());
				for (ClassCount cc : lcc) {
					LOG.info(owlpp.render(c)+owlpp.render(cc.c)+cc.score);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}

}
