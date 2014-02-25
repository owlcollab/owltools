package owltools.gaf.eco;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Factory to create instances of {@link EcoMapper} and {@link TraversingEcoMapper}.
 */
public class EcoMapperFactory {
	
	private static final OWLReasonerFactory reasonerFactor = new ElkReasonerFactory();

	private EcoMapperFactory() {
		// private constructor, no instances allowed
	}
	
	public static class OntologyMapperPair<MAPPER extends EcoMapper> {
		
		private final OWLGraphWrapper graph;
		private final MAPPER mapper;
		
		/**
		 * @param graph
		 * @param mapper
		 */
		OntologyMapperPair(OWLGraphWrapper graph, MAPPER mapper) {
			this.graph = graph;
			this.mapper = mapper;
		}

		/**
		 * @return the graph
		 */
		public OWLGraphWrapper getGraph() {
			return graph;
		}

		/**
		 * @return the mapper
		 */
		public MAPPER getMapper() {
			return mapper;
		}
	}
	
	/**
	 * Create an instance of a {@link EcoMapper}. Uses a separate parser. Load
	 * the ECO and mappings using their PURLs.
	 * 
	 * @return mapper pair
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_PURL
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<EcoMapper> createEcoMapper() throws OWLException, IOException {
		return createEcoMapper(new ParserWrapper());
	}
	
	/**
	 * Create an instance of a {@link EcoMapper}. Uses a the manager to load ECO via the
	 * PURL. Load mappings using the PURL.
	 * @param m 
	 * 
	 * @return mapper pair
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_PURL
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<EcoMapper> createEcoMapper(OWLOntologyManager m) throws OWLException, IOException {
		ParserWrapper p = new ParserWrapper();
		p.setManager(m);
		return createEcoMapper(p);
	}
	
	/**
	 * Create an instance of a {@link EcoMapper}. Uses the given
	 * {@link ParserWrapper} to load the ontology. Retrieves ECO and the
	 * mappings using their PURLs.
	 * 
	 * @param p
	 * @return mapper pair
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_PURL
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<EcoMapper> createEcoMapper(ParserWrapper p) throws OWLException, IOException {
		return createEcoMapper(p, EcoMapper.ECO_PURL);
	}
	
	/**
	 * Create an instance of a {@link EcoMapper}. Uses the given
	 * {@link ParserWrapper} to load the ontology. Retrieves ECO from the given location and the
	 * mapping from the PURL.
	 * 
	 * @param p
	 * @param location
	 * @return mapper pair
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<EcoMapper> createEcoMapper(ParserWrapper p, String location) throws OWLException, IOException {
		final OWLOntology eco = p.parseOWL(location);
		final OWLGraphWrapper graph = new OWLGraphWrapper(eco);
		final EcoMapper mapper = createEcoMapper(graph);
		final OntologyMapperPair<EcoMapper> pair = new OntologyMapperPair<EcoMapper>(graph, mapper);
		return pair ;
	}
	
	/**
	 * Create an instance of a {@link EcoMapper}. Retrieves the mappings using
	 * the PURL.
	 * 
	 * @param graph graph containing ECO
	 * @return mapper
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static EcoMapper createEcoMapper(OWLGraphWrapper graph) throws IOException {
		Reader reader = null;
		try {
			reader = createReader(EcoMapper.ECO_MAPPING_PURL);
			EcoMappings mappings = loadEcoMappings(reader, graph);
			return createEcoMapper(mappings);
		}
		finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	static EcoMapper createEcoMapper(EcoMappings mappings) {
		return new EcoMapperImpl(mappings);
	}
	
	/**
	 * Create a {@link TraversingEcoMapper} instance using a new
	 * {@link ParserWrapper} to load ECO. ECO and the mappings are retrieved
	 * using their PURLs.
	 * <p>
	 * Creates an ELK reasoner to be used in the traversal methods. Use
	 * {@link TraversingEcoMapper#dispose()} to ensure proper cleanup of the ELK
	 * worker thread pool.
	 * 
	 * @return mapper pair
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_PURL
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<TraversingEcoMapper> createTraversingEcoMapper() throws OWLException, IOException {
		return createTraversingEcoMapper(new ParserWrapper());
	}
	
	/**
	 * Create a {@link TraversingEcoMapper} instance using the given
	 * {@link ParserWrapper} to load ECO. ECO and the mappings are retrieved
	 * using their PURLs.
	 * <p>
	 * Creates an ELK reasoner to be used in the traversal methods. Use
	 * {@link TraversingEcoMapper#dispose()} to ensure proper cleanup of the ELK
	 * worker thread pool.
	 * 
	 * @param p
	 * @return mapper
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_PURL
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<TraversingEcoMapper> createTraversingEcoMapper(ParserWrapper p) throws OWLException, IOException {
		return createTraversingEcoMapper(p, EcoMapper.ECO_PURL);
	}
	
	/**
	 * Create a {@link TraversingEcoMapper} instance using the given
	 * {@link ParserWrapper} to load ECO from the given location. The mappings
	 * are retrieved using the PURL.
	 * <p>
	 * Creates an ELK reasoner to be used in the traversal methods. Use
	 * {@link TraversingEcoMapper#dispose()} to ensure proper cleanup of the ELK
	 * worker thread pool.
	 * 
	 * @param p
	 * @param location
	 * @return mapper
	 * @throws OWLException
	 * @throws IOException
	 * 
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static OntologyMapperPair<TraversingEcoMapper> createTraversingEcoMapper(ParserWrapper p, String location) throws OWLException, IOException {
		OWLOntology eco = p.parseOWL(EcoMapper.ECO_PURL_IRI);
		OWLReasoner reasoner = reasonerFactor.createReasoner(eco);
		Reader reader = null;
		try {
			OWLGraphWrapper ecoGraph = new OWLGraphWrapper(eco);
			reader = createReader(EcoMapper.ECO_MAPPING_PURL);
			final TraversingEcoMapper mapper = createTraversingEcoMapper(reader, ecoGraph, reasoner, true);
			return new OntologyMapperPair<TraversingEcoMapper>(ecoGraph, mapper);
		}
		finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	/**
	 * Create a {@link TraversingEcoMapper} instance using the given
	 * {@link OWLGraphWrapper}. It is assumed that ECO can be retrieved from the
	 * graph using its default IRI. The mappings are retrieved using the PURL.
	 * <p>
	 * Uses the given reasoner in the traversal methods. If disposeReasoner is
	 * set to true, dispose also the reasoner, while calling
	 * {@link TraversingEcoMapper#dispose()}.
	 * 
	 * @param all
	 *            graph containing all ontologies, including ECO
	 * @param reasoner
	 *            reasoner capable of traversing ECO
	 * @param disposeReasoner
	 *            set to true if the reasoner should be disposed, when calling
	 *            {@link TraversingEcoMapper#dispose()}
	 * @return mapper
	 * @throws IOException
	 * @throws OWLException
	 * @throws IllegalArgumentException
	 *             throw when the reasoner is null, or the
	 *             {@link OWLGraphWrapper} does not contain ECO.
	 * 
	 * @see EcoMapper#ECO_PURL_IRI
	 * @see EcoMapper#ECO_MAPPING_PURL
	 */
	public static TraversingEcoMapper createTraversingEcoMapper(OWLGraphWrapper all, OWLReasoner reasoner, boolean disposeReasoner) throws IOException, OWLException {
		
		// This has bitten me, so let's try and be specific...
		if( reasoner == null )	{
			throw new IllegalArgumentException("No reasoner was specified for use with the EcoTools. Add a reasoner for the command line");
		}
				
		OWLOntology eco = null;
		
		// assume the graph wrapper is more than eco
		// try to find ECO by its purl
		Set<OWLOntology> allOntologies = all.getAllOntologies();
		for (OWLOntology owlOntology : allOntologies) {
			OWLOntologyID id = owlOntology.getOntologyID();
			IRI ontologyIRI = id.getOntologyIRI();
			if (ontologyIRI != null) {
				if (EcoMapper.ECO_PURL_IRI.equals(ontologyIRI)) {
					eco = owlOntology;
				}
			}
		}
		if (eco == null) {
			throw new IllegalArgumentException("The specified graph did not contain ECO with the IRI: "+EcoMapper.ECO_PURL_IRI);
		}

		OWLGraphWrapper ecoGraph = new OWLGraphWrapper(eco);
		Reader reader = null;
		try {
			reader = createReader(EcoMapper.ECO_MAPPING_PURL);
			EcoMappings mappings = loadEcoMappings(reader, ecoGraph);
			return new TraversingEcoMapperImpl(mappings, reasoner, disposeReasoner);
		}
		finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	static Reader createReader(String src) throws IOException {
		if (src.indexOf(':') > 0) {
			// assume its an url
			URL url = new URL(src);
			return new InputStreamReader(url.openStream());
		}
		
		// treat as file
		File file = new File(src);
		return new FileReader(file);
	}
	
	static TraversingEcoMapper createTraversingEcoMapper(Reader mappingsReader, OWLGraphWrapper eco, OWLReasoner reasoner, boolean disposeReasoner) throws IOException, OWLException {
		EcoMappings mappings = loadEcoMappings(mappingsReader, eco);
		return new TraversingEcoMapperImpl(mappings, reasoner, disposeReasoner);
	}
	
	private static EcoMappings loadEcoMappings(Reader mappingsReader, OWLGraphWrapper eco) throws IOException {
		EcoMappings mappings = new EcoMappings();
		List<String> lines = IOUtils.readLines(mappingsReader);
		for (String line : lines) {
			line = StringUtils.trimToNull(line);
			if (line != null) {
				char c = line.charAt(0);
				if ('#' != c) {
					String[] split = StringUtils.split(line, '\t');
					if (split.length == 3) {
						String code = split[0];
						String ref = split[1];
						String ecoId = split[2];
						OWLClass cls = eco.getOWLClassByIdentifier(ecoId);
						if (cls != null) {
							mappings.add(code, ref, cls);
						}
					}
				}
			}
		}
		return mappings;
	}
	
	/**
	 * Helper to access the mapping for ECO codes. ECO codes should always have
	 * a 'Default' mapping. Optionally, they have additional mappings for
	 * specific annotation references.
	 */
	static class EcoMappings {
		
		static final String DEFAULT_REF = "Default";
		
		private final Map<String, Map<String, OWLClass>> allMappings = new HashMap<String, Map<String,OWLClass>>();
		
		void add(String code, String ref, OWLClass cls) {
			Map<String, OWLClass> codeMap = allMappings.get(code);
			if (codeMap == null) {
				codeMap = new HashMap<String, OWLClass>();
				allMappings.put(code, codeMap);
			}
			if (ref == null) {
				ref = DEFAULT_REF;
			}
			codeMap.put(ref, cls);
		}
		
		OWLClass get(String code, String ref) {
			OWLClass result = null;
			if (code != null) {
				Map<String, OWLClass> codeMap = allMappings.get(code);
				if (codeMap != null) {
					if (ref == null) {
						ref = DEFAULT_REF;
					}
					result = codeMap.get(ref);
				}
			}
			return result;
		}
		
		OWLClass get(String code) {
			return get(code, DEFAULT_REF);
		}
		
		Set<OWLClass> getAll(String code) {
			Set<OWLClass> result = new HashSet<OWLClass>();
			if (code != null) {
				Map<String, OWLClass> codeMap = allMappings.get(code);
				if (codeMap != null) {
					result.addAll(codeMap.values());
				}
			}
			return result;
		}
		
		boolean hasCode(String code) {
			return allMappings.containsKey(code);
		}
		
		Map<OWLClass, String> getReverseMap() {
			Map<OWLClass, String> simpleMap = new HashMap<OWLClass, String>();
			for(Entry<String, Map<String, OWLClass>> e : allMappings.entrySet()) {
				Map<String, OWLClass> codeMap = e.getValue();
				OWLClass defaultClass = codeMap.get(DEFAULT_REF);
				if (defaultClass != null) {
					simpleMap.put(defaultClass, e.getKey());
				}
			}
			return simpleMap;
		}
	}
}
