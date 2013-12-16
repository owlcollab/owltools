package owltools.renderer.markdown;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

public class MarkdownRendererTest extends OWLToolsTestBasics {

	@Test
	public void testRender() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper();

		MarkdownRenderer mr = new 		MarkdownRenderer();
		mr.render(g.getSourceOntology(), "target/caro");
	}

	@Ignore
	@Test
	public void testRenderUberon() throws Exception {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology o = 
				manager.loadOntologyFromOntologyDocument(new File("/Users/cjm/repos/uberon/uberon.owl"));
		MarkdownRenderer mr = new 		MarkdownRenderer();
		mr.render(o, "target/uberon");
	}


	private OWLGraphWrapper getOntologyWrapper() throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource("renderer/caro.owl"));
		return new OWLGraphWrapper(ontology);
	}
		
	
	
}
