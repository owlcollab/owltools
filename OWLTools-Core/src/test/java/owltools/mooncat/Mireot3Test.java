package owltools.mooncat;

import java.io.IOException;
import java.util.Set;

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

	private static boolean RENDER_ONTOLOGY_FLAG = false;
	
	@Test
	public void testMireot() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		
		// this test ontology has a class defined using a caro class, and imports caro_local
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro_mireot_test.owl"), true);
		g.getSourceOntology();
		
		Mooncat m = new Mooncat(g);
		g.addSupportOntologiesFromImportsClosure();

		final Set<OWLOntology> referencedOntologies = m.getReferencedOntologies();
		final Set<OWLEntity> externalReferencedEntities = m.getExternalReferencedEntities();
		final Set<OWLObject> closureOfExternalReferencedEntities = m.getClosureOfExternalReferencedEntities();
		final Set<OWLAxiom> closureAxiomsOfExternalReferencedEntities = m.getClosureAxiomsOfExternalReferencedEntities();
		
		if (RENDER_ONTOLOGY_FLAG) {
			for (OWLOntology o : referencedOntologies) {
				System.out.println("ref="+o);
			}
			
			for (OWLEntity e : externalReferencedEntities) {
				System.out.println("e="+e);
			}
			
			for (OWLObject e : closureOfExternalReferencedEntities) {
				System.out.println("c="+e);
			}
			
			for (OWLAxiom ax : closureAxiomsOfExternalReferencedEntities) {
				System.out.println("M_AX:"+ax);
			}
		}
	}
	
}
