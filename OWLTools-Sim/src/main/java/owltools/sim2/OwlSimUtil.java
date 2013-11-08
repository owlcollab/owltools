package owltools.sim2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.mooncat.TransformationUtils;

/**
 * Utility methods wrapping OwlSim
 * 
 * @author cjm
 *
 */
public class OwlSimUtil {
	
	/**
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
	
}
