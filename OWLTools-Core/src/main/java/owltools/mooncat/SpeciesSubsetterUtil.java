package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

/**
 * 
 * 
 * @author cjm
 * 
 */
public class SpeciesSubsetterUtil {

	private Logger LOG = Logger.getLogger(SpeciesSubsetterUtil.class);

	OWLGraphWrapper graph;

	OWLOntology ont;
	OWLOntologyManager mgr;
	OWLDataFactory fac;
	public OWLReasoner reasoner;
	public OWLObjectProperty viewProperty;
	public OWLClass taxClass;
	public OWLClass rootClass;
	String[] defaultRelationsToRemove =
	{
		"evolved_from",
		"homologous_to"
		
	};

	public SpeciesSubsetterUtil(OWLGraphWrapper g) {
		graph = g;
	}


	public void removeOtherSpecies() {

		ont = graph.getSourceOntology();
		mgr = ont.getOWLOntologyManager();
		fac = mgr.getOWLDataFactory();
		
		MacroExpansionVisitor mev = new MacroExpansionVisitor(ont);
		mev.expandAll();
		
		if (viewProperty == null) {
			IRI iri = graph.getIRIByIdentifier("RO:0002162");
			LOG.info("View property.IRI = "+iri);

			viewProperty = fac.getOWLObjectProperty(iri);
		}
		LOG.info("View property = "+viewProperty);

		Set<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (String p : defaultRelationsToRemove) {
			IRI iri = graph.getIRIByIdentifier(p);
			OWLObjectProperty prop = fac.getOWLObjectProperty(iri);
			LOG.info(" FILTERING: "+prop);
			//props.add(prop);
			for (OWLAxiom ax : ont.getAxioms()) {
				if (ax.getObjectPropertiesInSignature().contains(prop)) {
					LOG.info("REMOVING:"+ax);
					rmAxioms.add(ax);
				}
			}
		}

		LOG.info("Removing: "+rmAxioms.size());
		mgr.removeAxioms(ont, rmAxioms);

		/*
		Mooncat m = new Mooncat(graph);
		m.retainAxiomsInPropertySubset(graph.getSourceOntology(),props,null);
		m.removeDanglingAxioms();
		*/
		
		rmAxioms = new HashSet<OWLAxiom>();
		if (rootClass == null)
			rootClass = fac.getOWLThing();
		//rootClass = graph.getOWLClassByIdentifier("UBERON:0001062");
		LOG.info("AE = "+rootClass);
		LOG.info("TC = "+taxClass);
		OWLClassExpression rx = fac.getOWLObjectSomeValuesFrom(viewProperty,
				taxClass);
		OWLSubClassOfAxiom qax = fac.getOWLSubClassOfAxiom(
				rootClass, rx);
		mgr.addAxiom(ont, qax);
		LOG.info("Constraint: "+qax);
		
		Set<OWLClass> ucs = reasoner.getEquivalentClasses(fac.getOWLNothing()).getEntities();
		LOG.info("UCS: "+ucs.size());
		for (OWLClass uc : ucs) {
			LOG.info("Removing: "+uc);
			rmAxioms.addAll(ont.getAxioms(uc));
			rmAxioms.add(fac.getOWLDeclarationAxiom(uc));
			rmAxioms.addAll(ont.getAnnotationAssertionAxioms(uc.getIRI()));
		}
		mgr.removeAxioms(ont, rmAxioms);
	}

}
