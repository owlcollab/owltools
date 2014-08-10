package owltools.sim2.kb;

import org.semanticweb.owlapi.model.IRI;

public class OwlSimVarianceEntityReferenceNotFoundException extends Exception {

	private static final long serialVersionUID = 6423212555022436347L;

	public OwlSimVarianceEntityReferenceNotFoundException(IRI referenceEntity) {
		super("Reference entity does not exist: " + referenceEntity.toString());
	}
}
