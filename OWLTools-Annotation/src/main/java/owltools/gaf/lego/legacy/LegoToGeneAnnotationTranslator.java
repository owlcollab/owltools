package owltools.gaf.lego.legacy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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

import owltools.gaf.BioentityDocument;
import owltools.gaf.GafDocument;
import owltools.gaf.eco.SimpleEcoMapper;
import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;

public class LegoToGeneAnnotationTranslator extends AbstractLegoTranslator {

	public LegoToGeneAnnotationTranslator(OWLGraphWrapper graph, OWLReasoner reasoner, SimpleEcoMapper mapper) {
		super(graph, reasoner, mapper);
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
							summary.entityTaxon = getEntityTaxon(summary.entity, individual, modelGraph);
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

}
