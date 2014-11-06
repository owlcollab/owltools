package owltools.graph;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.OWLToolsTestBasics;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.io.ParserWrapper;

public class OWLGraphWrapperGOEdgeTest extends OWLToolsTestBasics {

	@Test
	public void testRelSet() throws Exception {

		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo");

		///
		/// Part 1
		///

		OWLObjectProperty sumthin = g.getOWLObjectPropertyByIdentifier("part_of");
		assertNotNull("I should have something, not nothing", sumthin);

		///
		/// Part 2: generate a set of them.
		///
		
		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		Set<OWLObjectProperty> props = g.relationshipIDsToPropertySet(rel_ids);
		
		// Since neurogenesis.obo should only have is_a and part_of defined, there
		// should just be the latter in there.
		assertEquals("one prop", props.size(), 1);
	}
	
	@Test
	@Ignore("Unfinished")
	public void testEdgeRelClassification() throws Exception {
		
		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo");
		OWLObject x = g.getOWLClassByIdentifier("GO:0022008");

		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		Set<OWLObjectProperty> props = g.relationshipIDsToPropertySet(rel_ids);
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
	
	@Test
	public void testEdgeClosures() throws Exception {
		
		//OWLGraphWrapper g = getOntologyWrapper("go.20130314.owl"); // this also works
		OWLGraphWrapper g = getOntologyWrapper("graph/neurogenesis.obo");

		OWLObject x = g.getOWLClassByIdentifier("GO:0022008");

		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		
		OWLShuntGraph graphSegment = new OWLShuntGraph();
		graphSegment = g.addTransitiveAncestorsToShuntGraph(x, graphSegment , rel_ids);
		
		for(OWLShuntEdge e : graphSegment.edges){

			final String oID = e.obj;
			final String eLabel = e.pred;

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
				fail("Node should not exist: (" + oID + "): " + eLabel);
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
				fail("Relation should not exist: (" + oID + "): " + eLabel);
			}
		}
	}
	
	private OWLGraphWrapper getOntologyWrapper(String file) throws Exception {
		ParserWrapper p = new ParserWrapper();
		return p.parseToOWLGraph(getResourceIRIString(file));
	}

}
