package owltools.graph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.ParserWrapper;

/**
 * Test of {@link OWLGraphWrapperEdgesExtended}.
 * @author Frederic Bastian
 * @version September 2014
 * @since November 2013
 *
 */
public class OWLGraphWrapperEdgesExtendedTest
{
    private final static Logger log = 
    		LogManager.getLogger(OWLGraphWrapperEdgesExtendedTest.class.getName());
    
    private static OWLGraphWrapper wrapper;
	/**
	 * Default Constructor. 
	 */
	public OWLGraphWrapperEdgesExtendedTest() {
		super();
	}
	
	/**
	 * Load the ontology <code>/graph/OWLGraphManipulatorTest.obo</code> into {@link #wrapper}.
	 *  
	 * @throws OWLOntologyCreationException 
	 * @throws OBOFormatParserException
	 * @throws IOException
	 * 
	 * @see #wrapper
	 */
	@BeforeClass
	public static void loadTestOntology() 
			throws OWLOntologyCreationException, OBOFormatParserException, IOException
	{
		log.debug("Wrapping test ontology into CustomOWLGraphWrapper...");
		ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(OWLGraphWrapperEdgesExtendedTest.class.getResource(
        		"/graph/OWLGraphManipulatorTest.obo").getFile());
    	wrapper = new OWLGraphWrapper(ont);
		log.debug("Done wrapping test ontology into CustomOWLGraphWrapper.");
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#isOWLObjectInSubsets(OWLObject, Collection)}.
	 */
	@Test
	public void isOWLObjectInSubsetsTest()
	{
		Collection<String> testSubsets = new ArrayList<String>();
		testSubsets.add("test_subset1");
		//FOO:0006 is part of the subset test_subset1
		OWLClass testClass = wrapper.getOWLClassByIdentifier("FOO:0006");
		assertTrue("FOO:0006 is not seen as belonging to test_subset1", 
				wrapper.isOWLObjectInSubsets(testClass, testSubsets));
		//FOO:0009 is in test_subset2, not in test_subset1
		testClass = wrapper.getOWLClassByIdentifier("FOO:0009");
		assertFalse("FOO:0009 is incorrectly seen as belonging to test_subset2", 
				wrapper.isOWLObjectInSubsets(testClass, testSubsets));
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getSubPropertiesOf(OWLObjectPropertyExpression)}.
	 */
	@Test
	public void shouldGetSubPropertiesOf()
	{
		OWLObjectProperty fakeRel1 = 
				wrapper.getOWLObjectPropertyByIdentifier("fake_rel1");
		OWLObjectProperty fakeRel2 = 
				wrapper.getOWLObjectPropertyByIdentifier("fake_rel2");
		//fake_rel2 is the only sub-property of fake_rel1
		Set<OWLObjectPropertyExpression> subprops = wrapper.getSubPropertiesOf(fakeRel1);
		assertTrue("Incorrect sub-properties returned: " + subprops, 
				subprops.size() == 1 && subprops.contains(fakeRel2));
		
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getSubPropertyClosureOf(OWLObjectPropertyExpression)}.
	 */
	@Test
	public void shouldGetSubPropertyClosureOf()
	{
		OWLObjectProperty fakeRel1 = 
				wrapper.getOWLObjectPropertyByIdentifier("fake_rel1");
		List<OWLObjectProperty> expectedSubProps = new ArrayList<OWLObjectProperty>();
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel2"));
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel3"));
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel4"));
		//fake_rel3 and fake_rel4 are sub-properties of fake_rel2, 
		//which is the sub-property of fake_rel1
		//we also test the order of the returned properties
		LinkedHashSet<OWLObjectPropertyExpression> subprops = 
				wrapper.getSubPropertyClosureOf(fakeRel1);
		assertEquals("Incorrect sub-properties returned: ", 
				expectedSubProps, new ArrayList<OWLObjectPropertyExpression>(subprops));
		
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getSubPropertyReflexiveClosureOf(OWLObjectPropertyExpression)}.
	 */
	@Test
	public void shouldGetSubPropertyReflexiveClosureOf()
	{
		OWLObjectProperty fakeRel1 = 
				wrapper.getOWLObjectPropertyByIdentifier("fake_rel1");
		List<OWLObjectProperty> expectedSubProps = new ArrayList<OWLObjectProperty>();
		expectedSubProps.add(fakeRel1);
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel2"));
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel3"));
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel4"));
		//fake_rel3 and fake_rel4 are sub-properties of fake_rel2, 
		//which is the sub-property of fake_rel1
		//we also test the order of the returned properties
		LinkedHashSet<OWLObjectPropertyExpression> subprops = 
				wrapper.getSubPropertyReflexiveClosureOf(fakeRel1);
		assertEquals("Incorrect sub-properties returned: ", 
				expectedSubProps, new ArrayList<OWLObjectPropertyExpression>(subprops));
		
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getSuperPropertyReflexiveClosureOf(OWLObjectPropertyExpression)}.
	 */
	@Test
	public void shouldGetSuperPropertyReflexiveClosureOf()
	{
		OWLObjectProperty fakeRel3 = 
				wrapper.getOWLObjectPropertyByIdentifier("fake_rel3");
		List<OWLObjectProperty> expectedSubProps = new ArrayList<OWLObjectProperty>();
		expectedSubProps.add(fakeRel3);
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel2"));
		expectedSubProps.add(wrapper.getOWLObjectPropertyByIdentifier("fake_rel1"));
		//fake_rel3 is sub-property of fake_rel2, 
		//which is the sub-property of fake_rel1
		//we also test the order of the returned properties
		LinkedHashSet<OWLObjectPropertyExpression> superProps = 
				wrapper.getSuperPropertyReflexiveClosureOf(fakeRel3);
		assertEquals("Incorrect super properties returned: ", 
				expectedSubProps, new ArrayList<OWLObjectPropertyExpression>(superProps));
		
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOWLGraphEdgeSubRelsReflexive(OWLGraphEdge)}.
	 */
	@Test
	public void shouldGetOWLGraphEdgeSubRelsReflexive()
	{
		OWLOntology ont = wrapper.getSourceOntology();
		OWLClass source = 
				wrapper.getOWLClassByIdentifier("FOO:0001");
		OWLClass target = 
				wrapper.getOWLClassByIdentifier("FOO:0002");
		OWLObjectProperty overlaps = 
				wrapper.getOWLObjectPropertyByIdentifier("RO:0002131");
		OWLObjectProperty partOf = 
				wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty hasPart = 
				wrapper.getOWLObjectPropertyByIdentifier("BFO:0000051");
		OWLObjectProperty inDeepPartOf = 
				wrapper.getOWLObjectPropertyByIdentifier("in_deep_part_of");
		OWLGraphEdge sourceEdge = new OWLGraphEdge(source, target, overlaps, 
				Quantifier.SOME, ont);
		OWLGraphEdge partOfEdge = new OWLGraphEdge(source, target, partOf, 
				Quantifier.SOME, ont);
		OWLGraphEdge hasPartEdge = new OWLGraphEdge(source, target, hasPart, 
				Quantifier.SOME, ont);
		OWLGraphEdge deepPartOfEdge = new OWLGraphEdge(source, target, inDeepPartOf, 
				Quantifier.SOME, ont);
		
		LinkedHashSet<OWLGraphEdge> subRels = 
				wrapper.getOWLGraphEdgeSubRelsReflexive(sourceEdge);
		int edgeIndex = 0;
		for (OWLGraphEdge edge: subRels) {
			if (edgeIndex == 0) {
				assertEquals("Incorrect sub-rels returned at index 0", sourceEdge, edge);
			} else if (edgeIndex == 1 || edgeIndex == 2) {
				assertTrue("Incorrect sub-rels returned at index 1 or 2: " + edge, 
						edge.equals(partOfEdge) || edge.equals(hasPartEdge));
			} else if (edgeIndex == 3) {
				assertEquals("Incorrect sub-rels returned at index 3", 
						deepPartOfEdge, edge);
			}
			edgeIndex++;
		}
		assertTrue("No sub-relations returned", edgeIndex > 0);
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#combinePropertyPairOverSuperProperties(
	 * OWLQuantifiedProperty, OWLQuantifiedProperty)}.
	 * 
	 * @throws Exception 
	 */
	@Test
	public void shouldCombinePropertyPairOverSuperProperties() throws Exception
	{
		//try to combine a has_developmental_contribution_from 
		//and a transformation_of relation (one is a super property of the other, 
		//2 levels higher)
		OWLObjectProperty transf = wrapper.getOWLObjectPropertyByIdentifier(
				"http://semanticscience.org/resource/SIO_000657");
		OWLQuantifiedProperty transfQp = 
				new OWLQuantifiedProperty(transf, Quantifier.SOME);
		OWLObjectProperty devCont = wrapper.getOWLObjectPropertyByIdentifier("RO:0002254");
		OWLQuantifiedProperty devContQp = 
				new OWLQuantifiedProperty(devCont, Quantifier.SOME);
		
		OWLQuantifiedProperty combine = wrapper.combinePropertyPairOverSuperProperties(transfQp, devContQp);
		assertEquals("relations SIO:000657 and RO:0002254 were not properly combined " +
				"into RO:0002254", devContQp, combine);
		//combine in the opposite direction, just to be sure :p
		combine = wrapper.combinePropertyPairOverSuperProperties(devContQp, transfQp);
		assertEquals("Reversing relations in method call generated an error", 
				devContQp, combine);
		
		//another test case: two properties where none is parent of the other one, 
		//sharing several common parents, only the more general one is transitive.
		//fake_rel3 and fake_rel4 are both sub-properties of fake_rel2, 
		//which is not transitive, but has the super-property fake_rel1 
		//which is transitive. fake_rel3 and fake_rel4 should be combined into fake_rel1.
		OWLObjectProperty fakeRel3 = wrapper.getOWLObjectPropertyByIdentifier("fake_rel3");
		OWLQuantifiedProperty fakeRel3Qp = 
				new OWLQuantifiedProperty(fakeRel3, Quantifier.SOME);
		OWLObjectProperty fakeRel4 = wrapper.getOWLObjectPropertyByIdentifier("fake_rel4");
		OWLQuantifiedProperty fakeRel4Qp = 
				new OWLQuantifiedProperty(fakeRel4, Quantifier.SOME);
		
		combine = wrapper.combinePropertyPairOverSuperProperties(fakeRel3Qp, fakeRel4Qp);
		OWLObjectProperty fakeRel1 = wrapper.getOWLObjectPropertyByIdentifier("fake_rel1");
		assertEquals("relations fake_rel3 and fake_rel4 were not properly combined " +
				"into fake_rel1", fakeRel1, combine.getProperty());
		//combine in the opposite direction, just to be sure :p
		combine = wrapper.combinePropertyPairOverSuperProperties(fakeRel4Qp, fakeRel3Qp);
		assertEquals("Reversing relations in method call generated an error", 
				fakeRel1, combine.getProperty());
		
		//another test case: part_of o develops_from -> develops_from 
		//fake_rel5 is a sub-property of develops_from, so we should have 
		//part_of o fake_rel5 -> develops_from
		OWLObjectProperty fakeRel5 = wrapper.getOWLObjectPropertyByIdentifier("fake_rel5");
        OWLQuantifiedProperty fakeRel5Qp = 
                new OWLQuantifiedProperty(fakeRel5, Quantifier.SOME);
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLQuantifiedProperty partOfQp = 
                new OWLQuantifiedProperty(partOf, Quantifier.SOME);
        
        combine = wrapper.combinePropertyPairOverSuperProperties(partOfQp, fakeRel5Qp);
        OWLObjectProperty dvlpFrom = wrapper.getOWLObjectPropertyByIdentifier("RO:0002202");
        assertEquals("relations part_of and fake_rel5 were not properly combined " +
                "into develops_from", dvlpFrom, combine.getProperty());
        
        //should  work also with a sub-property of part_of
        OWLObjectProperty deepPartOf = wrapper.getOWLObjectPropertyByIdentifier("in_deep_part_of");
        OWLQuantifiedProperty deepPartOfQp = 
                new OWLQuantifiedProperty(deepPartOf, Quantifier.SOME);
        combine = wrapper.combinePropertyPairOverSuperProperties(deepPartOfQp, fakeRel5Qp);
        assertEquals("relations in_deep_part_of and fake_rel5 were not properly combined " +
                "into develops_from", dvlpFrom, combine.getProperty());
        
        //finally, check that the method produce the same result 
        //as combinedQuantifiedPropertyPair, for instance with the fake_rel3, 
        //which is transitive
        combine = wrapper.combinePropertyPairOverSuperProperties(fakeRel3Qp, fakeRel3Qp);
        assertEquals("relations fake_rel3 and fake_rel3 were not properly combined " +
                "into fake_rel3", fakeRel3, combine.getProperty());
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#combineEdgePairWithSuperProps(OWLGraphEdge, OWLGraphEdge)}.
	 */
	@Test
	public void shouldCombineEdgePairWithSuperProps()
	{
		OWLOntology ont = wrapper.getSourceOntology();
		OWLClass source = 
				wrapper.getOWLClassByIdentifier("FOO:0001");
		OWLClass target = 
				wrapper.getOWLClassByIdentifier("FOO:0002");
		OWLClass target2 = 
				wrapper.getOWLClassByIdentifier("FOO:0003");
		OWLObjectProperty overlaps = 
				wrapper.getOWLObjectPropertyByIdentifier("RO:0002131");
		OWLObjectProperty partOf = 
				wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLGraphEdge edge1 = new OWLGraphEdge(source, target, overlaps, 
				Quantifier.SOME, ont);
		OWLGraphEdge edge2 = new OWLGraphEdge(target, target2, partOf, 
				Quantifier.SOME, ont);
		OWLGraphEdge expectedEdge = new OWLGraphEdge(source, target2, overlaps, 
				Quantifier.SOME, ont);
		
		assertEquals("Incorrect combined relation", expectedEdge, 
				wrapper.combineEdgePairWithSuperProps(edge1, edge2));
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOutgoingEdgesNamedClosureOverSupProps(OWLObject)}.
	 * Note that this method uses a different test ontology than the other tests: 
	 * {@code graph/superObjectProps.obo}.
	 */
	@Test
	public void shouldGetNamedClosureOverSuperProps() throws OWLOntologyCreationException, 
	    OBOFormatParserException, IOException {
	    ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(OWLGraphWrapperEdgesExtendedTest.class.getResource(
                "/graph/superObjectProps.obo").getFile());
        OWLGraphWrapper ontWrapper = new OWLGraphWrapper(ont);
        
        //get all required objects
        OWLClass foo1 = ontWrapper.getOWLClassByIdentifier("FOO:0001");
        OWLClass foo2 = ontWrapper.getOWLClassByIdentifier("FOO:0002");
        OWLClass foo3 = ontWrapper.getOWLClassByIdentifier("FOO:0003");
        OWLClass foo4 = ontWrapper.getOWLClassByIdentifier("FOO:0004");
        OWLClass foo5 = ontWrapper.getOWLClassByIdentifier("FOO:0005");
        OWLClass foo6 = ontWrapper.getOWLClassByIdentifier("FOO:0006");
        OWLClass foo7 = ontWrapper.getOWLClassByIdentifier("FOO:0007");
        
        OWLGraphEdge foo2IsAFoo1 = 
                new OWLGraphEdge(foo2, foo1, null, Quantifier.SUBCLASS_OF, ont);
        
        OWLObjectProperty partOf = ontWrapper.
                getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLGraphEdge foo3PartOfFoo1 = 
                new OWLGraphEdge(foo3, foo1, partOf, Quantifier.SOME, ont);
        
        OWLObjectProperty fakeRel = ontWrapper.
                getOWLObjectPropertyByIdentifier("fake_rel1");
        OWLGraphEdge foo4ToFoo3 = 
                new OWLGraphEdge(foo4, foo3, fakeRel, Quantifier.SOME, ont);
        OWLGraphEdge foo4ToFoo1 = 
                new OWLGraphEdge(foo4, foo1, Arrays.asList(
                        new OWLQuantifiedProperty(fakeRel, Quantifier.SOME), 
                        new OWLQuantifiedProperty(partOf, Quantifier.SOME)), 
                        ont);
        
        OWLObjectProperty deepPartOf = ontWrapper.
                getOWLObjectPropertyByIdentifier("in_deep_part_of");
        OWLGraphEdge foo5ToFoo3 = 
                new OWLGraphEdge(foo5, foo3, deepPartOf, Quantifier.SOME, ont);
        OWLGraphEdge foo5ToFoo1 = 
                new OWLGraphEdge(foo5, foo1, partOf, Quantifier.SOME, ont);
        
        OWLObjectProperty developsFrom = ontWrapper.
                getOWLObjectPropertyByIdentifier("RO:0002202");
        OWLGraphEdge foo6ToFoo2 = 
                new OWLGraphEdge(foo6, foo2, deepPartOf, Quantifier.SOME, ont);
        OWLGraphEdge foo6ToFoo3 = 
                new OWLGraphEdge(foo6, foo3, developsFrom, Quantifier.SOME, ont);
        OWLGraphEdge foo6ToFoo1 = 
                new OWLGraphEdge(foo6, foo1, deepPartOf, Quantifier.SOME, ont);
        OWLGraphEdge foo6ToFoo1Bis = 
                new OWLGraphEdge(foo6, foo1, developsFrom, Quantifier.SOME, ont);
        
        OWLGraphEdge foo7ToFoo5 = 
                new OWLGraphEdge(foo7, foo5, developsFrom, Quantifier.SOME, ont);
        OWLGraphEdge foo7ToFoo3 = 
                new OWLGraphEdge(foo7, foo3, developsFrom, Quantifier.SOME, ont);
        OWLGraphEdge foo7ToFoo1 = 
                new OWLGraphEdge(foo7, foo1, developsFrom, Quantifier.SOME, ont);
        
        //Start tests
        Set<OWLGraphEdge> expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(foo2IsAFoo1);
        assertEquals("Incorrect closure edges for foo2", expectedEdges, 
                ontWrapper.getOutgoingEdgesNamedClosureOverSupProps(foo2));
        
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(foo3PartOfFoo1);
        assertEquals("Incorrect closure edges for foo3", expectedEdges, 
                ontWrapper.getOutgoingEdgesNamedClosureOverSupProps(foo3));
        
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(foo4ToFoo3);
        expectedEdges.add(foo4ToFoo1);
        assertEquals("Incorrect closure edges for foo4", expectedEdges, 
                ontWrapper.getOutgoingEdgesNamedClosureOverSupProps(foo4));
        
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(foo5ToFoo3);
        expectedEdges.add(foo5ToFoo1);
        assertEquals("Incorrect closure edges for foo4", expectedEdges, 
                ontWrapper.getOutgoingEdgesNamedClosureOverSupProps(foo5));
        
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(foo6ToFoo3);
        expectedEdges.add(foo6ToFoo2);
        expectedEdges.add(foo6ToFoo1);
        expectedEdges.add(foo6ToFoo1Bis);
        assertEquals("Incorrect closure edges for foo4", expectedEdges, 
                ontWrapper.getOutgoingEdgesNamedClosureOverSupProps(foo6));
        
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(foo7ToFoo5);
        expectedEdges.add(foo7ToFoo3);
        expectedEdges.add(foo7ToFoo1);
        assertEquals("Incorrect closure edges for foo4", expectedEdges, 
                ontWrapper.getOutgoingEdgesNamedClosureOverSupProps(foo7));
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getGCIOutgoingEdges(OWLObject)} and 
	 * {@link OWLGraphWrapperEdgesExtended#getGCIIncomingEdges(OWLObject)}
	 * @throws OWLOntologyCreationException
	 * @throws OBOFormatParserException
	 * @throws IOException
	 */
	@Test
	public void shouldGetGCIEdges() throws OWLOntologyCreationException, 
	    OBOFormatParserException, IOException {
	    ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLObjectProperty developsFrom = wrapper.getOWLObjectPropertyByIdentifier("RO:0002202");
        OWLClass cls1 = wrapper.getOWLClassByIdentifier("ID:1");
        OWLClass cls2 = wrapper.getOWLClassByIdentifier("ID:2");
        OWLClass taxon1 = wrapper.getOWLClassByIdentifier("NCBITaxon:9606");
        OWLClass taxon2 = wrapper.getOWLClassByIdentifier("NCBITaxon:10090");
        
        Set<OWLGraphEdge> expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls2, cls1, partOf, Quantifier.SOME, 
                ont, null, taxon1, partOf));
        expectedEdges.add(new OWLGraphEdge(cls2, cls1, developsFrom, Quantifier.SOME, 
                ont, null, taxon2, partOf));
        assertEquals("Incorrect gci_relations retrieved", expectedEdges, 
                wrapper.getGCIOutgoingEdges(cls2));
        assertEquals("Incorrect gci_relations retrieved", expectedEdges, 
                wrapper.getGCIIncomingEdges(cls1));
        
        //check empty GCIs
        expectedEdges = new HashSet<OWLGraphEdge>();
        assertEquals("Incorrect gci_relations retrieved", expectedEdges, 
                wrapper.getGCIOutgoingEdges(wrapper.getOWLClassByIdentifier("ID:3")));
        assertEquals("Incorrect gci_relations retrieved", expectedEdges, 
                wrapper.getGCIIncomingEdges(wrapper.getOWLClassByIdentifier("ID:3")));
        
        //check that usual getOutgoingEdges is not affected
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls2, cls1, ont));
        assertEquals("Incorrect non-gci_relations retrieved", expectedEdges, 
                wrapper.getOutgoingEdges(cls2));
        assertEquals("Incorrect non-gci_relations retrieved", expectedEdges, 
                wrapper.getIncomingEdges(cls1));
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOutgoingEdgesWithGCI(OWLObject)}
	 */
	@Test
	public void shouldGetOutgoingEdgesWithGCI() throws OWLOntologyCreationException, 
	OBOFormatParserException, IOException {
	    ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLObjectProperty developsFrom = wrapper.getOWLObjectPropertyByIdentifier("RO:0002202");
        OWLClass cls6 = wrapper.getOWLClassByIdentifier("ID:6");
        OWLClass cls7 = wrapper.getOWLClassByIdentifier("ID:7");
        OWLClass cls8 = wrapper.getOWLClassByIdentifier("ID:8");
        OWLClass cls9 = wrapper.getOWLClassByIdentifier("ID:9");
        OWLClass cls10 = wrapper.getOWLClassByIdentifier("ID:10");
        OWLClass taxon1 = wrapper.getOWLClassByIdentifier("NCBITaxon:9606");
        
        Set<OWLGraphEdge> expectedEdges = new HashSet<OWLGraphEdge>();
        //class with no GCI relations
        expectedEdges.add(new OWLGraphEdge(cls8, cls6, partOf, Quantifier.SOME, 
                ont, null));
        expectedEdges.add(new OWLGraphEdge(cls8, cls7, developsFrom, Quantifier.SOME, 
                ont, null));
        assertEquals("Incorrect outgoing edges with GCI retrieved", expectedEdges, 
                wrapper.getOutgoingEdgesWithGCI(cls8));
        //class with both a classical relation and a GCI relation
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls9, cls7, partOf, Quantifier.SOME, 
                ont, null));
        expectedEdges.add(new OWLGraphEdge(cls9, cls10, developsFrom, Quantifier.SOME, 
                ont, null, taxon1, partOf));
        assertEquals("Incorrect outgoing edges with GCI retrieved", expectedEdges, 
                wrapper.getOutgoingEdgesWithGCI(cls9));
	}
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getIncomingEdgesWithGCI(OWLObject)}
     */
    @Test
    public void shouldGetIncomingEdgesWithGCI() throws OWLOntologyCreationException, 
    OBOFormatParserException, IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLObjectProperty developsFrom = wrapper.getOWLObjectPropertyByIdentifier("RO:0002202");
        OWLClass cls8 = wrapper.getOWLClassByIdentifier("ID:8");
        OWLClass cls5 = wrapper.getOWLClassByIdentifier("ID:5");
        OWLClass cls10 = wrapper.getOWLClassByIdentifier("ID:10");
        OWLClass cls7 = wrapper.getOWLClassByIdentifier("ID:7");
        OWLClass cls6 = wrapper.getOWLClassByIdentifier("ID:6");
        OWLClass taxon1 = wrapper.getOWLClassByIdentifier("NCBITaxon:10090");
        
        Set<OWLGraphEdge> expectedEdges = new HashSet<OWLGraphEdge>();
        //class with no incoming GCI relations
        expectedEdges.add(new OWLGraphEdge(cls7, cls5, partOf, Quantifier.SOME, 
                ont, null));
        assertEquals("Incorrect incoming edges with GCI retrieved", expectedEdges, 
                wrapper.getIncomingEdgesWithGCI(cls5));
        //class with both a classical relation and a GCI relation incoming
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls8, cls6, partOf, Quantifier.SOME, 
                ont, null));
        expectedEdges.add(new OWLGraphEdge(cls10, cls6, developsFrom, Quantifier.SOME, 
                ont, null, taxon1, partOf));
        assertEquals("Incorrect incoming edges with GCI retrieved", expectedEdges, 
                wrapper.getIncomingEdgesWithGCI(cls6));
    }
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getNamedAncestorsWithGCI(OWLClass)} and 
     * {@link OWLGraphWrapperEdgesExtended#getOWLClassAncestorsWithGCI(OWLClass)}.
     */
    @Test
    public void shouldGetGCIAncestors() throws OWLOntologyCreationException, 
    OBOFormatParserException, IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLClass cls8 = wrapper.getOWLClassByIdentifier("ID:8");
        OWLClass cls7 = wrapper.getOWLClassByIdentifier("ID:7");
        OWLClass cls6 = wrapper.getOWLClassByIdentifier("ID:6");
        OWLClass cls5 = wrapper.getOWLClassByIdentifier("ID:5");
        OWLClass cls4 = wrapper.getOWLClassByIdentifier("ID:4");
        OWLClass cls9 = wrapper.getOWLClassByIdentifier("ID:9");
        OWLClass cls10 = wrapper.getOWLClassByIdentifier("ID:10");
        
        Set<OWLClass> expectedAncestors = new HashSet<OWLClass>();
        //no ancestors through GCI
        expectedAncestors.add(cls4);
        expectedAncestors.add(cls5);
        expectedAncestors.add(cls6);
        expectedAncestors.add(cls7);
        assertEquals("Incorrect ancestors with GCI", expectedAncestors, 
                wrapper.getNamedAncestorsWithGCI(cls8));
        //ancestors with GCI
        expectedAncestors = new HashSet<OWLClass>();
        expectedAncestors.add(cls10);
        expectedAncestors.add(cls4);
        expectedAncestors.add(cls7);
        expectedAncestors.add(cls6);
        expectedAncestors.add(cls5);
        assertEquals("Incorrect ancestors through GCI and classical relations", 
                expectedAncestors, wrapper.getNamedAncestorsWithGCI(cls9));
        assertEquals("Incorrect ancestors through GCI and classical relations", 
                expectedAncestors, wrapper.getOWLClassAncestorsWithGCI(cls9));
    }
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassDirectDescendantsWithGCI(OWLClass)}, 
     * and {@link OWLGraphWrapperEdgesExtended#getOWLClassDescendantsWithGCI(OWLClass)}
     */
    @Test
    public void shouldGetGCIDescendants() throws OWLOntologyCreationException, 
    OBOFormatParserException, IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLClass cls8 = wrapper.getOWLClassByIdentifier("ID:8");
        OWLClass cls5 = wrapper.getOWLClassByIdentifier("ID:5");
        OWLClass cls6 = wrapper.getOWLClassByIdentifier("ID:6");
        OWLClass cls7 = wrapper.getOWLClassByIdentifier("ID:7");
        OWLClass cls4 = wrapper.getOWLClassByIdentifier("ID:4");
        OWLClass cls9 = wrapper.getOWLClassByIdentifier("ID:9");
        OWLClass cls10 = wrapper.getOWLClassByIdentifier("ID:10");
        
        Set<OWLClass> expecteDescendants = new HashSet<OWLClass>();
        //no descendants through GCI
        expecteDescendants.add(cls7);
        expecteDescendants.add(cls8);
        expecteDescendants.add(cls9);
        assertEquals("Incorrect ancestors through GCI", expecteDescendants, 
                wrapper.getOWLClassDescendantsWithGCI(cls5));
        //descendants with GCI
        expecteDescendants = new HashSet<OWLClass>();
        expecteDescendants.add(cls10);
        expecteDescendants.add(cls9);
        expecteDescendants.add(cls6);
        expecteDescendants.add(cls8);
        assertEquals("Incorrect descendants through GCI and classical relations", 
                expecteDescendants, wrapper.getOWLClassDescendantsWithGCI(cls4));
        //direct descendants through both classical relation and GCI
        expecteDescendants = new HashSet<OWLClass>();
        expecteDescendants.add(cls8);
        expecteDescendants.add(cls10);
        assertEquals("Incorrect direct descendant through GCI and classical relations", 
                expecteDescendants, wrapper.getOWLClassDirectDescendantsWithGCI(cls6));
    }
	
	/**
	 * Test {@link OWLGraphEdge#equalsGCI(OWLGraphEdge)}
	 */
	@Test
	public void testEqualsGCI() throws OWLOntologyCreationException, 
	OBOFormatParserException, IOException {
	    ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLObjectProperty developsFrom = wrapper.getOWLObjectPropertyByIdentifier("RO:0002202");
        OWLClass cls1 = wrapper.getOWLClassByIdentifier("ID:1");
        OWLClass cls2 = wrapper.getOWLClassByIdentifier("ID:2");
        OWLClass taxon1 = wrapper.getOWLClassByIdentifier("NCBITaxon:9606");
        OWLClass taxon2 = wrapper.getOWLClassByIdentifier("NCBITaxon:10090");
        
        assertTrue("Incorrect value returned by equalsGCI", 
                new OWLGraphEdge(cls2, cls1, ont, null, taxon1, partOf).equalsGCI(
                new OWLGraphEdge(cls1, cls2, 
                        ont, null, taxon1, partOf)));
        assertFalse("Incorrect value returned by equalsGCI", 
                new OWLGraphEdge(cls2, cls1, ont, null, taxon1, partOf).equalsGCI(
                new OWLGraphEdge(cls2, cls1, 
                        ont, null, taxon2, partOf)));
        assertFalse("Incorrect value returned by equalsGCI", 
                new OWLGraphEdge(cls2, cls1, ont, null, taxon1, developsFrom).equalsGCI(
                new OWLGraphEdge(cls2, cls1, 
                        ont, null, taxon1, partOf)));
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#edgeToSourceExpression(OWLGraphEdge)}
	 */
	@Test
	public void shouldGetEdgeToSourceExpression() throws OWLOntologyCreationException, 
	OBOFormatParserException, IOException {
	    ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLClass cls1 = wrapper.getOWLClassByIdentifier("ID:1");
        OWLClass cls2 = wrapper.getOWLClassByIdentifier("ID:2");
        OWLClass taxon1 = wrapper.getOWLClassByIdentifier("NCBITaxon:9606");
        
        OWLGraphEdge edge = new OWLGraphEdge(cls2, cls1, partOf, Quantifier.SOME, ont, 
                null, taxon1, partOf);
        OWLDataFactory factory = ont.getOWLOntologyManager().getOWLDataFactory();
        OWLClassExpression expectedExpression = factory.getOWLObjectIntersectionOf(
                cls2, factory.getOWLObjectSomeValuesFrom(partOf, taxon1));
        
        assertEquals("Incorrect edgeToSourceExpression returned", expectedExpression, 
                wrapper.edgeToSourceExpression(edge));
        
        //non-GCI
        edge = new OWLGraphEdge(cls2, cls1, ont);
        assertEquals("Incorrect edgeToSourceExpression returned", cls2, 
                wrapper.edgeToSourceExpression(edge));
	}
	
	/**
	 * Test the method {@link OWLGraphWrapperEdgesExtended#clearCachedEdges()}.
	 */
	@Test
	public void shouldClearCachedEdges() throws OWLOntologyCreationException, 
	OBOFormatParserException, IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
	    OWLOntology ont = parserWrapper.parse(this.getClass().getResource(
                "/graph/gciRelRetrieval.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);

        OWLClass cls1 = wrapper.getOWLClassByIdentifier("ID:1");
        OWLClass cls2 = wrapper.getOWLClassByIdentifier("ID:2");
        
        //get GCI relations, this will load the cache
        assertEquals("Incorrect number of gci_relations returned", 2, 
                wrapper.getGCIOutgoingEdges(cls2).size());
        //delete a gci_relation, without clearing the cache
        ont.getOWLOntologyManager().removeAxioms(ont, 
                wrapper.getGCIOutgoingEdges(cls2).iterator().next().getAxioms());
        //same number of axioms seen
        assertEquals("Incorrect number of gci_relations returned", 2, 
                wrapper.getGCIOutgoingEdges(cls2).size());
        assertEquals("Incorrect number of gci_relations returned", 2, 
                wrapper.getGCIIncomingEdges(cls1).size());
        //clear cache, we should see the change
        wrapper.clearCachedEdges();
        assertEquals("Incorrect number of gci_relations returned", 1, 
                wrapper.getGCIOutgoingEdges(cls2).size());
        assertEquals("Incorrect number of gci_relations returned", 1, 
                wrapper.getGCIIncomingEdges(cls1).size());
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getAllOWLClasses()}
	 */
	@Test
	public void shouldGetAllOWLClasses()
	{
		assertEquals("Incorrect Set of OWLClasses returned", 17, 
				wrapper.getAllOWLClasses().size());
	}
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getAllOWLClassesFromSource()}
     */
    @Test
    public void shouldGetAllOWLClassesFromSource()
    {
        assertEquals("Incorrect Set of OWLClasses returned", 16, 
                wrapper.getAllOWLClassesFromSource().size());
    }
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOntologyRoots()}
	 */
	@Test
	public void shouldGetOntologyRoots()
	{
		//the ontology has 2 roots, FOO:0001 and FOO:0100
	    //NCBITaxon are due to GCI relations
		Set<OWLClass> roots = wrapper.getOntologyRoots();
		assertTrue("Incorrect roots returned: " + roots, 
				roots.size() == 4 && 
				roots.contains(wrapper.getOWLClassByIdentifier("FOO:0001")) && 
				roots.contains(wrapper.getOWLClassByIdentifier("FOO:0100")) && 
                roots.contains(wrapper.getOWLClassByIdentifier("NCBITaxon:9606")) && 
                roots.contains(wrapper.getOWLClassByIdentifier("NCBITaxon:10090")));
	}
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getOntologyLeaves()}
     */
    @Test
    public void shouldGetOntologyLeaves()
    {
        //the ontology has 9 leaves, FOO:0100, FOO:0003, FOO:0005, FOO:0010, FOO:0011, 
        //FOO:0012, FOO:0013, FOO:0014 and FOO:0015, an the taxon due to GCIs
        Set<OWLClass> leaves = wrapper.getOntologyLeaves();
        assertTrue("Incorrect leaves returned: " + leaves, 
                leaves.size() == 9 && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0100")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0003")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0005")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0010")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0011")) &&  
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0014")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0015")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("NCBITaxon:9606")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("NCBITaxon:10090")));
    }
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassDescendants(OWLClass)}
	 */
	@Test
	public void shouldGetOWLClassDescendants()
	{
		Set<OWLClass> descendants = wrapper.getOWLClassDescendants(
				wrapper.getOWLClassByIdentifier("FOO:0007"));
		assertTrue("Incorrect descendants returned: " + descendants, 
				descendants.size() == 3 && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0010")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0011")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0009")) );
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassDirectDescendants(OWLClass)}
	 */
	@Test
	public void shouldGetOWLClassDirectDescendants()
	{
		Set<OWLClass> descendants = wrapper.getOWLClassDirectDescendants(
				wrapper.getOWLClassByIdentifier("FOO:0009"));
		assertTrue("Incorrect descendants returned: " + descendants, 
				descendants.size() == 2 && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0010")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0011")));
	}
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassDirectAncestors(OWLClass)}
     */
    @Test
    public void shouldGetOWLClassDirectAncestors()
    {
        Set<OWLClass> parents = wrapper.getOWLClassDirectAncestors(
                wrapper.getOWLClassByIdentifier("FOO:0011"));
        //FOO:0011 has 2 direct parents, FOO:0009 and FOO:0002
        assertTrue("Incorrect parents returned: " + parents, 
                parents.size() == 2 && 
                parents.contains(wrapper.getOWLClassByIdentifier("FOO:0009")) && 
                parents.contains(wrapper.getOWLClassByIdentifier("FOO:0002")));
    }
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassAncestors(OWLClass)}
	 */
	@Test
	public void shouldGetOWLClassAncestors()
	{
		Set<OWLClass> ancestors = wrapper.getOWLClassAncestors(
				wrapper.getOWLClassByIdentifier("FOO:0009"));
		//FOO:0009 has one parent, FOO:0007, which has one parent, FOO:0006, 
		//which has one parent, FOO:0001
		assertTrue("Incorrect ancestors returned: " + ancestors, 
				ancestors.size() == 3 && 
				ancestors.contains(wrapper.getOWLClassByIdentifier("FOO:0007")) && 
				ancestors.contains(wrapper.getOWLClassByIdentifier("FOO:0006")) && 
				ancestors.contains(wrapper.getOWLClassByIdentifier("FOO:0001")) );
	}
}
