package owltools.io;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.io.OWLGsonRenderer;

public class OWLGsonRendererTest extends OWLToolsTestBasics {


	@Test
	public void testAxioms() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		OWLGsonRenderer gr = new OWLGsonRenderer();
		OWLOntology ont = wrapper.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			
		}
		for (OWLAxiom a : ont.getAxioms()) {
			gr.render(a);
		}
	}

	@Test
	public void testGEdges() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		OWLGsonRenderer gr = new OWLGsonRenderer();
		OWLOntology ont = wrapper.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			for (OWLGraphEdge e : wrapper.getOutgoingEdgesClosure(c)) {
				gr.render(e);
			}
		}
	}

	@Test
	public void testOnt() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		OWLGsonRenderer gr = new OWLGsonRenderer();
		gr.render(wrapper.getSourceOntology());
	}

	
	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws OWLOntologyCreationException, FileNotFoundException, IOException{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
}
