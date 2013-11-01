package owltools.sim2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class SimJSONTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(SimJSONTest.class);

	@Test
	public void testJSONOutput() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException, UnknownOWLClassException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);
		final int truncLen = 200;
		
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			sos = new FastOwlSim(sourceOntol);
			//sos.setReasoner(reasoner);
			LOG.info("Reasoner="+sos.getReasoner());

			SimJSONEngine sj = new SimJSONEngine(g, sos);

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				//System.out.println("COMPARING: "+i);
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					String jsonStr = sj.compareAttributeSetPair(
							sos.getAttributesForElement(i),
							sos.getAttributesForElement(j)
							);
					if(jsonStr.length() < truncLen)
						LOG.warn(jsonStr);
					else
						LOG.info("SAMPLE:"+jsonStr.substring(0,  truncLen));
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}



}
