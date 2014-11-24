package owltools.gaf.lego.legacy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.BioentityDocument;
import owltools.gaf.GafDocument;
import owltools.gaf.eco.SimpleEcoMapper;
import owltools.graph.OWLGraphWrapper;

import com.google.common.collect.Sets;

public class LegoAllIndividualToGeneAnnotationTranslator extends AbstractLegoTranslator {
	
	private OWLGraphWrapper graph;

	public LegoAllIndividualToGeneAnnotationTranslator(OWLGraphWrapper graph, OWLReasoner reasoner, SimpleEcoMapper mapper) {
		super(graph, reasoner, mapper);
		this.graph = graph;
	}
	
	private boolean isEco(OWLClass cls) {
		String identifier = graph.getIdentifier(cls);
		return identifier != null && identifier.startsWith("ECO:");
	}
	
	private OWLClass getEco(Set<OWLClass> set) {
		for (OWLClass cls : set) {
			if (isEco(cls)) {
				return cls;
			}
		}
		return null;
	}
	
	private boolean isTaxon(OWLClass cls) {
		String identifier = graph.getIdentifier(cls);
		return identifier != null && identifier.startsWith("NCBITaxon:");
	}
	
	private OWLClass getTaxon(Set<OWLClass> set) {
		for (OWLClass cls : set) {
			if (isTaxon(cls)) {
				return cls;
			}
		}
		return null;
	}
	
	private Set<OWLClass> getTypes(OWLNamedIndividual i, OWLOntology modelAbox) {
		Set<OWLClassAssertionAxiom> axioms = modelAbox.getClassAssertionAxioms(i);
		Set<OWLClass> types = new HashSet<OWLClass>();
		for (OWLClassAssertionAxiom axiom : axioms) {
			OWLClassExpression ce = axiom.getClassExpression();
			if (ce instanceof OWLClass) {
				OWLClass cls = ce.asOWLClass();
				if (cls.isBuiltIn() == false) {
					types.add(cls);
				}
			}
		}
		return types;
	}
	
	private Metadata mergeMetadata(Metadata...data) {
		Metadata result = null;
		for (Metadata metadata : data) {
			if (metadata != null) {
				if (result == null) {
					result = metadata.copy();
				}
				else {
					if (result.evidence == null && metadata.evidence != null) {
						Metadata oldResult = result;
						result = metadata.copy();
						if (oldResult.sources != null && !oldResult.sources.isEmpty()) {
							if (result.sources == null) {
								result.sources = Sets.newHashSet(oldResult.sources);
							}
							else {
								result.sources.addAll(oldResult.sources);
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	public void translate(OWLOntology modelAbox, GafDocument annotations, BioentityDocument entities, List<String> additionalRefs) {
		final OWLGraphWrapper modelGraph = new OWLGraphWrapper(modelAbox);
		final OWLDataFactory f = modelGraph.getDataFactory();
		
		final Set<OWLNamedIndividual> annotationIndividuals = new HashSet<OWLNamedIndividual>();
		final Map<IRI, Metadata> evidenceIndividuals = new HashMap<IRI, Metadata>();
		final Map<OWLNamedIndividual, OWLClass> taxonIndividuals = new HashMap<OWLNamedIndividual, OWLClass>();
		
		for(OWLNamedIndividual individual : modelAbox.getIndividualsInSignature()) {
			Set<OWLClass> individualTypes = getTypes(individual, modelAbox);
			OWLClass eco = getEco(individualTypes);
			OWLClass taxon = getTaxon(individualTypes);
			if (eco != null) {
				// is eco
				Metadata metadata = extractMetadata(individual, modelGraph, null);
				metadata.evidence = eco;
				evidenceIndividuals.put(individual.getIRI(), metadata);
			}
			else if(taxon != null) {
				// is taxon (in future, currently not used)
				taxonIndividuals.put(individual, taxon);
			}
			else {
				// assume annotation
				annotationIndividuals.add(individual);
			}
		}
		
		final Map<OWLNamedIndividual,Metadata> allMetadata = new HashMap<OWLNamedIndividual, Metadata>();
		for(OWLNamedIndividual individual : annotationIndividuals) {
			Metadata metadata = extractMetadata(individual, modelGraph, evidenceIndividuals);
			allMetadata.put(individual, metadata);
		}
		
		Set<Summary> summaries = new HashSet<Summary>();
		for (OWLObjectPropertyAssertionAxiom axiom : modelAbox.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			final OWLObjectPropertyExpression p = axiom.getProperty();
			if (enabledBy.equals(p)) {
				
				// gene/protein/complex
				final OWLNamedIndividual object = axiom.getObject().asOWLNamedIndividual();
				Set<OWLClass> objectTypes = getTypes(object, modelAbox);
				for (OWLClass objectType : objectTypes) {
					final Summary summary = new Summary();
					summary.entity = objectType;
					summary.entityTaxon = getEntityTaxon(objectType, object, modelGraph);
					summary.entityType = getEntityType(objectType, modelGraph);
					summaries.add(summary);

					final OWLNamedIndividual subject = axiom.getSubject().asOWLNamedIndividual();
					
					// get associated meta data
					final Metadata linkMetadata = extractMetadata(axiom.getAnnotations(), modelGraph, evidenceIndividuals);
					final Metadata objectMetadata = allMetadata.get(object);
					final Metadata subjectMetadata = allMetadata.get(subject);
					final Metadata mfMetadata = mergeMetadata(linkMetadata, objectMetadata, subjectMetadata);
					
					// handle types
					for(OWLClass cls : getTypes(subject, modelAbox)) {
						if (isMf(cls)) {
							summary.addMf(cls, mfMetadata);
						}
						else {
							// TODO how do we record that in a GAF?
						}
					}

					// get all OWLObjectPropertyAssertionAxiom for subject
					Set<OWLObjectPropertyAssertionAxiom> subjectAxioms = modelAbox.getObjectPropertyAssertionAxioms(subject);
					for(OWLObjectPropertyAssertionAxiom current : subjectAxioms) {
						final Metadata currentLinkMetadata = extractMetadata(current.getAnnotations(), modelGraph, evidenceIndividuals);
						final OWLObjectPropertyExpression currentP = current.getProperty();
						final OWLNamedIndividual currentObj = current.getObject().asOWLNamedIndividual();
						final Metadata currentObjMetadata = allMetadata.get(currentObj);
						if (occursIn.equals(currentP)) {
							// check for cc for subject (occurs in)
							final Metadata metadata = mergeMetadata(currentObjMetadata, currentLinkMetadata);
							for(OWLClass cls : getTypes(currentObj, modelAbox)) {
								if (isCc(cls)) {
									summary.addCc(cls, metadata);
								}
								else {
									summary.addExpression(occursIn, cls, metadata, f);
								}
							}
						}
						else if (partOf.equals(currentP)) {
							// check for bp for subject (part_of)
							final Metadata metadata = mergeMetadata(currentObjMetadata, currentLinkMetadata);
							for(OWLClass cls : getTypes(currentObj, modelAbox)) {
								if (isBp(cls)) {
									summary.addBp(cls, metadata);
								}
								else {
									summary.addExpression(currentP, cls, metadata, f);
								}
							}
							
						}else if (enabledBy.equals(currentP)) {
							// do nothing
						}
//						else if (taxon.equals(currentP)) {
//							// do nothing
//						}
						else {
							Set<OWLClass> types = getTypes(currentObj, modelAbox);
							final Metadata miscMetadata = mergeMetadata(currentObjMetadata, currentLinkMetadata);
							for (OWLClass cls : types) {
								summary.addExpression(currentP, cls, miscMetadata, f);	
							}
						}
					}
				}
			}
		}
		
		for(Summary summary : summaries) {
			if (summary.entity != null) {
				addAnnotations(modelGraph, summary, annotations, entities);
			}
		}
	}

	
	private Metadata extractMetadata(OWLNamedIndividual individual, OWLGraphWrapper modelGraph, Map<IRI, Metadata> allEvidences) {
		Metadata metadata = new Metadata();
		Set<OWLAnnotationAssertionAxiom> assertionAxioms = modelGraph.getSourceOntology().getAnnotationAssertionAxioms(individual.getIRI());
		for (OWLAnnotationAssertionAxiom axiom : assertionAxioms) {
			OWLAnnotationProperty currentProperty = axiom.getProperty();
			OWLAnnotationValue value = axiom.getValue();
			extractMetadata(currentProperty, value, metadata, allEvidences);
		}
		return metadata;
	}
	
	private void extractMetadata(OWLAnnotationProperty p, OWLAnnotationValue v, final Metadata metadata, Map<IRI, Metadata> allEvidences)
	{
		if (this.evidence.equals(p) && allEvidences != null && metadata.evidence == null) {
			Metadata evidenceMetadata = allEvidences.get(v);
			if (evidenceMetadata != null) {
				metadata.evidence = evidenceMetadata.evidence;
				metadata.sources = evidenceMetadata.sources;
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
				String source = ((OWLLiteral) v).getLiteral();
				if (metadata.sources == null) {
					metadata.sources = new HashSet<String>();
				}
				metadata.sources.add(source);
			}
		}
		
	}
	
	private Metadata extractMetadata(Collection<OWLAnnotation> annotations, OWLGraphWrapper modelGraph, Map<IRI, Metadata> allEvidences) {
		Metadata metadata = new Metadata();
		if (annotations != null && !annotations.isEmpty()) {
			for (OWLAnnotation owlAnnotation : annotations) {
				OWLAnnotationProperty currentProperty = owlAnnotation.getProperty();
				OWLAnnotationValue value = owlAnnotation.getValue();
				extractMetadata(currentProperty, value, metadata, allEvidences);
			}
		}
		return metadata;
	}

}
