package owltools.ncbi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Provides static methods for converting NCBI Taxonomy data files into
 * OWL format. 
 *
 * <p>The <a href="http://www.ncbi.nlm.nih.gov">National Center for Biotechnology Information</a>
 * provides a <a href="http://www.ncbi.nlm.nih.gov/taxonomy">Taxonomy Database</a>
 * with a large number of terms for classifying organisms.
 * This tool converts that data to a
 * <a href="http://www.w3.org/TR/owl2-overview/">Web Ontology Language (OWL)</a>
 * representation.</p>
 *
 * <p>The latest NCBI Taxonomy data can be downloaded from
 * <a href="ftp://ftp.ebi.ac.uk/pub/databases/taxonomy/taxonomy.dat">ftp://ftp.ebi.ac.uk/pub/databases/taxonomy/taxonomy.dat</a>
 * (file size is more than 200MB).
 * The data format is line-based. Blocks are separated by "//",
 * and each line within the block provides the pair of a field name and a field
 * value separated by a colon. See the <code>src/test/resources/sample.dat</code> file for an example.</p>
 *
 * <p>This tool uses <a href="http://owlapi.sourceforge.net">OWLAPI</a>
 * to create and manipulate the ontology, and depends on
 * <a href="http://oboformat.googlecode.com">OBOFormat</a>
 * for the IRIs and labels of several annotation properties.</p>
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class NCBI2OWL extends NCBIConverter {
	/**
	 * Create a logger.
	 */
	protected final static Logger logger =
		Logger.getLogger(NCBI2OWL.class);

	/**
	 * Command line usage information.
	 */
	protected static String usage = 
		"usage: ncbi-converter [-ca] <input.dat> <output.owl> [axioms.txt] [-m [merged.dmp]] [-t [taxdmp.zip]] [-n [names.dmp]] [-l [citations.dmp]]\n\n" +
		"       -c   Convert to OWL.\n" +
		"       -a   Print axiom list.\n" +
		"       -ca  Convert and print axiom list.\n"+
		"       -m   Extract alternate identifier information from the given merged.dmp file.\n"+
		"       -t   Extract alternate identifier, unique name, and citation information from the taxdmp.zip file.\n"+
		"       -n   Extract unique name information from given names.dmp file.\n"+
		"       -l   Extract literature citation information from the given citation.dmp file\n";


	/**
	 * Handle command-line arguments when this class is used in an
	 * executable Jar file. See <code>usage</code>.
	 *
	 * @param args one or more strings to provide options and paths
	 */
	public static void main(String[] args) {
		try {
			boolean printAxioms = false;
			boolean checkAxioms = false;
			
			String inputDat = null;
			String mergedDmp = null;
			String namesDmp = null;
			String citationDmp = null;
			String taxdmp = null;
			String outputOwl = null;
			
			String axiomFile = "axioms.txt";
			
			for (int i = 0; i < args.length; i++) {
				String current = args[i];
				if (current.charAt(0) == '-') {
					// option
					if ("-ca".equals(current)) {
						printAxioms = true;
					}
					if ("-a".equals(current)) {
						checkAxioms = true;
					}
					else if ("-c".equals(current)) {
						// do nothing
					}
					else if ("-m".equals(current)) {
						if ((i+1) < args.length ) {
							i++;
							mergedDmp = args[i];
						}
						else {
							mergedDmp = "merged.dmp";
						}
					}
					else if ("-n".equals(current)) {
						if ((i+1) < args.length ) {
							i++;
							namesDmp = args[i];
						}
						else {
							namesDmp = "names.dmp";
						}
					}
					else if ("-l".equals(current)) {
						if ((i+1) < args.length ) {
							i++;
							citationDmp = args[i];
						}
						else {
							citationDmp = "citations.dmp";
						}
					}
					else if ("-t".equals(current)) {
						if ((i+1) < args.length ) {
							i++;
							taxdmp = args[i];
						}
						else {
							taxdmp = "taxdmp.zip";
						}
					}
					else {
						// unknown option
						error("unknown option: "+current);
						return;
					}
				}
				else {
					// treat as value
					if (inputDat == null) {
						inputDat = current;
					}
					else if (outputOwl == null) {
						outputOwl = current;
					}
					else if (printAxioms) {
						axiomFile = current;
					}
					else {
						error("Unexpected number of input parameters.");
						return;
					}
				}
			}
			
			if (inputDat == null) {
				// default input name
				inputDat = "taxonomy.dat";
			}
			
			// check input file
			final File inputFile = new File(inputDat);
			if (!inputFile.exists()) {
				error("The specified input file doesn't exist: "+inputDat);
				return;
			}
			if (!inputFile.isFile()) {
				error("The specified input file is not a file: "+inputDat);
				return;
			} 
			if (!inputFile.canRead()) {
				error("The specified input file can't be read, please check the permissions: "+inputDat);
				return;
			}
			
			if (outputOwl == null) {
				// default output name
				outputOwl = "ncbitaxon.owl";
			}
			// check output file
			final File outputFile = new File(outputOwl);
			if (outputFile.isDirectory()) {
				error("The specified output file is a directory: "+outputOwl);
				return;
			} 
			if (outputFile.exists() && !outputFile.canWrite()) {
				error("The specified output file can't be over written, please check the permissions: "+outputOwl);
				return;
			}
			
			// start converting taxonomy.
			if (checkAxioms) {
				checkAxioms(inputDat, outputOwl);
			}
			else {
				InputStream mergeInfo = null;
				if (mergedDmp != null) {
					mergeInfo = new FileInputStream(mergedDmp);
				}
				else if (taxdmp != null) {
					ZipFile zipFile = new ZipFile(taxdmp);
					ZipEntry entry = zipFile.getEntry("merged.dmp");
					mergeInfo = zipFile.getInputStream(entry);
				}
				InputStream citationInfo = null;
				if (citationDmp != null) {
					citationInfo = new FileInputStream(citationDmp);
				}
				else if (taxdmp != null) {
					ZipFile zipFile = new ZipFile(taxdmp);
					ZipEntry entry = zipFile.getEntry("citations.dmp");
					citationInfo = zipFile.getInputStream(entry);
				}
				
				Map<String, String> uniqueNames = null;
				if (namesDmp != null) {
					uniqueNames = loadUniqueNames(new FileInputStream(namesDmp));
				}
				else if (taxdmp != null) {
					ZipFile zipFile = new ZipFile(taxdmp);
					ZipEntry entry = zipFile.getEntry("names.dmp");
					uniqueNames = loadUniqueNames(zipFile.getInputStream(entry));
				}
				OWLOntology ontology = convertToOWL(inputDat, outputOwl, mergeInfo, citationInfo, uniqueNames);
				if (printAxioms) {
					printAxioms(ontology, axiomFile);
				}
			}
			
		} catch (Exception e) {
			error(e.getMessage());
		}
	}

	public static void error(String msg) {
		System.err.println("Error: "+msg);
		System.err.println();
		System.err.println(usage);
		// exit with error code.
		System.exit(-1);
	}
	
	/**
	 * Read a data file and create an OWL representation.
	 *
	 * @param inputPath the path to the input data file (e.g. taxonomy.dat)
	 * @param uniqueNames 
	 * @return OWL ontology
	 * @throws IOException if the paths do not resolve
	 * @throws OWLOntologyCreationException if OWLAPI fails to create an
	 *	empty ontology
	 * @throws OWLOntologyStorageException if OWLAPI can't save the file
	 */
	public static OWLOntology convertToOWL(String inputPath, Map<String, String> uniqueNames)
			throws IOException, OWLOntologyCreationException,
			OWLOntologyStorageException
	{
		// Create the ontology.
		OWLOntology ontology = NCBIOWL.createOWLOntology();

		// Read the input file.
		File file = new File(inputPath);
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		try {
			// Version the ontology using the date of the source file.
			SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
			String date = day.format(file.lastModified());
			annotate(ontology, "owl:versionIRI",
				IRI.create(OBO + "ncbitaxon/" + date + "/ncbitaxon.owl"));
	
			// Handle each line of the file in turn.
			// Labels should be unique, so we keep a list of them.
			HashSet<String> labels = new HashSet<String>();
			OWLClass taxon = null;
			String line;
			int lineNumber = 0;
			while ((line = br.readLine()) != null) {
				taxon = handleLine(ontology, uniqueNames, labels, taxon,
						line, lineNumber);
				lineNumber++;
				if (lineNumber % 10000 == 0) {
					logger.debug("At line " + lineNumber);
				}
			}
			logger.debug("Finished reading lines: " + lineNumber);
			logger.debug("Filled ontology. Axioms: " +
				ontology.getAxiomCount());
			return ontology;
		}finally {
			br.close();
		}
	}

	/**
	 * Read a data file, create an OWL representation, and save an OWL file.
	 *
	 * @param inputPath the path to the input data file (e.g. taxonomy.dat)
	 * @param outputPath the path to the output OWL file
	 *	(e.g. ncbi_taxonomy.owl).
	 * @param uniqueNames
	 * @return OWL ontology
	 * @throws IOException if the paths do not resolve
	 * @throws OWLOntologyCreationException if OWLAPI fails to create an
	 *	empty ontology
	 * @throws OWLOntologyStorageException if OWLAPI can't save the file
	 */
	public static OWLOntology convertToOWL(String inputPath,
			String outputPath,
			Map<String, String> uniqueNames) throws IOException,
			OWLOntologyCreationException,
			OWLOntologyStorageException
	{
		return convertToOWL(inputPath, outputPath, null, null, uniqueNames);
	}
	
	/**
	 * Read a data file, create an OWL representation, and save an OWL file.
	 * Create alternate identifiers from the merge.dmp file information
	 *
	 * @param inputPath the path to the input data file (e.g. taxonomy.dat)
	 * @param outputPath the path to the output OWL file
	 *	(e.g. ncbi_taxonomy.owl).
	 * @param mergeInfo the input stream of the merged information
	 * @param citationInfo the input stream of the citation information
	 * @param uniqueNames
	 * @return OWL ontology
	 * @throws IOException if the paths do not resolve
	 * @throws OWLOntologyCreationException if OWLAPI fails to create an
	 *	empty ontology
	 * @throws OWLOntologyStorageException if OWLAPI can't save the file
	 */
	public static OWLOntology convertToOWL(String inputPath,
			String outputPath, InputStream mergeInfo,
			InputStream citationInfo,
			Map<String, String> uniqueNames) throws IOException,
			OWLOntologyCreationException,
			OWLOntologyStorageException {
		File outputFile = new File(outputPath);
		IRI outputIRI = IRI.create(outputFile);
		OWLOntology ontology = convertToOWL(inputPath, uniqueNames);
		
		if (mergeInfo != null) {
			addAltIds(ontology, mergeInfo);
		}
		
		if (citationInfo != null) {
			addCitationInfo(ontology, citationInfo);
		}
		
		logger.debug("Saving ontology...");

		ontology.getOWLOntologyManager().saveOntology(
			ontology, outputIRI);
		return ontology;
	}
	
	/**
	 * Extract a map of unique names from the names.dmp input stream. The map
	 * contains only the values from the unique name column for the type of
	 * "scientific name".
	 * 
	 * @param nameInfo
	 * @return unique names
	 * @throws IOException
	 */
	static Map<String, String> loadUniqueNames(InputStream nameInfo) throws IOException {
		Map<String, String> uniqueNames = new HashMap<String, String>();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(nameInfo));
		try {
			// read stream
			String line;
			while((line = reader.readLine()) != null) {
				List<String> split = splitDmpLine(line);
				if (split != null && split.size() > 3) {
					String id = split.get(0);
					String uniqueName = split.get(2);
					String type = split.get(3);
					if (id != null && uniqueName != null && type != null && "scientific name".equals(type)) {
						uniqueNames.put(id, uniqueName);
					}
				}
			}
		}
		finally {
			reader.close();
		}
		return uniqueNames;
	}
	
	/**
	 * Split the string at the separator '|'. Return empty string and strings
	 * containing only whitespaces as null.
	 * 
	 * @param line
	 * @return list of values
	 * 
	 * This method is package private for testing purposes.
	 */
	static List<String> splitDmpLine(String line) {
		List<String> list = new ArrayList<String>();
		int start = 0;
		for (int i = 0; i < line.length(); i++) {
			char c= line.charAt(i);
			if ('|' == c) {
				String field = line.substring(start, i);
				// trim to null
				field = field.trim();
				final int length = field.length();
				if (length == 0 || (length == 1 && Character.isWhitespace(field.charAt(0)))) {
					field = null;
				}
				// add to list, do NOT skip null values
				list.add(field);
				
				// update index
				start = i + 1;
			}
		}
		return list;
	}
	
	/**
	 * Extract literature citation information from the citation.dmp stream and add them to the ontology.
	 * Currently extract PMIDs only.
	 * 
	 * @param ontology
	 * @param citationInfo
	 * @throws IOException
	 */
	private static void addCitationInfo(OWLOntology ontology, InputStream citationInfo) throws IOException {
		logger.debug("Adding citation information.");
		final BufferedReader reader = new BufferedReader(new InputStreamReader(citationInfo));
		try {
			String line;
			while((line = reader.readLine()) != null) {
				List<String> split = splitDmpLine(line);
				if (split != null && split.size() >= 7) {
					String pubmed_id = split.get(2);
					String medline_id = split.get(3);
					String taxon_list = split.get(6); // whitespace separate list of taxon ids
					if ((pubmed_id != null || medline_id != null) && taxon_list != null) {
						String value = null;
						// "0" denotes no information
						if (pubmed_id != null && "0".equals(pubmed_id) == false) {
							value = pubmed_id;
						}
						else if (medline_id != null && "0".equals(medline_id) == false) {
							value = medline_id;
						}
						
						if (value != null) {
							for(String taxon : splitTaxonList(taxon_list)) {
								
								// get OWLClass 
								IRI iri = createNCBIIRI(taxon);
								OWLClass cls = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
								// check that the class exists, i.e. is declared
								if (ontology.getDeclarationAxioms(cls).isEmpty() == false) {
									// add xref
									annotate(ontology, cls , "oio:hasDbXref", "PMID:"+value);
								}
							}
						}
					}
				}
			}
		}
		finally {
			reader.close();
		}
	}
	
	
	/**
	 * split a string into substring using whitespaces as separator.
	 * Assumes that there are no leading or trailing whitespaces.
	 * 
	 * @param string
	 * @return list of taxons (never null)
	 */
	static List<String> splitTaxonList(String string) {
		List<String> list = new ArrayList<String>();
		int start = 0;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (Character.isWhitespace(c)) {
				final String substring = string.substring(start, i);
				if (substring.length() > 0) {
					list.add(substring);
				}
				start = i + 1;
			}
		}
		if (start < (string.length() - 1)) {
			list.add(string.substring(start));
		}
		return list;
	}
	
	private static void addAltIds(OWLOntology ontology, InputStream mergeInfo) throws IOException {
		logger.debug("Adding alternative identifiers from merge information.");
		final BufferedReader reader = new BufferedReader(new InputStreamReader(mergeInfo));
		try {
			// OWL stuff
			OWLAnnotationProperty ap = NCBIOWL.setupAltIdProperty(ontology);

			// read stream
			String line;
			while((line = reader.readLine()) != null) {
				line = line.trim();
				// minimum length 4, expected format:
				// [0-9]+\s*\|[0-9]+\s*\|
				// number(aka obsolete/alt id) whitespaces* pipe number(aka merged) whitespace* pipe
				String altIdString = null;
				String merged = null;

				if (line.length() >= 4) {
					int start = 0;
					for (int i = 0; i < line.length(); i++) {
						char c= line.charAt(i);
						if ('|' == c) {
							if (start == 0) {
								altIdString = line.substring(start, i);
								start = i + 1;
							}
							else {
								merged = line.substring(start, i);
								break;
							}
						}
						else if (Character.isWhitespace(c) || Character.isDigit(c)) {
							// expected characters
							// read until pipe symbol
						}
						else {
							// unexpected character
							break;
						}
					}
				}
				if (altIdString != null && merged != null) {
					NCBIOWL.addAltId(ontology, merged, altIdString, ap);
				}
				else {
					logger.warn("Could not parse line in merge info: "+line);
				}
			}
		}
		finally {
			reader.close();
		}
	}

	/**
	 * Handle one line of the data file. There are several cases:
	 *
	 * <ol>
	 * <li>separator (//): return null</li>
	 * <li>ID: create a new taxon (OWLClass)<li>
	 * <li>PARENT ID: add the parent as the superclass of this taxon</li>
	 * <li>RANK: add a rank annotation to the right IRI</li>
	 * <li>GC ID: add a DbXref</li>
	 * <li>MGC ID: do nothing (TODO)</li>
	 * <li>INCLUDE: do nothing (TODO)</li>
	 * <li>Synonyms: add a synonym annotation</li>
	 * <ul>
	 *
	 * @param ontology the current ontology
	 * @param uniqueLabels map of unique labels as extracted from the dmp files
	 * @param labels a list to check that labels are unique
	 * @param taxon the current class or null
	 * @param line the line to handle
	 * @param lineNumber for logging purposes
	 * @return null or the taxon for the next line
	 */
	protected static OWLClass handleLine(OWLOntology ontology,
			Map<String, String> uniqueLabels,
			HashSet<String> labels, OWLClass taxon,
			String line, int lineNumber) {
		if (line.trim().equals("//")) { return taxon; }

		// Get the field name and value for the line, or return.
		String[] result = parseLine(line, lineNumber);
		if (result == null) { return taxon; }
		String fieldName = result[0];
		String fieldValue = result[1];

		// Create a new class from an ID and return it.
		if (fieldName.equals("id")) {
			// Do a check for missing labels before leaving this taxon.
			if (taxon != null) {
				checkTaxon(ontology, taxon);
			}
			return createTaxon(ontology, fieldValue);
		}

		// If no taxon is defined, we have a problem.
		if (taxon == null) {
			logger.error("Null taxon for line " + lineNumber
					+ ": " + line);
			return null;
		}

		// Handle the other fieldNames.
		if (fieldName.equals("parent id")) {
			// Do not add NCBITaxon_0 as a parent
			if (!fieldValue.equals("0")) {
				assertSubClass(ontology, taxon, fieldValue);
			}
		} else if (fieldName.equals("rank")) {
			// Ignore "no rank"
			if(!fieldValue.equals("no rank")) {
				String name = reformatName(fieldValue);
				annotate(ontology, taxon, "ncbitaxon:has_rank",
					IRI.create(NCBI + name));
				// Warn if rank is not in the list of ranks.
				if (!NCBIOWL.ranks.contains(fieldValue)) {
					logger.warn("Unrecognized RANK '" +
						fieldValue +"' on line " +
						lineNumber);
				}
			}
		} else if (fieldName.equals("scientific name")) {
			// handle the scientific name
			// if there is a unique name us it and add the original value as exact synonym
			final String id = getTaxonID(taxon);
			String uniqueLabel = null;
			if (uniqueLabels != null) {
				uniqueLabel = uniqueLabels.get(id);
			}
			String label = fieldValue;
			if (uniqueLabel != null) {
				annotate(ontology, taxon, "rdfs:label", uniqueLabel);
				labels.add(uniqueLabel);
				synonym(ontology, taxon, "ncbitaxon:scientific_name", "oio:hasExactSynonym", label);
			}
			else {
				if (labels.contains(label)) {
					label = label + " [NCBITaxon:" + id + "]";
				}
				annotate(ontology, taxon, "rdfs:label", label);
				labels.add(label);
			}
		} else if (fieldName.equals("includes")) { // TODO: handle?
		} else if (fieldName.equals("gc id")) {
			String value = "GC_ID:" + fieldValue;
			annotate(ontology, taxon, "oio:hasDbXref", value);
		} else if (fieldName.equals("mgc id")) { // TODO: handle?
		} else if (NCBIOWL.synonymTypes.containsKey(fieldName)) {
			String typeCURIE = reformatName(
				"ncbitaxon:" + fieldName);
			String propertyCURIE =
				NCBIOWL.synonymTypes.get(fieldName);
			synonym(ontology, taxon, typeCURIE, propertyCURIE,
				fieldValue);
		} else {
			logger.error("Unknown field name '" + fieldName +
				"' for line " + lineNumber + ": " + line);
		}

		return taxon;
	}
	
	/**
	 * Parse a line of the data file into a pair of strings:
	 * the field name and the field value. Lines are expected to be in two
	 * parts, separated by a colon. Leading and trailing whitespace
	 * is trimmed.
	 *
	 * @param line the line to be parsed
	 * @param lineNumber used for logging purposes
	 * @return either null or the pair of the field name (lower case)
	 *	and field value
	 */
	public static String[] parseLine(String line, int lineNumber) {
		String[] parts = line.split(":", 2);
		if (parts.length == 2 &&
			parts[0].trim().length() > 0 &&
			parts[1].trim().length() > 0) {
			return new String[] {
				parts[0].trim().toLowerCase(),
				parts[1].trim()
			};
		}
		else {
			logger.warn("Bad line " + lineNumber + ": " + line);
			return null;
		}
	}
	
	/**
	 * Load an ontology from a file and then print a list of its axioms.
	 *
	 * @param inputPath the path for the input ontology file
	 * @param outputPath the path for the output file
	 * @throws IOException if it cannot write to the outputPath
	 * @throws OWLOntologyCreationException if OWLAPI can't read the
	 *	ontology file
	 */
	public static void checkAxioms(String inputPath, String outputPath)
			throws IOException, OWLOntologyCreationException {
		File inputFile = new File(inputPath);
		OWLOntology ontology = NCBIOWL.loadOWLOntology(inputFile);
		logger.debug("Loaded ontology. Axioms: " +
			ontology.getAxiomCount());
		printAxioms(ontology, outputPath);
	}

	/**
	 * Print string representations of all the axioms in an ontology,
	 * one axiom statement per line. Sorted lists of axioms can be used
	 * to compare ontologies.
	 *
	 * @param ontology the ontology to print
	 * @param outputPath the path for the output file
	 * @throws IOException if it cannot write to the outputPath
	 */
	public static void printAxioms(OWLOntology ontology, String outputPath)
			throws IOException {
		logger.debug("Printing axioms...");
		java.util.Set<OWLAxiom> axioms = ontology.getAxioms();
		Iterator<OWLAxiom> iterator = axioms.iterator();

		FileWriter fw = new FileWriter(outputPath);
		BufferedWriter bw = new BufferedWriter(fw);

		while(iterator.hasNext()) {
			OWLAxiom axiom = iterator.next();
			bw.write(axiom.toString() + "\n");
		}
		bw.close();
	}

}
