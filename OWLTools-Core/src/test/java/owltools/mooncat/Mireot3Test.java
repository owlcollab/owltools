package owltools.mooncat;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;

public class Mireot3Test extends OWLToolsTestBasics {

	@Test
	public void testMireot() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		
		// this test ontology has a class defined using a caro class, and imports caro_local
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro_mireot_test.owl"), true);
		OWLOntology ont = g.getSourceOntology();
		
		Mooncat m = new Mooncat(g);
		g.addSupportOntologiesFromImportsClosure();
	
		for (OWLOntology o : m.getReferencedOntologies()) {
			System.out.println("ref="+o);
		}
		
		for (OWLEntity e : m.getExternalReferencedEntities()) {
			System.out.println("e="+e);
		}
		for (OWLObject e : m.getClosureOfExternalReferencedEntities()) {
			System.out.println("c="+e);
		}
		for (OWLAxiom ax : m.getClosureAxiomsOfExternalReferencedEntities()) {
			System.out.println("M_AX:"+ax);
		}
	}
	
}
