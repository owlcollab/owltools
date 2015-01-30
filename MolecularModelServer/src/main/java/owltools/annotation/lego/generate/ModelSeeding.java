package owltools.annotation.lego.generate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;

import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelManager;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;

import com.google.common.collect.Sets;

public class ModelSeeding<METADATA> {
	
	private final ExpressionMaterializingReasoner reasoner;
	private final SeedingDataProvider dataProvider;
	
	public ModelSeeding(ExpressionMaterializingReasoner reasoner, SeedingDataProvider dataProvider) {
		this.reasoner = reasoner;
		this.dataProvider = dataProvider;
		reasoner.setIncludeImports(true);
	}

	public Pair<String, ModelContainer> seedModel(MolecularModelManager<METADATA> manager, String bp, final METADATA metadata) throws Exception {
		final Map<Bioentity, List<GeneAnnotation>> geneProducts = dataProvider.getGeneProducts(bp);
		if (geneProducts.isEmpty()) {
			throw new Exception("No gene products found for the given process id: "+bp);
		}
		
		final String modelId = manager.generateBlankModel(null, metadata);
		final ModelContainer model = manager.getModel(modelId);
		final OWLGraphWrapper modelGraph = new OWLGraphWrapper(model.getAboxOntology());
		final Relations relations = setupRelations(modelGraph);
		
		// create bp
		Collection<Pair<String, String>> bpAnnotations = null;
		final Pair<String, OWLNamedIndividual> bpIndividual = manager.createIndividualNonReasoning(modelId, bp, bpAnnotations , metadata);
		
		// create gene products
		final Map<Bioentity, Pair<String, OWLNamedIndividual>> gpIndividuals = new HashMap<Bioentity, Pair<String,OWLNamedIndividual>>();
		for(Bioentity gp : geneProducts.keySet()) {
			List<GeneAnnotation> source = geneProducts.get(gp);
			
			// explicitly create OWL class for gene product
			final IRI gpIRI = MolecularModelJsonRenderer.getIRI(gp.getId(), modelGraph);
			final OWLClass gpClass = modelGraph.getDataFactory().getOWLClass(gpIRI);
			model.addAxiom(modelGraph.getDataFactory().getOWLDeclarationAxiom(gpClass));
			
			Collection<Pair<String, String>> gpAnnotations = generateAnnotations(source);
			Pair<String, OWLNamedIndividual> gpIndividual = manager.createIndividualNonReasoning(modelId, gp.getId(), gpAnnotations, metadata);
			gpIndividuals.put(gp, gpIndividual);
		}
		
		Map<Bioentity, List<Pair<String, OWLNamedIndividual>>> mfIndividuals = new HashMap<Bioentity, List<Pair<String,OWLNamedIndividual>>>();
		// add functions
		Map<Bioentity, List<GeneAnnotation>> functions = dataProvider.getFunctions(geneProducts.keySet());
		for(Bioentity gp : functions.keySet()) {
			List<GeneAnnotation> functionAnnotations = functions.get(gp);
			Pair<String, OWLNamedIndividual> gpIndividual = gpIndividuals.get(gp);
			List<Pair<String, OWLNamedIndividual>> mfIndividualList = new ArrayList<Pair<String,OWLNamedIndividual>>(functionAnnotations.size());
			mfIndividuals.put(gp, mfIndividualList);
			
			// TODO choose one representative and preserve others as choice!
			// for now group to minimize mf individuals
			Map<String, List<GeneAnnotation>> mfGroups = removeRedundants(groupByCls(functionAnnotations), modelGraph);
			for(Entry<String, List<GeneAnnotation>> mfGroup : mfGroups.entrySet()) {
				String mf = mfGroup.getKey();
				Collection<Pair<String, String>> mfAnnotations = generateAnnotations(mfGroup.getValue());
				Pair<String, OWLNamedIndividual> mfIndividual = manager.createIndividualNonReasoning(modelId, mf, mfAnnotations , metadata);
				mfIndividualList.add(mfIndividual);
				manager.addFactNonReasoning(modelId, relations.enabled_by_id, mfIndividual.getKey(), gpIndividual.getKey(), mfAnnotations, metadata);
				manager.addFactNonReasoning(modelId, relations.part_of_id, mfIndividual.getKey(), bpIndividual.getKey(), null, metadata);
				
				// TODO check c16 for 'occurs in'
			}
		}
		
//		// set GO:0003674 'molecular_function' for gp with unknown function
//		for(Bioentity gp : Sets.difference(geneProducts.keySet(), functions.keySet())) {
//			Pair<String, OWLNamedIndividual> gpIndividual = gpIndividuals.get(gp);
//			
//			Pair<String, OWLNamedIndividual> mfIndividual = manager.createIndividualNonReasoning(modelId, "GO:0003674", null, metadata);
//			mfIndividuals.put(gp, Collections.singletonList(mfIndividual));
//			manager.addFactNonReasoning(modelId, enabled_by_id, mfIndividual.getKey(), gpIndividual.getKey(), null, metadata);
//			manager.addFactNonReasoning(modelId, part_of_id, mfIndividual.getKey(), bpIndividual.getKey(), generateAnnotations(geneProducts.get(gp)), metadata);
//		}
		// remove individuals for gp with unknown function
		Set<Bioentity> unused = Sets.difference(geneProducts.keySet(), functions.keySet());
		for(Bioentity gp : unused) {
			Pair<String, OWLNamedIndividual> gpIndividual = gpIndividuals.remove(gp);
			manager.deleteIndividualNonReasoning(modelId, gpIndividual.getKey(), metadata);
		}
		
		// add locations
		Map<Bioentity, List<GeneAnnotation>> locations = dataProvider.getLocations(functions.keySet());
		for(Bioentity gp : locations.keySet()) {
			List<Pair<String, OWLNamedIndividual>> relevantMfIndividuals = mfIndividuals.get(gp);
			if (relevantMfIndividuals == null) {
				continue;
			}
			List<GeneAnnotation> locationAnnotations = locations.get(gp);
			Map<String, List<GeneAnnotation>> locationGroups = removeRedundants(groupByCls(locationAnnotations), modelGraph);
			for(Entry<String, List<GeneAnnotation>> locationGroup : locationGroups.entrySet()) {
				String location = locationGroup.getKey();
				Collection<Pair<String, String>> source = generateAnnotations(locationGroup.getValue());
				Pair<String, OWLNamedIndividual> locationIndividual = manager.createIndividualNonReasoning(modelId, location, source, metadata);
				for(Pair<String, OWLNamedIndividual> relevantMfIndividual : relevantMfIndividuals) {
					manager.addFactNonReasoning(modelId, relations.occurs_in_id, relevantMfIndividual.getKey(), locationIndividual.getKey(), source, metadata);
				}
			}
			
		}
		
		// add relations
		// TODO
		
		return Pair.of(modelId, model);
	}
	
	static class Relations {
		final OWLObjectProperty part_of;
		final String part_of_id;
		final OWLObjectProperty enabled_by;
		final String enabled_by_id;
		final OWLObjectProperty occurs_in;
		final String occurs_in_id;
		
		Relations(OWLObjectProperty part_of, String part_of_id,
				OWLObjectProperty enabled_by, String enabled_by_id,
				OWLObjectProperty occurs_in, String occurs_in_id) {
			this.part_of = part_of;
			this.part_of_id = part_of_id;
			this.enabled_by = enabled_by;
			this.enabled_by_id = enabled_by_id;
			this.occurs_in = occurs_in;
			this.occurs_in_id = occurs_in_id;
		}
	}
	
	private Relations setupRelations(OWLGraphWrapper graph) throws Exception {
		final OWLObjectProperty part_of = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		if (part_of == null) {
			throw new Exception("Could not find 'part_of'");
		}
		reasoner.materializeExpressions(part_of);
		final String part_of_id = graph.getIdentifier(part_of.getIRI());
		final OWLObjectProperty enabled_by = graph.getOWLObjectPropertyByIdentifier("RO:0002333");
		if (enabled_by == null) {
			throw new Exception("Could not find 'enabled_by'");
		}
		final String enabled_by_id = graph.getIdentifier(enabled_by.getIRI());
		final OWLObjectProperty occurs_in = graph.getOWLObjectPropertyByIdentifier("BFO:0000066");
		if (occurs_in == null) {
			throw new Exception("Could not find 'occurs in'");
		}
		final String occurs_in_id = graph.getIdentifier(occurs_in.getIRI());
		
		Relations relations = new Relations(part_of, part_of_id, enabled_by, 
								enabled_by_id, occurs_in, occurs_in_id);
		return relations;
	}
	
	private Map<String, List<GeneAnnotation>> groupByCls(List<GeneAnnotation> annotations) {
		Map<String, List<GeneAnnotation>> groups = new HashMap<String, List<GeneAnnotation>>();
		for (GeneAnnotation annotation : annotations) {
			String cls = annotation.getCls();
			List<GeneAnnotation> group = groups.get(cls);
			if (group == null) {
				group = new ArrayList<GeneAnnotation>();
				groups.put(cls, group);
			}
			group.add(annotation);
		}
		return groups;
	}
	
	private Map<String, List<GeneAnnotation>> removeRedundants(Map<String, List<GeneAnnotation>> groups, final OWLGraphWrapper graph) {
		// calculate all ancestors for each group
		Map<String, Set<String>> allAncestors = new HashMap<String, Set<String>>();
		for(String cls : groups.keySet()) {
			OWLClass owlCls = graph.getOWLClassByIdentifier(cls);
			Set<OWLClassExpression> superClassExpressions = reasoner.getSuperClassExpressions(owlCls, false);
			final Set<String> ancestors = new HashSet<String>();
			allAncestors.put(cls, ancestors);
			for (OWLClassExpression ce : superClassExpressions) {
				ce.accept(new OWLClassExpressionVisitorAdapter(){

					@Override
					public void visit(OWLClass desc) {
						ancestors.add(graph.getIdentifier(desc));
					}

					@Override
					public void visit(OWLObjectSomeValuesFrom desc) {
						OWLClassExpression filler = desc.getFiller();
						filler.accept(new OWLClassExpressionVisitorAdapter(){
							@Override
							public void visit(OWLClass desc) {
								ancestors.add(graph.getIdentifier(desc));
							}
							
						});
					}
					
				});
			}
		}
		// check that cls is not an ancestor in any other group
		Map<String, List<GeneAnnotation>> redundantFree = new HashMap<String, List<GeneAnnotation>>();
		for(String cls : groups.keySet()) {
			boolean nonRedundant = true;
			for(Entry<String, Set<String>> group : allAncestors.entrySet()) {
				if (group.getValue().contains(cls)) {
					nonRedundant = false;
					break;
				}
			}
			if (nonRedundant) {
				redundantFree.put(cls, groups.get(cls));
			}
		}
		
		return redundantFree;
	}
	
	private Collection<Pair<String, String>> generateAnnotations(List<GeneAnnotation> source) {
		List<Pair<String,String>> pairs = null;
		if (source != null && !source.isEmpty()) {
			pairs = new ArrayList<Pair<String,String>>(source.size());
			for (GeneAnnotation annotation : source) {
				pairs.add(Pair.of("source", annotation.toString()));
			}
		}
		return pairs;
	}
}
