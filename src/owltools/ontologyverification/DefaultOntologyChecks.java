package owltools.ontologyverification;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMeriot;
import owltools.ontologyverification.annotations.AfterReasoning;
import owltools.ontologyverification.annotations.Check;

public class DefaultOntologyChecks {

	@AfterLoading
	@Check
	public CheckResult check1(OWLGraphWrapper owlGraphWrapper) {
		return null;
	}
	
	@AfterLoading
	@AfterMeriot
	@AfterReasoning
	@Check
	public CheckResult check2(OWLGraphWrapper owlGraphWrapper) {
		return null;
	}
	
	@Check
	public CheckResult check3(OWLGraphWrapper owlGraphWrapper) {
		return null;
	}
}
