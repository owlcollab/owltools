package owltools.frame.jsonld;

import java.util.HashSet;
import java.util.Set;

import owltools.frame.ClassFrame;
import owltools.frame.OntologyFrame;

public class OntologyFrameLD extends FrameLD implements OntologyFrame {

	String ontologyIRI;
	String versionIRI;
		
	Set<ClassFrameLD> classes = new HashSet<ClassFrameLD>();
	// Set<PropertyFrameLD> properties;
	
	public void addClassFrame(ClassFrame f) {
		classes.add((ClassFrameLD) f);
	}
	
}
