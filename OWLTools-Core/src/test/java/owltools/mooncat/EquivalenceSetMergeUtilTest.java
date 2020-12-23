package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class EquivalenceSetMergeUtilTest extends OWLToolsTestBasics {

    @Test
    public void testMerge() throws OWLOntologyCreationException, IOException, IncoherentOntologyException, OWLOntologyStorageException {
        ParserWrapper pw = new ParserWrapper();
        OWLGraphWrapper g =
                pw.parseToOWLGraph(getResourceIRIString("equivalence-set-merge-util-test.obo"));
        OWLOntology ont1 = g.getSourceOntology();
        OWLReasonerFactory rf = new ReasonerFactory();
        OWLReasoner reasoner = rf.createReasoner(ont1);
        EquivalenceSetMergeUtil esmu = new EquivalenceSetMergeUtil(g, reasoner);
        esmu.setPrefixScore("A", 8.0);
        esmu.setPrefixScore("B", 6.0);
        esmu.setPrefixScore("C", 4.0);
        OWLAnnotationProperty lp = g.getDataFactory().getOWLAnnotationProperty( OWLRDFVocabulary.RDFS_LABEL.getIRI() );
        esmu.setPropertyPrefixScore( lp, "C", 5.0);
        esmu.setPropertyPrefixScore( lp, "B", 4.0);
        esmu.setPropertyPrefixScore( lp, "A", 3.0);
        
        OWLAnnotationProperty dp = g.getDataFactory().getOWLAnnotationProperty( Obo2OWLVocabulary.IRI_IAO_0000115.getIRI() );
        esmu.setPropertyPrefixScore( dp, "B", 5.0);
        esmu.setPropertyPrefixScore( dp, "A", 4.0);
        esmu.setPropertyPrefixScore( dp, "C", 3.0);
        
        esmu.setRemoveAxiomatizedXRefs(true);

        esmu.merge();
        OWLDocumentFormat fmt = new OBODocumentFormat();
        pw.saveOWL(g.getSourceOntology(), "target/esmu.owl");
        //pw.setCheckOboDoc(false);
        pw.saveOWL(g.getSourceOntology(), fmt, "target/esmu.obo");
        
        OWLOntology ont2 = pw.parseOWL(getResourceIRIString("equivalence-set-merge-util-expected.obo"));
        assertEquals(0, compare(ont1, ont2));
    }

}
