package owltools.frame.jsonld;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.frame.FrameMaker;
import owltools.graph.OWLGraphWrapper;
import owltools.util.OwlHelper;

public class FrameMakerLD extends FrameMaker {
	
	OWLGraphWrapper g;
	OWLReasoner reasoner;
	

	public FrameMakerLD(OWLGraphWrapper g) {
		super();
		this.g = g;
	}

	@Override
	public StubLD makeStub(OWLObject obj) {
		StubLD stub = new StubLD();
		setStubInfo(obj, stub);
		return stub;
		
	}
	
	private void setStubInfo(OWLObject obj, StubLD stub) {
		stub.id = g.getIdentifier(obj);
		stub.label = g.getLabel(obj);		
	}

	@Override
	public PropertyStubLD makePropertyStub(OWLObject obj) {
		PropertyStubLD stub = new PropertyStubLD();
		setStubInfo(obj, stub);		
		return stub;
		
	}
	
	private PropertyStubLD makeAnnotationPropertyStub(
			OWLAnnotationProperty obj) {
		PropertyStubLD stub = new PropertyStubLD();
		setStubInfo(obj, stub);		
		return stub;
	}
	
	@Override
	public ClassStubLD makeClassStub(OWLObject obj) {
		ClassStubLD stub = new ClassStubLD();
		setStubInfo(obj, stub);		
		return stub;
		
	}

	@Override
	public ClassFrameLD makeClassFrame(OWLClass c) {
		ClassFrameLD f = new ClassFrameLD();
		f.id = g.getIdentifier(c);
		f.label = g.getLabel(c);
		
		f.directSuperclasses = new HashSet<StubLD>();
		f.directRestrictions = new HashSet<RestrictionLD>();
		f.equivalentClasses = new HashSet<ExpressionLD>();
		for (OWLClassExpression x : OwlHelper.getSuperClasses(c, g.getAllOntologies())) {
			if (x.isAnonymous()) {
				if (x instanceof OWLObjectSomeValuesFrom) {
					f.directRestrictions.add(makeSomeValuesFrom((OWLObjectSomeValuesFrom)x));
				}
			}
			else {
				f.directSuperclasses.add(makeStub(x));
			}
		}
		for (OWLClassExpression x : OwlHelper.getEquivalentClasses(c, g.getAllOntologies())) {
			ExpressionLD xt = makeExpression(x);
			f.equivalentClasses.add(xt);
		}
		for (OWLOntology ont : g.getAllOntologies()) {
			for (OWLAnnotationAssertionAxiom aaa : ont.getAnnotationAssertionAxioms(c.getIRI())) {
				addAnnotations(aaa, f);
			}
		}
		return f;
	}
	
	public OntologyFrameLD makeOntologyFrame(Set<OWLClass> cset) {
		return makeOntologyFrame(null, cset);
	}
	
	public OntologyFrameLD makeOntologyFrame(String ontologyIRI, Set<OWLClass> cset) {
		OntologyFrameLD ofr = new OntologyFrameLD();
		ofr.ontologyIRI = ontologyIRI;
		
		for (OWLClass c : cset) {
			ofr.addClassFrame(makeClassFrame(c));
		}
		return ofr;
		
	}
	
	private void addAnnotations(OWLAnnotationAssertionAxiom aaa, ClassFrameLD f) {
		if (f.annotations == null)
			f.annotations = new HashSet<AnnotationLD>();
		f.annotations.add(makeAnnotation(aaa.getProperty(), aaa.getValue()));
		
	}

	private AnnotationLD makeAnnotation(OWLAnnotationProperty property,
			OWLAnnotationValue value) {
		return new AnnotationLD(makeAnnotationPropertyStub(property), makeAnnotationValue(value));
	}

	public Object makeAnnotationValue(OWLAnnotationValue v) {
		if (v instanceof IRI) {
			return ((IRI)v).toString();
		}
		else if (v instanceof OWLLiteral) {
			String lit = ((OWLLiteral)v).getLiteral();
			return lit;
		}
		else {
			return null;
		}
	}
	

	@Override
	public ExpressionLD makeExpression(OWLClassExpression x) {
		return (ExpressionLD) super.makeExpression(x);
	}

	@Override
	public ExpressionLD makeIntersectionOf(OWLObjectIntersectionOf x) {
		Set<ExpressionLD> ops = new HashSet<ExpressionLD>();
		for (OWLClassExpression op : x.getOperands()) {
			ops.add(makeExpression(op));
		}
		return new IntersectionOfLD(ops);
	}

	@Override
	public RestrictionLD makeSomeValuesFrom(OWLObjectSomeValuesFrom x) {
		SomeValuesFromLD svf = new SomeValuesFromLD(makePropertyStub(x.getProperty()), makeStub(x.getFiller()));
		return svf;
		
	}

}
