package owltools.test;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

/**
 * Tests the various wrapper get* methods in OWLGraphWrapper
 * 
 * @author cjm
 *
 */
public class FactPPTest extends OWLToolsTestBasics {

	@Test
	public void FactPPTest() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		for (int i=0; i<3; i++) {
			ParserWrapper pw = new ParserWrapper();
			OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
			FaCTPlusPlusReasonerFactory reasonerFactory = new FaCTPlusPlusReasonerFactory();
			OWLReasoner reasoner = reasonerFactory.createReasoner(g.getSourceOntology());
			for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
				for (OWLClass ec : reasoner.getSuperClasses(c, true).getFlattened()) {
					System.out.println(c+"\t"+ec);
				}
			}

		}
	}

}
