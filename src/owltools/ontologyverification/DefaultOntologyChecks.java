package owltools.ontologyverification;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.Check;

public class DefaultOntologyChecks {

	@AfterLoading
	@Check
	public CheckResult check1(OWLGraphWrapper owlGraphWrapper) {
		return null;
	}
}
