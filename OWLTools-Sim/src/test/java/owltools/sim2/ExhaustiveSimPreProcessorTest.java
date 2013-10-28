package owltools.sim2;

import java.io.IOException;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class ExhaustiveSimPreProcessorTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(ExhaustiveSimPreProcessorTest.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = manager.getOWLDataFactory();
	OWLOntology sourceOntol;
	SimpleOwlSim sos;
	OWLGraphWrapper g;



	@Test
	public void exhaustiveTestOnGO() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException {
		/*
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResourceIRIString("go-subset-t1.obo"));
		g = new OWLGraphWrapper(sourceOntol);
		IRI vpIRI = g.getOWLObjectPropertyByIdentifier("GOTESTREL:0000001").getIRI();
		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.property = vpIRI;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse("src/test/resources/simplegaf-t1.txt");

		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		
		AutomaticSimPreProcessor pproc = new AutomaticSimPreProcessor();
		pproc.setInputOntology(sourceOntol);
		pproc.setOutputOntology(sourceOntol);
		pproc.preprocess();
		*/
	}

	


}
