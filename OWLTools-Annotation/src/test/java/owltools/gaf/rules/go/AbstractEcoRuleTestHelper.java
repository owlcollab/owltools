package owltools.gaf.rules.go;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.EcoMapperFactory.OntologyMapperPair;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.graph.OWLGraphWrapper;

public abstract class AbstractEcoRuleTestHelper extends OWLToolsTestBasics {

	protected static TraversingEcoMapper eco = null;
	protected static OWLGraphWrapper ecoGraph = null;
	private static Level elkLogLevel = null;
	private static Logger elkLogger = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		elkLogger = Logger.getLogger("org.semanticweb.elk");
		elkLogLevel = elkLogger.getLevel();
		elkLogger.setLevel(Level.ERROR);
		final OntologyMapperPair<TraversingEcoMapper> pair = EcoMapperFactory.createTraversingEcoMapper();
		eco = pair.getMapper();
		ecoGraph = pair.getGraph();
		
	}
	
	protected GafDocument loadGaf(String gaf) throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource(gaf));
		return gafdoc;
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (ecoGraph != null) {
			ecoGraph = null;
		}
		if (eco != null) {
			eco.dispose();
			eco = null;
		}
		if (elkLogLevel != null && elkLogger != null) {
			elkLogger.setLevel(elkLogLevel);
			elkLogger = null;
			elkLogLevel = null;
		}
	}
}
