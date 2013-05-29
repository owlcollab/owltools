package owltools.sim2.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.obolibrary.macro.MacroExpansionVisitor;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

public class PhenoSimHQEPreProcessor extends AbstractOBOSimPreProcessor {

	protected String QUALITY = "PATO_0000001";
	protected String ABNORMAL = "PATO_0000460";
	protected boolean isMultiSpecies = true;

	public void preprocess() {

		final String[] excludeLabels = {
				"quality", "physical object quality", "process quality", "qualitative","1-D extent","abnormal","deviation(from_normal)",
				"increased object quality","decreased object quality",
				"biological_process", "cellular_component", "molecular_function", "cellular process","multicellular organismal process",
				"anatomical structure development","tissue development", "organ development",
				"organ", "blastula","epiblast","blastodic", "viscus", "abdomen organ"
		};
		fixObjectProperties(); // repair ontology
		makeHasPhenotypeInstancesDirect(); // NEW x Type has_phenotype some C ==> x Type C
		makeDevelopmentMorphologyLinks();
		ignoreClasses(new HashSet<String>(Arrays.asList(excludeLabels)));
		addPhenotypePropertyChain();
		removeDisjointClassesAxioms();
		addPhenoAxioms();
		expandInheresInPartOf();
		saveState("expanded");
		flush();

		removeUnreachableAxioms();
		saveState("init");


		// classes and properties
		OWLClass owlThing = getOWLDataFactory().getOWLThing();
		OWLObjectProperty df = getOWLObjectPropertyViaOBOSuffix(DEVELOPS_FROM);
		OWLObjectProperty cpo = getOWLObjectPropertyViaOBOSuffix(COMPOSED_PRIMARILY_OF);
		OWLObjectProperty inheresIn = getOWLObjectPropertyViaOBOSuffix(INHERES_IN);
		OWLObjectProperty hasPart = getOWLObjectPropertyViaOBOSuffix(HAS_PART);


		// E.g. hand part_of some hand
		makeReflexive(PART_OF);

		// E.g. limb bud develops_from some limb bud
		makeReflexive(DEVELOPS_FROM);

		// Everything is made from itself
		// (not truly reflexive, but an approximation for this analysis)
		makeReflexive(COMPOSED_PRIMARILY_OF);

		// ENTITY VIEWS
		if (true) {

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

			// TODO - affected X => qualifier abnormal

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
		}

		saveState("views");

		// INTERSECTIONS: Q and inheres_in some E
		// note: need to materialize QE class expressions - these are currently embedded in
		// "has_part some QE" expressions
		flush();
		// Assume: <PhenoClass> = has_part some (Q and inheres_in some E)
		//     or: Individual: <i> Types: has_phenotype some has_part some (Q and inheres_in some E)
		OWLClass phenotypeRootClass = materializeClassExpression( getOWLDataFactory().getOWLObjectSomeValuesFrom(hasPart, owlThing) );
		Set<OWLClass> compositePhenotypeClasses = materializeClassExpressionsReferencedBy(hasPart);
		LOG.info("things with parts: "+compositePhenotypeClasses.size());
		flush();
		generateLeastCommonSubsumers(compositePhenotypeClasses);
		saveState("qe-intersections");
		flush();

		// note the ontology should have an axiom "Quality SubClassOf inheres_in some Thing"
		OWLClass inheresInSomeThing = this.viewMapByProp.get(owlThing).get(inheresIn);
		if (inheresInSomeThing == null) {
			LOG.warn("Cannot get view class for root of "+inheresIn);
		}
		//flush();
		Set<OWLClass> phenotypeClasses = getReflexiveSubClasses(inheresInSomeThing); // DEP?
		LOG.info("num inheres in some owl:Thing = "+phenotypeClasses.size());

		// add all MP, HP, etc - these have form "has_part some ..."
		phenotypeClasses.addAll(getReflexiveSubClasses(phenotypeRootClass)); // DEP?
		LOG.info(" + has part some owl:Thing = "+phenotypeClasses.size());

		// E.g. has_phenotype some (Q and inheres_in some E)
		// Note that has_phenotype <- has_phenotype o has_part, so this should work for both:
		//  * i1 Type: has_phenotype some (Q and inheres_in some E)
		//  * i1 Type: has_phenotype some (has_part some (Q and inheres_in some E))
		// [auto-generated labels for the latter may look odd]
		// NEW
		//  TODO - auto-detect this
		createPropertyView(getOWLObjectPropertyViaOBOSuffix(HAS_PART), inheresInSomeThing, "%s phenotype");
		//createPropertyView(getOWLObjectPropertyViaOBOSuffix(HAS_PHENOTYPE), phenotypeClasses, "%s phenotype");
		saveState("phenotypes");
		getReasoner().flush();


		//getReasoner().flush();
		//Set<OWLClassExpression> attExprs = this.getDirectAttributeClassExpressions();
		//Set<OWLClass> attClasses = materializeClassExpressions(attExprs);

		trim();
		saveState("final-trimmed");
	}

	// --
	// UTIL
	// --

	protected void makeHasPhenotypeInstancesDirect() {
		// x Type has_phenotype some C ==> x Type C
		LOG.info("x Type has_phenotype some C ==> x Type C");
		OWLObjectProperty hasPhenotype = getOWLObjectPropertyViaOBOSuffix(HAS_PHENOTYPE);
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLClassAssertionAxiom caa : outputOntology.getAxioms(AxiomType.CLASS_ASSERTION)) {
			OWLClassExpression ex = caa.getClassExpression();
			OWLIndividual i = caa.getIndividual();
			if (ex instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)ex;
				if (svf.getProperty().equals(hasPhenotype)) {
					rmAxioms.add(caa);
					newAxioms.add(getOWLDataFactory().getOWLClassAssertionAxiom(svf.getFiller(), i));
				}

			}
		}
		LOG.info("making instances direct: +"+newAxioms.size()+ " -"+rmAxioms.size());
		addAxiomsToOutput(newAxioms, false);
		removeAxiomsFromOutput(rmAxioms, false);
	}

	/**
	 * In MP we have
	 *  + abn. tooth. morphology
	 *    + abn. tooth. dev.
	 *  
	 *  Transform:
	 *  
	 *  'X devel' (P) = _ and R some U ==> add:
	 *    quality and inh some P SubClassOf morphology and inh some E
	 *  
	 */
	protected void makeDevelopmentMorphologyLinks() {
		// TODO - use OPPL for this
		/*
		String oppl =
			"SELECT ?P EquivalentTo "+RESULTS_IN_MORPHOGENESIS_OF+" SOME ?U "+
			"BEGIN ADD ('inheres in' some ?P) SubClassOf (morphology and 'inheres in' some ?U)";
		 */
		LOG.info("making dev-morph links");
		OWLObjectProperty rimo = this.getOWLObjectPropertyViaOBOSuffix(RESULTS_IN_MORPHOGENESIS_OF);
		OWLObjectProperty rido = this.getOWLObjectPropertyViaOBOSuffix(RESULTS_IN_DEVELOPMENT_OF);
		OWLObjectProperty inheresIn = this.getOWLObjectPropertyViaOBOSuffix(INHERES_IN);
		OWLClass morphologyCls = this.getOWLClassViaOBOSuffix("PATO_0000051");
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLEquivalentClassesAxiom eca : outputOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
			Set<OWLObjectProperty> opSig = eca.getObjectPropertiesInSignature();
			if (opSig.contains(rimo) || opSig.contains(rido)) {
				LOG.info("   ECA:"+eca);
				OWLClass goProcCls = null;
				OWLClassExpression goProcExpr = null;
				for (OWLClassExpression x : eca.getClassExpressions()) {
					if (x.isAnonymous()) {
						goProcExpr = x;
					}
					else {
						goProcCls = (OWLClass) x;
					}
				}
				if (goProcCls != null && goProcExpr != null) {
					if (goProcExpr instanceof OWLObjectIntersectionOf) {
						for (OWLClassExpression d : ((OWLObjectIntersectionOf)goProcExpr).getOperands() ) {
							if (d instanceof OWLObjectSomeValuesFrom) {
								OWLClassExpression anatCls = ((OWLObjectSomeValuesFrom)d).getFiller();
								OWLObjectSomeValuesFrom lhs = getOWLDataFactory().getOWLObjectSomeValuesFrom(inheresIn, goProcCls);
								OWLObjectSomeValuesFrom inhExpr = getOWLDataFactory().getOWLObjectSomeValuesFrom(inheresIn, anatCls);
								OWLObjectIntersectionOf rhs = getOWLDataFactory().getOWLObjectIntersectionOf(morphologyCls, inhExpr);
								newAxioms.add(getOWLDataFactory().getOWLSubClassOfAxiom(lhs,rhs));
							}
						}
					}
				}
			}
		}
		addAxiomsToOutput(newAxioms, false);
	}

	/**
	 * @return 'E' classes
	 */
	protected Set<OWLClass> getPhenotypeEntityClasses() {
		Set<OWLClass> entityClasses = new HashSet<OWLClass>();

		// add all that are NOT excluded
		for (OWLClass c : inputOntology.getClassesInSignature(true)) {
			if (!isVerbotenEntity(c)) {
				entityClasses.add(c);
				continue;
			}
			else {
				LOG.info("Excluding from entity set:"+c);
			}
		}

		// exclude all qualities
		entityClasses.removeAll(getQualityClasses());

		entityClasses.removeAll(classesToSkip);


		return entityClasses;
	}

	public boolean isVerbotenEntity(OWLClass c) {
		String ont = getOntologyPrefix(c);
		if (isMultiSpecies) {
			// in a multi-species analysis we only use multi-species ontologies
			// to make new groupings
			if (ont.equals("FMA") || ont.equals("MA") || ont.equals("EHDAA2") ||
					ont.startsWith("EMAP") || ont.equals("ZFA") || ont.equals("ZFS") ||
					ont.equals("FBbt") || ont.equals("WBbt")) {
				return true;
			}
		}
		// phenotype classes don't make entity classes
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
	 * @return set of classes
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
		outSet.removeAll(classesToSkip);
		LOG.info("found "+outSet.size()+" candidates; intersection with "+inSet.size());
		outSet.retainAll(inSet);
		return outSet;
	}

	private void addPhenoAxioms() {
		OWLClass qualityCls = getOWLClassViaOBOSuffix(QUALITY);
		OWLClass abnormalCls = getOWLClassViaOBOSuffix(ABNORMAL);

		// Quality SubClassOf qualifier SOME abnormal
		// (not globally true, approximation for analysis)
		addAxiomToOutput(getOWLDataFactory().getOWLSubClassOfAxiom(qualityCls,
				getOWLDataFactory().getOWLObjectSomeValuesFrom(getOWLObjectPropertyViaOBOSuffix(QUALIFIER), 
						abnormalCls)),
						false);

		// Quality EquivalentTo inheres_in some Thing
		// (not globally true, approximation for analysis)
		addAxiomToOutput(getOWLDataFactory().getOWLEquivalentClassesAxiom(qualityCls,
				getOWLDataFactory().getOWLObjectSomeValuesFrom(getOWLObjectPropertyViaOBOSuffix(INHERES_IN), 
						getOWLDataFactory().getOWLThing())),
						false);

		// Relational Qualities
		// towards/depends_on SubProp of inheres_in
		// TEMPORARY
		addAxiomToOutput(getOWLDataFactory().getOWLSubObjectPropertyOfAxiom(getOWLObjectPropertyViaOBOSuffix(DEPENDS_ON),
				getOWLObjectPropertyViaOBOSuffix(INHERES_IN)),
				false);

	}



	// this should already be present, but we assert in anyway to be sure
	private void addPhenotypePropertyChain() {
		OWLObjectProperty hpart = getOWLObjectPropertyViaOBOSuffix(HAS_PART);
		OWLObjectProperty hphen = getOWLObjectPropertyViaOBOSuffix(HAS_PHENOTYPE);

		List<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>();
		chain.add(hphen);
		chain.add(hpart);
		// has_phenotype <- has_phenotype o has_part
		addAxiomToOutput(getOWLDataFactory().getOWLSubPropertyChainOfAxiom(chain , hphen), false);
	}

	// TODO
	private void expandInheresInPartOf() {
		LOG.info("Expanding IPO; axioms before="+outputOntology.getAxiomCount());
		IRI ipoIRI = getIRIViaOBOSuffix(INHERES_IN_PART_OF);

		OWLAnnotationProperty eap = getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000424"));
		OWLAnnotationProperty aap = getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000425"));

		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLAnnotationAssertionAxiom ax : outputOntology.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
			if (ax.getProperty().equals(eap) || ax.getProperty().equals(aap)) {
				rmAxioms.add(ax);
			}
		}
		LOG.info("Clearing old expansions: "+rmAxioms.size());
		getOWLOntologyManager().removeAxioms(outputOntology, rmAxioms);

		OWLAnnotationAssertionAxiom aaa = getOWLDataFactory().getOWLAnnotationAssertionAxiom(eap, ipoIRI, 
				getOWLDataFactory().getOWLLiteral("BFO_0000052 some (BFO_0000050 some ?Y)"));
		addAxiomToOutput(aaa, false);

		MacroExpansionVisitor mev;
		mev = new MacroExpansionVisitor(outputOntology);
		mev.expandAll();
		flush();



		//mev.expandAll();
		LOG.info("Expanded IPO; axioms after="+outputOntology.getAxiomCount());
	}


	private void fixObjectProperties() {
		OWLEntityRenamer oer;
		oer = new OWLEntityRenamer(getOWLOntologyManager(), outputOntology.getImportsClosure());
		Map<OWLEntity,IRI> e2iri = new HashMap<OWLEntity,IRI>();
		for (OWLObjectProperty p : outputOntology.getObjectPropertiesInSignature(true)) {
			String frag = p.getIRI().getFragment();
			if (frag == null) {
				LOG.info(p+" has no fragment");
				continue;
			}
			LOG.info("Checking "+p+" which has fragment: '"+frag+"'");
			if (frag.equals("part_of")) {
				e2iri.put(p, this.getIRIViaOBOSuffix(PART_OF));
				LOG.info("Mapping legacy property: "+p);
			}
			else if (frag.equals("inheres_in_part_of")) {
				e2iri.put(p, this.getIRIViaOBOSuffix(INHERES_IN_PART_OF));
				LOG.info("Mapping legacy property: "+p);
			}
			else if (frag.equals("inheres_in")) {
				e2iri.put(p, this.getIRIViaOBOSuffix(INHERES_IN));
				LOG.info("Mapping legacy property: "+p);
			}
			else if (frag.equals("towards")) {
				e2iri.put(p, this.getIRIViaOBOSuffix(DEPENDS_ON));
				LOG.info("Mapping legacy property: "+p);
			}
			else if (frag.equals("qualifier")) {
				e2iri.put(p, this.getIRIViaOBOSuffix(QUALIFIER));
				LOG.info("Mapping legacy property: "+p);
			}
			else if (p.getIRI().toString().equals("http://purl.obolibrary.org/obo/RO_0002180")) {
				// TEMP
				e2iri.put(p, this.getIRIViaOBOSuffix(QUALIFIER));
				LOG.info("Mapping legacy property: "+p+" TODO - FIX IN SOURCE ONTOLOGY");
			}
		}
		LOG.info("Mapping legacy properties: "+e2iri.size());
		List<OWLOntologyChange> changes = oer.changeIRI(e2iri);
		LOG.info("Changes: "+changes.size());
		this.getOWLOntologyManager().applyChanges(changes);
	}



}
