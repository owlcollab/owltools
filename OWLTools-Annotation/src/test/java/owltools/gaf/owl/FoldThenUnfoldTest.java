package owltools.gaf.owl;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class FoldThenUnfoldTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(FoldThenUnfoldTest.class);
	
	@Test
	public void testConversion() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("mgi-exttest-go-subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("mgi-exttest.gaf"));

		AnnotationExtensionFolder t1 = new AnnotationExtensionFolder(g);
		AnnotationExtensionUnfolder t2 = new AnnotationExtensionUnfolder(g);
		
		t1.fold(gafdoc);
		LOG.info("Num folds: "+t1.getFoldedClassMap().size());
		
		t2.unfold(gafdoc);
	}

}
