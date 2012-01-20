package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;

/**
 * Tools for creating a sub ontology spanning multiple ontologies. Uses
 * {@link Mooncat} and {@link DLQueryTool}.
 * 
 */
public class QuerySubsetGenerator {
	
	private static final Logger LOG = Logger.getLogger(QuerySubsetGenerator.class);

	/**
	 * Create a new sub ontology from a given DL query and source ontology. The
	 * subset will be created in the target ontology.
	 * 
	 * @param dlQueryString
	 * @param sourceGraph
	 * @param targetGraph
	 * @param reasonerFactory
	 */
	public void createSubOntologyFromDLQuery(String dlQueryString,
			OWLGraphWrapper sourceGraph, OWLGraphWrapper targetGraph, 
			OWLReasonerFactory reasonerFactory)
	{
		try {
			Set<OWLClass> subset;
			subset = DLQueryTool.executeDLQuery(dlQueryString, sourceGraph, reasonerFactory);
			if (subset.isEmpty()) {
				return;
			}
			createSubSet(sourceGraph, targetGraph, subset);
			
		} catch (ParserException e) {
			LOG.error("Could not parse query: "+dlQueryString, e);
			// TODO throw Exception?
			return;
		} catch (OWLOntologyCreationException e) {
			LOG.error("Could not create ontology.", e);
			// TODO throw Exception?
			return;
		}
	}
	
	/**
	 * Create a new sub ontology from a given DL query and source ontology. The
	 * subset will be created in the target ontology.
	 * 
	 * @param namedQuery
	 * @param sourceGraph
	 * @param targetGraph
	 * @param reasonerFactory
	 */
	public void createSubOntologyFromDLQuery(OWLClass namedQuery,
			OWLGraphWrapper sourceGraph, OWLGraphWrapper targetGraph, 
			OWLReasonerFactory reasonerFactory)
	{
		try {
			Set<OWLClass> subset = DLQueryTool.executeQuery(namedQuery, sourceGraph.getSourceOntology(), reasonerFactory);
			if (subset.isEmpty()) {
				return;
			}
			createSubSet(sourceGraph, targetGraph, subset);
		} catch (OWLOntologyCreationException e) {
			LOG.error("Could not create ontology.", e);
			// TODO throw Exception?
			return;
		}
	}

	private void createSubSet(OWLGraphWrapper sourceGraph, OWLGraphWrapper targetGraph, 
			Set<OWLClass> subset) throws OWLOntologyCreationException 
	{
		OWLOntology sourceOntology = sourceGraph.getSourceOntology();
		OWLOntology targetOntology = targetGraph.getSourceOntology();
		OWLOntologyManager targetManager = targetOntology.getOWLOntologyManager();
		
		// add subset to target ontology.
		for(OWLClass cls : subset) {
			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			axioms.addAll(sourceOntology.getAxioms(cls));
			targetManager.addAxioms(targetOntology, axioms);
		}
		
		LOG.info("Start Mooncat for subset.");
		Mooncat mooncat = new Mooncat(targetGraph);
		for (OWLOntology ont : sourceGraph.getAllOntologies()) {
			mooncat.addReferencedOntology(ont);
		}
			
		// create Closure
		Set<OWLAxiom> axioms = mooncat.getClosureAxiomsOfExternalReferencedEntities();
		mooncat.addSubAnnotationProperties(axioms);
			
		// add missing axioms
		targetManager.addAxioms(targetOntology, axioms);
		LOG.info("Added "+axioms.size()+" to the sub ontology");
		return;
	}

}
