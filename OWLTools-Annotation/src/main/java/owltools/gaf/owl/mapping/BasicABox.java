package owltools.gaf.owl.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.owl.GAFOWLBridge.GAFDescription;
import owltools.gaf.owl.GAFOWLBridge.Vocab;
import owltools.graph.OWLGraphWrapper;

public class BasicABox extends GAFOWLBridge {

	Map<String,OWLNamedIndividual> emap = new HashMap<String,OWLNamedIndividual>();

	public BasicABox(OWLGraphWrapper g) {
		super(g);
	}



	public void translateGeneAnnotation(GeneAnnotation a) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDataFactory fac = graph.getDataFactory();
		String e = a.getBioentity();

		List<GAFDescription> descriptions = getDescription(a);
		for (GAFDescription gdesc : descriptions) {
			OWLClassExpression svf = gdesc.classExpression;

			//OWLNamedIndividual i = getBioentityIndividual(e);
			OWLNamedIndividual i = null;
			i = emap.get(e);

			axioms.add(fac.getOWLClassAssertionAxiom(svf, i));
		}
		addAxioms(axioms);
	}

	protected void translateBioentity(Bioentity e) {
		OWLDataFactory fac = graph.getDataFactory();
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		IRI iri = graph.getIRIByIdentifier(e.getId());
		OWLNamedIndividual cls =  graph.getDataFactory().getOWLNamedIndividual(iri);
		emap.put(e.getId(), cls);

		// --label---
		axioms.add(fac.getOWLAnnotationAssertionAxiom(fac.getRDFSLabel(),
				cls.getIRI(),
				fac.getOWLLiteral(e.getSymbol())));

		/*
		// --taxon--
		OWLClass taxCls = getOWLClass(e.getNcbiTaxonId()); // todo - cache
		axioms.add(fac.getOWLSubClassOfAxiom(cls, 
				fac.getOWLObjectSomeValuesFrom(getGeneAnnotationObjectProperty(Vocab.IN_TAXON), 
						taxCls)));

		// TODO - other properties
		 */

		addAxioms(axioms);


	}



}
