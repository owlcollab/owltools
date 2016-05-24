package owltools.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.common.base.Optional;

public class ImportClosureSlurper {
	
	private static final Logger LOGGER = Logger.getLogger(ImportClosureSlurper.class);
	
	OWLOntology ontology;

	public ImportClosureSlurper(OWLOntology ontology) {
		super();
		this.ontology = ontology;
		
	}
	
	public void save() throws OWLOntologyStorageException, IOException {
		save(".");
	}

	
	public void save(String base) throws OWLOntologyStorageException, IOException {
		save(new File(base).getCanonicalFile(), System.out);
	}
	
	public void save(String base, String file) throws IOException, OWLOntologyStorageException {
		if (file == null) {
			save(base);
			return;
		}
		File baseFolder =  new File(base).getCanonicalFile();
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(new File(baseFolder, file));
			save(baseFolder, os);
		}finally {
			IOUtils.closeQuietly(os);
		}
	}

	public void save(File base, OutputStream outputStream) throws IOException, OWLOntologyStorageException {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream));
		save(base, w);
	}


	public void save(File baseFolder, BufferedWriter w) throws IOException, OWLOntologyStorageException {
		w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		w.write("<catalog prefer=\"public\" xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n");
		
		for (OWLOntology ont : ontology.getImportsClosure()) {
			validateImports(ont);
			
			OWLOntologyID ontologyID = ont.getOntologyID();
			IRI actualIRI = null;
			Optional<IRI> optional = ontologyID.getOntologyIRI();
			if (optional.isPresent()) {
				actualIRI = optional.get();
			}

			// Not really sure why this is here, but apparently we can get
			// an ontology without an IRI, in which case we'll generate one
			// that is 'sort of' unique (only fails if two different machines run
			// this tool at the exact same time).
			//
			if (actualIRI == null) {
				IRI generatedIRI = IRI.generateDocumentIRI();
				actualIRI = generatedIRI;
			}
			// Always write the actualIRI
			String actualLocalFile = createLocalFileName(actualIRI);
			IRI outputStream = IRI.create(new File(baseFolder, actualLocalFile));
			ont.getOWLOntologyManager().saveOntology(ont, outputStream);
			
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("name: "+ actualIRI +" local: "+actualLocalFile);
			}
			w.write("  <uri name=\""+ actualIRI  +"\" uri=\""+ actualLocalFile +"\"/>\n");

			//
			// In case there is a difference between the source document IRI
			// and the IRI of the resolved target (e.g., there is an HTTP
			// redirect from a legacy IRI to a newer IRI), then write an entry
			// in the catalog that points the legacy IRI to the newer, canonical one.
			// Examples of this include:
			//  http://purl.obolibrary.org/obo/so.owl
			// which redirects to:
			//  http://purl.obolibrary.org/obo/so-xp.obo.owl
			//

			IRI 			documentIRI = ont.getOWLOntologyManager().getOntologyDocumentIRI(ont);
			if (documentIRI != actualIRI) {
				String sourceLocalFile = createLocalFileName(actualIRI);
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("alias: "+ documentIRI + " ==> " + actualIRI + " local: "+ sourceLocalFile);
				}
				w.write("  <uri name=\""+ documentIRI +"\" uri=\""+ sourceLocalFile +"\"/>\n");
			}
		}
		w.write("</catalog>\n");
		w.flush();
	}
	
	static String createLocalFileName(IRI iri) {
		String iriString = iri.toString();
		iriString = iriString.replaceFirst("http://", "").replaceFirst("https://", "");
		iriString = iriString.replace(':', '_');
		iriString = iriString.replace('\\', '_');
		return iriString;
	}
	
	static void validateImports(OWLOntology ontology) throws IOException {
		Set<IRI> directImportDocuments = ontology.getDirectImportsDocuments();
		Set<OWLOntology> directImports = ontology.getDirectImports();
		if (directImports.size() < directImportDocuments.size()) {
			// less imports than actually declared
			// assume something went wrong, throw Exception
			throw new IOException("The ontology has less actual imports then declared.\nActual: "+directImports+"\n Declared: "+directImportDocuments);
		}
	}

}
