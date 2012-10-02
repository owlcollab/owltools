package owltools.sim.preprocessor;

import java.util.Properties;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.io.OWLPrettyPrinter;


/**
 * An ontology pre-processor will take an input ontology, apply a series of transforms, and generate
 * an output ontology
 * 
 * The output ontology may in many cases be the same as the input ontology
 * 
 * @author cjm
 *
 */
public interface SimPreProcessor {
	
	/**
	 * Generate output ontology
	 */
	public void preprocess();
	
	/**
	 * The pre-processor may generate view classes. For example, it may generate a class equivalent to "P some C" for every input
	 * class C and some property P.
	 * 
	 * This method returns all the view classes generated for any given input ontology class
	 * 
	 * @param class
	 * @return
	 */
	public Set<OWLClass> getViewClasses(OWLClass c);
	
	/**
	 * @return input ontology
	 */
	public OWLOntology getInputOntology();
	
	/**
	 * sets the input ontology. Should be done prior to pre-processing
	 * 
	 * @param inputOntology
	 */
	public void setInputOntology(OWLOntology inputOntology);

	/**
	 * @return output ontology
	 */
	public OWLOntology getOutputOntology();

	/**
	 * sets the output ontology. Should be done prior to pre-processing.
	 * 
	 * Note that this can have the same value as the input ontology. In this case all new
	 * declarations and axioms are added to the same ontology
	 * 
	 * @param inputOntology
	 */
	public void setOutputOntology(OWLOntology outputOntology);
	
	/**
	 * Sets the reasoner factory. If this is called, then setReasoner() does not need to be explicitly called
	 * 
	 * @param reasonerFactory
	 */
	public void setReasonerFactory(OWLReasonerFactory reasonerFactory);

	/**
	 * @return reasoner object. Typically an Elk instance
	 */
	public OWLReasoner getReasoner();

	/**
	 * Sets the reasoner. The reasoner should have been instantiated with the output ontology 
	 * 
	 * @param reasoner
	 */
	public void setReasoner(OWLReasoner reasoner);
	
	/**
	 * Returns the LCS of a and b. If a and b have multiple named LCSs, then the LCS is the class intersection
	 * of all these
	 * 
	 * @param a
	 * @param b
	 * @return LCS
	 */
	public OWLClassExpression getLowestCommonSubsumer(OWLClassExpression a, OWLClassExpression b);

	public void setOWLPrettyPrinter(OWLPrettyPrinter owlpp);

	public void setSimProperties(Properties simProperties);
	
	public OWLObjectProperty getAboxProperty();


}
