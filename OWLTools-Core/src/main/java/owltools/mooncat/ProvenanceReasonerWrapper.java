package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class ProvenanceReasonerWrapper {
	
	private Logger LOG = Logger.getLogger(ProvenanceReasonerWrapper.class);

	
	final OWLOntology ontology;
	final OWLReasonerFactory rf;
	Set<OWLEdge> edges ;
	public OWLOntology outputOntology;
	String SOURCE_PROPERTY_IRI = "http://purl.org/dc/elements/1.1/source";
	
	class OWLEdge {
		public final OWLClass c;
		public final OWLClass p;
		Set<IRI> requires = new HashSet<IRI>();
		boolean isJustified = true;
		public OWLEdge(OWLClass c, OWLClass p) {
			super();
			this.c = c;
			this.p = p;
		}
		@Override
		public String toString() {
			return "OWLEdge [c=" + c + ", p=" + p + "]";
		}
		
		
	}
	
	
	public ProvenanceReasonerWrapper(OWLOntology ontology, OWLReasonerFactory rf) {
		super();
		this.ontology = ontology;
		this.rf = rf;
		outputOntology = ontology;
	}

	public void reason() throws OWLOntologyCreationException {
		OWLOntologyManager m = getManager();
		OWLDataFactory df = m.getOWLDataFactory();
		OWLAnnotationProperty requiresAP = df.getOWLAnnotationProperty(IRI.create(SOURCE_PROPERTY_IRI));

		OWLReasoner reasoner = rf.createReasoner(ontology);
		edges = new HashSet<OWLEdge>();
		for (OWLClass c : ontology.getClassesInSignature(Imports.EXCLUDED)) {
			for (OWLClass p : reasoner.getSuperClasses(c, true).getFlattened()) {
				
				// only consider edges that could potentially be inferred
				if (ontology.getEquivalentClassesAxioms(p).size() > 0) {
					OWLEdge e = new OWLEdge(c,p);
					edges.add(e);
					LOG.info("Edge: "+e);
				}
			}
		}
		LOG.info("EDGES:"+edges.size());
		for (OWLEdge e : edges) {
			if (!isEdgeEntailed(e, ontology, reasoner)) {
				e.isJustified = false;
			}
		}
		reasoner.dispose();

		for (OWLOntology leaveOutOntology : ontology.getImportsClosure()) {
			reasonLeavingOneOut(leaveOutOntology);
		}
		
		// Annotate the edges
		Set<OWLSubClassOfAxiom> scas = new HashSet<OWLSubClassOfAxiom>();
		for (OWLEdge e : edges) {
			LOG.info("FINAL EDGE: "+e+" REQUIRES: "+e.requires+" JUSTIFIED: "+e.isJustified);
			Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
			for (IRI r : e.requires) {
				anns.add(df.getOWLAnnotation(requiresAP, r));
			}
			OWLSubClassOfAxiom sca = df.getOWLSubClassOfAxiom(e.c, e.p, anns);
			scas.add(sca);
		}
		
		// todo - make this configurable
		LOG.info("Adding axioms: "+scas.size());
		m.addAxioms(outputOntology, scas);
	}

	public void reasonLeavingOneOut(OWLOntology leaveOutOntology) throws OWLOntologyCreationException {
		OWLOntologyManager m = getManager();
		OWLOntology ont2 = m.createOntology();
		
		LOG.info("LEAVE ONE OUT: "+leaveOutOntology);
		for (OWLOntology io : ontology.getImportsClosure()) {
			if (io.equals(leaveOutOntology)) {
				LOG.info("SKIPPING:"+io);
				continue;
			}
			m.addAxioms(ont2, io.getAxioms());
			
		}
		
		OWLReasoner reasoner = rf.createReasoner(ont2);
		for (OWLEdge e : edges) {
			if (!e.isJustified) {
				// there is no point checking unjustified edges;
				// these are edges that when removed cannot be re-inferred using the entire ontology set.
				// as reasoning is monotonic, removing imports cannot bring it back.
				continue;
			}
			//LOG.info("Testing "+e); 
			if (!isEdgeEntailed(e, ont2, reasoner)) {
				IRI req = leaveOutOntology.getOntologyID().getOntologyIRI().orNull();
				LOG.info(e + " requires "+req);
				e.requires.add(req);
				
			}
		}
		reasoner.dispose();
	}
	
	public boolean isEdgeEntailed(OWLEdge e, OWLOntology currentOntology, OWLReasoner reasoner) {
		OWLOntologyManager m = getManager();

		Set<OWLSubClassOfAxiom> scas = currentOntology.getSubClassAxiomsForSubClass(e.c);
		Set<OWLSubClassOfAxiom> rmAxioms = new HashSet<OWLSubClassOfAxiom>();
		for (OWLSubClassOfAxiom sca : scas) {
			if (sca.getSuperClass().equals(e.p)) {
				LOG.info("REMOVING: "+sca);
				rmAxioms.add(sca);
			}
		}
		boolean isEdgeAsserted = rmAxioms.size() > 0;
		if (isEdgeAsserted) {
			m.removeAxioms(currentOntology, rmAxioms);
			reasoner.flush();
		}
		boolean isEntailed;
		isEntailed = reasoner.getSuperClasses(e.c, false).containsEntity(e.p);
		if (isEdgeAsserted) {
			m.addAxioms(currentOntology, rmAxioms);
			reasoner.flush();
		}

		return isEntailed;
	}

	public Set<OWLEdge> getEdges() {
		return edges;
	}

	public OWLOntologyManager getManager() {
		return ontology.getOWLOntologyManager();
	}
}
