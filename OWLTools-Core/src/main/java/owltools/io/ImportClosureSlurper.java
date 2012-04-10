package owltools.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class ImportClosureSlurper {
	
	OWLOntology ontology;

	public ImportClosureSlurper(OWLOntology ontology) {
		super();
		this.ontology = ontology;
		
	}
	
	public void save() throws OWLOntologyStorageException, IOException {
		save(".");
	}

	
	public void save(String base) throws OWLOntologyStorageException, IOException {
		save(base, System.out);
	}
	
	public void save(String base, String file) throws IOException, OWLOntologyStorageException {
		if (file == null) {
			save(base);
			return;
		}
		FileOutputStream os = new FileOutputStream(new File(file));
		save(base, os);
	}

	public void save(String base, OutputStream outputStream) throws IOException, OWLOntologyStorageException {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream));
		save(base, w);
	}


	public void save(String base, BufferedWriter w) throws IOException, OWLOntologyStorageException {
		save(base, new RDFXMLOntologyFormat(), w);
	}
		
	public void save(String base, OWLOntologyFormat fmt, BufferedWriter w) throws IOException, OWLOntologyStorageException {
		
		w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		w.write("<catalog prefer=\"public\" xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n");

		for (IRI impDoc : ontology.getDirectImportsDocuments()) {
			boolean hasMatch = false;
			for (OWLOntology o : ontology.getDirectImports()) {
				if (o.getOntologyID().getOntologyIRI().equals(impDoc)) {
					hasMatch = true;
					break;
				}
			}
			if (!hasMatch) {
				// TODO - throw error
				System.err.println("WARNING: importsDocument and imported ontology IRI mismatch: "+impDoc);
			}
		}
		
		for (OWLOntology subOnt : ontology.getImportsClosure()) {
			String iri = subOnt.getOntologyID().getOntologyIRI().toString();
			String local = base + "/" + iri.replaceFirst("http://", "");
			IRI outputStream = IRI.create(new File(local));
			ontology.getOWLOntologyManager().saveOntology(subOnt, fmt, outputStream);
			w.write("  <uri id=\"User Entered Import Resolution\" name=\""+iri +"\" uri=\""+local+"\"/>\n");
			
			for (OWLOntology di : subOnt.getDirectImports()) {
				System.out.println("import\t"+subOnt.getOntologyID()+"\t"+di.getOntologyID());
			}
		}
		
		w.write("  <group id=\"Folder Repository, directory=, recursive=false, Auto-Update=false, version=2\" prefer=\"public\" xml:base=\"\">\n");
		w.write("  </group>\n");
		w.write("</catalog>\n");
		w.close();
	}

}
