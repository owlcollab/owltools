package owltools.sim2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.mooncat.TransformationUtils;
import owltools.vocab.OBOUpperVocabulary;

/**
 * Utility methods wrapping OwlSim
 * 
 * Most of this functionality duplicates code found elsewhere in OWLTools,
 * but using these static methods may be more straightforward.
 * 
 * The most convenient starting point is {@lin #createOwlSimFromOntologyFiles(String, Set, Set)}
 * 
 * @author cjm
 *
 */
public class OwlSimUtil {

	/**
	 * 
	 * @param ontologyFile
	 * @param assocFiles
	 * @param labelFiles 
	 * @return OwlSim object, ready for action
	 * @throws OWLOntologyCreationException
	 * @throws OBOFormatParserException
	 * @throws IOException
	 * @throws UnknownOWLClassException 
	 */
	public static OwlSim createOwlSimFromOntologyFiles(String ontologyFile, 
			Set<String> assocFiles,
			Set<String> labelFiles) throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		OwlSim owlsim = createOwlSimFromOntologyFile(ontologyFile);
		for (String f : assocFiles) {
			addElementToAttributeAssociationsFromFile(owlsim.getSourceOntology(), f);
		}
		for (String f : labelFiles) {
			addElementLabels(owlsim.getSourceOntology(), f);
		}
		owlsim.createElementAttributeMapFromOntology();
		return owlsim;
	}

	

	/**
	 * Generates an OwlSim object from an ontology file
	 * 
	 * does *not* call 
	 * 
	 * @param filepath
	 * @return
	 * @throws OWLOntologyCreationException
	 * @throws IOException 
	 * @throws OBOFormatParserException 
	 */
	public static OwlSim createOwlSimFromOntologyFile(String filepath) throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		return createOwlSimFromOntologyFile(new File(filepath));
	}
	
	/**
	 * @param file
	 * @return
	 * @throws OWLOntologyCreationException
	 * @throws IOException 
	 * @throws OBOFormatParserException 
	 */
	public static OwlSim createOwlSimFromOntologyFile(File file) throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLOntology ont;
		if (file.getPath().endsWith(".obo")) {
			OBOFormatParser p = new OBOFormatParser();
			OBODoc obodoc = p.parse(file);
			Obo2Owl bridge = new Obo2Owl(m);
			ont = bridge.convert(obodoc);
		}
		else {
			ont = m.loadOntology(IRI.create(file));
		}
		
		return new FastOwlSimFactory().createOwlSim(ont);
	}
	
	private static void addElementToAttributeAssociationsFromFile(
			OWLOntology ont, String filepath) throws IOException {
		 addElementToAttributeAssociationsFromFile(ont, new File(filepath));
	}
	
	public static void addElementToAttributeAssociationsFromFile(OWLOntology ont, File file) throws IOException {
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = m.getOWLDataFactory();
		List<String> lines = FileUtils.readLines(file);
		for (String line : lines) {
			if (line.startsWith("#"))
				continue;
			String[] colVals = line.split("\t");
			if (colVals.length != 2) {
				throw new IOException("Incorrect number of value: "+line);
			}
			OWLNamedIndividual i = df.getOWLNamedIndividual(getIRIById(colVals[0]));
			OWLClass c = df.getOWLClass(getIRIById(colVals[1]));
			m.addAxiom(ont, df.getOWLClassAssertionAxiom(c, i));
		}
	}
	public static void addElementLabels(OWLOntology ont, String filepath) throws IOException {
		addElementLabels(ont, new File(filepath));
	}
	
	public static void addElementLabels(OWLOntology ont, File file) throws IOException {
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = m.getOWLDataFactory();
		List<String> lines = FileUtils.readLines(file);
		for (String line : lines) {
			if (line.startsWith("#"))
				continue;
			String[] colVals = line.split("\t");
			if (colVals.length != 2) {
				throw new IOException("Incorrect number of value: "+line);
			}
			IRI i = getIRIById(colVals[0]);
			OWLAnnotation ann = df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral(colVals[1])); 
			m.addAxiom(ont, df.getOWLAnnotationAssertionAxiom(i, ann));
		}
	}
	
//	public OWLNamedIndividiual getElementById(OwlSim sim, String id) {
//		return sim.getSourceOntology();
//	}

	private static IRI getIRIById(String id) {
		return IRI.create(OBOUpperVocabulary.OBO + id.replace(":", "_"));
	}
	
}
