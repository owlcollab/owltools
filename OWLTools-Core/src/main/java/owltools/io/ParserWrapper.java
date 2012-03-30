package owltools.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.List;

import org.apache.log4j.Logger;
import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.obolibrary.oboformat.writer.OBOFormatWriter.NameProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphWrapper;

/**
 * Convenience class wrapping org.oboformat that abstracts away underlying details of ontology format or location
 * @author cjm
 *
 */
public class ParserWrapper {

	private static Logger LOG = Logger.getLogger(ParserWrapper.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); // persist?
	OBODoc obodoc;


	public OWLOntologyManager getManager() {
		return manager;
	}
	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}
	public void addIRIMapper(OWLOntologyIRIMapper mapper) {
		manager.addIRIMapper(mapper);
	}
	public void removeIRIMapper(OWLOntologyIRIMapper mapper) {
		manager.removeIRIMapper(mapper);
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

	public OWLOntology parseOBO(String source) throws IOException, OWLOntologyCreationException {
		OBOFormatParser p = new OBOFormatParser();
		LOG.info("Parsing: "+source);
		final String id;
		if (isIRI(source)) {
			obodoc = p.parse(IRI.create(source).toURI().toURL());
			id = source;
		}
		else {
			final File file = new File(source);
			obodoc = p.parse(file);
			String fileName = file.getName();
			if (fileName.endsWith(".obo") || fileName.endsWith(".owl")) {
				fileName.substring(0, fileName.length() - 4);
			}
			id = fileName;
		}
		if (obodoc == null) {
			throw new IOException("Loading of ontology failed: "+source);
		}
		/*
		 * This fixes an exception for ontologies without an declared id. 
		 * Only by URL encoding the path it is guaranteed that a valid 
		 * ontology id is generated.
		 */
		obodoc.addDefaultOntologyHeader(URLEncoder.encode(id, "UTF-8"));

		Obo2Owl bridge = new Obo2Owl(manager);
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
		LOG.info("parsing: "+iri.toString()+" using "+manager);
		OWLOntology ont;
		try {
			ont = manager.loadOntology(iri);
		} catch (OWLOntologyAlreadyExistsException e) {
			// Trying to recover from exception
			OWLOntologyID ontologyID = e.getOntologyID();
			ont = manager.getOntology(ontologyID);
			if (ont == null) {
				// throw original exception, if no ontology could be found
				// never return null ontology
				throw e;
			}
			else {
				LOG.info("Skip already loaded ontology: "+iri);
			}
		} catch (OWLOntologyDocumentAlreadyExistsException e) {
			// Trying to recover from exception
			IRI duplicate = e.getOntologyDocumentIRI();
			ont = manager.getOntology(duplicate);
			if (ont == null) {
				for(OWLOntology managed : manager.getOntologies()) {
					if(duplicate.equals(managed.getOntologyID().getOntologyIRI())) {
						LOG.info("Skip already loaded ontology: "+iri);
						ont = managed;
						break;
					}
				}
			}
			if (ont == null) {
				// throw original exception, if no ontology could be found
				// never return null ontology
				throw e;
			}
		}
		return ont;
	}

	public void saveOWL(OWLOntology ont, String file, OWLGraphWrapper graph) throws OWLOntologyStorageException {
		OWLOntologyFormat owlFormat = new RDFXMLOntologyFormat();
		saveOWL(ont, owlFormat, file, graph);
	}
	public void saveOWL(OWLOntology ont, OWLOntologyFormat owlFormat, String file, OWLGraphWrapper graph) throws OWLOntologyStorageException {
		if (owlFormat instanceof OBOOntologyFormat) {
			try {
				FileOutputStream os = new FileOutputStream(new File(file));
				saveOWL(ont, owlFormat, os, graph);
			} catch (FileNotFoundException e) {
				throw new OWLOntologyStorageException("Could not open file: "+file, e);
			}
		}
		else {
			manager.saveOntology(ont, owlFormat, IRI.create(file));
		}
	}
	public void saveOWL(OWLOntology ont, OWLOntologyFormat owlFormat,
			OutputStream outputStream, OWLGraphWrapper graph) throws OWLOntologyStorageException {
		if (owlFormat instanceof OBOOntologyFormat) {
			Owl2Obo bridge = new Owl2Obo();
			OBODoc doc;
			BufferedWriter bw = null;
			try {
				doc = bridge.convert(ont);
				OBOFormatWriter oboWriter = new OBOFormatWriter();
				bw = new BufferedWriter(new OutputStreamWriter(outputStream));
				if (graph != null) {
					oboWriter.write(doc, bw, new OWLGraphWrapperNameProvider(graph, doc));
				}
				else {
					oboWriter.write(doc, bw);
				}
				
			} catch (OWLOntologyCreationException e) {
				throw new OWLOntologyStorageException("Could not create temporary OBO ontology.", e);
			} catch (IOException e) {
				throw new OWLOntologyStorageException("Could not write ontology to output stream.", e);
			}
			finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (IOException e) {
						LOG.warn("Could not close writer.", e);
					}
				}
			}
		}
		else {
			manager.saveOntology(ont, owlFormat, outputStream);
		}
	}
	
	public OBODoc getOBOdoc() {
		return obodoc;
	}

	/**
	 * Provide names for the {@link OBOFormatWriter} using an {@link OWLGraphWrapper}.
	 */
	public static class OWLGraphWrapperNameProvider implements NameProvider {
		private final OWLGraphWrapper graph;
		private final String defaultOboNamespace;

		/**
		 * @param graph
		 */
		public OWLGraphWrapperNameProvider(OWLGraphWrapper graph) {
			super();
			this.graph = graph;
			this.defaultOboNamespace = null;
			
		}
		
		/**
		 * @param graph
		 * @param defaultOboNamespace
		 */
		public OWLGraphWrapperNameProvider(OWLGraphWrapper graph, String defaultOboNamespace) {
			super();
			this.graph = graph;
			this.defaultOboNamespace = defaultOboNamespace;
			
		}
		
		/**
		 * @param graph
		 * @param oboDoc
		 */
		public OWLGraphWrapperNameProvider(OWLGraphWrapper graph, OBODoc oboDoc) {
			super();
			this.graph = graph;
			String defaultOboNamespace = null;
			if (oboDoc != null) {
				Frame headerFrame = oboDoc.getHeaderFrame();
				if (headerFrame != null) {
					defaultOboNamespace = headerFrame.getTagValue(OboFormatTag.TAG_DEFAULT_NAMESPACE, String.class);
				}
			}
			this.defaultOboNamespace = defaultOboNamespace;
			
		}

		public String getName(String id) {
			String name = null;
			OWLObject obj = graph.getOWLObjectByIdentifier(id);
			if (obj != null) {
				name = graph.getLabel(obj);
			}
			return name;
		}

		public String getDefaultOboNamespace() {
			return defaultOboNamespace;
		}
	}
	

}