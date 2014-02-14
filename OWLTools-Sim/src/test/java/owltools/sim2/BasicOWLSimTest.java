package owltools.sim2;

import java.io.IOException;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapperEdges;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim2.AbstractOWLSimTest;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.UnknownOWLClassException;
import owltools.sim2.io.FormattedRenderer;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class BasicOWLSimTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(BasicOWLSimTest.class);
	
	@Test
	public void testBasicSim() throws IOException, OWLOntologyCreationException, MathException, UnknownOWLClassException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResource("sim/mp-subset-1.obo").getAbsolutePath());
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);
		setOutput("target/basic-owlsim-test.out");
		
		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();
			
			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					showSim(i,j);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}

	@Test 
	public void testGetEntropy() throws OWLOntologyCreationException, IOException, OBOFormatParserException, UnknownOWLClassException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			owlsim = new SimpleOwlSim(sourceOntol);
			((SimpleOwlSim) owlsim).setReasoner(reasoner);

			reasoner.flush();
			Double e = owlsim.getEntropy();
			LOG.info("ENTROPY OF ONTOLOGY = "+e);

			for (String subset : g.getAllUsedSubsets()) {
				LOG.info("SUBSET:"+subset);
				e = owlsim.getEntropy(g.getOWLClassesInSubset(subset));
				LOG.info(" ENTROPY OF "+subset+" = "+e);
			}
		}
		finally {
			reasoner.dispose();
		}		
	}





}
