package owltools.mooncat;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.AxiomAnnotationTools.AxiomAnnotationsChanger;

public class RedundantAxiomTagger {
    private static Logger LOG = Logger.getLogger(RedundantAxiomTagger.class);

    public static void tagRedundantAxioms(OWLReasoner reasoner) {
        OWLOntology ont = reasoner.getRootOntology();
        OWLOntologyManager mgr = ont.getOWLOntologyManager();
        OWLDataFactory df = mgr.getOWLDataFactory();
        OWLAnnotationProperty anProp = df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#source"));
        for (OWLSubClassOfAxiom ax : ont.getAxioms(AxiomType.SUBCLASS_OF)) {
            if (!ax.getSuperClass().isAnonymous()) {
                OWLClass supc = (OWLClass) ax.getSuperClass();
                
                mgr.removeAxiom(ont, ax);
                reasoner.flush();
                NodeSet<OWLClass> ancs = reasoner.getSuperClasses(ax.getSubClass(), false);
                //LOG.info(ax + " ANCS="+ancs);
                if (ancs.containsEntity( supc)) {
                    String direct = "indirect";
                    if (reasoner.getSuperClasses(ax.getSubClass(), true).containsEntity( supc)) {
                        direct = "direct";
                    }
                    LOG.info("SCA = "+ax+" D="+direct);
                    OWLAnnotation ann = df.getOWLAnnotation(anProp, df.getOWLLiteral(direct));
                    OWLAxiom newAxiom = changeAxiomAnnotations(ax, Collections.singleton(ann), df);
                    mgr.addAxiom(ont, newAxiom);
                }
                else {
                    // put it back
                    mgr.addAxiom(ont, ax);
                }
            }
        }
       
    }
    
    /**
     * Update the given axiom to the new set of axiom annotation. Recreates the
     * axiom with the new annotations using the given factory.
     * 
     * @param axiom
     * @param annotations
     * @param factory
     * @return newAxiom
     */
    public static OWLAxiom changeAxiomAnnotations(OWLAxiom axiom, Set<OWLAnnotation> annotations, OWLDataFactory factory) {
        final AxiomAnnotationsChanger changer = new AxiomAnnotationsChanger(annotations, factory);
        final OWLAxiom newAxiom = axiom.accept(changer);
        return newAxiom;
    }


}
