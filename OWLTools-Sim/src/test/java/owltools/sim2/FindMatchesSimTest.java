package owltools.sim2;

import java.io.IOException;
import java.util.List;

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
import owltools.sim2.scores.ElementPairScores;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class FindMatchesSimTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(FindMatchesSimTest.class);

	@Test
	public void testBasicSim() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException, UnknownOWLClassException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);
		setOutput("target/find-matches-test.out");

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {

				renderer.getResultOutStream().println("\nI = "+i);
				
				List<ElementPairScores> scoreSets = owlsim.findMatches(i, "MGI");
				int rank = 1;
				for (ElementPairScores s : scoreSets) {
					renderer.getResultOutStream().println("\n  RANK = "+rank);
					renderer.printPairScores(s);
					rank++;
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}

	


}
