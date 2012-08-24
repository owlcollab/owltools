package owltools.sim.preprocessor;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class PhenoSimHQEPreProcessor extends AbstractOBOSimPreProcessor {

	public String QUALITY = "PATO_0000001";
	public boolean isMultiSpecies = true;

	public void preprocess() {

		// classes and properties
		OWLClass owlThing = getOWLDataFactory().getOWLThing();
		OWLObjectProperty df = getOWLObjectPropertyViaOBOSuffix(DEVELOPS_FROM);
		OWLObjectProperty cpo = getOWLObjectPropertyViaOBOSuffix(COMPOSED_PRIMARILY_OF);
		OWLObjectProperty inheresIn = getOWLObjectPropertyViaOBOSuffix(INHERES_IN);
		OWLObjectProperty hasPart = getOWLObjectPropertyViaOBOSuffix(HAS_PART);
		
		removeDisjointClassesAxioms();

		// E.g. hand part_of some hand
		makeReflexive(PART_OF);

		// E.g. limb bud develops_from some limb bud
		makeReflexive(DEVELOPS_FROM);

		// Everything is made from itself
		makeReflexive(COMPOSED_PRIMARILY_OF);

		// Get all "E" classes - i.e. everything except qualities
		// this is filtered to include only classes that are useful for grouping classes
		Set<OWLClass> entityClasses = getPhenotypeEntityClasses();
		LOG.info("Num entity classes:"+entityClasses.size());
		
		// VIEW: composed_primarily_of
		// E.g. "composed primarily of some hair cell".
		// only build this view for parents of a cpo relationship
		createPropertyView(cpo, filterByDirectProperty(entityClasses, cpo), "%s-based entity");

		// VIEW: develops_from
		// E.g. "develops from some limb bud".
		// only build this view for parents of a df relationship
		createPropertyView(df, filterByDirectProperty(entityClasses, df), "%s or derivative");
		//createPropertyView(df, entityClasses, "%s or derivative");
		//filterUnused(dfAxioms);

		// VIEW: part_of
		// E.g. "part of some limb"
		createPropertyView(getOWLObjectPropertyViaOBOSuffix(PART_OF), entityClasses, "%s structure");

		// Maintain a list of new E classes
		// E.g. E = {limb, limb structure, limb bud, limb bud derivative}
		entityClasses.addAll(newClasses);
		entityClasses.add(owlThing);

		// VIEW: inheres_in
		// the resulting classes are dependent continuants, on the same footing as qualities;
		// intersections can be made with these
		newClasses = new HashSet<OWLClass>();
		createPropertyView(inheresIn, entityClasses, "affected %s");

		// note: need to materialize QE class expressions - these are currently embedded in
		// "has_part some QE" expressions
		getReasoner().flush();
		Set<OWLClass> thingsWithParts = materializeClassExpressionsReferencedBy(hasPart);
		getReasoner().flush();
		generateLeastCommonSubsumers(thingsWithParts, thingsWithParts);
		getReasoner().flush();

		// note the ontology should have an axiom "Quality SubClassOf inheres_in some Thing"
		OWLClass inheresInSomeThing = this.viewMapByProp.get(owlThing).get(inheresIn);
		if (inheresInSomeThing == null) {
			LOG.warn("Cannot get view class for root of "+inheresIn);
		}
		getReasoner().flush();

		// E.g. has_phenotype some (Q and inheres_in some E)
		// Note that has_phenotype <- has_phenotype o has_part, so this should work for both:
		//  * i1 Type: has_phenotype some (Q and inheres_in some E)
		//  * i1 Type: has_phenotype some (has_part some (Q and inheres_in some E))
		// [auto-generated labels for the latter may look odd]
		createPropertyView(getOWLObjectPropertyViaOBOSuffix(HAS_PHENOTYPE), inheresInSomeThing, "%s phenotype");

		getReasoner().flush();
		
		// INTERSECTIONS - final
		// we have previously created QE intersections - this step ensures that all LCSs of
		// individuals are materialized
		generateLeastCommonSubsumersForAttributeClasses();

		//getReasoner().flush();
		//Set<OWLClassExpression> attExprs = this.getDirectAttributeClassExpressions();
		//Set<OWLClass> attClasses = materializeClassExpressions(attExprs);

		// INTERSECTIONS
		// E.g 'has phenotype some affected limb' and 'has phenotype hyperplastic'.
		// This is not ideal as each individual can have multiple of each;
		// better to do before but we need to materialize
		getReasoner().flush();
		//generateLeastCommonSubsumers(attClasses, attClasses);

		//this.getOWLOntologyManager().removeAxioms(outputOntology, tempAxioms);
		trim();
	}
	
	// --
	// UTIL
	// --
	
	/**
	 * In MP we have
	 *  abn. tooth. dev. SubClassOf abn. tooth. morphology
	 *  
	 *  Transform:
	 *  
	 *  'X devel' (P) = _ and R some U ==> add:
	 *    quality and inh some P SubClassOf morphology and inh some E
	 *  
	 */
	protected void makeProcessStructureLinks() {
		String rel = this.RESULTS_IN_MORPHOGENESIS_OF;
		String oppl =
			"SELECT ?P EquivalentTo "+rel+" SOME ?U "+
			"BEGIN ADD ('inheres in' some ?P) SubClassOf (morphology and 'inheres in' some ?U)";
	}

	/**
	 * @return 'E' classes
	 */
	protected Set<OWLClass> getPhenotypeEntityClasses() {
		Set<OWLClass> entityClasses = new HashSet<OWLClass>();
		
		for (OWLClass c : inputOntology.getClassesInSignature(true)) {
			if (!isVerbotenEntity(c)) {
				entityClasses.add(c);
				continue;
			}
			else {
				LOG.info("Filtering out:"+c);
			}
		}

		entityClasses.removeAll(getQualityClasses());
		
		

		return entityClasses;
	}
		
	public boolean isVerbotenEntity(OWLClass c) {
		String ont = getOntologyPrefix(c);
		if (isMultiSpecies) {
			if (ont.equals("FMA") || ont.equals("MA") || ont.equals("EHDAA2") ||
					ont.startsWith("EMAP") || ont.equals("ZFA") || ont.equals("ZFS") ||
					ont.equals("FBbt") || ont.equals("WBbt")) {
				return true;
			}
		}
		// in future: do this ontologically
		if (ont.equals("MP") || ont.equals("HP") || ont.equals("FYPO") || ont.equals("WormPhenotype"))
			return true;
		if (isUpperLevel(c))
			return true;

		return false;
	}

	private String getOntologyPrefix(OWLClass c) {
		String id = c.getIRI().toString().replace("http://purl.obolibrary.org/obo/", "");
		return id.replaceAll("_.*", "");
	}

	public Set<OWLClass> getQualityClasses() {
		OWLClass qualityCls = getOWLClassViaOBOSuffix(QUALITY);
		return getReflexiveSubClasses(qualityCls);
	}

	/**
	 * Note: if the ontology contains
	 * Z SubClassOf df some Z2, Z2 SubClassOf U
	 * then Z2 will be retained, but U will not. Consider including supeclasses.
	 * 
	 * @param inSet
	 * @param p
	 * @return
	 */
	public Set<OWLClass> filterByDirectProperty(Set<OWLClass> inSet, OWLObjectProperty p) {
		Set<OWLClass> outSet = new HashSet<OWLClass>();
		for (OWLAxiom ax : outputOntology.getReferencingAxioms(p, true)) {
			if (ax instanceof OWLSubClassOfAxiom) {
				OWLClassExpression sup = ((OWLSubClassOfAxiom)ax).getSuperClass();
				if (sup instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)sup;
					if (svf.getProperty().equals(p)) {
						if (svf.getFiller() instanceof OWLClass) {
							// ignore reflexivity cases
							if (!svf.getFiller().equals(((OWLSubClassOfAxiom)ax).getSubClass())) {
								outSet.add((OWLClass) svf.getFiller());
							}
						}
					}
				}
			}
			else if (ax instanceof OWLEquivalentClassesAxiom) {
				for (OWLClassExpression sup : ((OWLEquivalentClassesAxiom)ax).getClassExpressions()) {
					if (sup instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)sup;
						if (svf.getProperty().equals(p)) {
							if (svf.getFiller() instanceof OWLClass) {
								outSet.add((OWLClass) svf.getFiller());
							}
						}
					}

				}

			}
		}
		LOG.info("found "+outSet.size()+" candidates; intersection with "+inSet.size());
		outSet.retainAll(inSet);
		return outSet;
	}

}
