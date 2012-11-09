package owltools.gaf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
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
	private final boolean disposeReasonerP;
	
	private final Map<String, Set<OWLClass>> mappingCache = new HashMap<String, Set<OWLClass>>();
	
	/**
	 * Create an instance for the given graph and reasoner.
	 * 
	 * @param graph
	 * @param reasoner
	 * @param disposeReasoner set to true, if the reasoner should also be disposed
	 * @throws UnknownOWLOntologyException
	 * @throws OWLOntologyCreationException
	 * 
	 * @see #dispose()
	 */
	public EcoTools (OWLGraphWrapper graph, OWLReasoner reasoner, boolean disposeReasoner) throws UnknownOWLOntologyException, OWLOntologyCreationException {

		// This has bitten me, so let's try and bew specific...
		if( reasoner == null ){ throw new Error("No reasoner was specified for use with the EcoTools. Add a reasoner for the command line"); }
		
		// assume the graph wrapper is more than eco
		// try to find ECO by its purl
		Set<OWLOntology> allOntologies = graph.getAllOntologies();
		OWLOntology eco = null;
		for (OWLOntology owlOntology : allOntologies) {
			OWLOntologyID id = owlOntology.getOntologyID();
			IRI ontologyIRI = id.getOntologyIRI();
			if (ontologyIRI != null) {
				if (ECO_PURL.equals(ontologyIRI.toString())) {
					eco = owlOntology;
				}
			}
		}
		if (eco != null) {
			// found eco create new wrapper
			this.eco = new OWLGraphWrapper(eco);
		}
		else {
			// did not find eco, use whole wrapper
			this.eco = graph;
		}
		
		this.reasoner = reasoner;
		this.disposeReasonerP = disposeReasoner;
	}
	
	/**
	 * Create a new instance using the given {@link ParserWrapper} to load the ECO.
	 * 
	 * @param pw
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 */
	public EcoTools (ParserWrapper pw) throws OWLOntologyCreationException, IOException {
		this(loadECO(pw));
	}
	
	/**
	 * Create a new instance using the ECO graph wrapper.
	 * 
	 * @param eco
	 */
	public EcoTools(OWLGraphWrapper eco) {
		this.eco = eco;
		OWLReasonerFactory factory = new ElkReasonerFactory();
		final OWLOntology sourceOntology = eco.getSourceOntology();
		reasoner = factory.createReasoner(sourceOntology);
		disposeReasonerP = true;
	}
	
	/**
	 * Retrieve the ECO classes for the given GO annotation codes.
	 * 
	 * @param goCode
	 * @return set of ECO classes
	 */
	public Set<OWLClass> getClassesForGoCode(String goCode) {
		if (goCode == null) {
			return Collections.emptySet();
		}
		Set<OWLClass> classes = mappingCache.get(goCode);
		if (classes == null) {
			// only synchronize for write operations
			synchronized (mappingCache) {
				final Set<String> goXref = createGoEcoXrefs(goCode);
				classes = new HashSet<OWLClass>();	
				Set<OWLObject> allOWLObjects = eco.getAllOWLObjects();
				for (OWLObject owlObject : allOWLObjects) {
					if (eco.isObsolete(owlObject)) {
						continue;
					}
					
					if (owlObject instanceof OWLClass) {
						
						List<ISynonym> synonyms = eco.getOBOSynonyms(owlObject);
						if (synonyms != null && !synonyms.isEmpty()) {
							for (ISynonym synonym : synonyms) {
								if (goCode.equals(synonym.getLabel())) {
									if (hasGoEcoXref(goXref, synonym)) {
										classes.add((OWLClass) owlObject);
										break;
									}
								}
							}
						}
					}
				}
				if (classes.isEmpty()) {
					classes = Collections.emptySet();
				}
				mappingCache.put(goCode, classes);

			}
		}
		return classes;
	}
	
	static Set<String> createGoEcoXrefs(String code) {
		Set<String> set = new HashSet<String>(3);
		set.add("GO:"+code);
		set.add("GOECO:"+code);
		return set;
	}
	
	static boolean hasGoEcoXref(Set<String> goXrefs, ISynonym synonym) {
		Set<String> xrefs = synonym.getXrefs();
		if (xrefs != null && !xrefs.isEmpty()) {
			for(String xref : xrefs) {
				if (goXrefs.contains(xref)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Wrapper method for the reasoner.
	 * 
	 * @param sources
	 * @param reflexive
	 * @return set of super classes
	 */
	public Set<OWLClass> getAncestors(Set<OWLClass> sources, boolean reflexive) {
		if (sources == null || sources.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> result = new HashSet<OWLClass>();
		for (OWLClass source : sources) {
			Set<OWLClass> set = reasoner.getSuperClasses(source, false).getFlattened();
			for (OWLClass cls : set) {
				if (cls.isBuiltIn() == false) {
					result.add(cls);
				}
			}
		}
		if (reflexive) {
			result.addAll(sources);
		}
		if (result.isEmpty()) {
			return Collections.emptySet();
		}
		return result;
		
	}
	
	/**
	 * Wrapper method for the reasoner
	 * 
	 * @param sources
	 * @param reflexive
	 * @return set of sub classes
	 */
	public Set<OWLClass> getDescendents(Set<OWLClass> sources, boolean reflexive) {
		if (sources == null || sources.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> result = new HashSet<OWLClass>();
		for (OWLClass source : sources) {
			Set<OWLClass> set = reasoner.getSubClasses(source, false).getFlattened();
			for (OWLClass cls : set) {
				if (cls.isBuiltIn() == false) {
					result.add(cls);
				}
			}
		}
		if (reflexive) {
			result.addAll(sources);
		}
		if (result.isEmpty()) {
			return Collections.emptySet();
		}
		return result;
	}
	
	/**
	 * Clean up the internal data structures, usually done as last operation.
	 */
	public void dispose() {
		mappingCache.clear();
		if (disposeReasonerP) {
			reasoner.dispose();
		}
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
			if (eco.isObsolete(owlObject)) {
				continue;
			}
			if (owlObject instanceof OWLClass) {
				List<ISynonym> synonyms = eco.getOBOSynonyms(owlObject);
				if (synonyms != null && !synonyms.isEmpty()) {
					for (ISynonym synonym : synonyms) {
						final String label = synonym.getLabel();
						if (goCodes.contains(label)) {
							if (hasGoEcoXref(createGoEcoXrefs(label), synonym)) {
								classes.add((OWLClass) owlObject);
							}
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
