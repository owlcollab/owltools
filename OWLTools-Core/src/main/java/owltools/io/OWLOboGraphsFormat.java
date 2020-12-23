package owltools.io;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormatImpl;


public class OWLOboGraphsFormat extends OWLDocumentFormatImpl {


	public OWLOboGraphsFormat() {
	}

	@Nonnull
	@Override
	public String getKey() {
		return "Obo Graphs Format";
	}

	@Override
	public boolean isPrefixOWLDocumentFormat() {
		return true;
	}

	@Override
	public PrefixDocumentFormat asPrefixOWLDocumentFormat() {
		throw new UnsupportedOperationException(getClass().getName()
				+ " is not a PrefixDocumentFormat");
	}
}
