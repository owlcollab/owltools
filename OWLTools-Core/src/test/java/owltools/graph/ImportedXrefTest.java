package owltools.graph;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

/**
 * Tests related to issue: http://code.google.com/p/owltools/issues/detail?id=82
 */
public class ImportedXrefTest extends OWLToolsTestBasics {

	@Test
	public void test() throws Exception {
		// load the base ontology
		ParserWrapper pw = new ParserWrapper();
		OWLOntology direct = pw.parseOBO(getResourceIRIString("graph/xref_test.obo"));
		
		OWLGraphWrapper directGraph = new OWLGraphWrapper(direct);
		
		// check that the test class has the expected number of xrefs
		OWLClass c = directGraph.getOWLClassByIdentifier("FOO:0001");
		
		List<String> directDefXrefs = directGraph.getDefXref(c);
		assertEquals(2, directDefXrefs.size());

		List<String> directXrefs = directGraph.getXref(c);
		assertEquals(2, directXrefs.size());
		
		// create an ontology using an import
		OWLOntologyManager manager = pw.getManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLOntology importer = manager.createOntology();
		OWLImportsDeclaration importDeclaration = factory.getOWLImportsDeclaration(direct.getOntologyID().getOntologyIRI().orElse(null));
		manager.applyChange(new AddImport(importer, importDeclaration));
		
		OWLGraphWrapper importerGraph = new OWLGraphWrapper(importer);
		
		// check that the wrapper uses also imports for lookups of xrefs
		List<String> importedDefXrefs = importerGraph.getDefXref(c);
		assertEquals(2, importedDefXrefs.size());

		List<String> importedXrefs = importerGraph.getXref(c);
		assertEquals(2, importedXrefs.size());
	}

}
