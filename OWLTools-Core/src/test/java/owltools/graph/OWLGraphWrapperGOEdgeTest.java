package owltools.graph;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Ignore;
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
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

public class OWLGraphWrapperGOEdgeTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	@Test
	public void testRelSet() throws Exception {

		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo");

		//OWLOntology ontology = g.getSourceOntology();
		//OWLOntologyManager manager = ontology.getOWLOntologyManager();
		//manager.saveOntology(ontology, new org.semanticweb.owlapi.io.RDFXMLOntologyFormat(), IRI.create(new File("neurogenesis-wonky.owl")));

		///
		/// Part 1
		///

		// 
		//OWLObjectProperty sumthin = g.getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty sumthin = g.getOWLObjectPropertyByIdentifier("part_of");
		assertNotNull("I should have something, not nothing", sumthin);

		///
		/// Part 2: generate a set of them.
		///
		
		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		Set<OWLPropertyExpression> props = g.relationshipIDsToPropertySet(rel_ids);
		
		// Since neurogenesis.obo should only have is_a and part_of defined, there
		// should just be the latter in there.
		assertEquals("one prop", props.size(), 1);
//		for( OWLObjectProperty p : props ){
//			if( p.)
//		}
//		assertEquals("one prop", props., 1);
//
	}
	
	// ...
	@Test
	public void testEdgeRelClassification() throws Exception {
		
		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo");
		OWLObject x = g.getOWLClassByIdentifier("GO:0022008");

		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		Set<OWLPropertyExpression> props = g.relationshipIDsToPropertySet(rel_ids);
		Set<OWLGraphEdge> oge = g.getOutgoingEdgesClosure(x); // over reports
		for( OWLGraphEdge e : oge ){

//			if (e.getTarget() instanceof OWLClass) {
//			}
			
			OWLObject target = e.getTarget();

			// The edges we're looking at should be like:
			// http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=GO:0022008

			String rel = g.classifyRelationship(e, target, props);
			
			
		}
	}
	
	
	// Note that this code is mostly from OWLGraphWrapperEdgesAdvanced.
	// Specifically, this is based on addTransitiveAncestorsToShuntGraph
	// WANRING: This test will not catch anything if your graph is a subset.
	@Ignore("Waiting on better getOutgoingEdgesClosure specification (Chris and Heiko)") @Test
	public void testEdgeClosures() throws Exception {
		
		//OWLGraphWrapper g = getOntologyWrapper("go.20130314.owl"); // this also works
		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo");
		//OWLOntology relo = getOntology("ro.owl");
		//g.mergeOntology(relo);
		//OWLOntology ontology = g.getSourceOntology();
		//OWLOntologyManager manager = ontology.getOWLOntologyManager();
		//manager.saveOntology(ontology, new org.semanticweb.owlapi.io.RDFXMLOntologyFormat(), IRI.create(new File("neurogenesis-wonky.owl")));

		OWLObject x = g.getOWLClassByIdentifier("GO:0022008");

		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		Set<OWLPropertyExpression> props = g.relationshipIDsToPropertySet(rel_ids);
		Set<OWLGraphEdge> oge = g.getOutgoingEdgesClosure(x); // over reports?
		for( OWLGraphEdge e : oge ){

//			e.getTarget().accept(new OWLObjectVisitorAdapter(){
//
//				/* (non-Javadoc)
//				 * @see org.semanticweb.owlapi.util.OWLObjectVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLClass)
//				 */
//				@Override
//				public void visit(OWLClass desc) {
//					
//				}
//				
//			});
//			
//			if (e.getTarget() instanceof OWLClass) {
//				
//			}
			
			OWLObject target = e.getTarget();

			// The edges we're looking at should be like:
			// http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=GO:0022008

			String rel = g.classifyRelationship(e, target, props);

			LOG.info("id: " + g.getIdentifier(target) + ", " + rel);
			if( rel != null ){

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

				LOG.info("\t(" + oID + ", " + oLabel + "): " + eLabel);
				if( oID == null || oLabel == null || eLabel == null ){
					fail("Node should be well defined: (" + oID + ", " + oLabel + "): " + eLabel);
				}

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
					// Ignore...
					fail("Relation should not exist: (" + oID + ", " + oLabel + "): " + eLabel);
				}
			}
		}
	}
	
	private OWLGraphWrapper getOntologyWrapper(String file) throws Exception {
		ParserWrapper p = new ParserWrapper();
		return p.parseToOWLGraph(getResourceIRIString(file));
	}

}
