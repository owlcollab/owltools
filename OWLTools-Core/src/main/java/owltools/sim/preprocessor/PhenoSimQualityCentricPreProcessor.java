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

public class PhenoSimQualityCentricPreProcessor extends AbstractOBOSimPreProcessor {

	public String QUALITY = "PATO_0000001";
	public boolean isMultiSpecies = true;

	public void preprocess() {

		OWLClass owlThing = getOWLDataFactory().getOWLThing();

		// E.g. hand part_of some hand
		makeReflexive(PART_OF);

		// E.g. limb bud develops_from some limb bud
		makeReflexive(DEVELOPS_FROM);

		// i.e. everything except qualities
		// TODO - filter
		Set<OWLClass> entityClasses = getPhenotypeEntityClasses();
		LOG.info("Num entity classes:"+entityClasses.size());

		// E.g. "develops from some limb bud"
		OWLObjectProperty df = getOWLObjectPropertyViaOBOSuffix(DEVELOPS_FROM);
		createPropertyView(df, filterByDirectProperty(entityClasses, df), "%s or derivative");
		//createPropertyView(df, entityClasses, "%s or derivative");
		//filterUnused(dfAxioms);

		// E.g. "part of some limb"
		createPropertyView(getOWLObjectPropertyViaOBOSuffix(PART_OF), entityClasses, "%s structure");

		// E.g. E = {limb, limb structure, limb bud, limb bud derivative}
		entityClasses.addAll(newClasses);

		// reset
		//newClasses = new HashSet<OWLClass>();
		entityClasses.add(owlThing);
		OWLObjectProperty inheresIn = getOWLObjectPropertyViaOBOSuffix(INHERES_IN);
		createPropertyView(inheresIn, entityClasses, "%s phenotype");

		//getReasoner().flush();
		//generateLeastCommonSubsumers(getQualityClasses(), newClasses);


		OWLClass inheresInSomeThing = this.viewMapByProp.get(owlThing).get(inheresIn);
		if (inheresInSomeThing == null) {
			LOG.warn("Cannot get view class for root of "+inheresIn);
		}
		getReasoner().flush();
		//newClasses = new HashSet<OWLClass>();
		//createPropertyView(getOWLObjectPropertyViaOBOSuffix(HAS_PHENOTYPE), inheresInSomeThing, "has %s");
		createPropertyView(getOWLObjectPropertyViaOBOSuffix(HAS_PHENOTYPE), inheresInSomeThing, "has %s");

		getReasoner().flush();
		Set<OWLClassExpression> attExprs = this.getDirectAttributeClassExpressions();
		Set<OWLAxiom> tempAxioms = materializeClassExpressions(attExprs);
		Set<OWLClass> attClasses = this.extractClassesFromDeclarations(tempAxioms);
		getReasoner().flush();
		generateLeastCommonSubsumers(attClasses, attClasses);
		//this.getOWLOntologyManager().removeAxioms(outputOntology, tempAxioms);
	}

	public Set<OWLClass> getPhenotypeEntityClasses() {
		Set<OWLClass> entityClasses = new HashSet<OWLClass>();
		
		for (OWLClass c : inputOntology.getClassesInSignature(true)) {
			if (!isVerboten(c)) {
				entityClasses.add(c);
				continue;
			}
		}

		entityClasses.removeAll(getQualityClasses());
		
		

		return entityClasses;
	}
	
	public boolean isVerboten(OWLClass c) {
		if (isMultiSpecies) {
			String ont = getOntologyPrefix(c);
			if (ont.equals("FMA") || ont.equals("MA") || ont.equals("EHDAA2") ||
					ont.startsWith("EMAP") || ont.equals("ZFA") || ont.equals("ZFS") ||
					ont.equals("FBbt") || ont.equals("WBbt")) {
				return true;
			}
		}
		Set<OWLAnnotation> anns = c.getAnnotations(inputOntology);
		for (OWLAnnotation ann : anns) {
			String ap = ann.getProperty().getIRI().toString();
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof OWLLiteral) {
				OWLLiteral lv = (OWLLiteral)v;

			}
			if (v instanceof IRI) {
				IRI iv = (IRI)v;
				if (ap.endsWith("inSubset")) {
					if (iv.toString().endsWith("upper_level")) {
						//LOG.info("removing upper level class: "+c);
						return true;
					}
				}

			}
		}

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
