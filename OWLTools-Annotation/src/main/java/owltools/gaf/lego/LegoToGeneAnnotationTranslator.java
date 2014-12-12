package owltools.gaf.lego;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.obolibrary.obo2owl.Owl2Obo;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.Bioentity;
import owltools.gaf.BioentityDocument;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.SimpleEcoMapper;
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;

public class LegoToGeneAnnotationTranslator {

	private final OWLObjectProperty partOf;
	private final OWLObjectProperty occursIn;
	private final OWLObjectProperty enabledBy;
	
	private final OWLAnnotationProperty source;
	private final OWLAnnotationProperty contributor;
	private final OWLAnnotationProperty date;
	private final OWLAnnotationProperty evidence;
	
	private final OWLClass mf;
	private final Set<OWLClass> mfSet;
	
	private final OWLClass cc;
	private final Set<OWLClass> ccSet;
	
	private final OWLClass bp;
	private final Set<OWLClass> bpSet;

	private final SimpleEcoMapper mapper;
	
	public LegoToGeneAnnotationTranslator(OWLGraphWrapper graph, OWLReasoner reasoner, SimpleEcoMapper mapper) {
		this.mapper = mapper;
		OWLDataFactory df = graph.getDataFactory();
		partOf = OBOUpperVocabulary.BFO_part_of.getObjectProperty(df);
		occursIn = OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(df);
		
		mf = OBOUpperVocabulary.GO_molecular_function.getOWLClass(df);
		cc = graph.getOWLClassByIdentifier("GO:0005575");
		bp = OBOUpperVocabulary.GO_biological_process.getOWLClass(df);
		
		enabledBy = OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(df);
		
		source = df.getOWLAnnotationProperty(LegoAnnotationType.source.getAnnotationProperty());
		contributor = df.getOWLAnnotationProperty(LegoAnnotationType.contributor.getAnnotationProperty());
		date = df.getOWLAnnotationProperty(LegoAnnotationType.date.getAnnotationProperty());
		evidence = df.getOWLAnnotationProperty(LegoAnnotationType.evidence.getAnnotationProperty());
		
		bpSet = getAllSubClasses(bp, graph, reasoner, true);
		mfSet = getAllSubClasses(mf, graph, reasoner, true);
		ccSet = getAllSubClasses(cc, graph, reasoner, false);
	}
	
	private Set<OWLClass> getAllSubClasses(OWLClass cls, OWLGraphWrapper g, OWLReasoner r, boolean reflexive) {
		Set<OWLClass> allSubClasses = r.getSubClasses(cls, false).getFlattened();
		Iterator<OWLClass> it = allSubClasses.iterator();
		while (it.hasNext()) {
			OWLClass current = it.next();
			if (current.isBuiltIn()) {
				it.remove();
			}
		}
		if (reflexive) {
			allSubClasses.add(cls);
		}
		return allSubClasses;
	}

	
	private static class Entry<T> {
		T value;
		Metadata metadata;
		// TODO multi-species interactions
	}
	
	private static class Metadata {
		OWLClass evidence = null;
		String assignedBy = null;
		String date = null;
		Set<String> sources = null;
		
		Metadata copy() {
			Metadata metadata = new Metadata();
			metadata.evidence = this.evidence;
			metadata.assignedBy = this.assignedBy;
			metadata.date = this.date;
			metadata.sources = copy(this.sources);
			return metadata;
		}
		
		private static Set<String> copy(Set<String> c) {
			if (c == null) {
				return null;
			}
			return new HashSet<String>(c);
		}
		
		private static Metadata combine(Metadata primary, Metadata secondary) {
			if (primary.evidence != null) {
				return primary.copy();
			}
			if (secondary.evidence != null) {
				return secondary.copy();
			}
			if (primary.sources != null && !primary.sources.isEmpty()) {
				return primary.copy();
			}
			return secondary.copy();
		}
	}

	private class Summary {
		
		Set<Entry<OWLClass>> activities = null;
		Set<Entry<OWLObjectSomeValuesFrom>> activityExpressions = null;
		Set<Entry<OWLClass>> locations = null;
		Set<Entry<OWLClass>> processes = null;
		OWLClass entity = null;
		String entityType = null;
		String entityTaxon = null;
		
		void addMfOrBp(OWLClass cls, Metadata metadata) {
			if (isMf(cls)) {
				activities = addAnnotation(cls, metadata , activities);
			}
			else if (isBp(cls)) {
				processes = addAnnotation(cls, metadata, processes);
			}
		}
		
		void addCc(OWLClassExpression ce, Metadata metadata) {
			if (ce instanceof OWLClass) {
				OWLClass cls = ce.asOWLClass();
				locations = addAnnotation(cls, metadata, locations);
			}
		}
		
		private <T> Set<Entry<T>> addAnnotation(T cls, Metadata metadata, Set<Entry<T>> set) {
			if (set == null) {
				set = new HashSet<Entry<T>>();
			}
			Entry<T> entry = new Entry<T>();
			entry.value = cls;
			entry.metadata = metadata.copy();
			set.add(entry);
			return set;
		}

		void addProcesses(Set<Entry<OWLClass>> processes, Metadata metadata) {
			if (processes != null) {
				if (this.processes == null) {
					this.processes = new HashSet<Entry<OWLClass>>();
				}
				for(Entry<OWLClass> process : processes) {
					Entry<OWLClass> newEntry = new Entry<OWLClass>();
					newEntry.value = process.value;
					newEntry.metadata = Metadata.combine(metadata, process.metadata);
					this.processes.add(newEntry);
				}
			}
		}
		
		void addLocations(Set<Entry<OWLClass>> locations) {
			if (locations != null) {
				if (this.locations == null) {
					this.locations = new HashSet<Entry<OWLClass>>(locations);
				}
				else {
					this.locations.addAll(locations);
				}
			}
		}
		
		void addExpression(OWLObjectPropertyExpression p, Collection<Entry<OWLClass>> entries, OWLDataFactory f) {
			if (entries != null && !entries.isEmpty()) {
				for (Entry<OWLClass> entry : entries) {
					OWLObjectSomeValuesFrom svf = f.getOWLObjectSomeValuesFrom(p, entry.value);
					activityExpressions = addAnnotation(svf, entry.metadata, activityExpressions);
				}
			}
		}
		
		void addExpression(OWLObjectPropertyExpression p, OWLClass cls, Metadata metadata, OWLDataFactory f) {
			OWLObjectSomeValuesFrom svf = f.getOWLObjectSomeValuesFrom(p, cls);
			activityExpressions = addAnnotation(svf, metadata, activityExpressions);
		}
	}
	
	private boolean isMf(OWLClass cls) {
		return mfSet.contains(cls);
	}
	
	private boolean isBp(OWLClass cls) {
		return bpSet.contains(cls);
	}
	
	private boolean isCc(OWLClass cls) {
		return ccSet.contains(cls);
	}
	
	public void translate(OWLOntology modelAbox, GafDocument annotations, BioentityDocument entities, List<String> additionalRefs) {
		final OWLGraphWrapper modelGraph = new OWLGraphWrapper(modelAbox);
		final OWLDataFactory f = modelGraph.getDataFactory();
		Set<OWLNamedIndividual> individuals = modelAbox.getIndividualsInSignature();
		
		// create initial summaries
		Map<OWLNamedIndividual, Summary> summaries = new HashMap<OWLNamedIndividual, Summary>();
		for (OWLNamedIndividual individual : individuals) {
			Summary summary = new Summary();
			summaries.put(individual, summary);

			Metadata metadata = extractMetadata(individual, modelGraph, additionalRefs);
			
			Set<OWLClassAssertionAxiom> assertionAxioms = modelAbox.getClassAssertionAxioms(individual);
			for (OWLClassAssertionAxiom axiom : assertionAxioms) {
				OWLClassExpression ce = axiom.getClassExpression();
				if (!ce.isAnonymous()) {
					if (!ce.isBottomEntity() && !ce.isOWLNothing() && !ce.isOWLThing() && !ce.isTopEntity()) {
						summary.addMfOrBp(ce.asOWLClass(), metadata);
					}
				}
				else if (ce instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression p = svf.getProperty();
					OWLClassExpression filler = svf.getFiller();
					if (occursIn.equals(p)) {
						summary.addCc(filler, metadata);
					}
					else if (enabledBy.equals(p)) {
						if (filler instanceof OWLClass) {
							summary.entity = (OWLClass) filler;
							summary.entityType = getEntityType(summary.entity, modelGraph);
							summary.entityTaxon = getEntityTaxon(summary.entity, modelGraph);
							// TODO multi-species
						}
					}
					else {
						if (filler instanceof OWLClass) {
							OWLClass cls = (OWLClass) filler;
							summary.addExpression(p, cls, metadata, f);
						}
					}
				}
			}
		}
		
		
		// extract process and other infos
		for(OWLNamedIndividual individual : summaries.keySet()) {
			Summary summary = summaries.get(individual);
			if (summary.entity != null) {
				Set<OWLObjectPropertyAssertionAxiom> axioms = modelAbox.getObjectPropertyAssertionAxioms(individual);
				for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
					Metadata metadata = extractMetadata(axiom.getAnnotations(), modelGraph, additionalRefs);
					OWLIndividual object = axiom.getObject();
					OWLObjectPropertyExpression property = axiom.getProperty();
					if (partOf.equals(property)) {
						Summary objectSummary = summaries.get(object);
						if (objectSummary != null) {
							summary.addProcesses(objectSummary.processes, metadata);
							// only add locations from the process, if there are no ones already present.
							if (summary.locations == null || summary.locations.isEmpty()) {
								summary.addLocations(objectSummary.locations);
							}
						}
					}
					else if (property instanceof OWLObjectProperty) {
						Summary objectSummary = summaries.get(object);
						if (objectSummary != null) {
							// handle as additional information
							if (objectSummary.activities != null) {
								summary.addExpression(property, objectSummary.activities, f);
							}
							else {
								summary.addExpression(property, objectSummary.processes, f);
							}
						}
					}
				}
			}
		}
		
		// report
		for(OWLNamedIndividual individual : summaries.keySet()) {
			Summary summary = summaries.get(individual);
			if (summary.entity != null) {
				addAnnotations(modelGraph, summary, annotations, entities);
			}
		}
	}

	
	private Metadata extractMetadata(OWLNamedIndividual individual, OWLGraphWrapper modelGraph, List<String> additionalRefs) {
		Metadata metadata = new Metadata();
		Set<OWLAnnotationAssertionAxiom> assertionAxioms = modelGraph.getSourceOntology().getAnnotationAssertionAxioms(individual.getIRI());
		for (OWLAnnotationAssertionAxiom axiom : assertionAxioms) {
			OWLAnnotationProperty currentProperty = axiom.getProperty();
			OWLAnnotationValue value = axiom.getValue();
			extractMetadata(currentProperty, value, metadata, modelGraph, additionalRefs);
		}
		if (metadata.sources == null && additionalRefs != null) {
			metadata.sources = new HashSet<String>(additionalRefs);
		}
		return metadata;
	}
	
	private void extractMetadata(OWLAnnotationProperty p, OWLAnnotationValue v, Metadata metadata,
			OWLGraphWrapper modelGraph, List<String> additionalRefs)
	{
		if (this.evidence.equals(p)) {
			if (v instanceof IRI) {
				IRI iri = (IRI) v;
				metadata.evidence = modelGraph.getOWLClass(iri);
			}
			else if (v instanceof OWLLiteral) {
				String literal = ((OWLLiteral) v).getLiteral();
				if (StringUtils.startsWith(literal, OBOUpperVocabulary.OBO)) {
					IRI iri = IRI.create(literal);
					metadata.evidence = modelGraph.getOWLClass(iri);
				}
				else {
					metadata.evidence = modelGraph.getOWLClassByIdentifier(literal);
				}
			}
		}
		else if (this.contributor.equals(p)) {
			if (v instanceof OWLLiteral) {
				metadata.assignedBy = ((OWLLiteral) v).getLiteral();
			}
		}
		else if (this.date.equals(p)) {
			if (v instanceof OWLLiteral) {
				metadata.date = ((OWLLiteral) v).getLiteral();
			}
		}
		else if (this.source.equals(p)) {
			if (v instanceof OWLLiteral) {
				String sourceValue = ((OWLLiteral) v).getLiteral();
				if (metadata.sources == null) {
					metadata.sources = new HashSet<String>();
				}
				metadata.sources.add(sourceValue);
			}
		}
		if (additionalRefs != null) {
			if (metadata.sources == null) {
				metadata.sources = new HashSet<String>();
			}
			metadata.sources.addAll(additionalRefs);
		}
	}
	
	private Metadata extractMetadata(Collection<OWLAnnotation> annotations, OWLGraphWrapper modelGraph, List<String> additionalRefs) {
		Metadata metadata = new Metadata();
		if (annotations != null && !annotations.isEmpty()) {
			for (OWLAnnotation owlAnnotation : annotations) {
				OWLAnnotationProperty currentProperty = owlAnnotation.getProperty();
				OWLAnnotationValue value = owlAnnotation.getValue();
				extractMetadata(currentProperty, value, metadata, modelGraph, additionalRefs);
			}
		}
		if (metadata.sources == null && additionalRefs != null) {
			metadata.sources = new HashSet<String>(additionalRefs);
		}
		return metadata;
	}

	/**
	 * Get the type of an enabled by entity, e.g. gene, protein
	 * 
	 * @param modelGraph 
	 * @param entity 
	 * @return type
	 */
	String getEntityType(OWLClass entity, OWLGraphWrapper modelGraph) {
		String id = modelGraph.getIdentifier(entity);
		if (id.startsWith("UniProtKB")) {
			return "protein"; // TODO
		}
		return "gene";
	}
	
	String getEntityTaxon(OWLClass entity, OWLGraphWrapper modelGraph) {
		return null; // TODO
	}
	
	public Pair<GafDocument, BioentityDocument> translate(String id, OWLOntology modelAbox, List<String> additionalReferences) {
		final GafDocument annotations = new GafDocument(id, null);
		final BioentityDocument entities = new BioentityDocument(id);
		translate(modelAbox, annotations, entities, additionalReferences);
		return Pair.of(annotations, entities);
	}

	private GeneAnnotation createAnnotation(Entry<OWLClass> e, Bioentity entity, String Aspect, OWLGraphWrapper g, Collection<OWLObjectSomeValuesFrom> c16) {
		GeneAnnotation annotation = new GeneAnnotation();
		annotation.setBioentityObject(entity);
		annotation.setBioentity(entity.getId());
		annotation.setAspect(Aspect);
		
		//annotation.setActsOnTaxonId(taxonRelPair) // TODO?
		
		if (e.metadata.assignedBy != null) {
			annotation.setAssignedBy(e.metadata.assignedBy);
		}

		annotation.setCls(g.getIdentifier(e.value));

		if (e.metadata.evidence != null) {
			String ecoId = g.getIdentifier(e.metadata.evidence);
			if (ecoId != null) {
				String goCode = null;
				Pair<String, String> pair = mapper.getGoCode(ecoId);
				if (pair != null) {
					goCode = pair.getLeft();
				}
				annotation.setEvidence(goCode, ecoId);
			}
		}
		annotation.setLastUpdateDate(e.metadata.date);
		//annotation.setRelation(relation); // TODO
		if (e.metadata.sources != null) {
			annotation.addReferenceIds(e.metadata.sources);
		}
		
		if (c16 != null && !c16.isEmpty()) {
			List<ExtensionExpression> expressions = new ArrayList<ExtensionExpression>();
			for (OWLObjectSomeValuesFrom svf : c16) {
				OWLObjectPropertyExpression property = svf.getProperty();
				OWLClassExpression filler = svf.getFiller();
				if (property instanceof OWLObjectProperty && filler instanceof OWLClass) {
					String rel = getRelId(property, g);
					String objectId = g.getIdentifier(filler);
					ExtensionExpression expr = new ExtensionExpression(rel, objectId);
					expressions.add(expr);
				}
			}
			annotation.setExtensionExpressions(Collections.singletonList(expressions));
		}
		
		return annotation;
	}
	
	private String getRelId(OWLObjectPropertyExpression p, OWLGraphWrapper graph) {
		String relId = null;
		for(OWLOntology ont : graph.getAllOntologies()) {
			relId = Owl2Obo.getIdentifierFromObject(p, ont, null);
			if (relId != null && relId.indexOf(':') < 0) {
				return relId;
			}
		}
		return relId;
	}
	
	private Bioentity createBioentity(OWLClass entityCls, String entityType, String taxon, OWLGraphWrapper g) {
		Bioentity bioentity = new Bioentity();
		String id = g.getIdentifier(entityCls);
		bioentity.setId(id);
		String geneLbl = g.getLabel(entityCls);
		String[] split = StringUtils.split(id, ":", 2);
		if (split.length == 2) {
			bioentity.setDb(split[0]);
		}
		bioentity.setSymbol(geneLbl);
		bioentity.setTypeCls(entityType);
		bioentity.setNcbiTaxonId(taxon);
		// TODO more bioentity content
		return bioentity;
	}
	
	private void addAnnotations(OWLGraphWrapper modelGraph,
			Summary summary, 
			GafDocument annotations, BioentityDocument entities) 
	{
		final OWLDataFactory f = modelGraph.getDataFactory();
		Bioentity entity = createBioentity(summary.entity, summary.entityType, summary.entityTaxon , modelGraph);
		entities.addBioentity(entity);
		annotations.addBioentity(entity);
		
		if (summary.activities != null) {
			for (Entry<OWLClass> e: summary.activities) {
				List<OWLObjectSomeValuesFrom> c16 = new ArrayList<OWLObjectSomeValuesFrom>();
				boolean renderActivity = true;
				if (mf.equals(e.value)) {
					// special handling for top level molecular functions
					// only add as annotation, if there is more than one annotation
					// otherwise they tend to be redundant with the bp or cc annotation
					int count = 0;
					if (summary.processes != null) {
						count += summary.processes.size();
					}
					if (summary.locations != null) {
						count += summary.locations.size();
					}
					if (count <= 1 && (summary.activityExpressions == null || summary.activityExpressions.isEmpty())) {
						renderActivity = false;
					}
				}
				if (renderActivity) {
					if (summary.processes != null) {
						for (Entry<OWLClass> processEntry : summary.processes) {
							c16.add(f.getOWLObjectSomeValuesFrom(partOf, processEntry.value));
						}
					}
					if (summary.locations != null) {
						for (Entry<OWLClass> locationEntry : summary.locations) {
							c16.add(f.getOWLObjectSomeValuesFrom(occursIn, locationEntry.value));
						}
					}
					if (summary.activityExpressions != null) {
						for(Entry<OWLObjectSomeValuesFrom> expressionEntry : summary.activityExpressions) {
							c16.add(expressionEntry.value);
						}
					}
					GeneAnnotation annotation = createAnnotation(e, entity, "F", modelGraph, c16);
					annotations.addGeneAnnotation(annotation);
				}
			}
		}
		if (summary.processes != null) {
			for (Entry<OWLClass> e : summary.processes) {
				GeneAnnotation annotation = createAnnotation(e, entity, "P", modelGraph, null);
				annotations.addGeneAnnotation(annotation);
			}
		}
		if (summary.locations != null) {
			for (Entry<OWLClass> e : summary.locations) {
				if (isCc(e.value)) {
					GeneAnnotation annotation = createAnnotation(e, entity, "C", modelGraph, null);
					annotations.addGeneAnnotation(annotation);
				}
			}
		}
	}
}
