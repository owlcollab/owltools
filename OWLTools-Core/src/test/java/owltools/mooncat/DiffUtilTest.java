package owltools.mooncat;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

public class DiffUtilTest extends OWLToolsTestBasics {

    private static Logger LOG = Logger.getLogger(DiffUtilTest.class);

    @Test
    public void testIdentical() throws OWLOntologyCreationException {
        ParserWrapper pw = new ParserWrapper();
        OWLOntology ont = pw.parseOWL(getResourceIRIString("caro.obo"));
        Diff diff = DiffUtil.getDiff(ont, ont);
        // don't expect identity, as declarations are filtered
        //assertEquals(ont.getAxiomCount(), diff.intersectionOntology.getAxiomCount());
        assertEquals(0, diff.ontology1remaining.getAxiomCount());
        assertEquals(0, diff.ontology2remaining.getAxiomCount());
    }

    @Test
    public void testDiffCaro() throws OWLOntologyCreationException {
        ParserWrapper pw = new ParserWrapper();
        OWLOntology ont1 = pw.parseOWL(getResourceIRIString("caro.obo"));
        OWLOntology ont2 = pw.parseOWL(getResourceIRIString("caro_local.owl"));
        Diff diff = DiffUtil.getDiff(ont1, ont2);
        assertEquals(95, diff.intersectionOntology.getAxiomCount());
        assertEquals(169, diff.ontology1remaining.getAxiomCount());
        assertEquals(111, diff.ontology2remaining.getAxiomCount());
    }

    @Test
    public void testDiffInCommon() throws OWLOntologyCreationException {
        ParserWrapper pw = new ParserWrapper();
        OWLOntology ont1 = pw.parseOWL(getResourceIRIString("difftest1.obo"));
        OWLOntology ont2 = pw.parseOWL(getResourceIRIString("difftest2.obo"));
        Diff diff = new Diff();
        diff.ontology1 = ont1;
        diff.ontology2 = ont2;
        diff.isCompareClassesInCommon = true;
        DiffUtil.getDiff(diff);
        LOG.debug(diff.ontology1remaining.getAxioms());
        LOG.debug(diff.ontology2remaining.getAxioms());
        LOG.debug(diff.intersectionOntology.getAxioms());
        assertEquals(4, diff.intersectionOntology.getAxiomCount());
        assertEquals(5, diff.ontology1remaining.getAxiomCount());
        assertEquals(6, diff.ontology2remaining.getAxiomCount());
    }

    @Test
    public void testDiffAll() throws OWLOntologyCreationException {
        ParserWrapper pw = new ParserWrapper();
        OWLOntology ont1 = pw.parseOWL(getResourceIRIString("difftest1.obo"));
        OWLOntology ont2 = pw.parseOWL(getResourceIRIString("difftest2.obo"));
        Diff diff = new Diff();
        diff.ontology1 = ont1;
        diff.ontology2 = ont2;
        diff.isCompareClassesInCommon = false;
        DiffUtil.getDiff(diff);
        LOG.debug(diff.ontology1remaining.getAxioms());
        LOG.debug(diff.ontology2remaining.getAxioms());
        LOG.debug(diff.intersectionOntology.getAxioms());
        assertEquals(6, diff.intersectionOntology.getAxiomCount());
        assertEquals(11, diff.ontology1remaining.getAxiomCount());
        assertEquals(7, diff.ontology2remaining.getAxiomCount());
    }

    @Test
    public void testDiffUnannotated() throws OWLOntologyCreationException {
        ParserWrapper pw = new ParserWrapper();
        OWLOntology ont1 = pw.parseOWL(getResourceIRIString("difftest1.obo"));
        OWLOntology ont2 = pw.parseOWL(getResourceIRIString("difftest2.obo"));
        Diff diff = new Diff();
        diff.ontology1 = ont1;
        diff.ontology2 = ont2;
        diff.isCompareClassesInCommon = true;
        diff.isCompareUnannotatedForm = true;
        DiffUtil.getDiff(diff);
        LOG.debug(diff.ontology1remaining.getAxioms());
        LOG.debug(diff.ontology2remaining.getAxioms());
        LOG.debug(diff.intersectionOntology.getAxioms());
        assertEquals(6, diff.intersectionOntology.getAxiomCount());
        assertEquals(4, diff.ontology1remaining.getAxiomCount());
        assertEquals(5, diff.ontology2remaining.getAxiomCount());
    }
    
    @Test
    public void testDiffUnannotatedAll() throws OWLOntologyCreationException {
        ParserWrapper pw = new ParserWrapper();
        OWLOntology ont1 = pw.parseOWL(getResourceIRIString("difftest1.obo"));
        OWLOntology ont2 = pw.parseOWL(getResourceIRIString("difftest2.obo"));
        Diff diff = new Diff();
        diff.ontology1 = ont1;
        diff.ontology2 = ont2;
        diff.isCompareClassesInCommon = false;
        diff.isCompareUnannotatedForm = true;
        DiffUtil.getDiff(diff);
        LOG.debug(diff.ontology1.getAxioms());
        LOG.debug(diff.ontology2.getAxioms());
        LOG.debug(diff.ontology1remaining.getAxioms());
        LOG.debug(diff.ontology2remaining.getAxioms());
        LOG.debug(diff.intersectionOntology.getAxioms());
        assertEquals(8, diff.intersectionOntology.getAxiomCount());
        assertEquals(10, diff.ontology1remaining.getAxiomCount());
        assertEquals(6, diff.ontology2remaining.getAxiomCount());
    }


}
