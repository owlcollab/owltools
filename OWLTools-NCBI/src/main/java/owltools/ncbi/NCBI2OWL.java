package owltools.ncbi;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.ncbi.NCBIOWL;
import owltools.ncbi.NCBIConverter;

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
		"usage: ncbi-converter [-ca] <input.dat> <output.owl> [axioms.txt]\n\n" +
		"       -c   Convert to OWL.\n" +
		"       -a   Print axiom list.\n" +
		"       -ca  Convert and print axiom list.";


	/**
	 * Handle command-line arguments when this class is used in an
	 * executable Jar file. See <code>usage</code>.
	 *
	 * @param args one or more strings to provide options and paths
	 */
	public static void main(String[] args) {
		try {
			switch (args.length) {
				case 0:
					convertToOWL("taxonomy.dat",
						"ncbitaxon.owl");
					return;
				case 2:
					convertToOWL(args[0], args[1]);
					return;
				case 3: 
					if (args[0].equals("-c")) {
						convertToOWL(args[1], args[2]);
						return;
					} else if (args[0].equals("-a")) {
						checkAxioms(args[1], args[2]);
						return;
					}
				case 4:
					if (args[0].equals("-ca")) {
						OWLOntology ontology =
							convertToOWL(args[1],
								args[2]);
						printAxioms(ontology, args[3]);
						return;
					}
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.toString());
			System.out.println(usage);
		}
		System.out.println(usage);
	}

	/**
	 * Read a data file and create an OWL representation.
	 *
	 * @param inputPath the path to the input data file (e.g. taxonomy.dat)
	 * @return OWL ontology
	 * @throws IOException if the paths do not resolve
	 * @throws OWLOntologyCreationException if OWLAPI fails to create an
	 *	empty ontology
	 * @throws OWLOntologyStorageException if OWLAPI can't save the file
	 */
	public static OWLOntology convertToOWL(String inputPath)
			throws IOException, OWLOntologyCreationException,
			OWLOntologyStorageException
	{
		// Create the ontology.
		OWLOntology ontology = NCBIOWL.createOWLOntology();

		// Read the input file.
		File file = new File(inputPath);
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(
			new InputStreamReader(fis));

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
			taxon = handleLine(ontology, labels, taxon,
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
	}

	/**
	 * Read a data file, create an OWL representation, and save an OWL file.
	 *
	 * @param inputPath the path to the input data file (e.g. taxonomy.dat)
	 * @param outputPath the path to the output OWL file
	 *	(e.g. ncbi_taxonomy.owl).
	 * @return OWL ontology
	 * @throws IOException if the paths do not resolve
	 * @throws OWLOntologyCreationException if OWLAPI fails to create an
	 *	empty ontology
	 * @throws OWLOntologyStorageException if OWLAPI can't save the file
	 */
	public static OWLOntology convertToOWL(String inputPath,
			String outputPath) throws IOException,
			OWLOntologyCreationException,
			OWLOntologyStorageException {
		File outputFile = new File(outputPath);
		IRI outputIRI = IRI.create(outputFile);
		OWLOntology ontology = convertToOWL(inputPath);

		logger.debug("Saving ontology...");

		ontology.getOWLOntologyManager().saveOntology(
			ontology, outputIRI);
		return ontology;
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
	 * @param labels a list to check that labels are unique
	 * @param taxon the current class or null
	 * @param line the line to handle
	 * @param lineNumber for logging purposes
	 * @return null or the taxon for the next line
	 */
	protected static OWLClass handleLine(OWLOntology ontology,
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
			String label = fieldValue;
			if (labels.contains(label)) {
				String id = getTaxonID(taxon);
				label = label + " [NCBITaxon:" + id + "]";
			}
			annotate(ontology, taxon, "rdfs:label", label);
			labels.add(label);
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
