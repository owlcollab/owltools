package owltools;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests the various wrapper get* methods in OWLGraphWrapper
 * 
 * @author cjm
 *
 */
public class GetObjectsTest extends OWLToolsTestBasics {

	@Test
	public void testGetEntity() throws Exception {
		for (int i=0; i<3; i++) {
			ParserWrapper pw = new ParserWrapper();
			OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
			OWLOntology ont = g.getSourceOntology();
			OWLDataFactory df = g.getDataFactory();

			// no such entity exists in CARO
			IRI testIRI = IRI.create("http://purl.obolibrary.org/obo/FOO_987");
			String iriStr = testIRI.toString();

			OWLEntity c = null;
			if (i==0)
				c = df.getOWLClass(testIRI);
			else if (i==1)
				c = df.getOWLObjectProperty(testIRI);
			else if (i==2)
				c = df.getOWLNamedIndividual(testIRI);

			System.out.println("e="+c);
			if (true) {
				Set<OWLDeclarationAxiom> decls = ont.getDeclarationAxioms(c);
				assertTrue(decls.size() == 0);
				for (OWLDeclarationAxiom d : decls) {
					System.out.println(d);
				}
				// should return null as not declared
				assertTrue(g.getOWLObjectProperty(iriStr) == null);
				assertTrue(g.getOWLIndividual(iriStr) == null);
				assertTrue(g.getOWLClass(iriStr) == null);
			}



			OWLDeclarationAxiom dax = df.getOWLDeclarationAxiom(c);
			g.getManager().addAxiom(ont, dax);

			if (true) {
				Set<OWLDeclarationAxiom> decls = ont.getDeclarationAxioms(c);
				assertTrue(decls.size() == 1);
				for (OWLDeclarationAxiom d : decls) {
					System.out.println(d);
				}
				if (i==0) {
					assertTrue(g.getOWLObjectProperty(iriStr) == null);
					assertTrue(g.getOWLIndividual(iriStr) == null);
					assertTrue(g.getOWLClass(iriStr).getIRI().toString().equals(iriStr));
				}
				else if (i==1) {
					assertTrue(g.getOWLIndividual(iriStr) == null);
					assertTrue(g.getOWLClass(iriStr) == null);
					assertTrue(g.getOWLObjectProperty(iriStr).getIRI().toString().equals(iriStr));
				}
				else if (i==2) {
					assertTrue(g.getOWLObjectProperty(iriStr) ==null);
					assertTrue(g.getOWLIndividual(iriStr).getIRI().toString().equals(iriStr));
					assertTrue(g.getOWLClass(iriStr) == null);
				}
			}

		}
	}

}
