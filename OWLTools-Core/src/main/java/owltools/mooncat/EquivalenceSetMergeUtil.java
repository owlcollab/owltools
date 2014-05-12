package owltools.mooncat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
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

		Set<Node<? extends OWLEntity>> nodes = new HashSet<Node<? extends OWLEntity>>();
		Map<OWLEntity, Node<? extends OWLEntity>> nodeByRep = 
				new HashMap<OWLEntity, Node<? extends OWLEntity>>();
		for (OWLClass c : ont.getClassesInSignature()) {
			Node<OWLClass> n = reasoner.getEquivalentClasses(c);
			if (n.getSize() > 1) {
				nodes.add(n);
				nodeByRep.put(c, n);
			}			
		}
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			IndividualNodeSetPolicy policy = reasoner.getIndividualNodeSetPolicy();
			Node<OWLNamedIndividual> n = reasoner.getSameIndividuals(i);
			// warning - elk doesn't do this
			LOG.info("SAME INDS: "+n.getEntities());
			if (n.getSize() > 1) {
				nodes.add(n);
				nodeByRep.put(i, n);
			}			
		}
		LOG.info("TOTAL SETS-OF-SETS (redundant): "+nodeByRep.keySet().size());

		Map<OWLEntity,IRI> e2iri = new HashMap<OWLEntity,IRI>();

		Set<OWLEntity> seenClasses = new HashSet<OWLEntity>();
		Set <OWLAxiom> newAxioms = new HashSet<OWLAxiom>();

		for (Node<? extends OWLEntity> n : nodes) {
			boolean isSeen = false;
			for (OWLEntity c : n.getEntities()) {
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
				OWLEntity rep = null;
				Double best = null;
				for (OWLEntity c : n.getEntities()) {
					Double score = getScore(c, prefixScoreMap);
					LOG.info(c +" SC: "+score);
					if (best == null || (score != null && score > best)) {
						rep = c;
						best = score;
					}
				}
				LOG.info("BEST: "+best+" FOR: "+n.getEntities());
				for (OWLEntity c : n.getEntities()) {
					if (c.equals(rep))
						continue;
					LOG.info(c + " --> "+rep);
					e2iri.put(c, rep.getIRI());
					if (isAddEquivalenceAxioms) {
						// if A is equivalent to B we may wish to retain a historical
						// record of this equivalance - after A is merged into B
						// (assuming B is the representative), all axioms referencing A
						// will be gone, so we re-add the original equivalence,
						// possibly translating to an obo-style xref
						OWLAxiom eca;

						if (true) {
							// TODO - allow other options - for now make an xref
							OWLAnnotationProperty lap = graph.getAnnotationProperty(OboFormatTag.TAG_XREF.getTag());
							OWLAnnotationValue value = 
									graph.getDataFactory().getOWLLiteral(graph.getIdentifier(c));
							eca =
									graph.getDataFactory().getOWLAnnotationAssertionAxiom(lap, rep.getIRI(), value);
						}
						else {
							if (c instanceof OWLClass) {
								graph.getDataFactory().getOWLEquivalentClassesAxiom((OWLClass)c, (OWLClass)rep);
							}
							else {
								graph.getDataFactory().getOWLSameIndividualAxiom((OWLNamedIndividual)c, (OWLNamedIndividual)rep);
							}
							// note: this creates a dangler
							// note: if  |equivalence set| = n>2, creates n-1 axioms 

						}
						LOG.info("Preserving ECA to represetative: "+eca);
						newAxioms.add(eca);
					}
				}
			}

			// some properties may be desired to have cardinality = 1
			for (OWLAnnotationProperty p : propertyPrefixScoreMap.keySet()) {
				Map<String, Double> pmap = propertyPrefixScoreMap.get(p);
				OWLEntity rep = null;
				Double best = null;
				for (OWLEntity c : n.getEntities()) {
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
				for (OWLEntity c : n.getEntities()) {
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

	private Double getScore(OWLEntity c, Map<String, Double> pmap) {
		for (String p : pmap.keySet()) {
			if (hasPrefix(c,p)) {
				return pmap.get(p);
			}
		}
		return null;
	}

	private boolean hasPrefix(OWLEntity c, String p) {
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
