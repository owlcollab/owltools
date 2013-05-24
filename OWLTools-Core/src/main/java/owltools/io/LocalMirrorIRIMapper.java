package owltools.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This mapper allows for IRI resolution based on matching *parts* of URIs.
 * 
 * For example, the user can choose to map http://purl.obolibrary.org/obo ==> /users/me/obo-mirror/
 * 
 * These mappings are created by feeding in a basic mapping file, with two columns:
 * (1) URL Pattern (the URI to be mapped must start-with this pattern
 * (2) Substitution (typically a local or global path in the users filesystem)
 * 
 * {@link OWLOntologyIRIMapper} using the mappings to a local directory
 */
public class LocalMirrorIRIMapper implements OWLOntologyIRIMapper {

	private static final Logger logger = Logger.getLogger(LocalMirrorIRIMapper.class);

	private final Map<IRI, IRI> mappings;

	private File parentFolder;

	LocalMirrorIRIMapper(Map<IRI, IRI> mappings) {
		this.mappings = mappings;
	}

	/**
	 * Create an instance from the given directory file.
	 * 
	 * Each line in the file is of the form
	 * 
	 * URLPrefix WHITESPACE LocalPath
	 * 
	 * In the future apache-style redirects may be possible
	 * 
	 * @param mappingFile
	 * @throws IOException
	 */
	public LocalMirrorIRIMapper(String mappingFile) throws IOException {
		this(new File(mappingFile).getAbsoluteFile());
	}

	/**
	 * Create an mappingXmlIRIMapper from the given mapping.xml file.
	 * Assume, that relative paths are relative to the mapping file location.
	 * 
	 * @param mappingFile
	 * @throws IOException
	 */
	public LocalMirrorIRIMapper(File mappingFile) throws IOException {
		this(mappingFile, mappingFile.getAbsoluteFile().getParentFile());
	}

	/**
	 * Create an mappingXmlIRIMapper from the given mapping.xml file. 
	 * Use the parentFolder to resolve relative paths from the mapping file. 
	 * 
	 * @param mappingFile
	 * @param parentFolder
	 * @throws IOException
	 */
	public LocalMirrorIRIMapper(File mappingFile, File parentFolder) throws IOException {
		this(parseDirectoryMappingFile(new FileInputStream(mappingFile), parentFolder));
		this.parentFolder = parentFolder;
	}

	/**
	 * Create an instance from the given mapping URL.
	 * Assume, there are no relative paths in the mapping file.
	 * 
	 * @param mappingURL
	 * @throws IOException
	 */
	public LocalMirrorIRIMapper(URL mappingURL) throws IOException {
		if ("file".equals(mappingURL.getProtocol())) {
			try {
				File mappingFile = new File(mappingURL.toURI());
				mappings = parseDirectoryMappingFile(new FileInputStream(mappingFile), mappingFile.getParentFile());
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
		else {
			mappings = parseDirectoryMappingFile(mappingURL.openStream(), null);
		}
	}

	/**
	 * Create an instance from the given mapping URL.
	 * Use the parentFolder to resolve relative paths from the mapping file. 
	 * 
	 * @param mappingURL
	 * @param parentFolder
	 * @throws IOException
	 */
	public LocalMirrorIRIMapper(URL mappingURL, File parentFolder) throws IOException {
		this(parseDirectoryMappingFile(mappingURL.openStream(), parentFolder));
	}

	@Override
	public IRI getDocumentIRI(IRI ontologyIRI) {
		String source = ontologyIRI.toString();
		IRI targetIRI = null;
		String targetPrefix = null;
		for (IRI sourcePrefixIRI : mappings.keySet()) {
			String sourcePrefix = sourcePrefixIRI.toString();
			if (source.startsWith(sourcePrefix)) {
				// check if we have a targetIRI
				if (targetIRI != null) {
					// tie-braking : more specific (longer) prefixes take priority
					if (mappings.get(sourcePrefix).toString().length() < targetPrefix.length()) {
						continue;
					}
				}

				targetPrefix = mappings.get(sourcePrefixIRI).toString();
				String target = source.replaceFirst(sourcePrefix, targetPrefix);
				
				if (target != null) {
					if (parentFolder != null && target.indexOf(":") < 0) {
						// there is a parent folder and the mapping is not an IRI or URL
						File file = new File(target);
						if (!file.isAbsolute()) {
							file = new File(parentFolder, target);
						}
						try {
							file = file.getCanonicalFile();
							targetIRI = IRI.create(file);
						} catch (IOException e) {
							logger.warn("Skipping mapping: "+source+"   "+target, e);
						}
					}
					else {
						targetIRI = IRI.create(target);
					}
				}

				logger.info("Mapping "+source +" ==> "+targetIRI);
			}
		}
		return targetIRI;
	}

	/**
	 * Parse the inputStream as a partial redirect mapping file and extract IRI mappings.
	 * 
	 * Optional: Resolve relative file paths with the given parent folder.
	 * 
	 * @param inputStream input stream (never null)
	 * @param parentFolder folder or null
	 * @return mappings
	 * @throws IOException
	 * @throws IllegalArgumentException if input stream is null
	 */
	static Map<IRI, IRI> parseDirectoryMappingFile(InputStream inputStream, final File parentFolder) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream should never be null, missing resource?");
		}

		try {
			final Map<IRI, IRI> mappings = new HashMap<IRI, IRI>();
			for (String line : IOUtils.readLines(inputStream)) {
				if (line.startsWith("#")) {
					continue;
				}
				
				String[] toks = line.split(" ", 2);
				if (toks.length != 2) {
					throw new IOException("Each line must have 1 space: "+line);
				}
				mappings.put(IRI.create(toks[0]),
						IRI.create(toks[1]));
			}
			return mappings;

		} finally {
			inputStream.close();
		}
	}
}
