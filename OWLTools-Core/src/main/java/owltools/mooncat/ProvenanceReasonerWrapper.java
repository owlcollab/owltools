package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class ProvenanceReasonerWrapper {
	
	private Logger LOG = Logger.getLogger(ProvenanceReasonerWrapper.class);

	
	OWLOntology ontology;
	OWLReasonerFactory rf;
	Set<OWLEdge> edges ;
	
	class OWLEdge {
		public final OWLClass c;
		public final OWLClass p;
		Set<IRI> requires = new HashSet<IRI>();
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
	}

	public void reason() throws OWLOntologyCreationException {
		OWLReasoner reasoner = rf.createReasoner(ontology);
		edges = new HashSet<OWLEdge>();
		for (OWLClass c : ontology.getClassesInSignature(false)) {
			for (OWLClass p : reasoner.getSuperClasses(c, true).getFlattened()) {
				OWLEdge e = new OWLEdge(c,p);
				edges.add(e);
				LOG.info("Edge: "+e);
			}
		}
		LOG.info("EDGES:"+edges.size());
		reasoner.dispose();

		for (OWLOntology leaveOutOntology : ontology.getImportsClosure()) {
			reasonLeavingOneOut(leaveOutOntology);
		}
		
		OWLOntologyManager m = getManager();
		OWLDataFactory df = m.getOWLDataFactory();
		Set<OWLSubClassOfAxiom> scas = new HashSet<OWLSubClassOfAxiom>();
		OWLAnnotationProperty requiresAP = df.getOWLAnnotationProperty(IRI.create("http://example.org/foo/requires"));
		for (OWLEdge e : edges) {
			LOG.info("FINAL EDGE: "+e+" REQUIRES: "+e.requires);
			Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
			for (IRI r : e.requires) {
				anns.add(df.getOWLAnnotation(requiresAP, r));
			}
			OWLSubClassOfAxiom sca = df.getOWLSubClassOfAxiom(e.c, e.p, anns);
			scas.add(sca);
		}
		
		// todo - make this configurable
		LOG.info("Adding axioms: "+scas.size());
		m.addAxioms(ontology, scas);
	}

	public void reasonLeavingOneOut(OWLOntology leaveOutOntology) throws OWLOntologyCreationException {
		OWLOntologyManager m = getManager();
		OWLDataFactory df = m.getOWLDataFactory();
		OWLOntology ont2 = m.createOntology();
		
		LOG.info("LEAVE ONE OUT: "+leaveOutOntology);
		for (OWLOntology io : ontology.getImportsClosure()) {
			if (io.equals(leaveOutOntology)) {
				LOG.info("SKIPPING:"+io);
				continue;
			}
			m.addAxioms(ont2, io.getAxioms());
			/*
			AddImport ai = 
					new AddImport(ont2,
							df.getOWLImportsDeclaration(io.getOntologyID().getOntologyIRI()));
			m.applyChange(ai);
			*/
			
		}
		
		OWLReasoner reasoner = rf.createReasoner(ont2);
		for (OWLEdge e : edges) {
			//LOG.info("Testing "+e); 

			Set<OWLSubClassOfAxiom> scas = ont2.getSubClassAxiomsForSubClass(e.c);
			Set<OWLSubClassOfAxiom> rmAxioms = new HashSet<OWLSubClassOfAxiom>();
			for (OWLSubClassOfAxiom sca : scas) {
				if (sca.getSuperClass().equals(e.p)) {
					LOG.info("REMOVING: "+sca);
					rmAxioms.add(sca);
				}
			}
			if (rmAxioms.size() > 0) {
				m.removeAxioms(ont2, rmAxioms);
				reasoner.flush();
			}
			if (reasoner.getSuperClasses(e.c, false).containsEntity(e.p)) {
				//LOG.info("Still has: "+e);
			}
			else {
				IRI req = leaveOutOntology.getOntologyID().getOntologyIRI();
				LOG.info(e + " requires "+req);
				e.requires.add(req);
			}
			if (rmAxioms.size() > 0) {
				m.addAxioms(ont2, rmAxioms);
				reasoner.flush();
			}
		}
		reasoner.dispose();
		
	}
	
	

	public Set<OWLEdge> getEdges() {
		return edges;
	}

	public OWLOntologyManager getManager() {
		return ontology.getOWLOntologyManager();
	}
}
