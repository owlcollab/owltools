package owltools.gaf.lego.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.obolibrary.obo2owl.Owl2Obo;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
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

abstract class AbstractLegoTranslator {

	protected final OWLObjectProperty partOf;
	protected final OWLObjectProperty occursIn;
	protected final OWLObjectProperty enabledBy;
	
	protected final OWLAnnotationProperty source;
	protected final OWLAnnotationProperty contributor;
	protected final OWLAnnotationProperty date;
	protected final OWLAnnotationProperty evidence;
	
	protected final OWLClass mf;
	protected final Set<OWLClass> mfSet;
	
	protected final OWLClass cc;
	protected final Set<OWLClass> ccSet;
	
	protected final OWLClass bp;
	protected final Set<OWLClass> bpSet;

	protected final SimpleEcoMapper mapper;
	
	protected AbstractLegoTranslator(OWLGraphWrapper graph, OWLReasoner reasoner, SimpleEcoMapper mapper) {
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
	
	protected static Set<OWLClass> getAllSubClasses(OWLClass cls, OWLGraphWrapper g, OWLReasoner r, boolean reflexive) {
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

	
	protected static class Entry<T> {
		T value;
		Metadata metadata;
		// TODO multi-species interactions
	}
	
	protected static class Metadata {
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

	protected class Summary {
		
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
		
		void addMf(OWLClass cls, Metadata metadata) {
			if (isMf(cls)) {
				activities = addAnnotation(cls, metadata , activities);
			}
		}
		
		void addBp(OWLClass cls, Metadata metadata) {
			if (isBp(cls)) {
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
	
	protected boolean isMf(OWLClass cls) {
		return mfSet.contains(cls);
	}
	
	protected boolean isBp(OWLClass cls) {
		return bpSet.contains(cls);
	}
	
	protected boolean isCc(OWLClass cls) {
		return ccSet.contains(cls);
	}
	
	public abstract void translate(OWLOntology modelAbox, GafDocument annotations, BioentityDocument entities, List<String> additionalRefs);
	
	/**
	 * Get the type of an enabled by entity, e.g. gene, protein
	 * 
	 * @param modelGraph 
	 * @param entity 
	 * @return type
	 */
	protected String getEntityType(OWLClass entity, OWLGraphWrapper modelGraph) {
		String id = modelGraph.getIdentifier(entity);
		if (id.startsWith("UniProtKB")) {
			return "protein"; // TODO
		}
		return "gene";
	}
	
	protected String getEntityTaxon(OWLClass entity, OWLNamedIndividual individual, OWLGraphWrapper modelGraph) {
		return null; // TODO
	}
	
	public Pair<GafDocument, BioentityDocument> translate(String id, OWLOntology modelAbox, List<String> additionalReferences) {
		final GafDocument annotations = new GafDocument(id, null);
		final BioentityDocument entities = new BioentityDocument(id);
		translate(modelAbox, annotations, entities, additionalReferences);
		return Pair.of(annotations, entities);
	}

	protected GeneAnnotation createAnnotation(Entry<OWLClass> e, Bioentity entity, String Aspect, OWLGraphWrapper g, Collection<OWLObjectSomeValuesFrom> c16) {
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
	
	protected String getRelId(OWLObjectPropertyExpression p, OWLGraphWrapper graph) {
		String relId = null;
		for(OWLOntology ont : graph.getAllOntologies()) {
			relId = Owl2Obo.getIdentifierFromObject(p, ont, null);
			if (relId != null && relId.indexOf(':') < 0) {
				return relId;
			}
		}
		return relId;
	}
	
	protected Bioentity createBioentity(OWLClass entityCls, String entityType, String taxon, OWLGraphWrapper g) {
		Bioentity bioentity = new Bioentity();
		BioentityStrings strings = getBioentityStrings(entityCls, entityType, taxon, g);
		String id = strings.id;
		bioentity.setId(id);
		if (strings.db != null) {
			bioentity.setDb(strings.db);
		}
		bioentity.setSymbol(strings.symbol);
		bioentity.setTypeCls(strings.type);
		bioentity.setNcbiTaxonId(taxon);
		// TODO more bioentity content
		return bioentity;
	}
	
	protected static class BioentityStrings {
		String id;
		String db;
		String symbol;
		String type;
	}
	
	protected BioentityStrings getBioentityStrings(OWLClass entityCls, String entityType, String taxon, OWLGraphWrapper g) {
		BioentityStrings strings = new BioentityStrings();
		strings.id = g.getIdentifier(entityCls);
		strings.db = null;
		String[] split = StringUtils.split(strings.id, ":", 2);
		if (split.length == 2) {
			strings.db = split[0];
		}
		strings.symbol = g.getLabel(entityCls);
		strings.type = entityType;
		return strings;
	}

	protected void addAnnotations(OWLGraphWrapper modelGraph,
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
