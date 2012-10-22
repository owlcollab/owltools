package owltools.gaf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.io.ParserWrapper;

/**
 * Methods to simplify the work with the evidence code ontology (ECO). 
 */
public class EcoTools {
	
	public static final String ECO_PURL = "http://purl.obolibrary.org/obo/eco.owl";
	
	private final OWLGraphWrapper eco;
	private final OWLReasoner reasoner;
	
	public EcoTools (ParserWrapper pw) throws OWLOntologyCreationException, IOException {
		this(loadECO(pw));
	}
	
	public EcoTools(OWLGraphWrapper eco) {
		this.eco = eco;
		OWLReasonerFactory factory = new ElkReasonerFactory();
		final OWLOntology sourceOntology = eco.getSourceOntology();
		reasoner = factory.createReasoner(sourceOntology);
	}
	
	/**
	 * Retrieve the ECO classes for the given GO annotation codes.
	 * 
	 * @param goCodes
	 * @return set of ECO classes
	 */
	public Set<OWLClass> getClassesForGoCodes(String...goCodes) {
		return getClassesForGoCodes(asSet(goCodes));
	}
	
	/**
	 * Retrieve the ECO classes for the given GO annotation codes.
	 * 
	 * @param goCodes
	 * @return set of ECO classes
	 */
	public Set<OWLClass> getClassesForGoCodes(Set<String> goCodes) {
		if (goCodes == null || goCodes.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> classes = new HashSet<OWLClass>();
		Set<OWLObject> allOWLObjects = eco.getAllOWLObjects();
		for (OWLObject owlObject : allOWLObjects) {
			if (owlObject instanceof OWLClass) {
				List<ISynonym> synonyms = eco.getOBOSynonyms(owlObject);
				if (synonyms != null && !synonyms.isEmpty()) {
					for (ISynonym synonym : synonyms) {
						if (goCodes.contains(synonym.getLabel())) {
							classes.add((OWLClass) owlObject);
						}
					}
				}
			}
		}
		return classes;
	}
	
	public Set<String> getCodes(Set<OWLClass> classes) {
		return getCodes(classes, false);
	}
	
	public Set<String> getCodes(Set<OWLClass> classes, boolean includeDescendants) {
		Set<String> codes = new HashSet<String>();
		
		if (includeDescendants) {
			// use reasoner to infer descendants
			Set<OWLClass> allSubClasses = new HashSet<OWLClass>();
			for (OWLClass owlClass : classes) {
				allSubClasses.add(owlClass);
				NodeSet<OWLClass> nodeSet = reasoner.getSubClasses(owlClass, false);
				for(Node<OWLClass> node : nodeSet) {
					if (node.isTopNode() == false && node.isBottomNode() == false) {
						allSubClasses.addAll(node.getEntities());
					}
				}
			}
			classes = allSubClasses;
		}
		for (OWLClass owlClass : classes) {
			final String oboId = eco.getIdentifier(owlClass);
			if (oboId == null) {
				continue;
			}
			codes.add(oboId);
			List<ISynonym> synonyms = eco.getOBOSynonyms(owlClass);
			if (synonyms != null && !synonyms.isEmpty()) {
				for (ISynonym synonym : synonyms) {
					final String synLabel = synonym.getLabel();
					if (synLabel.length() <= 4) {
						// TODO replace the length hack with a proper way to identify the codes
						// do not rely on SCOPE, there are currently inconsistencies in ECO
						// NOT all GO codes are EXACT synonyms
						codes.add(synLabel);
					}
				}
			}
		}
		return codes;
	}
	
	public void dispose() {
		reasoner.dispose();
	}
	
	
	/**
	 * Load the evidence code ontology (ECO) from its default PURL.
	 * 
	 * @param pw
	 * @return eco ontology graph
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public static OWLGraphWrapper loadECO(ParserWrapper pw) throws IOException, OWLOntologyCreationException {
		return pw.parseToOWLGraph(ECO_PURL);
	}
	
	/**
	 * Create a set from an array. Syntactic sugar.
	 * 
	 * @param ts
	 * @return set
	 */
	static <T> Set<T> asSet(T...ts) {
		if (ts.length == 0) {
			return Collections.emptySet();
		}
		Set<T> set = new HashSet<T>(Arrays.asList(ts));
		return set;
	}
	
	/**
	 * Retrieve the ECO classes for the given GO annotation codes.
	 * 
	 * @param eco
	 * @param goCodes
	 * @return set of ECO classes
	 */
	public static Set<OWLClass> getClassesForGoCodes(OWLGraphWrapper eco, String...goCodes) {
		return getClassesForGoCodes(eco, asSet(goCodes));
	}

	/**
	 * Retrieve the ECO classes for the given GO annotation codes.
	 * 
	 * @param eco
	 * @param goCodes
	 * @return set of ECO classes
	 */
	public static Set<OWLClass> getClassesForGoCodes(OWLGraphWrapper eco, Set<String> goCodes) {
		if (goCodes == null || goCodes.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> classes = new HashSet<OWLClass>();
		Set<OWLObject> allOWLObjects = eco.getAllOWLObjects();
		for (OWLObject owlObject : allOWLObjects) {
			if (owlObject instanceof OWLClass) {
				List<ISynonym> synonyms = eco.getOBOSynonyms(owlObject);
				if (synonyms != null && !synonyms.isEmpty()) {
					for (ISynonym synonym : synonyms) {
						if (goCodes.contains(synonym.getLabel())) {
							classes.add((OWLClass) owlObject);
						}
					}
				}
			}
		}
		return classes;
	}
	
	public static Set<String> getCodes(Set<OWLClass> classes, OWLGraphWrapper eco) {
		return getCodes(classes, eco, false);
	}
	
	/**
	 * Retrieve the set of codes for a given set of ECO classes. Option: include
	 * sub classes of all the classes. Uses a reasoner to infer the sub classes.
	 * 
	 * @param classes
	 * @param eco
	 * @param includeDescendants
	 *            set to true if sub classes should be included
	 * @return set of codes potentially used for the given ECO classes
	 */
	public static Set<String> getCodes(Set<OWLClass> classes, OWLGraphWrapper eco, boolean includeDescendants) {
		Set<String> codes = new HashSet<String>();
		
		if (includeDescendants) {
			// use reasoner to infer descendants
			OWLReasonerFactory factory = new ElkReasonerFactory();
			final OWLOntology sourceOntology = eco.getSourceOntology();
			OWLReasoner reasoner = factory.createReasoner(sourceOntology);
			try {
				Set<OWLClass> allSubClasses = new HashSet<OWLClass>();
				for (OWLClass owlClass : classes) {
					allSubClasses.add(owlClass);
					NodeSet<OWLClass> nodeSet = reasoner.getSubClasses(owlClass, false);
					for(Node<OWLClass> node : nodeSet) {
						if (node.isTopNode() == false && node.isBottomNode() == false) {
							allSubClasses.addAll(node.getEntities());
						}
					}
				}
				classes = allSubClasses;
			}
			finally {
				reasoner.dispose();
			}
		}
		for (OWLClass owlClass : classes) {
			final String oboId = eco.getIdentifier(owlClass);
			if (oboId == null) {
				continue;
			}
			codes.add(oboId);
			List<ISynonym> synonyms = eco.getOBOSynonyms(owlClass);
			if (synonyms != null && !synonyms.isEmpty()) {
				for (ISynonym synonym : synonyms) {
					final String synLabel = synonym.getLabel();
					if (synLabel.length() <= 4) {
						// TODO replace the length hack with a proper way to identify the codes
						// do not rely on SCOPE, there are currently inconsistencies in ECO
						// NOT all GO codes are EXACT synonyms
						codes.add(synLabel);
					}
				}
			}
		}
		return codes;
	}
	
}
