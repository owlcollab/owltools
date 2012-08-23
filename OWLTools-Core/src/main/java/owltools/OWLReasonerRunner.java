package owltools;

import org.apache.log4j.Logger;

import com.clarkparsia.pellet.owlapiv3.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DLExpressivityChecker;
import org.semanticweb.owlapi.util.VersionInfo;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.factplusplus.owlapiv3.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Adapted from Matthew Horridge's OWLAPI examples by Chris Mungall.
 * 
 * See: http://wiki.geneontology.org/index.php/OBO-Edit:Reasoner_Benchmarks
 */
public class OWLReasonerRunner {

	public static final String PHYSICAL_IRI = "http://purl.org/obo/owl/CARO";
	protected final static Logger logger = Logger.getLogger(OWLReasonerRunner.class);


	public static void main(String[] args) {

		Collection<String> paths = new ArrayList<String>();
		int i=0;
                // REDUNDANT: see new method
//		String reasonerClassName = "uk.ac.manchester.cs.factplusplus.owlapiv3.Reasoner";
		String reasonerName = null;
		boolean createNamedRestrictions = false;
		boolean createDefaultInstances = false;

		while (i < args.length) {
			String opt = args[i];
			System.out.println("processing arg: "+opt);
			i++;
			if (opt.equals("--pellet")) {
	//			reasonerClassName = "com.clarkparsia.pellet.owlapiv3.Reasoner";
				reasonerName = "pellet";
			}
			else if (opt.equals("--hermit")) {
		//		reasonerClassName = "org.semanticweb.HermiT.Reasoner";
				reasonerName = "hermit";
			}
			else if (opt.equals("--no-reasoner")) {
			//	reasonerClassName = "";
				reasonerName = "";
			}
			else if (opt.equals("-r") || opt.equals("--namerestr")) {
				createNamedRestrictions = true;
			}
			else if (opt.equals("-i") || opt.equals("--inst")) {
				createDefaultInstances = true;
			}
			else {
				paths.add(opt);
			}
		}

		if (paths.size() == 0) {
			paths.add(PHYSICAL_IRI);
		}
		System.out.println("OWLAPI: "+VersionInfo.getVersionInfo().getVersion());

		try {
			for (String iri : paths) {
				showMemory();
				
				// Create our ontology manager in the usual way.
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				
				OWLDataFactory df = manager.getOWLDataFactory();
				

				// Load a copy of the  ontology.
				IRI x = IRI.create(iri);
				System.out.println("loading: "+x);
				OWLOntology ont = manager.loadOntologyFromOntologyDocument(x);
				
				System.out.println("Loaded " + ont);

				showMemory();
				

				// We need to create an instance of OWLReasoner.  OWLReasoner provides the basic
				// query functionality that we need, for example the ability obtain the subclasses
				// of a class etc.  See the createReasoner method implementation for more details
				// on how to instantiate the reasoner
				OWLReasoner reasoner = createReasoner(ont,reasonerName,manager);
				


				// We now need to load some ontologies into the reasoner.  This is typically the
				// imports closure of an ontology that we're interested in.  
				Set<OWLOntology> importsClosure = manager.getImportsClosure(ont);
				System.out.println("importsClosure: "+importsClosure);
				//reasoner.loadOntologies(importsClosure);

				showMemory();
				
				OWLDataFactory owlFactory = manager.getOWLDataFactory();


				// we want to be able to make queries such as "what is a nucleus part_of"? To be able to do this
				// we have to create named restrictions for all C x P combinations, where C is a class and P is an ObjectProperty.
				// we can then say "what is a nucleus a subclass of" and then extract the part information from amongst the named classes
				// that are returned
				Set<OWLClass> nrClasses = new HashSet<OWLClass>();
				if (createNamedRestrictions) {
					System.out.println("creating named restrictions");

					Collection<OWLEquivalentClassesAxiom> newAxioms = new ArrayList<OWLEquivalentClassesAxiom>();
					Set<OWLClass> owlClasses = ont.getClassesInSignature();
					Set<OWLObjectProperty> owlProperties = ont.getObjectPropertiesInSignature();	
					for (OWLObjectProperty property : owlProperties) {
						IRI pIRI = property.getIRI();
						if (!property.isTransitive(ont))
							continue;
						System.out.println("  creating named restrictions for: "+property);
						for (OWLClass cls : owlClasses) {
							OWLObjectSomeValuesFrom restr = 
								owlFactory.getOWLObjectSomeValuesFrom(property, cls);
							IRI rIRI = IRI.create(pIRI+"/"+cls.getIRI().getFragment());
							OWLClass ec = 
								owlFactory.getOWLClass(rIRI);
							nrClasses.add(ec);
							//OWLLabelAnnotation label = 
							//	owlFactory.getOWLLabelAnnotation(pIRI.getFragment(), cls.getIRI().getFragment());
							OWLEquivalentClassesAxiom ecAxiom = owlFactory.getOWLEquivalentClassesAxiom(ec,restr);
							newAxioms.add(ecAxiom);
						}
					}
					for (OWLEquivalentClassesAxiom ecAxiom : newAxioms) {
						manager.addAxiom(ont, ecAxiom);							
					}
				}
				if (createDefaultInstances) {
					Set<OWLClass> owlClasses = ont.getClassesInSignature();

					for (OWLClass cls : owlClasses) {
						IRI iiri = IRI.create(cls.getIRI()+"/default-inst");					
						owlFactory.getOWLNamedIndividual(iiri);
						// TODO
					}
					
				}

				long initTime = System.nanoTime();
				//reasoner.prepareReasoner();
			//	reasoner.precomputeInferences(arg0)
				long totalTime = System.nanoTime() - initTime;

				showMemory();
				
				System.out.println("   Total reasoner time = "
						+ (totalTime / 1000000d) + " ms");


				// We can examine the expressivity of our ontology (some reasoners do not support
				// the full expressivity of OWL)
				DLExpressivityChecker checker = new DLExpressivityChecker(importsClosure);
				System.out.println("Expressivity: " + checker.getDescriptionLogicName());

				// We can determine if the ontology is actually consistent.  (If an ontology is
				// inconsistent then owl:Thing is equivalent to owl:Nothing - i.e. there can't be any
				// models of the ontology)
				boolean consistent = reasoner.isConsistent();
				System.out.println("Consistent: " + consistent);
				System.out.println("\n");

				// We can easily get a list of unsatisfiable classes.  (A class is unsatisfiable if it
				// can't possibly have any instances).  Note that the getunsatisfiableClasses method
				// is really just a convenience method for obtaining the classes that are equivalent
				// to owl:Nothing.
				Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
				if (unsatisfiableClasses.getSize() > 0) {
					System.out.println("The following classes are unsatisfiable: ");
					for(OWLClass cls : unsatisfiableClasses) {
						if (cls.toString().equals("Nothing"))
							continue;
						System.out.println("    unsatisfiable: " + getLabel(cls,ont,df));
					}
				}
				else {
					System.out.println("There are no unsatisfiable classes");
				}
				System.out.println("\n");

				// ------------------
				// INFERRED SUPERCLASS AND INFERRED EQUIVALENCE REPORT
				// ------------------
				
				for (OWLClass cls : ont.getClassesInSignature()) {
					if (nrClasses.contains(cls))
						continue; // do not report these
					
					// REPORT INFERRED EQUIVALENCE BETWEEN NAMED CLASSES
					for (OWLClass ec : reasoner.getEquivalentClasses(cls)) {
						if (nrClasses.contains(ec))
							continue; // do not report these
						if (cls.toString().compareTo(ec.toString()) > 0) // equivalence is symmetric: report each pair once
							System.out.println("  INFERRED: equivalent "+getLabel(cls,ont,df)+" "+getLabel(ec,ont,df));
					}
					
					// REPORT INFERRED SUBCLASSES NOT ALREADY ASSERTED

					NodeSet<OWLClass> scs = reasoner.getSuperClasses(cls, true);
					for (Node<OWLClass> scSet : scs) {
						for (OWLClass sc : scSet) {
							if (sc.toString().equals("Thing")) {
								continue;
							}
							if (nrClasses.contains(sc))
								continue; // do not report subclasses of owl:Thing

							// we do not want to report inferred subclass links
							// if they are already asserted in the ontology
							boolean isAsserted = false;
							for (OWLClassExpression asc : cls.getSuperClasses(ont)) {
								if (asc.equals(sc)) {
									// we don't want to report this
									isAsserted = true;								
								}
							}
							for (OWLClassExpression ec : cls.getEquivalentClasses(ont)) {
								
								if (ec instanceof OWLObjectIntersectionOf) {
									OWLObjectIntersectionOf io = (OWLObjectIntersectionOf)ec;
									for (OWLClassExpression op : io.getOperands()) {
										if (op.equals(sc)) {
											isAsserted = true;
										}
									}
								}
							}
							if (!isAsserted) {
								System.out.println("  INFERRED:  "+getLabel(cls,ont,df)+" subClassOf "+getLabel(sc,ont,df));
							}
						}
					}
				}
				
				// ------------------
				// CREATE NEW ONTOLOGY WITH INFERENCES
				// ------------------

				
				// To generate an inferred ontology we use implementations of inferred axiom generators
				// to generate the parts of the ontology we want (e.g. subclass axioms, equivalent classes
				// axioms, class assertion axiom etc. - see the org.semanticweb.owlapi.util package for more
				// implementations).  
				// Set up our list of inferred axiom generators
				List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
				gens.add(new InferredSubClassAxiomGenerator());

				// Put the inferred axiomns into a fresh empty ontology - note that there
				// is nothing stopping us stuffing them back into the original asserted ontology
				// if we wanted to do this.
				OWLOntology infOnt = manager.createOntology(IRI.create(ont.getOntologyID() + "_inferred"));

				// Now get the inferred ontology generator to generate some inferred axioms
				// for us (into our fresh ontology).  We specify the reasoner that we want
				// to use and the inferred axiom generators that we want to use.
				InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, gens);
				iog.fillOntology(manager, infOnt);

//				OWLOntologyFormat owlFormat = new org.semanticweb.owlapi.io.RDFXMLOntologyFormat();
				OWLXMLOntologyFormat owlFormat = new OWLXMLOntologyFormat();

				// Save the inferred ontology. (Replace the IRI with one that is appropriate for your setup)
				manager.saveOntology(infOnt, owlFormat, IRI.create("file:///tmp/inferredont.owl"));

				//manager.saveOntology(infOnt, IRI.create("file:///tmp/inferredont.owl"));

				// dispose of old reasoner
				reasoner.dispose();
			}
		}


		catch(UnsupportedOperationException exception) {
			logger.error("Unsupported reasoner operation.");
		}
		catch (OWLOntologyCreationException e) {
			logger.error("Could not load the ontology: " + e.getMessage());
		} catch (OWLOntologyChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownOWLOntologyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String getLabel(OWLClass cls, OWLOntology ont, OWLDataFactory df) {
		String label = cls.toString();

		OWLAnnotationProperty ap = df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		for (OWLAnnotation a : cls.getAnnotations(ont, ap)) {
			label = "["+cls.toString()+" ! "+a.getValue().toString()+"]";
		}
		return label;
	}

	private static OWLReasoner createReasoner(OWLOntology ont, String reasonerName, 
			OWLOntologyManager manager) {
			OWLReasonerFactory reasonerFactory = null;
			if (reasonerName == null || reasonerName.equals("factpp"))
				reasonerFactory = new FaCTPlusPlusReasonerFactory();
			else if (reasonerName.equals("pellet"))
				reasonerFactory = new PelletReasonerFactory();
			else if (reasonerName.equals("hermit")) {
				//return new org.semanticweb.HermiT.Reasoner.ReasonerFactory().createReasoner(ont);
				//reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();			
			}
			else
				logger.error("no such reasoner: "+reasonerName);
			
			OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
			return reasoner;
	}
	
	public static void showMemory() {
		System.gc();
		System.gc();
		System.gc();
		long tm = Runtime.getRuntime().totalMemory();
		long fm = Runtime.getRuntime().freeMemory();
		long mem = tm-fm;
		System.out.println("Memory total:"+tm+" free:"+fm+" diff:"+mem+" (bytes) diff:"+(mem/1000000)+" (mb)");

	}
	
//	private void checkEL() {
//		OWL2ELProfile prof = new OWL2ELProfile();
//	}
//	
//	private static OWLReasoner old__createReasoner(OWLOntologyManager man, String reasonerClassName) {
//		try {
//			// The following code is a little overly complicated.  The reason for using
//			// reflection to create an instance of pellet is so that there is no compile time
//			// dependency (since the pellet libraries aren't contained in the OWL API repository).
//			// Normally, one would simply create an instance using the following incantation:
//			//
//			//     OWLReasoner reasoner = new Reasoner()
//			//
//			// Where the full class name for Reasoner is org.mindswap.pellet.owlapi.Reasoner
//			//
//			// Pellet requires the Pellet libraries  (pellet.jar, aterm-java-x.x.jar) and the
//			// XSD libraries that are bundled with pellet: xsdlib.jar and relaxngDatatype.jar
//			//String reasonerClassName = "org.mindswap.pellet.owlapi.Reasoner";
//			//String reasonerClassName = "uk.ac.manchester.cs.factplusplus.owlapi.Reasoner";
//			Class reasonerClass = Class.forName(reasonerClassName);
//			Constructor<OWLReasoner> con = reasonerClass.getConstructor(OWLOntologyManager.class);
//			return con.newInstance(man);
//		}
//		catch (ClassNotFoundException e) {
//			throw new RuntimeException(e);
//		}
//		catch (IllegalAccessException e) {
//			throw new RuntimeException(e);
//		}
//		catch (NoSuchMethodException e) {
//			throw new RuntimeException(e);
//		}
//		catch (InvocationTargetException e) {
//			throw new RuntimeException(e);
//		}
//		catch (InstantiationException e) {
//			throw new RuntimeException(e);
//		}
//	}
}
