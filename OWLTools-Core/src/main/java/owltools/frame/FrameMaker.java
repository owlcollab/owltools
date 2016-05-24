package owltools.frame;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.graph.OWLGraphWrapper;

public abstract class FrameMaker {
	
	OWLGraphWrapper g;
	OWLReasoner reasoner;
	
	

	public abstract Stub makeStub(OWLObject obj);

	public abstract ClassStub makeClassStub(OWLObject obj);
	public abstract PropertyStub makePropertyStub(OWLObject obj);
	
	public abstract ClassFrame makeClassFrame(OWLClass c);
	
	public Expression makeExpression(OWLClassExpression x) {
		if (x.isAnonymous()) {
			if (x instanceof OWLObjectIntersectionOf) {
				return makeIntersectionOf((OWLObjectIntersectionOf)x);
			}
			else if (x instanceof OWLObjectSomeValuesFrom) {
				return makeSomeValuesFrom((OWLObjectSomeValuesFrom)x);
			}
			else {
				return null;
			}
		}
		else {
			return makeClassStub(x);
		}
	}

	public abstract Expression makeIntersectionOf(OWLObjectIntersectionOf x);

	public abstract Restriction makeSomeValuesFrom(OWLObjectSomeValuesFrom x);

	public String translateToJson(Frame f) {
		Gson gson = new GsonBuilder()
	    .setPrettyPrinting()
	    .disableHtmlEscaping()
	    .create();
		return gson.toJson(f);
	}
}
