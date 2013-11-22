package owltools.graph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.io.ParserWrapper;

/**
 * Test the methods related to acquisition of sub-annotation properties 
 * in {@link OWLGraphWrapperEdgesExtended}.
 * @author Frederic Bastian
 * @version November 2013
 *
 */
public class OGWSubAnnotationTest {
    private final static Logger log = 
            LogManager.getLogger(OGWSubAnnotationTest.class.getName());
    
    private static OWLGraphWrapper wrapper;
    private static OWLAnnotationProperty subsetProp;
    private static OWLAnnotationProperty groupProp;
    private static OWLAnnotationProperty fake1Prop;
    private static OWLAnnotationProperty fake2Prop;
    private static OWLAnnotationProperty lonelyProp;
    /**
     * Default Constructor. 
     */
    public OGWSubAnnotationTest()
    {
        super();
    }
    
    /**
     * Load the ontology <code>/graph/subannotprops.owl</code> into {@link #wrapper}, 
     * and also loads the {@code OWLAnnotationProperty}s needed.
     */
    @BeforeClass
    public static void loadTestOntology() 
            throws OWLOntologyCreationException, OBOFormatParserException, 
            IOException {
        ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(OGWSubAnnotationTest.class.getResource(
                "/graph/subannotprops.owl").getFile());
        wrapper = new OWLGraphWrapper(ont);
        OWLDataFactory f = wrapper.getManager().getOWLDataFactory();
        subsetProp = f.getOWLAnnotationProperty(
                IRI.create("http://www.geneontology.org/formats/oboInOwl#SubsetProperty"));
        groupProp = f.getOWLAnnotationProperty(
                IRI.create("http://purl.obolibrary.org/obo/uberon/core#grouping_class"));
        fake1Prop = f.getOWLAnnotationProperty(
                IRI.create("http://purl.obolibrary.org/obo/uberon/core#fake1"));
        fake2Prop = f.getOWLAnnotationProperty(
                IRI.create("http://purl.obolibrary.org/obo/uberon/core#fake2"));
        lonelyProp = f.getOWLAnnotationProperty(
                IRI.create("http://purl.obolibrary.org/obo/uberon/core#lonely"));
    }

    
    /**
     * Test {@link 
     * OWLGraphWrapperEdgesExtended#getSubAnnotationPropertiesOf(OWLAnnotationProperty)}.
     */
    @Test
    public void shouldGetSubAnnotationPropertiesOf() {
        Set<OWLAnnotationProperty> expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(groupProp);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                wrapper.getSubAnnotationPropertiesOf(subsetProp));
        
        expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(fake1Prop);
        expectedSubprops.add(fake2Prop);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                wrapper.getSubAnnotationPropertiesOf(groupProp));
        
        assertEquals("Incorrect sub-properties returned", new HashSet<OWLAnnotationProperty>(),  
                wrapper.getSubAnnotationPropertiesOf(lonelyProp));
        
    }
    /**
     * Test {@link 
     * OWLGraphWrapperEdgesExtended#getSubAnnotationPropertyClosureOf(OWLAnnotationProperty)}.
     */
    @Test
    public void shouldGetSubAnnotationPropertyClosureOf() {
        Set<OWLAnnotationProperty> expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(groupProp);
        expectedSubprops.add(fake1Prop);
        expectedSubprops.add(fake2Prop);
        LinkedHashSet<OWLAnnotationProperty> subprops = 
                wrapper.getSubAnnotationPropertyClosureOf(subsetProp);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                subprops);
        assertEquals("Incorrect order for sub-properties", groupProp, 
                subprops.iterator().next());
        
        expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(fake1Prop);
        expectedSubprops.add(fake2Prop);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                wrapper.getSubAnnotationPropertyClosureOf(groupProp));
        
        assertEquals("Incorrect sub-properties returned", new HashSet<OWLAnnotationProperty>(),  
                wrapper.getSubAnnotationPropertyClosureOf(lonelyProp));
        
    }
    /**
     * Test {@link 
     * OWLGraphWrapperEdgesExtended#
     * getSubAnnotationPropertyReflexiveClosureOf(OWLAnnotationProperty)}.
     */
    @Test
    public void shouldGetSubAnnotationPropertyReflexiveClosureOf() {
        Set<OWLAnnotationProperty> expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(subsetProp);
        expectedSubprops.add(groupProp);
        expectedSubprops.add(fake1Prop);
        expectedSubprops.add(fake2Prop);
        LinkedHashSet<OWLAnnotationProperty> subprops = 
                wrapper.getSubAnnotationPropertyReflexiveClosureOf(subsetProp);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                subprops);
        Iterator<OWLAnnotationProperty> iterator = subprops.iterator();
        assertEquals("Incorrect order for sub-properties", subsetProp, 
                iterator.next());
        assertEquals("Incorrect order for sub-properties", groupProp, 
                iterator.next());
        
        expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(groupProp);
        expectedSubprops.add(fake1Prop);
        expectedSubprops.add(fake2Prop);
        subprops = 
                wrapper.getSubAnnotationPropertyReflexiveClosureOf(groupProp);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                subprops);
        assertEquals("Incorrect order for sub-properties", groupProp, 
                subprops.iterator().next());
        
        expectedSubprops = new HashSet<OWLAnnotationProperty>();
        expectedSubprops.add(lonelyProp);
        assertEquals("Incorrect sub-properties returned", expectedSubprops,  
                wrapper.getSubAnnotationPropertyReflexiveClosureOf(lonelyProp));
        
    }
    
}
