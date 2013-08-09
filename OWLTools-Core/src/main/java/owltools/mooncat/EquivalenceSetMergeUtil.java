package owltools.mooncat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import owltools.graph.OWLGraphWrapper;

/**
 * 
 * @author cjm
 * 
 */
public class EquivalenceSetMergeUtil {

	private Logger LOG = Logger.getLogger(EquivalenceSetMergeUtil.class);

	OWLGraphWrapper graph;

	// map from species-class to generic-class
	OWLOntology ont;
	public OWLReasoner reasoner;
	Map<String,Double> prefixScoreMap = new HashMap<String,Double>();
	Map<OWLAnnotationProperty,Map<String,Double>> propertyPrefixScoreMap = 
			new HashMap<OWLAnnotationProperty,Map<String,Double>>();
	boolean isAddEquivalenceAxioms = true;


	public EquivalenceSetMergeUtil(OWLGraphWrapper g, OWLReasoner r) {
		graph = g;
		ont = graph.getSourceOntology();
		reasoner = r;
	}
	
	public void setPropertyPrefixScore(OWLAnnotationProperty p, String prefix, Double score) {
		LOG.info("Setting "+p+" priority: "+prefix+" = "+score);
		if (!propertyPrefixScoreMap.containsKey(p))
			propertyPrefixScoreMap.put(p, new HashMap<String,Double>());
		setPrefixScore(propertyPrefixScoreMap.get(p), prefix, score);
	}

	public void setPrefixScore(Map<String, Double> pmap, String prefix,
			Double score) {
		pmap.put(prefix, score);		
	}
	public void setPrefixScore(String prefix,
			Double score) {
		prefixScoreMap.put(prefix, score);		
	}

	/**
	 * 
	 */
	public void merge() {

		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		Map<OWLClass, Node<OWLClass>> nodeByRep = 
				new HashMap<OWLClass, Node<OWLClass>>();
		for (OWLClass c : ont.getClassesInSignature()) {
			Node<OWLClass> n = reasoner.getEquivalentClasses(c);
			if (n.getSize() > 1) {
				nodes.add(n);
				nodeByRep.put(c, n);
			}			
		}
		LOG.info("TOTAL SETS-OF-SETS (redundant): "+nodeByRep.keySet().size());

		Map<OWLEntity,IRI> e2iri = new HashMap<OWLEntity,IRI>();

		Set<OWLClass> seenClasses = new HashSet<OWLClass>();
		Set <OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		
		for (Node<OWLClass> n : nodes) {
			boolean isSeen = false;
			for (OWLClass c : n.getEntities()) {
				if (seenClasses.contains(c)) {
					isSeen = true;
					break;
				}
				seenClasses.add(c);
			}
			if (isSeen) {
				continue;
			}
			if (true) {
				OWLClass rep = null;
				Double best = null;
				for (OWLClass c : n.getEntities()) {
					Double score = getScore(c, prefixScoreMap);
					if (best == null || (score != null && score > best)) {
						rep = c;
						best = score;
					}
				}
				for (OWLClass c : n.getEntities()) {
					if (c.equals(rep))
						continue;
					LOG.info(c + " --> "+rep);
					e2iri.put(c, rep.getIRI());
					if (isAddEquivalenceAxioms) {
						// note: this creates a dangler
						newAxioms.add(graph.getDataFactory().getOWLEquivalentClassesAxiom(c, rep));
					}
				}
			}

			// some properties may be desired to have cardinality = 1
			for (OWLAnnotationProperty p : propertyPrefixScoreMap.keySet()) {
				Map<String, Double> pmap = propertyPrefixScoreMap.get(p);
				OWLClass rep = null;
				Double best = null;
				for (OWLClass c : n.getEntities()) {
					String v = graph.getAnnotationValue(c, p);
					if (v == null || v.equals(""))
						continue;
					LOG.info(c + " . "+p+" = "+v);
					Double score = getScore(c, pmap);
					if (best == null || (score != null && score > best)) {
						rep = c;
						best = score;
					}
				}
				for (OWLClass c : n.getEntities()) {
					if (c.equals(rep))
						continue;
					Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
					for (OWLAnnotationAssertionAxiom ax : ont.getAnnotationAssertionAxioms(c.getIRI())) {
						if (ax.getProperty().equals(p)) {
							rmAxioms.add(ax); // todo - allow translation to other property
						}
					}
					graph.getManager().removeAxioms(ont, rmAxioms);
				}
			}
		}

		OWLEntityRenamer oer = new OWLEntityRenamer(graph.getManager(), graph.getAllOntologies());

		LOG.info("Mapping "+e2iri.size()+" entities");
		List<OWLOntologyChange> changes = oer.changeIRI(e2iri);
		graph.getManager().applyChanges(changes);
		LOG.info("Mapped "+e2iri.size()+" entities!");
		
		graph.getManager().addAxioms(ont, newAxioms);

		// remove any reflexive assertions remaining
		// note: this is incomplete. Need a policy for what to do with equivalence axioms etc.
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLSubClassOfAxiom a : ont.getAxioms(AxiomType.SUBCLASS_OF)) {
			if (a.getSubClass().equals(a.getSuperClass())) {
				LOG.info("REFLEXIVE: "+a);
				rmAxioms.add(a);
			}
		}
		for (OWLEquivalentClassesAxiom a : ont.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
			if (a.getClassExpressions().size() < 2) {
				LOG.info("UNARY: "+a);
				rmAxioms.add(a);
			}
		}
		if (rmAxioms.size() > 0) {
			LOG.info("REMOVING REFLEXIVE AXIOMS: "+rmAxioms.size());
			graph.getManager().removeAxioms(ont, rmAxioms);
		}

	}

	private Double getScore(OWLClass c, Map<String, Double> pmap) {
		for (String p : pmap.keySet()) {
			if (hasPrefix(c,p)) {
				return pmap.get(p);
			}
		}
		return null;
	}

	private boolean hasPrefix(OWLClass c, String p) {
		if (p.startsWith("http")) {
			return c.getIRI().toString().startsWith(p);
		}
		if (c.getIRI().toString().contains("/"+p))
			return true;
		if (c.getIRI().toString().contains("#"+p))
			return true;
		return false;
	}



}
