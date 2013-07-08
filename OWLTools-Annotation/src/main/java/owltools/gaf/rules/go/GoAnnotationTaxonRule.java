package owltools.gaf.rules.go;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.rules.AnnotationTaxonRule;
import owltools.graph.OWLGraphWrapper;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Checks if an annotation is valid according to GO taxon constraints.
 * 
 */
public class GoAnnotationTaxonRule extends AnnotationTaxonRule {
	
	private static final Logger LOG = Logger.getLogger(GoAnnotationTaxonRule.class);
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.AnnotationTaxonRule";
	
	private final String unsatifiableModule;

	public GoAnnotationTaxonRule(OWLGraphWrapper graph, String unsatifiableModule) {
		super(graph);
		this.unsatifiableModule = unsatifiableModule;
		if (unsatifiableModule != null) {
			File file = new File(unsatifiableModule);
			FileUtils.deleteQuietly(file);
		}
	}

	@Override
	protected void handleUnsatisfiable(Set<OWLClass> unsatisfiable, OWLOntology ont) {
		if (unsatifiableModule == null) {
			// do nothing
			return;
		}
		{
			LOG.info("Creating module for unsatisfiable classes in taxon rule.");
			// create a new manager, re-use factory
			// avoid unnecessary change events
			final OWLOntologyManager m = OWLManager.createOWLOntologyManager(ont.getOWLOntologyManager().getOWLDataFactory());
			
			// extract module
			SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(m, ont, ModuleType.BOT);
			Set<OWLEntity> sig = new HashSet<OWLEntity>(unsatisfiable);
			Set<OWLAxiom> moduleAxioms = sme.extract(sig);
			
			// save module
			OutputStream moduleOutputStream = null;
			try {
				OWLOntology module = m.createOntology(IRI.generateDocumentIRI());
				m.addAxioms(module, moduleAxioms);
				LOG.info("Writing module for unsatisfiable classes to file: "+unsatifiableModule);
				moduleOutputStream = new FileOutputStream(unsatifiableModule);
				m.saveOntology(module, moduleOutputStream);
				LOG.info("Finished writing unsatisfiable module for taxon rule.");
			} catch (OWLException e) {
				LOG.warn("Could not create module for unsatisfiable classes.", e);
			} catch (IOException e) {
				LOG.warn("Could not write module for unsatisfiable classes.", e);
			}
			finally {
				IOUtils.closeQuietly(moduleOutputStream);
			}
		}
	}
	
}

