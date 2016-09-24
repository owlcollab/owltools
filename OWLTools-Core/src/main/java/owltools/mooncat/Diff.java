package owltools.mooncat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.common.collect.Sets.SetView;

public class Diff {
    public OWLOntology ontology1;
    public OWLOntology ontology2;
    public OWLOntology intersectionOntology;
    public OWLOntology unionOntology;

    public OWLOntology ontology1remaining;
    public OWLOntology ontology2remaining;
    public OWLOntology ontologyDiff;
    
    public boolean isCompareClassesInCommon = false;
    public boolean isCompareUnannotatedForm = false;
    public boolean isFilterDeclarations = true;
    public boolean isAddSharedDeclarations = true;
       
    // map between compared axiom (may be unannotated form) and complete axioms
    public Map<OWLAxiom, Set<OWLAxiom>> umap1 = new HashMap<>();
    public Map<OWLAxiom, Set<OWLAxiom>> umap2 = new HashMap<>();

    public OWLOntology ontology(Set<OWLAxiom> axioms) throws OWLOntologyCreationException {
        return ontology1.getOWLOntologyManager().createOntology(axioms);
    }



    public String toString() {

        return "|Ont1| = " + ontology1.getAxiomCount() 
                + " |Ont2| = " + ontology2.getAxiomCount()
                + " |^| = " + intersectionOntology.getAxiomCount()
                + " |Ont1r| = " + ontology2remaining.getAxiomCount()
                + " |Ont2r| = " + ontology2remaining.getAxiomCount();
    }
    
    public Set<OWLAxiom> mapAxiom(OWLAxiom a, Map<OWLAxiom, Set<OWLAxiom>> umap) {
        if (umap.containsKey(a))
            return umap.get(a);
        else
            return new HashSet<>();
    }
    
    public Set<OWLAxiom> mapAxioms(Set<OWLAxiom> axioms, Map<OWLAxiom, Set<OWLAxiom>> umap) {
        Set<OWLAxiom> mapped = new HashSet<>();
        for (OWLAxiom a : axioms) {
            mapped.addAll( mapAxiom(a, umap));
        }
        return mapped;
    }
    
    public Set<OWLAxiom> mapAxioms1(Set<OWLAxiom> axioms) {
        return mapAxioms(axioms, umap1);
    }
    public Set<OWLAxiom> mapAxioms2(Set<OWLAxiom> axioms) {
        return mapAxioms(axioms, umap2);
    }
}