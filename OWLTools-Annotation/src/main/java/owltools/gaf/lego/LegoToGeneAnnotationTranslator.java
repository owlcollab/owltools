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
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.Bioentity;
import owltools.gaf.BioentityDocument;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;

public class LegoToGeneAnnotationTranslator {

	private final OWLObjectProperty partOf;
	private final OWLObjectProperty occursIn;
	private final OWLObjectProperty enabledBy;
	
	private final OWLClass mf;
	private final Set<OWLClass> mfDescendants;
	
	private final OWLClass cc;
	private final Set<OWLClass> ccDescendants;
	
	private final OWLClass bp;
	private final Set<OWLClass> bpDescendants;
	
	public LegoToGeneAnnotationTranslator(OWLGraphWrapper graph, OWLReasonerFactory reasonerFactory) {
		OWLDataFactory df = graph.getDataFactory();
		partOf = OBOUpperVocabulary.BFO_part_of.getObjectProperty(df);
		occursIn = OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(df);
		
		mf = OBOUpperVocabulary.GO_molecular_function.getOWLClass(df);
		cc = graph.getOWLClassByIdentifier("GO:0005575");
		bp = OBOUpperVocabulary.GO_biological_process.getOWLClass(df);
		
		enabledBy = OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(df);
		
		OWLReasoner r = null;
		try {
			r = reasonerFactory.createReasoner(graph.getSourceOntology());
			bpDescendants = getAllSubClasses(bp, graph, r);
			mfDescendants = getAllSubClasses(mf, graph, r);
			ccDescendants = getAllSubClasses(cc, graph, r);
		}
		finally {
			if (r != null) {
				r.dispose();
			}
		}
	}
	
	private Set<OWLClass> getAllSubClasses(OWLClass cls, OWLGraphWrapper g, OWLReasoner r) {
		Set<OWLClass> allSubClasses = r.getSubClasses(cls, false).getFlattened();
		Iterator<OWLClass> it = allSubClasses.iterator();
		while (it.hasNext()) {
			OWLClass current = it.next();
			if (current.isBuiltIn()) {
				it.remove();
			}
		}
		return allSubClasses;
	}

	
	private static class Entry<T> {
		T value;
		String evidence;	// TODO eco class + GO code
		String assignedBy;		// TODO multivalue?
		List<String> sources; // TODO
		// TODO multi-species interactions
	}
	
	private class Summary {
		
		OWLNamedIndividual individual;
		Set<Entry<OWLClass>> activities = null;
		Set<Entry<OWLObjectSomeValuesFrom>> activityExpressions = null;
		Set<Entry<OWLClass>> locations = null;
		Set<Entry<OWLClass>> processes = null;
		OWLClass geneProduct = null;
		String taxon = null;
		
		void addMfOrBp(OWLClass cls, String evidence, String assignedBy) {
			if (isMf(cls)) {
				activities = addAnnotation(cls, evidence, assignedBy, activities);
			}
			else if (isBp(cls)) {
				processes = addAnnotation(cls, evidence, assignedBy, processes);
			}
		}
		
		void addCc(OWLClassExpression ce, String evidence, String assignedBy) {
			if (ce instanceof OWLClass) {
				OWLClass cls = ce.asOWLClass();
				if (isCc(cls)) {
					locations = addAnnotation(cls, evidence, assignedBy, locations);
				}
			}
		}
		
		private <T> Set<Entry<T>> addAnnotation(T cls, String evidence, String assignedBy, Set<Entry<T>> set) {
			if (set == null) {
				set = new HashSet<Entry<T>>();
			}
			Entry<T> entry = new Entry<T>();
			entry.value = cls;
			entry.evidence = evidence;
			entry.assignedBy = assignedBy;
			set.add(entry);
			return set;
		}

		void addProcesses(Set<Entry<OWLClass>> processes) {
			if (processes != null) {
				if (this.processes == null) {
					this.processes = new HashSet<Entry<OWLClass>>(processes);
				}
				else {
					this.processes.addAll(processes);
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
					activityExpressions = addAnnotation(svf, entry.evidence, entry.assignedBy, activityExpressions);
				}
			}
		}
	}
	
	private boolean isMf(OWLClass cls) {
		return mfDescendants.contains(cls);
	}
	
	private boolean isBp(OWLClass cls) {
		return bpDescendants.contains(cls);
	}
	
	private boolean isCc(OWLClass cls) {
		return ccDescendants.contains(cls);
	}
	
	public Pair<GafDocument, BioentityDocument> translate(String id, LegoModelGenerator model) {
		final GafDocument annotations = new GafDocument(id, null);
		final BioentityDocument entities = new BioentityDocument(id, null);
		final OWLOntology abox = model.getAboxOntology();
		final OWLGraphWrapper modelGraph = new OWLGraphWrapper(abox);
		final OWLDataFactory f = modelGraph.getDataFactory();
		Set<OWLNamedIndividual> individuals = abox.getIndividualsInSignature();
		
		// create initial summaries
		Map<OWLNamedIndividual, Summary> summaries = new HashMap<OWLNamedIndividual, Summary>();
		for (OWLNamedIndividual individual : individuals) {
			Summary summary = new Summary();
			summary.individual = individual;
			summaries.put(individual, summary);
			
			Set<OWLClassAssertionAxiom> assertionAxioms = abox.getClassAssertionAxioms(individual);
			for (OWLClassAssertionAxiom axiom : assertionAxioms) {
				OWLClassExpression ce = axiom.getClassExpression();
				if (!ce.isAnonymous()) {
					if (!ce.isBottomEntity() && !ce.isOWLNothing() && !ce.isOWLThing() && !ce.isTopEntity()) {
						summary.addMfOrBp(ce.asOWLClass(), null, null);
					}
				}
				else if (ce instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression p = svf.getProperty();
					OWLClassExpression filler = svf.getFiller();
					if (occursIn.equals(p)) {
						summary.addCc(filler, null, null);
					}
					else if (enabledBy.equals(p)) {
						if (filler instanceof OWLClass) {
							summary.geneProduct = (OWLClass) filler;
							summary.taxon = null;
							// TODO taxon, multi-species interaction taxon?
						}
					}
				}
			}
		}
		
		
		// extract process and other infos
		for(OWLNamedIndividual individual : summaries.keySet()) {
			Summary summary = summaries.get(individual);
			if (summary.geneProduct != null) {
				Set<OWLObjectPropertyAssertionAxiom> axioms = abox.getObjectPropertyAssertionAxioms(individual);
				for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
					OWLIndividual object = axiom.getObject();
					OWLObjectPropertyExpression property = axiom.getProperty();
					if (partOf.equals(property)) {
						Summary objectSummary = summaries.get(object);
						if (objectSummary != null) {
							summary.addProcesses(objectSummary.processes);
							summary.addLocations(objectSummary.locations);
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
			if (summary.geneProduct != null) {
				addAnnotations(modelGraph, summary, annotations, entities);
			}
		}
		
		return Pair.of(annotations, entities);
	}

	private GeneAnnotation createAnnotation(Entry<OWLClass> e, Bioentity entity, String Aspect, OWLGraphWrapper g, Collection<OWLObjectSomeValuesFrom> c16) {
		GeneAnnotation annotation = new GeneAnnotation();
		annotation.setBioentityObject(entity);
		annotation.setBioentity(entity.getId());
		annotation.setAspect(Aspect);
		
		//annotation.setActsOnTaxonId(taxonRelPair) // TODO?
		
		if (e.assignedBy != null) {
			annotation.setAssignedBy(e.assignedBy);
		}

		annotation.setCls(g.getIdentifier(e.value));

		if (e.evidence != null) {
			annotation.setEvidence(e.evidence, null); // TODO
		}
		
		//annotation.setLastUpdateDate(date); // TODO
		//annotation.setRelation(relation); // TODO
		if (e.sources != null) {
			annotation.addReferenceIds(e.sources);
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
	
	private Bioentity createBioentity(OWLClass geneProduct, String taxon, OWLGraphWrapper g) {
		Bioentity entity = new Bioentity();
		String id = g.getIdentifier(geneProduct);
		entity.setId(id);
		String geneLbl = g.getLabel(geneProduct);
		String[] split = StringUtils.split(id, ":", 2);
		if (split.length == 2) {
			entity.setDb(split[0]);
		}
		entity.setSymbol(geneLbl);
		entity.setTypeCls("protein");
		entity.setNcbiTaxonId(taxon);
		// TODO more bioentity content
		return entity;
	}
	
	private void addAnnotations(OWLGraphWrapper modelGraph,
			Summary summary, 
			GafDocument annotations, BioentityDocument entities) 
	{
		final OWLDataFactory f = modelGraph.getDataFactory();
		String taxon = summary.taxon;
		Bioentity entity = createBioentity(summary.geneProduct, taxon , modelGraph);
		entities.addBioentity(entity);
		annotations.addBioentity(entity);
		
		if (summary.activities != null) {
			for (Entry<OWLClass> e: summary.activities) {
				List<OWLObjectSomeValuesFrom> c16 = new ArrayList<OWLObjectSomeValuesFrom>();
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
		if (summary.processes != null) {
			for (Entry<OWLClass> e : summary.processes) {
				GeneAnnotation annotation = createAnnotation(e, entity, "P", modelGraph, null);
				annotations.addGeneAnnotation(annotation);
			}
		}
		if (summary.locations != null) {
			for (Entry<OWLClass> e : summary.locations) {
				GeneAnnotation annotation = createAnnotation(e, entity, "C", modelGraph, null);
				annotations.addGeneAnnotation(annotation);
			}
		}
	}
}
