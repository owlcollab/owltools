package owltools.graph;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link OWLGraphEdge}.
 * @author Frederic Bastian
 * @version October 2014
 * @since October 2014
 */
public class OWLGraphEdgeTest {
    private final static Logger log = 
            LogManager.getLogger(OWLGraphEdgeTest.class.getName());
    
    /**
     * Test the methods {@link OWLGraphEdge#equals(OWLGraphEdge)}, 
     * {@link OWLGraphEdge#hashCode()}, 
     * {@link OWLGraphEdge#equalsIgnoreOntology(OWLGraphEdge)} and 
     * {@link OWLGraphEdge#equalsIgnoreOntologyAndGCI(OWLGraphEdge)}.
     * 
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    @Test
    public void testEquals() throws OWLOntologyCreationException, IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(OWLGraphWrapperEdgesExtendedTest.class.getResource(
                "/graph/OWLGraphManipulatorTest.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLClass source = wrapper.getOWLClassByIdentifier("FOO:0002");
        OWLClass target = wrapper.getOWLClassByIdentifier("FOO:0001");
        OWLClass taxon = wrapper.getOWLClassByIdentifier("NCBITaxon:9606");
        OWLClass taxon2 = wrapper.getOWLClassByIdentifier("NCBITaxon:10090");
        
        //everything identical
        OWLGraphEdge edge1 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, 
                ont, null, taxon, partOf);
        OWLGraphEdge edge2 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, 
                ont, null, taxon, partOf);
        assertTrue("Equal edges seen as non-equal", edge1.equals(edge2));
        assertEquals("Equal edges with different hashCodes", 
                edge1.hashCode(), edge2.hashCode());
        assertTrue("Equal edges seen as non-equal", edge1.equalsIgnoreOntology(edge2));
        assertTrue("Equal edges seen as non-equal", edge1.equalsIgnoreOntologyAndGCI(edge2));
        
        //different ontologies
        edge2 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, 
                wrapper.getManager().createOntology(), null, taxon, partOf);
        assertFalse("Different edges seen as equal", edge1.equals(edge2));
        assertTrue("Equal edges with different ontologies seen as non-equal " +
        		"by equalsIgnoreOntology", edge1.equalsIgnoreOntology(edge2));
        assertTrue("Equal edges with different ontologies seen as non-equal " +
                "by equalsIgnoreOntologyAndGCI", edge1.equalsIgnoreOntologyAndGCI(edge2));
        
        //different GCI fillers and relations
        edge2 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, 
                ont, null, taxon2, partOf);
        assertFalse("Different edges seen as equal", edge1.equals(edge2));
        assertTrue("Equal edges with different GCI fillers and relations seen as non-equal " +
                "by equalsIgnoreOntologyAndGCI", edge1.equalsIgnoreOntologyAndGCI(edge2));
    }
    
    /**
     * Test the methods {@link OWLGraphEdge#equalsGCI(OWLGraphEdge)}.
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    @Test
    public void testEqualGCIs() throws OWLOntologyCreationException, IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(OWLGraphWrapperEdgesExtendedTest.class.getResource(
                "/graph/OWLGraphManipulatorTest.obo").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        
        OWLObjectProperty partOf = wrapper.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLClass source = wrapper.getOWLClassByIdentifier("FOO:0002");
        OWLClass target = wrapper.getOWLClassByIdentifier("FOO:0001");
        OWLClass taxon = wrapper.getOWLClassByIdentifier("NCBITaxon:9606");
        OWLClass taxon2 = wrapper.getOWLClassByIdentifier("NCBITaxon:10090");
        
        //identical GCI parameters
        OWLGraphEdge edge1 = new OWLGraphEdge(source, target, null, Quantifier.SUBCLASS_OF, 
                ont, null, taxon, partOf);
        OWLGraphEdge edge2 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, 
                ont, null, taxon, partOf);
        assertTrue("Equal GCI parameters seen as non-equal", edge1.equalsGCI(edge2));
        
        //no GCI parameters for both edge
        edge1 = new OWLGraphEdge(source, target, null, Quantifier.SUBCLASS_OF, 
                ont, null, null, null);
        edge2 = new OWLGraphEdge(source, target, partOf, Quantifier.SOME, 
                ont, null, null, null);
        assertTrue("Equal GCI parameters seen as non-equal", edge1.equalsGCI(edge2));
        
        //different GCI parameters
        edge1 = new OWLGraphEdge(source, target, null, Quantifier.SUBCLASS_OF, 
                ont, null, taxon, partOf);
        edge2 = new OWLGraphEdge(source, target, null, Quantifier.SUBCLASS_OF, 
                ont, null, taxon2, partOf);
        assertFalse("Non-equal GCI parameters seen as equal", edge1.equalsGCI(edge2));
    }
}
