
package owltools.gaf;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.parser.GAFParser;

public class GAFParserTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(GAFParserTest.class);

	@Test
	public void testParser() throws IOException{
		GAFParser p = new GAFParser();
		
		p.parse(getResource("test_gene_association_mgi.gaf"));
		
		while(p.next()){
			LOG.debug(p.toString());
			LOG.debug(p.getDb() + "\t" + p.getTaxon());
		}
	}
	
}
