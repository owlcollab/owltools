package owltools.gaf.inference;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;

/**
 * Matrix representing the applicability of classes and taxa. Uses the
 * {@link TaxonConstraintsEngine} to infer the taxon constraints.
 * 
 * Use the static methods to create and write instances.
 */
public class ClassTaxonMatrix {
	
	private static final Logger LOG = Logger.getLogger(ClassTaxonMatrix.class);

	private final Map<OWLClass, Integer> matrixIndicies;
	private final Map<OWLClass, Integer> matrixTaxonIndicies;
	private final boolean[][] matrix;

	private int classCount = 0;
	private int taxonCount = 0;

	protected ClassTaxonMatrix(int classCount, int taxonCount) {
		matrixIndicies = new HashMap<OWLClass, Integer>();
		matrixTaxonIndicies = new HashMap<OWLClass, Integer>();
		matrix = new boolean[classCount][taxonCount];
	}

	protected void add(boolean applicable, OWLClass owlClass, OWLClass taxon) {
		Integer classIndex = matrixIndicies.get(owlClass);
		if (classIndex == null) {
			classIndex = Integer.valueOf(classCount);
			classCount++;
			matrixIndicies.put(owlClass, classIndex);
		}
		Integer taxonIndex = matrixTaxonIndicies.get(taxon);
		if (taxonIndex == null) {
			taxonIndex = Integer.valueOf(taxonCount);
			taxonCount++;
			matrixTaxonIndicies.put(taxon, taxonIndex);
		}
		matrix[classIndex.intValue()][taxonIndex.intValue()] = applicable;
	}
	
	public Boolean get(OWLClass cls, OWLClass taxon) {
		Integer taxonIndex = matrixTaxonIndicies.get(taxon);
		if (taxonIndex == null) {
			return null;
		}
		Integer classIndex = matrixIndicies.get(cls);
		if (classIndex == null) {
			return null;
		}
		return Boolean.valueOf(matrix[classIndex.intValue()][taxonIndex.intValue()]);
	}

	/**
	 * Create a new taxon-class-matrix for the given classes and taxa.
	 * 
	 * @param graph
	 * @param classes
	 * @param taxa taxa identifier
	 * @return matrix
	 */
	public static ClassTaxonMatrix create(OWLGraphWrapper graph, Collection<OWLClass> classes, String...taxa) {
		List<OWLClass> taxaClasses = new ArrayList<OWLClass>(taxa.length);
		for(String taxon : taxa) {
			OWLClass c = graph.getOWLClassByIdentifier(taxon);
			if (c != null) {
				taxaClasses.add(c);
			}
			else {
				LOG.info("No class found for taxon: "+taxon);
			}
		}
		return create(graph, classes, taxaClasses );
	}
	
	/**
	 * Create a new taxon-class-matrix for the given classes and taxa.
	 * 
	 * @param graph
	 * @param classes
	 * @param taxa
	 * @return matrix
	 */
	public static ClassTaxonMatrix create(OWLGraphWrapper graph, Collection<OWLClass> classes, Collection<OWLClass> taxa) {
		ClassTaxonMatrix matrix = new ClassTaxonMatrix(classes.size(), taxa.size());
		TaxonConstraintsEngine engine = new TaxonConstraintsEngine(graph);
		for (OWLClass owlClass : classes) {
			for (OWLClass taxon : taxa) {
				boolean applicable = engine.isClassApplicable(owlClass, taxon);
				matrix.add(applicable, owlClass, taxon);
			}
		}
		return matrix;
	}

	/**
	 * Write a {@link ClassTaxonMatrix} using the default delimiter.
	 * 
	 * Hint: Do not forget to close the writer after use.
	 * 
	 * @param matrix
	 * @param writer
	 * @throws IOException
	 */
	public static void write(ClassTaxonMatrix matrix, BufferedWriter writer) throws IOException {
		// default delimiter is a tab
		write(matrix, '\t', writer);
	}

	/**
	 * Write a {@link ClassTaxonMatrix} using the specified delimiter.
	 * 
	 * Hint: Do not forget to close the writer after use.
	 * 
	 * @param matrix
	 * @param delimiter
	 * @param writer
	 * @throws IOException
	 */
	public static void write(ClassTaxonMatrix matrix, char delimiter, BufferedWriter writer) throws IOException {

		// create sorted list of classes and taxa
		List<OWLClass> sortedClasses = createSortedClasses(matrix.matrixIndicies);
		List<OWLClass> sortedTaxa = createSortedClasses(matrix.matrixTaxonIndicies);

		// write header
		for (OWLClass taxon : sortedTaxa) {
			writer.append(delimiter);
			writer.append(taxon.getIRI().toString());
		}
		writer.newLine();

		// write lines
		for (int i = 0; i < sortedClasses.size(); i++) {
			writer.append(sortedClasses.get(i).getIRI().toString());
			for (boolean b : matrix.matrix[i]) {
				writer.append(delimiter);
				writer.append(Boolean.toString(b));
			}
			writer.newLine();
		}

		// flush
		writer.flush();
	}

	private static List<OWLClass> createSortedClasses(final Map<OWLClass, Integer> map) {
		List<OWLClass> list = new ArrayList<OWLClass>(map.keySet());
		Collections.sort(list, new Comparator<OWLClass>() {

			@Override
			public int compare(OWLClass c1, OWLClass c2) {
				Integer i1 = map.get(c1);
				Integer i2 = map.get(c2);
				return i1.compareTo(i2);
			}
		});
		return list;
	}
}
