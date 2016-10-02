package owltools.io;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormatImpl;


public class OWLOboGraphsYamlFormat extends OWLDocumentFormatImpl {


	public OWLOboGraphsYamlFormat() {
	}

	@Nonnull
	@Override
	public String getKey() {
		return "Obo Graphs YAML Format";
	}

	@Override
	public boolean isPrefixOWLOntologyFormat() {
		return true;
	}

	@Override
	public PrefixDocumentFormat asPrefixOWLOntologyFormat() {
		throw new UnsupportedOperationException(getClass().getName()
				+ " is not a PrefixDocumentFormat");
	}
}
