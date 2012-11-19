package owltools.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphWrapper;

/**
 * reads in a table (e.g. tab-delimited table) converting each row to an OWL Axiom.
 * 
 * currently only reads first two columns (sub and ob)
 * 
 * @author cjm
 *
 */
public class TableToAxiomConverter {

	private static Logger LOG = Logger.getLogger(TableToAxiomConverter.class);

	/**
	 * If AxiomType is ClassAssertion, then axioms will be of form
	 * ClassAssertion(sub obj)
	 * 
	 * If AxiomType is SubClassOf, then axioms will be of form
	 * SubClassOf(sub obj)
	 * 
	 * if a property P is specified, then obj is transformed to a class expression "P some obj'"
	 * where obj' is the original value for obj
	 * 
	 * @author cjm
	 *
	 */
	public class Config {
		public boolean isOboIdentifiers = true;
		public boolean isSwitchSubjectObject = false;
		public AxiomType<?> axiomType;
		public OWLClass individualsType = null;
		public IRI property = null;
		public String defaultCol1 = null;
		public String defaultCol2 = null;
		
		public Map<OWLClass,OWLClass> classMap = new HashMap<OWLClass,OWLClass>();
		public Map<Integer,String> iriPrefixMap = new HashMap<Integer,String>();

		public void setPropertyToLabel() {
			property = OWLRDFVocabulary.RDFS_LABEL.getIRI();
		}
		public void setPropertyToComment() {
			property = OWLRDFVocabulary.RDFS_COMMENT.getIRI();
		}

		public void setAxiomType(String n) {
			axiomType = AxiomType.getAxiomType(n);
		}
	}

	public Config config = new Config();
	public OWLGraphWrapper graph;



	public TableToAxiomConverter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
	}

	public void parse(String fn) throws IOException {
		File myFile = new File(fn);
		FileReader fileReader = new FileReader(myFile);
		BufferedReader reader = new BufferedReader(fileReader);
		String line;
		while ((line = reader.readLine()) != null) {
			String[] row = line.split("\t");
			if (config.defaultCol1 != null)
				row[0] = config.defaultCol1;
			if (config.defaultCol2 != null) {
				String[] row2 = new String[2];
				row2[0] = row[0];
				row = row2;
				row[1] = config.defaultCol2;
			}
			addRow(row);
		}

		if (config.individualsType != null) {
			OWLDataFactory df = graph.getDataFactory();
			graph.getManager().applyChange(new AddAxiom(graph.getSourceOntology(), 
					df.getOWLDeclarationAxiom(config.individualsType)));
		}

	}

	public Set<OWLAxiom> rowToAxioms(String[] row) {
		OWLDataFactory df = graph.getDataFactory();
		Set<OWLAxiom> axs = new HashSet<OWLAxiom>();

		if (row.length < 2)
			return axs;
		String sub = row[0];
		String obj = row[1];

		if (sub.length() == 0 || obj.length() == 0)
			return axs;

		if (config.isSwitchSubjectObject) {
			sub = row[1];
			obj = row[0];
		}
		// add prefix. typically just turns this into an OBO-ID
		if (config.iriPrefixMap.containsKey(1)) {
			sub = config.iriPrefixMap.get(1) + sub;
		}
		if (config.iriPrefixMap.containsKey(2)) {
			obj = config.iriPrefixMap.get(2) + obj;
		}
		OWLAxiom ax = null;

		if (config.axiomType.equals(AxiomType.CLASS_ASSERTION)) {
			// format: Arg2 Type Arg1
			OWLClass c = resolveClass(sub);
			axs.add(df.getOWLDeclarationAxiom(c));
			if (config.property == null) {
				ax = df.getOWLClassAssertionAxiom(c, (OWLIndividual) resolveIndividual(obj));
			}
			else {			
				OWLObjectProperty p = df.getOWLObjectProperty(config.property);
				OWLObjectSomeValuesFrom ce = df.getOWLObjectSomeValuesFrom(p, c);
				//System.out.println("CA :"+ce+" "+obj);
				ax = df.getOWLClassAssertionAxiom(ce,(OWLIndividual) resolveIndividual(obj));
			}
		}
		else if (config.axiomType.equals(AxiomType.SAME_INDIVIDUAL)) {
			OWLNamedIndividual c = (OWLNamedIndividual) resolveIndividual(sub);
			OWLNamedIndividual e = (OWLNamedIndividual) resolveIndividual(obj);
			axs.add(df.getOWLDeclarationAxiom(c));
			axs.add(df.getOWLDeclarationAxiom(e));
			ax = df.getOWLSameIndividualAxiom(c, e);
		}
		else if (config.axiomType.equals(AxiomType.SUBCLASS_OF)) {
			OWLClass c = resolveClass(sub);
			axs.add(df.getOWLDeclarationAxiom(c));
			if (config.property == null) {
				ax = df.getOWLSubClassOfAxiom(c, resolveClass(obj));
			}
			else {		
				// TODO - make the argument to which this applies configurable
				OWLObjectProperty p = df.getOWLObjectProperty(config.property);
				OWLObjectSomeValuesFrom ce = df.getOWLObjectSomeValuesFrom(p, resolveClass(obj));
				axs.add(df.getOWLDeclarationAxiom(resolveClass(obj)));
				//System.out.println("CA :"+ce+" "+obj);
				ax = df.getOWLSubClassOfAxiom(c, ce);
			}
		}
		else if (config.axiomType.equals(AxiomType.EQUIVALENT_CLASSES)) {
			OWLClass c = resolveClass(sub);
			OWLClass e = resolveClass(obj);
			axs.add(df.getOWLDeclarationAxiom(c));
			axs.add(df.getOWLDeclarationAxiom(e));
			ax = df.getOWLEquivalentClassesAxiom(c, e);
		}
		else if (config.axiomType.equals(AxiomType.ANNOTATION_ASSERTION)) {
			if (true) {
				//axs.add(df.getOWLDeclarationAxiom(resolveClass(sub)));
			}
			ax = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(config.property), 
					resolveIRI(sub), literal(obj));
		}
		else if (config.axiomType.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			ax = df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(config.property), 
					resolveIndividual(sub), resolveIndividual(obj));
		}
		else {
			LOG.error("Cannot handle: "+config.axiomType);
		}
		if (ax != null)
			axs.add(ax);
		if (config.individualsType != null) {
			axs.add(df.getOWLClassAssertionAxiom(config.individualsType, resolveIndividual(obj)));
		}
		return axs;
	}

	public void addRow(String[] row) {
		addRow(graph.getSourceOntology(), row);

	}
	public void addRow(OWLOntology ont, String[] row) {
		for (OWLAxiom ax : rowToAxioms(row)) {
			graph.getManager().applyChange(new AddAxiom(ont, ax));
		}
	}

	private OWLAnnotationValue literal(String obj) {
		return graph.getDataFactory().getOWLLiteral(obj);
	}
	
	private IRI resolveIRI(String id) {
		IRI iri;
		if (config.isOboIdentifiers && !id.startsWith("http:/")) {
			iri = graph.getIRIByIdentifier(id);
		}
		else {
			iri = IRI.create(id);
		}
		return iri;
	}

	// translates id to IRI if required.
	// always returns an OWLClass, even if not previously declared
	private OWLClass resolveClass(String id) {
		IRI iri = resolveIRI(id);
		OWLClass c= graph.getDataFactory().getOWLClass(iri);
		if (config.classMap.containsKey(c))
			return config.classMap.get(c);
		else
			return c;
	}
	
	// translates id to IRI if required.
	// always returns an OWLIndividual, even if not previously declared
	private OWLIndividual resolveIndividual(String id) {
		IRI iri = resolveIRI(id);
		OWLIndividual c= graph.getDataFactory().getOWLNamedIndividual(iri);
		return c;
	}
	

	/*
	private OWLIndividual xxxresolveIndividual(String id) {
		IRI iri;
		if (config.isOboIdentifiers && !id.startsWith("http:/")) {
			iri = graph.getIRIByIdentifier(id);
		}
		else {
			iri = IRI.create(id);
		}
		OWLIndividual ind = graph.getOWLIndividual(iri);
		if (ind == null)
			ind = graph.getDataFactory().getOWLNamedIndividual(iri);
		return ind;
	}
	*/

	public void buildClassMap(OWLGraphWrapper g) {
		IRI x = Obo2OWLVocabulary.IRI_OIO_hasDbXref.getIRI();
		for (OWLOntology ont : g.getAllOntologies()) {
			for (OWLClass c : ont.getClassesInSignature()) {
				for (OWLAnnotationAssertionAxiom aa : c.getAnnotationAssertionAxioms(ont)) {
					if (aa.getProperty().getIRI().equals(x)) {
						OWLAnnotationValue v = aa.getValue();
						if (v instanceof OWLLiteral) {
							String xid =((OWLLiteral)v).getLiteral();
							OWLClass xc = (OWLClass) g.getOWLObjectByIdentifier(xid);
							if (xc == null) {
								LOG.error("Cannot get class for: "+xid);
							}
							else {
								config.classMap.put(xc, c);
							}
							//LOG.info(c + " ===> "+xid);
						}
					}					
				}
			}
		}
	}
}
