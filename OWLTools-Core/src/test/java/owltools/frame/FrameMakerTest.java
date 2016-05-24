package owltools.frame;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.OWLToolsTestBasics;
import owltools.frame.jsonld.ClassFrameLD;
import owltools.frame.jsonld.FrameMakerLD;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link FrameMakerLD}.
 * 
 */
public class FrameMakerTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(OWLToolsTestBasics.class);

	
	@Test
	public void testMakeClassFrame() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("regulation_of_anti_apoptosis_xp-baseline.obo"));
		FrameMakerLD fm = new FrameMakerLD(g);
		for (OWLClass c : g.getAllOWLClasses()) {
			ClassFrameLD f = fm.makeClassFrame(c);
			String json = fm.translateToJson(f);
			LOG.info(json);
		}

	}

}
