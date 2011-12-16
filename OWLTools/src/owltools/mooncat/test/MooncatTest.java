package owltools.mooncat.test;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.test.OWLToolsTestBasics;

public class MooncatTest extends OWLToolsTestBasics {

	@Test
	public void testMireot() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		
		// this test ontology has a class defined using a caro class, and imports caro_local
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("mooncat_caro_test.obo"));
		Mooncat m = new Mooncat(g);
		m.addReferencedOntology(pw.parseOWL("http://purl.obolibrary.org/obo/caro.owl"));
		

		for (OWLEntity e : m.getExternalReferencedEntities()) {
			System.out.println("e="+e);
		}
		for (OWLObject e : m.getClosureOfExternalReferencedEntities()) {
			System.out.println("c="+e);
		}
		for (OWLAxiom ax : m.getClosureAxiomsOfExternalReferencedEntities()) {
			System.out.println("M_AX:"+ax);
		}
		
		m.mergeOntologies();
		for (OWLAxiom ax : m.getOntology().getAxioms()) {
			System.out.println(ax);
		}
	}
	
}
