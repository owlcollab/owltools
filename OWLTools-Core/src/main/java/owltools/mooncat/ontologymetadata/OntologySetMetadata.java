package owltools.mooncat.ontologymetadata;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;

public class OntologySetMetadata {
	public String serverManifestVersion;
	public Set<OntologyMetadata> ontologies;
	
	public OntologySetMetadata(OWLOntology ont) {
		ontologies = new HashSet<OntologyMetadata>();
		for (OWLOntology io : ont.getImportsClosure()) {
			OntologyMetadata om = new OntologyMetadata(io);
			ontologies.add(om);
			if (io.equals(ont)) {
				om.isRoot = true;
			}
		}
	}
}
