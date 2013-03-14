package owltools.graph;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.OWLToolsTestBasics;

public class OWLGraphWrapperGOEdgeTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	// Note that this code is mostly from OWLGraphWrapperEdgesAdvanced.
	// Specifically, this is based on addTransitiveAncestorsToShuntGraph
	@Test
	public void testEdgeClosures() throws Exception {
		
		//OWLGraphWrapper g = getOntologyWrapper("go.20130314.owl"); // this also works
		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo"); 
		//OWLOntology ontology = g.getSourceOntology();
		//OWLOntologyManager manager = ontology.getOWLOntologyManager();
		//manager.saveOntology(ontology, new org.semanticweb.owlapi.io.RDFXMLOntologyFormat(), IRI.create(new File("neurogenesis-wonky.owl")));

		OWLObject x = g.getOWLClassByIdentifier("GO:0022008");

		ArrayList<String> rel_ids = new ArrayList<String>();
		rel_ids.add("BFO:0000050");
		rel_ids.add("RO:0002211");
		rel_ids.add("RO:0002212");
		rel_ids.add("RO:0002213");
		HashSet<OWLObjectProperty> props = g.relationshipIDsToPropertySet(rel_ids);
		for( OWLGraphEdge e : g.getOutgoingEdgesClosure(x) ){
			OWLObject target = e.getTarget();

			// The edges we're looking at should be like:
			// http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=GO:0022008

			LOG.info("id: " + g.getIdentifier(target));
			String rel = g.classifyRelationship(e, target, props);

			if( rel != null ){
				LOG.info("\tclass" + rel);

				String oID = null;
				String oLabel = null;
				String eLabel = null;
				if( rel == "simplesubclass" ){
					oID = g.getIdentifier(target);
					oLabel = g.getLabelOrDisplayId(target);
					eLabel = g.getEdgeLabel(e);
				}else if( rel == "typesubclass" ){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)target;
					OWLClassExpression clsexp = some.getFiller();
					OWLClass cls = clsexp.asOWLClass();
					oID = g.getIdentifier(cls);
					oLabel = g.getLabelOrDisplayId(cls);
					eLabel = g.getEdgeLabel(e);
				}else{
					fail("There should be nothing else here...");
				}

				LOG.info("(" + oID + ", " + oLabel + "): " + eLabel);
				
				// Node checks.
				if( oID.equals("GO:0008150") ){ // biological_process
				}else if( oID.equals("GO:0032502") ){ // developmental process
				}else if( oID.equals("GO:0032501") ){ // multicellular organismal process
				}else if( oID.equals("GO:0044699") ){ // single-organism process
				}else if( oID.equals("GO:0009987") ){ // cellular process
				}else if( oID.equals("GO:0048856") ){ // anatomical structure development
				}else if( oID.equals("GO:0044707") ){ // single-multicellular organism process
				}else if( oID.equals("GO:0044763") ){ // single-organism cellular process
				}else if( oID.equals("GO:0007275") ){ // multicellular organismal development
				}else if( oID.equals("GO:0048869") ){ // cellular developmental process
				}else if( oID.equals("GO:0048731") ){ // system development
				}else if( oID.equals("GO:0030154") ){ // cell differentiation
				}else if( oID.equals("GO:0007399") ){ // nervous system development
				}else if( oID.equals("GO:0022008") ){ // neurogenesis
				}else{
					fail("Node should not exist: (" + oID + ", " + oLabel + "): " + eLabel);
				}
				
				// Edge checks.
				if( oID.equals("GO:0008150") && eLabel.equals("is_a") ){ // biological_process
				}else if( oID.equals("GO:0008150") && eLabel.equals("part_of") ){ // biological_process

				}else if( oID.equals("GO:0032502") && eLabel.equals("is_a") ){ // developmental process
				}else if( oID.equals("GO:0032502") && eLabel.equals("part_of") ){ // developmental process

				}else if( oID.equals("GO:0032501") && eLabel.equals("part_of") ){ // multicellular organismal process

				}else if( oID.equals("GO:0044699") && eLabel.equals("is_a") ){ // single-organism process
				}else if( oID.equals("GO:0044699") && eLabel.equals("part_of") ){ // single-organism process

				}else if( oID.equals("GO:0009987") && eLabel.equals("is_a") ){ // cellular process

				}else if( oID.equals("GO:0048856") && eLabel.equals("part_of") ){ // anatomical structure development

				}else if( oID.equals("GO:0044707") && eLabel.equals("part_of") ){ // single-multicellular organism process

				}else if( oID.equals("GO:0044763") && eLabel.equals("is_a") ){ // single-organism cellular process

				}else if( oID.equals("GO:0007275") && eLabel.equals("part_of") ){ // multicellular organismal development

				}else if( oID.equals("GO:0048869") && eLabel.equals("is_a") ){ // cellular developmental process

				}else if( oID.equals("GO:0048731") && eLabel.equals("part_of") ){ // system development

				}else if( oID.equals("GO:0030154") && eLabel.equals("is_a") ){ // cell differentiation

				}else if( oID.equals("GO:0007399") && eLabel.equals("part_of") ){ // nervous system development
				}else{
					// TODO: Fails with:
					// junit.framework.AssertionFailedError: Should not exist: (GO:0044699, single-organism process): is_a
					// junit.framework.AssertionFailedError: Should not exist: (GO:0007399, nervous system development): is_a
					fail("Relation should not exist: (" + oID + ", " + oLabel + "): " + eLabel);
				}
			}
		}
	}
	
	private OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource(file));
		return new OWLGraphWrapper(ontology);
	}
}
