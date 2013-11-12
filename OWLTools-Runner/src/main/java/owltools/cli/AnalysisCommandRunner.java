package owltools.cli;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



import org.apache.log4j.Logger;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.util.MinimalModelGenerator;

import owltools.cli.tools.CLIMethod;



/**
 * An instance of this class can execute owltools commands in sequence.
 * 
 * Typically, this class is called from a wrapper within its main() method.
 * 
 * Extend this class to implement additional functions. Use the {@link CLIMethod} 
 * annotation, to designate the relevant methods.
 * 
 * @author nlw
 *
 */
public class AnalysisCommandRunner extends Sim2CommandRunner {

	private static Logger LOG2 = Logger.getLogger(AnalysisCommandRunner.class);

  public Map<String, Set<OWLClass>> permutedElementMap = null;

	
	public OWLGraphWrapper g = null;
	public OWLOntology queryOntology = null;
	public boolean exitOnException = true;
	public boolean isDisposeReasonerOnExit = true;

	public OWLReasoner reasoner = null;
	public String reasonerName = "hermit";
	public Set<OWLObject> owlObjectCachedSet = null;

	Map<OWLClass,OWLClassExpression> queryExpressionMap = null;
	MinimalModelGenerator mmg = null;

	protected ParserWrapper pw = new ParserWrapper();

	/**
	 * Use {@link #getPrettyPrinter()} to access a pre-configured or the default pretty printer.
	 */
	private OWLPrettyPrinter owlpp;
	private String prettyPrinterFormat = null;
	private boolean prettyPrinterHideIds = false;

	public class OptionException extends Exception {

		// generated
		private static final long serialVersionUID = 8770773099868997872L;

		public OptionException(String msg) {
			super(msg);
		}

	}

	protected void exit(int code) {

		// if we are using this in a REPL context (e.g. owlrhino), we don't want to exit the shell
		// on an error - reporting the error is sufficient
		if (exitOnException)
			System.exit(code);
	}


	public List<String> parseArgString(String str) {
		List<String> args = new ArrayList<String>();
		int p = 0;
		StringBuffer ns = new StringBuffer();
		while (p < str.length()) {
			if (str.charAt(p) == ' ') {
				if (ns.length() > 0) {
					args.add(ns.toString());
					ns = new StringBuffer();
				}
			}
			else {
				ns.append(str.charAt(p));
			}
			p++;
		}
		if (ns.length() > 0) {
			args.add(ns.toString());
		}		
		return args;
	}


	@CLIMethod("--generate-simulated-reduction-data")
	public void createSimulatedData(Opts opts) throws Exception {
		//to be run after --load-instances
		//first version of this will make permutations by simply removing 
		//the annotated classes
		//it will print the results to the output file directly, rather than
		//creating the new individuals and storing them in the graph.  i 
		//find this easier, initially

		String outputFile = null;

		//check arguments
		while (opts.hasOpts()) {
			if (opts.nextEq("-o")) {
				outputFile = opts.nextOpt();
			}
			//TODO: add some optional flags for lifting just single levels or only in a single hierarchy
			else {
				break;
			}
		}

		fsimTest(opts);
/*		OwlSim owlsim = null; // simple owlsim

		//trying this for now
		loadProperties(opts);

		OWLOntology gprime =   g.getSourceOntology();
		owlsim = owlSimFactory.createOwlSim(gprime);

		//sos.setSimProperties(simProperties);
		owlsim.createElementAttributeMapFromOntology();

*/
		Set<OWLNamedIndividual> insts = owlsim.getAllElements();

		//iterate through all of the entities, and make a simulated
		//set for each
		for (OWLNamedIndividual i : insts) {
			//TODO: change this to an identifier rather than IRI
			String id = i.getIRI().toString(); 
			Set<OWLClass> atts = owlsim.getAttributesForElement(i);
			getCombinations(id, atts);
		}
	}		


  public void getCombinations(String id, Set<OWLClass> atts) { 
  	Set<OWLClass> prefixAtts = new HashSet<OWLClass>();
  	getCombinations(id, prefixAtts, atts, 0); 
  }
  
  private static void getCombinations(String id, Set<OWLClass> attsPrefix, Set<OWLClass> atts, int counter) {
		LOG2.info("Hello");
    	counter++;
    	id = id.concat(".").concat(Integer.toString(counter));
			LOG2.info(id +"\t"+ attsPrefix.toString());
			System.out.println("Hello");

      Object[] attsA = atts.toArray();
      
      for (int i = 0; i < attsA.length; i++) {
      	Set<OWLClass> nextAtts = new HashSet<OWLClass>();
 
      	//make the subset i+1..end
      	for (int j=i+1; j < attsA.length; j++) {
      		nextAtts.add((OWLClass) attsA[j]);
      	}
      	attsPrefix.add((OWLClass) attsA[i]);
      	getCombinations(id,attsPrefix, nextAtts, counter);

      }  
  }
 

	

	/*
	@CLIMethod("--generate-simulated-leaveoneout-data")
	public void createLeaveOneOutSimulatedData(Opts opts) throws Exception {

    String outputFile = null;

		//check arguments
		while (opts.hasOpts()) {
      if (opts.nextEq("-o")) {
				outputFile = opts.nextOpt();
			}
			//TODO: add some optional flags for lifting just single levels or only in a single hierarchy
			else {
				break;
			}
		}
		
		Map<OWLNamedIndividual, Set<OWLClass>> elementToAttributesMap = owlsim.getElementToAttributesMap();
		Map<OWLNamedIndividual, Set<OWLClass>> permutedElementToAttributesMap = new HashMap<OWLNamedIndividual, Set<OWLClass>>();
		
		//iterate through all of the individuals, and make a simulated
		//set for each by making permutations that drop just one of
		//the annotations

		for (OWLNamedIndividual i : elementToAttributesMap.keySet()) {
			//get all the Permutations
			permutedElementToAttributesMap.putAll(getLeaveOneOutPermutations(i,elementToAttributesMap.get(i)));
			dumpElementToAttributeMap(permutedElementToAttributesMap);
		}
	}
	
//	create IRI, create factory (get)Indiidual, declare individual, ask the factory to give it to me
	
	
	private Map<OWLNamedIndividual, Set<OWLClass>> getLeaveOneOutPermutations(OWLNamedIndividual i, Set<OWLClass> atts) {

		Map<OWLNamedIndividual, Set<OWLClass>> permutedSet = new HashMap<OWLNamedIndividual, Set<OWLClass>>();

		Set<OWLNamedIndividual> permutedIndividuals = new HashSet<OWLNamedIndividual>();
		OWLNamedIndividual newIndividual = i;
		int count = 0;
		for (OWLClass a : atts) {
			count++;
			Set<OWLClass> theseAtts = atts;
			theseAtts.remove(a);
			permutedSet.put(i, theseAtts);
			String id = g.getIdentifier(i);
			IRI iri = IRI.create(id.concat("-").concat(Integer.toString(count)));

		}
		return permutedSet;

	}
	*/
	

  
	/*
	private void getPermutations(Set<OWLClass> atts) {
    // in the style of HeapPermute
		//that it is the root (roots?) of the ontology.
		Set<OWLClass> baseCase = null;  //a single node, at the root of the ontology
		OWLClass firstClass = (OWLClass) atts.toArray()[0];
		OWLClass lastClass = (OWLClass) atts.toArray()[atts.size()-1];
		if (atts.size() == 1 && firstClass.isTopEntity()) {
			//base case: check if there is just a single attribute,
			//and make sure it is the root.
			//create a new owlindividual
			return ;
		} else {
			for (int i=0; i<atts.size(); i++) {
				//remove the lastClass, figure out what it's parents are, and 
				atts.remove(lastClass);
				lastClassParents = lastClass.getSuperClasses(null);
				atts.addAll(lastClassParents);
				atts.unique();
				getPermutations(atts);
				if (atts.size() % 2 == 1) {
					swap(firstClass, atts.toArray()[atts.size()-2])
				} else
					swap(atts.toArray()[atts.size())
					
				}
			}
		}
			
	}
	*/


	public void help() {
		System.out.println("owltools [ONTOLOGY ...] [COMMAND ...]\n");
		System.out.println("Commands/Options");
		System.out.println("  (type 'owltools COMMAND -h' for more info)");
	}

	public void helpFooter() {
		System.out.println("\nOntologies:");
		System.out.println("  These are specified as IRIs. The IRI is typically  'file:PATH' or a URL");
		System.out.println("\nLabel Resolution:");
		System.out.println("  you can pass in either a class label (enclosed in single quotes), an OBO ID or a IRI");
		System.out.println("\nExecution:");
		System.out.println("  note that commands are processed *in order*. This allows you to run mini-pipelines" +
				"  or programs on the command line.");
		System.out.println("  Each command has its own 'grammar'. Type owltools COMMAND -h to see options.");
		System.out.println("  Any argument that is not a command is assumed to be an ontology, and an attempt is made to load it.");
		System.out.println("  (again, this happens sequentially).");
		System.out.println("\nExamples:");
		System.out.println("  ");

	}

}
