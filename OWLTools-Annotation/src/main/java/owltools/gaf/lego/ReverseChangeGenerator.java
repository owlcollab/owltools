package owltools.gaf.lego;

import java.util.LinkedList;
import java.util.List;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitorEx;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;

/**
 * Create the reverse of an {@link OWLOntologyChange}.
 */
public class ReverseChangeGenerator implements OWLOntologyChangeVisitorEx<OWLOntologyChange> {

	public static final ReverseChangeGenerator INSTANCE = new ReverseChangeGenerator();
	
	private ReverseChangeGenerator() {
		// only one instance
	}

    public OWLOntologyChange visit(AddAxiom change) {
        return new RemoveAxiom(change.getOntology(), change.getAxiom());
    }


    public OWLOntologyChange visit(RemoveAxiom change) {
        return new AddAxiom(change.getOntology(), change.getAxiom());
    }


    public OWLOntologyChange visit(SetOntologyID change) {
    	return new SetOntologyID(change.getOntology(), change.getOriginalOntologyID());
    }


    public OWLOntologyChange visit(AddImport addImport) {
    	return new RemoveImport(addImport.getOntology(), addImport.getImportDeclaration());
    }


    public OWLOntologyChange visit(RemoveImport removeImport) {
    	return new AddImport(removeImport.getOntology(), removeImport.getImportDeclaration());
    }


    public OWLOntologyChange visit(AddOntologyAnnotation addOntologyAnnotation) {
    	return new RemoveOntologyAnnotation(addOntologyAnnotation.getOntology(), addOntologyAnnotation.getAnnotation());
    }


    public OWLOntologyChange visit(RemoveOntologyAnnotation removeOntologyAnnotation) {
    	return new AddOntologyAnnotation(removeOntologyAnnotation.getOntology(), removeOntologyAnnotation.getAnnotation());
    }
    
    public static List<OWLOntologyChange> invertChanges(List<OWLOntologyChange> originalChanges) {
    	final LinkedList<OWLOntologyChange> invertedChanges = new LinkedList<OWLOntologyChange>();
		for (OWLOntologyChange originalChange : originalChanges) {
			OWLOntologyChange invertedChange = originalChange.accept(ReverseChangeGenerator.INSTANCE);
			invertedChanges.push(invertedChange);
		}
		if (invertedChanges.isEmpty()) {
			return null;
		}
		return invertedChanges;
    }
}
