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
import owltools.gaf.io.GafWriter;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class UnfoldTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(UnfoldTest.class);
	
	@Test
	public void testUnfold() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("unfold-test-min.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		//g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("unfold-test.gaf"));

		AnnotationExtensionUnfolder aeu = new AnnotationExtensionUnfolder(g);
		
		aeu.unfold(gafdoc);
		//LOG.info("Num folds: "+aeu.getFoldedClassMap().size());
		
		GafWriter gw = new GafWriter();
		gw.setStream("target/unfolded.gaf");
		gw.write(gafdoc);
	}

}
