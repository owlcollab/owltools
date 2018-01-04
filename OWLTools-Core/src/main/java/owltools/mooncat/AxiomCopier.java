package owltools.mooncat;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * 
 * Copies axioms form a source ontology to a target ontology using equivalence axioms from
 * a mapping ontology
 * 
 * 
 * @author cjm
 *
 */
public class AxiomCopier {
    
    public boolean isCopyDuplicates = false;
    
    
    
    /**
     * Simple IRI-value pairs representing an Annotation minus property
     * 
     * @author cjm
     *
     */
    class AnnTuple {
        IRI iri;
        String val;
        
        
        public AnnTuple(IRI iri, String val) {
            super();
            this.iri = iri;
            this.val = val;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((iri == null) ? 0 : iri.hashCode());
            result = prime * result + ((val == null) ? 0 : val.hashCode());
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AnnTuple other = (AnnTuple) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (iri == null) {
                if (other.iri != null)
                    return false;
            } else if (!iri.equals(other.iri))
                return false;
            if (val == null) {
                if (other.val != null)
                    return false;
            } else if (!val.equals(other.val))
                return false;
            return true;
        }
        private AxiomCopier getOuterType() {
            return AxiomCopier.this;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "AnnTuple [iri=" + iri + ", val=" + val + "]";
        }
        
        
        
    }
    
    /**
     * Makes a copy of axioms in source ontology, ready to be placed in targetOnt.
     * 
     * Note: does not actually place the axioms in targetOnt, but the target ontology
     * is used to avoid creating duplicates. Also used to build an exist of classes
     * to be used in mapping
     * 
     * 
     * 
     * @param sourceOnt - ontology to copy axioms from
     * @param targetOnt - ontology to copy to (used to check for dupes and build index of classes)
     * @param mapOnt - ontology with equivalence axioms
     * @return copied and rewritten axioms
     */
    public Set<OWLAxiom> copyAxioms(OWLOntology sourceOnt, OWLOntology targetOnt, OWLOntology mapOnt) {
        
        // axioms in targetOnt (minus annotations)
        Set<OWLAxiom> axiomsExisting  = new HashSet<>();
        
        // all simple annotations in T
        Set<AnnTuple> annTuples = new HashSet();
        
        // all classes in T
        Set<OWLClass> classesInTarget = new HashSet<>();
        
        // reflexive mappings of classes S->T
        Map<OWLClass, OWLClass> eqMap = new HashMap<>();
        
        OWLDataFactory df = targetOnt.getOWLOntologyManager().getOWLDataFactory();
        
        // build index of what we have already, to avoid dupes
        for (OWLAxiom ax : targetOnt.getAxioms(Imports.EXCLUDED)) {
            
            // add non-annotation part
            axiomsExisting.add(ax.getAxiomWithoutAnnotations());
            
            if (ax.isLogicalAxiom()) {
                
                // add subclasses to index of classes to be mapped to
                if (ax instanceof OWLSubClassOfAxiom &&
                       !((OWLSubClassOfAxiom)ax).getSubClass().isAnonymous()) {
                    classesInTarget.add( ((OWLSubClassOfAxiom)ax).getSubClass().asOWLClass() );
                }
            }
            else if (ax instanceof OWLAnnotationAssertionAxiom) {
                
                // keep simple IRI->value index.
                // motivation: avoid copying for example X relatedSyn Y if we have X exactSyn Y
                OWLAnnotationAssertionAxiom aax = (OWLAnnotationAssertionAxiom)ax;
                AnnTuple tup = getAnnTuple(aax);
                annTuples.add(tup);
            }
        }
        
        // build equivalence map S->T, pointing source classes to target classes
        // this map is reflexive
        for (OWLAxiom ax : mapOnt.getAxioms(Imports.INCLUDED)) {
            if (ax instanceof OWLEquivalentClassesAxiom) {
                OWLEquivalentClassesAxiom eca = (OWLEquivalentClassesAxiom)ax;
                for (OWLClass c : eca.getNamedClasses()) {
                    if (classesInTarget.contains(c)) {
                        for (OWLClass xc : eca.getNamedClasses()) {
                            eqMap.put(xc, c);
                        }
                        continue;
                    }
                }
            }
        }        
        
        Set<OWLAxiom> axiomsToAdd  = new HashSet<>();
        
        // get copied axioms
        for (OWLAxiom ax : sourceOnt.getAxioms(Imports.EXCLUDED)) {
            
            OWLAxiom coreAxiom = ax.getAxiomWithoutAnnotations();
            OWLClass srcClass = null;
            if (ax.isLogicalAxiom()) {
                // LOGICAL
                
                Set<OWLClass> sig = ax.getClassesInSignature();
                
                if (sig.size() < 2) {
                    continue;
                }
                
                // of logical axioms, currently only subClassOf axioms (NC->NC) are copied
               if (ax instanceof OWLSubClassOfAxiom) {
                    OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom)ax;
                    if (!sca.getSubClass().isAnonymous() && !sca.getSuperClass().isAnonymous()) {
                        srcClass = sca.getSubClass().asOWLClass();
                        OWLClass supc = sca.getSuperClass().asOWLClass();
                        if (eqMap.containsKey(supc) && eqMap.containsKey(srcClass)) {
                            
                            // make a new axiom, including annotation
                            OWLSubClassOfAxiom newAxiom = df.getOWLSubClassOfAxiom(eqMap.get(srcClass), 
                                    eqMap.get(supc), 
                                    anns(df, srcClass, false));
                            
                            // add copy to list
                            if (isCopyDuplicates ||
                                    !axiomsExisting.contains(newAxiom.getAxiomWithoutAnnotations())) {
                                axiomsToAdd.add(newAxiom);
                            }
                        }
                    }
                    else if (!sca.getSubClass().isAnonymous() && sca.getSuperClass().isAnonymous()) {
                        // e.g. A SubClassOf R some B
                        srcClass = sca.getSubClass().asOWLClass();
                        OWLClassExpression supx = sca.getSuperClass();
                        if (supx instanceof OWLObjectSomeValuesFrom) {
                            OWLObjectSomeValuesFrom x = (OWLObjectSomeValuesFrom)supx;
                            OWLClass supc = x.getFiller().asOWLClass();
                            if (eqMap.containsKey(supc) && eqMap.containsKey(srcClass)) {

                                // make a new axiom, including annotation
                                OWLSubClassOfAxiom newAxiom = df.getOWLSubClassOfAxiom(
                                        eqMap.get(srcClass), 
                                        df.getOWLObjectSomeValuesFrom(x.getProperty(), eqMap.get(supc)),
                                        anns(df, srcClass, false));

                                // add copy to list
                                if (isCopyDuplicates ||
                                        !axiomsExisting.contains(newAxiom.getAxiomWithoutAnnotations())) {
                                    axiomsToAdd.add(newAxiom);
                                }
                            }
                        }
                    }
                }
            }
            else if (ax instanceof OWLAnnotationAssertionAxiom){
                // NON-LOGICAL/ANNOTATION

                OWLAnnotationAssertionAxiom aax = (OWLAnnotationAssertionAxiom)ax;
                OWLAnnotationSubject subj = aax.getSubject();
                
                // do not copy annotations on blank nodes
                if (!(subj instanceof IRI)) {
                    continue;
                }
 
                srcClass = df.getOWLClass((IRI)subj);
                
                // only copy if (reflexive) equiv mapping has entry
                if (eqMap.containsKey(srcClass)) {
                    
                    // OBO convention: xref annotations are treated differently for
                    // axiom annotations (as are defs)
                    boolean isXrefAnn = aax.getProperty().getIRI().toString().contains("Synonym");
                    
                    OWLAnnotationAssertionAxiom newAxiom = df.getOWLAnnotationAssertionAxiom(aax.getProperty(), 
                            eqMap.get(srcClass).getIRI(),
                            aax.getValue(),
                            anns(df, srcClass, isXrefAnn));
                    AnnTuple tup = getAnnTuple(newAxiom);

                    boolean isDupe = annTuples.contains(tup) || 
                            axiomsExisting.contains(newAxiom.getAxiomWithoutAnnotations());
                    if (isCopyDuplicates || !isDupe) {
                            axiomsToAdd.add(newAxiom);
                    }
                }
            }
            else {
                //System.out.println("SKIP:"+ax);

            }
        }
        return axiomsToAdd;
    }

    /**
     * make annotations to be placed on axiom
     * 
     * @param df
     * @param srcClass
     * @param isXrefAnn if true use hasDbXref not source
     * @return annotations
     */
    private Set<? extends OWLAnnotation> anns(OWLDataFactory df, OWLClass srcClass, boolean isXrefAnn) {
        String iri = srcClass.getIRI().toString();
        iri = iri.replaceAll(".*/", "");
        iri = iri.replaceAll("_", ":");
        String pn = "source";
        if (isXrefAnn) {
            pn = "hasDbXref";
        }
        OWLAnnotationProperty p = df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#"+pn));
        OWLAnnotation ann = df.getOWLAnnotation(p, df.getOWLLiteral(iri));
        return Collections.singleton(ann);
    }

    private AnnTuple getAnnTuple(OWLAnnotationAssertionAxiom aax) {
        String v;
        OWLAnnotationValue av = aax.getValue();
        if (av instanceof OWLLiteral) {
            v = ((OWLLiteral)av).getLiteral();
        }
        else {
            v = av.toString();
        }
        v = v.toLowerCase();
        return new AnnTuple((IRI)aax.getSubject(), v);
    }

   
 

}
