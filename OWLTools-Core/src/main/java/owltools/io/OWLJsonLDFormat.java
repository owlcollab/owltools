package owltools.io;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormatImpl;


public class OWLJsonLDFormat extends OWLDocumentFormatImpl {


	public OWLJsonLDFormat() {
	}

	@Nonnull
	@Override
	public String getKey() {
		return "OWL JSON-LD Format";
	}

	@Override
	public boolean isPrefixOWLOntologyFormat() {
		return false;
	}

	@Override
	public PrefixDocumentFormat asPrefixOWLOntologyFormat() {
		throw new UnsupportedOperationException(getClass().getName()
				+ " is not a PrefixDocumentFormat");
	}
}
