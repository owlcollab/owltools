package owltools.reasoner;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public class AxiomJengaTest extends AbstractReasonerTest {

    @Test
    public void testNoReplace() throws OBOFormatParserException, OWLOntologyCreationException, IOException {
        OWLGraphWrapper g =  getOntologyWrapper("jenga.obo");
        OWLOntology ontology = g.getSourceOntology();
        ElkReasonerFactory rf = new ElkReasonerFactory();
        OWLReasoner reasoner = rf.createReasoner(ontology);
        JengaTower jt = AxiomJenga.makeJengaTower(ontology, reasoner, false);
        System.out.println(jt);
        assertTrue(jt.maxJenga == 8);
   }
    
    @Test
    public void testWithReplace() throws OBOFormatParserException, OWLOntologyCreationException, IOException {
        OWLGraphWrapper g =  getOntologyWrapper("jenga.obo");
        OWLOntology ontology = g.getSourceOntology();
        ElkReasonerFactory rf = new ElkReasonerFactory();
        OWLReasoner reasoner = rf.createReasoner(ontology);
        JengaTower jt = AxiomJenga.makeJengaTower(ontology, reasoner, true);
        System.out.println(jt);
        assertTrue(jt.maxJenga == 0);

    }
    
    @Test
    public void testNoReplaceLogDef() throws OBOFormatParserException, OWLOntologyCreationException, IOException {
        OWLGraphWrapper g =  getOntologyWrapper("jenga_ld.obo");
        OWLOntology ontology = g.getSourceOntology();
        ElkReasonerFactory rf = new ElkReasonerFactory();
        OWLReasoner reasoner = rf.createReasoner(ontology);
        JengaTower jt = AxiomJenga.makeJengaTower(ontology, reasoner, false);
        System.out.println(jt);
        //assertTrue(jt.maxJenga == 0);

    }

}
