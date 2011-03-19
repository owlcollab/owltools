package owltools.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import junit.framework.TestCase;

public class AddImportTest extends TestCase {

	public static void testMakeImport() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); // persist?
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		IRI iri1 = IRI.create("file:test_resources/caro_mireot_test_noimport.owl");
		OWLOntology o1 = manager.loadOntologyFromOntologyDocument(iri1);
		IRI iri2 = IRI.create("file:test_resources/caro.owl");
		OWLOntology o2 = manager.loadOntologyFromOntologyDocument(iri2);
		
		AddImport ai = new AddImport(o1, dataFactory.getOWLImportsDeclaration(iri2));
		manager.applyChange(ai);
		
		for (OWLSubClassOfAxiom a : o1.getAxioms(AxiomType.SUBCLASS_OF, true)) {
			System.out.println(a);
		}
		for (OWLSubClassOfAxiom a : o2.getAxioms(AxiomType.SUBCLASS_OF, true)) {
			System.out.println("O2:"+a);
		}
	}
	
}
