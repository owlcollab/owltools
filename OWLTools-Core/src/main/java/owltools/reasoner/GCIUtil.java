package owltools.reasoner;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * Utility for rendering inferred axioms from GCIs
 * 
 * @see https://github.com/owlcollab/owltools/issues/146
 * @author cjm
 *
 */
public class GCIUtil {
	
	private static final Logger LOG = Logger.getLogger(GCIUtil.class);


	public static Set<OWLSubClassOfAxiom> getSubClassOfSomeValuesFromAxioms(OWLReasoner reasoner) {
		return getSubClassOfSomeValuesFromAxioms(reasoner.getRootOntology(), reasoner);
	}
	
	/**
	 * Generates trivial SVF axioms from existing GCIs
	 * 
	 * <pre>
	 * For each GCI of the form CX SubClassOf R some DX
	 *  for each C that is an inferred direct subclass of or equivalent to CX
	 *     for each D that is an inferred direct superclass of or equivalent to DX
	 *       add an axiom C SubClassOf R some D
	 * </pre>
	 * @param ontology
	 * @param reasoner
	 * @return axioms
	 */
	public static Set<OWLSubClassOfAxiom> getSubClassOfSomeValuesFromAxioms(OWLOntology ontology,
			OWLReasoner reasoner) {

		Set<OWLSubClassOfAxiom> axioms = new HashSet<OWLSubClassOfAxiom>();
		OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
		for (OWLSubClassOfAxiom ax : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
			OWLClassExpression c = ax.getSubClass();
			if (ax.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)ax.getSuperClass();
				OWLObjectPropertyExpression p = svf.getProperty();
				OWLClassExpression filler = svf.getFiller();
				if (filler.isAnonymous() || c.isAnonymous()) {
					if (c.isBottomEntity())
						continue;
					if (filler.isTopEntity())
						continue;
					
					Set<OWLClass> childSet = reasoner.getEquivalentClasses(c).getEntities();
					if (childSet.size() == 0) {
						childSet = reasoner.getSubClasses(c, true).getFlattened();
					}
					for (OWLClass childClass : childSet) {
						if (childClass.isBottomEntity())
							continue;
						Set<OWLClass> childClassSuperClasses = 
								reasoner.getSuperClasses(childClass, false).getFlattened();
						childClassSuperClasses.addAll(reasoner.getEquivalentClasses(childClass).getEntities());
						Set<OWLClass> parentSet = reasoner.getEquivalentClasses(filler).getEntities();
						if (parentSet.size() == 0) {
							parentSet = reasoner.getSuperClasses(filler, true).getFlattened();
						}
						// TODO: remove additional redundancy (R some X) SubClassOf (R some Y)
						// Elk cannot test arbitrary entailments
 						for (OWLClass parentClass : parentSet) {
							if (parentClass.isTopEntity())
								continue;
							
							// do not assert C SubClassOf part-of some D, if C SubClassOf D is entailed
							if (childClassSuperClasses.contains(parentClass))
								continue;
							axioms.add(df.getOWLSubClassOfAxiom(childClass, 
									df.getOWLObjectSomeValuesFrom(p, parentClass)));
						}
					}
				}

			}
		}
		LOG.info("Inferred SVFs: "+axioms.size());
		return axioms;
	}

}
