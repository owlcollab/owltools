package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class CoreMolecularModelManagerTest {

	@Test
	public void testUpdateImports() throws Exception {
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		final OWLDataFactory f = m.getOWLDataFactory();
		
		// setup other import
		final IRI other = IRI.generateDocumentIRI();
		m.createOntology(other);
		
		// setup obsolete
		final IRI obs1 = IRI.generateDocumentIRI();
		final IRI obs2 = IRI.generateDocumentIRI();
		final IRI obs3 = IRI.generateDocumentIRI();
		final Map<IRI, OWLOntology> obsoletes = new HashMap<IRI, OWLOntology>();
		obsoletes.put(obs1, m.createOntology(obs1));
		obsoletes.put(obs2, m.createOntology(obs2));
		obsoletes.put(obs3, m.createOntology(obs3));
		
		// setup additional
		final IRI add1 = IRI.generateDocumentIRI();
		m.createOntology(add1);
		final IRI add2 = IRI.generateDocumentIRI();
		m.createOntology(add2);
		final Set<IRI> additional = new HashSet<IRI>();
		additional.add(add1);
		additional.add(add2);
		
		// setup tbox
		final IRI tboxIRI = IRI.generateDocumentIRI();
		m.createOntology(tboxIRI);
		
		// setup abox
		final OWLOntology abox = m.createOntology(IRI.generateDocumentIRI());
		// add initial imports to abox
		m.applyChange(new AddImport(abox, f.getOWLImportsDeclaration(other)));
		m.applyChange(new AddImport(abox, f.getOWLImportsDeclaration(obs1)));
		m.applyChange(new AddImport(abox, f.getOWLImportsDeclaration(obs2)));
		m.applyChange(new AddImport(abox, f.getOWLImportsDeclaration(obs3)));
		
		// update imports
		CoreMolecularModelManager.updateImports(abox, tboxIRI, additional, obsoletes);
		
		// check the resulting imports
		Set<OWLImportsDeclaration> declarations = abox.getImportsDeclarations();
		assertEquals(4, declarations.size());
		Set<IRI> declaredImports = new HashSet<IRI>();
		for (OWLImportsDeclaration importsDeclaration : declarations) {
			declaredImports.add(importsDeclaration.getIRI());
		}
		assertEquals(4, declaredImports.size());
		assertTrue(declaredImports.contains(tboxIRI));
		assertTrue(declaredImports.contains(add1));
		assertTrue(declaredImports.contains(add1));
		assertTrue(declaredImports.contains(other));
	}

}
