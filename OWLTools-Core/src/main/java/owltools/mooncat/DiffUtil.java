package owltools.mooncat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.AxiomAnnotationTools;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class DiffUtil {


    public static Diff getDiff(OWLOntology ontology1, OWLOntology ontology2) throws OWLOntologyCreationException {
        Diff diff = new Diff();
        diff.ontology1 = ontology1;
        diff.ontology2 = ontology2;
        return getDiff(diff);
    }


    public static Diff getDiff(Diff diff) throws OWLOntologyCreationException {
        OWLOntology ontology1 = diff.ontology1;
        OWLOntology ontology2 = diff.ontology2;
        Set<OWLAxiom> axioms1 = ontology1.getAxioms();
        Set<OWLAxiom> axioms2 = ontology2.getAxioms();

        Set<OWLAxiom> sharedDeclarations = new HashSet<>();
        if (diff.isCompareClassesInCommon) {
            Set<OWLClass> cs1 = ontology1.getClassesInSignature();
            Set<OWLClass> cs2 = ontology2.getClassesInSignature();
            Set<OWLClass> cs = Sets.intersection(cs1, cs2);
            axioms1 = new HashSet<>();
            axioms2 = new HashSet<>();
            for (OWLClass c : cs) {
                sharedDeclarations.add(ontology1.
                        getOWLOntologyManager().
                        getOWLDataFactory().
                        getOWLDeclarationAxiom(c));
                axioms1.addAll(ontology1.getAxioms(c));
                axioms1.addAll(ontology1.getAnnotationAssertionAxioms(c.getIRI()));
                axioms2.addAll(ontology2.getAxioms(c));
                axioms2.addAll(ontology2.getAnnotationAssertionAxioms(c.getIRI()));
            }
        }
        
        axioms1 = filter(axioms1, diff);
        axioms2 = filter(axioms2, diff);

        // map from compared axiom to full axiom
        Map<OWLAxiom, Set<OWLAxiom>> umap1 = diff.umap1;
        Map<OWLAxiom, Set<OWLAxiom>> umap2 = diff.umap2;

        axioms1 = unannotateAxioms(axioms1, umap1, 
                ontology1.getOWLOntologyManager().getOWLDataFactory(),
                diff.isCompareUnannotatedForm);
        axioms2 = unannotateAxioms(axioms2, umap2, 
                ontology2.getOWLOntologyManager().getOWLDataFactory(),
                diff.isCompareUnannotatedForm);

        Set<OWLAxiom> intersectionAxioms = Sets.intersection(axioms1, axioms2);
        Set<OWLAxiom> unionAxioms = Sets.union(axioms1, axioms2);
        Set<OWLAxiom> axioms1remaining = Sets.difference(axioms1, axioms2);
        Set<OWLAxiom> axioms2remaining = Sets.difference(axioms2, axioms1);
        //System.out.println("A2R="+axioms2remaining);
        
        
        if (diff.isCompareUnannotatedForm) {
            intersectionAxioms = 
                    Sets.union(
                            diff.mapAxioms1(intersectionAxioms),
                            diff.mapAxioms2(intersectionAxioms));
            axioms1remaining = diff.mapAxioms1(axioms1remaining);
            axioms2remaining = diff.mapAxioms2(axioms2remaining);
        }
        
        if (diff.isAddSharedDeclarations) {
            axioms1remaining = Sets.union(axioms1remaining, sharedDeclarations);
            axioms2remaining = Sets.union(axioms2remaining, sharedDeclarations);
        }
 
        diff.intersectionOntology = diff.ontology(intersectionAxioms);
        diff.ontology1remaining = diff.ontology(axioms1remaining);
        diff.ontology2remaining = diff.ontology(axioms2remaining);
        diff.ontologyDiff = diff.ontology(Sets.union(axioms1remaining , axioms2remaining ));

        return diff;
    }


    // remove declarations
    private static Set<OWLAxiom> filter(Set<OWLAxiom> axioms, Diff diff) {
        if (diff.isFilterDeclarations) {
            return axioms.stream().filter(a -> !(a instanceof OWLDeclarationAxiom)).
                    collect(Collectors.toSet());
        }
        else {
            return axioms;
        }
    }


    private static Set<OWLAxiom> unannotateAxioms(Set<OWLAxiom> axioms,
            Map<OWLAxiom, Set<OWLAxiom>> umap, OWLDataFactory owlDataFactory, boolean isCompareUnannotatedForm) {
        Set<OWLAxiom> axiomsOut = new HashSet<>();
        for (OWLAxiom axiom : axioms) {
            OWLAxiom axiomMapped = null;
            if (isCompareUnannotatedForm) {
                axiomMapped = removeAnotations(axiom, owlDataFactory);
            }
            else {
                axiomMapped = axiom;
            }
            if (!umap.containsKey(axiomMapped))
                umap.put(axiomMapped, new HashSet<>());
            umap.get(axiomMapped).add(axiom);
            axiomsOut.add(axiomMapped);
        }
        return axiomsOut;
    }


    public static OWLAxiom removeAnotations(OWLAxiom a, OWLDataFactory factory) {
        return AxiomAnnotationTools.changeAxiomAnnotations(a,
                new HashSet<OWLAnnotation>(), factory);
    }

    private Map<OWLAxiom, OWLAxiom> reverseMap(Map<OWLAxiom, Set<OWLAxiom>> umap) {
        Map<OWLAxiom, OWLAxiom> rmap = new HashMap<>();
        for (OWLAxiom k : umap.keySet()) {
            for (OWLAxiom v : umap.get(k)) {
                rmap.put(v, k);
            }
        }
        return rmap;
    }




}
