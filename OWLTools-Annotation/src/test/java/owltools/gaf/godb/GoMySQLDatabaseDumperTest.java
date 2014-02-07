package owltools.gaf.godb;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class GoMySQLDatabaseDumperTest extends OWLToolsTestBasics {
	OWLGraphWrapper g;
	Set<GafDocument> gafdocs = new HashSet<GafDocument>();
	
	public void load() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("lmajor_f2p_test_go_subset_nd.obo"));
		GafObjectsBuilder b = new GafObjectsBuilder();
		gafdocs.add( b.buildDocument(getResource("lmajor_f2p_test.gaf")) );

	}
	@Test
	public void testDump() throws OWLOntologyCreationException, OBOFormatParserException, IOException, ReferentialIntegrityException {
		load();
		GoMySQLDatabaseDumper dumper = new GoMySQLDatabaseDumper(g);
		dumper.setGafdocs(gafdocs);
		dumper.setTargetDirectory("target/godb");
		dumper.dump();
	}

}
