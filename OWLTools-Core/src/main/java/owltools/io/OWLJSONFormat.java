package owltools.io;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormatImpl;


public class OWLJSONFormat extends OWLDocumentFormatImpl {

	// generated
	private static final long serialVersionUID = 9043860387981929348L;

	public OWLJSONFormat() {
	}

	@Nonnull
	@Override
	public String getKey() {
		return "OWL JSON Format";
	}

	@Override
	public boolean isPrefixOWLDocumentFormat() {
		return false;
	}

	@Override
	public PrefixDocumentFormat asPrefixOWLDocumentFormat() {
		throw new UnsupportedOperationException(getClass().getName()
				+ " is not a PrefixDocumentFormat");
	}
}
