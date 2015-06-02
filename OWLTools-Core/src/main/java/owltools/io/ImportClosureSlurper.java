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
			
			IRI iri = null;
			OWLOntologyID ontologyID = ont.getOntologyID();
			if (ontologyID != null) {
				iri = ontologyID.getOntologyIRI();
			}
			if (iri == null) {
				iri = IRI.generateDocumentIRI();
			}
			String local = createLocalFileName(iri);
			IRI outputStream = IRI.create(new File(baseFolder, local));
			ont.getOWLOntologyManager().saveOntology(ont, outputStream);
			
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("name: "+iri+" local: "+local);
			}
			w.write("  <uri name=\""+iri +"\" uri=\""+local+"\"/>\n");
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
