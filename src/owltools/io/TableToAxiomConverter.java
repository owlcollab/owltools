package owltools.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
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
 * These could be ClassAssertion axioms, SubClassOf axioms, etc
 * 
 * @author cjm
 *
 */
public class TableToAxiomConverter {
	
	public class Config {
		public boolean isOboIdentifiers = true;
		public boolean isSwitchSubjectObject = false;
		public AxiomType axiomType;
		public OWLClass individualsType = null;
		public IRI property = null;
		
		public void setPropertyToLabel() {
			property = OWLRDFVocabulary.RDFS_LABEL.getIRI();
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
		 String sub = row[0];
		 String obj = row[1];
		if (config.isSwitchSubjectObject) {
			sub = row[1];
			obj = row[0];
		}
		Set<OWLAxiom> axs = new HashSet<OWLAxiom>();
		OWLAxiom ax = null;
		
		if (config.axiomType.equals(AxiomType.CLASS_ASSERTION)) {
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
		else if (config.axiomType.equals(AxiomType.ANNOTATION_ASSERTION)) {
			ax = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(config.property), 
					((OWLNamedObject) resolveIndividual(sub)).getIRI(), literal(obj));
		}
		else if (config.axiomType.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			ax = df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(config.property), 
					resolveIndividual(sub), resolveIndividual(obj));
		}
		else {
			// TODO
		}
		axs.add(ax);
		if (config.individualsType != null) {
			axs.add(df.getOWLClassAssertionAxiom(config.individualsType, resolveIndividual(sub)));
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
		return graph.getDataFactory().getOWLTypedLiteral(obj);
	}

	private OWLClass resolveClass(String id) {
		if (config.isOboIdentifiers) {
			return (OWLClass) graph.getOWLObjectByIdentifier(id);
		}
		return graph.getOWLClass(id);
	}

	private OWLIndividual resolveIndividual(String id) {
		if (config.isOboIdentifiers) {
			return graph.getOWLIndividualByIdentifier(id);
		}
		return graph.getOWLIndividual(IRI.create(id));
	}
}
