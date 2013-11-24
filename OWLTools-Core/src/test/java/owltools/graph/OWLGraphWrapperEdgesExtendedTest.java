package owltools.graph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.ParserWrapper;

/**
 * Test of {@link OWLGraphWrapperEdgesExtended}.
 * @author Frederic Bastian
 * @version November 2013
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
	public OWLGraphWrapperEdgesExtendedTest()
	{
		super();
	}
//	@Override
//	protected Logger getLogger() {
//		return log;
//	}
	
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
		//2 levels higher, interesting unit test)
		OWLObjectProperty transf = wrapper.getOWLObjectPropertyByIdentifier(
				"http://semanticscience.org/resource/SIO_000657");
		OWLQuantifiedProperty transfQp = 
				new OWLQuantifiedProperty(transf, Quantifier.SOME);
		OWLObjectProperty devCont = wrapper.getOWLObjectPropertyByIdentifier("RO:0002254");
		OWLQuantifiedProperty devContQp = 
				new OWLQuantifiedProperty(devCont, Quantifier.SOME);
		
		//"combinePropertyPairOverSuperProperties" is package private, so we can test it
		OWLQuantifiedProperty combine = wrapper.combinePropertyPairOverSuperProperties(transfQp, devContQp);
		assertEquals("relations SIO:000657 and RO:0002254 were not properly combined " +
				"into RO:0002254", devContQp, combine);
		//combine in the opposite direction, just to be sure :p
		combine = wrapper.combinePropertyPairOverSuperProperties(devContQp, transfQp);
		assertEquals("Reversing relations in method call generated an error", 
				devContQp, combine);
		
		//another test case: two properties where none is parent of the other one, 
		//sharing several common parents, only the more general one is transitive. 
		//as I couldn't find any suitable example, fake relations were created
		//in the test ontology: 
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
	 * Test {@link OWLGraphWrapperEdgesExtended#getAllOWLClasses()}
	 */
	@Test
	public void shouldGetAllOWLClasses()
	{
		assertEquals("Incorrect Set of OWLClasses returned", 16, 
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
		Set<OWLClass> roots = wrapper.getOntologyRoots();
		assertTrue("Incorrect roots returned: " + roots, 
				roots.size() == 2 && 
				roots.contains(wrapper.getOWLClassByIdentifier("FOO:0001")) && 
				roots.contains(wrapper.getOWLClassByIdentifier("FOO:0100")));
	}
    
    /**
     * Test {@link OWLGraphWrapperEdgesExtended#getOntologyLeaves()}
     */
    @Test
    public void shouldGetOntologyLeaves()
    {
        //the ontology has 8 leaves, FOO:0100, FOO:0003, FOO:0005, FOO:0010, FOO:0011, 
        //FOO:0012, FOO:0013, FOO:0014 and FOO:0015
        Set<OWLClass> leaves = wrapper.getOntologyLeaves();
        assertTrue("Incorrect leaves returned: " + leaves, 
                leaves.size() == 9 && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0100")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0003")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0005")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0010")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0011")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0012")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0013")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0014")) && 
                leaves.contains(wrapper.getOWLClassByIdentifier("FOO:0015")));
    }
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassDescendants(OWLClass)}
	 */
	@Test
	public void shouldGetOWLClassDescendants()
	{
		Set<OWLClass> descendants = wrapper.getOWLClassDescendants(
				wrapper.getOWLClassByIdentifier("FOO:0002"));
		//FOO:0002 has 4 direct descendant, FOO:0004, FOO:0005, FOO:0011, and FOO:0014
		//FOO:0004 has two direct descendants, FOO:0003 and FOO:0015
		assertTrue("Incorrect descendants returned: " + descendants, 
				descendants.size() == 6 && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0004")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0005")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0011")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0014")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0003"))  && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0015")) );
	}
	
	/**
	 * Test {@link OWLGraphWrapperEdgesExtended#getOWLClassDirectDescendants(OWLClass)}
	 */
	@Test
	public void shouldGetOWLClassDirectDescendants()
	{
		Set<OWLClass> descendants = wrapper.getOWLClassDirectDescendants(
				wrapper.getOWLClassByIdentifier("FOO:0002"));
		//FOO:0002 has 4 direct descendant, FOO:0004, FOO:0005, FOO:0011, and FOO:0014
		assertTrue("Incorrect descendants returned: " + descendants, 
				descendants.size() == 4 && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0004")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0005")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0011")) && 
				descendants.contains(wrapper.getOWLClassByIdentifier("FOO:0014")) );
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
				wrapper.getOWLClassByIdentifier("FOO:0008"));
		//FOO:0008 has one parent, FOO:0007, which has one parent, FOO:0006, 
		//which has one parent, FOO:0001
		assertTrue("Incorrect ancestors returned: " + ancestors, 
				ancestors.size() == 3 && 
				ancestors.contains(wrapper.getOWLClassByIdentifier("FOO:0007")) && 
				ancestors.contains(wrapper.getOWLClassByIdentifier("FOO:0006")) && 
				ancestors.contains(wrapper.getOWLClassByIdentifier("FOO:0001")) );
	}
}
