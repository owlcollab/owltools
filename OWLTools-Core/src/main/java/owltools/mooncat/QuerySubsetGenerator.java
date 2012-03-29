package owltools.mooncat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import uk.ac.manchester.cs.owl.owlapi.OWLImportsDeclarationImpl;

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
	 * @param toMerge 
	 */
	public void createSubOntologyFromDLQuery(String dlQueryString,
			OWLGraphWrapper sourceGraph, OWLGraphWrapper targetGraph, 
			OWLReasonerFactory reasonerFactory, Set<OWLOntology> toMerge)
	{
		try {
			Set<OWLClass> subset;
			subset = DLQueryTool.executeDLQuery(dlQueryString, sourceGraph, reasonerFactory);
			if (subset.isEmpty()) {
				return;
			}
			createSubSet(targetGraph, subset, toMerge);
			
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
	 * @param toMerge 
	 */
	public void createSubOntologyFromDLQuery(OWLClass namedQuery,
			OWLGraphWrapper sourceGraph, OWLGraphWrapper targetGraph, 
			OWLReasonerFactory reasonerFactory, Set<OWLOntology> toMerge)
	{
		try {
			Set<OWLClass> subset = DLQueryTool.executeQuery(namedQuery, sourceGraph.getSourceOntology(), reasonerFactory);
			if (subset.isEmpty()) {
				return;
			}
			createSubSet(targetGraph, subset, toMerge);
		} catch (OWLOntologyCreationException e) {
			LOG.error("Could not create ontology.", e);
			// TODO throw Exception?
			return;
		}
	}

	/**
	 * Given a collection of classes (such as those generated from a reasoner getSubClasses call), create a 
	 * subset ontology and place it in targetGraph.
	 * 
	 * The subset ontology is created by first collecting all axioms from toMerge that form a description of the input subset classes,
	 * adding these to the target ontology, and then including the reference closure via Mooncat
	 * 
	 * @param targetGraph
	 * @param subset
	 * @param toMerge
	 * @throws OWLOntologyCreationException
	 */
	public void createSubSet(OWLGraphWrapper targetGraph, 
			Set<OWLClass> subset, Set<OWLOntology> toMerge) throws OWLOntologyCreationException 
	{
		OWLOntology targetOntology = targetGraph.getSourceOntology();
		
		// import axioms set
		Set<OWLAxiom> importAxioms = new HashSet<OWLAxiom>();
		for (OWLOntology mergeOntology : toMerge) {
			for (OWLClass cls : subset) {
				importAxioms.addAll(mergeOntology.getAxioms(cls));
			}
		}
		
		// remove merge imports
		OWLOntologyManager targetManager = targetOntology.getOWLOntologyManager();
		List<OWLOntologyChange> removeImports = new ArrayList<OWLOntologyChange>();
		for(OWLOntology m : toMerge) {
			removeImports.add(new RemoveImport(targetOntology, new OWLImportsDeclarationImpl(m.getOntologyID().getOntologyIRI())));
		}
		targetManager.applyChanges(removeImports);
		
		// add axiom set to target ontology.
		targetManager.addAxioms(targetOntology, importAxioms);
		
		LOG.info("Start Mooncat for subset.");
		Mooncat mooncat = new Mooncat(targetGraph);
		for (OWLOntology ont : toMerge) {
			mooncat.addReferencedOntology(ont);
		}
			
		// create Closure
		Set<OWLAxiom> axioms = mooncat.getClosureAxiomsOfExternalReferencedEntities();
		mooncat.addSubAnnotationProperties(axioms);
			
		// add missing axioms
		int count = targetManager.addAxioms(targetOntology, axioms).size();
		LOG.info("Added "+count+" axioms to the query ontology");
		return;
	}

}
