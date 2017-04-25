package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import static org.junit.Assert.*;

public class MooncatTest extends OWLToolsTestBasics {

    private static boolean RENDER_ONTOLOGY_FLAG = false;

    @Test
    public void testMireot() throws Exception {
        ParserWrapper pw = new ParserWrapper();

        // this test ontology has a class defined using a caro class, and imports caro_local
        OWLGraphWrapper g =
                pw.parseToOWLGraph(getResourceIRIString("mooncat_caro_test.obo"));
        Mooncat m = new Mooncat(g);
        m.addReferencedOntology(pw.parseOWL("http://purl.obolibrary.org/obo/caro.owl"));

        if (RENDER_ONTOLOGY_FLAG) {
            for (OWLEntity e : m.getExternalReferencedEntities()) {
                System.out.println("e="+e);
            }
            for (OWLObject e : m.getClosureOfExternalReferencedEntities()) {
                System.out.println("c="+e);
            }
            for (OWLAxiom ax : m.getClosureAxiomsOfExternalReferencedEntities()) {
                System.out.println("M_AX:"+ax);
            }
        }

        m.mergeOntologies();

        if (RENDER_ONTOLOGY_FLAG) {
            for (OWLAxiom ax : m.getOntology().getAxioms()) {
                System.out.println(ax);
            }
        }
    }

    @Test
    public void testRollUpPropsNoReasoner() throws Exception {
        rollUpProps(null);
    }
    
    @Test
    public void testRollUpPropsHermit() throws Exception {
        rollUpProps(new org.semanticweb.HermiT.ReasonerFactory());
    }

    public void rollUpProps(OWLReasonerFactory rf) throws Exception {
        ParserWrapper pw = new ParserWrapper();

        // this test ontology has a class defined using a caro class, and imports caro_local
        OWLGraphWrapper g =
                pw.parseToOWLGraph(getResourceIRIString("mooncat/retain-props-test.obo"));
        Mooncat m = new Mooncat(g);

        OWLReasoner reasoner = null;
        if (rf != null) {
            reasoner = rf.createReasoner(g.getSourceOntology());
        }
        Set<OWLObjectProperty> filterProps = new HashSet<>();
        OWLObjectProperty r1 = g.getOWLObjectPropertyByIdentifier("R:1");
        filterProps.add(r1);
        m.retainAxiomsInPropertySubset(g.getSourceOntology(), filterProps, reasoner);
        pw.saveOWL(g.getSourceOntology(), "target/retain.owl");
        
        OWLClass x1 = g.getOWLClassByIdentifier("X:1");
        OWLClass x2 = g.getOWLClassByIdentifier("X:2");
        OWLClass x3 = g.getOWLClassByIdentifier("X:3");
               
        int n = 0;
        for ( OWLGraphEdge e : g.getOutgoingEdges(x1)) {
            System.out.println(e);
            if (e.getSingleQuantifiedProperty().getProperty().equals(r1) && e.getTarget().equals(x2)) {
                n++;
            }
            else {
                n = -99;
            }
            
        }
        assertEquals(1, n);
        
        n = 0;
        for ( OWLGraphEdge e : g.getOutgoingEdges(x2)) {
            System.out.println(e);
            if (e.getSingleQuantifiedProperty().getProperty().equals(r1) && e.getTarget().equals(x3)) {
                n++;
            }
            else {
                n = -99;
            }
            
        }
        assertEquals(1, n);
    }

}
