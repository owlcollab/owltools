package owltools.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import junit.framework.TestCase;

public class FlyTest extends TestCase {

	public static void testConvertXPs() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("http://purl.org/obo/obo/FBbt.obo");
		OWLOntology ont = g.getOntology();
		OWLObject wmb = g.getOWLObjectByIdentifier("FBbt:00004326"); // wing margin bristle
		OWLObject eso = g.getOWLObjectByIdentifier("FBbt:00005168"); // external sensory organ
		
		Set<OWLObject> ancs = g.getAncestorsReflexive(wmb);
		assertTrue(ancs.contains(wmb)); // reflexivity test
		assertTrue(ancs.contains(eso)); //wing margin bristle --> external sensory organ
		
		for (OWLObject c : ancs) {
			System.out.println(g.getIdentifier(c)+" "+g.getLabel(c)+" URI:"+c);
		}
}
	
}
