package owltools.sim2;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math.MathException;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.io.OWLPrettyPrinter;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
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
			OWLAPIObo2Owl bridge = new OWLAPIObo2Owl(m);
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
	
	public static void compareLCSWithEnrichmentGrouping(OWLOntology ont, Double pthresh, OWLPrettyPrinter owlpp) throws UnknownOWLClassException, MathException {
		OwlSimFactory osf = new FastOwlSimFactory();
		OwlSim sim = osf.createOwlSim(ont);
		sim.createElementAttributeMapFromOntology();
		OWLClass populationClass = null;
		if (pthresh == null)
			pthresh = 0.01;
		int nHypotheses = ((sim.getAllAttributeClasses().size()-1) * sim.getAllAttributeClasses().size())/2;
		for (OWLClass c : sim.getAllAttributeClasses()) {
			for (OWLClass d : sim.getAllAttributeClasses()) {
				if (c.compareTo(d) > 0) {
					ScoreAttributeSetPair lcsic = sim.getLowestCommonSubsumerWithIC(c, d);
					//System.out.println("TEST: "+c+" "+d);
					EnrichmentResult enr = sim.calculatePairwiseEnrichment(populationClass, c, d);
					if (enr == null) {
						//System.err.println("NULL: "+c+" "+d);
						continue;
					}
					double pvc = enr.pValue * nHypotheses;
					if (pvc < pthresh) {
						// TODO - place this in OwlSim
						Set<OWLNamedIndividual> inds = 
								new HashSet<OWLNamedIndividual>(sim.getElementsForAttribute(c));
						inds.addAll(sim.getElementsForAttribute(d));
						double pu = inds.size() / (double)sim.getCorpusSize();
						double ic = - Math.log(pu) / Math.log(2);
						double foldChange = ic / lcsic.score;
						if (foldChange < 1.5)
							continue;
						if (lcsic.attributeClassSet.size() == 0) {
							System.err.println("# no LCS for "+c+" "+d);
							continue;
						}
						System.out.println(owlpp.render(c)+"\t"+owlpp.render(d)+"\t"+pvc+"\t"+owlpp.render(lcsic.getArbitraryAttributeClass())+"\t"+lcsic.score+"\t"+ic+"\t"+foldChange);
					}
				}
			}
			
			
		}
	}
	
}
