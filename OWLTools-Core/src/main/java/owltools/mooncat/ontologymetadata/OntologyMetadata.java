package owltools.mooncat.ontologymetadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

public class OntologyMetadata {
	
	String ontologyIRI;
	String versionIRI;
	Set<OntologyAnnotation> annotations;
	
	int classCount;
	int namedIndividualCount;
	int axiomCount;
	
	boolean isRoot = false;
	
	Set<String> importDirectives;

	public OntologyMetadata(OWLOntology ont) {
		super();
		OWLOntologyID id = ont.getOntologyID();
		if (id.getOntologyIRI() != null)
			ontologyIRI = id.getOntologyIRI().toString();
		if (id.getVersionIRI() != null)
			versionIRI = id.getVersionIRI().toString();
		importDirectives = new HashSet<String>();
		for (OWLImportsDeclaration oid : ont.getImportsDeclarations()) {
			importDirectives.add(oid.getIRI().toString());
		}
		classCount = ont.getClassesInSignature().size();
		namedIndividualCount = ont.getIndividualsInSignature().size();
		axiomCount = ont.getAxiomCount();
		annotations = new HashSet<OntologyAnnotation>();
		for (OWLAnnotation ann : ont.getAnnotations()) {
			annotations.add(new OntologyAnnotation(ann));
		}
	}
	
	

}
