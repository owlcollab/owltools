package owltools.mooncat;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class SpeciesSubsetterUtilTest extends OWLToolsTestBasics {

	static boolean renderObo = true;
	
	@Test
	public void testSubsetterSpecies() throws Exception {
		ParserWrapper p = new ParserWrapper();
		p.setCheckOboDoc(false);
		OWLOntology owlOntology = p.parse(getResourceIRIString("speciesMergeTest.obo"));
		OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(graph.getSourceOntology());
		SpeciesSubsetterUtil smu = new SpeciesSubsetterUtil(graph);
		//smu.viewProperty = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		smu.taxClass = graph.getOWLClassByIdentifier("T:1");
		smu.reasoner = reasoner;
		smu.removeOtherSpecies();
		
		p.saveOWL(smu.ont, new OBOOntologyFormat(), "target/speciesSubset.obo");
		//p.saveOWL(smu.ont,  getResourceIRIString("target/speciesSubset.owl"));
		
		assertNull(graph.getOWLClassByIdentifier("U:24"));
	}
	
	/**
	 * Test {@link SpeciesSubsetterUtil#removeSpecies()}.
	 */
    @Test
    public void testRemoveSpecies() throws Exception {
        OWLOntology owlOntology = new ParserWrapper().parse(getResourceIRIString("speciesRemoveTest.obo"));
        OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
        OWLReasonerFactory rf = new ElkReasonerFactory();
        OWLReasoner reasoner = rf.createReasoner(graph.getSourceOntology());
        SpeciesSubsetterUtil smu = new SpeciesSubsetterUtil(graph);
        smu.taxClass = graph.getOWLClassByIdentifier("T:2");
        smu.reasoner = reasoner;
        smu.removeSpecies();
        
        assertEquals("Incorrect number of classes", 10,  graph.getAllOWLClasses().size());
        assertNull(graph.getOWLClassByIdentifier("U:21"));
        assertNull(graph.getOWLClassByIdentifier("U:22"));
        assertNull(graph.getOWLClassByIdentifier("U:23"));
        assertNull(graph.getOWLClassByIdentifier("U:24"));
        
        owlOntology = new ParserWrapper().parse(getResourceIRIString("speciesRemoveTest.obo"));
        graph = new OWLGraphWrapper(owlOntology);
        rf = new ElkReasonerFactory();
        reasoner = rf.createReasoner(graph.getSourceOntology());
        smu = new SpeciesSubsetterUtil(graph);
        smu.taxClass = graph.getOWLClassByIdentifier("T:3");
        smu.reasoner = reasoner;
        smu.removeSpecies();
        
        assertEquals("Incorrect number of classes", 11,  graph.getAllOWLClasses().size());
        assertNull(graph.getOWLClassByIdentifier("U:22"));
        assertNull(graph.getOWLClassByIdentifier("U:23"));
        assertNull(graph.getOWLClassByIdentifier("U:24"));
        
        owlOntology = new ParserWrapper().parse(getResourceIRIString("speciesRemoveTest.obo"));
        graph = new OWLGraphWrapper(owlOntology);
        rf = new ElkReasonerFactory();
        reasoner = rf.createReasoner(graph.getSourceOntology());
        smu = new SpeciesSubsetterUtil(graph);
        smu.taxClass = graph.getOWLClassByIdentifier("T:1");
        smu.reasoner = reasoner;
        smu.removeSpecies();
        
        assertEquals("Incorrect number of classes", 14,  graph.getAllOWLClasses().size());
    }

	/**
	 * Test {@link SpeciesSubsetterUtil#explainTaxonConstraint(Set, Set)}.
	 */
    @SuppressWarnings("unchecked")
    @Test
    public void testExplainTaxonConstraint() throws Exception {
        ParserWrapper p = new ParserWrapper();
        OWLOntology owlOntology = p.parse(getResourceIRIString("graph/explainConstraints.owl"));
        OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
        OWLDataFactory factory = owlOntology.getOWLOntologyManager().getOWLDataFactory();
        
        String clsId1 = "UBERON:0000001";
        String clsId2 = "UBERON:0000002";
        String clsId3 = "UBERON:0000003";
        String clsId4 = "UBERON:0000004";
        String clsId5 = "UBERON:0000005";
        String clsId6 = "UBERON:0000006";
        String clsId7 = "UBERON:0000007";
        OWLClass cls1 = graph.getOWLClassByIdentifier(clsId1);
        OWLClass cls2 = graph.getOWLClassByIdentifier(clsId2);
        OWLClass cls3 = graph.getOWLClassByIdentifier(clsId3);
        OWLClass cls4 = graph.getOWLClassByIdentifier(clsId4);
        OWLClass cls5 = graph.getOWLClassByIdentifier(clsId5);
        OWLClass cls6 = graph.getOWLClassByIdentifier(clsId6);
        OWLClass cls7 = graph.getOWLClassByIdentifier(clsId7);
        String taxonId1 = "NCBITaxon:1";
        String taxonId2 = "NCBITaxon:2";
        String taxonId3 = "NCBITaxon:3";
        OWLClass taxon1 = graph.getOWLClassByIdentifier(taxonId1);
        OWLClass taxon2 = graph.getOWLClassByIdentifier(taxonId2);
        OWLClass taxon3 = graph.getOWLClassByIdentifier(taxonId3);
        OWLObjectProperty partOf = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
        OWLObjectProperty onlyInTaxon = graph.getOWLObjectPropertyByIdentifier("RO:0002160");
        OWLAnnotationProperty neverInTaxon = factory.getOWLAnnotationProperty(
                graph.getIRIByIdentifier("RO:0002161"));
        
        SpeciesSubsetterUtil smu = new SpeciesSubsetterUtil(graph);
        
        List<OWLObject> expectedExplain1 = Arrays.asList((OWLObject) cls1, 
                factory.getOWLObjectSomeValuesFrom(onlyInTaxon, taxon1));
        //explain why cls1 exists in taxon1
        assertEquals(expectedExplain1, 
            smu.explainTaxonConstraint(Arrays.asList(clsId1), Arrays.asList(taxonId1)).
                iterator().next());
        //explain why cls1 exists in taxon2 (same explanation, taxon2 is_a taxon1)
        assertEquals(expectedExplain1, 
                smu.explainTaxonConstraint(Arrays.asList(clsId1), Arrays.asList(taxonId2)).
                    iterator().next());
        //explain why cls1 exist in taxon3 (same explanation taxon3 is_a taxon1)
        assertEquals(expectedExplain1, 
                smu.explainTaxonConstraint(Arrays.asList(clsId1), Arrays.asList(taxonId3)).
                    iterator().next());
        
        List<OWLObject> expectedExplain2 = Arrays.asList((OWLObject) cls2, 
                factory.getOWLObjectSomeValuesFrom(onlyInTaxon, taxon2));
        //explain why cls2 exists in taxon2
        assertEquals(expectedExplain2, 
            smu.explainTaxonConstraint(Arrays.asList(clsId2), Arrays.asList(taxonId2)).
                iterator().next());
        //explain why cls2 does not exist in taxon3 (taxon3 disjoint from taxon2)
        assertEquals(expectedExplain2, 
            smu.explainTaxonConstraint(Arrays.asList(clsId2), Arrays.asList(taxonId3)).
                iterator().next());
        //explain why cls2 exists in taxon1 (cls2 is_a cls1)
        List<OWLObject> expectedExplain3 = Arrays.asList((OWLObject) cls2, cls1, 
                factory.getOWLObjectSomeValuesFrom(onlyInTaxon, taxon1));
        assertEquals(expectedExplain3, 
            smu.explainTaxonConstraint(Arrays.asList(clsId2), Arrays.asList(taxonId1)).
                iterator().next());
        
        
        List<OWLObject> expectedExplain4 = Arrays.asList(cls3, 
                factory.getOWLAnnotation(neverInTaxon, taxon3.getIRI()));
        //explain why cls3 does not exist in taxon3
        assertEquals(expectedExplain4, 
            smu.explainTaxonConstraint(Arrays.asList(clsId3), Arrays.asList(taxonId3)).
                iterator().next());
        //explain why cls3 exists in taxon1 (no specific explanations)
        assertTrue(smu.explainTaxonConstraint(Arrays.asList(clsId3), Arrays.asList(taxonId1)).
                isEmpty());
        //explain why cls3 exists in taxon2 (no specific explanations)
        assertTrue(smu.explainTaxonConstraint(Arrays.asList(clsId3), Arrays.asList(taxonId2)).
                isEmpty());
        
        
        List<OWLObject> expectedExplain5 = Arrays.asList(cls4, 
                factory.getOWLAnnotation(neverInTaxon, taxon2.getIRI()));
        //explain why cls4 does not exist in taxon2
        assertEquals(expectedExplain5, 
            smu.explainTaxonConstraint(Arrays.asList(clsId4), Arrays.asList(taxonId2)).
                iterator().next());

        List<OWLObject> expectedExplain6 = Arrays.asList(cls4, 
                factory.getOWLObjectSomeValuesFrom(partOf, cls3), 
                factory.getOWLAnnotation(neverInTaxon, taxon3.getIRI()));
        //explain why cls4 does not exist in taxon3
        assertEquals(expectedExplain6, 
            smu.explainTaxonConstraint(Arrays.asList(clsId4), Arrays.asList(taxonId3)).
                iterator().next());
        //explain why cls4 exists in taxon1 (no specific explanations)
        assertTrue(smu.explainTaxonConstraint(Arrays.asList(clsId4), Arrays.asList(taxonId1)).
                isEmpty());
        
        
        List<OWLObject> expectedExplain7 = Arrays.asList(cls7, 
                factory.getOWLObjectSomeValuesFrom(partOf, cls6), 
                factory.getOWLObjectSomeValuesFrom(partOf, cls5), 
                factory.getOWLAnnotation(neverInTaxon, taxon1.getIRI()));
        //explain why cls7 does not exist in taxon2
        assertEquals(expectedExplain7, 
            smu.explainTaxonConstraint(Arrays.asList(clsId7), Arrays.asList(taxonId2)).
                iterator().next());
        //explain why cls7 does not exist in taxon3
        assertEquals(expectedExplain7, 
            smu.explainTaxonConstraint(Arrays.asList(clsId7), Arrays.asList(taxonId3)).
                iterator().next());
        //explain why cls7 does not exist in taxon1
        assertEquals(expectedExplain7, 
            smu.explainTaxonConstraint(Arrays.asList(clsId7), Arrays.asList(taxonId1)).
                iterator().next());
        
        
        Collection<List<OWLObject>> allExplanations = smu.explainTaxonConstraint(
                Arrays.asList(clsId1, clsId2, clsId3, clsId4, clsId7), 
                Arrays.asList(taxonId1, taxonId2, taxonId3));
        assertTrue("Incorrect explanations returned, expected " + 
                Arrays.asList(expectedExplain1, expectedExplain2, expectedExplain4, 
                        expectedExplain5, expectedExplain7) + 
                 ", but was: " + allExplanations, 
                 allExplanations.size() == 7 && 
                 allExplanations.contains(expectedExplain1) && 
                 allExplanations.contains(expectedExplain2) && 
                 allExplanations.contains(expectedExplain3) && 
                 allExplanations.contains(expectedExplain4) && 
                 allExplanations.contains(expectedExplain5) && 
                 allExplanations.contains(expectedExplain6) && 
                 allExplanations.contains(expectedExplain7));
    }

}
