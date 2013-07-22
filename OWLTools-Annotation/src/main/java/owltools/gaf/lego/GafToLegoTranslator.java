package owltools.gaf.lego;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * <pre>
Given a set of gene associations, this procedure will generate LEGO individuals.

The set of associations can be specified by a user query. Includes:

 * grepping a GAF and feeding results
 * selecting all associations for all genes that are involved with some process

# STEP 0 - map GeneAssociation in GAF model

GeneAssociation(
 bioentity:<G>
 class:<C>
 ext:<EXT>
 reference:<Ref>
 evidence:<Ev> # TODO
)

==>

# STEP 1 - calculate class expression

IF <EXT>
 THEN let <CE> = IntersectionOf(<C> <EXT>)
 ELSE let <CE> = <C>

# STEP 2 - map to protein ID

IF <G>.IRI startsWith "uniProtKB"
 THEN let <Pr> = <G>
 ELSE let <Pr> = SELECT <Pr> WHERE <Pr> SubClassOf encoded_by some <G>  ### requires Omeo

# STEP 3 - create instance:

IF <C> SubClassOf MF THEN:

 NamedIndividual( <generateId>
   Types: 
     <CE>,
     enabled_by SOME <Pr>
   Facts:
     source <Ref>

ELSE IF <C> SubClassOf CC THEN:

 NamedIndividual( <generateId>
   Types: 
     'molecular_function', occurs_in some <CE> 
     enabled_by SOME <Pr>
   Facts:
     source <Ref>

ELSE IF <C> SubClassOf BP THEN:

  # note we create two individuals here

 NamedIndividual( <generateId-X>
   Types: 
     <CE>
   Facts:
     source <Ref>


 NamedIndividual( <generateId>
   Types: 
     'molecular_function'
     enabled_by SOME <Pr>
  Facts:
    part_of <generatedId>,
    source <ref>


# VARIANT OF ABOVE STEP

(optional)

keep a map of Refs -> generated Ids 

when performing <generateId>, first check map. If an individual Id has already been generated for this <Ref>, then re-use the existing id from the map.

Note this may result in multiple classification of individuals (MCI). The user can rectify these in Protege.

One variant of this strategy may be to retain the original Id,
generate new Ids for the collapsed aggregate MF individual, and
include evidence links back to the atomic MF individuals.

</pre>

@see <a href="http://viewvc.geneontology.org/viewvc/GO-SVN/trunk/experimental/lego/docs/lego-from-gaf.txt">lego-from-gaf.txt</a>
 */
public class GafToLegoTranslator {
	
	private static Logger logger = Logger.getLogger(GafToLegoTranslator.class);
	
	private final OWLGraphWrapper graph;
	private final Map<String, String> gp2protein;
	private OWLObjectProperty partOf;
	private OWLObjectProperty occursIn;
	private OWLClass mf;
	private OWLObjectProperty enabledBy;

	private Map<String, OWLObject> allOWLObjectsByAltId;

	public GafToLegoTranslator(OWLGraphWrapper graph, Map<String,String> gp2protein) {
		this.graph = graph;
		allOWLObjectsByAltId = graph.getAllOWLObjectsByAltId();
		this.gp2protein = gp2protein;
		partOf = graph.getOWLObjectPropertyByIdentifier("part_of");
		if (partOf == null) {
			throw new RuntimeException("Could not find part_of relation.");
		}
		occursIn = graph.getOWLObjectPropertyByIdentifier("occurs_in");
		if (occursIn == null) {
			throw new RuntimeException("Could not find occurs_in relation.");
		}
		mf = graph.getOWLClassByIdentifier("GO:0003674"); // molecular_function
		if (mf == null) {
			throw new RuntimeException("Could not find molecular_function class.");
		}
		// the graph wrapper has a problem retrieving the property.
//		enabledBy = graph.getOWLObjectPropertyByIdentifier("enabled_by");
		
		// do it via the IRI instead!
		IRI enabledByIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"enabled_by");
		enabledBy = graph.getOWLObjectProperty(enabledByIRI);
		if (enabledBy == null) {
			throw new RuntimeException("Could not find enabled_by relation.");
		}
	}
	
	protected void reportError(String error, GeneAnnotation annotation) {
		logger.error(error+" \t Annotation: "+annotation.toString());
	}
	
	protected void reportWarn(String warning, GeneAnnotation annotation) {
		logger.warn(warning+" \t Annotation: "+annotation.toString());
	}
	
	/**
	 * Translate the given {@link GafDocument} into an OWL representation of the LEGO model.
	 * 
	 * @param gaf
	 * @return lego ontology
	 */
	public OWLOntology translate(GafDocument gaf) {
		return translate(gaf.getGeneAnnotations());
	}
	
	/**
	 * Translate the given annotations ({@link GeneAnnotation}) into an OWL representation of the LEGO model.
	 * 
	 * @param annotations
	 * @return lego ontology
	 */
	public OWLOntology translate(Collection<GeneAnnotation> annotations) {
		final OWLDataFactory f = graph.getDataFactory();
		final OWLOntologyManager m = graph.getManager();
		try {
			OWLOntology lego = m.createOntology(IRI.generateDocumentIRI());
			// add all ontologies from the graph wrapper as import to the new ontology
			Set<OWLOntology> allOntologies = graph.getAllOntologies();
			for (OWLOntology importOntology : allOntologies) {
				IRI importIRI = importOntology.getOntologyID().getOntologyIRI();
				OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(importIRI);
				m.applyChange(new AddImport(lego, importDeclaration));
			}
			
			for(GeneAnnotation annotation : annotations) {
				/*
				 * GeneAssociation(
				 * bioentity:<G>
				 * class:<C>
				 * ext:<EXT>
				 * reference:<Ref>
				 * evidence:<Ev> # TODO
				* )
				*/
				final String annotationClsString = annotation.getCls();
				final OWLClass c = getOwlClass(annotationClsString);
				if (c == null) {
					reportError("Could not find a class for the given identifier: "+annotationClsString, annotation);
					continue;
				}
				
				// STEP 1 - calculate class expression
				//
				// IF <EXT>
				// THEN let <CE> = IntersectionOf(<C> <EXT>)
				// ELSE let <CE> = <C>

				final OWLClassExpression ce;
				Collection<ExtensionExpression> extensionExpressions = annotation.getExtensionExpressions();
				if (extensionExpressions != null && !extensionExpressions.isEmpty()) {
					Set<OWLClassExpression> operands = new HashSet<OWLClassExpression>();
					for(ExtensionExpression extension : extensionExpressions) {
						final String extensionClsString = extension.getCls();
						final String extensionRelationString = extension.getRelation();
						OWLClass extensionCls = getOwlClass(extensionClsString);
						if (extensionCls == null && extensionClsString.startsWith("UniProtKB:")) {
							IRI iri = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"pr/"+extensionClsString.substring(10));
							extensionCls = f.getOWLClass(iri);
						}
						if (extensionCls == null) {
							reportError("Could not find a class for the given extension cls identifier: "+extensionClsString, annotation);
							continue;
						}
						final OWLObjectProperty extensionRelation = graph.getOWLObjectPropertyByIdentifier(extensionRelationString);
						if (extensionRelation == null) {
							reportError("Could not find a class for the given extension relation identifier: "+extensionRelationString, annotation);
							continue;
						}
						operands.add(f.getOWLObjectSomeValuesFrom(extensionRelation, extensionCls));
					}
//					if (operands.size() != extensionExpressions.size()) {
//						reportError("Problems during the translation of the annotation extensions.", annotation);
//						continue;
//					}
					operands.add(c);
					ce = f.getOWLObjectIntersectionOf(operands);
				}
				else {
					ce = c;
				}
				
				// # STEP 2 - map to protein ID
				Bioentity bioentity = annotation.getBioentityObject();
				String dbprefix = bioentity.getDb();
				
				// TODO use ISO form information
				// IF <G>.IRI startsWith "uniProtKB"
				//  THEN let <Pr> = <G>
				OWLClass pr;
				if (dbprefix.equalsIgnoreCase("uniProtKB")) {
					 pr = f.getOWLClass(IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"pr/"+bioentity.getDBID()));
					 addBioEntity(pr, lego, bioentity);
				}
				else {
					// TODO use gp2protein
					reportWarn("Skipping non-uniProtKB bioentity: "+bioentity.getId(), annotation);
					continue;
				}
				
				// # STEP 3 - create instance:
				
				// use Aspect to switch between the three options: P == BP, C == CC, F = MF
				String aspect = annotation.getAspect();
				if (aspect == null) {
					reportError("Error, no aspect defined.", annotation);
					continue;
				}
				
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				String source = annotation.getReferenceId();
				// IF <C> SubClassOf MF THEN:
				if ("F".equals(aspect)) {
					//  NamedIndividual( <generateId>
					//   Types: 
					//	     <CE>,
					//	     enabled_by SOME <Pr>
					//	   Facts:
					//	     source <Ref>
					
					// create individual
					OWLNamedIndividual individual = f.getOWLNamedIndividual(generateNewIRI(lego, "mf"));
					axioms.add(f.getOWLDeclarationAxiom(individual));
					
					// facts
					OWLAnnotationProperty dcsource = getDcSourceProperty(lego, f);
					axioms.add(f.getOWLAnnotationAssertionAxiom(dcsource, individual.getIRI(), f.getOWLLiteral(source)));
					
					// types
					axioms.add(f.getOWLClassAssertionAxiom(ce, individual));
					axioms.add(f.getOWLClassAssertionAxiom(f.getOWLObjectSomeValuesFrom(enabledBy, pr), individual));
				}				
				// ELSE IF <C> SubClassOf CC THEN:
				else if ("C".equals(aspect)) {
					// NamedIndividual( <generateId>
					//   Types: 
					//     'molecular_function', occurs_in some <CE> 
					//     enabled_by SOME <Pr>
					//   Facts:
					//     source <Ref>
					OWLNamedIndividual individual = f.getOWLNamedIndividual(generateNewIRI(lego, "cc"));
					axioms.add(f.getOWLDeclarationAxiom(individual));
					
					// facts
					OWLAnnotationProperty dcsource = getDcSourceProperty(lego, f);
					axioms.add(f.getOWLAnnotationAssertionAxiom(dcsource, individual.getIRI(), f.getOWLLiteral(source)));
					
					// types
					axioms.add(f.getOWLClassAssertionAxiom(mf, individual));
					axioms.add(f.getOWLClassAssertionAxiom(f.getOWLObjectSomeValuesFrom(occursIn, ce), individual));
					axioms.add(f.getOWLClassAssertionAxiom(f.getOWLObjectSomeValuesFrom(enabledBy, pr), individual));
					
				}
				//ELSE IF <C> SubClassOf BP THEN:
				else if ("P".equals(aspect)) {
					//  # note we create two individuals here
	
					// NamedIndividual( <generateId-X>
					//   Types: 
					//     <CE>
					//   Facts:
					//     source <Ref>
					// create individual
					OWLNamedIndividual individualX = f.getOWLNamedIndividual(generateNewIRI(lego, "bp"));
					axioms.add(f.getOWLDeclarationAxiom(individualX));
					
					// facts
					OWLAnnotationProperty dcsource = getDcSourceProperty(lego, f);
					axioms.add(f.getOWLAnnotationAssertionAxiom(dcsource, individualX.getIRI(), f.getOWLLiteral(source)));
					
					// types
					axioms.add(f.getOWLClassAssertionAxiom(ce, individualX));
					
	
					// NamedIndividual( <generateId>
					//   Types: 
					//     'molecular_function'
					//     enabled_by SOME <Pr>
					//  Facts:
					//    part_of <generatedId-X>,
					//    source <ref>
					OWLNamedIndividual individual = f.getOWLNamedIndividual(generateNewIRI(lego, "bp"));
					axioms.add(f.getOWLDeclarationAxiom(individual));
					
					// facts
					axioms.add(f.getOWLAnnotationAssertionAxiom(dcsource, individual.getIRI(), f.getOWLLiteral(source)));
					axioms.add(f.getOWLObjectPropertyAssertionAxiom(partOf, individual, individualX));
					
					// types
					axioms.add(f.getOWLClassAssertionAxiom(mf, individual));
					axioms.add(f.getOWLClassAssertionAxiom(f.getOWLObjectSomeValuesFrom(enabledBy, pr), individual));
					
				}
				else {
					reportError("Error, unknown aspect: "+aspect, annotation);
					continue;
				}
				m.addAxioms(lego, axioms);
			}
			
			return lego;
		}
		catch (OWLException e) {
			throw new RuntimeException("Could not create lego model.", e);
		}
	}

	/**
	 * @param id
	 * @return cls or null
	 */
	private OWLClass getOwlClass(String id) {
		OWLClass cls = graph.getOWLClassByIdentifier(id);
		if (cls == null) {
			// check alt ids
			OWLObject owlObject = allOWLObjectsByAltId.get(id);
			if (owlObject != null && owlObject instanceof OWLClass) {
				cls = (OWLClass) owlObject;
			}
		}
		return cls;
	}
	
	private void addBioEntity(OWLClass pr, OWLOntology lego, Bioentity bioentity) {
		Set<OWLDeclarationAxiom> declarationAxioms = lego.getDeclarationAxioms(pr);
		if (declarationAxioms == null || declarationAxioms.isEmpty()) {
			// add class declaration and add label
			OWLOntologyManager m = lego.getOWLOntologyManager();
			OWLDataFactory f = m.getOWLDataFactory();
			
			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			axioms.add(f.getOWLDeclarationAxiom(pr));
			
			String label = bioentity.getSymbol()+" - "+bioentity.getFullName();
			
			axioms.add(f.getOWLAnnotationAssertionAxiom(f.getRDFSLabel(), pr.getIRI(), f.getOWLLiteral(label)));
			
			m.addAxioms(lego, axioms);
		}
	}

	private static final IRI DC_SOURCE = IRI.create("http://purl.org/dc/terms/source");
	
	private OWLAnnotationProperty getDcSourceProperty(OWLOntology lego, OWLDataFactory f) {
		OWLAnnotationProperty p = f.getOWLAnnotationProperty(DC_SOURCE);
		Set<OWLDeclarationAxiom> declarationAxioms = lego.getDeclarationAxioms(p);
		if (declarationAxioms == null || declarationAxioms.isEmpty()) {
			OWLOntologyManager m = lego.getOWLOntologyManager();
			m.addAxiom(lego, f.getOWLDeclarationAxiom(p));
		}
		return p;
	}

	private IRI generateNewIRI(OWLOntology ont, String type) {
		return IRI.create("http://geneontology.org/lego/"+type+"/"+UUID.randomUUID().toString());
	}
	
	/**
	 * Translate the given {@link GafDocument} into an OWL representation of the LEGO model.
	 * Additionally minimize the lego model and imports into one ontology module.
	 * 
	 * @param gaf
	 * @return minimized lego ontology
	 */
	public OWLOntology minimizedTranslate(GafDocument gaf) {
		OWLOntology all = translate(gaf);
		final OWLOntologyManager m = all.getOWLOntologyManager();
		
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(m, all, ModuleType.BOT);
		Set<OWLEntity> sig = new HashSet<OWLEntity>(all.getIndividualsInSignature());
		Set<OWLAxiom> moduleAxioms = sme.extract(sig);
		
		try {
			OWLOntology module = m.createOntology(IRI.generateDocumentIRI());
			m.addAxioms(module, moduleAxioms);
			return module;
		} catch (OWLException e) {
			throw new RuntimeException("Could not create minimized lego model.", e);
		}
	}
}
