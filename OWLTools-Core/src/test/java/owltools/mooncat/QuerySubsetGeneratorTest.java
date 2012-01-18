package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class QuerySubsetGeneratorTest extends OWLToolsTestBasics {

	private static boolean renderObo = false;
	
	@Test
	public void testCreateSubOntologyFromDLQuery() throws Exception {
		QuerySubsetGenerator generator = new QuerySubsetGenerator();
		
		OWLGraphWrapper sourceGraph = getSourceGraph();
		OWLGraphWrapper targetGraph = getTargetGraph();
		
		String dlQueryString = "FBbt_00005106 and 'part_of' some FBbt_00005095";
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		
		String query1 = "FBbt_00005106"; // neuron
		String query2 = "FBbt_00005106 and 'part_of' some FBbt_00005095"; // Brain
		int neuronClassCount = DLQueryTool.executeDLQuery(query1, sourceGraph, reasonerFactory).size();
		int neuronAndBrainClassCount = DLQueryTool.executeDLQuery(query2, sourceGraph, reasonerFactory).size();
		assertTrue(neuronClassCount > 1);
		assertTrue(neuronAndBrainClassCount > 1 && neuronAndBrainClassCount < neuronClassCount);
		
		generator.createSubOntologyFromDLQuery(dlQueryString, sourceGraph, targetGraph, reasonerFactory);

		Owl2Obo owl2Obo = new Owl2Obo();
		OBODoc oboDoc = owl2Obo.convert(targetGraph.getSourceOntology());
		if (renderObo) {
			renderOBO(oboDoc);
		}
		assertTrue(oboDoc.getTermFrames().size() > neuronAndBrainClassCount); // includes all the parents
	}
	
	private synchronized OWLGraphWrapper getSourceGraph() throws OWLOntologyCreationException, IOException {
		String resourceIRI = getResourceIRIString("mooncat/fly_anatomy.obo");
		ParserWrapper wrapper = new ParserWrapper();
		return new OWLGraphWrapper(wrapper.parseOBO(resourceIRI));
	}

	private OWLGraphWrapper getTargetGraph() throws Exception {
		OWLOntologyManager targetManager = OWLManager.createOWLOntologyManager();
		OWLOntologyID ontologyID  = new OWLOntologyID(IRI.create("http://test.owltools.org/dynamic"));
		OWLOntology targetOntology = targetManager.createOntology(ontologyID);
		OWLGraphWrapper targetGraph = new OWLGraphWrapper(targetOntology);
		return targetGraph;
	}

}
