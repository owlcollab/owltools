package owltools.graph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.ParserWrapper;

/**
 * Test the functionalities of {@link OWLGraphManipulator}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13, May 2013
 * @since Bgee 13
 *
 */
public class OWLGraphManipulatorTest
{
    private final static Logger log = 
    		LogManager.getLogger(OWLGraphManipulatorTest.class.getName());
    /**
     * The <code>OWLGraphWrapper</code> used to perform the test. 
     */
    private OWLGraphManipulator graphManipulator;
	
	/**
	 * Default Constructor. 
	 */
	public OWLGraphManipulatorTest() {
	
	}
	
	/**
	 * Load the (really basic) ontology <code>/ontologies/OWLGraphManipulatorTest.obo</code> 
	 * into {@link #graphManipulator}.
	 * It is loaded before the execution of each test, so that a test can modify it 
	 * without impacting another test.
	 *  
	 * @throws OWLOntologyCreationException 
	 * @throws OBOFormatParserException
	 * @throws IOException
	 * 
	 * @see #graphManipulator
	 */
	@Before
	public void loadTestOntology() 
			throws OWLOntologyCreationException, OBOFormatParserException, IOException
	{
		log.debug("Wrapping test ontology into OWLGraphManipulator...");
		ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(
        		this.getClass().getResource("/ontologies/OWLGraphManipulatorTest.obo").getFile());
    	this.graphManipulator = new OWLGraphManipulator(new OWLGraphWrapper(ont));
		log.debug("Done wrapping test ontology into OWLGraphManipulator.");
	}
	
	
	//***********************************************
	//    RELATION REDUCTION AND RELATED TESTS
	//***********************************************
	/**
	 * Test the functionality of {@link OWLGraphManipulator#reduceRelations()}.
	 */
	@Test
	public void shouldReduceRelations()
	{
		//get the original number of axioms
	    int axiomCountBefore = 
	    		this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		int relsRemoved = this.graphManipulator.reduceRelations();
		
		//get the number of axioms after removal
		int axiomCountAfter = 
				this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		//4 relations should have been removed
		assertEquals("Incorrect number of relations removed", 4, relsRemoved);
		//check that it corresponds to the number of axioms removed
		assertEquals("Returned value does not correspond to the number of axioms removed", 
				relsRemoved, axiomCountBefore - axiomCountAfter);
		
		//Check that the relations removed correspond to the proper relations to remove
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLClass root = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty overlaps = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002131");
		
		//FOO:0003 part_of FOO:0001 redundant 
		//(FOO:0003 in_deep_part_of FOO:0004  part_of FOO:0002 part_of FOO:0001)
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, root, partOf, Quantifier.SOME, ont);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0004 overlaps FOO:0001 redundant
		//(FOO:0004 part_of FOO:0002 part_of FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0004");
		checkEdge = new OWLGraphEdge(source, root, overlaps, Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0014 is_a FOO:0001 redundant
		//(FOO:0014 is_a FOO:0006 is_a FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014");
		checkEdge = new OWLGraphEdge(source, root);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0015 part_of FOO:0001 redundant
		//(FOO:0014 is_a FOO:0004 part_of FOO:0002 part_of FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0015");
		checkEdge = new OWLGraphEdge(source, root, partOf, Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
	}
	/**
	 * Test the functionality of {@link OWLGraphManipulator#reducePartOfIsARelations()}.
	 */
	@Test
	public void shouldReducePartOfIsARelations()
	{
		//get the original number of axioms
	    int axiomCountBefore = 
	    		this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		int relsRemoved = this.graphManipulator.reducePartOfIsARelations();
		
		//get the number of axioms after removal
		int axiomCountAfter = 
				this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		//3 relations should have been removed
		assertEquals("Incorrect number of relations removed", 3, relsRemoved);
		//check that it corresponds to the number of axioms removed
		assertEquals("Returned value does not correspond to the number of axioms removed", 
				relsRemoved, axiomCountBefore - axiomCountAfter);
		
		//Check that the relations removed correspond to the proper relations to remove
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLClass root = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		
		//FOO:0005 is_a FOO:0001 redundant 
		//(FOO:0005 part_of FOO:0002 part_of FOO:0001)
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0005");
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, root);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0014 is_a FOO:0001 redundant
		//(FOO:0014 is_a FOO:0002 part_of FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014");
		checkEdge = new OWLGraphEdge(source, root);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0013 is_a FOO:0001 redundant
		//(FOO:0013 part_of FOO:0001, equivalent direct outgoing edge)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0013");
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		checkEdge = new OWLGraphEdge(source, root, partOf, Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
	}
	
	//***********************************************
	//    TEST MAPPING RELATIONS TO PARENT
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#mapRelationsToParent(Collection)}.
	 */
	@Test
	public void shouldMapRelationsToParent()
	{
		//get the original number of axioms
	    int axiomCountBefore = 
	    		this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
	    //map sub-relations to part_of and has_developmental_contribution_from
	    Collection<String> parentRelIds = new ArrayList<String>();
	    parentRelIds.add("BFO:0000050");
	    parentRelIds.add("RO:0002254");
		int relsUpdated = this.graphManipulator.mapRelationsToParent(parentRelIds);
		
		//get the number of axioms after removal
		int axiomCountAfter = 
				this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		//3 relations should have been updated
		assertEquals("Incorrect number of relations updated", 3, relsUpdated);
		//check that the number of axioms is the same 
		assertEquals("Number of axioms has changed", 0, axiomCountBefore - axiomCountAfter);
		
		//Check that the relations updated correspond to the proper relations to update
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty inDeepPartOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("in_deep_part_of");
		OWLObjectProperty hasDvlptCont = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002254");
		OWLObjectProperty dvlptFrom = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002202");
		OWLObjectProperty transfOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("http://semanticscience.org/resource/SIO_000657");
		
		//FOO:0003 in_deep_part_of FOO:0004 updated to 
		//FOO:0003 part_of FOO:0004
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0004");
		
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, target, inDeepPartOf, 
				Quantifier.SOME, ont);
		OWLAxiom oldAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		
		checkEdge = new OWLGraphEdge(source, target, partOf, 
				Quantifier.SOME, ont);
		OWLAxiom newAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		
		assertFalse("Relation FOO:0003 in_deep_part_of FOO:0004 was not removed", 
				ont.containsAxiom(oldAxiom));
		assertTrue("Relation FOO:0003 part_of FOO:0004 was not added", 
				ont.containsAxiom(newAxiom));
		
		
		//FOO:0012 transformation_of FOO:0008 updated to 
		//FOO:0012 has_developmental_contribution_from FOO:0008
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0012");
		target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0008");

		checkEdge = new OWLGraphEdge(source, target, transfOf, 
				Quantifier.SOME, ont);
		oldAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));

		checkEdge = new OWLGraphEdge(source, target, hasDvlptCont, 
				Quantifier.SOME, ont);
		newAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));

		assertFalse("Relation FOO:0012 transformation_of FOO:0008 was not removed", 
				ont.containsAxiom(oldAxiom));
		assertTrue("Relation FOO:0012 has_developmental_contribution_from FOO:0008 was not added", 
				ont.containsAxiom(newAxiom));

		
		//FOO:0008 develops_from FOO:0007 updated to 
		//FOO:0008 has_developmental_contribution_from FOO:0007
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0008");
		target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0007");

		checkEdge = new OWLGraphEdge(source, target, dvlptFrom, 
				Quantifier.SOME, ont);
		oldAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));

		checkEdge = new OWLGraphEdge(source, target, hasDvlptCont, 
				Quantifier.SOME, ont);
		newAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));

		assertFalse("Relation FOO:0008 develops_from FOO:0007 was not removed", 
				ont.containsAxiom(oldAxiom));
		assertTrue("Relation FOO:0008 has_developmental_contribution_from FOO:0007 was not added", 
				ont.containsAxiom(newAxiom));
	}
	
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#mapRelationsToParent(Collection, Collection)}.
	 */
	@Test
	public void shouldMapRelationsToParentWithRelsExcluded()
	{
		//get the original number of axioms
	    int axiomCountBefore = 
	    		this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
	    //map sub-relations to part_of and has_developmental_contribution_from
	    Collection<String> parentRelIds = new ArrayList<String>();
	    parentRelIds.add("BFO:0000050");
	    parentRelIds.add("RO:0002254");
	    //exclude from mapping develops_from (and sub-relations)
	    Collection<String> relIdsExcluded = new ArrayList<String>();
	    relIdsExcluded.add("RO:0002202");
		int relsUpdated = this.graphManipulator.mapRelationsToParent(parentRelIds, relIdsExcluded);
		
		//get the number of axioms after removal
		int axiomCountAfter = 
				this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		//only 1 relations should have been updated, 
		//as develops_from is excluded from mapping (and so its sub-relation 
		//transformation_of as well)
		assertEquals("Incorrect number of relations updated", 1, relsUpdated);
		//check that the number of axioms is the same 
		assertEquals("Number of axioms has changed", 0, axiomCountBefore - axiomCountAfter);
		
		//Check that the relations updated correspond to the proper relations to update
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty inDeepPartOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("in_deep_part_of");
		
		//FOO:0003 in_deep_part_of FOO:0004 updated to 
		//FOO:0003 part_of FOO:0004
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0004");
		
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, target, inDeepPartOf, 
				Quantifier.SOME, ont);
		OWLAxiom oldAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		
		checkEdge = new OWLGraphEdge(source, target, partOf, 
				Quantifier.SOME, ont);
		OWLAxiom newAxiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		
		assertFalse("Relation FOO:0003 in_deep_part_of FOO:0004 was not removed", 
				ont.containsAxiom(oldAxiom));
		assertTrue("Relation FOO:0003 part_of FOO:0004 was not added", 
				ont.containsAxiom(newAxiom));
		
	}
	
	
	//***********************************************
	//    REMOVE CLASS AND PROPAGATE EDGE TESTS
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#removeClassAndPropagateEdges(String)}.
	 */
	@Test
	public void shouldRemoveClassAndPropagateEdges()
	{
		//remove FOO:0004, which has 2 outgoing edges: 
		//FOO:0004 part_of FOO:0002 and FOO:0004 overlaps FOO:0001
		//and two incoming edges: 
		//FOO:0015 is_a FOO:0004 and FOO:0003 in_deep_part_of FOO:0004
		//Of note, FOO:0003 has also a relation part_of to FOO:0001.
		int relsPropagated = this.graphManipulator.removeClassAndPropagateEdges("FOO:0004");
		//2 edges should have been propagated
		assertEquals("Incorrect number of edges propagated", 2, relsPropagated);
		//check that the class was indeed removed
		assertNull("Class FOO:0004 was not removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0004"));
		
		//check the actual relations
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty overlaps = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002131");
		
		//all outgoing edges of FOO:0004 should have been propagated to FOO:0015 
		//(because FOO:0015 is_a FOO:0004), but a more precise relation 
		//already exists. The resulting new edge is: FOO:0015 part_of FOO:0002.
		//FOO:0015 overlaps FOO:0001 is not added, as it already exists a relation 
		//FOO:0015 part_of FOO:0001
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0015");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0002");
		
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, target, partOf, 
				Quantifier.SOME, ont);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertTrue("Relation FOO:0015 part_of FOO:0002 was not correctly created", 
				ont.containsAxiom(axiom));
		
		//check that the relation overlaps FOO:0001 was not incorrectly created
		target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		
		checkEdge = new OWLGraphEdge(source, target, overlaps, 
				Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Relation FOO:0015 overlaps FOO:0001 was incorrectly created", 
				ont.containsAxiom(axiom));
		
		//Propagation from FOO:0003: FOO:0003 in_deep_part_of FOO:0004
		//only one resulting new edge, FOO:0003 part_of FOO:0002 (as FOO:0004 part_of FOO:0002).
		//the other outgoing edge (FOO:0004 overlaps FOO:0001) cannot be combined 
		//because overlaps is not transitive
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0002");
		
		checkEdge = new OWLGraphEdge(source, target, partOf, 
				Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertTrue("Relation FOO:0003 part_of FOO:0002 was not correctly created", 
				ont.containsAxiom(axiom));
		
		//check that FOO:0003 overlaps FOO:0001 was not incorrectly added
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		
		checkEdge = new OWLGraphEdge(source, target, overlaps, 
				Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Relation FOO:0003 overlaps FOO:0001 was incorrectly added", 
				ont.containsAxiom(axiom));
	}
	
	/**
	 * Test the functionalities of 
	 * {@link CustomOWLGraphWrapper#getOWLGraphEdgeSubRelsReflexive(OWLGraphEdge)}.
	 */
	@Test
	public void shouldGetOWLGraphEdgeSubRelsReflexive()
	{
		//get an edge to perform test on it
		//test with the fake relations of the test ontology
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		OWLObjectProperty fakeRel1 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("fake_rel1");
		
		OWLGraphEdge testEdge = new OWLGraphEdge(source, target, fakeRel1, Quantifier.SOME, ont);
		
		int subRelRank = 0;
		for (OWLGraphEdge subRel: this.graphManipulator.getOwlGraphWrapper().
				getOWLGraphEdgeSubRelsReflexive(testEdge)) {
			if (subRelRank == 0) {
				//first relation should be fake_rel1 (reflexive method)
				assertEquals("Incorrect order of sub-properties, 1st relation", 
						fakeRel1, subRel.getSingleQuantifiedProperty().getProperty());
			} else if (subRelRank == 1) {
				//then fake_rel2
				OWLObjectProperty fakeRel2 = this.graphManipulator.getOwlGraphWrapper().
						getOWLObjectPropertyByIdentifier("fake_rel2");
				assertEquals("Incorrect order of sub-properties, 2nd relation", 
						fakeRel2, subRel.getSingleQuantifiedProperty().getProperty());
			} else if (subRelRank == 2 || subRelRank == 3) {
				//next relation should be either fake_rel3, or fake_rel4
				OWLObjectProperty fakeRel3 = this.graphManipulator.getOwlGraphWrapper().
						getOWLObjectPropertyByIdentifier("fake_rel3");
				OWLObjectProperty fakeRel4 = this.graphManipulator.getOwlGraphWrapper().
						getOWLObjectPropertyByIdentifier("fake_rel4");
				assertTrue("Incorrect order of sub-properties, 3rd or 4th relation", 
					(fakeRel3.equals(subRel.getSingleQuantifiedProperty().getProperty()) || 
					fakeRel4.equals(subRel.getSingleQuantifiedProperty().getProperty())));
			} else {
				//should not be reached
				throw new AssertionError("Incorrect number of sub-relations");
			}
			subRelRank++;
		}
		
		//check that they were 4 sub-relations
		assertEquals("Incorrect number of sub-relations", 4, subRelRank);
	}
	
	//***********************************************
	//    RREMOVE RELS TO SUBSETS IF NON ORPHAN TESTS
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#removeRelsToSubsets(Collection)}. 
	 */
	@Test
	public void shouldRemoveRelsToSubsets()
	{
		//remove rels to subsets test_subset1 and test_subset2. Here is the configuration: 
		//FOO:0006 and FOO:0007 are part of test_subset1
		//FOO:0009 is part of test_subset2. 
		
		//FOO:0006 has 2 incoming edges: FOO:0014 is_a FOO:0006
		//and FOO:0007 has_developmental_contribution_from FOO:0006. 
		//The relation FOO:0014 is_a FOO:0006 should be removed, as FOO:0014 
		//has other is_a relations to classes not in the targeted subsets.
		//The has_developmental_contribution_from relation should not be removed.
		
		//FOO:0007 has 2 incoming edges: FOO:0010 part_of FOO:0007 and 
		//FOO:0009 overlaps FOO:0007. 
		//FOO:0010 part_of FOO:0007 should not be removed, because the only other 
		//is_a/part_of relation of FOO:0010 goes to a class in a targeted subset 
		//(FOO:0010 part_of FOO:0009)
		//FOO:0009 overlaps FOO:0007 should not be removed as it is not a part_of relation, 
		//nor a part_of sub-relation.
		//(so no incoming edges should be removed for FOO:0007)
		
		//FOO:0009 has 2 incoming edges: FOO:0010 part_of FOO:0009 and 
		//FOO:0011 part_of FOO:0009.
		//FOO:0010 part_of FOO:0009 should not be removed, because the only other 
		//is_a/part_of relation of FOO:0010 goes to a class in a targeted subset 
		//(FOO:0010 part_of FOO:0007). 
		//FOO:0011 part_of FOO:0009 should be removed, because it exists a relation 
		//FOO:0011 is_a FOO:0002, and FOO:0002 does not belong to a targeted subset.
		
		Collection<String> subsets = new ArrayList<String>();
		subsets.add("test_subset1");
		subsets.add("test_subset2");
		//get the original number of axioms
		int axiomCountBefore = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getAxiomCount();
		int relsRemoved = 
				this.graphManipulator.removeRelsToSubsets(subsets);
		//number of axioms after modification
		int axiomCountAfter = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getAxiomCount();
		
		//2 relations should have been removed
		assertEquals("Incorrect number of relations removed", 2, relsRemoved);
		//check it corresponds to the number of axioms removed
		assertEquals("The method did not return the correct number of relations removed", 
				relsRemoved, axiomCountBefore - axiomCountAfter);
		
		//check that the correct relations were removed
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		
		//FOO:0014 is_a FOO:0006 should have been removed
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0006");
		
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, target);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		
		assertFalse("Relation FOO:0014 is_a FOO:0006 was not removed", 
				ont.containsAxiom(axiom));
		
		//FOO:0011 part_of FOO:0009 should have been removed
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0011");
		target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0009");
		
		checkEdge = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		
		assertFalse("Relation FOO:0011 part_of FOO:0009 was not removed", 
				ont.containsAxiom(axiom));
	}
	
	
	//***********************************************
	//    RELATION FILTERING AND REMOVAL TESTS
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>false</code>.
	 */
	@Test
	public void shouldFilterRelations()
	{
		//filter relations to keep only is_a, part_of and develops_from
		//5 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("BFO:0000050", "RO:0002202"), 
				false, 5, true);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>true</code>.
	 */
	@Test
	public void shouldFilterRelationsWithSubRel()
	{
		//filter relations to keep is_a, part_of, develops_from, 
		//and their sub-relations.
		//3 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("BFO:0000050", "RO:0002202"), 
				true, 3, true);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * when filtering a relation with a non-OBO-style ID (in this method, 
	 * <code>http://semanticscience.org/resource/SIO_000657</code>).
	 */
	@Test
	public void shouldFilterRelationsWithNonOboId()
	{
		//filter relations to keep only is_a and transformation_of relations
		//14 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("http://semanticscience.org/resource/SIO_000657"), 
				true, 14, true);
	}	
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * when filtering all relations but is_a.
	 */
	@Test
	public void shouldFilterAllRelations()
	{
		//filter relations to keep only is_a relations
		//15 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList(""), 
				true, 15, true);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#removeRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>false</code>.
	 */
	@Test
	public void shouldRemoveRelations()
	{
		//remove part_of and develops_from relations
		//10 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("BFO:0000050", "RO:0002202"), 
			false, 10, false);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#removeRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>true</code>.
	 */
	@Test
	public void shouldRemoveRelationsWithSubRel()
	{
		//remove develops_from relations and sub-relations
		//2 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("RO:0002202"), 
			true, 2, false);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#removeRelations(Collection, boolean)} 
	 * with an empty list of relations to remove, to check that it actually removed nothing.
	 */
	@Test
	public void shouldRemoveNoRelation()
	{
		//remove nothing
		//0 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList(""), 
			true, 0, false);
	}
	/**
	 * Method to test the functionalities of 
	 * {@link OWLGraphManipulator#filterRelations(Collection, boolean)} and 
	 * {@link OWLGraphManipulator#removeRelations(Collection, boolean)}
	 * with various configurations, called by the methods performing the actual unit test. 
	 * 
	 * @param rels 				corresponds to the first parameter of 
	 * 							the <code>filterRelations</code> or 
	 * 							<code>removeRelations</code> method.
	 * @param subRels			corresponds to the second parameter of 
	 * 							the <code>filterRelations</code> or 
	 * 							<code>removeRelations</code> method.
	 * @param expRelsRemoved 	An <code>int</code> representing the expected number 
	 * 							of relations removed
	 * @param filter 			A <code>boolean</code> defining whether the method tested is 
	 * 							<code>filterRelations</code>, or <code>removeRelations</code>. 
	 * 							If <code>true</code>, the method tested is 
	 * 							<code>filterRelations</code>.
	 */
	private void shouldFilterOrRemoveRelations(Collection<String> rels, 
			boolean subRels, int expRelsRemoved, boolean filter)
	{
		//get the original number of axioms
		int axiomCountBefore = this.graphManipulator.getOwlGraphWrapper()
			    .getSourceOntology().getAxiomCount();
		
		//filter relations to keep 
		int relRemovedCount = 0;
		if (filter) {
			relRemovedCount = this.graphManipulator.filterRelations(rels, subRels);
		} else {
			relRemovedCount = this.graphManipulator.removeRelations(rels, subRels);
		}
		//expRelsRemoved relations should have been removed
		assertEquals("Incorrect number of relations removed", expRelsRemoved, relRemovedCount);
		
		//get the number of axioms after removal
		int axiomCountAfter = this.graphManipulator.getOwlGraphWrapper()
			    .getSourceOntology().getAxiomCount();
		//check that it corresponds to the returned value
		assertEquals("The number of relations removed does not correspond to " +
				"the number of axioms removed", 
				axiomCountBefore - axiomCountAfter, relRemovedCount);
	}
	
	

	//***********************************************
	//    SUBGRAPH FILTERING AND REMOVAL TESTS
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#filterSubgraphs(Collection)}.
	 */
	@Test
	public void shouldFilterSubgraphs()
	{
		//The test ontology includes several subgraphs, with 1 to be removed, 
		//and with two terms part of both a subgraph to remove and a subgraph to keep 
		//(FOO:0011, FOO:0014).
		//All terms belonging to the subgraph to remove, except these common terms, 
		//should be removed.
		
		//first, let's get the number of classes in the ontology
		int classCount = this.graphManipulator.getOwlGraphWrapper()
				    .getSourceOntology().getClassesInSignature().size();
		
		//filter the subgraphs, we want to keep: 
		//FOO:0002 corresponds to term "A", root of the first subgraph to keep. 
		//FOO:0013 to "subgraph3_root".
		//FOO:0014 to "subgraph4_root_subgraph2" 
		//(both root of a subgraph to keep, and part of a subgraph to remove).
		//subgraph starting from FOO:0006 "subgraph2_root" will be removed, 
		//(but not FOO:0006 itself, because it is an ancestor of FOO:0014; 
		//if FOO:0014 was not an allowed root, then FOO:0006 would be removed)
		Collection<String> toKeep = new ArrayList<String>();
		toKeep.add("FOO:0002");
		toKeep.add("FOO:0013");
		toKeep.add("FOO:0014");
		int countRemoved = this.graphManipulator.filterSubgraphs(toKeep);
		
		//The test ontology is designed so that 6 classes should have been removed
		assertEquals("Incorrect number of classes removed", 6, countRemoved);
		
		//test that these classes were actually removed from the ontology
		int newClassCount = this.graphManipulator.getOwlGraphWrapper()
			    .getSourceOntology().getClassesInSignature().size();
		assertEquals("filterSubgraph did not return the correct number of classes removed", 
				classCount - newClassCount, countRemoved);
		
		//Test that the terms part of both subgraphs were not incorrectly removed.
		//Their IDs are FOO:0011 and FOO:0014, they have slighty different relations to the root
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0011"));
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014"));
		
		//now, we need to check that the relations FOO:0003 B is_a FOO:0001 root, 
		//FOO:0004 C part_of FOO:0001 root, FOO:0005 D is_a FOO:0001 root
		//have been removed (terms should be kept as it is part of a subgraph to keep, 
		//but the relations to the root are still undesired subgraphs, 
		//that should be removed)
		OWLClass root = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		for (OWLGraphEdge incomingEdge: 
		    this.graphManipulator.getOwlGraphWrapper().getIncomingEdges(root)) {
			assertNotEquals("The relation FOO:0003 B is_a FOO:0001 root, " +
					"causing an undesired subgraph, was not correctly removed", 
					"FOO:0003", 
					this.graphManipulator.getOwlGraphWrapper().getIdentifier(
							incomingEdge.getSource()));
			assertNotEquals("The relation FOO:0004 C is_a FOO:0001 root, " +
					"causing an undesired subgraph, was not correctly removed", 
					"FOO:0004", 
					this.graphManipulator.getOwlGraphWrapper().getIdentifier(
							incomingEdge.getSource()));
			assertNotEquals("The relation FOO:0005 D is_a FOO:0001 root, " +
					"causing an undesired subgraph, was not correctly removed", 
					"FOO:0005", 
					this.graphManipulator.getOwlGraphWrapper().getIdentifier(
							incomingEdge.getSource()));
		}
	}
	
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#removeSubgraphs(Collection, boolean)}, 
	 * with the <code>boolean</code> parameter set to <code>true</code>.
	 */
	@Test
	public void shouldRemoveSubgraphs()
	{
		//The test ontology includes several subgraphs, with 1 to be removed, 
		//and with two terms part of both a subgraph to remove and a subgraph to keep.
		//All terms belonging to the subgraph to remove, except these common terms, 
		//should be removed.

		//first, let's get the number of classes in the ontology
		int classCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();

		//remove the subgraph
		Collection<String> toRemove = new ArrayList<String>();
		toRemove.add("FOO:0006");
		//add as a root to remove a term that is in the FOO:0006 subgraph, 
		//to check if the ancestors check will not lead to keep erroneously FOO:0007
		toRemove.add("FOO:0008");
		int countRemoved = this.graphManipulator.removeSubgraphs(toRemove, true);

		//The test ontology is designed so that 6 classes should have been removed
		assertEquals("Incorrect number of classes removed", 6, countRemoved);
		//test that these classes were actually removed from the ontology
		int newClassCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();
		assertEquals("removeSubgraph did not return the correct number of classes removed", 
				classCount - newClassCount, countRemoved);

		//Test that the terms part of both subgraphs, or part of independent subgraphs, 
		//were not incorrectly removed.
		//Their IDs are FOO:0011 and FOO:0014, they have slighty different relations to the root
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0011"));
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014"));
	}
	
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#removeSubgraphs(Collection, boolean)}, 
	 * with the <code>boolean</code> parameter set to <code>false</code>.
	 */
	@Test
	public void shouldRemoveSubgraphsAndSharedClasses()
	{
		//The test ontology includes several subgraphs, with 1 to be removed, 
		//and with two terms part of both a subgraph to remove and a subgraph to keep.
		//All terms belonging to the subgraph to remove, EVEN these common terms, 
		//should be removed.

		//first, let's get the number of classes in the ontology
		int classCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();

		//remove the subgraph
		Collection<String> toRemove = new ArrayList<String>();
		toRemove.add("FOO:0006");
		int countRemoved = this.graphManipulator.removeSubgraphs(toRemove, false);

		//The test ontology is designed so that 8 classes should have been removed
		assertEquals("Incorrect number of classes removed", 8, countRemoved);
		//test that these classes were actually removed from the ontology
		int newClassCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();
		assertEquals("removeSubgraph did not return the correct number of classes removed", 
				classCount - newClassCount, countRemoved);
	}
	
	/**
	 * Not a unit test, just for the fun, run 
	 * {@link OWLGraphManipulator#makeBasicOntology()}, which runs several methods 
	 * that are all unit tested here.
	 */
	public void shouldMakeBasicOntology()
	{
		this.graphManipulator.makeBasicOntology();
	}
	
	/**
	 * Test {@link owltools.graph.OWLGraphEdge#hashCode()}. 
	 * There used to be a problem that two equal <code>OWLGraphEdge<code> could have 
	 * different hashcodes, leading to the the possibility to have several identical 
	 * <code>OWLGraphEdge</code>s in a <code>Set</code>.
	 */
	@Test
	public void testOWLGraphEdgeHashCode()
	{
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		OWLGraphEdge edge1 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, ont);
		OWLGraphEdge edge2 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, ont);
		assertTrue("Two OWLGraphEdges are equal but have different hashcodes.", 
				edge1.equals(edge2) && edge1.hashCode() == edge2.hashCode());
	}
	
	/**
	 * Test that two <code>OWLClass</code>es that are equal have a same hashcode, 
	 * because the OWLGraphEdge bug get me paranoid. 
	 */
	@Test
	public void testOWLClassHashCode()
	{
		 OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		 OWLDataFactory factory = manager.getOWLDataFactory(); 
		 IRI iri = IRI.create("http://www.foo.org/#A");
		 OWLClass class1 = factory.getOWLClass(iri);
		 //get the class by another way, even if if I suspect the two references 
		 //will point to the same object
		 PrefixManager pm = new DefaultPrefixManager("http://www.foo.org/#"); 
		 OWLClass class2 = factory.getOWLClass(":A", pm);
		 
		 assertTrue("The two references point to different OWLClass objects", 
				 class1 == class2);
		 //then of course the hashcodes will be the same...
		 assertTrue("Two OWLClasses are equal but have different hashcode", 
				 class1.equals(class2) && class1.hashCode() == class2.hashCode());
	}/**
	 * Test that two <code>OWLClass</code>es that are equal have a same hashcode, 
	 * because the OWLGraphEdge bug get me paranoid. 
	 */
	@Test
	public void testOWLOntologyChangeHashCode()
	{
    	
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0005");
		OWLClass target = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, target, ont);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		OWLAxiomChange rm1 = new RemoveAxiom(ont, axiom);
		
		OWLGraphEdge checkEdge2 = new OWLGraphEdge(source, target, ont);
		OWLAxiom axiom2 = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge2));
		OWLAxiomChange rm2 = new RemoveAxiom(ont, axiom2);
		
		assertTrue("The two OWLAxiomChange objects are equal", 
				 rm1.equals(rm2));
		 //then of course the hashcodes will be the same...
		 assertTrue("Two OWLAxiomChange are equal but have different hashcode", 
				 rm1.equals(rm2) && rm1.hashCode() == rm2.hashCode());
		 
		 
		 
		 source = 
				 this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014");
		 target = 
				 this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");

		 checkEdge = new OWLGraphEdge(source, target, ont);
		 axiom = factory.getOWLSubClassOfAxiom(source, 
				 (OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				 edgeToTargetExpression(checkEdge));
		 OWLAxiomChange rm3 = new RemoveAxiom(ont, axiom);

		 assertFalse("Different OWLAxiomChange objects are equal", 
				 rm1.equals(rm3));
		 //then of course the hashcodes will be the same...
		 assertFalse("Different OWLAxiomChanges have same hashcode", 
				 rm1.hashCode() == rm3.hashCode());
	}
	
	/**
	 * Test <code>hashCode</code> method and <code>equals</code> method 
	 * of {@link owltools.graph.OWLQuantifiedProperty}. 
	 */
	@Test
	public void testOWLQuantifiedPropertyHashCode()
	{
		OWLObjectProperty prop1 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty prop2 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		
		assertTrue("Two OWLQuantifiedProperty are equal but have different hashcodes.", 
				prop1.equals(prop2) && prop1.hashCode() == prop2.hashCode());
		
		OWLObjectProperty prop3 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002202");
		
		assertFalse("Two different OWLQuantifiedProperty are equal", prop1.equals(prop3));
		assertFalse("Two different OWLQuantifiedProperty have same hashcode", 
				prop1.hashCode() == prop3.hashCode());
	}
}
