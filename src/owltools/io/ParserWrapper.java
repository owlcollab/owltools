package owltools.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.DescriptionTreeSimilarity;

/**
 * Convenience class wrapping org.oboformat that abstracts away underlying details of ontology format or location
 * @author cjm
 *
 */
public class ParserWrapper {

	private static Logger LOG = Logger.getLogger(DescriptionTreeSimilarity.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); // persist?
	String defaultOntology;
	OBODoc obodoc;


	public OWLOntologyManager getManager() {
		return manager;
	}
	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}
	public OWLGraphWrapper parseToOWLGraph(String iriString) throws OWLOntologyCreationException, IOException {
		return new OWLGraphWrapper(parse(iriString));		
	}
	public OWLGraphWrapper parseToOWLGraph(String iriString, boolean isMergeImportClosure) throws OWLOntologyCreationException, IOException {
		return new OWLGraphWrapper(parse(iriString), isMergeImportClosure);		
	}

	public OWLOntology parse(String iriString) throws OWLOntologyCreationException, IOException {
		if (iriString.endsWith(".obo"))
			return parseOBO(iriString);
		return parseOWL(iriString);		
	}

	public OWLOntology parseOBO(String iri) throws IOException, OWLOntologyCreationException {
		OBOFormatParser p = new OBOFormatParser();
		LOG.info("Parsing: "+iri);
		if (isIRI(iri)) {
			obodoc = p.parse(IRI.create(iri).toURI().toURL());
		}
		else {
			obodoc = p.parse(iri);
		}
		if (obodoc == null) {
			throw new IOException("Loading of ontology failed: "+iri);
		}
		if (defaultOntology != null) {
			obodoc.addDefaultOntologyHeader(defaultOntology);
		}
		else {
			obodoc.addDefaultOntologyHeader(iri);
		}

		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		return ontology;
	}

	public OWLOntology parseOBOFiles(List<String> files) throws IOException, OWLOntologyCreationException, FrameMergeException {
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = null;
		for (String f : files) {
			LOG.info("Parsing file " +f);
			if (obodoc == null)
				obodoc = p.parse(f);
			else {
				OBODoc obodoc2 = p.parse(f);
				obodoc.mergeContents(obodoc2);
			}
		}

		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		return ontology;		
	}

	public OWLOntology parseOWL(String iriString) throws OWLOntologyCreationException {
		IRI iri;
		LOG.info("parsing: "+iriString);
		if (isIRI(iriString)) {
			iri = IRI.create(iriString);
		}
		else {
			iri = IRI.create(new File(iriString));
		}
		return parseOWL(iri);
	}
	
	private boolean isIRI(String iriString) {
		return iriString.startsWith("file:") || iriString.startsWith("http:") || iriString.startsWith("https:");
	}

	public OWLOntology parseOWL(IRI iri) throws OWLOntologyCreationException {
		LOG.info("parsing: "+iri.toString());
		OWLOntology ont = manager.loadOntologyFromOntologyDocument(iri);
		return ont;
	}

	public void saveOWL(OWLOntology ont, String file) throws OWLOntologyStorageException {
		OWLOntologyFormat owlFormat = new RDFXMLOntologyFormat();
		saveOWL(ont, owlFormat, file);
	}
	public void saveOWL(OWLOntology ont, OWLOntologyFormat owlFormat, String file) throws OWLOntologyStorageException {
		manager.saveOntology(ont, owlFormat, IRI.create(file));
	}
	public OBODoc getOBOdoc() {
		return obodoc;
	}
	
	

}